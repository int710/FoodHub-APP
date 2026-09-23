# FoodHub Android

Ứng dụng Android viết bằng Kotlin và Jetpack Compose, gồm giao diện quản trị nhà hàng và luồng khách hàng quét QR tại bàn để nhắn tin hỗ trợ.

## Chức năng hiện có

### Quản trị viên

- Tổng quan doanh thu, đơn đang xử lý, món tạm hết và tin nhắn chưa đọc.
- Tìm kiếm, lọc, xác nhận, từ chối và cập nhật trạng thái đơn hàng.
- Xem chi tiết món, ghi chú, thanh toán và tiến độ của từng đơn.
- Tải danh sách bàn, tạo bàn mới và hiển thị QR do backend cấp.
- Tìm kiếm món ăn, lọc danh mục và bật/tắt trạng thái đang bán.
- Danh sách hội thoại, lọc tin chưa đọc, trả lời nhanh và gửi tin nhắn trên giao diện.
- Thông tin tài khoản và đăng xuất khỏi màn Admin.

> Đơn hàng, thực đơn và tin nhắn Admin hiện dùng mock data. Riêng tab **Bàn** đã gọi API thật và cần access token của tài khoản Admin.

### Khách hàng

- Quét QR trực tiếp bằng CameraX và ML Kit Barcode Scanning.
- Chọn ảnh QR từ thư viện và bật/tắt đèn flash.
- Gọi `POST /api/v1/table/scan` để xác thực mã QR và nhận table token.
- Lưu phiên bàn trong `SharedPreferences` với tên `table_session`.
- Kết nối chat Socket.IO bằng `auth.tableToken`.
- Gửi/nhận tin nhắn, tải lịch sử, hiển thị trạng thái nhập tin và tự kết nối lại.

App hiện mở vào giao diện Admin. Chọn ảnh đại diện `AD`, sau đó nhấn **Đăng xuất khỏi Admin** để chuyển sang luồng khách hàng quét QR và chat.

## Yêu cầu môi trường

- Android Studio có Android SDK Platform 36.
- JDK 17.
- Thiết bị hoặc emulator Android API 26 trở lên.
- Kết nối Internet để quét bàn và sử dụng chat.

## Chạy ứng dụng

1. Mở thư mục project bằng Android Studio.
2. Chờ Gradle sync và cài SDK còn thiếu nếu được yêu cầu.
3. Chọn thiết bị Android hoặc emulator.
4. Nhấn **Run**, hoặc build bằng lệnh:

```powershell
.\gradlew.bat assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Chạy Android Lint bằng:

```powershell
.\gradlew.bat lintDebug
```

## QR tại bàn

Ứng dụng chấp nhận nội dung QR ở một trong các dạng:

- Token thuần.
- JSON có trường `qrToken`.
- URL có query parameter `qrToken`, `qr_token` hoặc `token`.

Token được gửi tới backend dưới dạng:

```json
{
  "qrToken": "TABLE_QR_TOKEN"
}
```

Ứng dụng chỉ cho phép vào bàn khi backend trả về table token hợp lệ. Vì vậy, QR bất kỳ không do hệ thống FoodHub tạo sẽ bị từ chối.

## Backend và Socket.IO

Backend hiện được cấu hình trực tiếp trong source:

```text
https://foodhub-8lv1.onrender.com
```

API quét bàn:

```text
POST https://foodhub-8lv1.onrender.com/api/v1/table/scan
```

Các sự kiện Socket.IO đang sử dụng:

- `conversation:join`
- `message:send`
- `message:new`
- `message:history`
- `typing:start`
- `typing:stop`

Table token và `conversationId` được lưu trong `table_session`. Table token có thể được dùng làm header `X-Table-Token` cho các API `DINE_IN` tiếp theo.

### Quản lý bàn và tạo QR

Tab **Bàn** sử dụng các endpoint quản trị:

```text
GET  /api/v1/table
POST /api/v1/table
GET  /api/v1/table/{id}/qr
```

Các endpoint này yêu cầu header `Authorization: Bearer <accessToken>`. Lấy access token bằng API đăng nhập Admin hoặc Swagger, mở tab **Bàn**, nhấn biểu tượng chìa khóa và dán token vào. Token được lưu cục bộ trong `SharedPreferences` tên `admin_session`.

Khi tạo bàn, backend tự sinh `qrToken`. Ứng dụng gọi endpoint QR và dùng ZXing để hiển thị thành ảnh QR. Không tự đặt `qrToken` trong app.

## Cấu trúc chính

```text
app/src/main/java/com/foodhub/app/
├── MainActivity.kt          # Điểm vào app và chuyển luồng Admin/khách hàng
├── AdminShell.kt            # Theme, thanh điều hướng và NavHost Admin
├── AdminScreens.kt          # Các màn hình Admin
├── AdminModels.kt           # Model và mock data Admin
├── AdminTablesScreen.kt     # Quản lý bàn và hiển thị QR
├── AdminTableRepository.kt  # API bàn và lưu access token Admin
├── ScanScreen.kt            # Camera, đọc QR và xác nhận vào bàn
├── TableScanRepository.kt   # API quét bàn và lưu phiên
├── ChatScreen.kt            # Giao diện chat khách hàng
└── ChatSocketClient.kt      # Socket.IO và sự kiện chat
```

## Trạng thái triển khai

| Hạng mục | Trạng thái |
| --- | --- |
| Giao diện Admin | Hoàn thành bản UI tương tác |
| Điều hướng Admin | Đã hoạt động |
| API quản lý bàn Admin | Đã tích hợp, cần access token |
| API đơn hàng/thực đơn Admin | Chưa tích hợp |
| Socket chat Admin | Chưa tích hợp |
| Camera và đọc QR | Đã tích hợp |
| API xác thực bàn | Đã tích hợp |
| Chat Socket.IO khách hàng | Đã tích hợp |
