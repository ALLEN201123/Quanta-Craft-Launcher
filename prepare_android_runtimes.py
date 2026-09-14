"""Fetch and verify FCL Android JRE assets; keep replaced Linux runtimes outside assets."""
from pathlib import Path
import concurrent.futures, hashlib, json, subprocess, tarfile, shutil, struct, posixpath
ROOT = Path(__file__).resolve().parent
CACHE = ROOT / "runtime-source"
CACHE.mkdir(exist_ok=True)

def get(url):
    return subprocess.check_output(["curl", "--ssl-revoke-best-effort", "-fLsS", "--retry", "2", "--max-time", "180", url])

revision = json.loads(get("https://api.github.com/repos/FCL-Team/FoldCraftLauncher/commits/main"))
commit = revision["sha"]
tree_sha = revision["commit"]["tree"]["sha"]
tree = json.loads(get("https://api.github.com/repos/FCL-Team/FoldCraftLauncher/git/trees/" + tree_sha + "?recursive=1"))
entries = {x["path"]: x for x in tree["tree"]}
prefix = "FCL/src/main/jreAssets/app_runtime/java/"
selected = [p for p in entries if p.startswith((prefix + "jre21/", prefix + "jre25/")) and p.endswith(".tar.xz")]

def fetch(p):
    info = entries[p]
    out = CACHE / (p.split("/")[-2] + "-" + p.split("/")[-1])
    data = out.read_bytes() if out.exists() else get("https://raw.githubusercontent.com/FCL-Team/FoldCraftLauncher/" + commit + "/" + p)
    assert len(data) == info["size"], (p, len(data), info["size"])
    blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    assert blob == info["sha"], (p, "Git blob hash mismatch")
    out.write_bytes(data)
    print("Verified", out.name, len(data), flush=True)
    return p, out, hashlib.sha256(data).hexdigest()

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    files = list(pool.map(fetch, selected))
assets = ROOT / "HMCLPE/src/main/assets/app_runtime/java"
stage = CACHE / ("prepared-" + commit[:12])
stage.mkdir(exist_ok=True)

def unpack(archive, dest):
    dest.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive) as tar:
        links = []
        for entry in tar:
            relative = posixpath.normpath(entry.name)
            if relative == ".":
                continue
            assert not relative.startswith(("/", "../")) and ":" not in relative, relative
            target = dest / relative
            assert target.resolve().is_relative_to(dest.resolve())
            if entry.isdir():
                target.mkdir(parents=True, exist_ok=True)
            elif entry.isfile():
                target.parent.mkdir(parents=True, exist_ok=True)
                with tar.extractfile(entry) as src, target.open("wb") as out:
                    shutil.copyfileobj(src, out)
            elif entry.issym() or entry.islnk():
                link = posixpath.normpath(posixpath.join(posixpath.dirname(relative), entry.linkname) if entry.issym() else entry.linkname)
                assert not link.startswith(("/", "../")) and ":" not in link
                links.append((target, dest / link))
            else:
                raise RuntimeError("Unsupported archive entry: " + entry.name)
        for target, src in links:
            assert src.is_file(), (str(target), str(src))
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(src, target)

for p, archive, sha in files:
    name, part = p.split("/")[-2:]
    major = name[3:]
    target_name = "JRE" + major if part == "universal.tar.xz" else major + "-" + part.removeprefix("bin-").removesuffix(".tar.xz")
    unpack(archive, stage / target_name)

readelf = "D:/Android/Sdk/ndk/27.3.13750724/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-readelf.exe"
report = {"repository": "FCL-Team/FoldCraftLauncher", "commit": commit, "files": [{"path": p, "sha256": sha} for p, a, sha in files], "architectures": {}}
for major in (21, 25):
    common = stage / ("JRE" + str(major))
    (common / "version").write_text("20260913", encoding="ascii")
    archs = []
    for directory in stage.glob(str(major) + "-*"):
        for f in directory.rglob("libjvm.so"):
            output = subprocess.check_output([readelf, "-d", str(f)], text=True)
            assert "libc.so.6" not in output and "libc.so]" in output, output
            print("Android Bionic verified:", f, flush=True)
        archs.append(directory.name)
    assert archs
    report["architectures"][str(major)] = archs

# All downloads/extractions validated before changing packaged assets.
quarantine = CACHE / "original-linux-runtimes"
quarantine.mkdir(exist_ok=True)
for major in (21, 25):
    old = assets / ("JRE" + str(major))
    if old.exists() and not (quarantine / old.name).exists():
        shutil.move(str(old), str(quarantine / old.name))
for directory in stage.iterdir():
    if directory.is_dir():
        shutil.copytree(directory, assets / directory.name, dirs_exist_ok=True)
(CACHE / "runtime-provenance.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
print("Android runtimes prepared", report["architectures"], flush=True)
