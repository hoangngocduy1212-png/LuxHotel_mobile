# LuxHotel Backend

Backend REST API (Node.js + Express + MySQL) cho ứng dụng Android LuxHotel.
Schema được thiết kế **khớp 1-1** với `DatabaseHelper.kt` (SQLite) hiện tại của bạn — cùng tên cột, cùng khóa chính (`username`, `room_number`), cùng cách hash mật khẩu (SHA-256), cùng quy ước trạng thái (`available/occupied/maintenance/booked` chữ thường cho phòng, `BOOKED/CHECKED_IN/CHECKED_OUT/CANCELLED` chữ hoa cho booking).

## 1. Cài đặt

### Yêu cầu
- Node.js >= 18
- MySQL 8.0 (có thể dùng XAMPP/WAMP nếu bạn quen thuộc, hoặc cài MySQL Server/Workbench riêng)

### Các bước
```bash
cd luxhotel-backend
npm install
cp .env.example .env
```

Mở file `.env` và điền thông tin MySQL thật của bạn:
```
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=<mật khẩu MySQL của bạn>
DB_NAME=luxhotel
JWT_SECRET=<chuỗi bí mật bất kỳ, càng dài càng tốt>
```

Tạo database và bảng:
```bash
mysql -u root -p < sql/schema.sql
```
Lệnh này tự tạo database `luxhotel`, 8 bảng, và seed sẵn 6 tài khoản nhân viên + 15 phòng giống hệt `seedInitialData()` trong Kotlin.

Chạy server:
```bash
npm start
# hoặc npm run dev (tự restart khi sửa code, cần: npm install -g nodemon)
```

Server chạy tại `http://localhost:3000`. Kiểm tra nhanh:
```bash
curl http://localhost:3000/health
```

## 2. Tài khoản mặc định (giống app hiện tại)

| Username | Password | Role |
|---|---|---|
| admin | admin123 | ADMIN |
| receptionist1 / receptionist2 | letan123 | RECEPTIONIST |
| housekeeping1/2/3 | buong123 | HOUSEKEEPING |

## 3. Danh sách API chính

Tất cả API (trừ `/api/auth/login`) yêu cầu header:
```
Authorization: Bearer <token>
```
Token lấy được từ response của `/api/auth/login`.

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | /api/auth/login | Đăng nhập |
| GET | /api/rooms | Danh sách tất cả phòng |
| GET | /api/rooms/available | Phòng còn trống |
| GET | /api/rooms/calendar?date=yyyy-MM-dd | Lịch phòng theo ngày |
| PATCH | /api/rooms/:room_number/status | Đổi trạng thái phòng |
| GET | /api/customers/status | Khách đang ở / sắp đến / đã rời |
| POST | /api/customers | Tạo khách hàng |
| GET | /api/bookings?status=BOOKED | Danh sách booking theo trạng thái |
| POST | /api/bookings | Tạo đặt phòng |
| POST | /api/bookings/:id/checkin | Nhận phòng |
| POST | /api/bookings/:id/checkout | Trả phòng (tự tạo task dọn phòng) |
| POST | /api/bookings/:id/service-charges | Thêm phí dịch vụ |
| GET | /api/housekeeping-tasks?date=&taskType=CLEAN | Nhiệm vụ dọn phòng |
| PATCH | /api/housekeeping-tasks/:id/done | Đánh dấu hoàn thành |
| GET | /api/maintenance-reports | Báo cáo sự cố |
| POST | /api/maintenance-reports | Gửi báo cáo (kèm ảnh, multipart field "image") |
| PATCH | /api/maintenance-reports/:id/status | Cập nhật trạng thái sửa chữa |
| GET | /api/reports/dashboard | Thống kê tổng quan (Admin) |
| GET | /api/reports/revenue?year=&month= | Doanh thu theo tháng (Admin) |

## 4. Tích hợp vào app Android (thay thế SQLite)

Ý tưởng: **giữ nguyên các data class** (`User`, `RoomInfo`, `Customer`, `CheckoutBooking`, `ServiceCharge`, `TaskItem`, `MaintenanceReportItem`...) vì JSON trả về đã được đặt tên field khớp với chúng. Bạn chỉ cần:

1. Thêm Retrofit + Gson vào `build.gradle`:
```gradle
implementation "com.squareup.retrofit2:retrofit:2.11.0"
implementation "com.squareup.retrofit2:converter-gson:2.11.0"
implementation "com.squareup.okhttp3:logging-interceptor:4.12.0"
```

2. Tạo `ApiService.kt` (interface Retrofit) với các hàm tương ứng từng endpoint ở trên, ví dụ:
```kotlin
interface ApiService {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/rooms")
    suspend fun getAllRooms(): List<RoomInfo>

    @PATCH("api/rooms/{room_number}/status")
    suspend fun updateRoomStatus(@Path("room_number") roomNumber: String, @Body body: Map<String, String>)
    // ... tương tự cho các hàm khác trong DatabaseHelper
}
```

3. Tạo `RetrofitClient.kt` gắn base URL:
```kotlin
object RetrofitClient {
    // Máy ảo Android Studio (emulator) gọi localhost máy tính qua địa chỉ đặc biệt 10.0.2.2
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val instance: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor()) // tự gắn "Authorization: Bearer <token>"
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
```
   - Nếu chạy trên **thiết bị thật**, thay `10.0.2.2` bằng địa chỉ IP LAN của máy tính chạy backend (vd `192.168.1.5`), và đảm bảo điện thoại + máy tính cùng mạng Wifi.
   - Nhớ thêm `android:usesCleartextTraffic="true"` trong `AndroidManifest.xml` (vì backend đang chạy HTTP, chưa có HTTPS) khi test local.

4. Thay từng lời gọi `dbHelper.xxx()` bằng lời gọi Retrofit tương ứng (chạy trong coroutine `viewModelScope.launch { ... }` hoặc tương tự), lưu token JWT vào `SharedPreferences` sau khi đăng nhập thành công để dùng cho các request sau.

5. Xóa dần các bảng SQLite (có thể giữ `DatabaseHelper.kt` lại một thời gian để tham chiếu, nhưng ngừng dùng làm nguồn dữ liệu chính).

## 5. Triển khai (deploy) để nhiều máy/điện thoại cùng dùng chung

Để tất cả điện thoại (không chỉ máy bạn) truy cập được, bạn cần đưa backend + MySQL lên một server online, ví dụ:
- **Railway** hoặc **Render** (có gói miễn phí, dễ deploy Node.js + MySQL)
- VPS giá rẻ (Vietnam: Vultr, DigitalOcean, hoặc nhà cung cấp trong nước)

Sau khi deploy, chỉ cần đổi `BASE_URL` trong `RetrofitClient.kt` thành domain/IP public của server.

## 6. Ghi chú bảo mật (để trình bày với giảng viên nếu cần)

- Mật khẩu hiện hash bằng SHA-256 để **giữ tương thích với hệ thống hiện tại** của bạn. Về lâu dài, nên chuyển sang `bcrypt` (có salt, chống rainbow-table tốt hơn) — có thể nêu đây là hướng cải tiến trong báo cáo đồ án.
- Đăng nhập trả về JWT (JSON Web Token) hết hạn sau 7 ngày (`JWT_EXPIRES_IN` trong `.env`), giúp xác thực nhiều thiết bị mà không cần lưu session trên server.
- Phân quyền theo `role` (ADMIN / RECEPTIONIST / HOUSEKEEPING) được kiểm tra ở middleware `authorize()`, khớp với 3 vai trò trong app.
