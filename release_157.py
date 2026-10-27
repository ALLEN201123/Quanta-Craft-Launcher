# -*- coding: utf-8 -*-
"""QCL 1.5.7 一键发布：
   ① 删旧 v1.5.7 Release（避免同名重传 422）② 创建 + 上传 5 个架构包
   ③ 推 launcher_version.json（359/1.5.7）+ 解 base64 复核 ④ 清 jsDelivr 缓存
   ⑤ 设默认分支为 1.5.7
   （tag v1.5.7 由发布前的 git 步骤手动建好并推上去，这里只负责挂 Release）
"""
import base64
import glob
import json
import os
import ssl
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request

REPO = 'ALLEN201123/Quanta-Craft-Launcher'
TAG = 'v1.5.7'
NAME = 'QCL 1.5.7'
BRANCH = '1.5.7'
VC = 359
VN = '1.5.7'
SRC = r'D:\HMCL-PE重构QCL计划\outputs\release_1.5.7'
NOTES = r'D:\HMCL-PE重构QCL计划\outputs\_110_work\release_notes_1.5.7.md'
JSON_LOCAL = r'D:\_qcl_test\launcher_version_157.json'
CTX = ssl.create_default_context()

# ★ git credential fill 走 helper-selector 会卡死；直接调 wincred 助手拿凭证
WINCRED_CANDIDATES = [
    r'C:/Program Files/Git/mingw64/libexec/git-core/git-credential-wincred.exe',
    r'C:/Users/123/.workbuddy/binaries/PortableGit/versions/1.2.0/mingw64/bin/git-credential-wincred.exe',
]
p = None
for cand in WINCRED_CANDIDATES:
    if os.path.isfile(cand):
        p = subprocess.run([cand, 'get'], input=b'protocol=https\nhost=github.com\n\n',
                           capture_output=True, cwd=r'D:/QCL-build')
        if b'password=' in p.stdout:
            print('    credential helper:', cand)
            break
else:
    p = subprocess.run(['git', 'credential', 'fill'], input=b'protocol=https\nhost=github.com\n\n',
                       capture_output=True, cwd=r'D:/QCL-build')
TOKEN = [l.split('=', 1)[1].strip() for l in p.stdout.decode('utf-8', 'replace').splitlines()
         if l.startswith('password=')][0]
HDR = {'Authorization': 'token ' + TOKEN, 'User-Agent': 'qcl-release',
       'Accept': 'application/vnd.github+json'}


def api(method, url, data=None, retries=4):
    for i in range(retries):
        try:
            h = dict(HDR)
            body = None
            if data is not None:
                body = json.dumps(data).encode()
                h['Content-Type'] = 'application/json'
            req = urllib.request.Request(url, data=body, headers=h, method=method)
            with urllib.request.urlopen(req, timeout=180, context=CTX) as r:
                raw = r.read()
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            if e.code in (502, 503, 504) and i < retries - 1:
                time.sleep(3 + i * 4)
                continue
            raise


def upload(url, path, retries=5):
    size = os.path.getsize(path)
    name = os.path.basename(path)
    for i in range(retries):
        try:
            with open(path, 'rb') as f:
                req = urllib.request.Request(url + '?name=' + urllib.parse.quote(name),
                                             data=f.read(), method='POST')
                req.add_header('Authorization', 'token ' + TOKEN)
                req.add_header('Content-Type', 'application/vnd.android.package-archive')
                req.add_header('User-Agent', 'qcl-release')
                req.add_header('Content-Length', str(size))
                with urllib.request.urlopen(req, timeout=1800, context=CTX) as r:
                    return json.loads(r.read())
        except urllib.error.HTTPError as e:
            if e.code in (502, 503, 504) and i < retries - 1:
                print('   %s upload %d/%d failed(%d), retry' % (name, i + 1, retries, e.code), flush=True)
                time.sleep(5 + i * 6)
                continue
            raise
    raise RuntimeError('upload failed: ' + name)


# ① 删旧 Release
print('[1] check and delete existing %s ...' % TAG)
try:
    rel = api('GET', 'https://api.github.com/repos/%s/releases/tags/%s' % (REPO, TAG))
    print('    found id=%s, deleting ...' % rel['id'])
    api('DELETE', 'https://api.github.com/repos/%s/releases/%d' % (REPO, rel['id']))
    print('    deleted OK')
except Exception as e:
    print('    no old release (or already deleted):', str(e)[:60])

# ② 创建 + 上传
print('[2] create Release ...')
apks = sorted(glob.glob(os.path.join(SRC, '*.apk')))
print('    %d apk(s) to upload' % len(apks))
assert len(apks) == 5, 'expect 5 apks, got %d' % len(apks)
notes = open(NOTES, encoding='utf-8').read()
rel = api('POST', 'https://api.github.com/repos/%s/releases' % REPO,
          {'tag_name': TAG, 'name': NAME, 'body': notes, 'draft': False, 'prerelease': False})
print('    created id=%s' % rel['id'])
url = rel['upload_url'].split('{')[0]
for a in apks:
    print('    uploading %s (%d) ...' % (os.path.basename(a), os.path.getsize(a)), flush=True)
    t0 = time.time()
    upload(url, a)
    print('      done %.0fs' % (time.time() - t0), flush=True)
rd = api('GET', 'https://api.github.com/repos/%s/releases/tags/%s' % (REPO, TAG))
print('    release has %d assets:' % len(rd['assets']))
for a in rd['assets']:
    print('       %-42s %d' % (a['name'], a['size']))
print('    page:', rd['html_url'])

# ③ 推 launcher_version.json
print('[3] push launcher_version.json (%d / %s) ...' % (VC, VN))
raw = open(JSON_LOCAL, encoding='utf-8').read()
API = 'https://api.github.com/repos/%s/contents/launcher_version.json' % REPO
cur = api('GET', API + '?ref=main')
print('    online old versionCode =',
      json.loads(base64.b64decode(cur['content']).decode('utf-8'))['latestRelease']['versionCode'])
body = {'message': 'launcher_version.json: %s (versionCode %d)' % (VN, VC),
        'content': base64.b64encode(raw.encode('utf-8')).decode('utf-8'),
        'sha': cur['sha'], 'branch': 'main'}
r = api('PUT', API, body)
print('    PUT commit =', r['commit']['sha'][:12])
ok = False
for i in range(8):
    time.sleep(4)
    d = api('GET', API + '?ref=main')
    j = json.loads(base64.b64decode(d['content']).decode('utf-8'))
    got = j['latestRelease']['versionCode']
    print('    check %d: GitHub main = %s / %s' % (i + 1, got, j['latestRelease']['versionName']))
    if got == VC:
        print('    OK confirmed %d' % VC)
        ok = True
        break
if not ok:
    print('    !! not confirmed as %d' % VC)

# ④ 清 jsDelivr 缓存
print('[4] purge jsDelivr cache ...')
for purge in ['https://purge.jsdelivr.net/gh/%s@main/launcher_version.json' % REPO,
              'https://purge.jsdelivr.net/gh/%s/launcher_version.json' % REPO]:
    try:
        out = urllib.request.urlopen(urllib.request.Request(purge, headers={'User-Agent': 'qcl'}),
                                     timeout=60, context=CTX).read().decode('utf-8')
        print('    purge ->', out.replace('\n', ' ')[:40])
    except Exception as e:
        print('    purge failed', str(e)[:60])

# ⑤ 默认分支
print('[5] set default branch to %s ...' % BRANCH)
try:
    r = api('PATCH', 'https://api.github.com/repos/%s' % REPO, {'default_branch': BRANCH})
    print('    result default_branch =', r.get('default_branch'))
except Exception as e:
    print('    failed:', str(e)[:80])
repo = api('GET', 'https://api.github.com/repos/%s' % REPO)
print('    verify default branch =', repo.get('default_branch'))
print('\n=== ALL DONE ===')
