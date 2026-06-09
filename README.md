```markdown
# ⚡ GameBoosterPro

A professional, low-overhead split-tunnel network optimization utility for Android. Built natively using a clean, human-designed Material 3 adaptive design architecture, **GameBoosterPro** leverages Android's low-level `VpnService` layer and non-blocking asynchronous TCP socket challenges to route gaming data packets through the lowest-latency edge nodes dynamically without breaking game asset execution loops.

---

## 🎨 System Aesthetic & UI Architecture

Unlike traditional performance tools, this framework abandons typical AI-generated clichés (glowing sci-fi borders, flashing neon grids). Instead, it adopts a refined, high-density industrial utility interface focused entirely on real-time data representation.

* **Color Token Palette:** High-density adaptive dark canvas (`#121212`) utilizing crisp engineering accents (`#1A73E8` Blue and `#007A33` Green).
* **Typography Framework:** Structured standard Roboto / Inter system typography aligned strictly to a modern 16dp component grid layout.
* **Minimalist Iconography:** Built via explicit custom vector paths showcasing a clean, geometric shield emblem profile asset.

---

## 🎮 Immersive In-Game Overlay Control

When you leave the application workspace to hop into your match, a clean, minimal draggable control bubble stays right on your screen. Tap it anytime mid-game to pull up your real-time latency and toggle network paths effortlessly without minimizing your gameplay loop.



---

## ⚙️ Core Engineering Framework


```
[ User Interface Engine ] <---> [ Local Telemetry Broadcasts ]
|                                  |
v                                  v
+--------------------+              +--------------------+
|   ServerPicker     |              |  TelemetryWidget   |
| (Asynchronous TCP) |              | (Home Screen Live) |
+--------------------+              +--------------------+
|
v
+--------------------+
|   BoosterService   | ---> [ App allowed UID matching ]
| (VpnService Layer) | ---> [ Scoped IP .addRoute() ]
+--------------------+ ---> [ CDN Domain .allowBypass() ]
|
v
+--------------------+
| FloatingBubble     |
| (System Overlay)   |
+--------------------+
```

### 📶 Dynamic Asynchronous Ping Matrix
The app completely eliminates arbitrary estimations. The `ServerPicker` subsystem orchestrates low-overhead, multi-threaded Java socket challenges (`Socket.connect`) directly against public cloud hosting endpoints (Singapore, US-East, Europe). It computes precise network round-trip time (RTT) in milliseconds, sorts the active profiles, and binds the connection to the single lowest MS route.

### 🎮 Targeted Multi-Game Configuration
Built natively with deep integration mappings for premium high-performance profiles:
* **Call of Duty: Mobile** (`com.activision.callofduty.shooter`)
* **eFootball** (`jp.konami.pesam`)

### 🛠️ Advanced CDN Bypass Architecture
Standard VPN utilities capture device networking globally, which consistently causes modern mobile titles to throw file corruption errors (`"Download configuration failed"`) at initial loading screens because their secure Content Delivery Networks (CDNs) get blocked. 

GameBoosterPro solves this by applying isolated packet filtering:
1.  **Selective Package Capture:** Triggers `.addAllowedApplication()` specifically tracking only the designated game's internal UID space.
2.  **IP Scoping:** Restricts virtual interface routing exclusively to live multiplayer match server blocks via targeted `.addRoute()` mappings.
3.  **Default Network Fallback:** Invokes `.allowBypass()`, letting initialization assets, CDN dependency scripts, and security handshakes load smoothly over the device's stock network routing adapter.

---

## 🚀 Fully Automated CI/CD Actions Pipeline

This repository features a structural, zero-configuration GitHub Actions continuous integration build compiler pipeline configured inside `.github/workflows/build.yml`.

```yaml
# Summary of the compilation sequence:
- Installs automated Ubuntu-latest virtual execution containers.
- Configures secure, production-grade Java 17 (Temurin) and Gradle 8.8 environments.
- Automatically handles Android build dependencies and asset packaging toolchains.
- Automatically runs a terminal shell `keytool` command inside the safe runner session context to auto-generate a valid `app/debug.keystore` workspace artifact before compilation—completely avoiding the need to manually configure repository secrets.
- Compiles the source repository via `./gradlew assembleDebug`.
- Automatically publishes the final, compiled, fully signed debug APK directly into your repository's GitHub Actions output tab for immediate over-the-air installation.

```
## 📂 Structural Directory Mapping
```text
GameBoosterPro/
├── .github/
│   └── workflows/
│       └── build.yml             # Automated Gradle compiler script
├── app/
│   ├── build.gradle.kts          # Material 3 and ViewBinding configurations
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml # System permissions, service bounds & widget arrays
│           └── java/com/gamebooster/pro/
│               ├── MainActivity.kt        # Exposed dropdown configuration profile panel
│               ├── ServerPicker.kt        # Asynchronous TCP ping execution model
│               ├── BoosterService.kt      # Core VpnService background routing engine
│               ├── FloatingBubbleService.kt # Draggable live system-alert draw overlay
│               └── TelemetryWidget.kt     # Multi-telemetry launcher provider class
└── res/
    ├── drawable/
    │   └── ic_launcher_foreground.xml # Clean geometric shield vector icon asset
    ├── layout/
    │   ├── activity_main.xml     # Clean high-density layout interface 
    │   └── widget_layout.xml     # Comprehensive home screen tracker panel
    └── xml/
        └── widget_info.xml       # Native structural appwidget-provider metadata

```
## 🏗️ Development & Local Installation Steps
 1. **Clone the project repository to your workspace:**
   ```bash
   git clone [https://github.com/yourusername/GameBoosterPro.git](https://github.com/yourusername/GameBoosterPro.git)
   
   ```
 2. **Open the repository folder directly inside Android Studio (Ladybug or newer).**
 3. **Allow Gradle 8.8 to finish syncing local system cache structures.**
 4. **Deploy directly via terminal commands or push changes to GitHub:**
   ```bash
   ./gradlew assembleDebug
   
   ```
 5. **Check your GitHub repository's Actions tab** to watch the automated compiler pipeline build your release APK smoothly on the cloud!
```

```
