#!/bin/sh
# QCL 测试用 adb 包装：自动重连（MuMu 的 127.0.0.1:7555 频繁 offline）
# 用法: ./adbq.sh shell "ls /sdcard"
export PATH="/usr/bin:/bin:/c/Windows/System32:$PATH"
ADB="/d/Android/Sdk/platform-tools/adb.exe"
DEV="127.0.0.1:7555"

ensure() {
    for i in 1 2 3 4 5 6 7 8 9 10; do
        ST=$("$ADB" -s "$DEV" get-state 2>/dev/null)
        if [ "$ST" = "device" ]; then return 0; fi
        "$ADB" connect "$DEV" >/dev/null 2>&1
        sleep 1
    done
    return 1
}

ensure || { echo "ADB_CONNECT_FAILED"; exit 1; }
OUT=$("$ADB" -s "$DEV" "$@" 2>&1)
RC=$?
# 掉线则重连重试一次
case "$OUT" in
    *"not found"*|*"device offline"*)
        ensure
        OUT=$("$ADB" -s "$DEV" "$@" 2>&1)
        RC=$?
        ;;
esac
printf '%s\n' "$OUT"
exit $RC
