# FlowDrama — Android take-home

App xem phim ngắn (short drama), video dọc. 23 màn theo mockup `SCR-xxxx`, Kotlin + Jetpack Compose (không XML layout,
không Java), `minSdk 24`. Dữ liệu từ đúng hai endpoint của đề, quảng cáo bằng ad unit **test** của Google, bật/tắt bằng
Firebase Remote Config.

## Setup

**Cần:** Android Studio Quail 1 (2026.1.1) trở lên · Android SDK 37 · JDK 17 (thiếu thì Gradle tự tải).

```bash
git clone <repo-url> flowdrama && cd flowdrama
./gradlew :app:assembleDevDebug      # hoặc mở bằng Android Studio rồi Run
```

Không có bước tay nào khác:
- `app/google-services.json` đã commit (project Firebase `take-home-test-c5997`).
- Lib ads của tác giả (`com.github.dinhn:ads-*` 1.1.1) nằm sẵn trong repo dạng maven local (`third_party/maven`, chỉ bản build
  AAR + POM, khoảng 200 KB). Lib phát hành trên GitHub Packages, mà GitHub Packages luôn đòi token — token để trong repo public
  bị GitHub tự thu hồi, nên vendor bản build để clone sạch không cần đăng nhập gì.
- Build bằng dòng lệnh mà chưa từng mở Studio thì cần `ANDROID_HOME` (hoặc `sdk.dir` trong `local.properties`, Studio tự tạo).

| Flavor | Dữ liệu | Dùng khi |
|---|---|---|
| `prod` (**mặc định** — Run trong Studio ra `prodDebug`) | gọi `https://flowdrama.luonluon.com/flowdrama/api/` | chấm bài, thử offline thật |
| `dev` | đọc `fixtures/` đóng gói trong app (ảnh poster trong fixtures trả 404) | phát triển, chụp màn (host thật trả 403 khi bị gọi dồn) — có thêm launcher debug, xem cuối file |

Cùng `applicationId` `com.takehome.drama` và cùng `versionName` (Firebase chỉ đăng ký một app). Bản release
(`assembleProdRelease`, R8 full mode + shrink) không ký nếu `local.properties` thiếu `RELEASE_*`.

## Màn nào xong, màn nào mới là khung

Công sức dồn vào luồng xem phim và phần đề chấm nặng (dữ liệu, ads, lifecycle). Không màn nào bỏ hẳn; không màn nào
là khung rỗng — nhưng độ sâu khác nhau:

**Làm tới** — đủ trạng thái, có unit test ViewModel/use case, chạy tay nhiều vòng, so cạnh mockup:

| SCR | Màn | Ghi chú |
|---|---|---|
| 0302 | Player | Pager dọc kiểu TikTok, pool 3 ExoPlayer + preload tập kế, nhớ vị trí (Room) mỗi ~5 s + khi pause/thoát — mở lại hay vuốt lại tập xem dở đều xem tiếp đúng chỗ (xem hết thì từ đầu), tự sang tập kế, tốc độ, chất lượng/ngôn ngữ từ `sources`, mất mạng → Try again phát tiếp đúng chỗ |
| 0303 / 0304 | Sheet chọn tập / cài đặt phát | Tab 1–20…, tốc độ 0.5–3×, quality lấy từ dữ liệu (không bịa 1080p), autoplay |
| 0604 | Buffering | Overlay "Loading…", báo khi Auto tự hạ quality |
| 0301 | Chi tiết phim | Lưới tập 4 trạng thái (đã xem / đang xem / chưa xem / khoá), mở khoá bằng rewarded. Chọn dải tập bằng chip (1–20, 21–40…) thay cho "1–68 ▾" của mockup — không dùng popup/dropdown |
| 0101 | Home Day Cycle | Theo giờ máy (sáng / trưa / tối), hero = `heroShowId`, "Pick up your flow" từ Room, ô native |
| 0202 | Browse Preview Dock | Preview câm phía trên, chip genre đổi lưới không reset preview. Chưa có chip "New" của mockup |
| 0203 | Reels | Pager dọc nhiều show, nút "Watch all N episodes" |
| 0001 | Splash | Chờ catalogue + Remote Config (RC tối đa 3 s, cả lượt tối đa 8 s, hiện ít nhất 1 s) — không bao giờ treo |

**Xong, mức vừa** — đủ chức năng và trạng thái chính, khớp mockup, ít edge case hơn:

| SCR | Màn | Còn thiếu / tự quyết |
|---|---|---|
| 0002–0005 | Onboarding (ngôn ngữ, value prop, mood, pre-prompt thông báo) | Một destination + `HorizontalPager`. Bước 0005 chỉ xin quyền `POST_NOTIFICATIONS` (API 33+); **chưa lên lịch** thông báo 3 lần/ngày |
| 0201 | Search | Lọc client-side trên catalogue (title mọi ngôn ngữ + tag), debounce 300 ms |
| 0401 / 0402 / 0403 | Library / History / Saved | Room, sống qua restart; History sửa/xoá được. Saved không có dòng "Ep. 14 out today" của mockup — API không có ngày ra tập |
| 0501 / 0502 | Settings / Language | Dark/Light đổi ngay; 10 ngôn ngữ UI đổi ngay không restart. Terms / Privacy chỉ báo "chưa có" |

**Lệch đề có chủ ý:** đề §2.1 nói SCR-0002 chọn trong 5 ngôn ngữ của catalogue; app theo mockup — **10 ngôn ngữ UI** (en, hi, ta,
id, ar, fr, es, pt-BR, tr, de). Ngôn ngữ nội dung (tên phim, audio) vẫn là 5 của catalogue; UI chọn ngôn ngữ catalogue không có
thì nội dung lùi về `defaultLanguage`. Tập khoá mở bằng rewarded là **tự quyết** (đề không yêu cầu luồng mở khoá; không làm 0305).
| 0601 / 0602 / 0603 / 0605 | Skeleton / Empty / Offline / Toast | Dùng chung mọi màn dữ liệu; shimmer tắt khi bật Reduce Motion |

Chuông ở Home dẫn sang Settings (chưa có màn thông báo). Light theme dùng bộ token tạm — mockup ghi "Light needs a
light token set, still to be designed".

## Dữ liệu — những chỗ payload cố tình làm khó

- **Map đa ngôn ngữ**: ngôn ngữ nội dung đã chọn → `defaultLanguage` → giá trị đầu tiên có. Catalogue chỉ có 5 ngôn ngữ
  nội dung; UI có 10 — chọn ngôn ngữ UI không có trong catalogue thì nội dung lùi về `defaultLanguage`.
- **Hai loại tag**: `isCategory=true` → chip genre (Browse, Onboarding); `false` → rail Home. Trong fixtures không show nào
  gắn tag rail → rail theo tag rỗng bị bỏ, Home dùng rail "Also flowing…" (mọi show trừ hero, theo `viewCount`). API thật
  có gắn nên rail Trending/Popular hiện ở flavor `prod`.
- **Tên tập rỗng** → "Ep. N". **`episodeCount` lệch** file tập: lưới tập tin file tập; `episodeCount` chỉ dùng ở chỗ chưa tải
  tập (card, Saved).
- **Quality key lạ** (`1080p`, `2160p`, `abc`): `sources` parse thành map, key nào cũng nhận; một rendition hỏng không làm
  mất tập. **`status` lạ** → nhánh `Unknown`, không crash. Phần tử JSON sai kiểu bị bỏ từng phần tử, không vỡ cả danh sách.
- Thứ tự tập = thứ tự mảng (không giả định `number` liên tục). `viewCount`/`likeCount` là `Long`, format "4.7M" theo locale.

## Thư viện và vì sao

| Thư viện | Vì sao |
|---|---|
| Jetpack Compose (BOM 2026.09) + Material 3 | Đề bắt buộc Compose; component tự dựng theo token Sunset Flow, không dùng popup Material mặc định |
| Navigation Compose (type-safe) | Destination `@Serializable`, back stack chuẩn, sống qua process death |
| Koin 4 | DI gọn, không cần annotation processor; `koinViewModel()` cho Compose |
| Ktor 3 + kotlinx.serialization | Kotlin-first; serializer khoan dung tự viết cho payload "bẩn" |
| Room 2.8 | Lịch sử xem / vị trí phát / show đã lưu — cần query và sống qua restart |
| DataStore | Cài đặt nhỏ (ngôn ngữ, theme, gu, tập đã mở khoá) |
| Media3 ExoPlayer 1.11 | Player chuẩn của Android; pool 3 ExoPlayer tự quản (tập hiện tại + 2 lân cận nạp sẵn vài giây) + `SimpleCache` cho vuốt tập mượt |
| Coil 3 | Ảnh Compose, cache đĩa 100 MB |
| Firebase Remote Config / Analytics / Crashlytics | Kill switch ads (đề yêu cầu); analytics + crash sau interface, no-op khi thiếu config |
| `com.github.dinhn:ads-*` 1.1.1 (vendor ở `third_party/maven`) | Wrapper AdMob của tác giả, đã dùng ở dự án thật; Google Mobile Ads 23.6 bên dưới |
| Timber | Log chỉ ở bản debug |
| JUnit 4, Turbine, Robolectric | Unit test (use case, mapper, ViewModel MVI, Room DAO) |

Kiến trúc: Clean Architecture nhiều module (`:domain` Kotlin thuần, `:data`, `:core:*`, `:feature:onboarding|main|watch`),
ViewModel theo MVI — mọi hành động qua `processIntent`, mỗi hành động nghiệp vụ một use case. Rule chi tiết ở `docs/rules/`.

## Ads

Lib ads của tác giả (`com.github.dinhn:ads-core|ads-admob|ads-firebase`). Mỗi format một manager ở `:core:ads`
(`InterstitialAdManager`, `RewardAdManager`, `NativeAdManager`); feature chỉ dùng các manager này. App id và mọi ad unit
đều là **test ID** của Google (`ca-app-pub-3940256099942544/...`). Native ad dựng `NativeAdView` bằng code (đề cấm XML layout).

### Placement

| id | Format | Vị trí | Vì sao |
|---|---|---|---|
| `interstitial_onboarding` | Interstitial | Hết onboarding, trước Home | Ranh giới luồng tự nhiên, user chưa xem dở gì. Load từ lúc mở onboarding; lúc kết thúc **chỉ hiện nếu đã sẵn**, chưa sẵn thì vào Home ngay. Tối đa 1 lần/phiên |
| `native_home` | Native | Home, sau "Pick up your flow", trước các rail | Hoà vào feed, Home không phát video. Ô chỉ xuất hiện khi ad đã load — tắt / lỗi / offline thì **không có ô nào** (không khung trống) |
| `rewarded_bonus` | Rewarded | Mở tập khoá: chạm ô khoá ở Detail → sheet "Watch ad"; trang khoá trong Player → nút "Watch an ad to unlock" | Opt-in rõ: chỉ mở khi user bấm. Mockup tự gợi ý "rewarded ads to open locked episodes". Xem hết → tập mở vĩnh viễn; đóng sớm → vẫn khoá |

Không ad nào hiện khi video đang phát (Player dừng hẳn trước khi show rewarded). Không làm banner, app-open, UMP/consent
(bài test, bỏ có chủ ý). Mọi lời gọi SDK bọc try/catch — lỗi = "không có ad", app đi tiếp.

### Kill switch `ads_config` — thứ tự ưu tiên

Tham số Remote Config kiểu JSON đúng hình dạng đề (`enable`, `disabled_versions`, `placements.<id>.enable`):

1. `enable == false` → tắt hết
2. `versionName` ∈ `disabled_versions` (khớp chuỗi **chính xác**) → tắt hết
3. `placements.<id>.enable == false` → tắt riêng placement đó
4. placement không khai → mặc định của code (bật)

Kill switch rộng đứng trước cấu hình hẹp: tắt khẩn cấp theo app/version không bị một placement còn ghi `true` vô hiệu
hoá. JSON parse khoan dung theo từng field (sai kiểu một field không làm hỏng field khác). **Chọn có chủ ý:** JSON hỏng hẳn
hoặc `enable` sai kiểu → coi như không cấu hình → ads **bật** (fail-open) — gõ nhầm trên Console không âm thầm tắt doanh thu;
muốn tắt chắc chắn thì đặt đúng `"enable": false`. Chưa từng fetch / offline →
dùng giá trị phiên trước (lưu DataStore), lần đầu thì default nhúng sẵn. Config fetch muộn vẫn có hiệu lực ngay: màn đang
mở tự ẩn/hiện ô native và nút rewarded.

### Cách test nhanh

`versionName` hiện ở cuối Settings: **`FlowDrama v1.0.0 (1)`**. Interstitial chỉ có ở cuối onboarding (một lần mỗi lần cài);
mở lại app thì chỉ còn ô native ở Home và nút rewarded ở tập khoá — muốn xem lại interstitial thì xoá dữ liệu app. `minimumFetchInterval`: **0 s ở bản debug**, 3600 s ở
release. Fetch chạy lúc mở app (Splash chờ tối đa 3 s), nên "mở lại" = **vuốt kill app** rồi mở, không phải về Home.

| Phép thử | Đặt trên Console (`ads_config`) → Publish → kill + mở lại | Kết quả |
|---|---|---|
| Tắt theo version | `{"enable":true,"disabled_versions":["1.0.0"]}` | không ad nào |
| Version thắng placement | `{"enable":true,"disabled_versions":["1.0.0"],"placements":{"native_home":{"enable":true}}}` | vẫn không ad nào |
| Tắt toàn app | `{"enable":false}` | không ad nào |
| Tắt một placement | `{"enable":true,"disabled_versions":[],"placements":{"rewarded_bonus":{"enable":false}}}` | interstitial + native còn; tập khoá chỉ báo "locked" |

Bản `dev` thử được không cần Console (ghi đè trong phiên):

```bash
adb shell am start -S -n com.takehome.drama/.MainActivity --es debug_screen SCR-0101 \
    --es debug_ads_config '{"enable":true,"disabled_versions":["1.0.0"]}'
```

## State và lifecycle

Đã chạy tay trên emulator (Pixel 8, API 37):

| Tình huống | Hành vi |
|---|---|
| Xoay máy ở Player / Browse / Search / Reels | Không mất state, video phát tiếp không về đầu. Ngang: video dọc hiện vừa khung (viền đen hai bên); Browse đặt preview bên trái, lưới bên phải. Các màn khác chỉ cuộn được, không có layout ngang riêng (mockup chỉ có dọc) |
| Process bị kill khi ở nền (`am kill`) / "Don't keep activities" | Mở lại đúng màn, đúng tập, phát tiếp từ vị trí đã lưu |
| Player vào nền | Dừng, không phát tiếng; quay lại thì phát tiếp |
| Mất mạng giữa lúc xem | Báo "Can't play — check your connection" + Try again; có mạng lại thì phát tiếp đúng chỗ |
| Máy bay, cài mới, mở lần đầu | Không crash, Splash không treo, không ô ads trống, màn offline (0603) + Try again |
| Máy bay sau khi đã dùng | Catalogue + file tập lần trước (cache đĩa) và ads config phiên trước được dùng; chỉ video cần mạng |

## AI và thời gian

- **Có dùng AI**: Claude Code (Claude Opus) chạy theo workflow từng phase — đọc đề/mockup → plan → code (TDD) → review độc
  lập → chụp màn trên emulator so mockup → sửa. Các quyết định lớn (phạm vi, 10 ngôn ngữ UI, placement, lib ads, chỗ lệch
  mockup) do tôi chốt; quyết định nhỏ AI đề xuất, tôi duyệt qua PR — mỗi phase một PR, tôi review trước khi merge.
  Commit có trailer `Co-Authored-By: Claude`.
- **Thời gian**: theo lịch commit từ 23/10/2026 16:35 tới 24/10/2026 khoảng 15:30 (khoảng 23 giờ lịch), trừ khoảng 12 giờ nghỉ
  → **khoảng 11 giờ làm thực tế**, chia 7 phase, mỗi phase một PR.

## Hạn chế tự biết

- **minSdk 24 chưa chạy trên máy/emulator API 24** — chỉ build + lint. `formatCompactCount` trên API 24–27 (ICU < 60) làm
  tròn khác ("470K", 999 → "1K") và hậu tố `ar` bị cụt; test ghim API 28/35.
- Thông báo "3 lần/ngày" (0005) chỉ xin quyền, chưa lên lịch. Không có màn thông báo; Terms / Privacy chưa có trang.
- Chữ "khi nào" ở History ("today", "x min left") không tự đổi nếu để màn mở qua nửa đêm.
- Hàng Watching có tập mới nhất đã xem hết vẫn hiện thanh đầy (bấm vào thì mở tập kế). Số tập ở Saved lấy
  `episodeCount` của catalogue nên có thể lệch file tập.
- Lần đầu cài mà offline, bước chọn mood của onboarding chỉ hiện màn offline + Try again (mood cần tag thật của catalogue); Skip vẫn đi tiếp.
- Ảnh poster của fixtures (`/thumbnails/*.jpeg`) trả 404 → hiện placeholder; flavor `prod` có ảnh thật.
- Chưa đo thời gian ra khung hình đầu / bộ nhớ khi vuốt 20 tập trên máy thật (emulator dùng decoder phần mềm, số không đại diện).
- Code debug (`DebugCatalogFault`, `DebugLibrarySeeder`) nằm ở source set `main`, có cờ chặn ở prod — sạch hơn nếu chuyển sang `src/dev`.
- Không có UI test tự động; UI kiểm bằng ảnh chụp emulator đặt cạnh mockup (`tools/ui-check/`).
- Tiến độ xem và tập đã mở khoá gắn với **vị trí tập trong mảng** (đề nói thứ tự = thứ tự mảng); server chèn/đổi thứ tự tập
  thì tiến độ cũ có thể gắn nhầm tập.
- Một tập free mà không có nguồn phát dùng được → Player báo lỗi toàn màn thay vì chỉ đánh dấu riêng trang đó.
- UI và nội dung khác hướng chữ (vd. UI tiếng Pháp + nội dung tiếng Ả Rập) thì dòng meta trộn hai thứ tiếng có thể đảo thứ tự
  (chưa cô lập bidi từng phần).

## Nếu có thêm thời gian

1. Chạy đủ checklist trên API 24 và một máy thật cấu hình yếu; đo first-frame và bộ nhớ của Player.
2. Lên lịch thông báo 3 khung giờ (WorkManager) + màn thông báo cho chuông Home.
3. Bộ token Light theme thật, layout ngang riêng cho Home/Detail.
4. Screenshot test (Paparazzi/Roborazzi) cho các màn đã khớp mockup.
5. Chuyển code debug sang `src/dev`, thêm CI chạy `Verify`.

## Lệnh

```bash
./gradlew :app:assembleDevDebug         # dev, debug
./gradlew :app:assembleProdRelease      # prod, release (R8)
./gradlew lintDevDebug
./gradlew :domain:test :data:testDebugUnitTest :core:ui-foundation:testDebugUnitTest :core:ads:testDebugUnitTest \
    :core:media:testDebugUnitTest :feature:main:testDebugUnitTest :feature:onboarding:testDebugUnitTest \
    :feature:watch:testDebugUnitTest
```

Bản `dev` mở thẳng một màn: `adb shell am start -S -n com.takehome.drama/.MainActivity --es debug_screen SCR-0302 --es show_id 12485`
(danh sách mã và extra ở `app/.../navigation/DebugScreenLauncher.kt`; so mockup bằng `tools/ui-check/compare.sh`).
