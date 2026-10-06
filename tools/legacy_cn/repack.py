# -*- coding: utf-8 -*-
"""把 StrTranslate 的产物 class 覆盖回原 jar。
   ★ 产物目录里混了整个 jar 的资源（png/ogg/txt，87 个），绝不能整目录塞进去 ——
   会用「英文原文资源」覆盖掉 QCL 注入的官方中文点阵 font/glyph_XX.png。
   只挑 .class，且只覆盖原 jar 里本来就存在的条目。"""
import zipfile, os, sys, shutil

orig_jar = sys.argv[1]
tr_dir   = sys.argv[2]
out_jar  = sys.argv[3]

zf = zipfile.ZipFile(orig_jar, 'r')
names = set(zf.namelist())

# 收集产物里的 class（相对路径）
tr_classes = {}
for root, dirs, files in os.walk(tr_dir):
    for fn in files:
        if not fn.endswith('.class'):
            continue
        full = os.path.join(root, fn)
        rel = os.path.relpath(full, tr_dir).replace('\\', '/')
        tr_classes[rel] = open(full, 'rb').read()

only_in_tr = [k for k in tr_classes if k not in names]
print('产物 class 数: %d ；原 jar 里有同名条目: %d ；产物独有(将丢弃): %d'
      % (len(tr_classes), len(tr_classes)-len(only_in_tr), len(only_in_tr)))
if only_in_tr[:10]:
    print('  丢弃样例:', only_in_tr[:10])

replaced = 0
if os.path.exists(out_jar):
    os.remove(out_jar)
zo = zipfile.ZipFile(out_jar, 'w', zipfile.ZIP_DEFLATED)
for info in zf.infolist():
    data = zf.read(info.filename)
    if info.filename in tr_classes:
        data = tr_classes[info.filename]
        replaced += 1
    # 保留原压缩方式
    zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    zi.compress_type = info.compress_type
    zi.external_attr = info.external_attr
    zo.writestr(zi, data)
zo.close(); zf.close()
print('已写入 %s，替换 %d 个 class' % (out_jar, replaced))
