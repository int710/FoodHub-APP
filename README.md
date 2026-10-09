# FoodHub

Ứng dụng Android Kotlin/Jetpack Compose và backend TypeScript trong `FoodHub-main`.

Rà soát cập nhật: **07/10/2026**, dựa trên mã nguồn hiện tại, kể cả thay đổi chưa commit. Danh sách dưới đây ghi nhận phần chưa triển khai và hạn chế có bằng chứng trong source. Chưa kiểm thử trên thiết bị hoặc dịch vụ production trong lần rà soát này; các luồng cần xác minh thực tế được ghi riêng.

## Chức năng đã có triển khai

- Đăng ký, đăng nhập, refresh token, xác thực email, khôi phục mật khẩu và hồ sơ.
- Thực đơn, chi tiết món, tùy chọn, flash sale, giỏ hàng; đặt tại bàn, mang đi và giao hàng.
- Thanh toán tiền mặt/VNPay, lịch sử đơn có tải thêm, hủy đơn và đánh giá món.
- Quét QR, phiên bàn và chat khách hàng/Admin qua Socket.IO.
- Admin quản lý đơn, bếp, bàn/QR, món, danh mục, flash sale, thêm/sửa tùy chọn và upload ảnh.
- Thông báo, đánh dấu đã đọc; Admin nhận sự kiện cập nhật đơn.

Có mã triển khai chưa đồng nghĩa đã hoạt động đúng trên môi trường phát hành. Giao diện Admin hiện có API thật; mô tả cũ về mock data không còn phù hợp.

## Các chức năng tương ứng endpoint API còn thiếu hoặc chưa hoàn chỉnh

Phạm vi bảng này là các HTTP endpoint đã khai báo trong `FoodHub-main/src/routes` và luồng sử dụng tương ứng trên Android. Tất cả đường dẫn bên dưới có prefix `/api/v1`. Không tính việc thiếu chức năng ngoài hợp đồng API hiện tại như thống kê doanh thu, push nền hay API hoàn tiền mới là endpoint chưa tích hợp.

Theo báo cáo đối chiếu endpoint hiện có: 61 route, 58 có lời gọi Android, 1 alias và 2 callback VNPay. Chưa thấy endpoint nghiệp vụ nào trong danh sách này hoàn toàn thiếu lời gọi app; phần còn thiếu chủ yếu nằm ở cách sử dụng dữ liệu, quyền và xử lý lỗi. Có lời gọi API không đồng nghĩa toàn bộ chức năng đã hoàn chỉnh.

Đường dẫn Android trong cột bằng chứng tính từ `app/src/main/java/com/example/foodhubapp/`.

| Endpoint API | Chức năng tương ứng | Phần chưa hoàn chỉnh | Bằng chứng / việc cần làm |
| --- | --- | --- | --- |
| `GET /order/history` | Xem danh sách đơn Admin | App chỉ gọi `page=1&limit=100`, không tải thêm nên bỏ sót đơn ở trang sau. | `feature/admin/order/data/AdminOrderRepository.kt`. Bổ sung phân trang và bộ lọc phía server. Lịch sử đơn khách có `loadMore` riêng, không thuộc lỗi này. |
| `GET /menu/items` | Xem danh sách món quản trị | App chỉ gọi `page=1&limit=100`, không tải thêm món. | `feature/admin/menu/data/AdminMenuRepository.kt`. Sử dụng pagination và tải thêm trên UI. |
| `GET /notifications` | Xem thông báo | Chỉ lấy trang 1, tối đa 50 thông báo. Khi chọn thông báo đơn hàng, app bỏ qua `orderId` và chỉ mở danh sách đơn. | `feature/shared/notification/data/NotificationRepository.kt`, `navigation/AppNavGraph.kt`. Thêm phân trang và điều hướng tới đúng đơn. |
| `GET /reviews/items/:menuItemId` | Xem đánh giá món | Chỉ lấy trang 1, tối đa 20 đánh giá; chưa có tải thêm. | `feature/customer/menu/data/FoodRepository.kt`. Đọc pagination và bổ sung tải trang tiếp theo. |
| `POST /reviews/feedback` | Gửi đánh giá kèm ảnh | Rating/comment đã có; trường `images` luôn gửi mảng rỗng. App chưa có chọn/upload ảnh cho khách, trong khi `POST /media/upload-image` chỉ cho ADMIN. | `feature/customer/order/data/OrderRepository.kt`, `FoodHub-main/src/routes/media.routes.ts`. Hoàn thiện luồng ảnh và quyền upload phù hợp. |
| `POST /media/upload-image` | Admin tải ảnh món | Upload multipart chưa tự refresh token/thử lại khi gặp 401 như các lời gọi JSON; có thể thất bại khi access token hết hạn. | `core/network/FoodHubApiClient.kt`, hàm `uploadImage`. Bổ sung refresh và thử lại có giới hạn. |
| `GET /conversations` | Admin xem danh sách hội thoại | App gán `isOnline=true` và suy ra `unread` là 0/1 từ người gửi cuối/người được phân công. Trạng thái online và số tin chưa đọc hiển thị chưa phản ánh dữ liệu thực. | `feature/admin/chat/data/AdminConversationRepository.kt`. Thống nhất dữ liệu API và cách hiển thị; cần cơ chế đọc/presence nếu muốn hiển thị chính xác. |
| `PATCH /menu/item-variants/:groupVariantId` | Sửa nhóm/tùy chọn món | Đã sửa/thêm được, chưa hỗ trợ xóa tùy chọn: backend upsert/create và không xóa tùy chọn bị bỏ khỏi payload. | `feature/admin/menu/ui/AdminVariantManager.kt`, `FoodHub-main/src/services/menu.services.ts`. Đây là giới hạn của API hiện tại, không phải endpoint chưa tích hợp. |
| `GET /order/history`, `GET /order/kitchen`, `PATCH /order/kitchen/:itemId/status`, `PATCH /order/:id/confirm`, `PATCH /order/:id/reject`, `PATCH /order/:id/serve`, `PATCH /order/:id/complete`, `PATCH /payment/:orderId/cash-confirm`, `GET /menu/items`, `PATCH /menu/items/:id/toggle`, `GET /conversations`, `PATCH /conversations/:id/close` | Nhân viên STAFF xem/xử lý đơn, bếp, món và hội thoại | Backend cấp quyền STAFF nhưng AppNavGraph chỉ đưa ADMIN vào quản trị; STAFF vào Home khách hàng. Các thao tác tương ứng chưa có luồng STAFF trên app. | `navigation/AppNavGraph.kt` và middleware `requireRole` trong các route. Bổ sung điều hướng và màn hình STAFF theo quyền từng endpoint. |
| `POST /order/:type/new` với `type=DINE_IN`, `GET /order/history` | Khách QR đặt món rồi theo dõi đơn | Tạo đơn hỗ trợ phiên bàn không đăng nhập; history và màn danh sách đơn lại yêu cầu tài khoản. Luồng theo dõi đơn của khách vãng lai chưa hoàn chỉnh. | `FoodHub-main/src/routes/orders.routes.ts`, `feature/customer/order/viewmodel/OrderListViewModel.kt`. History hiện chưa có hợp đồng truy vấn theo phiên bàn; cần bổ sung backend trước khi tích hợp app. |

`GET /table/all` là alias của `GET /table`; app dùng `/table`, không cần triển khai thêm chức năng cho alias. `GET /payment/vnpay/return` và `GET /payment/vnpay/ipn` là callback backend cho trình duyệt/VNPay, không phải endpoint app cần gọi trực tiếp.

## Endpoint đã tích hợp nhưng chưa xác minh hoạt động thực tế

Các mục dưới đây cần kiểm thử môi trường thật; chưa có bằng chứng để ghi là lỗi:

| Endpoint / nhóm endpoint | Nội dung cần xác minh |
| --- | --- |
| `POST /order/:type/new`, các PATCH trạng thái đơn, `GET /order/kitchen`, `PATCH /order/kitchen/:itemId/status` | Ba loại đơn, hủy theo trạng thái, xử lý bếp, phục vụ và hoàn tất. |
| `POST /payment/vnpay/create`, `GET /payment/vnpay/return`, `GET /payment/vnpay/ipn`, `GET /payment/:orderId`, `PATCH /payment/:orderId/cash-confirm` | Thanh toán tiền mặt/VNPay, chữ ký và callback lặp/thất bại; tiền về sau khi hủy được backend ghi `requiresRefund`, chưa có endpoint hoàn tiền trong router hiện tại. |
| `POST /user/verify-email`, `POST /user/forgot-password`, `POST /user/reset-password` | Gửi email, token/link và mở lại app qua deep link. |
| `POST /user/refresh-token`, `POST /media/upload-image` | Refresh trong môi trường thật, quyền upload và cấu hình R2. |
| `POST /table/scan`, `POST /table/session/end`, các endpoint giỏ hàng `/cart/:type/...` | Nhiều khách cùng bàn, token hết hạn, giỏ dùng chung và kết thúc phiên. |
| `POST /reviews/feedback`, `GET /reviews/items/:menuItemId`, `GET /notifications`, `GET /notifications/unread-count`, `PATCH /notifications/:id/read`, `PATCH /notifications/read-all` | Gửi/đọc đánh giá, chống gửi trùng, lưu trạng thái sau mở lại app và đồng bộ thông báo đã đọc. |

Cần đối chiếu bản backend đang triển khai với source/migration hiện tại trước khi kiểm thử. Rà soát này kiểm tra mã nguồn, không chạy thử các endpoint production.

Báo cáo kiểm tra trước đây: [đối chiếu endpoint](ENDPOINT_COVERAGE_2026-10-06.md), [rà soát ngày 06/10](PROJECT_AUDIT_2026-10-06.md), [chuẩn bị phát hành](RELEASE_READINESS.md). Đây là tài liệu lịch sử; các mô tả trạng thái cũ có thể đã được thay thế bởi mã hiện tại và README này. Lần cập nhật tài liệu này không chạy lại build/test.

## Sửa hủy đơn — 08/10/2026

- `PATCH /order/:id/cancel`: khách được hủy đơn của mình, chưa thanh toán, ở trạng thái `PENDING_PAYMENT` hoặc `PENDING_CONFIRMATION`. App hiển thị nút hủy cho cả hai trạng thái và cả ba loại đơn.
- Vẫn chặn đơn đã thanh toán, đã xác nhận hoặc đang chế biến. Backend kiểm tra lại quyền/trạng thái trong transaction để xử lý hủy đồng thời với thanh toán/xác nhận.
- Hộp thoại hủy đóng ngay sau khi nhập lý do và nhấn xác nhận; thông báo thành công/lỗi hiển thị trên màn danh sách sau khi API trả kết quả. Nếu VNPay báo tiền về sau khi hủy, giữ đơn đã hủy và ghi nhận cần hoàn tiền.
- Kiểm chứng sửa đổi: backend build và 23 test đơn/thanh toán qua; Android unit test và assembleDebug qua. Chưa kiểm thử trên backend production/thiết bị thật.
- Cần triển khai backend và cài APK mới để bản đang chạy áp dụng sửa đổi. Không cần migration mới cho sửa đổi này.

## Chạy dự án và chuẩn bị phát hành

- Android: mở project trong Android Studio; cấu hình hiện tại dùng compileSdk/targetSdk 37, minSdk 24, Gradle daemon JDK 25.
- Build debug: `.\gradlew.bat assembleDebug`.
- Unit test: `.\gradlew.bat testDebugUnitTest`; lint: `.\gradlew.bat lintDebug`.
- Backend: vào `FoodHub-main`, cài dependency và dùng `npm run dev`; build bằng `npm run build`.
- Backend có các lệnh kiểm tra `npm run test:chat`, `npm run test:orders`, `npm run test:reviews`.
- API đang được cấu hình trong source: `https://foodhub-8lv1.onrender.com/api/v1`.
- Trước phát hành: kiểm thử thiết bị thật, xác minh cấu hình DB/Redis/email/VNPay/R2, triển khai migration, cấu hình ký release và tăng `versionCode` (hiện là 1). `app/build.gradle.kts` chưa khai báo signing release.

## Cấu trúc source và hướng dẫn phát triển

```text
app/src/main/java/com/example/foodhubapp/
├── MainActivity.kt
├── navigation/       # Route và AppNavGraph
├── core/             # HTTP, token, session và hạ tầng dùng chung
├── theme/            # Màu, font và theme Compose
└── feature/
    ├── customer/     # Home, menu, cart, order, profile, table, chat, onboarding, splash
    ├── admin/        # Overview, order/bếp, menu, table, chat, profile và navigation
    └── shared/       # Auth, notification và model chat dùng chung
FoodHub-main/          # Backend TypeScript
```

- Mỗi feature giữ `data`, `model`, `viewmodel`, `ui` khi cần. Model dùng chung đặt trong `shared`, hạ tầng đặt trong `core`.
- Route nối ViewModel với UI và xử lý điều hướng; Screen nhận state/callback; Repository gọi API, ViewModel quản lý state/nghiệp vụ.
- Khi chuyển Figma sang Compose: dùng đúng frame/node, đối chiếu spacing/màu/font, tái sử dụng theme, chuyển SVG thành VectorDrawable và kiểm tra trên kích thước màn hình nhỏ.
- Kiểm tra build sau khi sửa mã; kiểm thử phù hợp với hành vi thay đổi.

Đã hợp nhất thông tin cấu trúc source và hướng dẫn Compose cần thiết vào đây, xóa `FIGMA_TO_COMPOSE_README.md` và README con trong `feature/` để dự án chỉ còn một README chính. Các tài liệu nghiệp vụ/API và báo cáo lịch sử được giữ để tra cứu.
