#!/usr/bin/env bash
# Mở thẳng một màn trên emulator theo mã SCR (bản dev) rồi chụp màn hình.
#
#   tools/ui-check/device-shot.sh SCR-0301 /tmp/ui/SCR-0301.app.png [show_id] [episode_index] [wait_sec]
#
# Cần: đúng một emulator/thiết bị đang chạy (`adb devices`) và bản devDebug đã cài
# (`./gradlew :app:installDevDebug`). Exit khác 0 khi không có máy hoặc app crash — đó là
# BLOCKED/DIFF chứ không phải ảnh để chấm.
set -euo pipefail

SCR="${1:?mã SCR, ví dụ SCR-0301}"
OUT="${2:?đường dẫn ảnh .png}"
SHOW_ID="${3:-12485}"
EP_INDEX="${4:-0}"
WAIT_SEC="${5:-3}"
PKG="com.takehome.drama"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"

if ! "$ADB" devices | awk 'NR>1 && $2=="device"' | grep -q .; then
  echo "BLOCKED: không có emulator/thiết bị nào đang chạy (adb devices rỗng)" >&2
  exit 10
fi

mkdir -p "$(dirname "$OUT")"
"$ADB" logcat -c
"$ADB" shell am start -S -W -n "$PKG/.MainActivity" \
  --es debug_screen "$SCR" --es show_id "$SHOW_ID" --ei episode_index "$EP_INDEX" >/dev/null
sleep "$WAIT_SEC"

if "$ADB" logcat -d -b crash | grep -q "$PKG"; then
  echo "CRASH khi mở $SCR:" >&2
  "$ADB" logcat -d -b crash | tail -40 >&2
  exit 11
fi

"$ADB" exec-out screencap -p > "$OUT"
echo "$SCR → $OUT"
