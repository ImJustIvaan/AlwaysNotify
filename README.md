# AlwaysNotify

An Android app that boosts notifications from apps you choose into a banner
that pops up on top of whatever app you're currently using — so you never
miss the ones that matter.

## How it works

- `AlwaysNotifyListenerService` is a `NotificationListenerService` that
  observes every notification posted on the device.
- For any app you've enabled in **Choose apps**, it hands the notification's
  content off to `OverlayBannerManager`, which draws it as a banner card at
  the top of the screen using a `TYPE_APPLICATION_OVERLAY` window, on top of
  whatever app is currently open — independent of the system's own heads-up
  notification UI, which some OEMs delay or suppress.
- **AlwaysNotify never posts an actual system notification.** The overlay
  banner is the only alert it shows, so there's no duplicate entry sitting
  next to the source app's own notification in the shade. This also means
  the banner only appears while the device is unlocked and in use — a
  normal app can't draw over the lock screen without posting a real,
  system-managed notification, so a locked device won't show anything from
  AlwaysNotify (the source app's own notification, if it posts one, still
  behaves normally there).
- The banner shows the app's label, title, text, icon, and the source
  notification's image if it had one (a photo, album art, a big-picture
  attachment). Tap it to open the source notification's content intent,
  tap the close button, or wait ~6 seconds for it to auto-dismiss.
- `MainActivity` walks through the two permissions the app needs
  (notification listener access, and "display over other apps") and links
  to **Choose apps**.
- `AppSelectionActivity` lists every installed app with a search box and a
  checkbox per app; selections are stored in `SharedPreferences` via
  `PrefsManager`.

## Project structure

```
app/src/main/java/com/alwaysnotify/app/
  AlwaysNotifyListenerService.kt  reads notifications, filters to selected apps
  OverlayBannerManager.kt         draws the boosted alert over other apps
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
2. **Display over other apps** (`SYSTEM_ALERT_WINDOW`) — required for the
   overlay banner; without it, AlwaysNotify has no way to show boosted
   alerts at all, since it doesn't post its own notifications.
