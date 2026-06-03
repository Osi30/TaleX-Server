# TaleX-Server — Hướng dẫn chạy Docker (WSL Ubuntu)

> Hướng dẫn dành cho dev chạy PostgreSQL container phục vụ phát triển local.
> Setup: **WSL Ubuntu + Docker Engine** (không dùng Docker Desktop).

---

## Mở Ubuntu Terminal

Mở **Windows Terminal** → bấm mũi tên `v` cạnh dấu `+` → chọn **Ubuntu**.

---

## Khởi động Docker + Vào thư mục project

Mỗi lần mở Ubuntu mới, chạy 2 lệnh này:

```bash
service docker start
cd /mnt/c/TaleX/TaleX-Server
```

Kiểm tra Docker đang chạy:

```bash
docker ps
```

> Nếu thấy bảng trống (không có container nào) là Docker đã sẵn sàng.
> Nếu báo lỗi `Cannot connect to the Docker daemon` → chạy lại `service docker start`.

---

## Chạy container

### Chạy chỉ PostgreSQL (dùng khi dev local với IntelliJ/Maven)

```bash
docker compose up db -d
```

- Chạy PostgreSQL 16 ở background
- App Spring Boot bạn chạy riêng bằng IntelliJ hoặc `./mvnw spring-boot:run` trên Windows

### Chạy cả PostgreSQL + Spring Boot app

```bash
docker compose up -d
```

- Chạy cả 2 service: `db` (PostgreSQL) + `app` (Spring Boot)
- App tự build từ Dockerfile, tự connect tới database

### Chạy cả 2 nhưng rebuild app (khi sửa code)

```bash
docker compose up --build -d
```

---

## Tắt container

### Tắt tất cả container

```bash
docker compose down
```

### Tắt chỉ app, giữ database chạy

```bash
docker compose stop app
```

### Tắt chỉ database, giữ app chạy

```bash
docker compose stop db
```

### Tắt tất cả + xóa data database (reset sạch)

```bash
docker compose down -v
```

> **Cẩn thận:** `-v` xóa toàn bộ data trong PostgreSQL. Chỉ dùng khi muốn reset database về trống.

---

## Khởi động lại container đã tắt

### Bật lại container đã stop (không cần rebuild)

```bash
docker compose start db
```

```bash
docker compose start app
```

```bash
docker compose start
```

---

## Xem trạng thái + log

### Xem container đang chạy

```bash
docker ps
```

### Xem log

```bash
# Log tất cả
docker compose logs -f

# Log chỉ app
docker compose logs -f app

# Log chỉ database
docker compose logs -f db
```

> Bấm `Ctrl+C` để thoát xem log.

---

## Kết nối Database từ tool (DataGrip, DBeaver, pgAdmin)

| Thông tin | Giá trị |
|-----------|---------|
| Host | `localhost` |
| Port | `5432` |
| Database | `talex-db` |
| Username | `postgres` |
| Password | `12345` |

---

## Truy cập API (khi chạy cả app)

| URL | Mô tả |
|-----|-------|
| http://localhost:8080 | API endpoint |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| http://localhost:8080/api-docs | OpenAPI JSON |

---

## Tóm tắt lệnh nhanh

| Muốn làm gì | Lệnh |
|-------------|------|
| Bật Docker | `service docker start` |
| Vào project | `cd /mnt/c/TaleX/TaleX-Server` |
| Chạy DB | `docker compose up db -d` |
| Chạy tất cả | `docker compose up -d` |
| Rebuild + chạy | `docker compose up --build -d` |
| Tắt tất cả | `docker compose down` |
| Tắt chỉ app | `docker compose stop app` |
| Tắt chỉ DB | `docker compose stop db` |
| Bật lại DB | `docker compose start db` |
| Reset DB sạch | `docker compose down -v` |
| Xem container | `docker ps` |
| Xem log app | `docker compose logs -f app` |

---

## Xử lý lỗi thường gặp

### Port 5432 đã bị chiếm

```
Error: bind: address already in use
```

PostgreSQL đang chạy trên Windows. Tắt nó:

```bash
# Chạy trên PowerShell (Windows), không phải Ubuntu
net stop postgresql-x64-16
```

Hoặc đổi port trong `docker-compose.yml`:

```yaml
ports:
  - "5433:5432"    # dùng port 5433
```

### Docker daemon chưa chạy

```
Cannot connect to the Docker daemon
```

Chạy lại:

```bash
service docker start
```

### REDIS_PASSWORD is required

```
error while interpolating: required variable REDIS_PASSWORD is missing
```

Cần có file `.env` trong thư mục project. Tạo từ template:

```bash
cp .env.example .env
# Sau đó điền password thật vào file .env
```
