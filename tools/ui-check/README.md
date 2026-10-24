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

Mở thẳng màn chỉ có ở flavor **dev** (`DebugScreenLauncher` trong `:app`). Sheet (0303/0304) không phải
destination — mở màn cha rồi thao tác tiếp bằng `adb shell input`. Trạng thái hệ thống mở màn cha kèm điều kiện
dữ liệu: `SCR-0601` (Home, catalogue treo 60 s), `SCR-0602` (Saved sau khi xoá Library), `SCR-0603` (Home, lần tải
đầu lỗi mạng — Try again ra dữ liệu), `SCR-0605` (Browse, bấm ＋).

Extra thêm qua `EXTRA_ARGS` (truyền nguyên cho `am start`):

```bash
EXTRA_ARGS="--ez debug_seed_library true" tools/ui-check/compare.sh SCR-0402 /tmp/ui   # lịch sử + Saved như mockup
EXTRA_ARGS="--ez debug_clear_library true" tools/ui-check/compare.sh SCR-0401 /tmp/ui  # Library rỗng
EXTRA_ARGS="--es debug_theme light" tools/ui-check/compare.sh SCR-0501 /tmp/ui         # theme Light
```

Còn có `--el debug_catalog_delay_ms N`, `--ei debug_offline_calls N`, `--ei debug_hour 21`, `--es debug_query ceo`,
`--es debug_ads_config '{…}'` (xem KDoc `DebugScreenLauncher`).

Mockup mặc định đọc từ Knowledge store của project; đổi bằng `MOCKUP_HTML=/đường/dẫn.html`.
