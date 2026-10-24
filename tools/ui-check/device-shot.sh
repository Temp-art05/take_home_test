#!/usr/bin/env bash
# Mở thẳng một màn trên emulator theo mã SCR (bản dev) rồi chụp màn hình.
#
#   tools/ui-check/device-shot.sh SCR-0301 /tmp/ui/SCR-0301.app.png [show_id] [episode_index] [wait_sec]
#
# Tuỳ chọn trước khi chụp (cho sheet không phải destination, vd. SCR-0303/0304):
#   TAP_TEXT="Eps"        chạm tâm node đầu tiên có text/content-desc đúng chuỗi đó
#   LONGPRESS_TEXT="1×"   nhấn giữ node đó
#   TAP_AT="975 1915" / LONGPRESS_AT="975 1708"  như trên nhưng theo toạ độ px — dùng khi màn có
#                         animation vô hạn (video, spinner): uiautomator dump chờ "idle" mãi nên hết 15s là bỏ
#   AFTER_WAIT=2          giây chờ sau thao tác (mặc định 2)
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
  --es debug_screen "$SCR" --es show_id "$SHOW_ID" --ei episode_index "$EP_INDEX" ${EXTRA_ARGS:-} >/dev/null
sleep "$WAIT_SEC"

# Tìm node theo text/content-desc trong uiautomator dump → in "x y" của tâm bounds.
node_center() {
  perl -e 'alarm shift; exec @ARGV' 15 "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null || return 0
  "$ADB" exec-out cat /sdcard/ui.xml | python3 -c '
import re, sys
want = sys.argv[1]
xml = sys.stdin.read()
for node in re.findall(r"<node [^>]*>", xml):
    attrs = dict(re.findall(r"([\w-]+)=\"([^\"]*)\"", node))
    if want in (attrs.get("text"), attrs.get("content-desc")):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attrs["bounds"]))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1"
}

if [[ -n "${TAP_TEXT:-}${LONGPRESS_TEXT:-}${TAP_AT:-}${LONGPRESS_AT:-}" ]]; then
  TARGET="${TAP_TEXT:-${LONGPRESS_TEXT:-}}"
  if [[ -n "${TAP_AT:-}${LONGPRESS_AT:-}" ]]; then
    POS="${TAP_AT:-$LONGPRESS_AT}"
  else
    POS="$(node_center "$TARGET")"
  fi
  if [[ -z "$POS" ]]; then
    echo "BLOCKED: không thấy node \"$TARGET\" trên màn" >&2
    exit 12
  fi
  read -r X Y <<<"$POS"
  if [[ -n "${TAP_TEXT:-}${TAP_AT:-}" ]]; then
    "$ADB" shell input tap "$X" "$Y"
  else
    "$ADB" shell input swipe "$X" "$Y" "$X" "$Y" 800
  fi
  sleep "${AFTER_WAIT:-2}"
fi

if "$ADB" logcat -d -b crash | grep -q "$PKG"; then
  echo "CRASH khi mở $SCR:" >&2
  "$ADB" logcat -d -b crash | tail -40 >&2
  exit 11
fi

"$ADB" exec-out screencap -p > "$OUT"
echo "$SCR → $OUT"
