# RippleFiles-File Manager

RippleFiles is a beautifully designed, expressive file manager for Android, built with modern Kotlin and Jetpack Compose. It features a unique, customizable design language called "Skyline Ledger" that combines hard-edged geometric shapes with elegant typography and fluid micro-animations.

## Features
- **Local & Cloud Storage**: Seamlessly browse local files alongside Google Drive, Dropbox, and MEGA.
- **Skyline Ledger UI**: A brutalist yet playful aesthetic with customizable corner roundness, font styles, and amber accents.
- **Expressive Animations**: Rail slide tab indicators, icon lifts, and fluid layout transitions.
- **Built-in Tools**: Zip extraction, batch file renaming, predictive back navigation, and a robust document viewer for PDF and DOCX files.
- **Storage Cleaner**: A sleek, graphical breakdown of your storage with smart categorizations.

## Tech Stack
- Kotlin
- Jetpack Compose (Material 3)
- AndroidX Navigation & ViewModel
- Immutable Collections (`kotlinx.collections.immutable`)
- Apache POI (for DOCX parsing)

## Building
This project requires Android SDK 35 (or 36, depending on your setup) and JDK 17+.

## Downloads
[![Get it on Google Play](https://img.shields.io/badge/Get%20it%20on-Google%20Play-green?logo=google-play)](https://play.google.com/store/apps/details?id=com.ripple.filemanager)

## Screenshots
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/0a9faf83-2bd5-4002-938f-84ca2482fca6" />
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/ea0a4b2b-8466-40a5-8405-7ddfeadf75d9" />
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/cf740c3c-444b-4248-a94a-a2ff7bdb4de6" />
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/9974b81c-b7de-4739-bba6-39db80a8cedc" />
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/236eebcd-5a1d-4e34-b287-cd27d46a013e" />
<img width="1080" height="2424" alt="Image" src="https://github.com/user-attachments/assets/96779b73-60cf-46c6-9b17-5807bfac0cfa" />

```bash
cd android
./gradlew assembleDebug
```
