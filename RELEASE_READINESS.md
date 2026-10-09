# FoodHub Release Readiness

Updated: 2026-09-24

## Implemented end-to-end

- Customer authentication: register, login, logout, refresh token, forgot/reset password, verify email, view and update profile.
- Public menu: categories, detail, flash-sale price, variants, ratings and recent reviews.
- Cart and checkout: dine-in, takeaway and delivery; cash or VNPay; edit/delete/clear cart.
- Dine-in identity: requests send both table token and user token when available, so the table session and customer order history remain linked.
- Customer orders: history, filters, cancel, retry VNPay and review every completed menu item.
- Admin orders: list/detail, cash confirmation, confirm/reject, kitchen item workflow, serve and complete.
- Tables: list/create, enable/disable, QR display/regeneration, scan and explicit end-session.
- Chat: customer table-session chat, admin conversation list/detail/reply/close and persisted history.
- Notifications: unread state, mark one/all as read and navigate to related orders.
- Admin menu: category/item CRUD, availability, flash sale, create variant groups and image upload.
- Realtime: chat events and admin order refresh on new/status/item Socket.IO events.
- Table lifecycle: completed/cancelled/payment-failed orders no longer keep a table occupied; the final table session closes chat and clears the shared cart.

## Verification completed

- Android `testDebugUnitTest`: 19 tests passed.
- Android `assembleDebug`: passed.
- Android `assembleRelease`: passed.
- Backend `npm run build`: passed.
- Backend `npm run lint`: 0 errors; the repository still has pre-existing formatting warnings.

Generated release artifact:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

It is intentionally unsigned. A private release keystore must never be committed to Git.

## Required before production deployment

1. Push the backend commit and redeploy the Render service. Android changes alone cannot add the new server endpoints.
2. Configure Render environment variables:

```text
NODE_ENV=production
DATABASE_URL_POSTGRESQL=...
REDIS_URL=...
MONGO_DB_USER=...
MONGO_DB_PASS=...
SECRET_ACCESS_TOKEN=...
SECRET_REFRESH_TOKEN=...
SECRET_VERIFY_EMAIL=...
SECRET_FORGOT_PASSWORD=...
SECRET_TABLE_TOKEN=...
CLIENT_URL=https://<public-table-qr-host>
APP_DEEP_LINK_URL=foodhub://
VNPAY_TMN_CODE=...
VNPAY_HASH_SECRET=...
VNPAY_RETURN_URL=https://foodhub-8lv1.onrender.com/api/v1/payment/vnpay/return
ResendAPIKey=...
From_Send_Email=...
R2_URL_ENDPOINT=...
R2_ACCESS_KEY_ID=...
R2_SECRET_ACCESS_KEY=...
R2_NAME_BUCKET=...
```

`CLIENT_URL` controls the URL encoded in each table QR. `APP_DEEP_LINK_URL=foodhub://` controls email verification, password reset and VNPay result links that reopen Android. After changing `CLIENT_URL`, regenerate each table QR from the Admin app.

3. Run the production database deployment command configured for Prisma migrations before starting the new backend.
4. Create a release keystore, configure Android signing locally/CI, increment `versionCode`, then generate a signed AAB for Play Console.
5. Smoke-test on a real device against Render: login, scan QR, create all three order types, cash/VNPay, Admin processing, chat, session end, notification navigation, review and image upload.

## External limitations

- DBeaver is not required by the app or deployment; it is only a database administration tool.
- VNPay, email and R2 upload require valid provider credentials and public callback URLs.
- The generated APK cannot be uploaded to a store until it is signed. The keystore/password must be supplied by the owner or CI secret store.
