#!/usr/bin/env bash
# Một lệnh cho một màn: chụp mockup + chụp app + ghép cạnh nhau.
#
#   tools/ui-check/compare.sh SCR-0301 /tmp/ui [show_id] [episode_index] [wait_sec]
#   → /tmp/ui/SCR-0301.mockup.png, SCR-0301.app.png, SCR-0301.compare.png
#
# Lần đầu: `npm --prefix tools/ui-check install`. Bản devDebug phải đã cài trên emulator.
set -euo pipefail

SCR="${1:?mã SCR}"
DIR="${2:?thư mục ảnh ra}"
HERE="$(cd "$(dirname "$0")" && pwd)"

[ -d "$HERE/node_modules/playwright-core" ] || npm --prefix "$HERE" install --silent

mkdir -p "$DIR"
node "$HERE/mockup-shot.mjs" --scr "$SCR" --out "$DIR/$SCR.mockup.png"
"$HERE/device-shot.sh" "$SCR" "$DIR/$SCR.app.png" "${3:-12485}" "${4:-0}" "${5:-3}"
node "$HERE/side-by-side.mjs" --mockup "$DIR/$SCR.mockup.png" --app "$DIR/$SCR.app.png" \
  --out "$DIR/$SCR.compare.png" --title "$SCR"
