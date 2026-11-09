# Smart Dialer v1.0.2 — pre-push validation (local, 2026-11-09)

Local static pass only. No Android SDK and no Gradle run exists on this
machine, so Kotlin compilation and resource linking are NOT proven here.
CI (JDK 21 + Gradle 8.7 + AGP 8.6.1) is the first real build.

## v1.0.2: totally English UI (Tamim, 18:37, 9 Nov 2026)

After v1.0.1, Tamim asked for the UI totally in English. Every
user-visible Bangla string was replaced with English:

- `strings.xml` (22 strings): Auto-call toasts, Default route/set,
  "Enter a number first", number hint "Enter number", "Call",
  "Search contacts", "Favourites" / "All contacts", favourite
  added/removed, Recents/Contacts empty texts, "Clear", "Settings",
  "Default provider", "Choose SIM (for Original SIM)", settings
  note (IP-call limit, in English), "Save", "Cancel", "Default" badge.
- Kotlin (22 replacements): `CallHelper` results ("Calling...",
  "Dialer opened", "Could not open dialer", "No call permission,
  dialer opened", IP-open / Play Store messages), installed labels
  ("Installed" / "Not installed" / "Not verified") in `Adapters` and
  `CallHelper`, Play Store toasts ("Play Store could not be opened").
- Dial pad: key 1 sub-label "অ" removed (blank, like English dial
  pads); the button text is now "1\n" — digit only.
- Provider names and company names unchanged — proper names, already
  English. Layout, colours, sizes and all behaviour unchanged from
  v1.0.1: the v1.0.1 dial-pad/tint fixes are untouched.

Proof: python scan of `app/src/main` for U+0980–U+09FF (includes
Bengali digits U+09E6–U+09EF) returns **0 hits** in res/ and Kotlin.
No Bangla remains in any user-visible string. No Bangla in code
comments either (the scan covers comments; 0 hits total). The only
Bangla left in the project is in README.md / VALIDATION.md history
text (documentation for Tamim, not app UI) — and the v1.0.2 entries
there are in English.

Version consistency: `1.0.2` / `versionCode 3` in app/build.gradle,
README version line + history, proguard comment. `1.0.1` remains only
in history text. strings.xml: no unescaped apostrophes (new English
strings use "does not", no contractions). All `@string` refs still
resolve — no string name was added, removed or renamed, only values
changed. No `.github` in the project; package, keystore and workflow
file untouched.

## What v1.0.0 got wrong on the phone (Tamim, 18:26, 9 Nov 2026)

Tamim's screenshot of v1.0.0 on his phone: dial pad completely missing,
bottom nav three solid-green buttons with no labels, SIM 1 and SIM 2 both
solid green with no selected state. He said it was nowhere near the
approved preview. All three were real bugs in the code, not phone quirks.

Root causes (verified in the v1.0.0 files before fixing):

1. Dial pad invisible — `setupDialPad()` built plain buttons with
   `GridLayout.LayoutParams` width=0, height=0, weight 1f, inside a
   `wrap_content` GridLayout, so every button measured 0x0. The
   provider-list RecyclerView (`0dp` + `layout_weight="1"`) ate all the
   space above it, so the pad had nowhere to go.
2. Green tint — the theme is `Theme.Material3.Dark.NoActionBar` with
   `colorPrimary` = `#22C55E`. Under AppCompat/Material, `android.widget
   .Button` is inflated as a Material button and gets
   `backgroundTint=colorPrimary` by default, painting over the dark
   `bg_card` / `bg_card_selected` drawables. That hit SIM 1/2, the three
   bottom tabs, the call button (green by accident there) and every dial
   pad button. Provider cards are plain layouts, which is why they
   rendered dark and correct in the screenshot.
3. Layout order — provider list on top uncapped, then SIM row, number
   display, (missing) pad, call row, nav. Nothing guaranteed the pad
   stayed on screen.

## v1.0.1 fixes

- Dial pad + call row moved OUT of the weighted FrameLayout into their
  own `dialPadSection` below it, shown only on the Dialer tab. The
  provider list is now the only weighted child, takes the leftover
  space and scrolls internally — it can never push the pad off-screen.
- Dial pad rebuilt as 4 fixed rows of fixed-size TextViews (64dp,
  4 rows x (64dp + 8dp margins) = 288dp total) inside the `dialGrid`
  LinearLayout. No GridLayout, no 0-size weights anywhere in the pad.
- Every tinted `Button` replaced with TextView: SIM 1/2, call button,
  the three bottom tabs, clear-recents. TextView takes no
  backgroundTint, so `bg_card` / `bg_card_selected` render as drawn.
  Selected SIM: green text + green-stroke card; unselected: white text
  on the dark card. Selected tab: green text + green icon; unselected
  tabs: grey text + grey icon (`compoundDrawableTintList`, API 23+,
  minSdk 24). No tab is ever solid green and labels are always visible.
- Remaining `Button` uses are only RadioButton (settings dialog),
  AlertDialog positive/negative buttons (system), and ImageButton
  (settings/backspace — rendered correctly in v1.0.0).

## Layout arithmetic (fixed vs flexible)

Fixed, outside the weighted area: top bar ~56dp + default label ~24dp
+ dial pad 288dp + call row 72dp + bottom nav 56dp = ~496dp.
Flexible (FrameLayout, weight 1): provider list + SIM row 44dp (only
when SIM is selected) + number display ~44dp + suggestions (~36dp,
only while typing — shrinks the list, never the pad).
On Tamim's phone (~900dp usable) the list shows ~4 provider cards and
scrolls for the other 4; on a 700dp screen it shows ~1.5 cards and
scrolls — the pad stays fully visible either way.

## Preview-match audit (preview artifact `smart-dialer` = final target)

Checked element by element against the approved preview and Tamim's
phone screenshot:

- Provider card: dark card `#131B2E`, 16dp rounded corners, 52dp green
  circle with white bold initials, name 16sp bold white + subtitle
  13sp grey, right side IP/SIM badge green bold + status grey +
  ডিফল্ট green. Selected card: green 2dp stroke. Matches preview.
  v1.0.1 adds 8dp bottom margin per card so cards are separated
  rounded cards like the preview (v1.0.0 cards touched each other);
  same margin added to Recents/Contacts item cards.
- Nav bar: 56dp dark `#131B2E` bar, icon + label per tab
  (Dialer / Recents / Contacts). Selected tab green text + green
  icon, other two grey text + grey icon. Matches preview.
- Auto-call pill: green pill, white bold text, settings button on a
  dark card at the right. Matches preview (preview gear =
  `ic_menu_preferences` wrench here; no framework gear drawable).
- Number display: 26sp centred, grey "নম্বর লেখো" hint. Matches.
- Dial pad: 64dp dark oval buttons, digit + letters, centred rows.
  Matches the preview's round pad.
- Call button: green pill, white bold "কল". Matches.
- SIM row: only shown for Original SIM; selected slot green,
  unselected white-on-dark. Matches the preview's SIM 1/2 choice.

## Checks that passed locally (scripted scan, ALL PASS)

- Stray backslash-quote scan over all 6 Kotlin files: none found.
- Brace, paren and bracket balance in all 6 Kotlin files: equal.
- Import-vs-use check: all imports used (ColorStateList added for the
  tab icon tint, used 4 times; Button/GridLayout imports removed).
- strings.xml: no unescaped apostrophes, no unescaped `&`.
- Dotted style names: only `Theme.SmartDialer`, and it carries a parent
  (`Theme.Material3.Dark.NoActionBar`). No other styles defined.
- Resource references: every `@color`, `@drawable`, `@string` used in
  layouts, themes and Kotlin resolves to a definition in the project
  (framework `android.R.drawable.btn_star_big_on/off` correctly
  excluded from the app-resource check). 46 layout ids defined, all 46
  referenced in Kotlin, none missing.
- Version consistency: `1.0.1` / `versionCode 2` in app/build.gradle
  and README.md agree. `1.0.0` remains only in history text (README,
  VALIDATION) and in v1.0.0 code comments.
- No `<Button` element and no GridLayout in any layout; no plain
  `Button(` constructor in any Kotlin file (RadioButton + AlertDialog
  buttons excepted).
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
  to `~/workspace/signing-keys/smartdialer-debug.keystore` (keystore
  untouched by this fix).
- No `.github` directory in the project: the workflow file is kept
  out of the push set on purpose (see
  `~/workspace/smartdialer-workflow-android.yml`).

## Library artifacts inspected (exact pinned versions)

Downloaded from Google's Maven repo (dl.google.com) and inspected
(v1.0.0, unchanged in v1.0.1):

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
ContextCompat, ActivityCompat, ColorStateList (framework class,
API 23+, minSdk 24), MainActivity (app class, in source).

Not checkable locally: Kotlin compilation, AAPT resource linking,
dex contents (no APK was built here — no Android SDK on this
machine), the Material3 theme rendering on a real phone, and the
SIM-slot ordering of `TelecomManager.getCallCapablePhoneAccounts`
(device-dependent, best effort). The v1.0.1 layout/tint fixes are
proven only by the structural checks above until Tamim confirms them
on his phone.

## Known limits carried into v1.0.1

- AGP 8.6.1 (not BubbleDock's 8.5.2) because the task requires JDK 21
  in CI and AGP 8.6 is the first AGP line with JDK 21 support.
- Alo Talk package name is unverified (no Play listing found that
  could be tied to Alo Communications). It opens a Play Store search.
- IP calls open the provider app with the number prefilled; only the
  SIM route dials directly. This is an Android platform limit, not a
  bug in this app.
- Workflow file (`~/workspace/smartdialer-workflow-android.yml`) was
  NOT changed in this fix, per instructions — it still names the APK
  and artifact `SmartDialer-v1.0.0-debug`. It must be updated to
  `v1.0.1` via the browser before the v1.0.1 push, or CI will produce
  a v1.0.0-named artifact from v1.0.1 code.

## v1.0.0 validation (kept for history)

Local static pass only. No Android SDK and no Gradle run exists on this
machine, so Kotlin compilation and resource linking are NOT proven here.
CI (JDK 21 + Gradle 8.7 + AGP 8.6.1) is the first real build. v1.0.0 CI
did pass (run #2, 2m 6s) — and the app still failed on the phone, which
is why this file now records layout/tint structure, not just a green
local scan: a passing CI build does not prove the UI renders right.
