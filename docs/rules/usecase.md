# Use case

Class thường ở `:domain/usecase/<area>/` (vd. `usecase/profile/ProfileUseCase.kt`), nhận repository qua constructor. **Không base class, không
truyền dispatcher.** Trả stream thì trả thẳng `Flow<T>`, không bọc `Result`.

## Gộp theo nghiệp vụ

Một class = **một năng lực nghiệp vụ**, không phải một class cho mỗi hàm repository.

```kotlin
// ĐÚNG — một năng lực, nhiều hàm tên rõ
class ProfileUseCase(
    private val profileRepository: ProfileRepository,
    private val accountRepository: AccountRepository,
) {
    fun observe(): Flow<Profile> = profileRepository.observeProfile()
    suspend fun refresh(): Result<Unit> = profileRepository.refresh()
    suspend fun updateName(name: String): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.length !in NAME_LENGTH) return Result.failure(DomainException.InvalidName())
        return profileRepository.updateName(trimmed)
    }
}

// SAI — wrapper một dòng, mỗi hàm repo một class
class ObserveProfileUseCase(private val repo: ProfileRepository) {
    operator fun invoke() = repo.observeProfile()
}
class RefreshProfileUseCase(...)
class UpdateProfileNameUseCase(...)
```

- `operator fun invoke` chỉ khi class đúng là **một hành động duy nhất** (`LogoutUseCase`).
- Đọc/observe thuần là **một hàm** của use case nghiệp vụ cần nó, không tách class riêng.
- Đặt tên theo năng lực: `ProfileUseCase`, `CheckoutUseCase`, `AccountStateUseCase`.

## Rule nằm ở đâu

Mọi quyết định nằm ở use case: gate, đếm, ngưỡng, lịch, phân loại lỗi, kích thước trang, trang kế,
khử trùng lặp, chuỗi gọi nhiều repository, gom/trộn nhiều stream.

ViewModel chỉ còn: map sang state, string res, navigation, analytics, job/lifecycle, nhịp UI
(debounce, animation).

- Paging trả `Page<T>(items, hasMore)`; ViewModel không so `count >= PAGE_SIZE`.
- Remote Config: `:domain` không thấy `:core:analytics` ⇒ ViewModel đọc RC rồi **truyền giá trị** vào use case.
- Constructor quá nhiều tham số là dấu hiệu use case chưa gộp đúng — **cấm** facade gói dependency
  (`XxxDeps`) để né.

## Khi nào ViewModel inject thẳng, không qua use case

Chỉ ba nhóm, ngoài ra phải có use case:

| Nhóm | Ví dụ |
|---|---|
| Cổng hạ tầng (SDK sau interface) | `Analytics`, `BillingGateway` |
| Preference **hiển thị** của chính màn đó | ngôn ngữ, theme, cờ đã xem hướng dẫn |
| Nguồn cấu hình | `RemoteConfigSource`, `AppConfig` |

Dễ nhầm: nằm trong DataStore không có nghĩa là "preference". Counter, cap, số lượt… phục vụ rule
nghiệp vụ là **dữ liệu nghiệp vụ** ⇒ đi qua use case.

## DI

Khai `factory { ProfileUseCase(get(), get()) }` trong `DataModule`. Dùng `single` chỉ khi use case
giữ state in-memory có chủ đích (cache, single-flight) — ghi rõ lý do bằng comment.
