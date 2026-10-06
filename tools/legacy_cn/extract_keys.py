import zipfile, re, os, struct, json

def cs(data):
    if data[:4] != b'\xca\xfe\xba\xbe': return []
    off=8; cnt=struct.unpack('>H',data[off:off+2])[0]; off+=2
    i=1; out=[]
    while i<cnt:
        t=data[off]
        if t==1:
            ln=struct.unpack('>H',data[off+1:off+3])[0]
            out.append(data[off+3:off+3+ln].decode('utf-8','replace')); off+=3+ln
        elif t in (7,8,16,19,20): off+=3
        elif t==15: off+=4
        elif t in (3,4,9,10,11,12,17,18): off+=5
        elif t in (5,6): off+=9; i+=1
        else: break
        i+=1
    return out

TP=('paulscode/','com/jcraft/','com/google/','org/','io/','it/','javax/','net/','com/mojang/authlib/','org/lwjgl/','com/a/','de/','gnu/','com/fasterxml/','netminecraft/')
PREFIX=('options','selectWorld','selectServer','deathScreen','disconnect','multiplayer',
        'texturePack','potion','mob','item','gui','menu','createWorld','chat','key',
        'advancements','stats','effect','entity','enchantment','itemcrafting',
        'merchant','command','book','tile','color','music','jukebox','subtitle',
        'soundCategory','particle','structure','worldGen','select','gui','tile',
        'ore','rail','minecart','boat','achievement','stat','selector','sound')

allkeys = {}
for jar in sorted(os.listdir('legacy_jar')):
    zf=zipfile.ZipFile(os.path.join('legacy_jar',jar))
    for name in zf.namelist():
        if not name.endswith('.class') or name.startswith(TP): continue
        try: d=zf.read(name)
        except: continue
        for s in set(cs(d)):
            if not s or len(s)>90 or len(s)<4: continue
            if re.search(r'[\u4e00-\u9fff]', s): continue
            head = s.split('.')[0]
            if head in PREFIX and '.' in s and re.match(r'^[a-zA-Z]+(\.[a-zA-Z0-9_]+)+$', s):
                allkeys.setdefault(s, set()).add(jar.replace('.jar',''))

mapping={}
for line in open('en2zh3.tsv',encoding='utf-8'):
    line=line.rstrip('\n')
    if not line or line.startswith('#'): continue
    p=line.split('\t')
    if len(p)>=2: mapping[p[0]]=p[1]

todo = sorted(k for k in allkeys if k not in mapping)
print("全部 key 总数:", len(allkeys), " 尚未在映射表:", len(todo))
with open('legacy_keys.tsv','w',encoding='utf-8') as f:
    f.write("# 远古版本硬编码的翻译 key（字节码常量池里，玩家界面直接显示它们）\n")
    f.write("# 格式：key<TAB>出现该 key 的版本\n")
    for k in todo:
        f.write(f"{k}\t{','.join(sorted(allkeys[k]))}\n")
json.dump({k:sorted(v) for k,v in allkeys.items()}, open('legacy_keys.json','w',encoding='utf-8'), ensure_ascii=False, indent=1)
print("已导出 legacy_keys.tsv / legacy_keys.json")
