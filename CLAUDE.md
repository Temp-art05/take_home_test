# CLAUDE.md

Hướng dẫn cho Claude Code khi làm việc trong repo này.

Android app template: Clean Architecture, 100% Compose, 9 module (tổ chức theo `docs/rules/architecture.md`). Checklist dựng project mới: `TODO.md`.

## Rule bắt buộc — đọc trước khi viết code

| File | Nội dung |
|---|---|
| [`docs/rules/architecture.md`](docs/rules/architecture.md) | module, chiều phụ thuộc, **khi nào được tạo module mới** |
| [`docs/rules/usecase.md`](docs/rules/usecase.md) | use case **một hành động một class + `invoke`** (Notica), rule nằm ở đâu, khi nào VM inject thẳng |
| [`docs/rules/data-layer.md`](docs/rules/data-layer.md) | repository + datasource **có interface/Impl**, mapper, DTO, network, Room |
| [`docs/rules/presentation.md`](docs/rules/presentation.md) | ViewModel/Screen, **MVI: mọi hành động qua `processIntent`**, navigation, Main 5 tab |
| [`docs/rules/error-handling.md`](docs/rules/error-handling.md) | `Result`, chuỗi `ApiException → DomainException → @StringRes` |

Hướng dẫn & mẫu:

| File | Nội dung |
|---|---|
| [`docs/guides/sample-flow.md`](docs/guides/sample-flow.md) | luồng mẫu post feed (tab Home) đủ tầng — đọc để hiểu base; cách xoá |
| [`docs/guides/new-api.md`](docs/guides/new-api.md) | thêm một API mới từng bước (domain → data → presentation) |
| [`docs/guides/utils.md`](docs/guides/utils.md) | Toasty, Context ext, NetworkUtils, NotificationHelper |

## Build

```bash
./gradlew :app:assembleDevDebug          # dev flavor, debug
./gradlew :app:assembleProdRelease       # prod flavor, release (R8 + shrinkResources)
./gradlew lintDevDebug
```

Sau khi sửa build script hoặc nâng thư viện, **luôn chạy cả `assembleProdRelease`**: AGP 9 bật
`android.r8.strictFullModeForKeepRules` mặc định nên release có thể gãy dù debug xanh.

Base không kèm test. Thêm test thì khai lib vào catalog + `testImplementation` ở module cần.

## Toolchain

AGP 9.2 · Gradle 9.8 · JDK 17 · Kotlin 2.4 · compileSdk/targetSdk 37 · minSdk 24 (đề bài yêu cầu).

AGP ghim ở 9.2.1 vì Android Studio Quail 1 (2026.1.1) chỉ sync được tới bản này (Quail 2 → 9.3.x,
9.4.x cần Studio mới hơn). Nâng AGP phải đối chiếu bản Studio của cả team. Kiểm nhanh:
`version.properties` trong `Android Studio.app/Contents/plugins/android/lib/libagp-version.jar`
→ `lastStableBuildVersion`.

AGP 9 dùng **built-in Kotlin**: module Android **không** apply `org.jetbrains.kotlin.android`
(chỉ `:domain` dùng `kotlin-jvm`). Không có block `kotlinOptions`.

JDK 17 do toolchain quy định; máy chưa có thì Gradle tự tải (plugin foojay trong cả hai
`settings.gradle.kts`). Hai cảnh báo deprecated còn lại (`Configuration.setVisible`, Project làm
dependency notation) đến từ nội bộ AGP.

R8: module library không minify — keep rule của library phải nằm trong `consumer-rules.pro`.
Ktor / kotlinx-serialization / Navigation tự kèm rule, base không cần rule riêng.

## build-logic (convention plugins)

| Plugin | Dùng cho |
|---|---|
| `base.android.application` | `:app` |
| `base.android.library` | mọi module library |
| `base.android.compose` | module có Compose (áp thêm lên 2 cái trên) |
| `base.android.feature` | `:feature:*` — library + compose + serialization + dependency chung |
| `base.kotlin.jvm` | `:domain` |

`compileSdk`/`targetSdk`/`minSdk`/Java version khai **một chỗ**: `build-logic/.../ProjectExt.kt` →
`object AndroidConfig`. Đừng copy-paste cấu hình vào build file module.

## BuildConfig isolation

BuildConfig (`BASE_URL`, `IS_DEBUG`, flavor, version) **chỉ tồn tại trong `:app`**. `ConfigModule` map
nó vào `AppConfig` (`:domain/config`) và bind làm Koin single; module khác inject `get<AppConfig>()`.
Thêm field build-time: sửa `AppConfig` + `ConfigModule.kt`.

Flavor `dev` có `applicationIdSuffix = ".dev"`. Signing release đọc từ `local.properties`
(`RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`);
thiếu thì APK unsigned.

## DI (Koin)

`FlowDramaApp.onCreate()` gọi `startKoin` với `configModule`, `appModule` (`AppScope` — coroutine scope
theo process), `analyticsModule(isDebug)`, `remoteConfigModule`, `dataModule`, `adsModule`, và một module cho mỗi
feature (`mainModule`). Sau đó `AdsInitializer.start()` (MobileAds init nền nhờ `OPTIMIZE_INITIALIZATION`, gọi `register()` của `InterstitialAdManager`/`RewardAdManager`/`NativeAdManager`, áp `ads_config`). Feature tự khai DI của mình; ViewModel inject qua `koinViewModel()`.

SDK analytics bên thứ ba thêm ở `:app` bằng `single { ExtraAnalyticsPlugins(listOf(...)) }`.

## Lib ads

Lib của tác giả `com.github.dinhn:ads-core|ads-admob|ads-firebase` kéo từ GitHub Packages (`lurdsoft/lib-ads-android`),
cred `gpr.user`/`gpr.key` trong `gradle.properties` — PAT `read:packages` **commit có chủ đích** (tác giả lib cho phép),
đừng gỡ. `ads-admob` kéo theo adapter mediation → cần repo Pangle + Mintegral trong `settings.gradle.kts`. Chỉ `:core:ads`
phụ thuộc lib; native dựng `NativeAdView` bằng code (đề cấm XML layout), không dùng `AdsNative(layoutId)`.

## Firebase

Base **không kèm `google-services.json`** và vẫn chạy được. Mọi thứ đụng Firebase nằm ở
`:core:analytics`, sau interface, mặc định no-op:

| Interface | No-op | Firebase impl |
|---|---|---|
| `Analytics` | `NoOpAnalytics` | `AnalyticsImpl` + `FirebaseAnalyticsPlugin` |
| `CrashReporter` | `NoOpCrashReporter` | `FirebaseCrashReporter` |
| `RemoteConfigSource` | `NoOpRemoteConfigSource` | `RemoteConfigManager` |

Guard duy nhất: `isFirebaseAvailable(context)`. `app/google-services.json` đã commit (project
`take-home-test-c5997`), 2 plugin Firebase đã bật. Firebase chỉ đăng ký **một** app `com.takehome.drama`
nên flavor `dev` không có `applicationIdSuffix`/`versionNameSuffix` — đừng thêm lại.

Remote config: fetch single-flight, seed default của `RemoteKeys` vào Firebase, nạp giá trị phiên trước
từ DataStore lúc khởi tạo; debug build fetch interval = 0.

## Theme, i18n, ảnh

- `AppTheme` mặc định **dark** (design Sunset Flow), light là bộ token tạm. Chủ ý **không** bật dynamic color.
- Ngôn ngữ UI: 10 locale ra mắt `en, hi, ta, id, ar, fr, es, pt-BR, tr, de` (`LaunchLanguages` ở `:domain`; `ar` là RTL).
  Thư mục: `values-hi/ta/in/ar/fr/es/pt-rBR/tr/de` (Indonesia dùng mã cũ `in`). Thêm locale sửa **cả 3 chỗ**:
  `res/xml/locales_config.xml`, `localeFilters` trong `app/build.gradle.kts`, enum `AppLocale` (`:core:ui-foundation`).
- **Thêm/sửa string là phải dịch luôn đủ 9 ngôn ngữ còn lại** trong cùng thay đổi — không để `en` một mình. Chuỗi không dịch
  (tên thương hiệu, mã) đánh `translatable="false"`. Lint `MissingTranslation` (error) chặn nếu sót.
- Ảnh qua `AppAsyncImage`; `appImageLoader`: memory cache theo % RAM, disk cache 100 MB TTL 7 ngày.
