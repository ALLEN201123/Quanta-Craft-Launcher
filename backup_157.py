# -*- coding: utf-8 -*-
"""QCL 1.5.7 发布后备份：三件套 zip + backup_code_1.5.7（bundle + 密钥 + 说明）

产出（D:\\HMCL-PE重构QCL计划\\）：
  QCL-1.5.7-完整源码.zip        git archive，只含 git 跟踪文件
  QCL-1.5.7-原生库.zip          以 1.5.6 同名单的 819 个文件为准（大文件集合，gitignore 掉的运行库）
  QCL-1.5.7-构建中间产物.zip    runtime-source/
  QCL-1.5.7-源码包说明.txt
  backup_code_1.5.7\\           qcl_1.5.7.gitbundle + 3 个密钥 + README_恢复说明.txt
"""
import hashlib
import os
import subprocess
import time
import zipfile

VER = '1.5.7'
BRANCH = '1.5.7'
REPO = r'D:\QCL-build'
OUT = r'D:\HMCL-PE重构QCL计划'
PREFIX = 'QCL-%s/' % VER
LOG_PATH = os.path.join(OUT, 'outputs', '_110_work', 'backup_%s.log' % VER)
os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)
LOG = open(LOG_PATH, 'w', encoding='utf-8')


def log(*a):
    s = ' '.join(str(x) for x in a)
    print(s, flush=True)
    LOG.write(s + '\n')
    LOG.flush()


def sha256(p):
    h = hashlib.sha256()
    with open(p, 'rb') as f:
        for c in iter(lambda: f.read(1 << 20), b''):
            h.update(c)
    return h.hexdigest()


def human(n):
    return '%.1f MB' % (n / 1e6)


head = subprocess.run(['git', 'rev-parse', 'HEAD'], cwd=REPO, capture_output=True,
                      text=True, encoding='utf-8').stdout.strip()
tracked = subprocess.run(['git', 'ls-files'], cwd=REPO, capture_output=True,
                         text=True, encoding='utf-8').stdout.splitlines()
log('分支 %s @ %s，git 跟踪 %d 个文件' % (BRANCH, head[:12], len(tracked)))

# ---------- 1) 主源码包 ----------
main_zip = os.path.join(OUT, 'QCL-%s-完整源码.zip' % VER)
t0 = time.time()
r = subprocess.run(['git', 'archive', '--format=zip', '-9', '-o', main_zip,
                    '--prefix=' + PREFIX, BRANCH],
                   cwd=REPO, capture_output=True, text=True, encoding='utf-8', errors='replace')
if r.returncode != 0:
    log('!! git archive 失败:', r.stderr[:600])
    raise SystemExit(1)
log('[1] 主源码包 %s  用时 %ds' % (human(os.path.getsize(main_zip)), int(time.time() - t0)))

with zipfile.ZipFile(main_zip) as z:
    bad = z.testzip()
    names = z.namelist()
log('    zip 校验:', 'OK' if bad is None else '!! 损坏于 ' + str(bad), '条目', len(names))
must_absent = ['.jks', '.keystore', 'local.properties', '.apk', '/build/']
leaks = [n for n in names if any(m in n for m in must_absent)]
log('    敏感文件泄漏:', '无' if not leaks else '%d 个 %s' % (len(leaks), leaks[:5]))
for probe in ['QCL/build.gradle',
              'QCL/src/main/java/com/qcl/launcher/launcher/download/fabric/LegacyFabricSupport.java',
              'QCL/src/main/java/com/qcl/launcher/launcher/download/favorite/FavoriteManager.java',
              'QCL/src/main/assets/announcement.json', 'README.md', 'release_157.py']:
    log('    抽查 %-78s %s' % (probe, 'OK' if PREFIX + probe in names else '!! 缺失'))

# ---------- 2) 原生库包（以 1.5.6 清单为准）----------
ref = os.path.join(OUT, 'QCL-1.5.6-原生库.zip')
native_zip = os.path.join(OUT, 'QCL-%s-原生库.zip' % VER)
t0 = time.time()
with zipfile.ZipFile(ref) as zref:
    want = [i.filename for i in zref.infolist() if not i.is_dir()]
added = missing = 0
with zipfile.ZipFile(native_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=1) as z:
    for rel in want:
        sp = os.path.join(REPO, rel.replace('/', os.sep))
        if not os.path.isfile(sp):
            missing += 1
            continue
        z.write(sp, PREFIX + rel)
        added += 1
log('[2] 原生库包 %s  %d 个文件（缺 %d）  用时 %ds'
    % (human(os.path.getsize(native_zip)), added, missing, int(time.time() - t0)))
with zipfile.ZipFile(native_zip) as z:
    log('    zip 校验:', 'OK' if z.testzip() is None else '!! 损坏')

# ---------- 3) 构建中间产物 ----------
mid_src = os.path.join(REPO, 'runtime-source')
mid_zip = os.path.join(OUT, 'QCL-%s-构建中间产物.zip' % VER)
if os.path.isdir(mid_src):
    t0 = time.time()
    n = 0
    with zipfile.ZipFile(mid_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=1) as z:
        for root, dirs, files in os.walk(mid_src):
            for f in files:
                p = os.path.join(root, f)
                try:
                    z.write(p, PREFIX + 'runtime-source/' + os.path.relpath(p, mid_src).replace(os.sep, '/'))
                    n += 1
                except Exception as e:
                    log('    跳过', p, str(e)[:60])
    log('[3] 中间产物包 %s  %d 文件  用时 %ds'
        % (human(os.path.getsize(mid_zip)), n, int(time.time() - t0)))
else:
    log('[3] runtime-source 不存在，跳过')

# ---------- 4) backup_code_1.5.7 ----------
bk = os.path.join(OUT, 'backup_code_%s' % VER)
os.makedirs(bk, exist_ok=True)
bundle = os.path.join(bk, 'qcl_%s.gitbundle' % VER)
t0 = time.time()
r = subprocess.run(['git', 'bundle', 'create', bundle, '--all'], cwd=REPO,
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
if r.returncode != 0:
    log('!! git bundle 失败:', r.stderr[:600])
    raise SystemExit(1)
log('[4] git bundle %s  用时 %ds' % (human(os.path.getsize(bundle)), int(time.time() - t0)))
# bundle 自校验
r = subprocess.run(['git', 'bundle', 'verify', bundle], cwd=REPO,
                   capture_output=True, text=True, encoding='utf-8', errors='replace')
log('    bundle verify:', 'OK' if r.returncode == 0 else '!! ' + (r.stderr or r.stdout)[:300])
hmac_ = hashlib.sha256(open(bundle, 'rb').read()).hexdigest()
log('    bundle sha256:', hmac_)

for ks in ('qcl.keystore', 'key-store.jks', 'debug.keystore'):
    src = os.path.join(REPO, ks)
    if os.path.isfile(src):
        dst = os.path.join(bk, ks)
        if not os.path.exists(dst) or os.path.getsize(dst) != os.path.getsize(src):
            with open(src, 'rb') as f, open(dst, 'wb') as g:
                g.write(f.read())
        log('    密钥 %-18s %d bytes' % (ks, os.path.getsize(dst)))
    else:
        log('    !! 密钥缺失', ks)

# ---------- 5) 说明文件 ----------
VCMAP = {'1.5.7': 359}
txt = os.path.join(OUT, 'QCL-%s-源码包说明.txt' % VER)
with open(txt, 'w', encoding='utf-8', newline='\n') as fh:
    fh.write("""QCL %(VER)s 源码包说明

【打完包时的状态】
  分支      = %(BRANCH)s
  HEAD      = %(HEAD)s
  git 跟踪  = %(NTRACK)d 个文件
  versionCode = %(VC)d / versionName = %(VER)s

【文件】
  QCL-%(VER)s-完整源码.zip          %(SZ_MAIN)s
      用 git archive 从分支 %(BRANCH)s 打出，包内顶层目录是 QCL-%(VER)s/。
      已自动排除 build/、.gradle/、.idea/、签名密钥、*.apk。
      SHA-256 : %(SHA_MAIN)s

  QCL-%(VER)s-原生库.zip            %(SZ_NATIVE)s
      gitignore 掉的运行库/原生库大文件集合（与 1.5.6 同清单 %(N_NATIVE)d 项）。
      不含它仍可编译，但内置 Java 运行时需重新 prepare_android_runtimes.py 生成。

  QCL-%(VER)s-构建中间产物.zip      %(SZ_MID)s
      runtime-source/（JRE 准备中间产物）。

【怎么恢复】
  1) 解压完整源码 zip，得到 QCL-%(VER)s/ 目录。
  2) 解压原生库 zip 与中间产物 zip 到同一目录（覆盖合并）。
  3) 把 backup_code_%(VER)s/ 里的 qcl.keystore、key-store.jks、debug.keystore 放回工程根。
  4) 构建（JDK 17）：
       cd QCL-%(VER)s
       ./gradlew :QCL:assembleRelease -Darch=arm64
     出全架构包不加 -Darch。
  5) 需要 git 历史：git clone backup_code_%(VER)s/qcl_%(VER)s.gitbundle QCL-build

【核对】
  certutil -hashfile "QCL-%(VER)s-完整源码.zip" SHA256

打包时间：%(STAMP)s
""" % {
        'VER': VER, 'BRANCH': BRANCH, 'HEAD': head, 'NTRACK': len(tracked),
        'VC': VCMAP[VER],
        'SZ_MAIN': human(os.path.getsize(main_zip)), 'SHA_MAIN': sha256(main_zip),
        'SZ_NATIVE': human(os.path.getsize(native_zip)), 'N_NATIVE': len(want),
        'SZ_MID': human(os.path.getsize(mid_zip)) if os.path.isfile(mid_zip) else '(未生成)',
        'STAMP': time.strftime('%Y-%m-%d %H:%M:%S'),
    })
log('[5] 说明文件:', os.path.basename(txt))

log('\n===== 结果 =====')
for p in (main_zip, native_zip, mid_zip, txt, bundle):
    if os.path.isfile(p):
        log('  %-40s %10s' % (os.path.basename(p), human(os.path.getsize(p))))
log('备份完成')
