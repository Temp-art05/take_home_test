# Base App

Android app template: Clean Architecture + Jetpack Compose, 9 module, convention plugins.
Clone về là chạy được ngay — kể cả khi chưa cấu hình Firebase.

## Tech stack

| Mảng | Thư viện |
|---|---|
| Build | AGP 9.2 · Gradle 9.8 · JDK 17 · Kotlin 2.4 · KSP 2.3 · convention plugins |
| SDK | compileSdk / targetSdk 37 · minSdk 26 |
| UI | Compose (BOM 2026.09) + Material 3 · Coil 3 |
| Navigation | Navigation Compose, type-safe `@Serializable` destination |
| DI | Koin 4 |
| Network | Ktor 3 (OkHttp, content negotiation, logging) |
| Firebase | Analytics · Crashlytics · Remote Config · Messaging (sau interface, mặc định no-op) |
| Database | Room 2.8 + KSP |
| Logging | Timber |

## Cấu trúc module

```
build-logic/          convention plugins (included build)

:app                  FlowDramaApp, MainActivity, navigation/AppNavGraph, di/, fcm/
:feature:main         Main 3 tab (Home / Feature / Profile) + Settings
                        di/ navigation/ presentation/<màn>/{component,model}
:core:utils           toast/ (Toasty), extension/, network/, notification/, bridge/
:core:navigation      ScreenProvider, NavigationRequest, throttle, Destination
:core:analytics       Analytics / CrashReporter / RemoteConfigSource + impl Firebase
:core:ui-foundation   BaseViewModel, CollectAsEffect, AppLocale, lỗi → string res
:core:design-system   theme light/dark, typography, component, AppAsyncImage
:domain               Kotlin thuần — model, repository interface, usecase/<area>/, DomainException
:data                 Ktor client, ApiException, datasource, repository impl, DataModule
```

```
app                 → mọi module
feature:*           → core:*, domain   (không data, không feature khác)
data                → domain
core:ui-foundation  → domain
core:* còn lại      → (không module nào)
domain              → (không gì — Kotlin/JVM thuần)
```

Feature giao tiếp với nhau qua `Destination` (điều hướng) hoặc **Bridge** (interface ở
`:core:utils/bridge`, impl trong feature) — mẫu code ở [`docs/rules/architecture.md`](docs/rules/architecture.md).

Chỉ tạo module mới khi nó thật sự lớn — xem [`docs/rules/architecture.md`](docs/rules/architecture.md).

## Quy ước

Rule code nằm ở [`docs/rules/`](docs/rules) — đọc trước khi viết code:

- **Use case gộp theo nghiệp vụ**, không mỗi hàm repository một class.
- **Repository và DataSource đều có interface + Impl.**
- **Không dispatch intent**: ViewModel có hàm tên rõ, Screen truyền callback tên rõ.
- Lỗi: `ApiException → DomainException → @StringRes`, ViewModel không import `:data`.

**Màn Main 3 tab** — `HorizontalPager` làm container: tắt vuốt, đổi tab tức thì (không trượt qua tab
giữa), tab đã mở được giữ sống nên không mất scroll/state và không load lại, tab chưa mở chưa compose.

## Luồng mẫu

Tab **Home** là luồng mẫu chạy thật đủ tầng: API `dummyjson.com/posts` (Ktor) → repository → use case → ViewModel,
phân trang, bookmark lưu Room, lỗi báo bằng `ErrorState` / `Toasty`. Đọc [`docs/guides/sample-flow.md`](docs/guides/sample-flow.md);
thêm API mới theo [`docs/guides/new-api.md`](docs/guides/new-api.md).

## Bắt đầu

**Yêu cầu:** Android Studio 2026.1.1 (Quail 1)+ · Android SDK 37 · JDK 17 (thiếu thì Gradle tự tải)

```bash
git clone <repo-url> my-new-app
cd my-new-app
./gradlew :app:assembleDevDebug
```

Không cần `google-services.json` — Firebase tự rơi về no-op. Sau đó theo **[TODO.md](TODO.md)**.

## Build

```bash
./gradlew :app:assembleDevDebug        # dev, debug
./gradlew :app:assembleProdRelease     # prod, release (R8 + shrinkResources)
./gradlew lintDevDebug
```

## Ads + Remote Config

> Nháp Phase 5 — README nộp bài hoàn thiện ở Phase 7.

Ads dùng lib ads của tác giả (`com.github.dinhn:ads-core|ads-admob|ads-firebase` 1.1.1), kéo từ GitHub Packages
`maven.pkg.github.com/lurdsoft/lib-ads-android` — tích hợp theo cách của project thật dùng lib (HomeMorph). Token đọc package
(PAT classic, chỉ scope `read:packages`) **commit có chủ đích** trong `gradle.properties` (`gpr.user`/`gpr.key`, ghi đè được bằng
env `GITHUB_ACTOR`/`GITHUB_TOKEN`) để clone sạch build được không bước tay. `ads-admob` kéo theo adapter mediation (AppLovin,
Meta, Mintegral, Pangle) nên `settings.gradle.kts` có thêm repo Pangle + Mintegral. Cấu trúc như HomeMorph: mỗi format một manager ở `:core:ads` — `InterstitialAdManager`, `RewardAdManager`, `NativeAdManager` — với `enum AdType` (vị trí + ad unit id), `register()`, `isEnabled`, `isReady`, `preload`, `show…`; `AdsInitializer` (≈ `AppAds`) init lib và gọi `register()`. Feature chỉ dùng các manager này.
App id và ad unit id đều là **test ID** của Google (`AndroidManifest.xml`, `core/ads/.../AdUnits.kt`).

### Placement

| id | Format | Vị trí | Vì sao |
|---|---|---|---|
| `interstitial_onboarding` | Interstitial | Hết onboarding, trước Home | Ranh giới luồng tự nhiên, user chưa xem dở gì. Load từ lúc mở onboarding; lúc kết thúc **chỉ hiện nếu đã sẵn**, chưa sẵn thì vào Home ngay — không bắt chờ. Tối đa 1 lần/phiên. |
| `native_home` | Native | Home, sau "Pick up your flow", trước các rail | Hoà vào feed, không có video phát ở Home. Ô chỉ xuất hiện khi ad đã load — tắt / lỗi / offline thì **không có ô nào** (không khung trống). Card dựng bằng code theo token Sunset Flow, badge "Ad", CTA không gradient. |
| `rewarded_bonus` | Rewarded | Mở một tập khoá (chạm ô khoá ở Detail → sheet "Watch ad"; trang khoá trong Player → nút "Watch an ad to unlock") | Opt-in rõ: chỉ hiện khi user bấm nút. Design tự đề xuất "rewarded ads to open locked episodes". Xem hết → tập mở vĩnh viễn (DataStore), đóng sớm → vẫn khoá. Tắt placement → chạm tập khoá ra toast như cũ. |

Không ad nào khi video đang phát: Home/Detail không phát video, trang khoá của Player đã dừng và VM `pause()` lần nữa
trước khi show. Không làm app-open (đề không yêu cầu, dễ làm phiền khi user ra vào app). Mọi lời gọi SDK bọc try/catch — lỗi = "không có ad", app
đi tiếp. Interstitial / rewarded dùng overlay loading của lib (chỉ show ad đã sẵn nên overlay chỉ nháy). Không làm UMP/consent.

Tracking: lib tự bắn sang Firebase (`trackWithFirebase`) — `ad_impression`, `paid_ad_impression`, `paid_ad_impression_value`,
`paid_ad_impression_<tier>` (tier theo quốc gia, `autoGeoTier`), `track_ad_request`, `track_ad_matched_request`,
`event_user_click_ads` và các mốc doanh thu. Bản debug log thêm mọi sự kiện qua Timber tag `AppAdsEvents`.

### Log ads (tap logo Splash 7 lần)

Như HomeMorph: chạm logo ở Splash **7 lần**, mỗi lần cách nhau ≤ 3 s → bật/tắt log của lib (Logcat tag `FOR_TEST_ADS`:
`[PRE-LOAD]`, `[LOADED]`, `[FAILED]`, `[SHOWED]`, `[PAID]`…), toast "Ad logs on/off". Bản debug luôn cho phép (và log
bật sẵn); bản release chỉ khi Remote Config `enable_log_ads = true`. Không lưu bền — mở lại app về mặc định.

### Kill switch `ads_config`

Tham số Remote Config kiểu JSON, đúng hình dạng đề (`enable`, `disabled_versions`, `placements.<id>.enable`), thêm
`interval_between_interstitial` (giây giữa hai lần interstitial, default 20 — như HomeMorph, nhưng áp thật bằng cách đăng ký
lại placement; HomeMorph chỉ đổi default nên giá trị trên Console không có tác dụng).
Thứ tự (`AdsPolicy`, có unit test):

1. `enable == false` → tắt hết
2. `versionName` ∈ `disabled_versions` (khớp chuỗi **chính xác** — `"1.0"`, `"1.0.0 "` không chặn `1.0.0`) → tắt hết
3. `placements.<id>.enable == false` → tắt riêng placement đó
4. placement không khai → default của code (bật)

Kill switch rộng đứng trước cấu hình hẹp: tắt khẩn cấp không bị một placement còn ghi `true` vô hiệu hoá.
JSON parse **khoan dung theo field** — gõ sai kiểu một placement không làm hỏng các field khác; `"false"` dạng chuỗi được nhận.
Chưa từng fetch / offline → default nhúng sẵn (mọi placement bật; load lỗi thì như không có ad).

Áp config: request ad đầu tiên chờ Remote Config áp xong (fetch xong hoặc hết 3 s — cùng ngưỡng Splash), sau đó vẫn
quan sát nên fetch về muộn cũng có hiệu lực ngay; trước mỗi lần load/show policy được kiểm lại. Màn đang mở cũng đổi theo:
native biến mất / hiện ra; Player ẩn hoặc hiện nút "Watch an ad to unlock" (tắt → không bao giờ báo "No ad available");
Detail đang mở sheet mà rewarded bị tắt → đóng sheet, toast "Episode N is locked"; onboarding được bật muộn → load
interstitial lúc đó.

**Test nhanh**: `versionName` hiện ở cuối Settings (`FlowDrama v1.0.0 (1)`). Console → `ads_config` →
`{"enable":true,"disabled_versions":["1.0.0"],"placements":{"native_home":{"enable":true}}}` → Publish → vuốt kill app,
mở lại → không ad nào. `minimumFetchInterval`: **0 s ở bản debug**, 3600 s ở release (mặc định SDK 12 h).
Bản dev còn thử được không cần Console:

```
adb shell am start -S -n com.takehome.drama/.MainActivity --es debug_screen SCR-0101 \
    --es debug_ads_config '{"enable":true,"disabled_versions":["1.0.0"],"placements":{"native_home":{"enable":true}}}'
```

## Chưa có trong base

- **Test** — chủ ý không kèm; thêm lib test vào catalog khi project cần.
- **CI**, **detekt / ktlint**, **baseline profile**.
