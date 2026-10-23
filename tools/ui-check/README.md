# ui-check — so màn app với mockup

Công cụ cho vòng "dựng UI → chụp → so với design". Mockup HTML của đề là spec; mỗi màn trong đó
có mã `SCR-xxxx`.

```bash
npm --prefix tools/ui-check install          # một lần (playwright-core, chạy bằng Chrome đã cài)
./gradlew :app:installDevDebug               # bản dev mới mở thẳng được màn theo mã SCR
tools/ui-check/compare.sh SCR-0301 /tmp/ui   # → SCR-0301.mockup.png / .app.png / .compare.png
```

| File | Việc |
|---|---|
| `mockup-shot.mjs` | Chụp khung điện thoại của một mã SCR trong mockup, rộng 1080px (`--list` liệt kê các màn) |
| `device-shot.sh` | Mở thẳng màn trên emulator bằng extra `debug_screen` rồi `adb screencap`. Sheet: `TAP_TEXT=Eps` / `LONGPRESS_TEXT=1×` chạm/nhấn giữ node trước khi chụp |
| `side-by-side.mjs` | Ghép mockup và app thành một ảnh hai cột cùng chiều rộng |
| `compare.sh` | Ba bước trên trong một lệnh |

Mở thẳng màn chỉ có ở flavor **dev** (`DebugScreenLauncher` trong `:app`). Sheet (0303/0304) và
trạng thái hệ thống (06xx) không phải destination — mở màn cha rồi thao tác tiếp bằng `adb shell input`.

Mockup mặc định đọc từ Knowledge store của project; đổi bằng `MOCKUP_HTML=/đường/dẫn.html`.
