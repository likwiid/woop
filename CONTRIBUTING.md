# Contributing to WOOP

Thanks for taking the time to help. Bug reports, fixes, tests and small focused
features are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on
  the approach before you spend time on it.
- Read [PLAN.md](PLAN.md) for the product, UX and architecture decisions, and
  the evidence each feature rests on.
- By contributing you agree that your work is licensed under the project's
  license, GPL-3.0-or-later.

## Setup

You need JDK 17 or newer and the Android SDK (`ANDROID_HOME` or
`local.properties`).

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

To install on a phone or emulator with USB debugging on, run `./install.sh`
(see the README).

## Making changes

1. Fork the repository and create a branch from `main`, named with a prefix:
   `feat-`, `fix-`, `docs-`, `refactor-`, `test-` or `chore-`, for example
   `fix-reminder-after-reboot`.
2. Keep each pull request focused on one thing.
3. Write commit messages as a single line: `prefix: description`, for example
   `fix: reschedule check-ins after the phone restarts`.
4. Add or update tests for logic changes, especially reminder scheduling.
5. Run `./gradlew testDebugUnitTest assembleDebug` before you push. CI runs the
   same checks.
6. Add the SPDX header to new source files:

   ```kotlin
   // SPDX-License-Identifier: GPL-3.0-or-later
   ```

## Code guidelines

- Kotlin, Jetpack Compose and Material 3. Comments explain why, not what.
- Keep the app fully local: no internet permission, accounts, servers, sync or
  analytics. A change that needs the network will not be merged.
- Keep it calm: no streaks, scores, badges or guilt-driven notifications.
- Science claims in the app must cite a source and say how strong the
  evidence is. Do not overstate findings, and do not quote books or podcasts.
- Use plain hyphens in user-visible text, not em or en dashes.

## Reporting bugs

Use the bug report template. Include your phone model, Android version and, if
you can, the steps to reproduce. Please do not paste the text of your own
WOOPs into issues.

## Security issues

Do not open a public issue. See [SECURITY.md](SECURITY.md).

## Code of conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By
taking part you agree to uphold it.
