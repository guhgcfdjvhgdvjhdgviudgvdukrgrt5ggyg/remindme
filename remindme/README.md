# RemindMe - Reminder & Alarm App

Simple, reliable reminder app with lock-screen full-screen alarm.

## Features (MVP)
- Add/Edit/Delete reminders
- Date + Time picker
- Repeat (Once/Daily/Weekly/Monthly)
- Snooze + Done
- Vibration
- Lock screen full-screen alarm
- Heads-up notification when unlocked
- Phone restart support (structure ready)

## Build APK (Local)
1. Install Flutter SDK
2. cd remindme
3. flutter pub get
4. flutter build apk --release

APK path: `build/app/outputs/flutter-apk/app-release.apk`

## GitHub Upload
Upload this `remindme/` folder as a repo on GitHub. Then build APK locally and release if desired.

## Note (Battery Optimization)
On Xiaomi/Oppo/Vivo/Realme/Infinix/Tecno phones: set app to "Unrestricted" in Battery Optimization + Auto-start ON for reliable alarms.
