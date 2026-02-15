# C++ Windows Client Guide

Hướng dẫn xây dựng client C++ cho Windows 11, chạy ngầm, auto-start, và tính năng khóa máy nâng cao.

## Tech Stack đề xuất

- **C++17** trở lên
- **WebSocket**: [libwebsockets](https://libwebsockets.org/) hoặc [Beast (Boost.Asio)](https://www.boost.org/doc/libs/release/libs/beast/)
- **JSON**: [nlohmann/json](https://github.com/nlohmann/json)
- **Build**: CMake
- **Static linking**: Tất cả `.dll` được link static vào `.exe`

## 1. Khởi tạo CMake Project

```cmake
cmake_minimum_required(VERSION 3.20)
project(parental-client LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)

# Static linking - tất cả DLL gắn vào exe
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
set(BUILD_SHARED_LIBS OFF)

# Dependencies (dùng vcpkg hoặc FetchContent)
find_package(Boost REQUIRED COMPONENTS system)
find_package(OpenSSL REQUIRED)
find_package(nlohmann_json REQUIRED)

add_executable(parental-client
    src/main.cpp
    src/websocket_client.cpp
    src/device_info.cpp
    src/command_handler.cpp
    src/overlay_window.cpp
    src/keyboard_hook.cpp
    src/service_manager.cpp
    src/network_monitor.cpp
)

target_link_libraries(parental-client PRIVATE
    Boost::system
    OpenSSL::SSL OpenSSL::Crypto
    nlohmann_json::nlohmann_json
    ws2_32 crypt32 secur32  # Windows libs
)

# Static link CRT
set_target_properties(parental-client PROPERTIES
    MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>"
    LINK_FLAGS "/SUBSYSTEM:WINDOWS /ENTRY:mainCRTStartup"  # No console window
)
```

## 2. Cấu trúc thư mục

```
parental-client/
├── CMakeLists.txt
├── src/
│   ├── main.cpp                 # Entry point & orchestration
│   ├── config.h                 # Server URL, DeviceID, Emergency code
│   ├── websocket_client.h/cpp   # WebSocket connection & messaging
│   ├── device_info.h/cpp        # Get device name, IP, generate DeviceID
│   ├── command_handler.h/cpp    # Process server commands
│   ├── overlay_window.h/cpp     # Lock screen overlay
│   ├── keyboard_hook.h/cpp      # Block key combinations
│   ├── service_manager.h/cpp    # Windows Service registration
│   └── network_monitor.h/cpp    # Wait for network, auto-reconnect
├── scripts/
│   ├── install.ps1              # Install & auto-start script
│   └── uninstall.ps1            # Remove script
└── vcpkg.json                   # Dependencies
```

## 3. Config

```cpp
// src/config.h
#pragma once
#include <string>

namespace Config {
    // Server connection
    constexpr const char* SERVER_URL = "ws://192.168.1.100:8080/ws/device";  // Đổi theo IP server
    // constexpr const char* SERVER_URL = "wss://192.168.1.100:8080/ws/device";  // SSL

    // Emergency code - hoạt động khi không kết nối được server
    constexpr const char* EMERGENCY_CODE = "PARENT@2024";

    // Reconnect settings
    constexpr int RECONNECT_INITIAL_DELAY_MS = 1000;
    constexpr int RECONNECT_MAX_DELAY_MS = 30000;

    // Network check interval
    constexpr int NETWORK_CHECK_INTERVAL_MS = 5000;
}
```

## 4. Device Info (lấy DeviceID cố định)

```cpp
// src/device_info.cpp
#include <windows.h>
#include <iphlpapi.h>
#include <string>

// Lấy DeviceID dựa trên Machine GUID (không đổi khi reinstall Windows)
std::string getDeviceId() {
    HKEY hKey;
    char value[256];
    DWORD size = sizeof(value);

    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
        "SOFTWARE\\Microsoft\\Cryptography", 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        RegQueryValueExA(hKey, "MachineGuid", nullptr, nullptr, (LPBYTE)value, &size);
        RegCloseKey(hKey);
        return std::string(value);
    }
    return "UNKNOWN";
}

std::string getDeviceName() {
    char name[MAX_COMPUTERNAME_LENGTH + 1];
    DWORD size = sizeof(name);
    GetComputerNameA(name, &size);
    return std::string(name);
}

std::string getIPAddress() {
    // Use GetAdaptersInfo or gethostbyname
    // Implementation depends on network setup
    char hostname[256];
    gethostname(hostname, sizeof(hostname));
    hostent* host = gethostbyname(hostname);
    if (host && host->h_addr_list[0]) {
        return inet_ntoa(*(in_addr*)host->h_addr_list[0]);
    }
    return "0.0.0.0";
}
```

## 5. WebSocket Client

```cpp
// src/websocket_client.h
#pragma once
#include <functional>
#include <string>

class WebSocketClient {
public:
    using MessageHandler = std::function<void(const std::string&)>;

    bool connect(const std::string& url);
    void disconnect();
    bool isConnected() const;
    void send(const std::string& message);
    void setOnMessage(MessageHandler handler);
    void setOnDisconnect(std::function<void()> handler);
    void poll();  // Call in main loop

private:
    // Beast/libwebsockets implementation
    MessageHandler onMessage;
    std::function<void()> onDisconnect;
    bool connected = false;
};
```

### Kết nối và đăng ký:

```cpp
void connectAndRegister(WebSocketClient& client) {
    if (client.connect(Config::SERVER_URL)) {
        // Gửi register message ngay sau kết nối
        nlohmann::json reg;
        reg["type"] = "register";
        reg["deviceId"] = getDeviceId();
        reg["deviceName"] = getDeviceName();
        reg["ipAddress"] = getIPAddress();
        client.send(reg.dump());
    }
}
```

## 6. Command Handler

```cpp
// src/command_handler.cpp
#include <nlohmann/json.hpp>
#include <windows.h>
#include <thread>
#include <chrono>

using json = nlohmann::json;

void handleCommand(const json& msg) {
    std::string type = msg["type"];

    if (type == "command") {
        std::string command = msg["command"];
        int delay = msg["delaySeconds"];

        // Schedule trên client
        if (delay > 0) {
            std::thread([command, delay]() {
                std::this_thread::sleep_for(std::chrono::seconds(delay));
                executeCommand(command);
            }).detach();
        } else {
            executeCommand(command);
        }
    }
    else if (type == "message") {
        std::string content = msg["content"];
        showNotification(content);
    }
}

void executeCommand(const std::string& command) {
    if (command == "SHUTDOWN") {
        system("shutdown /s /f /t 0");
    }
    else if (command == "RESTART") {
        system("shutdown /r /f /t 0");
    }
    else if (command == "LOCK") {
        showOverlay();  // Show lock screen overlay
    }
    else if (command == "UNLOCK") {
        hideOverlay();  // Hide lock screen overlay
    }
}

void showNotification(const std::string& message) {
    // Dùng Windows Toast Notification hoặc MessageBox
    // Chỉ hiển thị khi UNLOCKED
    MessageBoxA(NULL, message.c_str(), "Thông báo từ phụ huynh", MB_OK | MB_ICONINFORMATION | MB_TOPMOST);
}
```

## 7. Overlay Window (Lock Screen)

```cpp
// src/overlay_window.cpp
// Tạo fullscreen topmost window khi LOCK

#include <windows.h>
#include <string>

HWND overlayHwnd = NULL;
std::string adminMessage = "";
bool isLocked = false;

LRESULT CALLBACK OverlayProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_PAINT: {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hwnd, &ps);
            RECT rect;
            GetClientRect(hwnd, &rect);

            // Background đen
            FillRect(hdc, &rect, (HBRUSH)GetStockObject(BLACK_BRUSH));

            // Hiển thị message từ admin
            SetTextColor(hdc, RGB(255, 255, 255));
            SetBkMode(hdc, TRANSPARENT);
            if (!adminMessage.empty()) {
                DrawTextA(hdc, adminMessage.c_str(), -1, &rect,
                    DT_CENTER | DT_VCENTER | DT_SINGLELINE);
            }

            // TODO: Vẽ nút Shutdown và Restart

            EndPaint(hwnd, &ps);
            return 0;
        }
        case WM_CLOSE:
        case WM_DESTROY:
            return 0;  // Không cho đóng
        default:
            return DefWindowProc(hwnd, msg, wParam, lParam);
    }
}

void showOverlay() {
    if (overlayHwnd) return;
    isLocked = true;

    WNDCLASS wc = {};
    wc.lpfnWndProc = OverlayProc;
    wc.hInstance = GetModuleHandle(NULL);
    wc.lpszClassName = "ParentalOverlay";
    RegisterClass(&wc);

    int screenW = GetSystemMetrics(SM_CXSCREEN);
    int screenH = GetSystemMetrics(SM_CYSCREEN);

    overlayHwnd = CreateWindowEx(
        WS_EX_TOPMOST | WS_EX_TOOLWINDOW,
        "ParentalOverlay", "Locked",
        WS_POPUP | WS_VISIBLE,
        0, 0, screenW, screenH,
        NULL, NULL, GetModuleHandle(NULL), NULL
    );

    SetForegroundWindow(overlayHwnd);
}

void hideOverlay() {
    isLocked = false;
    if (overlayHwnd) {
        DestroyWindow(overlayHwnd);
        overlayHwnd = NULL;
    }
}
```

## 8. Keyboard Hook (chặn phím tắt)

```cpp
// src/keyboard_hook.cpp
#include <windows.h>

HHOOK keyboardHook = NULL;

LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
    if (nCode >= 0 && isLocked) {
        KBDLLHOOKSTRUCT* kbd = (KBDLLHOOKSTRUCT*)lParam;

        // Block Win key
        if (kbd->vkCode == VK_LWIN || kbd->vkCode == VK_RWIN) return 1;

        // Block Alt+Tab
        if (kbd->vkCode == VK_TAB && (kbd->flags & LLKHF_ALTDOWN)) return 1;

        // Block Alt+F4
        if (kbd->vkCode == VK_F4 && (kbd->flags & LLKHF_ALTDOWN)) return 1;

        // Block Ctrl+Esc (Start Menu)
        if (kbd->vkCode == VK_ESCAPE && (GetAsyncKeyState(VK_CONTROL) & 0x8000)) return 1;

        // Block Ctrl+Shift+Esc (Task Manager)
        if (kbd->vkCode == VK_ESCAPE &&
            (GetAsyncKeyState(VK_CONTROL) & 0x8000) &&
            (GetAsyncKeyState(VK_SHIFT) & 0x8000)) return 1;

        // Block Ctrl+Alt+Delete - Note: cannot be blocked by hook alone
        // Requires Windows Service or Group Policy

        // Block Ctrl+Tab, Ctrl+Shift+Tab
        if (kbd->vkCode == VK_TAB && (GetAsyncKeyState(VK_CONTROL) & 0x8000)) return 1;

        // Block Sticky Keys (5x Shift)
        if (kbd->vkCode == VK_SHIFT) {
            // Disable Sticky Keys via registry
            SystemParametersInfo(SPI_SETSTICKYKEYS, sizeof(STICKYKEYS),
                &(STICKYKEYS){sizeof(STICKYKEYS), 0}, 0);
        }
    }
    return CallNextHookEx(keyboardHook, nCode, wParam, lParam);
}

void installKeyboardHook() {
    keyboardHook = SetWindowsHookEx(WH_KEYBOARD_LL,
        LowLevelKeyboardProc, GetModuleHandle(NULL), 0);
}

void removeKeyboardHook() {
    if (keyboardHook) {
        UnhookWindowsHookEx(keyboardHook);
        keyboardHook = NULL;
    }
}
```

> ⚠️ **Lưu ý**: Không thể chặn `Ctrl+Alt+Delete` bằng keyboard hook. Cần dùng Windows Service hoặc Group Policy.
> Xem phần "Nâng cao" bên dưới.

## 9. Auto-reconnect & Network Monitor

```cpp
// src/network_monitor.cpp
#include <windows.h>
#include <wininet.h>
#pragma comment(lib, "wininet.lib")

bool isNetworkAvailable() {
    DWORD flags;
    return InternetGetConnectedState(&flags, 0);
}

// Main loop với auto-reconnect
void mainLoop(WebSocketClient& client) {
    int retryDelay = Config::RECONNECT_INITIAL_DELAY_MS;

    while (true) {
        // Chờ mạng
        while (!isNetworkAvailable()) {
            Sleep(Config::NETWORK_CHECK_INTERVAL_MS);
        }

        // Kết nối
        if (!client.isConnected()) {
            if (client.connect(Config::SERVER_URL)) {
                connectAndRegister(client);
                retryDelay = Config::RECONNECT_INITIAL_DELAY_MS;  // Reset
            } else {
                Sleep(retryDelay);
                retryDelay = min(retryDelay * 2, Config::RECONNECT_MAX_DELAY_MS);  // Exponential backoff
                continue;
            }
        }

        client.poll();
        Sleep(100);
    }
}
```

## 10. Emergency Code

Khi không kết nối được server, cho phép nhập emergency code để mở khóa:

```cpp
void checkEmergencyCode() {
    if (isLocked && !client.isConnected()) {
        // Hiển thị input field trên overlay
        std::string input = showEmergencyInput();
        if (input == Config::EMERGENCY_CODE) {
            hideOverlay();
            removeKeyboardHook();
        }
    }
}
```

## 11. Windows Service (Nâng cao - tự hồi sinh)

```cpp
// src/service_manager.cpp
// Đăng ký app dưới dạng Windows Service để:
// 1. Tự khởi động khi boot
// 2. Tự hồi sinh nếu bị kill

#include <windows.h>

bool installService(const char* exePath) {
    SC_HANDLE schSCManager = OpenSCManager(NULL, NULL, SC_MANAGER_CREATE_SERVICE);
    if (!schSCManager) return false;

    SC_HANDLE schService = CreateServiceA(
        schSCManager,
        "ParentalControl",
        "Parental Control Client",
        SERVICE_ALL_ACCESS,
        SERVICE_WIN32_OWN_PROCESS,
        SERVICE_AUTO_START,        // Auto start
        SERVICE_ERROR_NORMAL,
        exePath,
        NULL, NULL, NULL, NULL, NULL
    );

    if (schService) {
        // Cấu hình recovery - restart khi crash
        SERVICE_FAILURE_ACTIONS_FLAG flag = { TRUE };
        ChangeServiceConfig2(schService, SERVICE_CONFIG_FAILURE_ACTIONS_FLAG, &flag);

        SC_ACTION actions[3] = {
            { SC_ACTION_RESTART, 1000 },  // Restart sau 1 giây
            { SC_ACTION_RESTART, 5000 },  // Restart sau 5 giây
            { SC_ACTION_RESTART, 10000 }, // Restart sau 10 giây
        };
        SERVICE_FAILURE_ACTIONS sfa = {};
        sfa.dwResetPeriod = 86400;  // Reset counter sau 24h
        sfa.cActions = 3;
        sfa.lpsaActions = actions;
        ChangeServiceConfig2(schService, SERVICE_CONFIG_FAILURE_ACTIONS, &sfa);

        CloseServiceHandle(schService);
    }

    CloseServiceHandle(schSCManager);
    return schService != NULL;
}
```

## 12. Scripts

### Install Script (`scripts/install.ps1`)

```powershell
# Run as Administrator
$appPath = "$PSScriptRoot\parental-client.exe"
$serviceName = "ParentalControl"

# Cách 1: Windows Service (khuyến nghị)
New-Service -Name $serviceName `
            -DisplayName "Parental Control Client" `
            -BinaryPathName $appPath `
            -StartupType Automatic `
            -Description "Parental Control monitoring client"

# Cấu hình recovery
sc.exe failure $serviceName reset= 86400 actions= restart/1000/restart/5000/restart/10000

# Start service
Start-Service $serviceName

Write-Host "Parental Control client installed successfully!"

# Cách 2: Task Scheduler (đơn giản hơn, chạy sau khi mạng kết nối)
# $trigger = New-ScheduledTaskTrigger -AtStartup
# $action = New-ScheduledTaskAction -Execute $appPath
# $settings = New-ScheduledTaskSettingsSet -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
# Register-ScheduledTask -TaskName "ParentalControl" -Trigger $trigger -Action $action -Settings $settings -RunLevel Highest
```

### Uninstall Script (`scripts/uninstall.ps1`)

```powershell
# Run as Administrator
$serviceName = "ParentalControl"

Stop-Service $serviceName -Force -ErrorAction SilentlyContinue
sc.exe delete $serviceName

# Hoặc nếu dùng Task Scheduler
# Unregister-ScheduledTask -TaskName "ParentalControl" -Confirm:$false

Write-Host "Parental Control client uninstalled."
```

## 13. Build (single .exe, static linked)

```powershell
# Với vcpkg
cmake -B build -S . -DCMAKE_TOOLCHAIN_FILE=C:/vcpkg/scripts/buildsystems/vcpkg.cmake -DVCPKG_TARGET_TRIPLET=x64-windows-static
cmake --build build --config Release

# Output: build/Release/parental-client.exe (single file, no DLL needed)
```

## Lưu ý quan trọng

1. **WSS (SSL)**: Khi server dùng HTTPS, phải kết nối qua `wss://`. Cần link OpenSSL static.
2. **Ctrl+Alt+Delete**: Không thể chặn bằng user-mode hook. Cần:
   - Disable Task Manager qua Group Policy / Registry
   - Hoặc thay thế shell (HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon\Shell)
3. **UAC**: App cần chạy với quyền Administrator hoặc dưới dạng Service
4. **Antivirus**: App có thể bị flag bởi antivirus do có keyboard hook và overlay. Cần thêm exclusion.
5. **Firewall**: Đảm bảo port WebSocket được mở trên firewall
