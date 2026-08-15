# ⚡ Android MTK BROM Service & Flashing Tool (UnlockTool Engine) with ESP32-S3 N16R8 Bridge

A professional-grade Android application and ESP32-S3 Hardware Bridge designed for MediaTek (MTK) chipset servicing, scatter flashing, partition read/write, FRP unlocking, and hardware-level BROM / Test-Point automation.

---

## 📱 Features

1. **UnlockTool Multi-Tab Flashing Console:**
   - **Brand & Model Presets:** Xiaomi / Redmi (Redmi 9A, 9C, Note 8 Pro, 10S, etc.), Oppo, Realme, Vivo, Samsung, Infinix, Tecno.
   - **Scatter File Parser:** Parse v1.1.x & v2.x XML/txt scatters (e.g. `MT6765_Android_scatter.txt`).
   - **Auto NV Data Backup (IMEI / Baseband Guard):** Dumps `nvram`, `nvdata`, `protect1`, `protect2`, `nvcfg`, and `secro` with SHA-256 validation before any wipe/flash action.
   - **Active BROM Sniffer:** Automatically listens for MTK USB port (`0x0E8D`) upon pressing START and shoots `0xA0` handshake within 100ms before BROM timeouts.
   - **Service Functions:** Read Device Info, Scatter Flash, Erase FRP, Factory Reset, Bypass SLA/DAA Auth, Unlock Bootloader, Read/Write single partitions.
   - **Built-in Gemini AI Assistant:** Instant MTK error analysis, test-point lookup, scatter validation, and repair troubleshooting.

2. **ESP32-S3 N16R8 Hardware Bridge:**
   - Designed specifically for **ESP32-S3-DevKitC-1 (N16R8: 16MB Flash, 8MB Octal PSRAM)**.
   - **Dual Type-C Interface:**
     - `USB Port`: Native USB Host to connect directly to Target MTK Phone via OTG.
     - `COM Port`: UART & 5V Power supply from PC or USB adapter.
   - **Hardware Test-Point Trigger:** Configurable 50ms - 300ms ground pulse for forcing dead/hard-bricked devices into BROM.
   - **Wireless Control:** WebSocket / HTTP JSON API over WiFi Access Point & Station mode.

---

## 🔌 ESP32-S3 N16R8 Pinout & Wiring

| Interface | ESP32-S3 Pin | Target / Function | Wire Color / Note |
| :--- | :--- | :--- | :--- |
| **USB D+** | `GPIO 20` | Target Phone USB D+ | Green (or via Native Type-C USB Port) |
| **USB D-** | `GPIO 19` | Target Phone USB D- | White (or via Native Type-C USB Port) |
| **GND** | `GND` | Common Ground | Black |
| **5V VBUS** | `5V / VBUS` | Target Phone Power | Red (5V Rail) |
| **Test-Point (TP)** | `GPIO 4` | Force BROM CLK/DAT0 Pulse | Pulled low (100ms) for TP Trigger |
| **VBUS Relay** | `GPIO 5` | Power Cycle Reset | Cut/Restore VBUS power |
| **Hardware UART TX** | `GPIO 17` | MTK RX (Direct Motherboard) | High-speed BROM Serial |
| **Hardware UART RX** | `GPIO 18` | MTK TX (Direct Motherboard) | High-speed BROM Serial |
| **Status RGB LED** | `GPIO 48` | WS2812 RGB State Indicator | Blue=Ready, Green=Flashing, Red=Error |

---

## 🛠️ How to Flash ESP32-S3 N16R8 Firmware

### Option A: Using PlatformIO (Recommended)
1. Open the `/esp32-firmware` folder in VS Code with the PlatformIO extension.
2. Connect your ESP32-S3 to your computer via the **COM** Type-C port.
3. Run the following command:
   ```bash
   pio run -e esp32s3_n16r8 -t upload
   ```
4. Configuration is already set for 16MB Flash (`qio`) and 8MB Octal PSRAM (`qio_opi`).

### Option B: Using Arduino IDE
1. Go to **Tools > Board** -> Select `ESP32S3 Dev Module`.
2. Configure settings:
   - **Flash Size:** `16MB (128Mb)`
   - **Partition Scheme:** `16M Flash (3MB APP/9.9MB FATFS)` or `Default 16MB`
   - **PSRAM:** `OPI PSRAM` (Octal PSRAM)
   - **USB Mode:** `Hardware CDC and JTAG`
   - **USB CDC On Boot:** `Enabled`
3. Open `esp32-firmware/src/main.cpp` and click **Upload**.

---

## 📦 How to Build the Android APK

1. In the AI Studio top bar, open the **Settings / Export** menu.
2. Select **"Generate APK / Export Project as ZIP"** or push directly to **GitHub**.
3. If building locally with Gradle:
   ```bash
   gradle :app:assembleRelease
   ```
4. Install the generated APK on your Android smartphone or tablet.

---

## 🤖 GitHub Actions Automated Cloud Build

This repository includes a pre-configured, modern GitHub Actions workflow (`.github/workflows/build.yml`):
- **Android APK Build:** Automatically compiles `assembleDebug` APK with Java 17 Temurin.
- **ESP32-S3 Firmware Build:** Compiles binary files (`firmware.bin`, `bootloader.bin`, `partitions.bin`) using PlatformIO for ESP32-S3 N16R8.
- **Automatic Artifacts:** Once pushed to GitHub, go to the **Actions** tab on your GitHub repository to download the ready-to-use `.apk` and `.bin` files directly!

---

## 🚀 Standard Flashing Workflow (Real Hardware)

1. Launch **MTK UnlockTool** on your Android device.
2. Select your desired brand/model (e.g. `Redmi 9A (MT6765)`), load scatter file if needed.
3. Keep **Auto NV Data Backup** checked (`[✓] IMEI Guard ON`).
4. Press **START / EXECUTE** on your chosen action (e.g. *Erase FRP* or *Flash Partition*).
5. The terminal will show: `>>> [WAITING FOR MTK BROM PORT] ⏳ Sniffing Active...`
6. **Now**, turn off target phone, hold `Volume Up + Volume Down` buttons, and insert the USB-OTG cable into the phone.
7. The app instantly grabs BROM handshake, runs pre-backup, and completes the operation safely!
