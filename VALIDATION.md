# Smart Dialer v1.0.0 — pre-push validation (local, 2026-11-09)

Local static pass only. No Android SDK and no Gradle run exists on this
machine, so Kotlin compilation and resource linking are NOT proven here.
CI (JDK 21 + Gradle 8.7 + AGP 8.6.1) is the first real build.

## Checks that passed locally

- Stray backslash-quote scan over all 6 Kotlin files: none found.
- Brace balance and paren balance in all 6 Kotlin files: equal.
- Import-vs-use check: all imports used (2 unused imports found in the
  first pass and removed: PackageManager in CallHelper, ImageView in
  MainActivity). Second pass: 0 warnings.
- strings.xml: no unescaped apostrophes, no unescaped `&`.
- Dotted style names: only `Theme.SmartDialer`, and it carries a parent
  (`Theme.Material3.Dark.NoActionBar`). No other styles defined.
- Resource references: every `@color`, `@drawable`, `@string` used in
  layouts, themes and Kotlin resolves to a definition in the project.
  45 layout ids defined, all 45 referenced in Kotlin, none missing.
- Version consistency: `1.0.0` / `versionCode 1` in app/build.gradle
  and README.md agree. No other version string in the project.
- settings.gradle has pluginManagement + dependencyResolutionManagement
  repository blocks matching the plugins/deps used (google,
  mavenCentral, gradlePluginPortal).
- Manifest: only `.MainActivity` declared (app class). No external
  provider, receiver, service or activity is declared, so there is no
  external manifest class to check in dex.
- Manifest `<queries>`: launcher intent + one `<package>` entry per
  verified IP provider (Brilliant, Amber IT, OrbiTalk, Alaap, Dial,
  AgniTalk). Alo Talk has no verified package and no entry, by design.
- Keystore round-trip: `app/debug.keystore.b64` decodes byte-identical
  to `~/workspace/signing-keys/smartdialer-debug.keystore`.
- No `.github` directory in the project: the workflow file is kept
  out of the push set on purpose (see `~/workspace/smartdialer-workflow-android.yml`).
- No expression-body function with a bare `return` in any Kotlin file.

## Library artifacts inspected (exact pinned versions)

Downloaded from Google's Maven repo (dl.google.com) and inspected:

- material 1.12.0: `Theme.Material3.Dark.NoActionBar` present in the
  AAR (`res/values-night-v8/values-night-v8.xml`).
- recyclerview 1.3.2: `RecyclerView` and `LinearLayoutManager`
  classes present in classes.jar.
- appcompat 1.7.0: `AppCompatActivity` and `AlertDialog` present.
- core-ktx 1.13.1 + core 1.13.1 (transitive): `ContextCompat` and
  `ActivityCompat` present in core's classes.jar.
- AGP 8.6.1 plugin POM: reachable on dl.google.com (200).
- Kotlin 1.9.24 gradle plugin POM and stdlib jar: reachable on
  Maven Central (200).

Note: repo.maven.apache.org and the Google Maven Central mirror
returned 404/NoSuchKey for these same artifacts from this network
(Link3 transparent proxy), while dl.google.com served them fine.
CI uses google() + mavenCentral() as usual; if CI's Maven Central
fetch fails the same way, the `maven-central.storage-download.googleapis.com`
mirror already listed in settings.gradle is the fallback.

## Classes checked vs not checkable locally

Checked (listed above): Theme.Material3.Dark.NoActionBar,
RecyclerView, LinearLayoutManager, AppCompatActivity, AlertDialog,
ContextCompat, ActivityCompat, MainActivity (app class, in source).

Not checkable locally: Kotlin compilation, AAPT resource linking,
dex contents (no APK was built here — no Android SDK on this
machine), the Material3 theme rendering on a real phone, and the
SIM-slot ordering of `TelecomManager.getCallCapablePhoneAccounts`
(device-dependent, best effort).

## Known limits carried into v1.0.0

- AGP 8.6.1 (not BubbleDock's 8.5.2) because the task requires JDK 21
  in CI and AGP 8.6 is the first AGP line with JDK 21 support.
- Alo Talk package name is unverified (no Play listing found that
  could be tied to Alo Communications). It opens a Play Store search.
- IP calls open the provider app with the number prefilled; only the
  SIM route dials directly. This is an Android platform limit, not a
  bug in this app.
