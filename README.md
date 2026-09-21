# FoodHub Android UI

Ứng dụng mẫu Kotlin + Jetpack Compose dựng theo hai ảnh Figma trong thư mục gốc:

- **Quét QR Gọi Món Tại Bàn**: khung quét, chi nhánh, thông tin bàn, nút xác nhận.
- **Nhắn Tin Hỗ Trợ Trực Tiếp**: hội thoại mẫu, gợi ý trả lời và ô gửi tin.

## Chạy ứng dụng

1. Mở thư mục này bằng Android Studio.
2. Cài Android SDK Platform 36 khi Android Studio yêu cầu, rồi chờ Gradle sync.
3. Chọn thiết bị Android hoặc emulator API 26 trở lên và bấm **Run**.

Ứng dụng mở ở màn quét QR. Nút **Xác nhận vào bàn ngay** mở màn chat; nút quay lại đưa về màn quét. Tin nhắn gửi, gợi ý trả lời và nhập số bàn được giữ trong bộ nhớ khi ứng dụng đang mở.

Đây là bản **giao diện**: vùng camera/QR, ảnh nhà hàng và hội thoại là dữ liệu minh họa. Chưa kết nối camera, thư viện ảnh, nhân viên hoặc máy chủ. Ảnh thiết kế chỉ dùng để đối chiếu; khi có ảnh nền, logo và icon xuất riêng từ Figma, có thể thay các hình minh họa bằng đúng tài nguyên.
