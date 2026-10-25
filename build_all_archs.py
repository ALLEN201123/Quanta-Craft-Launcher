# -*- coding: utf-8 -*-
# 串行构建 5 个架构包。修正：按 ABI 标签精确匹配产物（原按"最新修改时间"取，会抓到别的架构的包）
import subprocess, os, glob, shutil, sys, time, re

JAVA_HOME = r'C:\Program Files\Java\jdk-17'
BUILD_DIR = r'D:\QCL-build'
OUT_DIR = r'D:\HMCL-PE重构QCL计划\outputs\release_1.1.2'
LOGDIR = r'D:\HMCL-PE重构QCL计划\outputs\_110_work'
# 2026-10-25：build.gradle 里 buildDir 覆盖回 QCL/build_alt（用户要求新包出在 build_alt）
APK_DIR = os.path.join(BUILD_DIR, 'QCL', 'build_alt', 'outputs', 'apk', 'release')

env = dict(os.environ)
env['JAVA_HOME'] = JAVA_HOME
env['PATH'] = JAVA_HOME + r'\bin;' + env.get('PATH', '')

# 读 build.gradle 的 versionName（产物命名与内部版本保持一致）
bg = open(os.path.join(BUILD_DIR, 'QCL', 'build.gradle'), encoding='utf-8', errors='replace').read()
vname = re.search(r'versionName\s+"([^"]+)"', bg).group(1)
print('versionName =', vname, flush=True)
# ★ 产物目录跟随版本号：release_<versionName>（避免 1.1.3 的包落进 release_1.1.2）
OUT_DIR = os.path.join(os.path.dirname(OUT_DIR), 'release_' + vname)
os.makedirs(OUT_DIR, exist_ok=True)
print('OUT_DIR =', OUT_DIR, flush=True)

# (构建参数 arch, 输出 label, apk 文件名里的 abi 标签)
# ★ build.gradle: abiTag = (qclArch == "all") ? "universal" : qclArch —— 用的是 -Darch 原值
targets = [
    ('arm',     'arm',      'arm'),
    ('arm64',   'arm64',    'arm64'),
    ('x86',     'x86',      'x86'),
    ('x86_64',  'x86_64',   'x86_64'),
    ('all',     'universal', 'universal'),
]

report = []
for arch, label, abitag in targets:
    # 构建前清掉输出目录里同 label 的旧产物，避免残留混淆
    for old in glob.glob(os.path.join(APK_DIR, '*.apk')):
        try:
            os.remove(old)
        except OSError:
            pass
    log_path = os.path.join(LOGDIR, 'build_release_%s.log' % label)
    args = ['gradlew.bat'] + ([] if arch == 'all' else ['-Darch=' + arch]) + [':QCL:assembleRelease']
    t0 = time.time()
    with open(log_path, 'wb') as lf:
        p = subprocess.run(args, cwd=BUILD_DIR, stdout=lf, stderr=subprocess.STDOUT,
                           env=env, shell=True, timeout=2400)
    rc = p.returncode
    # 精确匹配：产物名形如 QCL-release-<ver>-<abitag>.apk
    exact = os.path.join(APK_DIR, 'QCL-release-%s-%s.apk' % (vname, abitag))
    src = exact if os.path.isfile(exact) else None
    if src is None:
        cands = glob.glob(os.path.join(APK_DIR, '*-%s.apk' % abitag))
        src = cands[0] if cands else None
    dst = None
    if src:
        dst = os.path.join(OUT_DIR, 'QCL-release-%s-%s.apk' % (vname, label))
        shutil.copy2(src, dst)
    size = os.path.getsize(dst) if dst and os.path.exists(dst) else 0
    report.append((label, rc, size, int(time.time() - t0), os.path.basename(src) if src else 'NOT-FOUND'))
    print('%-10s rc=%d size=%d dur=%ds src=%s' % (label, rc, size, int(time.time() - t0),
                                                  os.path.basename(src) if src else 'NOT-FOUND'), flush=True)

# ★ 1.2.6：顺手把 5 个包也复制一份到 outputs 根目录
#   （产物按版本落在 release_<版本>\ 下，但用户习惯直接在 outputs\ 里找：1.2.4 问过一次、1.2.6 又问一次）
try:
    ROOT_MIRROR = os.path.dirname(OUT_DIR)
    for _f in glob.glob(os.path.join(OUT_DIR, '*.apk')):
        _dst = os.path.join(ROOT_MIRROR, os.path.basename(_f))
        if not os.path.exists(_dst) or os.path.getsize(_dst) != os.path.getsize(_f):
            shutil.copy2(_f, _dst)
            print('已放到 outputs 根目录: ' + os.path.basename(_f), flush=True)
except Exception as _e:
    print('复制到 outputs 根目录失败（不影响构建）:', _e, flush=True)

print('==== SUMMARY ====', flush=True)
for label, rc, size, dur, src in report:
    print('%-10s rc=%d %12d bytes (%.1f MB) %3ds  from=%s' % (label, rc, size, size / 1e6, dur, src), flush=True)
