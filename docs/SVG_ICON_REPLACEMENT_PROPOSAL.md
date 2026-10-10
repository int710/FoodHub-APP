# Đề xuất chuẩn hóa icon SVG cho FoodHub

## Bộ icon chuẩn

- Dùng **Material Symbols Rounded** (Apache 2.0), xuất thành Android Vector Drawable trong `res/drawable`.
- Kích thước chuẩn: 24dp; icon trong nút tròn 20–22dp; icon trạng thái 16dp.
- Không dùng emoji/ký tự Unicode làm icon vì hình dáng thay đổi theo máy và font.
- Tên file theo chức năng, không theo màn hình, để user/admin dùng chung một asset.

## Các icon mô phỏng đang có cần thay trước

| Hiện tại | Vị trí | SVG/vector đề xuất | Tên file |
|---|---|---|---|
| `♜` | Card/quét bàn | `table_restaurant` | `ic_table_restaurant.xml` |
| `×` | Đóng popup/camera | `close` | `ic_close.xml` |
| `ϟ` | Bật/tắt flash QR | `flash_on`, `flash_off` | `ic_flash_on.xml`, `ic_flash_off.xml` |
| `▣` | Chọn ảnh/thư viện | `photo_library` | `ic_photo_library.xml` |
| `▦` | Quét lại QR | `qr_code_scanner` | `ic_qr_scan.xml` |
| `●` | Trạng thái kết nối | `circle` 8dp hoặc dot vẽ bằng Compose | `ic_status_dot.xml` nếu cần |
| `✓` | Tin đã gửi | `done_all` | `ic_done_all.xml` |
| `★`, `☆` | Đánh giá | `star`, `star_border` | `ic_star.xml`, `ic_star_border.xml` |
| `−`, dấu cộng dạng chữ | Số lượng món | `remove`, `add` | `ic_remove.xml`, `ic_add.xml` |
| `🍽` | Icon danh mục mặc định | `restaurant` | `ic_category_restaurant.xml` |
| Chữ `AD` | Avatar admin giả lập | `account_circle` hoặc avatar mặc định | `ic_account_circle.xml` |

## Danh mục SVG dùng cho toàn app

### Điều hướng và thao tác chung

`ic_home`, `ic_receipt_long`, `ic_chat_bubble`, `ic_notifications`, `ic_person`,
`ic_search`, `ic_filter`, `ic_sort`, `ic_refresh`, `ic_more_vertical`, `ic_settings`,
`ic_logout`, `ic_arrow_back`, `ic_chevron_right`, `ic_close`, `ic_add`, `ic_remove`,
`ic_edit`, `ic_delete`, `ic_check`, `ic_check_circle`, `ic_error`, `ic_info`, `ic_calendar`.

### Đặt món và phục vụ

`ic_qr_scan`, `ic_table_restaurant`, `ic_takeaway_bag`, `ic_delivery_truck`, `ic_cart`,
`ic_restaurant`, `ic_menu_book`, `ic_kitchen`, `ic_room_service`, `ic_timer`,
`ic_location`, `ic_phone`, `ic_note`, `ic_coupon`, `ic_favorite`, `ic_favorite_border`,
`ic_star`, `ic_star_border`, `ic_inventory`, `ic_sold_out`.

### Thanh toán

`ic_cash`, `ic_wallet`, `ic_qr_payment`, `ic_credit_card`, `ic_receipt`,
`ic_paid`, `ic_pending_payment`, `ic_payment_success`, `ic_payment_failed`.

Logo ZaloPay phải dùng asset thương hiệu chính thức, không thay bằng Material Symbol.

### Chat và realtime

`ic_headset`, `ic_send`, `ic_attachment`, `ic_image`, `ic_camera`, `ic_call`,
`ic_done_all`, `ic_online`, `ic_offline`, `ic_user_chat`, `ic_table_chat`.

### Quản trị

`ic_dashboard`, `ic_analytics`, `ic_trending_up`, `ic_orders`, `ic_menu_manage`,
`ic_table_manage`, `ic_people`, `ic_revenue`, `ic_best_seller`, `ic_available`,
`ic_unavailable`, `ic_visibility`, `ic_download`.

## Quy tắc với icon danh mục từ API

Không đổi dữ liệu món ăn/menu hiện tại. App ánh xạ `category.name` hoặc giá trị emoji cũ sang
vector cục bộ (`Phở` → `ic_noodles`, `Đồ uống` → `ic_drink`, `Tráng miệng` →
`ic_dessert`), và dùng `ic_category_restaurant` khi không khớp. Backend về sau nên lưu
`iconKey` ổn định thay vì emoji hoặc URL SVG tùy ý.
