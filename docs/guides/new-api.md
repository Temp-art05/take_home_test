# Thêm một API mới — từng bước

Ví dụ: màn Profile cần `GET /me` và `PUT /me`. Làm theo đúng thứ tự, mỗi bước một file. Đối chiếu
luồng mẫu `sample-flow.md` khi cần.

## 1. Domain (`:domain`)

```kotlin
// model/Profile.kt
data class Profile(val id: String, val name: String, val avatarUrl: String?)

// repository/ProfileRepository.kt
interface ProfileRepository {
    suspend fun getProfile(): Result<Profile>
    suspend fun updateName(name: String): Result<Profile>
}

// usecase/profile/ProfileUseCase.kt — gộp theo năng lực, rule nằm ở đây
class ProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend fun load(): Result<Profile> = profileRepository.getProfile()

    suspend fun rename(name: String): Result<Profile> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(DomainException.Unknown())   // hoặc subclass riêng
        return profileRepository.updateName(trimmed)
    }
}
```

## 2. Data (`:data`)

```kotlin
// remote/dto/ProfileDto.kt — tả đúng JSON của BE
@Serializable
data class ProfileDto(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class UpdateProfileRequest(@SerialName("name") val name: String)

// remote/datasource/ProfileRemoteDataSource.kt
interface ProfileRemoteDataSource {
    suspend fun getProfile(): ProfileDto
    suspend fun updateName(name: String): ProfileDto
}

// remote/datasource/ProfileRemoteDataSourceImpl.kt
class ProfileRemoteDataSourceImpl(private val httpClient: HttpClient) : ProfileRemoteDataSource {
    override suspend fun getProfile(): ProfileDto = httpClient.get("me").body()
    override suspend fun updateName(name: String): ProfileDto =
        httpClient.put("me") { setBody(UpdateProfileRequest(name)) }.body()
}

// mapper/ProfileMapper.kt
fun ProfileDto.toDomain() = Profile(id = id, name = name, avatarUrl = avatarUrl)

// repository/ProfileRepositoryImpl.kt
class ProfileRepositoryImpl(private val remote: ProfileRemoteDataSource) : ProfileRepository {
    override suspend fun getProfile() = withCatching { remote.getProfile().toDomain() }
        .mapError { it.toDomainException() }

    override suspend fun updateName(name: String) = withCatching { remote.updateName(name).toDomain() }
        .mapError { it.toDomainException() }
}

// di/DataModule.kt
single<ProfileRemoteDataSource> { ProfileRemoteDataSourceImpl(get()) }
single<ProfileRepository> { ProfileRepositoryImpl(get()) }
factory { ProfileUseCase(get()) }
```

BE trả envelope `{ statusCode, message, data }` → body là `BaseResponse<ProfileDto>`, rồi `.data`.

## 3. Presentation (`:feature:*`)

```kotlin
// presentation/profile/ProfileContract.kt
data class ProfileViewState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    @param:StringRes val errorRes: Int? = null,
) : ViewState

sealed interface ProfileIntent : ViewIntent {
    data object RetryClick : ProfileIntent
    data class RenameConfirm(val name: String) : ProfileIntent
}

sealed interface ProfileSingleEvent : SingleEvent {
    data class ShowError(@param:StringRes val messageRes: Int) : ProfileSingleEvent
}

// presentation/profile/ProfileViewModel.kt
class ProfileViewModel(private val profileUseCase: ProfileUseCase) :
    BaseViewModel<ProfileIntent, ProfileViewState, ProfileSingleEvent>() {

    init { load() }

    override fun initState() = ProfileViewState()

    override fun processIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.RetryClick -> load()
            is ProfileIntent.RenameConfirm -> rename(intent.name)
        }
    }

    private fun rename(name: String) {
        viewModelScope.launch {
            profileUseCase.rename(name)
                .onSuccess { updateState { copy(profile = it) } }
                .onFailure { sendEvent(ProfileSingleEvent.ShowError(it.toErrorRes())) }
        }
    }

    private fun load() {
        updateState { copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            profileUseCase.load()
                .onSuccess { updateState { copy(isLoading = false, profile = it) } }
                .onFailure { updateState { copy(isLoading = false, errorRes = it.toErrorRes()) } }
        }
    }
}

// presentation/profile/ProfileScreen.kt
ProfileContent(
    viewState = viewState,
    onIntent = { intent -> viewModel.processIntent(intent) },
)

// di/MainModule.kt
viewModelOf(::ProfileViewModel)
```

## Checklist

- [ ] Repository + DataSource đều có interface, bind bằng interface trong DI
- [ ] Use case gộp vào năng lực có sẵn nếu cùng nghiệp vụ (đừng tạo `GetProfileUseCase` một dòng)
- [ ] Không try/catch ngoài repository; lỗi cần phân nhánh → subclass `DomainException`
- [ ] ViewModel có hàm tên rõ; Screen truyền lambda, không `::`
- [ ] String mới có cả `values/` và `values-vi/`
- [ ] Build `assembleProdRelease` (R8) trước khi merge
