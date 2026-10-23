# Tiện ích có sẵn (`:core:utils`)

## Toasty — toast có icon + màu

```kotlin
Toasty.success(context, R.string.saved).show()
Toasty.error(context, event.messageRes, allowQueue = false).show()
Toasty.info(context, "Đã copy").show()
Toasty.warning(context, R.string.low_storage, Toast.LENGTH_LONG).show()
Toasty.normal(context, R.string.hello).show()
```

- Hiện ở mép trên màn hình, nền bo tròn, icon tô màu theo loại (`res/values/colors.xml` của
  `:core:utils` — đổi màu brand ở đó).
- `allowQueue = false`: huỷ toast đang hiện để toast mới hiện ngay — dùng cho lỗi lặp lại / bấm liên tục.
- Gọi từ `CollectAsEffect` của screen khi nhận `SingleEvent` — ViewModel **không** gọi Toasty (không giữ Context).
- Từ Android 11, toast custom chỉ hiện khi app ở foreground. Báo khi app ở nền → `NotificationHelper`.

Mẫu thật: `feature/main/.../presentation/home/HomeScreen.kt`.

## Context extension

| Hàm | Việc |
|---|---|
| `context.openUrl(url)` | mở URL, trả `false` nếu không có app mở được |
| `context.shareText(text)` | bảng chia sẻ hệ thống |
| `context.copyToClipboard(text)` | copy (Android 13+ hệ thống tự hiện xác nhận) |
| `context.findActivity()` | lấy Activity từ ContextWrapper |

## Khác

| Thứ | Ở đâu | Việc |
|---|---|---|
| `NetworkUtils.isOnline(context)` | `network/` | có internet đã xác thực (không chỉ "đã nối wifi") |
| `NotificationHelper` | `notification/` | channel mặc định + hiện notification; `:app` truyền Intent + icon |
| `Flow.throttleFirst` / `select` / `catchAndLog` | `extension/FlowExt.kt` | chống double-click, chọn một phần state, log lỗi stream |
| `bridge/` | — | interface gọi chức năng chéo feature, xem `docs/rules/architecture.md` |

Thêm tiện ích mới: không UI, không nghiệp vụ, dùng ≥ 2 nơi → `:core:utils`. Có Compose UI → `:core:design-system`
hoặc `:core:ui-foundation`.
