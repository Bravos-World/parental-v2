# Frontend Admin Guide - Angular + Tailwind CSS

Hướng dẫn xây dựng giao diện quản trị cho Parental Control v2 sử dụng Angular và Tailwind CSS.

## 1. Khởi tạo dự án

```bash
# Cài đặt Angular CLI
npm install -g @angular/cli@latest

# Tạo project mới
ng new parental-admin --routing --style=scss --ssr=false
cd parental-admin

# Cài đặt Tailwind CSS
npm install -D tailwindcss @tailwindcss/postcss postcss

# Cấu hình PostCSS - tạo file postcss.config.js
cat > postcss.config.js << 'EOF'
module.exports = {
  plugins: {
    "@tailwindcss/postcss": {},
  },
};
EOF

# Thêm Tailwind vào src/styles.scss
echo '@import "tailwindcss";' > src/styles.scss
```

## 2. Cấu trúc thư mục đề xuất

```
src/app/
├── core/
│   ├── services/
│   │   ├── auth.service.ts          # Login, logout, change password
│   │   ├── device.service.ts        # Device CRUD, commands
│   │   └── websocket.service.ts     # WebSocket real-time updates (optional)
│   ├── guards/
│   │   └── auth.guard.ts            # Route protection
│   ├── interceptors/
│   │   └── auth.interceptor.ts      # Attach credentials to requests
│   └── models/
│       ├── device.model.ts
│       ├── command.model.ts
│       └── api-response.model.ts
├── features/
│   ├── login/
│   │   └── login.component.ts
│   ├── dashboard/
│   │   └── dashboard.component.ts   # Tổng quan thiết bị
│   ├── devices/
│   │   ├── device-list.component.ts
│   │   └── device-detail.component.ts
│   └── settings/
│       └── settings.component.ts    # Đổi mật khẩu
└── shared/
    └── components/                  # Shared UI components
```

## 3. Cấu hình proxy cho development

Tạo file `proxy.conf.json`:

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  },
  "/ws": {
    "target": "ws://localhost:8080",
    "secure": false,
    "ws": true
  }
}
```

Sửa `angular.json` → `architect.serve.options`:

```json
"proxyConfig": "proxy.conf.json"
```

## 4. Models

```typescript
// src/app/core/models/device.model.ts
export interface Device {
  deviceId: string;
  deviceName: string;
  ipAddress: string;
  status: "ONLINE" | "OFFLINE";
  lockStatus: "LOCKED" | "UNLOCKED";
  lastSeen: string;
  createdAt: string;
}

export interface DeviceEvent {
  id: number;
  deviceId: string;
  deviceName: string;
  eventType: string;
  description: string;
  timestamp: string;
}

export type CommandType = "LOCK" | "UNLOCK" | "SHUTDOWN" | "RESTART";

export interface CommandRequest {
  commandType: CommandType;
  delaySeconds: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}
```

## 5. Auth Service

```typescript
// src/app/core/services/auth.service.ts
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Router } from "@angular/router";
import { BehaviorSubject, tap } from "rxjs";

@Injectable({ providedIn: "root" })
export class AuthService {
  private isLoggedIn$ = new BehaviorSubject<boolean>(false);

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  login(username: string, password: string) {
    return this.http
      .post<any>(
        "/api/auth/login",
        { username, password },
        { withCredentials: true },
      )
      .pipe(tap(() => this.isLoggedIn$.next(true)));
  }

  logout() {
    return this.http
      .post<any>("/api/auth/logout", {}, { withCredentials: true })
      .pipe(
        tap(() => {
          this.isLoggedIn$.next(false);
          this.router.navigate(["/login"]);
        }),
      );
  }

  changePassword(oldPassword: string, newPassword: string) {
    return this.http.post<any>(
      "/api/auth/change-password",
      { oldPassword, newPassword },
      { withCredentials: true },
    );
  }

  get isAuthenticated() {
    return this.isLoggedIn$.asObservable();
  }
}
```

## 6. Device Service

```typescript
// src/app/core/services/device.service.ts
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import {
  Device,
  CommandRequest,
  ApiResponse,
  DeviceEvent,
} from "../models/device.model";

@Injectable({ providedIn: "root" })
export class DeviceService {
  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<ApiResponse<Device[]>>("/api/devices", {
      withCredentials: true,
    });
  }

  getOnline() {
    return this.http.get<ApiResponse<Device[]>>("/api/devices/online", {
      withCredentials: true,
    });
  }

  getDevice(deviceId: string) {
    return this.http.get<ApiResponse<Device>>(`/api/devices/${deviceId}`, {
      withCredentials: true,
    });
  }

  sendCommand(deviceId: string, command: CommandRequest) {
    return this.http.post<ApiResponse<void>>(
      `/api/devices/${deviceId}/command`,
      command,
      { withCredentials: true },
    );
  }

  sendMessage(deviceId: string, message: string) {
    return this.http.post<ApiResponse<void>>(
      `/api/devices/${deviceId}/message`,
      { message },
      { withCredentials: true },
    );
  }

  sendCommandToAll(command: CommandRequest) {
    return this.http.post<ApiResponse<void>>("/api/devices/command", command, {
      withCredentials: true,
    });
  }

  getEvents(deviceId: string, page = 0, size = 20) {
    return this.http.get<ApiResponse<any>>(
      `/api/devices/${deviceId}/events?page=${page}&size=${size}`,
      { withCredentials: true },
    );
  }
}
```

## 7. Auth Interceptor & Guard

```typescript
// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((err) => {
      if (err.status === 401 && !req.url.includes("/login")) {
        router.navigate(["/login"]);
      }
      return throwError(() => err);
    }),
  );
};
```

```typescript
// src/app/core/guards/auth.guard.ts
import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { map } from "rxjs";

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated.pipe(
    map((isAuth) => isAuth || router.createUrlTree(["/login"])),
  );
};
```

## 8. Routing

```typescript
// src/app/app.routes.ts
import { Routes } from "@angular/router";
import { authGuard } from "./core/guards/auth.guard";

export const routes: Routes = [
  {
    path: "login",
    loadComponent: () =>
      import("./features/login/login.component").then((m) => m.LoginComponent),
  },
  {
    path: "",
    canActivate: [authGuard],
    children: [
      { path: "", redirectTo: "dashboard", pathMatch: "full" },
      {
        path: "dashboard",
        loadComponent: () =>
          import("./features/dashboard/dashboard.component").then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        path: "devices/:id",
        loadComponent: () =>
          import("./features/devices/device-detail.component").then(
            (m) => m.DeviceDetailComponent,
          ),
      },
      {
        path: "settings",
        loadComponent: () =>
          import("./features/settings/settings.component").then(
            (m) => m.SettingsComponent,
          ),
      },
    ],
  },
];
```

## 9. Chạy development

```bash
ng serve --proxy-config proxy.conf.json
# Truy cập http://localhost:4200
```

## 10. Build production

```bash
ng build --configuration production
# Output tại dist/parental-admin/browser/
# Copy vào thư mục static của Spring Boot nếu muốn serve từ backend
```

## Lưu ý quan trọng

- Sử dụng `withCredentials: true` cho tất cả HTTP requests để gửi session cookie
- Kết nối WebSocket (optional) để real-time update danh sách thiết bị
- Polling `/api/devices` định kỳ (mỗi 5-10 giây) nếu không dùng WebSocket cho admin
