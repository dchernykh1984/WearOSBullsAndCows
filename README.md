# WearOS Bulls and Cows

**Bulls & Cows** for **Wear OS** watches, in Kotlin and Jetpack Compose. The watch
picks a secret number; you guess it, and every guess comes back with two counts -
**bulls**, the digits that are right and in the right place, and **cows**, the
digits that are in the code but somewhere else. There is no limit on guesses: you
play until you crack it, and the number of guesses it took is your score.
Everything runs on the watch: no phone, no network, no account.

This is a port of
[AmazfitBullsAndCows](https://github.com/dchernykh1984/AmazfitBullsAndCows), the
same game as a Zepp OS mini app. The rules, the layout proportions and the eleven
translations are carried over unchanged; the implementation is new.

## Playing it

- **Keypad** - the ten digits sit in a ring just inside the bezel, where a round
  screen has room for them and a thumb can reach them all. The disc they enclose
  holds the guess counter, the guess history and the guess you are composing.
- **Controls** - tap a digit to add it, tap **OK** to play the guess, tap **Del**
  to take a digit back. A digit already in the guess goes dim and stops taking
  taps: every code is distinct digits, so a digit once placed is spent. **OK** only
  lights up once the guess is as long as the code.
- **The history** shows the last three guesses. Once there are more, the counter at
  the top turns into a pager - `4-6/12` - and tapping it walks a screenful at a
  time back through the older guesses, round to the newest again when it reaches
  the oldest. Swiping up and down moves one row at a time and stops at each end.
  The tap is the control that always works: vertical swipes are the system's on a
  watch and do not reliably reach an app.
- **The notation** - a history row reads `1234 0B 4C`: the guess, then its bulls
  and its cows. The legend under the start menu says the same thing, so the first
  game does not need this README.
- **Leaving a game** - **Back** goes to the menu. The game is kept, and a
  **Continue** button appears at the top of the menu to pick it up exactly where it
  was, half-typed guess and all. A game put aside is lost when the app closes, and
  starting a new one drops it - neither costs anything, since an abandoned game is
  never a loss and never touches a record.
- **Difficulty** - 3, 4 or 5 digits, picked on the start screen. Four is the
  classic game; three is the quick version and five the long one. The button says
  the digit count rather than a name, because a ladder that reads itself needs no
  legend.
- **Records** - one per difficulty, on their own screen. Fewer guesses is what
  makes a record; the clock only separates two wins that took the same number of
  guesses, which happens constantly, because the guess counts are small integers.
- **Languages** - English, Russian, German, French, Italian, Spanish, Portuguese,
  Dutch, Polish, Czech and Kazakh. The watch's own language is followed, and all
  eleven are offered individually in the system per-app language list - so Kazakh,
  which Zepp OS had no device-language code for and could never select, finally
  reaches the people it was translated for.

## Devices

Round watches, **Wear OS 3 (API 30) and newer**. Built and tested against a
**OnePlus Watch 2R** (466x466 round, Wear OS 5). The ring and the board inside it
are derived from the screen diameter at runtime rather than from a device list, so
any round watch gets the same game.

## Setup

```bash
git clone https://github.com/dchernykh1984/WearOSBullsAndCows.git
cd WearOSBullsAndCows
```

A JDK 17 and the Android SDK (compileSdk 36) are all that is needed; Gradle comes
with the repository through the wrapper. Point the build at your SDK with a
`local.properties` holding `sdk.dir=/path/to/Android/sdk`, or export `ANDROID_HOME`.

## Develop

```bash
./gradlew testDebugUnitTest   # the JVM unit tests
./gradlew koverVerify         # unit tests + the coverage floor
./gradlew ktlintCheck         # formatting
./gradlew detekt              # static analysis
./gradlew lintDebug           # Android Lint, including the Wear OS checks
./gradlew assembleDebug       # build the APK
./gradlew connectedDebugAndroidTest   # instrumented tests (needs a watch or emulator)
./gradlew installDebug        # install on a watch over ADB
```

The whole pull-request gate in one line, which is exactly what CI runs:

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest koverVerify assembleDebug assembleRelease
```

### Layout of the code

```
wear/
  src/main/AndroidManifest.xml         watch-only, standalone, no permissions
  src/main/java/com/dchernykh/bullsandcows/
    MainActivity.kt                    the single activity
    BullsAndCowsViewModel.kt           the state the screen draws
    game/Code.kt                       what a code is, and what a guess earns
    game/BullsAndCows.kt               the game played with them
    game/Level.kt                      the three difficulties
    game/History.kt                    the window onto the guesses played
    game/Scores.kt                     what counts as a record
    game/Timing.kt                     how long a game took, and how to write it
    layout/RoundGeometry.kt            chord maths that keeps content off the bezel
    layout/Keypad.kt                   the ring of ten keys
    layout/BoardStack.kt               everything the ring encloses
    store/RecordStore.kt               the records, on Preferences DataStore
    ui/                                the Compose screens
  src/main/res/values*/strings.xml     the screen strings, a table per language
  src/main/res/mipmap-*/               the adaptive launcher icon
  src/test/                            JVM unit tests
  src/androidTest/                     instrumented tests - what needs a device
tools/make-launcher-icons.sh           regenerates that icon from the Zepp OS one
config/detekt/detekt.yml               static-analysis overrides
gradle/libs.versions.toml              every dependency and plugin version
```

The rule that shapes it: anything a test can reach without a device - the rules,
the scoring, the history window, the record decision, the round-screen layout - is
a plain Kotlin class outside the Compose layer, and `koverVerify` holds it to a
floor of 80. Only what genuinely needs a device is exempt, and each exemption is
written down where it is made, with the instrumented test that covers it instead.

## Pre-commit hooks (contributors)

```bash
uv tool install pre-commit   # or: pipx install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg --hook-type pre-push
```

On commit: whitespace and line endings, YAML/TOML/XML well-formedness, a non-ASCII
guard on source and config (translations in `res/values-*/` are exempt - that is
what they are for), and a check that apostrophes in string resources are escaped,
which is an aapt2 error rather than a warning. On the commit message: Conventional
Commits. On push: ktlint, detekt and the unit tests.

## Continuous integration and releases

Every pull request must pass: pre-commit, `actionlint`, commitizen, the Gradle gate
above, a CodeQL analysis, an OSV dependency scan and the instrumented tests on two
Wear OS emulators.

Releases are automated with `release-please`: it maintains a version-bump PR from
the Conventional Commits and, when merged, tags a GitHub Release. The release build
then produces a **signed APK**, verifies its signature, records a build-provenance
attestation and attaches the APK and its R8 mapping file to the release.

Verify a published APK came from this repository:

```bash
gh attestation verify wearos-bullsandcows-<version>.apk --repo dchernykh1984/WearOSBullsAndCows
```

### Dependency locking

`wear/gradle.lockfile` pins every transitive version. After changing a dependency,
regenerate it with the **Update lockfiles** workflow (or
`./gradlew :wear:dependencies --write-locks`) and commit the result.

## License

Released under the [MIT License](LICENSE).
