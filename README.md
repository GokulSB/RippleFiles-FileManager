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

```bash
cd android
./gradlew assembleDebug
```
