# FoodHub API - Ghi chu tam

Ngay doi chieu Swagger: 2026-09-18.

- Swagger: https://foodhub-8lv1.onrender.com/api-docs/
- OpenAPI JSON: https://foodhub-8lv1.onrender.com/api-docs.json
- Base URL: https://foodhub-8lv1.onrender.com/api/v1
- Pham vi: man hinh chi tiet mon va xac nhan don hang cho CUSTOMER.

## Xac thuc va ngu canh

- User: header `Authorization: Bearer <access_token>`.
- Phien ban QR: header `X-Table-Token: <table_token>`.
- `{type}`: `DINE_IN`, `TAKEAWAY`, `DELIVERY`.
- Swagger mo ta BearerAuth hoac TableTokenAuth cho cart/tao don, nhung chua ghi ro moi quy tac theo tung type. Can kiem tra them source/response thuc te.
- Moi type dung cart rieng; khong tron gio khi chuyen phuong thuc dat mon.

## Menu va chi tiet mon

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| GET | `/menu/all` | Menu cong khai, theo danh muc |
| GET | `/menu/categories` | Danh sach danh muc |
| GET | `/menu/item/{id}` | Chi tiet mon va nhom tuy chon |
| GET | `/reviews/items/{menuItemId}` | Danh gia mon; query `page`, `limit` |

Response chi tiet mon da kiem tra cong khai co `name`, `description`, `basePrice`, `image`, `isAvailable`, `isFeatured`, `totalOrder`, `avgRating`, `variantGroups`, `reviews`, `flashSale`.

- Nhom tuy chon: `name`, `type` (`SINGLE`/`MULTIPLE`), `isRequired`, `options`.
- Option: `id`, `name`, `priceAdd`, `isActive`.
- Dung radio cho SINGLE va checkbox cho MULTIPLE; chi hien nhom API tra ve.
- Burger thuc te co nhom "Them mon", khong co nhom kich co. Tra dao thuc te khong co nhom tuy chon.
- Menu list co `salePrice`, `salePercent`, `saleEndsAt`; khong mac dinh response chi tiet cung co cac field nay. Chi tiet da kiem tra co `flashSale`.
- `isFeatured` phu hop nhan "Noi bat", khong tu dong dong nghia "Best Seller".

## Cart

| Method | Endpoint | Muc dich / Body |
| --- | --- | --- |
| GET | `/cart/{type}/items` | Lay gio hang |
| POST | `/cart/{type}/items/add` | Them mon; `menuItemId`, `quantity`, `variantOptionIds`, `note` |
| PATCH | `/cart/{type}/items/{itemId}` | Sua `quantity`, `variantOptionIds`, `note`; it nhat mot field |
| DELETE | `/cart/{type}/items/{itemId}` | Xoa dong gio hang |
| DELETE | `/cart/{type}/clear` | Xoa toan bo gio |

`itemId` trong PATCH/DELETE la ID dong cart, khong phai menuItemId.

```json
{
  "menuItemId": "menu-item-id",
  "quantity": 2,
  "variantOptionIds": ["option-id"],
  "note": "Khong hanh tay"
}
```

- Bat buoc khi them: `menuItemId`, `quantity`.
- Quantity: so nguyen tu 1 den 99.
- Ghi chu mon: toi da 255 ky tu.
- Loi can xu ly: 400 ngu canh/type khong hop le, 401 thieu xac thuc (GET), 404 mon/dong cart khong ton tai, 422 validation.

## Ban va QR

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| POST | `/table/scan` | Gui `{ "qrToken": "..." }` de tao phien ban |
| GET | `/table/{id}` | Lay thong tin ban |

POST scan mo ta 200 co the la tao phien thanh cong HOAC ban dang bi chiem; phai doc payload, khong chi dua vao HTTP 200. QR khong hop le: 404.

Khong dung cac API admin tao ban, tao/regenerate QR, toggle ban cho CUSTOMER. Chua co endpoint chuyen ban duoc tai lieu hoa; khong tu sua tableId de doi ban.

## Tao don

| Method | Endpoint | Muc dich |
| --- | --- | --- |
| POST | `/order/{type}/new` | Tao don tu cart cua ngu canh hien tai |
| GET | `/order/history` | Lich su don, can Bearer token |
| PATCH | `/order/{id}/cancel` | Huy don voi `{ "reason": "..." }`; quyen/trang thai do backend kiem tra |

```json
{
  "paymentMethod": "VNPAY",
  "note": "Ghi chu chung cua don",
  "deliveryInfo": {
    "fullName": "Nguoi nhan",
    "phone": "0900000000",
    "address": "Dia chi giao hang",
    "note": "Ghi chu giao hang"
  }
}
```

- Body CreateOrderRequest tai lieu hoa `note`, `paymentMethod`, `deliveryInfo`; khong co danh sach mon trong body. Khong tu them `items` khi tao don.
- `deliveryInfo` dung cho DELIVERY. Swagger chua khai bao ro cac field bat buoc; can kiem tra validation thuc te.
- Phuong thuc UI nen bat: `CASH`, `VNPAY`.
- Response 200 tao don VNPay duoc mo ta co `paymentUrl`.
- Loi tao don: 400 ngu canh khong hop le; 401 thieu xac thuc/phien ban; 409 ban co don cho xac nhan.
- Huy don: reason 1-255 ky tu; 400 khong the huy, 403 khong co quyen, 409 don da thanh toan can hoan tien truoc.
- History query: `page`, `limit`, `status`, `type`, `from`, `to`, `tableId`, `customerId`. Co query khong dong nghia CUSTOMER duoc xem don nguoi khac.

## Thanh toan

| Method | Endpoint | Muc dich / Body |
| --- | --- | --- |
| POST | `/payment/vnpay/create` | Tao URL VNPay cho don da co; `{ "orderCode": "..." }` |
| GET | `/payment/{orderId}` | Lay thong tin thanh toan |
| GET | `/payment/vnpay/return` | Xu ly browser quay ve tu VNPay |
| GET | `/payment/vnpay/ipn` | Callback VNPay goi backend; app KHONG tu goi |

- Tao don VNPAY -> neu response co paymentUrl thi mo URL do, khong goi create thanh toan lap lai.
- API create thanh toan nhan orderCode; API lay thanh toan nhan orderId. Khong nham hai gia tri.
- Sau khi browser quay lai, doc trang thai thanh toan tu backend truoc khi bao thanh cong.
- Return co the tra JSON 200 hoac redirect 302 den frontend tuy cau hinh FE_URL.
- CASH: tao don va hien cho quan xac nhan; khong bao da thanh toan.
- `PATCH /payment/{orderId}/cash-confirm` danh cho STAFF/ADMIN, khong cho CUSTOMER.
- Confirm/reject/serve don va cap nhat trang thai bep cung la thao tac STAFF/ADMIN.

## Thiet ke nen giu / tam an

- Giu chi tiet mon, tuy chon dong, ghi chu, so luong, chia se Android va them gio.
- Xac nhan don: danh sach mon, sua/xoa, ghi chu chung, ngu canh dat mon, thong tin giao hang, CASH/VNPAY.
- Tam an yeu thich, voucher, uu dai VNPay 20k, MoMo/ZaloPay, hang Gold va doi ban truc tiep: chua co endpoint/luong tuong ung trong Swagger.
- Enum co MOMO khong chung minh da tich hop thanh toan MoMo; khong co ZALOPAY trong enum da doc.
- Chip nguyen lieu chua co field rieng trong response mon da kiem tra.
- Khong hardcode VAT 8%, discount hoac tong tien. Swagger chua mo ta API quote/preview tien truoc khi tao don.
- Sua du lieu Figma: 278000 + 145000 + 90000 = 513000, khong phai tam tinh 475000.
- Ma don chi hien sau khi backend tao don.
- Can trang thai loading, error/retry, mon het, thieu tuy chon bat buoc, gio trong, QR het han va thanh toan huy/that bai.

## Gioi han can xac minh lan sau

### Tich hop Android hien tai

- MenuRoute -> GET /menu/all -> chon ID mon -> food_detail/{foodId}.
- FoodDetailViewModel -> RemoteFoodRepository -> GET /menu/item/{id} -> StateFlow -> FoodDetailScreen.
- Anh that tai bang Coil; PreviewBurger chi dung Preview/test, khong dung trong route chay that.
- Co phien QR: them vao `/cart/DINE_IN/items/add` voi `X-Table-Token`; khong co phien QR: dung TAKEAWAY va Bearer token.
- Khach chua dang nhap hoac token het han: mo dialog dang nhap; dang nhap tu chi tiet mon se quay lai mon, nguoi dung bam them lai.
- DINE_IN da noi tu route `scan_table` -> `POST /table/scan` -> luu table token -> menu/cart/order. DELIVERY chua co UI chon ngu canh.
- Login duoc chinh theo contract /user/login voi email/password; chua khang dinh backend ho tro dang nhap so dien thoai.
- Gia flash sale hien thi chi khi isActive va nam trong startsAt/endsAt. Cach lam tron gia sale can doi chieu backend; backend van quyet dinh gia gio/don.
- Ngay tich hop 2026-09-18: server tra trang Cloudflare cho GET cong khai, chua the kiem thu live. Contract, request body/header va xu ly loi duoc kiem thu bang MockWebServer.

### Tich hop gio hang Android (cap nhat 2026-09-19)

Nhom API cart TAKEAWAY da duoc noi day du vao app:

| Chuc nang | Endpoint | Noi trien khai |
| --- | --- | --- |
| Lay gio hang | `GET /cart/TAKEAWAY/items` | `feature/cart/data/CartRepository.kt` |
| Them mon | `POST /cart/TAKEAWAY/items/add` | `feature/menu/data/FoodRepository.kt` |
| Sua dong gio | `PATCH /cart/TAKEAWAY/items/{itemId}` | `feature/cart/data/CartRepository.kt` |
| Xoa dong gio | `DELETE /cart/TAKEAWAY/items/{itemId}` | `feature/cart/data/CartRepository.kt` |
| Xoa toan bo | `DELETE /cart/TAKEAWAY/clear` | `feature/cart/data/CartRepository.kt` |

Luu y khi doc va sua code:

- `itemId` cua PATCH/DELETE la `CartItem.id`; khong dung `menuItemId`.
- `CartRepository.kt` chua model, parser JSON va request GET/PATCH/DELETE.
- `CartViewModel.kt` quan ly loading, loi, dang nhap, tang/giam so luong, sua option/ghi chu, xoa mot dong va xoa toan bo.
- `CartScreen.kt` chua `CartRoute`, UI danh sach, hop thoai sua mon va hop thoai xac nhan xoa.
- Route `cart` duoc khai bao trong `navigation/AppRoutes.kt` va noi trong `navigation/AppNavGraph.kt`.
- Menu va man chi tiet mon deu co nut mo gio hang.
- Dang nhap duoc goi tu gio hang se quay lai route `cart` sau khi thanh cong.
- `FoodHubApiClient` dung OkHttp de ho tro on dinh GET/POST/PATCH/DELETE va response `204 No Content`.

Quy tac sua dong gio:

- Body PATCH phai co it nhat mot trong `quantity`, `variantOptionIds`, `note`.
- `quantity` trong khoang 1..99; UI khong tu dong xoa khi giam xuong 0.
- `note` toi da 255 ky tu.
- Khi sua option, app tai lai chi tiet mon de hien cac nhom SINGLE/MULTIPLE va kiem tra nhom bat buoc.
- Sau PATCH/DELETE thanh cong, ViewModel GET lai cart; tong tien hien thi theo response backend, khong hardcode VAT/discount.
- Cart DINE_IN dung `X-Table-Token`; cart TAKEAWAY dung `Authorization: Bearer <access_token>`.

Kiem thu da co:

- `app/src/test/java/com/example/foodhubapp/CartRepositoryTest.kt` kiem tra endpoint, HTTP method, Bearer token, body PATCH va parse response.
- `./gradlew testDebugUnitTest` thanh cong ngay 2026-09-19 (13 tests).
- `./gradlew assembleDebug` thanh cong; APK debug o `app/build/outputs/apk/debug/app-debug.apk`.

Gioi han con lai:

- Chua kiem thu cart live voi access token CUSTOMER that; hien contract duoc kiem thu bang MockWebServer.
- Parser cart ho tro `data` la array hoac object co `items`/`cartItems`, menu item long hoac field phang, va option dang object hoac ID. Khi co response live, doi chieu va thu gon parser theo payload chinh thuc.
- App tu chon DINE_IN khi con phien QR hop le, neu khong se dung TAKEAWAY. DELIVERY van can luong nhap thong tin giao hang.
- Table token het han duoc xoa va UI yeu cau quet lai QR, khong doi no thanh Bearer token.

### Tich hop Home tu Figma (cap nhat 2026-09-19)

- Figma source: file `1Zjq09Ja7RnHwFjQmIvJc6`, node `7:466` (Trang Chu - Kham Pha & Dat Mon).
- UI Compose nam tai `feature/home/ui/HomeScreen.kt`; state va goi API nam tai `feature/home/viewmodel/HomeViewModel.kt`.
- Home dung `GET /menu/all` de hien danh muc, tim kiem local, mon noi bat/ban chay va goi y mon.
- `MenuFood` da bo sung `description`, `avgRating`, `isFeatured`, `totalOrder`, `isAvailable` tu response menu.
- Home dung giỏ DINE_IN khi co table token, neu khong dung TAKEAWAY khi co access token; khach chua xac thuc van xem duoc menu.
- Bam mon tren Home mo `food_detail/{foodId}`; bam gio mo route `cart`; bam avatar/bottom nav Ca nhan mo `profile`.
- Splash da dang nhap, dang nhap thanh cong va dang ky thanh cong deu vao route `home` thay vi `profile`.
- Anh Figma da duoc luu cuc bo trong `res/drawable-nodpi/home_figma_*.jpg` lam banner/avatar/fallback khi API khong co anh.
- Banner khuyen mai VNPAY tren Figma duoc thay bang banner kham pha menu, vi API hien tai khong co contract voucher/promo cho Home.
- Hai nut QR tren Home va nut khach vang lai o Login deu mo camera scanner that; DELIVERY, voucher va goi phuc vu van chua noi.
- Test: `HomeUiStateTest.kt` kiem tra loc/tim kiem/featured; `HomeScreenTest.kt` kiem tra cac section chinh va render tren emulator.

### Gioi han chung cua tai lieu

- Nhieu response dung `ApiResponse` voi `message`, `data`, `pagination`; `data` chua khai bao schema cu the theo endpoint.
- Chua thu tao don/thanh toan voi CUSTOMER dang nhap. Chua tao don hoac thanh toan trong lan doi chieu nay.
- Can xac minh payload cart, cach tinh gia sale + option + VAT, quyen theo tung type, va trang thai payment thuc te.

### Tich hop tab Don hang tu Figma node 88:2 (cap nhat 2026-09-19)

Tab Don hang da duoc noi vao bottom navigation cua Home va route `orders`:

| Chuc nang | Endpoint | Noi trien khai |
| --- | --- | --- |
| Lay lich su, loc type, phan trang | `GET /order/history` | `feature/order/data/OrderRepository.kt` |
| Huy don kem ly do | `PATCH /order/{id}/cancel` | `OrderRepository.kt`, `OrderListViewModel.kt` |
| Thanh toan lai don cho VNPay | `POST /payment/vnpay/create` | `OrderRepository.kt`, `OrderListScreen.kt` |

- `OrderListScreen.kt` giu style cua Home/Figma: 3 tab trang thai, filter type cuon ngang, card don, mo rong mon, tong tien va bottom navigation.
- Ba nhom UI duoc loc tu status API: dang xu ly, hoan thanh va da huy. Khong hien count tren tab vi API chua co summary count; count tren mot trang khong phai tong lich su.
- Thanh tien, mon, ban, delivery info va payment status uu tien doc tu response backend. Parser tam thoi chap nhan `data` dang array hoac object `orders/items/results` vi Swagger chua dinh nghia schema cu the.
- Tien do bep chi the hien cac moc status `CONFIRMED -> PREPARING -> READY -> SERVED`; khong gia lap phan tram hoac thoi gian du kien.
- `Huy don` chi duoc de xuat tren UI cho `PENDING_PAYMENT`, `PENDING_CONFIRMATION`, `CONFIRMED`; backend van la noi quyet dinh va co the tra 400/403/409.
- `Thanh toan ngay` chi hien cho `PENDING_PAYMENT`, gui `orderCode` den VNPay create va mo `paymentUrl` server tra ve.
- Bottom navigation da doi `Tin nhan` thanh `Thong bao`; man thong bao rieng chua duoc trien khai trong feature nay.
- Thu tu doc code: `OrderRepository.kt` -> `OrderListViewModel.kt` -> `OrderListScreen.kt` -> `AppNavGraph.kt`.
- Khong ket luan mot tinh nang khong ton tai chi vi Swagger khong ghi; hien tai nen an tren UI cho den khi co contract/source ro rang.
