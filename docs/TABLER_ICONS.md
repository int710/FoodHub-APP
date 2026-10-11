# Tabler Icons trong FoodHub Android

FoodHub dùng Tabler Icons outline 24×24, stroke 2, phiên bản `3.46.0`.
Nguồn chính thức: <https://tabler.io/icons> và <https://github.com/tabler/tabler-icons>.

## Cách tích hợp

- Chỉ 60 icon đang được tham chiếu được sinh vào `TablerIcons.kt`.
- Không thêm dependency icon pack vào APK.
- Không dùng emoji, ký tự Unicode hoặc Material Icons làm biểu tượng giao diện.
- Icon danh mục cũ từ API được ánh xạ sang icon Tabler cục bộ; dữ liệu menu hiện tại không cần sửa.
- Giấy phép MIT được lưu tại `docs/licenses/TABLER_ICONS_LICENSE.txt`.

## Tái tạo bộ icon

Tải đúng gói `@tabler/icons@3.46.0`, giải nén và chạy:

```powershell
node scripts/generate-tabler-icons.mjs <đường-dẫn-thư-mục-package>
```

Script tự quét mọi tham chiếu `TablerIcons.*` trong source Kotlin và chỉ sinh các
icon được sử dụng. Khi thêm icon mới, thêm ánh xạ trong script, dùng icon trong UI,
sau đó chạy lại lệnh trên.

## Quy ước

- Dùng icon outline cho UI mặc định; trạng thái được thể hiện bằng màu và nền, không trộn nhiều bộ nét.
- Kích thước thông thường: 24dp; trong button 18–22dp; trạng thái nhỏ 14–16dp.
- Luôn có `contentDescription` cho thao tác; icon trang trí dùng `null`.
- Logo ZaloPay và hình thương hiệu vẫn dùng asset chính thức, không thay bằng Tabler.
