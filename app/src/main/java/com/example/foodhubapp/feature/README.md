# Cấu trúc feature theo vai trò

```text
feature/
├── customer/
│   ├── home/
│   ├── menu/
│   ├── cart/
│   ├── order/
│   ├── profile/
│   ├── table/         # Quét QR, phiên bàn và giao diện bàn của khách
│   ├── chat/          # Giao diện và socket của khách
│   ├── onboarding/
│   └── splash/
├── admin/
│   ├── navigation/    # AdminShell, điều hướng và theme Admin
│   ├── model/         # Model đơn, món và bàn Admin
│   ├── components/    # Search, section header, avatar, empty state, định dạng tiền
│   ├── overview/ui/
│   ├── order/         # data, viewmodel, ui
│   ├── menu/          # data, viewmodel, ui và dialog chỉnh sửa
│   ├── table/         # data, ui
│   ├── chat/          # socket, data, model, viewmodel, ui
│   └── profile/ui/
└── shared/
    ├── auth/          # Đăng nhập, đăng ký, khôi phục và model tài khoản
    ├── notification/  # Thông báo dùng ở cả hai role
    └── chat/          # Model tin nhắn, trạng thái kết nối và listener
```

`core`, `theme`, `navigation/AppNavGraph` và `MainActivity` vẫn ở cấp ứng dụng vì phục vụ nhiều vai trò. Mỗi feature giữ các lớp `data`, `model`, `viewmodel`, `ui` khi cần; không tạo lớp trống chỉ để đủ cấu trúc.

Customer và Admin không import trực tiếp code của nhau. Model/giao diện cần cho cả hai role đặt trong `shared`; hạ tầng token, session và HTTP đặt trong `core`.

## Các file thay cho AdminScreens.kt

- Tổng quan: `AdminOverviewScreen.kt`, `AdminOverviewComponents.kt`.
- Đơn hàng: `AdminOrdersScreen.kt`, `AdminOrderDetailScreen.kt`, `AdminOrderComponents.kt`.
- Thực đơn: `AdminMenuScreen.kt`, `AdminMenuRow.kt`, `AdminCategoryComponents.kt`, `AdminMenuEditorDialog.kt`, `AdminFlashSaleDialog.kt`, `AdminVariantDialog.kt`.
- Chat: `AdminMessagesScreen.kt`, `AdminChatDetailScreen.kt`.
- Tài khoản: `AdminProfileScreen.kt`.
- Thành phần dùng chung: `AdminComponents.kt`.

Helper chỉ dùng trong một file giữ `private`; helper dùng giữa các file giữ `internal`. Refactor chỉ tổ chức source/package/import, không thay đổi endpoint, quyền backend hay luồng nghiệp vụ.
