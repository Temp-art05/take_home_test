# Luồng mẫu: Post feed (tab Home)

Một luồng chạy thật, đủ mọi tầng — đọc theo thứ tự dưới đây là hiểu cả base. API: `https://dummyjson.com/posts`.

```
HomeScreen ──(lambda)──▶ HomeViewModel ──▶ PostFeedUseCase ──▶ PostRepository (interface, :domain)
                                                                   │
                                              PostRepositoryImpl (:data) ──┬─▶ PostRemoteDataSource ─▶ Ktor
                                                                          └─▶ BookmarkLocalDataSource ─▶ Room
```

| # | Tầng | File | Việc |
|---|---|---|---|
| 1 | domain/model | `domain/.../model/Post.kt`, `Page.kt` | model thuần Kotlin |
| 2 | domain/repository | `domain/.../repository/PostRepository.kt` | interface — `Result` cho lệnh, `Flow` cho stream |
| 3 | domain/usecase | `domain/.../usecase/post/PostFeedUseCase.kt` | rule: kích thước trang, trang kế, khử trùng lặp |
| 4 | data/remote | `data/.../remote/dto/PostDto.kt`, `remote/datasource/PostRemoteDataSource(+Impl).kt` | gọi Ktor, trả DTO |
| 5 | data/local | `data/.../local/{entity,dao,database,datasource}/` | Room: `BookmarkEntity`, `BookmarkDao`, `AppDatabase`, `BookmarkLocalDataSource(+Impl)` |
| 6 | data/mapper | `data/.../mapper/PostMapper.kt` | DTO/Entity ↔ domain (extension function) |
| 7 | data/repository | `data/.../repository/PostRepositoryImpl.kt` | `withCatching` + `mapError { toDomainException() }` |
| 8 | data/di | `data/.../di/DataModule.kt` | bind theo interface, use case là `factory` |
| 9 | presentation | `feature/main/.../presentation/home/` | Contract, ViewModel (hàm tên rõ), Screen + `component/PostItem` |
| 10 | feature/di | `feature/main/.../di/MainModule.kt` | `viewModelOf(::HomeViewModel)` |

Những gì luồng này minh hoạ:
- **Phân trang** `Page<T>(items, hasMore)` — ViewModel không tự so `size >= PAGE_SIZE`.
- **Remote + local cùng phục vụ một màn**: list từ API, trạng thái bookmark stream từ Room.
- **Lỗi**: mất mạng → `DomainException.Network` → `R.string.error_network` → `ErrorState` (load lần đầu)
  hoặc `Toasty.error` (load thêm / bookmark lỗi).
- **Event một lần**: bookmark xong → `HomeSingleEvent.BookmarkToggled` → `Toasty.success`.
- **Không reload khi đổi tab**: load ở `init` của ViewModel, ViewModel scope theo Main.

## Xoá khi làm project thật

- `domain`: `model/Post.kt`, `repository/PostRepository.kt`, `usecase/post/`
- `data`: `remote/dto/PostDto.kt`, `remote/datasource/Post*`, `local/` (giữ `database/AppDatabase.kt` nếu dùng Room,
  thay entity/dao), `mapper/PostMapper.kt`, `repository/PostRepositoryImpl.kt`, các dòng SAMPLE trong `DataModule`
- `data/schemas/` khi đổi entity (hoặc bump version + viết Migration)
- `feature/main/.../presentation/home/` → thay bằng màn thật; string `home_*`
- `BASE_URL` trong `app/build.gradle.kts`

Không dùng Room: bỏ plugin `ksp` + `androidx.room`, `room {}`, `bundles.room`, `room.compiler` khỏi
`data/build.gradle.kts`, và hai dòng `apply false` ở `build.gradle.kts` gốc.
