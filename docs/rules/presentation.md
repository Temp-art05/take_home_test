# Presentation (ViewModel + Compose)

## Bộ file mỗi màn hình

Đặt trong `presentation/<screen>/` của feature; composable riêng của màn ở `component/`, UI model/enum ở `model/` (xem `architecture.md`).

| File | Nội dung |
|---|---|
| `FooContract.kt` | `data class FooViewState : ViewState`, `sealed interface FooIntent : ViewIntent`, `sealed interface FooSingleEvent : SingleEvent` |
| `FooViewModel.kt` | `BaseViewModel<FooIntent, FooViewState, FooSingleEvent>` |
| `FooScreen.kt` | `FooScreen` (nối ViewModel) + `FooContent` (stateless, có preview) |
| `navigation/FooScreenProvider.kt` | chỉ màn là một destination: `@Serializable` destination + factory composable |

## MVI — mọi hành động đi qua `processIntent`

Theo pattern của Notica. Mỗi màn có `sealed interface FooIntent : ViewIntent`; ViewModel override
`processIntent(intent)` → `when (intent)` → gọi hàm **private** xử lý. Không có hàm public nào khác
cho hành động của user.

```kotlin
// Contract
sealed interface FooIntent : ViewIntent {
    data object RetryClick : FooIntent
    data class SelectTab(val tab: MainTab) : FooIntent
}

// ViewModel
override fun processIntent(intent: FooIntent) {
    when (intent) {
        FooIntent.RetryClick -> load()
        is FooIntent.SelectTab -> selectTab(intent.tab)
    }
}

// Screen
FooContent(
    viewState = viewState,
    onIntent = { intent -> viewModel.processIntent(intent) },
)

// Content — stateless, chỉ biết gửi intent
@Composable
private fun FooContent(
    viewState: FooViewState,
    onIntent: (FooIntent) -> Unit,
)
```

Lý do: một cửa vào duy nhất cho mọi hành động — đọc `FooIntent` là biết màn có những hành động gì,
log/analytics hành động gắn được ở một chỗ, và luồng dữ liệu một chiều (Intent → State/Event).

## ViewModel

- Override `initState()` (state dựng lười nên được đọc dependency constructor).
- Load dữ liệu ban đầu ở `init {}`, **không** ở `LaunchedEffect` của screen — compose lại (đổi tab,
  xoay máy) không được kéo theo load lại.
- `updateState { copy(...) }` để đổi state; `sendEvent(...)` cho điều hướng/toast.
- **Loading là field của state** (`isLoading`), không phải nhánh của `Result`.
- Text lỗi trong state là `@StringRes Int` (`it.toErrorRes()`), không là `String`.
- Không giữ `Context`, không import `:data`.

## Screen

- `viewModel.viewState.collectAsStateWithLifecycle()` — không `collectAsState()`.
- `viewModel.singleEvent.CollectAsEffect { event -> ... }` (`:core:ui-foundation`) — không `LaunchedEffect { collect }` trần.
- Màu: `MaterialTheme.colorScheme.*`, không hardcode `AppColors.*` / `Color(0x...)`.
- Ảnh: `AppAsyncImage`, không gọi thẳng Coil.
- String thuộc module nào nằm ở module đó.

## Màn có video (`:core:media`)

- ViewModel **sở hữu** `EpisodeFeedPlayer` (tạo qua `EpisodeFeedPlayerFactory`, `release()` ở `onCleared`) — xoay
  máy không dựng lại player, không rebuffer. Screen chỉ vẽ `VideoSurface(viewModel.player)`.
- Luồng vuốt dọc (Player, Reels): **một** player gắn vào trang đã settle, trang lân cận hiện poster; preload
  do player lo theo `MediaProfile` (máy yếu preload ít hơn). Pager báo `PageSettled` → VM; VM đổi tập từ chỗ
  khác thì phát `ScrollToEpisode` → pager cuộn. Vị trí phát (tick 500ms) chỉ thanh track đọc.
- Lifecycle là intent: `LifecycleEventEffect(ON_START/ON_STOP)` → `ScreenStarted` / `ScreenStopped`; VM quyết
  định pause + lưu vị trí, và chỉ phát lại nếu trước khi vào nền đang phát.
- Thứ cần sống qua process death (tập đang xem, tốc độ, quality) ghi vào `SavedStateHandle`; vị trí phát lấy
  lại từ Room. Test VM bằng player giả, không cần ExoPlayer.

## Navigation

- Destination là `@Serializable data object/class : Destination`, khai **bên trong** `ScreenProvider`.
- Đăng ký ở `AppNavGraph`: `applyDestination(FooScreenProvider())`.
- Điều hướng: `onNavigate(NavigationRequest.NavigateTo(...))` (`NavigateToAndClearBackStack`,
  `PopBackStack`, `NavigateUp`, `PopTo`). Có throttle chống double-tap; điều hướng không do user bấm
  (deeplink, hết phiên) gọi `NavigationThrottle.awaitWindow()` trước.

## Main 5 tab (Home, Reels, Browse, Library, Settings)

`HorizontalPager` làm container giữ tab, không phải carousel: `userScrollEnabled = false`, đổi tab bằng
`scrollToPage` (không trượt qua tab giữa), `beyondViewportPageCount = tabs.lastIndex` (tab đã mở không
bị dispose → giữ scroll/state, không load lại), tab chưa mở chưa compose (`visitedTabs`). ViewModel tab
lấy bằng `koinViewModel()` → scope theo back stack entry của Main.

Bẫy: tab ẩn **vẫn RESUMED** — việc chỉ nên chạy khi tab hiện (polling, video) thì gate theo `selectedTab`.

## DI

ViewModel khai trong module Koin của feature (`viewModelOf(::FooViewModel)`), không ở `:app`.
