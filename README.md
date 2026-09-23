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
full **release** APK (R8 code shrinking + resource shrinking enabled) on
every push and uploads it as a workflow artifact.

The release build is signed with AGP's auto-managed debug keystore, so the
APK is installable out of the box with no signing secrets to configure. For
a Play Store submission, swap in a real upload keystore: add a
`signingConfigs.release` block in `app/build.gradle.kts` backed by keystore
credentials stored as GitHub Actions secrets, and point the `release`
build type's `signingConfig` at it instead.

To build locally with Android Studio or the Gradle CLI, you'll need the
Android SDK installed (`compileSdk 34`, `minSdk 26`):

```
gradle assembleRelease
```

## Permissions required on-device

1. **Notification access** — granted via Settings ▸ Apps ▸ Special access ▸
   Notification access (the app links directly to this screen).
2. **Post notifications** — the standard Android 13+ runtime permission,
   requested from the main screen.
