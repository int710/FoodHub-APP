# Ke hoach hoan thien phien dine-in, ban va lich su chat

> Trang thai 2026-09-24: cac hang muc release-critical trong tai lieu nay da duoc
> trien khai. Xem `RELEASE_READINESS.md` de biet checklist deploy va kiem thu.

## 1. Muc tieu

Hoan thien luong dine-in theo tung luot khach:

1. Khach quet QR va nhan mot phien dung bua rieng.
2. Don hang va cuoc tro chuyen thuoc dung phien do.
3. Admin thay duoc chat that, trang thai ban va don hien tai.
4. Khi khach roi di, Admin checkout mot lan de hoan tat don, dong chat, thu hoi token va tra ban.
5. Lich su cu duoc luu lai de tra cuu, nhung khach moi khong the doc du lieu cua khach truoc.

## 2. Hien trang va van de

### Android khach hang

- `TableScanRepository` luu `tableToken`, `tableId`, `tableName` va `conversationId` trong SharedPreferences.
- `ChatSocketClient` gui tin that qua Socket.IO va backend luu tin nhan vao MongoDB.
- Tab Tin nhan chi dua vao viec thiet bi con luu table session.
- Logout tai khoan user chi xoa access/refresh token, khong ket thuc table session.
- Chua co co che tu dong xoa local table session khi token ban bi backend thu hoi.

### Android Admin

- Man hinh Tin nhan van doc `AdminMockData.conversations`.
- Admin chua tai danh sach conversation tu backend.
- Admin chua co Socket.IO client su dung access token Admin de nhan va tra loi tin nhan that.
- Don hang tren Admin van dang dung mock data, nen chua co nut checkout that.

### Backend

- Table session chi ton tai tam thoi trong Redis, TTL 8 gio.
- MongoDB `Conversation` dang dung `customerId`; voi khach tai ban, gia tri nay thuc chat la `tableId`.
- Conversation chua luu `tableSessionId`, `tableId`, `orderId` hoac thoi diem dong.
- Da co `ConversationStatus.OPEN/CLOSED`, nhung chua co nghiep vu dong conversation.
- Don hang co trang thai `COMPLETED`, nhung API moi di den `SERVED`.
- Chua co thao tac checkout de dong phien, thu hoi token va giai phong ban.

## 3. Quy tac nghiep vu de xuat

### Phan biet trang thai ban

- `Table.isActive = false`: ban bao tri hoặc khong duoc phep phuc vu.
- Ban dang co khach khong nen duoc bieu dien bang `isActive`.
- Ban dang co khach khi ton tai mot `TableSession` trang thai `OPEN`.
- Ban trong khi khong co `TableSession.OPEN` va khong co don dine-in dang hoat dong.

### Vong doi phien dine-in

1. Khach quet QR tinh cua ban.
2. Backend kiem tra ban hoat dong va chua bi chiem.
3. Backend tao `TableSession.OPEN`, sinh `sessionId` va table JWT.
4. Moi don dine-in moi phai gan voi `tableSessionId`.
5. Conversation duoc tao theo `tableSessionId`, khong tao truc tiep theo `tableId`.
6. Nhieu lan goi mon trong cung luot khach van thuoc mot table session.
7. Khi thanh toan va ket thuc, Admin thuc hien checkout.
8. Checkout dong table session, conversation va tat ca don hop le cua phien.
9. QR vat ly cua ban khong can doi; khach tiep theo quet se nhan session moi.

### Logout va doi tai khoan

- Logout tai khoan customer khong tu dong checkout ban, vi tai khoan app va phien tai ban la hai phien khac nhau.
- Login Admin tren cung thiet bi khong duoc dung table token cua khach.
- Khu vuc Admin chi dung access token co role `ADMIN` hoac `STAFF`.
- Khi backend bao table token het han/da dong, Android phai xoa `TableSessionStore` va an tab Tin nhan.

### Lich su chat

- Chat dang phuc vu: `Conversation.status = OPEN`.
- Sau checkout: `Conversation.status = CLOSED`, gan `closedAt`, `closedById` va `closeReason`.
- Tin nhan khong bi xoa khi checkout.
- Admin co hai bo loc: `Dang phuc vu` va `Lich su`.
- Conversation da dong chi duoc doc, khong duoc gui them tin.
- Khach moi cua cung mot ban phai nhan conversation moi.

## 4. Mo hinh du lieu de xuat

Nen tao bang PostgreSQL `table_sessions` de co lich su ben vung. Redis chi dung de xac thuc nhanh va quan ly session dang song.

```prisma
enum TableSessionStatus {
  OPEN
  CLOSED
  EXPIRED
}

model TableSession {
  id          String             @id @default(cuid())
  tableId     String
  table       Table              @relation(fields: [tableId], references: [id])
  status      TableSessionStatus @default(OPEN)
  hostKey     String
  openedAt    DateTime           @default(now())
  closedAt    DateTime?
  closedById  String?
  closeReason String?
  orders      Order[]

  @@index([tableId, status])
  @@index([openedAt])
}
```

Bo sung vao `Order`:

```prisma
tableSessionId String?
tableSession   TableSession? @relation(fields: [tableSessionId], references: [id])
```

Bo sung vao MongoDB `Conversation`:

```text
tableId: string | null
tableSessionId: string | null
customerUserId: string | null
orderIds: string[]
closedAt: Date | null
closedById: string | null
closeReason: string | null
```

Tao unique partial index cho mot conversation dang mo tren moi `tableSessionId`.

## 5. API can bo sung

### Table session

```http
POST /api/v1/table/scan
GET  /api/v1/table/sessions/active
GET  /api/v1/table/:tableId/session
POST /api/v1/table/sessions/:sessionId/checkout
```

Payload checkout de xuat:

```json
{
  "reason": "CUSTOMER_LEFT",
  "completeServedOrders": true
}
```

Checkout phai chay theo thu tu:

1. Kiem tra role Admin/Staff.
2. Kiem tra phien con `OPEN`.
3. Kiem tra khong con don chua `SERVED`, `COMPLETED` hoac `CANCELLED`.
4. Kiem tra thanh toan theo quy tac cua nha hang.
5. Chuyen cac don `SERVED` thanh `COMPLETED`.
6. Dong conversation.
7. Xoa Redis `table:session:*`, `table:sessions:*`, `table:host:*` va cart con sot.
8. Dong `TableSession` va ghi nguoi thuc hien.
9. Phat socket event cho khach va Admin.

Toan bo thao tac PostgreSQL can nam trong transaction. Neu viec dong MongoDB/Redis that bai sau transaction, can co retry/idempotency de goi checkout lai an toan.

### Conversation Admin

```http
GET  /api/v1/conversations?status=OPEN&page=1&limit=30
GET  /api/v1/conversations/:id/messages?page=1&limit=50
POST /api/v1/conversations/:id/messages
PATCH /api/v1/conversations/:id/read
PATCH /api/v1/conversations/:id/close
```

Moi endpoint tren chi cho `ADMIN`/`STAFF`, ngoai tru endpoint gui/lay tin nhan cua customer phai kiem tra dung table session hoac user owner.

## 6. Socket event can hoan thien

### Backend phat cho Admin

- `conversation:list`
- `conversation:new`
- `conversation:updated`
- `conversation:closed`
- `message:new`
- `typing:start`
- `typing:stop`
- `table-session:opened`
- `table-session:closed`

### Backend phat cho khach

- `message:new`
- `conversation:closed`
- `table-session:closed`

Khi nhan `table-session:closed`, Android khach phai:

1. Xoa `TableSessionStore`.
2. Ngat socket.
3. An tab Tin nhan.
4. Xoa cart dine-in local neu co.
5. Dua khach ve Home va hien thong bao phien da ket thuc.

## 7. Thay doi Android Admin

1. Xoa `AdminMockData.conversations` khoi luong runtime.
2. Tao `AdminConversationRepository` va `AdminConversationViewModel`.
3. Socket Admin ket noi bang access token trong `TokenStore`, khong dung table token.
4. Khi mo tab Tin nhan, tai conversation `OPEN` tu backend.
5. Cap nhat danh sach theo `conversation:new/updated/closed`.
6. Man chi tiet tai lich su, join room va gui tin bang conversation ID.
7. Them tab/bo loc `Dang phuc vu` va `Lich su`.
8. Hien ten ban, khu vuc, ma don va thoi gian mo phien.
9. Them nut `Ket thuc phien/Tra ban` tai chi tiet ban hoac don.
10. Yeu cau xac nhan truoc checkout va hien ly do neu backend tu choi.

## 8. Thay doi Android khach hang

1. Luu `tableSessionId`, `expiresAt` cung table token.
2. Tab Tin nhan chi hien khi session con hieu luc.
3. Khi socket/API tra `401` do table token het han, xoa local table session.
4. Khi nhan event dong phien, khoa o nhap chat va chuyen cuoc chat sang trang thai chi doc trong thoi gian ngan truoc khi ve Home.
5. Khong xoa table session khi user logout tai khoan thong thuong.
6. Bo sung nut `Roi ban` cho khach neu business cho phep; thao tac nay chi gui yeu cau, khong tu checkout neu con don/thanh toan dang xu ly.

## 9. Quy tac an toan va bao mat

- Khong cho khach truy cap conversation bang `tableId` don thuan.
- Moi request/chat phai xac thuc `tableSessionId` trong JWT va Redis/PostgreSQL.
- Sau checkout, tat ca table JWT cua phien phai bi vo hieu ngay.
- API checkout phai idempotent: goi lai phien da dong tra ve ket qua hien tai, khong tao loi du lieu.
- Khong hard-delete message khi checkout.
- Co chinh sach retention, vi du luu chat 90 ngay, sau do an danh hoa hoac xoa theo yeu cau nghiep vu.
- Ghi audit log nguoi dong phien, thoi gian va ly do.

## 10. Thu tu trien khai khuyen nghi

### Giai doan 1: Backend lifecycle

- Tao `TableSession` trong PostgreSQL.
- Gan Order va Conversation voi table session.
- Them API checkout.
- Dong conversation va thu hoi Redis token khi checkout.
- Them migration va test transaction/idempotency.

### Giai doan 2: Admin chat that

- Them API danh sach/lich su conversation.
- Them socket list/update/close cho host.
- Thay mock conversation trong Android Admin.
- Gui va nhan tin hai chieu end-to-end.

### Giai doan 3: UX tra ban

- Nut checkout tren Admin.
- Trang thai ban trong/dang phuc vu/bao tri.
- Dong session tren Android khach theo socket event.
- Tab lich su chat va man chi tiet chi doc.

### Giai doan 4: Kiem thu va van hanh

- Kiem thu hai khach lien tiep dung cung mot ban.
- Kiem thu nhieu thiet bi trong cung mot table session.
- Kiem thu logout/login customer va login Admin tren cung thiet bi.
- Kiem thu server restart, Redis mat key va checkout duoc goi lai.
- Kiem thu don chua thanh toan, don bi huy va don da served.
- Redeploy backend va kiem tra production truoc khi phat hanh APK.

## 11. Tieu chi nghiem thu

- Tin nhan khach gui xuat hien tren Admin trong thoi gian thuc.
- Admin tra loi va khach nhan duoc ma khong can mo lai man hinh.
- Admin khong con thay conversation mock.
- Checkout bi chan neu con don dang che bien/phuc vu hoac chua du dieu kien thanh toan.
- Checkout thanh cong lam ban trong, dong chat va vo hieu table token cu.
- Khach cu khong gui duoc tin sau checkout.
- Khach moi quet cung QR nhan session va conversation moi.
- Admin van xem duoc chat cu trong Lich su.
- Logout tai khoan customer khong lam mat phien ban khi luot dung bua van con hieu luc.

## 12. Cac file hien tai lien quan

Backend:

- `FoodHub-main/src/services/tables.services.ts`
- `FoodHub-main/src/services/conversation.services.ts`
- `FoodHub-main/src/socket/chat/*.ts`
- `FoodHub-main/src/services/orders.services.ts`
- `FoodHub-main/prisma/schema/table.prisma`
- `FoodHub-main/prisma/schema/order.prisma`

Android:

- `app/src/main/java/com/example/foodhubapp/feature/table/data/TableScanRepository.kt`
- `app/src/main/java/com/example/foodhubapp/feature/table/ChatSocketClient.kt`
- `app/src/main/java/com/example/foodhubapp/feature/table/ChatScreen.kt`
- `app/src/main/java/com/example/foodhubapp/feature/table/AdminShell.kt`
- `app/src/main/java/com/example/foodhubapp/feature/table/screen/AdminScreens.kt`
