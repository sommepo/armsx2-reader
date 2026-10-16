# ARMSX2 Reader: the fork's guide

ARMSX2 Reader is a fork of ARMSX2, the PlayStation 2 emulator for Android (itself built on
PCSX2), that is to show a game's text as real text: tap a word in the game's own picture to
look it up, make Anki cards, keep a backlog and reading statistics. It is the Android half of
the PS2 reader; the desktop half is PCSX2 Reader (`../pcsx2-reader-fork`), with the same text
engine and the same look. Both are siblings of Eden Reader (Switch), Azahar Reader (3DS) and
PPSSPP Reader (PSP) and share their reading layer.

If a file `MAINTAINER.md` exists (the maintainer's own checkout), read it after this one.

## State (2026-10-14): the interface and eleven games' profiles; ペルソナ4 read on a device

**What is there**: the PS2-style menu in place of the app's library screen; the reading
layer's own screens (statistics, supported games, dictionaries, Anki setup, backlog,
settings) in the console's colours; the layer over the game's picture with the popup, the
session line and the cursor; and natively a text engine that loads a game's profile and runs
it once a frame, with its bridge to the app.

**Seventeen games have a profile**: ひぐらしのなく頃に祭 カケラ遊び (SLPM-66913), Remember11
(SLPM-65550), Ever17 Premium Edition (SLPM-65421), Never7 (SLPS-25256), ペルソナ4
(SLPM-66978), and since 2026-10-14 ペルソナ3 (SLPM-66445), ドラゴンクエストV (SLPM-65555),
サイレントヒル2 (SLPM-65051), 真・女神転生III マニアクス (SLPM-65462), DIGITAL DEVIL
SAGA (SLPM-65597), アマガミ (SLPS-25918), EVE burst error PLUS (SLPM-65320), うたわれるもの
散りゆく者への子守唄 (SLPS-25679), シャドウハーツ (SLPS-25041) and since 2026-10-15 METAL GEAR
SOLID 2 SUBSTANCE (SLPM-67002), METAL GEAR SOLID 3 SNAKE EATER (SLPM-65790) and CLANNAD
(SLPM-66302; its routes finished elsewhere can be marked, below). The two ペルソナ,
真・女神転生III, DIGITAL DEVIL SAGA and the two METAL GEAR SOLIDs need a character table that is not in Git (see "Ground rules"). The profiles are the desktop sibling's,
where they were written and seen to read the games (its guide's "Games" has what each
covers; copy a change from there). **In this app ペルソナ4 and Never7 were seen to read** (an
AYN Thor, 2026-10-13 and 2026-10-14: see "How it was checked"); the others have read nothing
here yet.

**What is not there yet**:

- **Hooks in a game's code** are not carried out by the recompilers; a profile reads memory
  once a frame.
- **A card's audio**: the mixer's output is handed to the engine's audio history (the same
  lines as in the desktop sibling, where it was seen to hold the game's sound); not run here.
- **The reader's buttons reach the game too**: its cursor button and statistics button are
  taken from the keys the activity gets. Pressing a button of the console for the reader
  (`pressButton`: *Next line*, touch play) reaches the first pad since 2026-10-16 (below);
  holding the game's sticks at rest (`holdSticks`) is still not connected.
- **Where the game's picture is** on the surface is worked out from the aspect ratio alone.
- **The Browser's "Options"** (△, or Y on a controller) is shown and opens nothing yet.
- **Save sync** (2026-10-16, below) has not been run on a device.

## Ground rules

- **Nothing goes to upstream ARMSX2 or PCSX2 from here.** The reading layer is AI-written;
  upstream asks about that in every pull request and PCSX2 rules agents out. The remote
  `upstream` cannot be pushed to. Do not present this app as ARMSX2.
- **No game data, no BIOS, in Git, ever.** Discs, the console's BIOS, anything extracted or
  derived from them, savestates, screenshots: under `private/`, which Git ignores. A bundled
  profile holds addresses and decoding logic only. Tests use made-up text. The one exception,
  by the maintainer's word: the README's pictures and animations in `docs/reader/media`.
- **A table made from a game is game data too** (a font's character table, as the two
  ペルソナ, 真・女神転生III and DIGITAL DEVIL SAGA need): it is `private/<game>/<SERIAL>.charset`, the profile asks for it with
  `reader.data("charset")`, and `tools/reader/private-data.sh`, which `build-android.sh`
  calls, puts it beside the profile in the assets for a build of your own (Git ignores it
  there). The profile says clearly when it is missing. A table can also be put beside a
  profile of one's own in the app's folder (`reader/profiles/<SERIAL>.charset`). The
  desktop sibling's guide says what ペルソナ4's table is.
- **Upstream files are touched as little as possible**, each change with a comment that
  begins `Reader fork`.
- **Bound what you run.** `tools/reader/limit.sh` runs a command under a memory limit and
  the scripts in `tools/reader/` use it; compile jobs are `READER_JOBS` (8).
- **A device is its owner's.** Before installing over the app on a device, its process must
  not be running; check which game is in front before sending any input.
- **Say what was checked and how**: an off-device test, a real device are different levels.

## Layout

| Path (under `ARMSX2/app/`) | |
| --- | --- |
| `src/main/java/com/armsx2/reader/` | The Android reading layer (Kotlin), the PPSSPP Reader fork's. `ReaderGlue`, `ReaderInput` and `ReaderNative` tie it to this app. |
| `src/main/java/com/armsx2/reader/ui/Ps2Home.kt`, `Ps2Glue.kt` | The PS2-style menu (Compose), and what ties it to the app's library and settings. |
| `src/main/cpp/reader/` | Native: the text engine (`core/`, with `core/emulator.cpp` tying it to the emulator), Lua (`lua/`), and `reader_jni.cpp`, the bridge to `ReaderNative`. The same engine as PCSX2 Reader's. |
| `src/main/assets/reader/profiles/` | The game scripts (profiles), one `<SERIAL>.lua` per game (`SLPM-66913.lua`), PCSX2 Reader's copied here: built into the APK unless `-PreaderBundleScripts=false`. |
| `src/main/java/com/armsx2/reader/ScriptStore.kt`, `ui/ReaderGamesActivity.kt` | The published list of game scripts read, a script checked and installed or removed, and the screen for it ("Game scripts"). |
| `src/main/res/**/…reader…` | The reading layer's strings, icons, preference pages and theme (`reader_themes.xml`: the PS2's colours). |
| `src/test/java/com/armsx2/reader/` | Tests of the reading layer, off the device. |
| `tools/reader/` (repository root) | Build and test scripts. `env.sh` has every path. |
| `private/` (repository root) | Ignored by Git. |

Upstream files touched: `app/build.gradle.kts` (the app's own ID
`io.github.sommepo.armsx2reader`, the reading layer's libraries, the tests),
`AndroidManifest.xml` (the reading layer's screens, AnkiDroid), `Main.kt` (the layer
attached, its keys first, its life cycle), `ui/GamesList.kt` (the menu in the library's
place), `ui/InGameOverlay.kt` (the next choice in the in-game menu),
`res/values/strings.xml` (the app's name), `cpp/CMakeLists.txt` (the engine and the
bridge), `cpp/pcsx2/VMManager.cpp` (which program runs, each frame, shutdown, a loaded
savestate), `cpp/pcsx2/GS/Renderers` (the CRT picture), `cpp/pcsx2/ImGui/ImGuiOverlays.cpp`
(nothing drawn over the game), and mended faults of the emulator's own:
`cpp/pcsx2/arm64/mac/aR5900MMI.cpp`, `cpp/pcsx2/arm64/mac/aR5900.cpp` (the Skip MPEG fix) and
`cpp/pcsx2/SIO/Pad/Pad.cpp` (see "How it was checked"; Pad.cpp also tells the reader of the
player's presses).

Branch `reader`, based on upstream tag `v6` (a shallow clone).

## The PS2's look

On by default. `Ps2Home.kt` draws the same three screens as the desktop app
(`reader/qt/ps2_home.cpp` in PCSX2 Reader): the **main menu** (Browser, System
Configuration, the seven lights), the **Browser** (the games as pictures afloat in the haze,
the chosen one's name and serial in the top right corner, a boxed book on a readable game)
and **System Configuration** (Reading Statistics, Game Scripts, Dictionaries, Anki,
Reader Settings, Emulator Settings, Memory Cards, On-screen Controller, Console Menu, Games
Shown, Look for Games Again, Folders and Setup, Game List). **The Browser holds only the games
whose text can be read** unless *Games Shown* is switched to *All* (pref `reader_all_games` in
`reader`, off at first; 2026-10-16). The games folder is looked through again at every start,
behind the list kept from the last time (before, a disc copied in later showed only after *Look
for Games Again*). A controller's d-pad, A (enter), B (back) and Y (options), or
touch: a touch chooses a game and a second one starts it. *Game List* under System
Configuration switches the menu off and the app's own library is back.

**The buttons drawn over a game for playing by touch** (the app's on-screen pad) are on at
first only on a device without game controls of its own (a phone; the same test as Play by
touch's, kept from the first start), off on the Thor; *On-screen Controller* under System
Configuration switches them (`ui/touch/TouchControls.kt`, an upstream file: upstream shows them
from the first start and again at every touch of the screen, which here is a word looked up).
**While a game is played by touch the pad is not drawn** (`TouchControls.readerTouchPlay`, set
through `ReaderOverlay.onTouchPlaying`): that game's few buttons are, below. **The app's cog
over the game** (the in-game settings, top right) is in the PS2 menu's look, faint until touched
(2026-10-16; it was a grey disc), and its place is passed to the reader's layer
(`TouchControls.settingsButtonArea`, `ReaderOverlay.appControlsAt`), which lets touches there
through: with Play by touch on, that layer took the cog's touch as the game's ○ (on the Fold the
cog could not be opened).

The reading layer's own screens have the menu's colours and are laid out as its pages: the
page's name in light blue over a line, the page, and "○ Back" along the bottom.

## Build and test

The Android SDK and the JDK are the ones the sibling forks use (`tools/reader/env.sh`).

```sh
tools/reader/build-android.sh        # ARMSX2/app/build/outputs/apk/debug/app-debug.apk
tools/reader/test-android.sh         # the reading layer's tests, off the device (219)
READER_PREVIEW_DIR=$PWD/private/previews tools/reader/test-android.sh   # … and pictures of its screens
```

`tools/reader/build-android.sh -PreaderBundleScripts=false :app:assembleDebug` makes the
published build, which comes with no game scripts (they are installed from the list, below).

`build-android.sh` first fetches what the emulator's shader compiler needs and the
repository does not hold (SPIRV-Tools, glslang and the rest, at the commits its `DEPS` file
names), and links two of them under the names the emulator's build looks for.

## How it was checked

- **Off the device** (2026-10-09): 197 tests of the reading layer (the PPSSPP Reader fork's,
  with PS2 serials and the bundled games), among them `Ps2HomeTest`: the menu's three screens
  drawn with made-up games, a controller's keys through them, a game started by key and by
  two touches. Pictures of the menu and of the reading layer's screens were looked at
  (`private/previews`).
- **The APK builds** with the engine, the bridge and the reading layer in it.
- **On a device** (AYN Thor, 2026-10-13, driven over adb): the menu, *Look for Games Again*,
  the Browser marking ペルソナ4 as readable; the game started from it at full speed, its
  profile and character table loaded by themselves; the first message counted in the
  session line (24 characters); a tap on 選択 in the game's picture marked those two
  characters and opened their entry. Two crashes of the emulator were met on the way and
  mended in upstream files: the recompiler stopped at the game's title screen (lane
  permutes written with a move the assembler has for scalars only, which a debug build's
  assertions refuse: `pcsx2/arm64/mac/aR5900MMI.cpp`), and a button that came while a game
  was starting found no pad (`pcsx2/SIO/Pad/Pad.cpp`). Keys sent with `input keyevent` move
  the menu but do not reach a game: its on-screen buttons do (hold 200 ms).
- **On a device** (AYN Thor, 2026-10-14): Never7 from its first start (the logo hang
  mended, above) into the story: messages counted, the second display's trackpad moving the
  cursor onto ニッコリ in a choice and its entry opening, the next choice (above). Buttons were
  pressed by writing to the controller's input device (`sendevent /dev/input/event9`, the
  "Odin Controller": 304 south, 305 east, 308 west, 315 start; the d-pad is ABS_HAT0X/Y), which
  reaches the game where `input keyevent` does not. A tap on a word in the picture opened
  nothing there; the trackpad and the cursor are the way in, and tapping is not looked into
  further (`MAINTAINER.md` has why).
- **On a device** (AYN Thor, 2026-10-14): ドラゴンクエストV from a new adventure log to its
  first messages, read with speakers; words looked up with the second display's trackpad and
  by touching them on the game (where Never7 opened nothing); the CRT picture over it; the Game
  scripts screen fetching the published list and updating a script from it. **PS2 Menu** in the
  upstream library (`ui/GamesList.kt`) switches the PS2 menu back on after *Game List* switched
  it off (before, nothing did).
- **Not seen on a device**: the other games reading, anything of ペルソナ4 past its first
  message, a card, the reader's screens opened from System Configuration.

## Playing by touch (2026-10-16)

The maintainer's wish (MAINTAINER.md): a visual novel played by touch alone, without the
on-screen controller. *Reader settings, Looking words up, Play by touch* (on from the first
start, `reader_touch_play`), for the games whose script says `-- reader: touch press` (PCSX2
Reader's guide, "Playing by touch", has the rule and the scripts' side, `reader.cursor`): a tap
on the game's picture beside any text, or in a message's box beside its words, presses the
advance button (*Next line*'s, ○ unless changed); a tap on an answer of a choice moves the
game's cursor there with the d-pad, one press every 250 ms while the script reports where the
cursor is, then presses ○ (always ○, not the advance button: that may be R1, which skips in
these games); a finger held on an answer looks its word up; with a popup open a tap only closes
it; while a choice is up a tap beside it does nothing; two presses that leave the cursor where it
was get one ○ (a message over the choice), 48 presses at most. **The second display (the
Thor's trackpad) is never played on**, and the trackpad and the cursor are as they were: touch
play is only taps on the game's own display. A game whose script says nothing (the role-playing
and action games) keeps its taps as before. The scripts that say it: Never7, Ever17, Remember11,
CLANNAD and アマガミ, which also report the cursor; ひぐらしのなく頃に祭 and EVE burst error PLUS,
where a tap goes on but their choices are not reported, so they also name the few buttons still
needed (`-- reader: buttons up down circle cross triangle`): `ui/TouchButtons.kt` shows them as a
column of round buttons at the right edge while Play by touch is on and such a game runs; a
finger held on one holds the button. **The five that report their cursor name buttons too** (2026-10-16, after the Fold): each title wants START, which a tap's ○ is not, and each has a save and load menu on a button of its own: Never7 and Ever17 `start select up down circle cross`, Remember11 and CLANNAD `start square up down circle cross`, アマガミ `start triangle up down circle cross`; ○ is in every column (and EVE's is `circle cross triangle`) since the maintainer, on the Fold at Never7's LOAD MENU, found × only backs out and no button chooses (a tap on the picture is ○, but did not choose there; not understood yet) (found in PCSX2 Reader's harness; each script's notes say what the others do). **On at first only on a device without game controls of its
own** (`ReaderSettings.touchPlayAtFirst`: no built-in gamepad, and not an AYN device), so a phone
(the Fold) has it and the Thor does not (2026-10-16). うたわれるもの was taken out (long battles).
The Eden Reader fork's touch play (`ui/ScreenTextLayer.kt`, `ui/ReaderOverlay.kt`,
`ReaderSession.pickAnswer`, `ReaderCatalog.touch`, `ReaderNative.answerCursor` in
`reader_jni.cpp`, the engine's `reader.cursor`), with the PS2's numbers:
**`ReaderNative.pressButton` now takes the emulator's own number of a pad input**
(`PadDualshock2::Inputs`: up 0, right 1, down 2, left 3, ○ 5, ✕ 6, R1 12; `ReaderInput.PAD_*`)
and `core/emulator.cpp`'s `PressButton` sets the first pad (it was an empty stub, so *Next line*
pressed nothing). The advance button was kept by the PSP's bit numbers copied from PPSSPP Reader
(13 ○, 14 ✕, 9 R); such a value is turned into the new one when the settings are first read
(`ReaderSettings.advanceButton`, `ReaderInput.advanceButtonFromPsp`).
**Checked**: off the device, `TouchPlayTest` (an answer picked against a made-up game whose
cursor is on none until the d-pad is pressed, back and sideways, a lesson over the choice, a
game that takes nothing given up on, the advance button's old numbers, which scripts say touch
press) and `ScreenTextOverlayTest`'s touch play cases (a tap on the picture and beside the text
goes on, a word is still looked up, an answer picked and a held one looked up, a tap beside a
choice or in its box does nothing, a game whose script says nothing keeps its taps); the APK
builds with it. In PCSX2 Reader on Linux with the same scripts and the same engine: Never7,
Ever17, Remember11 and CLANNAD picked by this press sequence, and Never7, CLANNAD and アマガミ
played by clicks (its guide says what was seen). **Not run on a device.**

**Every button, and a tab to tuck them away** (2026-10-16, the maintainer on the Fold: "just add every button like for ppsspp. And add a button on both readers to hide the controller buttons"): while a game is played by touch the column shows **every button of the controller** (`TouchButtons.ALL`: ▲ ▼ ◀ ▶ SELECT L1 L2, △ ○ × □ START R1 R2), in two short columns at the right edge, whatever the script's `-- reader: buttons` line says (the line stays, as a note of what that game needs: a subset chosen from it had left Never7's LOAD MENU with no button that chose). A tab above them (› / ‹) tucks them away and brings them back; the choice is kept (`reader_touch_buttons_tucked`). The same file in the PPSSPP, ARMSX2 and Flycast Reader forks, each with its console's buttons. Checked: JVM tests (`TouchButtonsTest`: every button shown, the tab, kept for the next game) and pictures of the column (`READER_PREVIEW_DIR`, `touch-buttons-all.png`, `touch-buttons-tucked.png`).

## Going on to the next choice (2026-10-14)

The desktop sibling's (its guide, "Going on to the next choice", has how it works): the game
runs as fast as it goes with ○ pressed every eighth frame until its profile reports a choice,
the text on the way not read, a press of any of the game's buttons stopping it. Here it is
started from the in-game menu (the cog, *Next Choice* in the Play tab's last row, shown while
a game with a text profile runs) or with a controller button chosen under *Reader settings,
Cursor* ("Button that goes on to the next choice", none at first); the emulator's message on
the picture says why it stopped. `reader/core/next_choice.*` and the engine are the
sibling's; `reader_jni.cpp` (`toggleNextChoice`, `onNextChoice`), `NextChoice.kt`,
`ReaderInput.kt` and the upstream `ui/InGameOverlay.kt` tie it to this app, and
`cpp/pcsx2/SIO/Pad/Pad.cpp` passes on the player's presses.
**On a device** (AYN Thor, Never7, 2026-10-14): from the opening to the first choice in 10
to 15 seconds (80 texts passed), the next ones in 7 to 9; the choice up and none picked, the
message under it counted; a press of ✕ during a run stopped it. The controller button was
not tried.

**Never7's films**: the game's database names the Skip MPEG fix for it ("prevents the game
from freezing"), but only the x86 recompiler carried that fix out; the ARM64 one this app
runs (`pcsx2/arm64/mac/aR5900.cpp`, the "mac" backend) now does it too, as there. Without it
the game hung for good on its first logo. Seen on the Thor: the logo passes ("sceMpegIsEnd
pattern found" in the log) and the title screen comes; the films are skipped, as on a
computer. Other games with the fix (Katamari and the like) were not tried.

**Nothing of the emulator's own is drawn over the game** (2026-10-14, the maintainer's
wish: only the game is to be seen): `pcsx2/ImGui/ImGuiOverlays.cpp` no longer draws the
statistics, the settings line, the controllers, the pause and speed marks or the shader
mark, whatever the settings say. Messages (such as the next choice's) and the save state
selector stay.

## Routes finished elsewhere (2026-10-15)

The desktop sibling's (its guide, "Routes finished elsewhere", has how it works, and "Games"
CLANNAD's routes): a game's script may offer the game's own progress to be set by hand
(`reader.progress`, scripts' API 2), and here the in-game menu (the cog) has **Routes** in a
row of its own below *Next Choice*, shown while such a game runs, with how many are marked.
It opens a list with a box for each (`reader/Routes.kt`; the group in front of each row, the
script's note under *How the game keeps them*); a box changed is written into the game's
memory by the script at once, and every box then shows the game's state. `reader_jni.cpp`
(`progress`, `markProgress`), `ReaderNative.kt` and the upstream `ui/InGameOverlay.kt` tie it
in. The engine's change is the sibling's (`Engine::Progress`, `MarkProgress`; a script writes
memory only while marking). **Not run on a device**: built and the off-device tests passed;
the desktop app did the same marking with CLANNAD (its guide says what was seen). This app
has no card of a game's own: CLANNAD saves on the card in the first slot, which it does not
format itself, so that card must already be formatted (by another game, or the console's
menu).

## Save sync (2026-10-16)

PCSX2 Reader's (its guide, "Save sync"), so the two keep one game's saves together: while a game
runs, the first memory card slot holds **a card of that game's own**,
`memcards/reader-saves/<serial>/card.ps2` (made formatted), whatever card the settings name;
its newest save is fetched from the service before the emulator opens the card and sent a
moment after the game writes it. Saves on the settings' own card (`Mcd001.ps2`) are not carried
over. `cpp/reader/save_sync/` and `cpp/reader/ext/` are PCSX2 Reader's files copied unchanged
(change them there and copy); the card's part is in `reader/core/emulator.cpp`
(`BeforeMemoryCards`, `MemoryCardOverride`, `OnMemoryCardWritten`, `ConfigureSaveSync`, the
last keeping the app's settings until the emulator has read its folders), tied in at the same
places as there (`pcsx2/VMManager.cpp`, `Pcsx2Config.cpp`, `SIO/Memcard/MemoryCardFile.cpp`,
`MemoryCardFolder.cpp`). The page is *Reader settings, Save sync* (`reader/SaveSync.kt`, the
page's code in `ReaderSettingsActivity`): *Sync game saves*, the service's address and code
and the device's name (the same settings the statistics used before, so they are sent there
too), *Sync now* with what the service last said, and a game whose save changed on two devices
with the choice of which to keep. A published build (`-PreaderSaveSync=false`, which passes
`READER_SAVE_SYNC=OFF` to CMake) has none of it.
**Checked**: the off-device tests (`SaveSyncTest`, the page's keys in `ReaderScreensTest`); the
APK carries the native side; the same engine, compiled on this machine with a small program,
fetched the newest Never7 card from a copy of the real service's data served on a loopback
port, byte for byte. **Not run on a device.**

## Game scripts (2026-10-14)

PCSX2 Reader's guide ("Game scripts") has it all: the scripts are its, and so is the list the
apps install them from (`reader/profiles/index.json` in its public repository, made by its
`tools/reader/scripts-index.py`). Here *Game scripts* (System Configuration, *Game Scripts*;
also under *Reader settings*) fetches that list on a thread of its own, shows each game of
the list or of the app with its state (*Not installed*, *Installed*, *Update available*, *Your
own script*, *Needs a newer app*) and an *Install* / *Update* / *Replace with the list's* and
a *Remove* button, and *Install from a file* takes `<SERIAL>.lua` and `<SERIAL>.<name>` files
chosen with Android's file picker. Everything goes into the app's `reader/profiles` folder,
which wins over the scripts in the APK; `.installed.json` there says what came from the list.
A script installed or removed while its game runs is reloaded at once. `ScriptStore.kt` is
PCSX2 Reader's `reader/desktop/script_index.*` in Kotlin, with the same rules and the same
record: change both together.
**Checked**: `ScriptStoreTest` (the desktop sibling's cases) and `ReaderScreensTest` (the
screen with a stand-in list: the states, *Install* fetching and writing the script, the badge's
catalogue seeing it, *Remove* with its question), off the device, with a picture of the
screen looked at. **Not tried**: on a device, the file picker, the published list on GitHub.

## The CRT picture

*Reader settings, CRT picture* shows the game as a cathode-ray tube would: scan lines, a
phosphor pattern, bulging glass, glow, a warm white, each with a slider, and six ready-made
looks. **On from the first start here** (a strong picture; the switch turns it off). It is the sibling forks' (one shader and one table of sliders:
`ARMSX2/app/src/main/cpp/reader/core/crt.*`, `crt_shader.glsl`; the settings page,
`reader/ui/CrtSettings.kt`, is built from the table), tied to this emulator as in PCSX2
Reader: one more present shader, `PresentShader::READER_CRT`
(`reader/render/crt_present.*`, `pcsx2/GS/Renderers`). **The PCSX2 Reader fork's
`docs/reader/crt.md` has all of it**; the shared files are changed in the Flycast Reader fork
and copied here by its `tools/reader/crt.py sync`. This app builds the shader's lighter make. **The shader files the app ships are its own copies** (`ARMSX2/app/src/main/assets/resources/shaders`): the constants' block in `vulkan/present.glsl` was lengthened there as well as in `bin/resources/shaders`.
**Until 2026-10-14 it was never drawn here**: the commit that brought it changed the
`pcsx2/GS` files at the repository's root, which the app does not build, instead of
`ARMSX2/app/src/main/cpp/pcsx2/GS`; the same changes are now in the latter. Adreno's GLSL ES
compiler refuses the shader's token pasting, so for OpenGL on Android `crt_present.cpp` hands
the vectors over through a function instead. **A handheld's first look** (`ReaderSession`,
`HANDHELD_CRT`): gentler than the shared table's first values, which were too strong on the
small screen and blew the bright parts out; kept as the user's own the first time, when nothing is kept yet.
**On a device** (AYN Thor, OpenGL, 2026-10-14): the curved, lined picture over Never7, the
reader's cursor landing on the characters through the bent glass and a word's entry opening.

## The second display's trackpad (2026-10-13)

On a device with two displays (an AYN Thor) the display the game is not on is the cursor's
trackpad **for as long as a game with a text profile runs, without a button**
(`HoverCursor.always`): drawn in the console's look (`ConsoleLook.ground` under a veil, the
console's pointer under the finger), the word *Trackpad* for a few seconds. The cursor is
moved by the finger alone there and shows on the game while it is used and four seconds
after. **Nothing of the controller is taken**: the cursor's button, the sticks and the d-pad
(which a controller reports beside the sticks, and which the reader swallowed with them
while the trackpad was held up) stay the game's. On one display everything is as it was:
the button holds the cursor, the sticks steer it. The same code in the PPSSPP, ARMSX2,
Flycast and Eden Reader forks; Azahar Reader is left out (the 3DS has a bottom screen of its
own there). Checked with `CursorModeTest` off the device, and a picture of the pad looked at
(`READER_PREVIEW_DIR`, `trackpad.png`). **On a device** (AYN Thor, 2026-10-13, ARMSX2 Reader
with ペルソナ4, touches sent over adb to the lower display): the pad came up by itself with
the first text, a stroke on it moved the cursor on the game, the cursor on 選択 opened its
entry, the console's pointer showed under the finger. **Not tried on a device**: a real
controller's d-pad and sticks while the pad is up (the fault this mends, covered by the test
only), and the other three apps.

## A game's menu, picked by a tap (2026-10-16)

A script may report a menu on the channel `menu` (`TextLine.CHANNEL_MENU`): the screen layer
treats it as a choice (a tap on an entry picks it, by the cursor the script reports), but
`ReaderSession` only places it on the screen: no statistics, no backlog, not in the panel, not
on the clipboard. EVE burst error PLUS reports its command menus so (PCSX2 Reader's guide has
where they are). Its buttons are now × and △. Tried in PCSX2 Reader on Linux; JVM test of the
line model (`TouchPlayTest`); not on a device. Looked for and not done (2026-10-16): ひぐらし's
story choices (none met in its opening); the other apps' games are in their own notes.
