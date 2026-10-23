# lib-ads (vendored)

Mã nguồn của `lib-ads-android` (`ads-core` + `ads-admob`), chép vào repo thay vì khai Maven.

| | |
|---|---|
| Nguồn | `lib-ads-android`, nhánh `develop`, tag **1.1.1** (`19c8075`, 07/2026) |
| Module | `:lib-ads:core` (`com.github.dinhn.ads.core`), `:lib-ads:admob` (`com.github.dinhn.ads.admob`) |
| App dùng qua | `:core:ads` — feature không import lib trực tiếp |

## Vì sao vendor

- Lib phát hành qua **GitHub Packages private** — cần PAT `read:packages` của org. Đề yêu cầu build được từ clone sạch, không bước tay, không nhúng token.
- Build script gốc dùng AGP 8 + `kotlin-android` + `kotlinOptions`; AGP 9.2 (built-in Kotlin) không nhận. Build file ở đây viết lại bằng convention plugin của repo.

## Khác bản gốc

- Bỏ `ads-applovin`, `ads-firebase`, 4 mediation adapter (Meta/AppLovin/Pangle/Mintegral) và repo Maven Pangle/Mintegral — bài test chỉ dùng test ID AdMob.
- `play-services-ads` giữ **23.6.0** như bản lib đã kiểm.
- Sửa trong code (đánh dấu `// FlowDrama:`):
  - `AdMobAdapter.initialize`: `MobileAds.initialize` chạy trên thread nền (bản gốc gọi trên main lúc `Application.onCreate`).
  - `AdsSdk.registerInterstitial/registerRewarded`: thêm tham số `showLoading` (mặc định `true` như bản gốc). `:core:ads` truyền `false` — không gắn overlay đen lên `decorView` (D6), chỉ show ad đã preload.
  - `NativeAdBinder`: bỏ layout XML mặc định (`res/layout/ads_native_*.xml` không chép) — đề cấm XML layout; `:core:ads` dựng `NativeAdView` bằng code.

## Nâng phiên bản

Chép lại `src/main` của hai module từ tag mới, giữ lại các chỗ `// FlowDrama:`, chạy `./gradlew :app:assembleProdRelease`
(keep rule của lib nằm ở `consumer-rules.pro` — adapter được nạp bằng reflection).
