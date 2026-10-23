# Checklist tạo project mới từ base

Xem `README.md` cho cấu trúc, `docs/rules/` cho quy ước code.

## 1. Đổi định danh

- [ ] `settings.gradle.kts` → `rootProject.name` (hiện: `BaseApp`)
- [ ] `app/build.gradle.kts` → `applicationId` (hiện: `com.base.app`)
- [ ] Android Studio → Refactor > Rename package `com.base.app` cho mọi module
      (nhớ `namespace` từng module)
- [ ] `app/src/main/res/values/strings.xml` → `app_name`
- [ ] Launcher icon: `app/src/main/res/mipmap-*` + `ic_launcher_background/foreground.xml`

## 2. Brand

- [ ] `core/design-system/.../theme/AppColor.kt` → thang `Primary` đang là placeholder, thay màu brand
      (giữ tên token `p50`..`p950`)
- [ ] `AppTheme.kt` → chỉnh `LightColorScheme` / `DarkColorScheme` nếu cần
- [ ] Font Outfit trong `core/design-system/src/main/res/font/` — đổi thì sửa `AppTypography.kt`

## 3. API

- [ ] `app/build.gradle.kts` → `BASE_URL` cho flavor `dev` và `prod` (hiện `https://dummyjson.com/` phục vụ luồng mẫu)
- [ ] Field build-time mới: sửa `AppConfig` (`:domain/config`) + `ConfigModule.kt`
- [ ] `ApiErrorBody` (`:data/remote/dto`) → khớp body lỗi của BE
- [ ] Mã lỗi nghiệp vụ cần phân nhánh → subclass `DomainException` + map trong `toDomainException()`
- [ ] Mỗi API: DTO → `XRemoteDataSource` + `Impl` → `XRepository` (`:domain`) + `Impl` → use case
      theo nghiệp vụ → khai trong `DataModule` (xem `docs/rules/data-layer.md`)

## 4. Firebase (tuỳ chọn)

- [ ] Tạo Firebase project, đăng ký **cả** `com.base.app` và `com.base.app.dev`
- [ ] Đặt `google-services.json` vào `app/` (đang gitignore — muốn commit thì bỏ dòng đó)
- [ ] Bỏ comment 2 plugin `google.services` + `firebase.crashlytics` trong `app/build.gradle.kts`
- [ ] Khai báo remote config key ở `:core:analytics` → `RemoteKeys.ALL_KEYS`

## 5. Signing

- [ ] Tạo keystore (không commit — `*.jks` đã gitignore)
- [ ] Thêm vào `local.properties`:
      ```
      RELEASE_STORE_FILE=key/release.jks
      RELEASE_STORE_PASSWORD=...
      RELEASE_KEY_ALIAS=...
      RELEASE_KEY_PASSWORD=...
      ```

## 6. Ngôn ngữ

- [ ] Thêm/bớt locale sửa **cả 3 chỗ**: `res/xml/locales_config.xml`, `localeFilters` trong
      `app/build.gradle.kts`, enum `AppLocale` (`:core:ui-foundation`)

## 7. Thay phần mẫu

- [ ] Xoá luồng mẫu post feed theo mục "Xoá khi làm project thật" trong `docs/guides/sample-flow.md`
- [ ] `:feature:main` → Home / Feature / Profile là màn mẫu, thay bằng màn thật
      (giữ `MainScreen` + `MainBottomNavBar`, sửa enum `MainTab`)
- [ ] `:core:utils/res/values/colors.xml` → màu Toasty theo brand

## 8. Nên làm sớm

- [ ] Test + CI (build dev + prod, lint)
- [ ] detekt / ktlint
- [ ] Baseline profile sau khi app có màn hình thật
