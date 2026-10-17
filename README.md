# 🐍 Snake3D

A 3D Snake game built with **Java 17 + JavaFX 17**, featuring three AI-controlled villain snakes, skins
(Classic / Neon / Rainbow / Ocean / Lime / Cosmic), golden fruits, special items, sounds and an
animated background video.

![Java](https://img.shields.io/badge/Java-17-blue) ![JavaFX](https://img.shields.io/badge/JavaFX-17.0.16-orange) ![Desktop](https://img.shields.io/badge/Desktop-Windows-green) ![APK](https://img.shields.io/badge/Android-APK-red)

## 🖥️ Desktop (Windows)

Prebuilt app in `dist/Snake3D/Snake3D.exe` (jpackage app-image, includes bundled JRE).

Run from source:

```bash
mvn javafx:run
```

## 📱 Android APK

The APK is built automatically by GitHub Actions (`.github/workflows/android.yml`)
on every push to `main` using GraalVM + [GluonFX](https://github.com/gluonhq/gluonfx-maven-plugin).

**Download:** open the [Actions tab](https://github.com/Rkshah123/Snake-3D/actions) → latest
*Build Android APK* run → artifact `Snake3D-APK`, or grab it from the
[`apk` release](https://github.com/Rkshah123/Snake-3D/releases/tag/apk).

> The GluonFX toolchain cannot cross-compile to Android on Windows, so local APK builds require a
> Linux environment (WSL2 works: `wsl-setup.sh` / `wsl-build.sh` in this repo automate that).

Install on your phone:

1. Copy the APK to your Android device (min. Android 8.x)
2. Open it → allow *Install from unknown sources* for the browser/file manager
3. Install → enjoy 🎮

## 🎮 Controls

| Key | Action |
|---|---|
| `W / ↑` | Up |
| `S / ↓` | Down |
| `A / ←` | Left |
| `D / →` | Right |
| `W / A / S / D` at the start screen | Cycle skins |
| `Enter / Space` at the start screen | Play |

Eat fruits to grow and speed up. Golden fruits give a temporary speed boost.
Special items (eagle / rodent / mongoose) shrink your snake. Avoid the three villain snakes!

## 🛠️ Tech notes

- Maven + JavaFX 17 (`javafx-controls`, `javafx-media`, `javafx-graphics` — win classifier)
- Desktop packaging via `jpackage` (module-path launcher, bundled runtime)
- Android packaging via GluonFX substrate (GraalVM native image)
- Sounds: `eat.wav`, `gameover.wav`, `gamestart.wav`, `s1.wav`, `s2.wav`; background video: `bgvideo.mp4`
- Icon: `icon.png` (runtime) / `icon.ico` (Windows exe)
