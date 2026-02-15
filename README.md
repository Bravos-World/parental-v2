# Parental Control v2 - Backend Server

Hệ thống backend quản lý thiết bị máy tính của trẻ em, hỗ trợ self-host đơn giản.

## Quick Start với Docker 🚀

Cách nhanh nhất để chạy hệ thống:

```bash
# Clone hoặc download docker-compose.yml từ repository
# Sau đó chạy:
docker compose up -d

# Kiểm tra logs
docker compose logs -f app

# Truy cập Swagger UI
# http://localhost:8080/swagger-ui.html
```

**Đăng nhập mặc định:**
- Username: `admin`
- Password: `admin` (⚠️ nhớ đổi trong production!)

## Tech Stack

- **Java 25** (Virtual Threads ready)
- **Spring Boot 4.0**
- **PostgreSQL**
- **WebSocket** (Raw WebSocket, không STOMP)
- **Session-Based Auth** (không JWT)
- **Swagger/OpenAPI 3**

## Yêu cầu

### Với Docker (recommended)
- Docker
- Docker Compose

### Với manual build
- Java 25+
- PostgreSQL 15+
- Gradle 9+

## Cấu hình biến môi trường

| Biến             | Mặc định     | Mô tả                |
| ---------------- | ------------ | -------------------- |
| `DB_HOST`        | `localhost`  | PostgreSQL host      |
| `DB_PORT`        | `5432`       | PostgreSQL port      |
| `DB_NAME`        | `parentalv2` | Database name        |
| `DB_USERNAME`    | `postgres`   | Database username    |
| `DB_PASSWORD`    | `postgres`   | Database password    |
| `ADMIN_USERNAME` | `admin`      | Admin login username |
| `ADMIN_PASSWORD` | `admin`      | Admin login password |
| `SERVER_PORT`    | `8080`       | Server port          |

## Build & Run

### 1. Tạo database

```sql
CREATE DATABASE parentalv2;
```

### 2. Build

```bash
./gradlew clean build -x test
```

### 3. Run

```bash
# Với biến môi trường mặc định
./gradlew bootRun

# Hoặc với docker-style environment
ADMIN_USERNAME=admin ADMIN_PASSWORD=mypassword \
DB_HOST=localhost DB_NAME=parentalv2 \
java -jar build/libs/parentalv2-1.0.0.jar
```

### 4. Truy cập

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

## REST API

### Authentication

| Method | Endpoint                    | Auth | Description                   |
| ------ | --------------------------- | ---- | ----------------------------- |
| POST   | `/api/auth/login`           | ❌   | Login (trả về session cookie) |
| POST   | `/api/auth/logout`          | ✅   | Logout                        |
| POST   | `/api/auth/change-password` | ✅   | Đổi mật khẩu                  |

**Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  -c cookies.txt
```

**Sử dụng session cookie cho các request tiếp theo:**

```bash
curl http://localhost:8080/api/devices -b cookies.txt
```

### Device Management

| Method | Endpoint                          | Description                      |
| ------ | --------------------------------- | -------------------------------- |
| GET    | `/api/devices`                    | Danh sách tất cả thiết bị        |
| GET    | `/api/devices/online`             | Danh sách thiết bị online        |
| GET    | `/api/devices/{deviceId}`         | Chi tiết thiết bị                |
| POST   | `/api/devices/{deviceId}/command` | Gửi lệnh tới thiết bị            |
| POST   | `/api/devices/{deviceId}/message` | Gửi tin nhắn tới thiết bị        |
| POST   | `/api/devices/command`            | Gửi lệnh tới tất cả thiết bị     |
| POST   | `/api/devices/message`            | Gửi tin nhắn tới tất cả thiết bị |
| GET    | `/api/devices/{deviceId}/events`  | Lịch sử hoạt động thiết bị       |

**Gửi lệnh (lock 1 tiếng):**

```bash
curl -X POST http://localhost:8080/api/devices/DEVICE_ID/command \
  -H "Content-Type: application/json" \
  -d '{"commandType":"LOCK","delaySeconds":3600}' \
  -b cookies.txt
```

**Gửi tin nhắn:**

```bash
curl -X POST http://localhost:8080/api/devices/DEVICE_ID/message \
  -H "Content-Type: application/json" \
  -d '{"message":"Đi ngủ đi con!"}' \
  -b cookies.txt
```

### Command Types & Delay

| Command    | Mô tả         | Delay mặc định |
| ---------- | ------------- | -------------- |
| `LOCK`     | Khóa máy      | 60 giây        |
| `UNLOCK`   | Mở khóa máy   | 60 giây        |
| `SHUTDOWN` | Tắt máy       | 60 giây        |
| `RESTART`  | Khởi động lại | 60 giây        |

- `delaySeconds <= 0`: Thực hiện ngay lập tức
- `delaySeconds` không set: Mặc định 60 giây
- Schedule thực hiện phía client, backend chỉ gửi lệnh

## WebSocket Protocol (cho Client developers)

### Endpoint

```
ws://SERVER_HOST:SERVER_PORT/ws/device
wss://SERVER_HOST:SERVER_PORT/ws/device  (nếu có SSL)
```

### Client → Server Messages

**1. Register (bắt buộc gửi ngay sau khi kết nối):**

```json
{
  "type": "register",
  "deviceId": "UNIQUE_DEVICE_ID",
  "deviceName": "PC-ConTrai",
  "ipAddress": "192.168.1.100"
}
```

> ⚠️ `deviceId` phải là duy nhất và không thay đổi. Nên dùng hardware ID hoặc UUID cố định.

**2. Status update:**

```json
{
  "type": "status",
  "lockStatus": "LOCKED"
}
```

**3. Event report:**

```json
{
  "type": "event",
  "eventType": "POWER_ON",
  "description": "System booted"
}
```

Event types: `POWER_ON`, `SHUTDOWN`, `LOCK`, `UNLOCK`, `RESTART`, `CONNECT`, `DISCONNECT`

### Server → Client Messages

**1. Registration acknowledgment:**

```json
{
  "type": "registered",
  "status": "ok"
}
```

**2. Command:**

```json
{
  "type": "command",
  "command": "LOCK",
  "delaySeconds": 3600
}
```

Commands: `LOCK`, `UNLOCK`, `SHUTDOWN`, `RESTART`

**3. Message (hiển thị notification):**

```json
{
  "type": "message",
  "content": "Đi ngủ đi con!"
}
```

### Lưu ý kết nối WebSocket

1. **Reconnection**: Client nên tự động reconnect khi mất kết nối (exponential backoff)
2. **WSS (SSL)**: Khi deploy production với HTTPS, cần sử dụng `wss://` thay vì `ws://`
3. **Register ngay**: Sau khi kết nối WebSocket thành công, client PHẢI gửi message `register` ngay
4. **Heartbeat**: WebSocket sẽ tự động đóng nếu không có hoạt động. Client nên gửi ping/pong hoặc message định kỳ

## Deploy

### Docker Compose (recommended)

Sử dụng Docker Compose để deploy nhanh chóng với prebuilt image `bravos/parentalv2:latest`.

**1. Tạo file docker-compose.yml:**

File `docker-compose.yml` đã có sẵn trong project. Bạn có thể tùy chỉnh các biến môi trường:

```yaml
services:
  db:
    image: postgres:17
    container_name: parentalv2-db
    environment:
      POSTGRES_DB: parentalv2
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

  app:
    image: bravos/parentalv2:latest
    container_name: parentalv2-app
    ports:
      - "8080:8080"
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: parentalv2
      DB_USERNAME: postgres
      DB_PASSWORD: postgres
      ADMIN_USERNAME: admin
      ADMIN_PASSWORD: admin  # ⚠️ Đổi password này!
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

volumes:
  pgdata:
```

**2. Khởi động services:**

```bash
docker compose up -d
```

**3. Kiểm tra logs:**

```bash
# Xem logs tất cả services
docker compose logs -f

# Chỉ xem logs của app
docker compose logs -f app
```

**4. Truy cập:**

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/v3/api-docs

**5. Dừng services:**

```bash
docker compose down

# Xóa cả volumes (⚠️ sẽ mất dữ liệu)
docker compose down -v
```

### Tùy chỉnh biến môi trường

Bạn có thể tạo file `.env` từ template để override các giá trị mặc định:

```bash
# Copy template
cp .env.example .env

# Chỉnh sửa .env với giá trị của bạn
nano .env
```

Ví dụ file `.env`:

```bash
# .env
ADMIN_USERNAME=myadmin
ADMIN_PASSWORD=mysecurepassword
DB_PASSWORD=mydbpassword
SERVER_PORT=8080
```

Sau đó cập nhật `docker-compose.yml` để sử dụng biến từ file `.env`:

```yaml
  db:
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
      
  app:
    environment:
      ADMIN_USERNAME: ${ADMIN_USERNAME:-admin}
      ADMIN_PASSWORD: ${ADMIN_PASSWORD:-admin}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
```

> 💡 File `.env` đã được thêm vào `.gitignore` để tránh commit thông tin nhạy cảm.

### Build từ source (cho developers)

Nếu bạn muốn build image từ source thay vì dùng prebuilt:

```bash
# Build
./gradlew clean build -x test

# Build Docker image
docker build -t parentalv2:local .

# Thay đổi image trong docker-compose.yml
# app:
#   image: parentalv2:local

docker compose up -d
```
