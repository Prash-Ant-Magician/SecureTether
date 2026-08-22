# SecureTether 🛡️

SecureTether is a privacy-focused Android application designed for secure, peer-to-peer file and data sharing using Bluetooth. It ensures that your transfers are encrypted and verified through a secure handshake process, featuring a built-in vault for protected storage.

## 🚀 Features

- **Secure Peer-to-Peer Transfer**: Share photos and files directly between devices using Bluetooth without relying on a central server.
- **Connection Verification**: Uses unique 6-digit verification codes to prevent Man-in-the-Middle (MITM) attacks during pairing.
- **Secure Vault**: An encrypted storage space within the app to keep your shared and received files safe.
- **Biometric/PIN Authentication**: Protects access to the app and its vault.
- **Real-time Discovery**: Quickly find and connect to nearby SecureTether-enabled devices.
- **Modern UI**: Built entirely with Jetpack Compose and Material 3 for a fluid, responsive experience.

## 🏗️ Architecture

The project follows **Clean Architecture** principles combined with the **MVVM (Model-View-ViewModel)** pattern, ensuring a scalable and testable codebase:

- **Domain Layer**: Contains business logic, models (`BluetoothDeviceDomain`), and repository interfaces (`BluetoothController`, `VaultRepository`).
- **Data Layer**: Implements repository interfaces, handling Bluetooth communication, local storage (DataStore), and encryption (KeystoreManager).
- **UI Layer**: Composable screens (`TransferScreen`, `VaultScreen`, `AuthScreen`) and ViewModels (`BluetoothViewModel`) that observe state from the domain layer.

## 🛠️ Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design System**: [Material 3](https://m3.material.io/)
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Concurrency**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Local Storage**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Camera/Media**: [CameraX](https://developer.android.com/training/camerax) & [Photo Picker](https://developer.android.com/training/data-storage/shared/photopicker)
- **Security**: [Android Keystore System](https://developer.android.com/training/articles/keystore) for hardware-backed encryption.

## 🔄 Workflow

1. **Authentication**: Users authenticate via the `AuthScreen` to gain access.
2. **Discovery**: The `TransferScreen` initiates Bluetooth discovery to find nearby devices.
3. **Secure Handshake**:
    - Initiator selects a device.
    - Both devices display a matching verification code.
    - Users confirm the code on both ends to establish an encrypted session.
4. **Data Transfer**: Files are streamed securely between devices with real-time progress tracking.
5. **Vault Management**: Received files are stored in the encrypted `Vault`, accessible only after re-authentication.

## 📦 Version Information

- **Current Version**: 1.0.0-alpha
- **Target SDK**: 35
- **Min SDK**: 26 (Android 8.0)
- **Compose BOM**: 2024.09.00

## 📂 Project Structure

```text
app/src/main/java/com/example/securetether/
├── data/           # Repository implementations & Data sources
│   ├── local/      # DataStore & Local File Handling
│   ├── security/   # Bluetooth encryption logic
│   └── repository/ # Concrete repository classes
├── domain/         # Business logic & Interfaces
│   ├── model/      # Domain entities
│   ├── security/   # Session & Keystore managers
│   └── repository/ # Repository definitions
├── di/             # Hilt Modules
└── ui/             # UI Components
    ├── screens/    # Compose Screens
    ├── viewmodel/  # ViewModels
    ├── navigation/ # Navigation graph
    └── theme/      # Material 3 Theme & Styling
```

## 🛠️ Setup & Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/SecureTether.git
   ```
2. Open the project in **Android Studio Ladybug (or newer)**.
3. Sync Project with Gradle Files.
4. Ensure Bluetooth and Location permissions are granted on your test devices.
5. Build and run the `:app` module.

---
*Developed as a secure alternative for local file sharing.*
