# TaleX-Server — Hướng dẫn chạy bằng Docker

> Hướng dẫn dành cho lập trình viên mới tham gia dự án. Chạy Spring Boot + PostgreSQL bằng Docker, không cần cài Java hay PostgreSQL trên máy.

---

## Yêu cầu

Cài sẵn trên máy:

| Tool | Kiểm tra | Tải về |
|---|---|---|
| **Docker Desktop** | `docker --version` | https://www.docker.com/products/docker-desktop |
| **Git** | `git --version` | https://git-scm.com |

> Mở Docker Desktop và đảm bảo nó đang chạy trước khi tiếp tục.

---

## Bước 1: Clone repo

```bash
git clone <repo-url>
cd TaleX-Server
```

---

## Bước 2: Chạy app + database

```bash
docker-compose up --build
```

Lệnh này sẽ tự động:
1. Build Docker image cho Spring Boot app (lần đầu mất ~3-5 phút)
2. Kéo image PostgreSQL 16 về (lần đầu mất ~1-2 phút)
3. Khởi động PostgreSQL, chờ health check OK
4. Khởi động Spring Boot app, kết nối database

Khi thấy log hiện dòng tương tự:

```
app-1  | Started TalexServerApplication in X.XXX seconds
```

App đã chạy thành công.

---

## Bước 3: Truy cập

| URL | Mô tả |
|---|---|
| http://localhost:8080 | API endpoint |
| http://localhost:8080/swagger-ui.html | Swagger UI (xem + test API) |
| http://localhost:8080/api-docs | API docs (JSON format) |

---

## Thông tin kết nối Database

Dùng DataGrip, DBeaver hoặc tool tương tự để kết nối:

| Thông tin | Giá trị |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `talex-db` |
| Username | `postgres` |
| Password | `12345` |

---

## Các lệnh thường dùng

### Chạy ở chế độ nền (background)

```bash
docker-compose up --build -d
```

### Xem log khi chạy nền

```bash
# Xem log tất cả services
docker-compose logs -f

# Xem log chỉ app
docker-compose logs -f app

# Xem log chỉ database
docker-compose logs -f db
```

### Tắt app + database

```bash
docker-compose down
```

### Tắt và xóa luôn data database (reset sạch)

```bash
docker-compose down -v
```

> **Lưu ý:** `-v` sẽ xóa volume chứa data PostgreSQL. Dùng khi muốn reset database về trạng thái trống.

### Rebuild khi sửa code

```bash
# Cách 1: Tắt rồi chạy lại
docker-compose down
docker-compose up --build

# Cách 2: Rebuild chỉ app (nhanh hơn, database vẫn chạy)
docker-compose up --build app
```

---

## Cấu trúc Docker trong project

```
TaleX-Server/
├── Dockerfile              ← Đóng gói Spring Boot app thành Docker image
├── .dockerignore           ← Loại trừ file không cần khi build image
├── docker-compose.yml      ← Chạy app + PostgreSQL cùng lúc
└── src/
    └── main/resources/
        └── application.yaml  ← Config database đọc từ environment variables
```

### Dockerfile — Multi-stage build

```
Stage 1 (build): JDK 21 + Maven → compile source → tạo JAR
Stage 2 (run):   JRE 21 → chỉ chạy JAR → image nhẹ
```

### docker-compose.yml — 2 services

```
services:
  app  → Spring Boot (port 8080), build từ Dockerfile
  db   → PostgreSQL 16 (port 5432), dùng image có sẵn
```

### application.yaml — Environment variables

```yaml
url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:talex-db}
username: ${DB_USERNAME:postgres}
password: ${DB_PASSWORD:12345}
```

- Chạy bằng Docker Compose: đọc env vars từ `docker-compose.yml` (DB_HOST=db)
- Chạy local không Docker: dùng giá trị mặc định (DB_HOST=localhost)

---

## Xử lý lỗi thường gặp

### Lỗi: Port 5432 đã bị chiếm

```
Error: bind: address already in use
```

Nguyên nhân: PostgreSQL đang chạy trên máy local, chiếm port 5432.

Cách sửa:
```bash
# Tắt PostgreSQL local (Windows)
net stop postgresql-x64-16

# Hoặc đổi port trong docker-compose.yml
ports:
  - "5433:5432"    # dùng port 5433 thay vì 5432
```

### Lỗi: Port 8080 đã bị chiếm

```bash
# Tìm process đang dùng port 8080 (Windows)
netstat -ano | findstr :8080

# Tắt process đó hoặc đổi port trong docker-compose.yml
ports:
  - "8081:8080"    # dùng port 8081 thay vì 8080
```

### Lỗi: Docker Desktop chưa chạy

```
error during connect: This error may indicate that the docker daemon is not running
```

Cách sửa: Mở Docker Desktop và đợi nó khởi động xong.

### Lỗi: Build thất bại — Maven download lỗi

```bash
# Xóa cache và build lại từ đầu
docker-compose build --no-cache
docker-compose up
```
