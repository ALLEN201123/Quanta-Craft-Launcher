# -*- coding: utf-8 -*-
"""生成 LegacyChinesePack 用的 asset jar：只含「字符串常量池真的变了」的 class。
   规则与既有 120 个 asset jar 一致（1.0 那个只有 52 个，而翻译产物有 99 个）——
   没被替换任何字符串的 class 不需要进包（避免无谓增大 APK）。"""
import zipfile, os, sys, struct

def strs(data):
    if data[:4] != b'\xca\xfe\xba\xbe': return []
    off=8; cnt=struct.unpack('>H',data[off:off+2])[0]; off+=2
    i=1; out=[]
    while i<cnt:
        t=data[off]
        if t==1:
            ln=struct.unpack('>H',data[off+1:off+3])[0]
            out.append(data[off+3:off+3+ln]); off+=3+ln
        elif t in (7,8,16,19,20): off+=3
        elif t==15: off+=4
        elif t in (3,4,9,10,11,12,17,18): off+=5
        elif t in (5,6): off+=9; i+=1
        else: break
        i+=1
    return out

orig_jar, tr_dir, out_jar = sys.argv[1], sys.argv[2], sys.argv[3]
zf = zipfile.ZipFile(orig_jar)
changed = {}
for root, dirs, files in os.walk(tr_dir):
    for fn in files:
        if not fn.endswith('.class'): continue
        full = os.path.join(root, fn)
        rel = os.path.relpath(full, tr_dir).replace('\\','/')
        try: od = zf.read(rel)
        except KeyError: continue
        nd = open(full,'rb').read()
        if od == nd: continue
        # 必须至少有一个字符串常量不同（防止纯字节重排被误判）
        os_, ns_ = set(strs(od)), set(strs(nd))
        if os_ == ns_: continue
        changed[rel] = nd

if os.path.exists(out_jar): os.remove(out_jar)
zo = zipfile.ZipFile(out_jar, 'w', zipfile.ZIP_DEFLATED)
for rel, data in sorted(changed.items()):
    zo.writestr(rel, data)
zo.close()
print('%s: 变化 class %d 个（产物 %d 个）' % (os.path.basename(out_jar), len(changed),
      sum(1 for r,d,fs in os.walk(tr_dir) for f in fs if f.endswith('.class'))))
