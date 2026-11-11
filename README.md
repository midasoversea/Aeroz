# PowerHub

Shizuku-powered Android power menu for quick access to screen off, reboot, recovery, bootloader, and power off.

## Planned features
- Reboot
- Reboot to recovery
- Reboot to bootloader / fastboot
- Power off
- Turn screen off
- Shizuku connection status and permission onboarding
- Light and dark themes with teal accents
- Android 11+ compatibility, with MIUI-focused testing

## Security and behavior
- Shizuku must be installed, started, and authorized.
- No root is required.
- The app does not bypass Android security.
- Some commands may be restricted or behave differently on OEM firmware.
- Wireless debugging-based Shizuku startup may need to be repeated after reboot.

## Build
Open in Android Studio and run:

```bash
./gradlew assembleDebug
```

APK:
`app/build/outputs/apk/debug/app-debug.apk`

## Requirements
- Android 11 or newer
- Shizuku running
- PowerHub authorized in Shizuku

## License
MIT
