# AlwaysNotify

An Android app that boosts notifications from apps you choose into large,
high-priority alerts that show as heads-up banners and are fully visible on
the lock screen — so you never miss the ones that matter.

## How it works

- `AlwaysNotifyListenerService` is a `NotificationListenerService` that
  observes every notification posted on the device.
- For any app you've enabled in **Choose apps**, it reposts a boosted copy
  on a dedicated `IMPORTANCE_HIGH` notification channel with
  `VISIBILITY_PUBLIC`, so the system shows it as a heads-up banner and
  displays its full content on the lock screen.
- `MainActivity` walks through the two permissions the app needs
  (notification listener access, and `POST_NOTIFICATIONS` on Android 13+)
  and links to **Choose apps**.
- `AppSelectionActivity` lists every installed app with a search box and a
  checkbox per app; selections are stored in `SharedPreferences` via
  `PrefsManager`.

## Project structure

```
app/src/main/java/com/alwaysnotify/app/
  AlwaysNotifyApp.kt              creates the notification channel
  AlwaysNotifyListenerService.kt  the core notification-boosting logic
  MainActivity.kt                 permission setup screen
  AppSelectionActivity.kt         per-app selection screen
  AppListAdapter.kt               RecyclerView adapter for the app list
  AppInfo.kt                      simple data holder
  PrefsManager.kt                 SharedPreferences-backed selection store
```

## Building

A GitHub Actions workflow (`.github/workflows/android-build.yml`) builds a
debug APK on every push and uploads it as a workflow artifact.

To build locally with Android Studio or the Gradle CLI, you'll need the
Android SDK installed (`compileSdk 34`, `minSdk 26`):

```
gradle assembleDebug
```

## Permissions required on-device

1. **Notification access** — granted via Settings ▸ Apps ▸ Special access ▸
   Notification access (the app links directly to this screen).
2. **Post notifications** — the standard Android 13+ runtime permission,
   requested from the main screen.
