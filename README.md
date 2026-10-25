# WOOP

**A calm Android planner for WOOP goal setting. It runs entirely on your phone.**

WOOP (Wish · Outcome · Obstacle · Plan) is the mental-contrasting method from Gabriele
Oettingen's research that Andrew Huberman covers in *Protocols* (Learning Protocol 2).

- **Write a WOOP**: one question per screen, with autosave, voice input and examples on tap.
- **Check in**: a one-minute re-read that tunes the plan when something didn't work.
- **Reminders**: nudges to write a new WOOP, plus a check-in schedule for each WOOP.
- **Home-screen widget**: the if-then plan of the WOOP you are focusing on, one glance away.
- **Science tab**: each finding the app relies on, with how strong the evidence is.

Everything stays on the phone. The app has no internet permission, no account and no
analytics. No streaks, no scores.

See [PLAN.md](PLAN.md) for the product, UX and architecture plan.

## Install

**Android:** download `WOOP-<version>.apk` from the
[latest release](https://github.com/likwiid/woop/releases/latest) and open it on your
phone. Allow installs from your browser when Android asks. It needs Android 8.0 or newer.

An APK signed with the WOOP release key is the official build. The certificate SHA-256
fingerprint of that key is:

```
52:DE:1A:03:CA:19:46:EA:AD:44:98:EA:E4:E2:93:A4:2A:B4:5D:E8:8E:F7:79:1B:A4:5B:CC:87:D0:D5:9B:D4
```

Check it with `apksigner verify --print-certs WOOP-<version>.apk`, and compare the
download with `SHA256SUMS.txt` on the release.

**iPhone:** there is no iOS version.

## Install with adb

Requirements: JDK 17+, the Android SDK (`ANDROID_HOME` or `local.properties`), and a
device with USB debugging on (Android 8.0+).

```bash
./install.sh
```

This builds the debug APK and installs it. With several devices connected, pass a serial
from `adb devices`:

```bash
./install.sh -s <serial>
```

Or do it by hand:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Develop

```bash
./gradlew testDebugUnitTest          # reminder scheduling tests
./gradlew connectedDebugAndroidTest  # Room database tests, on a device or emulator
./gradlew assembleRelease            # minified build, signed with the debug key so it also installs via adb
```

Stack: Kotlin, Jetpack Compose (Material 3), Navigation Compose, Room, Glance,
AlarmManager. minSdk 26, targetSdk 36.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Security issues go
to the address in [SECURITY.md](SECURITY.md), not to public issues. Changes are listed in
[CHANGELOG.md](CHANGELOG.md).

## License

WOOP is free software: you can redistribute it and modify it under the terms of the GNU
General Public License as published by the Free Software Foundation, either version 3 of
the License, or (at your option) any later version (**GPL-3.0-or-later**). See
[LICENSE](LICENSE) for the full text. Each source file carries the SPDX identifier
`GPL-3.0-or-later`.

The Atkinson Hyperlegible typeface is licensed separately under the SIL Open Font License
(see `FONT_LICENSE_OFL.txt`).

Copyright (C) 2026 Gabriel Ghoussoub.

## Credits

- **Gabriele Oettingen** and colleagues, whose research on mental contrasting and
  implementation intentions is the method this app walks you through. The Science tab
  lists every study the app relies on.
- **Andrew D. Huberman**, *Protocols: An Operating Manual for the Human Body*, Learning
  Protocol 2, which presents the method. The app describes it in its own words.
- Typeface: [Atkinson Hyperlegible](https://www.brailleinstitute.org/freefont/) by the
  Braille Institute.

## Disclaimers

WOOP is an independent project and is not affiliated with or endorsed by Gabriele
Oettingen, WOOP My Life, Andrew Huberman or Huberman Lab.

**Not medical or psychological treatment.** WOOP is a self-help tool. If you are
struggling, talk to a doctor or a mental health professional.

## Built by Likwiid

WOOP is built and maintained by [Likwiid](https://likwiid.com).
