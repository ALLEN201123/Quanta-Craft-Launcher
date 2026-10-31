# -*- coding: utf-8 -*-
"""给 1.5.9 补建公告 id=109（三语），插到 announcements 最前面。"""
import io
import json

P = r"D:\QCL-build\QCL\src\main\assets\announcement.json"

zh_cn = """【1.5.9：公告的「展开」按钮修好了，首次启动不再铺满半屏】

这一版修的，是 1.5.8 我自己改出来的问题。

一、公告「展开 / 收起」按钮，现在真的能看见了

1.5.8 里我为了修「公告把半屏铺满」，写了一句「撤销折叠」，结果改过头了：

· 每次显示公告，代码都把正文设成「不限行数」；
· 然后**把展开按钮直接隐藏掉**。

于是 XML 里明明写好的「展开」按钮，你根本看不见，更点不到；
而且每翻一页还会再隐藏一次 —— 这个功能等于被我废掉了。

现在改回正规做法：
· 公告正文**默认只显示 6 行**，末尾省略号；
· 下面的按钮**可见**，显示「Show more」；
· 点一下 → 显示全文，按钮文字变「Show less」；
· 再点一下 → 收回 6 行；
· 换一条公告 / 翻页 → 自动回到折叠态，按钮文字也跟着复位。

二、修「首次启动时公告铺满半个屏幕」

原因是我把顺序写反了：
· 先设置正文（这一步会触发一次重新排版）；
· 再做入场动画；
· 最后才让公告栏可见。
结果公告栏是**带着「还没折叠的满高正文」**进场的，首帧就撑满了半屏。

现在改成：
· 先把折叠状态和按钮文字/可见性全部落定；
· 再请求一次排版，让高度按 6 行算准；
· 最后才做入场动画。
首帧就是「6 行 + Show more」，不会再抖一下。

三、本版其它

· 1.5.8 的内容全部保留（26.3 能启动、MobileGlues 内置、日志查看器等）。
· 26.3 的真根因与修法见 1.5.8 的说明：
  26.3 默认仍走 OpenGL → 经 libmobileglues.so 的 glGetString 返回 NULL
  → 调用方 strlen(NULL) → 原生 SIGSEGV；
  QCL 从 26.3 起自动追加官方启动参数 --graphicsBackend vulkan。

四、已知问题（不变）

· 无 Vulkan 的设备，26.3 可能仍无法启动（设备能力，不是启动器缺陷）。
· 点两次 ESC 视角偶发偏移 —— 需要一份可复现的日志才能定位。
· 远古版本颜色异常 —— 第三方转译路线共有的天花板，彻底解决依赖自研渲染器。

（本版为 1.5.9，versionCode 361。）"""

zh_tw = """【1.5.9：公告的「展開」按鈕修好了，首次啟動不再鋪滿半屏】

這一版修的，是 1.5.8 我自己改出來的問題。

一、公告「展開 / 收起」按鈕，現在真的能看見了

1.5.8 裡我為了修「公告把半屏鋪滿」，寫了一句「撤銷折疊」，結果改過頭了：

· 每次顯示公告，程式碼都把正文設成「不限行數」；
· 然後**把展開按鈕直接隱藏掉**。

於是 XML 裡明明寫好的「展開」按鈕，你根本看不見，更點不到；
而且每翻一頁還會再隱藏一次 —— 這個功能等於被我廢掉了。

現在改回正規做法：
· 公告正文**預設只顯示 6 行**，末尾省略號；
· 下面的按鈕**可見**，顯示「Show more」；
· 點一下 → 顯示全文，按鈕文字變「Show less」；
· 再點一下 → 收回 6 行；
· 換一條公告 / 翻頁 → 自動回到折疊態，按鈕文字也跟著復位。

二、修「首次啟動時公告鋪滿半個螢幕」

原因是我把順序寫反了：
· 先設定正文（這一步會觸發一次重新排版）；
· 再做入場動畫；
· 最後才讓公告欄可見。
結果公告欄是**帶著「還沒折疊的滿高正文」**進場的，首幀就撐滿了半屏。

現在改成：
· 先把折疊狀態和按鈕文字/可見性全部落定；
· 再請求一次排版，讓高度按 6 行算準；
· 最後才做入場動畫。
首幀就是「6 行 + Show more」，不會再抖一下。

三、本版其它

· 1.5.8 的內容全部保留（26.3 能啟動、MobileGlues 內建、日誌檢視器等）。
· 26.3 的真根因與修法見 1.5.8 的說明：
  26.3 預設仍走 OpenGL → 經 libmobileglues.so 的 glGetString 回傳 NULL
  → 呼叫方 strlen(NULL) → 原生 SIGSEGV；
  QCL 從 26.3 起自動追加官方啟動參數 --graphicsBackend vulkan。

四、已知問題（不變）

· 無 Vulkan 的裝置，26.3 可能仍無法啟動（裝置能力，不是啟動器缺陷）。
· 點兩次 ESC 視角偶發偏移 —— 需要一份可重現的日誌才能定位。
· 遠古版本顏色異常 —— 第三方轉譯路線共有的天花板，徹底解決依賴自研渲染器。

（本版為 1.5.9，versionCode 361。）"""

en_us = """[1.5.9: the announcement "Show more" button works again, and the announcement no
longer fills half the screen on first launch]

This release fixes a problem I introduced myself in 1.5.8.

1. The "Show more / Show less" button is actually visible now

In 1.5.8, to fix "the announcement fills half the screen", I wrote "undo the
collapse" - and overdid it:

- every time an announcement was shown, the body was set to "unlimited lines";
- and then the Show more button was hidden outright.

So the button that was properly declared in the XML was invisible and
unclickable, and it was hidden again on every page change - the feature was
effectively dead.

Now it works the proper way:
- the announcement body defaults to 6 lines with a trailing ellipsis;
- the button below is visible and reads "Show more";
- tap it -> full text, button becomes "Show less";
- tap again -> back to 6 lines;
- switching announcements / paging -> returns to the collapsed state and the
  button label resets with it.

2. Fixed "the announcement fills half the screen on first launch"

The order was wrong:
- set the text first (which triggers a re-layout);
- then run the entrance animation;
- and only then make the announcement visible.
The panel therefore entered while still carrying the full-height, uncollapsed
body, filling the screen on the very first frame.

Now:
- the collapsed state plus button label/visibility are settled first;
- a layout pass runs so the height is computed for 6 lines;
- the entrance animation runs last.
The first frame is "6 lines + Show more", with no flicker.

3. Other notes

- everything from 1.5.8 is kept (26.3 launches, MobileGlues built in, the
  last-launch-log viewer, etc.);
- the real root cause and fix for 26.3 are described in the 1.5.8 notes:
  26.3 still defaults to OpenGL -> the GL path goes through libmobileglues.so's
  glGetString, which returns NULL -> the caller does strlen(NULL) -> a native
  SIGSEGV; QCL appends the official launch argument --graphicsBackend vulkan
  from 26.3 onward.

4. Known issues (unchanged)

- on devices without Vulkan, 26.3 may still fail to start (a device limitation,
  not a launcher defect);
- the view occasionally drifts after pressing ESC twice - needs a reproducible
  log to pin down;
- ancient-version colour anomalies are a ceiling shared by every third-party
  translation stack; the real fix depends on our own renderer.

(This release is 1.5.9, versionCode 361.)"""

ann = {
    "id": 109,
    "significant": True,
    "outdated": False,
    "minVersion": 0,
    "maxVersion": 2147483647,
    "specificLang": [],
    "title": [
        {"lang": "zh_CN", "text": "QCL 1.5.9：公告「展开」按钮修好了 + 首次显示不再铺满半屏"},
        {"lang": "zh_TW", "text": "QCL 1.5.9：公告「展開」按鈕修好了 + 首次顯示不再鋪滿半屏"},
        {"lang": "en_US", "text": "QCL 1.5.9: announcement Show-more button fixed + no more half-screen fill"},
    ],
    "date": "2026-10-31",
    "content": [
        {"lang": "zh_CN", "text": zh_cn},
        {"lang": "zh_TW", "text": zh_tw},
        {"lang": "en_US", "text": en_us},
    ],
}

with io.open(P, encoding="utf-8") as f:
    d = json.load(f)

# 已有 id=109 就先删掉（幂等）
d["announcements"] = [a for a in d["announcements"] if a["id"] != 109]
d["announcements"].insert(0, ann)

with io.open(P, "w", encoding="utf-8", newline="\n") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)

print("已写入 id=109")
# 校验
d2 = json.load(io.open(P, encoding="utf-8"))
ids = [a["id"] for a in d2["announcements"]]
print("公告 id 列表:", ids)
print("id 是否有重复:", "有" if len(ids) != len(set(ids)) else "无")
print("总公告数:", len(ids))
