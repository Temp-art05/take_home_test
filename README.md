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

## Chưa có trong base

- **Test** — chủ ý không kèm; thêm lib test vào catalog khi project cần.
- **CI**, **detekt / ktlint**, **baseline profile**.
