# Figma to Compose Workflow

Tai lieu nay dung lam checklist moi khi chuyen mot man hinh Figma sang Jetpack Compose trong FoodHubApp.

## 1. Lay thong tin Figma

- Dung link Figma co `node-id`, vi moi man hinh can tro toi dung frame/node.
- Tach `fileKey` va `nodeId` tu URL:
  - `https://www.figma.com/design/{fileKey}/...?node-id=1-2`
  - `node-id=1-2` doi thanh `nodeId=1:2`
- Goi Figma `get_design_context` truoc khi code de lay kich thuoc, mau, text, spacing, asset va anh tham chieu.

## 2. Dat file man hinh

- Tao man hinh moi theo feature:
  - `app/src/main/java/com/example/foodhubapp/feature/{featureName}/ui/{ScreenName}.kt`
- Neu man hinh co state tu API, tao them:
  - `app/src/main/java/com/example/foodhubapp/feature/{featureName}/viewmodel/{ScreenName}ViewModel.kt`
  - `app/src/main/java/com/example/foodhubapp/feature/{featureName}/data/{FeatureName}Repository.kt`
- Code dung chung nhieu feature dat trong:
  - `app/src/main/java/com/example/foodhubapp/core/...`
- Composable public nen dat theo ten man hinh:
  - `OnboardingScreen()`
  - `LoginScreen()`
  - `HomeScreen()`
- `MainActivity.kt` chi nen boot theme, tao `NavController`, va goi `AppNavGraph()`.

## 3. Map design sang codebase

- Tai su dung theme san co:
  - `AppBackground`
  - `Primary`, `PrimaryContainer`
  - `Tertiary`
  - `Neutral`, `OnSurfaceVariant`
  - `BodyFont`, `HeadingFont`
- Moi lan lam man hinh moi, neu Figma co mau, typography, shape, spacing, hoac style co the tai su dung, phai dua vao file theme tuong ung:
  - Mau dung chung: `app/src/main/java/com/example/foodhubapp/ui/theme/Color.kt`
  - Typography/font/text style dung chung: `app/src/main/java/com/example/foodhubapp/ui/theme/Type.kt`
  - Color scheme/theme app: `app/src/main/java/com/example/foodhubapp/ui/theme/Theme.kt`
  - Shape/radius dung chung: tao `Shape.kt` neu project chua co.
  - Spacing/size token dung chung: tao `Spacing.kt` neu can tai su dung nhieu noi.
- Chi khai bao `private val` trong file screen khi token do that su rieng cho mot man hinh va khong co kha nang tai su dung.
- Dung `Column`, `Row`, `Box`, `Surface` de thay cho absolute layout cua Figma.
- Giu spacing theo dp tu Figma, nhung cho phep `Spacer(weight = 1f)` o khu vuc can responsive.

## 4. Asset

- Anh lon tu Figma nen tai ve resource de khong phu thuoc URL tam thoi:
  - anh bitmap dat trong `app/src/main/res/drawable-nodpi`
  - icon vector Android dat trong `app/src/main/res/drawable`
- Uu tien tai asset bitmap tu Figma o dinh dang PNG neu co the, vi giu chat luong on dinh va ho tro alpha tot. Chi dung JPEG khi Figma chi tra ve anh nguon dang JPEG hoac khi can file nhe hon va anh khong can nen trong suot.
- Icon trong Figma nen tai SVG goc ve truoc, sau do chuyen thanh Android VectorDrawable XML trong `res/drawable`. Khong ve icon bang `Canvas` trong screen neu Figma da co icon asset.
- Android khong compile truc tiep file `.svg` trong `res/drawable`; file dua vao app phai la vector XML (`<vector ...>`). Dat ten theo vai tro UI de code ngan gon va de tai su dung.
- Dat ten resource ro nghia, snake_case:
  - `onboarding_hero_foodhub.png`
  - `ic_onboarding_qr.xml`
- Khi dung anh bitmap trong Compose:
  - `painterResource(id = R.drawable.ten_file)`
  - `contentScale = ContentScale.Crop` neu Figma crop anh trong frame.
- Khi dung icon trong Compose:
  - `Image(painter = painterResource(id = R.drawable.ic_ten_icon), contentDescription = null)`
  - Dat `contentDescription` khi icon la hanh dong/doc lap can accessibility label.

## 5. Preview va kiem tra

- Moi screen nen co preview dung kich thuoc frame Figma:
  - `@Preview(showBackground = true, widthDp = 390, heightDp = 948)`
- Sau khi code xong, chay:
  - `.\gradlew.bat assembleDebug`
- Neu man hinh co text dai, kiem tra `maxLines`, `overflow`, va cac `Spacer` de tranh bi tran tren may nho.

## 6. Kien truc MVVM

- Moi man hinh nen chia 2 lop composable:
  - `{ScreenName}Route()` noi ViewModel voi UI, collect state, xu ly navigation/callback.
  - `{ScreenName}Screen(uiState, ...)` chi render UI va co preview doc lap.
- Khong hard-code data API trong composable. UI chi nhan data tu `UiState`.
- Pattern chuan cho data:
  - Repository expose data dang `Flow`, `suspend fun`, hoac paging source tuy tinh nang.
  - `ViewModel` collect repository va expose `StateFlow<UiState>`.
  - Screen route dung `collectAsStateWithLifecycle()`.
  - UI composable chi nhan `uiState` va render.
- `ViewModel` khong import Compose UI (`Modifier`, `Color`, `@Composable`, resource UI...). ViewModel chi giu state, logic man hinh, va goi use case/repository.
- Repository khong biet UI. Repository chi phu trach data source: API, local database, Socket.IO, Firebase, cache.
- Khi co API that, tao interface repository truoc, implementation sau. Vi du:
  - `interface SplashLoadingRepository`
  - `class RemoteSplashLoadingRepository(...) : SplashLoadingRepository`

## 7. Nguyen ly OOP/SOLID

- Single Responsibility: moi class/file co mot nhiem vu ro rang. Screen render UI, ViewModel quan ly state, Repository lay data, Theme giu design token.
- Open/Closed: khi them API moi hoac data source moi, uu tien them implementation moi cua interface thay vi sua nhieu noi UI.
- Liskov Substitution: moi implementation cua mot interface phai thay the duoc nhau ma khong lam hong ViewModel. Vi du demo repository va remote repository cung tra ve cung contract.
- Interface Segregation: tach interface nho theo feature, khong tao mot repository qua lon chua moi API cua app.
- Dependency Inversion: ViewModel nen phu thuoc vao abstraction/interface, khong phu thuoc truc tiep vao Retrofit service hoac Socket client.
- Tranh dat business logic trong composable. Neu logic anh huong data, trang thai, loading, error, retry, navigation condition thi dua vao ViewModel/use case.
- Tranh duplicate token UI. Mau/font/radius/spacing lap lai tu 2 man hinh tro len thi dua vao theme.

## 8. Splash loading va API

- Splash nen don gian va nhanh: uu tien loading local 1-2 giay, khong goi API nang neu du lieu do chua can hien thi ngay.
- Khong dung `/menu/categories` hoac `/menu/all` cho splash chi de lam progress bar, vi day la data cua man Menu/Home.
- Splash van nen dung MVVM:
  - `FoodHubSplashRoute()` noi ViewModel voi UI.
  - `FoodHubSplashViewModel` expose `StateFlow<FoodHubSplashUiState>`.
  - `LocalSplashLoadingRepository` tao progress local ngan gon.
- API dung chung dat trong:
  - `app/src/main/java/com/example/foodhubapp/core/network/FoodHubApiClient.kt`
- API menu nen de trong repository rieng cho man Menu/Home:
  - `MenuRepository`
  - `RemoteMenuRepository`
- Voi FoodHub API hien tai:
  - Base URL that: `https://foodhub-8lv1.onrender.com/api/v1`
  - Menu/Home co the goi `GET /menu/categories` va `GET /menu/all` thong qua `RemoteMenuRepository`.
  - API tra ve dang `ApiResponse { message, data, pagination }`, repository parse `message` va `data`.
- Chi tao API loading/bootstrap rieng khi backend co endpoint nhe nhu `GET /health`, `GET /app/config`, hoac `GET /app/bootstrap`.
- Nen animate progress bang `animateFloatAsState()` de moi lan API tra progress moi, thanh loading chay muot.

## 9. Navigation Compose

- `MainActivity.kt` chi tao `rememberNavController()` va goi `AppNavGraph()`.
- Khai bao route tap trung trong:
  - `app/src/main/java/com/example/foodhubapp/navigation/AppRoutes.kt`
- Khai bao graph tap trung trong:
  - `app/src/main/java/com/example/foodhubapp/navigation/AppNavGraph.kt`
- Khong truyen `NavController` vao screen UI. Screen chi nhan callback:
  - `onStartClick`
  - `onLoginClick`
  - `onBack`
  - `onOpenMenu`
- Route composable xu ly navigation/callback. Vi du splash:
  - `FoodHubSplashRoute(onSplashFinished = { navController.navigate(...) })`
- Khi chuyen khoi splash/login, dung `popUpTo(...){ inclusive = true }` de khong quay lai man trung gian bang nut back.
- Man chua co design co the dung placeholder tam thoi, nhung khi co design phai tach thanh screen/route rieng theo MVVM.

## 10. Mau cau truc file

```kotlin
@Composable
fun ExampleRoute(
    viewModel: ExampleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExampleScreen(
        uiState = uiState,
        onRetry = viewModel::retry
    )
}

@Composable
fun ExampleScreen(
    uiState: ExampleUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Render uiState here.
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 948)
@Composable
private fun ExampleScreenPreview() {
    FoodHubAppTheme {
        ExampleScreen(
            uiState = ExampleUiState(),
            onRetry = {}
        )
    }
}
```
