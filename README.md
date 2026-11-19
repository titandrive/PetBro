# PetBro

A native Android companion for Petlibro feeders: feed with one tap, set your own meal schedule, and see how often your pet ate today. It's built for the **One RFID Smart Feeder (PLAF301)** and talks to Petlibro's US cloud the same way the official app does.

Not affiliated with Petlibro.

## How it works

- **Sign in** with a US Petlibro account that has access to your feeder (yours, or one the feeder is shared with). The password is sent once to get a session and never stored; the session token is encrypted with an Android Keystore key.
- **Requests:** plain HTTPS calls to `api.us.petlibro.com`, the same endpoints the Petlibro app uses. They're not a public API, so a Petlibro change can break them.
- **Tabs** along the bottom: Feeder, Schedule, History and Settings, with the paw button in the middle.
- **Starts instantly:** the devices and today's numbers from last time show at once, while a quiet check runs in the background (the refresh button spins; a note says if Petlibro couldn't be reached).

## Feed now

- **Tap the paw** in the middle of the tab bar to feed straight away.
- **Hold the paw** to change how many portions a tap gives (1–48), or to dispense now.
- The same amount is used by the Quick Settings tile, the launcher shortcut and automation, and can be set under **Settings → Quick feed**.

## Schedule

The Schedule tab works like PurgeBot's: one switch to feed automatically, then a card per meal.

| Option | Choices |
|---|---|
| Repeats | Daily, weekly on chosen days, or monthly on a day (or the last day) |
| Time | A clock-dial picker, following the phone's 12- or 24-hour setting |
| Portions | 1–48 per meal |
| Meals | As many as you like, each with its own name, and on or off |

Meals run on exact alarms, so they go out on time with the app closed, and are set again after a reboot or clock change. A meal that comes more than 5 minutes late (the phone was off, say) is skipped rather than served late. Each scheduled meal posts a notification: the portions when it's served, or an error saying why it didn't go out. Turning the schedule on asks for the app to be exempt from battery optimization, so Doze doesn't hold meals back.

The schedule runs on the phone: the phone has to be on and online at meal time.

## Feeder tab

- **Feeder:** the feeder and whether it's ready. With several PLAF301s on the account, pick the one PetBro feeds.
- **Meals:** the next meal, how often your pet ate today and for how long (e.g. *Ginny ate today: 7 times · 18 min*), and the last feed.
- **Account:** who's signed in, and sign out.

The eating numbers come from the feeder's RFID counts. Petlibro reports how many times and how long the pet ate today, not when.

## History

Every feed from the app, the schedule, the tile, shortcut, widget and automation, with its time, portions and whether Petlibro accepted it. Kept on the phone (the last 200); clearing it doesn't touch Petlibro's own log.

## Widget

A home-screen widget that feeds on a tap. It changes with its size:

| Size | Shows |
|---|---|
| Small | The paw and the portions a tap gives |
| Medium | The last feed's time, and *Tap to feed* |
| Large | That, plus today's feeds, how often your pet ate, and the next meal |

Each widget sets its own portions when it's placed (or from its long-press menu). The look is shared by every widget, as in Paid to Poop: match the app's theme or pick one, dark mode (App, System, Light or Dark), background opacity, show the last feed or just the paw, and whether a tap feeds or opens the app. These are also under **Settings → Widget look**.

## Tile, shortcut and automation

These feed the quick-feed amount with no confirmation.

- **Quick Settings tile:** add *Feed now* from the Quick Settings editor. It shows the portions, and *Feeding…* while a request is out.
- **Shortcut:** long-press the app icon.
- **Tasker and MacroDroid:** turn on **Settings → Automation → Allow automation apps** (off by default, so no other app can feed your pet unasked), then send a broadcast intent:

| Field | Value |
|---|---|
| Action | `local.petlibrocompanion.action.FEED_NOW` |
| Package | `local.petlibrocompanion` |
| Class | `local.petlibrocompanion.FeedNowReceiver` |
| Extra (optional) | `portions`, an integer from 1 to 48 |

Taps on the widget, tile and shortcut give one buzz. PetBro runs in the background for these, where Android drops ordinary vibrations, so they use notification vibration (no notification is shown); with notification vibration off, they're silent.

## Settings

- **App theme:** System, Light or Dark (with AMOLED black, and Ultra AMOLED for black cards), and the twelve themes shared with PurgeBot, each with a light and dark version. The sun/moon in the header flips light and dark; long-press it to follow the system.
- **Feedback:** haptics on or off.
- **Quick feed**, **Widget look** and **Automation**, above.

## Code

| File | What it holds |
|---|---|
| `MainActivity.kt` | Compose UI: the tabs, Feed Now, the Feeder, Schedule, History and Settings screens |
| `Components.kt` | Shared controls from PurgeBot: cards, switches, pill selectors, steppers, the theme picker and the clock dial |
| `Petlibro.kt` | Petlibro's API calls, and the Keystore-encrypted session with its cached devices and today's numbers |
| `Feeder.kt` | Account state, sign-in and background checks, feeding, the feed history and meal notifications |
| `Schedule.kt` | Meals, the exact alarm that serves them, and the receiver that resets it after a reboot |
| `FeedNow.kt` | The tile, shortcut activity and automation receiver, and the key that keeps other apps from feeding |
| `FeedWidget.kt` | The widget, its tap, its settings page and shared look |
| `Prefs.kt` | App preferences: haptics, quick-feed portions, automation |
| `Theme.kt` | Palettes, theme families and the type scale |
| `Haptics.kt` | Vibration patterns |
| `LucideIcons.kt` | Lucide icons |

## Credits

- Petlibro API calls follow the [Home Assistant Petlibro integration](https://github.com/jjjonesjr33/petlibro).
- UI, themes and shared controls from PurgeBot; widget settings from Paid to Poop.
- UI font: [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) (SIL Open Font License 1.1).
- Palettes: [Catppuccin](https://catppuccin.com), [Dracula](https://draculatheme.com) and Alucard, [Gruvbox](https://github.com/morhetz/gruvbox), [Nord](https://www.nordtheme.com), and the rest shared with Paid to Poop.
- Icons: [Lucide](https://lucide.dev) (ISC License).

## Building

Uses Android Studio's bundled JDK and the Android SDK (`~/Android/Sdk`).

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`. Install it with `adb install -r app/build/outputs/apk/release/app-release.apk`.

Builds are signed with `petbro-debug.p12` in the project folder when it's there (it's kept out of git), so updates install over the first PetBro and keep its sign-in; otherwise they use the usual debug key.
