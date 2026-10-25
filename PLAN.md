# WOOP — Product & Architecture Plan

An Android app for **mental contrasting with implementation intentions (MCII)**, the
method Gabriele Oettingen calls **WOOP** (Wish · Outcome · Obstacle · Plan) and that
Andrew Huberman presents in *Protocols* (Learning Protocol 2: "Use Mental Contrasting to
Achieve a State of Urgency").

Core jobs:

1. **Write** a WOOP (guided, one question at a time).
2. **Review** a WOOP (a short check-in that re-runs the contrast and tunes the plan).
3. **Remind** you to create a WOOP, and to check in on the ones you have.

Design constraints: ADHD-friendly, uncluttered, one-handed, offline, private, installable
with a single `adb install`.

---

## 1. The science the app is built on

| # | Finding | Source | Evidence | What the app does with it |
|---|---------|--------|----------|---------------------------|
| 1 | Positive fantasies alone **lower** energy (lower systolic blood pressure, less effort) and predict poorer achievement. | Kappes & Oettingen 2011, *JESP* 47(4):719–729; Oettingen & Mayer 2002, *JPSP* 83(5):1198–1212 | Multiple experiments | The Outcome step is never the last step. The flow always moves from the dream straight into the obstacle. |
| 2 | Contrasting the desired future with the present obstacle **energizes** and builds commitment. | Oettingen et al. 2009, *PSPB* 35(5):608–622 | Multiple experiments | Outcome → Obstacle are sequential screens, with an optional "imagine it" pause on each. |
| 3 | Order matters. Mental contrasting (future first, then reality) produces **selective commitment**: strong when success is likely, letting go when it isn't. Indulging or dwelling alone don't. | Oettingen, Pak & Schnetter 2001, *JPSP* 80(5):736–753 | Multiple experiments | Steps are locked in W→O→O→P order. A "how likely?" check after the Wish feeds a gentle "shrink it or let it go" path. |
| 4 | Mental contrasting **links obstacles to the behaviour** that overcomes them. | Kappes et al. 2012, *JESP* 48(4):811–818 | Experiments | Each obstacle gets its own if-then plan, pre-filled from the obstacle text. |
| 5 | **If-then plans** (implementation intentions) have a medium-to-large effect on goal attainment (d ≈ 0.65, 94 tests). | Gollwitzer 1999, *Am Psychol* 54:493–503; Gollwitzer & Sheeran 2006, *Adv Exp Soc Psychol* 38:69–119 | Meta-analysis | Plan step uses a strict "If … then I will …" template. The if-then is shown on the Today focus card and inside every check-in notification so the cue gets rehearsed. |
| 6 | **MCII (WOOP)** as a combined intervention: small-to-medium effect, g = 0.34 (21 studies, ~15.9k people). Interactive delivery beat paper-based (g = 0.47 vs 0.28). | Wang, Wang & Gai 2021, *Front Psychol* 12:565202 | Meta-analysis (authors flag possible publication bias) | The app is interactive by design, not a static worksheet. |
| 7 | MCII improves self-regulation in children **at risk for ADHD**, with stronger benefits than for peers. If-then plans also improve response inhibition in ADHD. | Gawrilow et al. 2013, *Motiv Emot* 37:134–145; Gawrilow & Gollwitzer 2008, *Cogn Ther Res* 32:261–280 | RCT + experiments (children) | The core method is a fit for ADHD; the UX below removes the remaining friction. |
| 8 | Letting go of goals that have become unattainable is linked to better well-being. | Wrosch et al. 2003, *PSPB* 29:1494–1508 | Longitudinal studies | "Let it go" is a first-class, guilt-free outcome next to "Achieved". |
| 9 | Announcing identity goals publicly can give a premature sense of completion and reduce effort. | Gollwitzer et al. 2009, *Psych Sci* 20(5):612–618 | Experiments (identity goals) | No social features. Everything stays on the phone. No `INTERNET` permission. |
| 10 | Mental imagery evokes stronger emotion than verbal thought about the same content. | Holmes & Mathews 2005, *Emotion* 5(4):489–497 | Experiments | Optional 30-second "Imagine it" timers on Outcome and Obstacle. |
| 11 | A cyclic sigh (double inhale, long exhale) lowers physiological arousal and lifts mood. | Balban et al. 2023, *Cell Rep Med* 4(1):100895 | RCT | One-line tip on the Obstacle imagery screen, where thinking about obstacles can raise stress. |
| 12 | Huberman's own practice: write the answers down (~10 min the first time), then **re-read and edit** whenever motivation dips (~5 min). He frames the target state as *urgency*, not motivation. | *Protocols*, Learning Protocol 2 | Practitioner report | Check-in flow = re-read + tune. The Today focus card keeps the if-then in view with a one-tap "Check in". |
| 13 | Huberman's hypothesis that WOOP may engage the anterior mid-cingulate cortex (aMCC, "tenacity" region). He notes there is no direct data yet. | Touroutoglou et al. 2020, *Cortex* 123:12–29 | **Hypothesis only** | Shown in the Science tab with a "Hypothesis" label. Not used to justify any feature. |

### How Huberman's version differs from Oettingen's, and what the app picks

- **Wish.** Huberman: name skills, credentials, proficiencies, not feelings. Oettingen: an
  important wish that is challenging but feasible. **App:** both. The prompt asks for a
  concrete result, then checks how likely it feels.
- **Outcome.** Huberman: what you will *feel*. Oettingen: the single best outcome,
  imagined vividly. **App:** "What's the best result, and how will it feel?"
- **Obstacle.** Huberman: hyper-realistic roadblocks, including how you'll react.
  Oettingen: the main *inner* obstacle (habit, emotion, belief). **App:** asks for the main
  thing *in you* first, allows up to 3 obstacles, and asks "how will you react?"
- **Plan.** Both: if-then statements. **App:** one if-then per obstacle; the "if" is
  pre-filled from the obstacle and the "then" is a single, small action.

---

## 2. UX principles (ADHD-friendly ergonomics)

| Principle | Implementation |
|-----------|----------------|
| One question per screen (low working-memory load) | The create and review flows are full-screen steppers. Previous answers appear as a small coloured "context chip" at the top, so nothing has to be held in memory. |
| One primary action, in the thumb zone | A single full-width 64 dp button pinned to the bottom, lifted above the keyboard (`imePadding`). Secondary actions are text buttons. |
| Big touch targets (Fitts's law) | ≥ 56 dp for choices, ≥ 48 dp for everything else. Choices are large cards, not radio buttons or sliders. |
| Few choices (Hick's law) | 3–5 options max per screen. 3 tabs total. |
| Wayfinding by colour | Each WOOP letter has its own colour (Wish indigo, Outcome amber, Obstacle coral, Plan teal), used everywhere. |
| Visible progress and time | Segmented 4-colour progress bar plus "About 5 min" on the start screen. The bar fills as you near the end. |
| Interruption-proof | Autosave on every keystroke into a draft. Leaving mid-flow is safe; Today offers "Continue your draft". |
| Lower the cost of starting | Notifications open straight into the flow. Examples are one tap away. Mic button on every text field (Android speech input). |
| No shame mechanics | No streaks that break. Today shows "check-ins done" as a cumulative count. Copy is warm and short. "Let it go" is celebrated, not hidden. |
| Legibility | Atkinson Hyperlegible typeface (Braille Institute), 18 sp body, 1.5 line height, WCAG AA contrast, respects system font size and dark mode. |
| Calm motion | Gentle animations only, all disabled when the system "Remove animations" setting is on. Light haptic on completion. |

---

## 3. Information architecture

```
Bottom tabs:   [ Today ]   [ Reminders ]   [ Science ]      (Settings = gear on Today)

Today
 ├─ Focus card (pinned WOOP: wish + if-then, "Check in" button)
 ├─ "Continue your draft" (if any)
 ├─ Due for check-in
 ├─ Your WOOPs (Active / Achieved / Let go)
 └─ [ + New WOOP ]  (extended FAB, bottom-right)

New WOOP (full-screen stepper)
 Start ─ Wish ─ Likelihood ─ Outcome ─ (Imagine) ─ Obstacles ─ (Imagine) ─ Plans ─ Summary + check-in schedule ─ Done

WOOP detail
 W / O / O / P cards (tap to edit) · Check in · Pin as focus · Achieved · Let go · Delete
 Check-in history · Check-in schedule

Check-in (review) flow, about 1–2 min
 Re-read & re-imagine ─ Did the obstacle show up? ─ How's it moving? ─ (Tune the plan / still doable?) ─ Note ─ Done

Reminders
 WOOP prompts (reminders to create a WOOP: days + time, presets)
 Check-ins (per-WOOP schedule)
 Permission banners (notifications, precise timing)

Science
 The method in 4 steps · 13 evidence cards with "Strong / Good / Early / Hypothesis /
 Practitioner" badges · tap a card for the detail, "In this app" and its sources
```

---

## 4. Data model (Room / SQLite)

```
goals      id · wish · outcome · timeframe(TODAY|WEEK|MONTH|LONGER) · likelihood(1–5)
           status(DRAFT|ACTIVE|ACHIEVED|LET_GO) · pinned · createdAt · updatedAt
           lastReviewedAt · checkInEveryDays(0=off) · checkInHour · checkInMinute
obstacles  id · goalId → goals · position · text · ifCue · thenAction
reviews    id · goalId → goals · at · planUse(USED|MISSED|NOT_YET) · momentum(MOVING|STUCK|DONE)
           stillDoable · note
prompts    id · label · hour · minute · daysMask(Mon=bit0 … Sun=bit6) · enabled
```

---

## 5. Architecture

- **Kotlin 2.1 + Jetpack Compose (Material 3)**, single activity, Navigation Compose.
- **MVVM**: one `ViewModel` per screen exposing `StateFlow` UI state; a `WoopRepository`
  over Room DAOs. A small manual `AppContainer` (no Hilt) keeps the build simple.
- **Room 2.7 + KSP** for storage. **SharedPreferences** for two flags (onboarding done,
  theme).
- **Reminders**: `AlarmManager`. Exact alarms (`setExactAndAllowWhileIdle`) whenever
  `canScheduleExactAlarms()`, otherwise `setAndAllowWhileIdle`. `USE_EXACT_ALARM` is granted at
  install on Android 13+, so a sideloaded build is always exact (Play would restrict this
  permission to alarm/calendar apps). Android 8–12 uses `SCHEDULE_EXACT_ALARM`. A `BroadcastReceiver` posts the
  notification and schedules the next occurrence. A boot/time-change/app-update receiver
  reschedules everything. `POST_NOTIFICATIONS` is requested in context, the first time a
  reminder is turned on.
- **Notifications** deep-link into the create flow or a specific check-in via intent
  extras on `MainActivity`. A goal or check-in opened this way sits directly on Today, so
  Back always lands there.
- **Home-screen widget** (Jetpack Glance), "Focus plan": the focus goal's if-then as one
  sentence, plus a "Check in" pill when one is due. Tapping it opens the goal, or the create
  flow when there is no WOOP yet. It redraws whenever goals change, and when a check-in alarm
  fires; there are no periodic updates. It sizes its text to the space, so the "then" part is
  never the bit cut off, and follows the system font size up to 1.5×.
- **Speech input** via `RecognizerIntent` (no microphone permission needed).
- **Privacy**: no `INTERNET` permission, no analytics, no account. Export = share JSON via
  the Android share sheet. `allowBackup=false`.
- **SDK**: minSdk 26 (Android 8), target/compile 36. Debug-signed APK, so
  `adb install` works with no keystore setup.

```
app/src/main/java/com/likwiid/woop/
 ├─ WoopApp.kt, MainActivity.kt, AppContainer.kt
 ├─ data/        Entities, Daos, WoopDatabase, WoopRepository, Prefs
 ├─ widget/      FocusWidget (Glance) + receiver
 ├─ reminders/   Schedule (pure time maths), ReminderScheduler, ReminderReceiver,
 │               RescheduleReceiver, Notifications
 └─ ui/
     ├─ AppViewModel.kt  (appViewModel {} factory over AppContainer)
     ├─ theme/       Color, Type (Atkinson Hyperlegible), Theme
     ├─ components/  Basics (ChoiceCard, LetterCard, buttons, progress), Flow (FlowScaffold,
     │               imagine visual), Inputs (WoopTextField with mic, TimeButton, DayPicker),
     │               PlanEditor
     ├─ today/  create/  detail/  review/  reminders/  learn/ (Science tab)  onboarding/  settings/
     └─ nav/         WoopNavHost
```

---

## 6. Build & install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or `./install.sh`, which builds and installs on the connected device.

---

## 7. Roadmap

| Phase | Scope |
|-------|-------|
| **1 (this build)** | Plan, full UI, local storage, create / check-in / remind flows, Science tab. Home-screen widget with the focus if-then (pulled forward from phase 2). |
| 2 | Import from JSON. |
| 3 | Optional voice-memo answers (Huberman: recording works too). Gentle insights ("your plans fire most on weekdays"). |
| 4 | Wear OS tile for the if-then. Encrypted backup to a user-chosen file. |

## 8. Testing

- Unit (`ScheduleTest`, 15 tests): next-trigger maths for prompts and check-ins (days mask,
  DST, missed occurrences, off), "due" logic, day labels, and the if-cue pre-fill.
- Manual, done on an API 36 emulator: onboarding, the full create flow, a full check-in
  (plan missed → stuck → tune → keep going → note), detail edits and menu, reminders
  (alarms confirmed exact in `dumpsys alarm`), Science cards, Settings, dark mode.
- Instrumented (`WoopDatabaseTest`, 11 tests, `./gradlew connectedDebugAndroidTest`, run
  on the emulator): drafts kept out of the list, pinned-first ordering, obstacle and check-in
  ordering, cascade deletes, prompt upsert, repository save / check-in / pin / export / delete
  everything, and the entities against the exported schema. Test goal ids start at 900000, so
  they never touch a real reminder alarm. Gradle uninstalls the app afterwards, so don't run
  them on a phone with real data.
- Accessibility, on the emulator:
  - 200 % font scale on every screen and the widget.
  - Every control has a TalkBack label (uiautomator audit). Spoken output was not listened to.
  - "Remove animations": pager, timer, check-in auto-advance and Science cards all work.
- Widget, on the emulator: 4×2, 4×1, dark mode, empty state, check-in pill, opening the goal,
  and the minified release build.
- Still to do: a full pass on a physical phone.
