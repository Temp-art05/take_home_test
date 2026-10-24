# Kiến trúc & tổ chức thư mục

Clean Architecture: `presentation → domain ← data`. `:domain` là tâm, không phụ thuộc gì.

## Module (9)

| Module | Package gốc | Chứa |
|---|---|---|
| `:app` | `com.takehome.drama` | `BaseApp`, `MainActivity`, `navigation/AppNavGraph`, `di/`, `fcm/` |
| `:domain` | `com.takehome.drama.domain` | Kotlin thuần: `model/`, `repository/` (interface), `usecase/<area>/`, `error/`, `config/` |
| `:data` | `com.takehome.drama.data` | `remote/`, `local/`, `mapper/`, `repository/` (impl), `util/`, `di/` |
| `:core:utils` | `com.takehome.drama.core.utils` | `toast/` (Toasty), `extension/`, `network/`, `notification/`, `bridge/` — tiện ích không UI-Compose, không nghiệp vụ |
| `:core:navigation` | `com.takehome.drama.core.navigation` | `core/` (ScreenProvider, NavigationRequest, throttle), `destination/`, `deeplink/`, `launcher/` |
| `:core:analytics` | `com.takehome.drama.core.analytics` | `Analytics`, `plugin/`, `crash/`, `remoteconfig/`, `util/` — mọi thứ Firebase, sau interface |
| `:core:ui-foundation` | `com.takehome.drama.core.ui.foundation` | `vm/` (BaseViewModel), `ext/` (CollectAsEffect), `error/` (→ string res), `locale/` |
| `:core:design-system` | `com.takehome.drama.core.designsystem` | `theme/`, `component/`, `ext/` |
| `:feature:main` | `com.takehome.drama.feature.main` | Main 3 tab + Settings |
| `:core:ads` | `com.takehome.drama.core.ads` | `interstitial/InterstitialAdManager`, `reward/RewardAdManager`, `nativead/NativeAdManager` (feature chỉ dùng 3 manager này, như HomeMorph), `AdsInitializer` (init + `register()`), `AdsLogToggle`, `config/` (parse `ads_config`, impl `AdsConfigRepository`), `nativead/`, `sdk/` (facade lib). Chỉ module này phụ thuộc lib ads (`com.github.dinhn:ads-*`, GitHub Packages) |

```
app          → mọi module
feature:*    → core:*, domain               (KHÔNG data, KHÔNG feature khác)
data         → domain
core:ui-foundation → domain
core:ads     → domain, core:analytics, core:design-system (+ lib ads com.github.dinhn:ads-*)
               (ngoại lệ: impl AdsConfigRepository cần Remote Config; card native cần token màu)
core:utils, core:navigation, core:analytics, core:design-system → (không module nào)
domain       → (không gì — Kotlin/JVM thuần)
```

Quy tắc cứng:
- `:domain` không có một dòng Android nào.
- `:feature:*` không import lẫn nhau — giao tiếp qua **Destination** (điều hướng) hoặc **Bridge** (gọi chức năng).
- Feature không phụ thuộc `:data`; chỉ thấy interface ở `:domain`.
- `:core:*` không phụ thuộc feature, không chứa nghiệp vụ.

## Cây thư mục chuẩn

### Feature

```
feature/foo/src/main/java/com/takehome/drama/feature/foo/
  di/                 FooModule.kt — viewModelOf(...), bind bridge impl
  navigation/         FooScreenProvider.kt (Destination @Serializable khai bên trong)
  presentation/
    <screen>/
      FooContract.kt  FooViewState + FooSingleEvent
      FooViewModel.kt
      FooScreen.kt    FooScreen + FooContent (stateless, có preview)
      component/      composable riêng của màn này
      model/          UI model / enum của màn (MainTab, ...)
  bridge/             FooBridgeImpl.kt — khi feature khác cần gọi chức năng của foo
  domain/  data/      CHỈ khi dữ liệu này feature khác không dùng (xem dưới)
```

Mẫu thật: `:feature:main` (`presentation/main/{component,model}`, `presentation/home/component`, `navigation/`, `di/`).
Luồng đủ tầng domain → data → presentation: `docs/guides/sample-flow.md`.

### Domain

```
domain/
  model/              Profile.kt, Page.kt
  repository/         ProfileRepository.kt (interface)
  usecase/
    profile/          ProfileUseCase.kt
    checkout/         CheckoutUseCase.kt, ApplyCouponUseCase.kt
  error/              DomainException.kt
  config/             AppConfig.kt
```

Use case **nhóm theo thư mục nghiệp vụ** (`usecase/<area>/`) và **gộp theo năng lực** — xem `usecase.md`.

### Data

Xem `data-layer.md`: `remote/{http,error,dto,log,datasource}`, `local/{dao,database,entity,datasource}`,
`mapper/`, `repository/`, `util/`, `di/`.

## Data riêng của feature

Dữ liệu **chỉ một feature dùng** (vd. lịch sử chat của feature chat) được đặt ngay trong feature:

```
feature/chat/.../feature/chat/
  domain/{model,repository,usecase}
  data/{remote,local,mapper,repository}
  di/ChatModule.kt    khai cả datasource/repository/use case của chat
```

Khi feature thứ hai cần dữ liệu đó → chuyển lên `:domain` + `:data`. Đừng để hai feature cùng đọc
một bảng Room nằm trong một feature.

## Bridge — gọi chức năng của feature khác

Feature A cần **dùng chức năng** (không chỉ mở màn) của feature B, ví dụ nhúng một composable hoặc
chạy một tác vụ: khai interface ở `:core:utils/bridge`, impl trong feature B, bind ở DI của B.
A inject interface, không import B.

```kotlin
// :core:utils — com.takehome.drama.core.utils.bridge (bridge có @Composable → thêm id("base.android.compose") vào core/utils/build.gradle.kts)
interface ChatBridge {
    @Composable
    fun ChatPanel(conversationId: String, modifier: Modifier)

    suspend fun clearHistory()
}

// :feature:chat — com.takehome.drama.feature.chat.bridge
class ChatBridgeImpl(
    private val chatHistory: ChatHistoryUseCase,
) : ChatBridge {
    @Composable
    override fun ChatPanel(conversationId: String, modifier: Modifier) {
        ChatPanelSection(conversationId = conversationId, modifier = modifier)
    }

    override suspend fun clearHistory() {
        chatHistory.clear()
    }
}

// :feature:chat — di/ChatModule.kt
val chatModule = module {
    single<ChatBridge> { ChatBridgeImpl(get()) }
}

// :feature:settings — ViewModel/Screen chỉ biết interface
class SettingsViewModel(private val chatBridge: ChatBridge) : BaseViewModel<...>() {
    override fun processIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.ClearChatClick -> viewModelScope.launch { chatBridge.clearHistory() }
        }
    }
}
```

Chỉ **mở màn** của feature khác thì không cần bridge: nhận `Destination` qua constructor của
`ScreenProvider`, `:app` nối hai đầu trong `AppNavGraph`.

## Deeplink & launcher (khi app có)

Đặt ở `:core:navigation`:

```
core/navigation/
  deeplink/   DeepLinkConstants (scheme/host), DeepLinkType.fromUri(uri) → loại deeplink
  launcher/   LauncherAction (sealed, @Serializable) — hành động khi mở app từ notification/intent
              LauncherActionHandler.from(intent) — đọc extra / payload FCM ra LauncherAction
```

`MainActivity` đọc `LauncherAction` từ intent (chỉ khi `savedInstanceState == null` và không mở
từ Recents), map sang `Destination` trong `:app`, rồi điều hướng sau `NavigationThrottle.awaitWindow()`.
`:core:navigation` chỉ định nghĩa kiểu dữ liệu — không biết destination cụ thể của feature.

## DI

Mỗi module tự khai DI ở `di/`: `dataModule` (`:data`), `analyticsModule` + `remoteConfigModule`
(`:core:analytics`), `mainModule` (`:feature:main/di`). `:app/di` chỉ có `configModule` (BuildConfig →
`AppConfig`) và `appModule` (`AppScope`). `BaseApp` gom tất cả trong `startKoin`.

## Khi nào tạo module mới

Mỗi module tốn thời gian sync/config Gradle — **không tạo module chỉ chứa một hai file**.

- Feature mới → package trong `:feature:main` trước. Tách `:feature:foo` khi ≥ 3 màn hoặc có data riêng
  hoặc có người làm riêng.
- Room → `:data/local` (đã cấu hình sẵn), không tạo `:core:database`.
- Tiện ích không UI dùng chung → `:core:utils`; thứ thuộc presentation → `:core:ui-foundation`;
  component giao diện → `:core:design-system`.
- SDK bên thứ ba nhiều code (billing, ads) → mới cân nhắc `:core:<tên>`, luôn sau interface.

Thêm module: build file chỉ cần `plugins { id("base.android.library") }` + `namespace` + dependency
riêng. Cấu hình chung ở `build-logic/convention`.
