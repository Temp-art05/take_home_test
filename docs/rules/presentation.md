# Presentation (ViewModel + Compose)

## Bộ file mỗi màn hình

Đặt trong `presentation/<screen>/` của feature; composable riêng của màn ở `component/`, UI model/enum ở `model/` (xem `architecture.md`).

| File | Nội dung |
|---|---|
| `FooContract.kt` | `data class FooViewState : ViewState`, `sealed interface FooSingleEvent : SingleEvent` |
| `FooViewModel.kt` | `BaseViewModel<FooViewState, FooSingleEvent>` |
| `FooScreen.kt` | `FooScreen` (nối ViewModel) + `FooContent` (stateless, có preview) |
| `navigation/FooScreenProvider.kt` | chỉ màn là một destination: `@Serializable` destination + factory composable |

## Không dispatch intent

**Cấm** kiểu `processIntent(intent)` / `onIntent = viewModel::processIntent` / `sealed interface FooIntent`.
Hành động của user là **hàm public tên rõ** trên ViewModel; `FooContent` nhận **callback tên rõ**;
`FooScreen` truyền **lambda**, không dùng method reference `::`.

```kotlin
// ViewModel
fun onRetryClick() = load()
fun selectTab(tab: MainTab) { ... }

// Screen
FooContent(
    viewState = viewState,
    onRetryClick = { viewModel.onRetryClick() },
    onTabSelected = { tab -> viewModel.selectTab(tab) },
)

// Content
@Composable
private fun FooContent(
    viewState: FooViewState,
    onRetryClick: () -> Unit,
    onTabSelected: (MainTab) -> Unit,
)
```

Lý do: Find Usages của từng hành động trỏ đúng một chỗ gọi; đọc signature của Content là biết màn
có những hành động gì.

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

## Navigation

- Destination là `@Serializable data object/class : Destination`, khai **bên trong** `ScreenProvider`.
- Đăng ký ở `AppNavGraph`: `applyDestination(FooScreenProvider())`.
- Điều hướng: `onNavigate(NavigationRequest.NavigateTo(...))` (`NavigateToAndClearBackStack`,
  `PopBackStack`, `NavigateUp`, `PopTo`). Có throttle chống double-tap; điều hướng không do user bấm
  (deeplink, hết phiên) gọi `NavigationThrottle.awaitWindow()` trước.

## Main 3 tab

`HorizontalPager` làm container giữ tab, không phải carousel: `userScrollEnabled = false`, đổi tab bằng
`scrollToPage` (không trượt qua tab giữa), `beyondViewportPageCount = tabs.lastIndex` (tab đã mở không
bị dispose → giữ scroll/state, không load lại), tab chưa mở chưa compose (`visitedTabs`). ViewModel tab
lấy bằng `koinViewModel()` → scope theo back stack entry của Main.

Bẫy: tab ẩn **vẫn RESUMED** — việc chỉ nên chạy khi tab hiện (polling, video) thì gate theo `selectedTab`.

## DI

ViewModel khai trong module Koin của feature (`viewModelOf(::FooViewModel)`), không ở `:app`.
