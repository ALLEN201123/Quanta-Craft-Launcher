# cacio FileDialog null 保护补丁

## 为什么需要

indev / infdev 等远古版本的「保存世界」「读取世界」用的是 AWT `FileDialog`。
`cacio-shared-1.10-SNAPSHOT.jar` 里的 `sun.awt.peer.cacio.CacioFileDialogPeer`
在 `postInitSwingComponent()` 里把 `FileDialog.getFile()` / `getDirectory()` /
`getFilenameFilter()` 的返回值**直接 `new File(...)` / 传给过滤器**，
而游戏创建 FileDialog 时通常**不会先调这些方法** → 返回 `null` → NPE
→ **对话框建不起来**（点保存/读取毫无反应，日志里反复刷同一个 NPE）。

## 怎么修的

**不重编译**（`java.awt.peer.FileDialogPeer` 是内部 API，本地没有 Java 8 JDK 可用），
用 ASM 在三个方法开头插入 null 判断，再把改好的类放进
`assets/app_runtime/caciocavallo/ResConfHack.jar`
（它在 `-Xbootclasspath/p` 里且排在 cacio-shared 之前 → 会优先于原版被加载）。

- `setFile(null)` → 直接返回（保留 JFileChooser 默认选中项）
- `setDirectory(null)` → 回退到 `System.getProperty("user.home")`（QCL 的 home，可写）
- `setFilenameFilter(null)` → 不设置过滤器（否则用户浏览目录时 NPE）

## 重新生成补丁

```bash
javac -encoding UTF-8 -cp "<asm.jar>;<asm-tree.jar>" -d out tools/cacio_patch/PatchCacio.java
java  -cp "out;<asm.jar>;<asm-tree.jar>" PatchCacio \
      QCL/src/main/assets/app_runtime/caciocavallo/cacio-shared-1.10-SNAPSHOT.jar \
      QCL/src/main/assets/app_runtime/caciocavallo/ResConfHack.jar
```
（执行前先备份 `ResConfHack.jar`；补丁是幂等的，重跑会覆盖同名类。）
