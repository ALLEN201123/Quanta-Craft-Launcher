import zipfile, re, os, struct

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

# ★★★ 核心：分三类，只把「玩家真能看见的」挑出来
#  A) 翻译 key（options.xxx / selectWorld.xxx / deathScreen.xxx / potion.prefix.xxx / mob.xxx
#     / disconnect.xxx / multiplayer.xxx / texturePack.xxx / item.xxx / gui.xxx / menu.xxx …）
#     这些是给 Language 表查的，翻它没用（要翻的是表里的值），但**它们出现在界面上就是英文**，
#     所以真正要靠"官方 lang 机制"或"把整条 key 也换成中文"来解决。
KEY = re.compile(r'^(options|selectWorld|selectServer|deathScreen|disconnect|multiplayer|'
                 r'texturePack|potion|mob|item|gui|menu|createWorld|chat|key|'
                 r'advancements|stats|effect|entity|enchantment|itemcrafting|'
                 r'merchant|command|book|tile|color|music|jukebox|subtitle|'
                 r'soundCategory|particle|structure|worldGen|select)\.')

#  B) 普通界面文案（含空格的自然语言 + 简单名词）
#  C) 要排除的：日志/异常/存档错误
LOG = re.compile(
    r'\bException\b|\berror\b|\bmethod \b|\bclass \b|stack ?trace|expected |failed to|'
    r'couldn.t|could not|unable to|invalid |isn.t|wasn.t|doesn.t|don.t|can.t |'
    r'\bnull\b|index out|array index|illegal|abort|throw|assert|coding failure|'
    r'\bjson\b|\bnode \b|\btag \b|\bnbt\b|chunk|download|server|socket|'
    r'-- |generated|java:|lwjgl:|minecraft:|os:|opengl:|pixel format|'
    r'report|crash|driver|videocard|billing|realms|permission denied|no such|'
    r'unknown |corrupt|duplicate|expected|\bnot found\b|out of memory|'
    r'Copyright|Happy birthday|Happy new year|Merry X-mas|version |Java:',
    re.I)

def classify(s):
    if not s or not (1 < len(s) < 100): return None
    if re.search(r'[\u4e00-\u9fff]', s): return None
    if not re.search(r'[A-Za-z]', s): return None
    if re.search(r'[%/\\@#$<>{}();\[\]=]', s): return None
    if s.endswith(('.class','.java','.png','.ogg','.txt')): return None
    if re.search(r'\bException\b|\bRuntimeException\b|\bError\b', s): return None
    if re.fullmatch(r'[A-Fa-f0-9]{3,}', s): return None
    if LOG.search(s): return None
    if KEY.match(s): return 'KEY'
    if ' ' in s: return 'UI'
    if re.fullmatch(r'[A-Za-z][A-Za-z \-\'&]{1,40}', s) and len(s) < 42:
        return 'WORD'
    return None

mapping = {}
for line in open('en2zh3.tsv', encoding='utf-8'):
    line=line.rstrip('\n')
    if not line or line.startswith('#'): continue
    p=line.split('\t')
    if len(p)>=2: mapping[p[0]]=p[1]

for jar in sorted(os.listdir('legacy_jar')):
    zf=zipfile.ZipFile(os.path.join('legacy_jar',jar))
    keys=[]; ui=[]; words=[]
    for name in zf.namelist():
        if not name.endswith('.class') or name.startswith(TP): continue
        try: d=zf.read(name)
        except: continue
        for s in set(cs(d)):
            if s in mapping: continue
            c = classify(s)
            if c=='KEY': keys.append(s)
            elif c=='UI': ui.append(s)
            elif c=='WORD': words.append(s)
    print(f"##### {jar} #####")
    print(f"  [KEY 翻译key] {len(keys)} 条")
    print(f"  [UI  界面句] {len(ui)} 条")
    print(f"  [WORD 单词]  {len(words)} 条")
    globals().setdefault('ALL',{})[jar]=(keys,ui,words)
    print()

# 汇总 KEY（这类最该处理：整个 key 集合就是界面文案全集）
import itertools
allkeys = {}
for jar,(k,u,w) in ALL.items():
    for x in k: allkeys.setdefault(x,set()).add(jar)
print("==== 全部版本的翻译 key（界面文案全集，需官方 lang 或整体替换） ====")
for x in sorted(allkeys):
    print(f"  {x}   ({','.join(sorted(v.replace('.jar','') for v in allkeys[x]))})")
