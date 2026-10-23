package com.base.app.core.ui.foundation.vm

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Base ViewModel: một [StateFlow] state + một kênh event một lần.
 *
 * Hành động của user là **hàm public tên rõ** trên ViewModel (`onRefreshClick()`,
 * `selectTab(tab)`), không phải một hàm dispatch chung kiểu `processIntent(intent)` —
 * Find Usages của từng hành động mới trỏ đúng chỗ gọi.
 *
 * ```
 * class HomeViewModel(
 *     private val homeFeed: HomeFeedUseCase,
 * ) : BaseViewModel<HomeViewState, HomeSingleEvent>() {
 *
 *     override fun initState() = HomeViewState()
 *
 *     init { load() }
 *
 *     fun onRetryClick() = load()
 *
 *     private fun load() {
 *         updateState { copy(isLoading = true) }
 *         viewModelScope.launch {
 *             homeFeed.load()
 *                 .onSuccess { updateState { copy(isLoading = false, items = it) } }
 *                 .onFailure { updateState { copy(isLoading = false, errorRes = it.toErrorRes()) } }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel<S : ViewState, E : SingleEvent> : ViewModel() {

    /**
     * `by lazy` chứ không khởi tạo thẳng: thân lớp cha chạy trước khi tham số constructor của
     * lớp con được gán, nên `initState()` đọc dependency sẽ nhận `null` và crash lúc Koin dựng VM.
     */
    private val _viewState: MutableStateFlow<S> by lazy { MutableStateFlow(initState()) }
    val viewState: StateFlow<S> by lazy { _viewState.asStateFlow() }

    private val eventChannel = Channel<E>(Channel.UNLIMITED)

    /** Event một lần, chỉ một collector nhận. Collect bằng `CollectAsEffect` ở screen. */
    val singleEvent: Flow<E> = eventChannel.receiveAsFlow()

    /** State ban đầu của màn hình. */
    protected abstract fun initState(): S

    protected val currentState: S
        get() = _viewState.value

    protected fun updateState(reducer: S.() -> S) {
        _viewState.update { it.reducer() }
    }

    /** Channel UNLIMITED nên `trySend` không bao giờ treo — không cần launch coroutine. */
    protected fun sendEvent(event: E) {
        eventChannel.trySend(event).onFailure { throwable ->
            Timber.e(throwable, "Failed to send event: $event")
        }
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        eventChannel.close()
    }
}
