# FoodHub Android App

Ứng dụng FoodHub dành cho khách hàng, nhân viên và quản trị viên, xây dựng bằng Kotlin và Jetpack Compose.

Backend được quản lý ở repository riêng `FoodHub`; không đặt bản sao backend hoặc thư mục backup trong repository này.

## Chức năng chính

- Đăng ký, đăng nhập, xác thực email, quên và đặt lại mật khẩu.
- Đặt món tại bàn bằng QR, mang về và giao tận nơi.
- Giỏ hàng, lịch sử đơn, trạng thái đơn realtime và thanh toán tiền mặt/VNPay.
- Chat realtime giữa khách hàng và quản trị viên theo tài khoản hoặc phiên bàn.
- Quản trị đơn hàng, bếp, thực đơn, danh mục, bàn/QR, thông báo và hồ sơ.

## Cấu trúc source

```text
app/src/main/java/com/example/foodhubapp/
├── MainActivity.kt
├── navigation/       # Route và điều hướng toàn app
├── core/             # Network, session, media, payment và UI dùng chung
├── theme/            # Theme Compose
└── feature/
    ├── customer/     # Phần dành cho khách hàng
    ├── admin/        # Phần dành cho quản trị viên/nhân viên
    └── shared/       # Auth, chat, notification và payment dùng chung
```

Resource Android nằm trong `app/src/main/res`. Icon Tabler đã được nhúng trực tiếp vào source Kotlin; giấy phép được lưu tại [docs/licenses/TABLER_ICONS_LICENSE.txt](docs/licenses/TABLER_ICONS_LICENSE.txt).

## Chạy dự án

1. Mở thư mục này bằng Android Studio.
2. Dùng JDK tương thích với phiên bản Android Gradle Plugin trong project.
3. Build bản debug:

```powershell
.\gradlew.bat assembleDebug
```

APK được tạo tại `app/build/outputs/apk/debug/app-debug.apk`.

Các lệnh kiểm tra hữu ích:

```powershell
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
```

## Quy ước

- Không commit `build`, `.gradle`, `.idea`, `local.properties`, backend hoặc thư mục backup.
- Mã theo feature; hạ tầng dùng chung đặt trong `core`, nghiệp vụ dùng chung đặt trong `feature/shared`.
- Khi thêm resource hoặc dependency, xóa bản cũ ngay khi không còn tham chiếu và chạy lint/build trước khi commit.
