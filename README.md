# Dialogue Mode for Android TV

A system-wide "night mode" for Android TV / Google TV boxes. It evens out movie audio so
dialogue is no longer too quiet while music and explosions are too loud. One volume setting
then works for both, which helps in apartments or late at night.

It processes **every app's audio** (Netflix, Stremio, Plex, local files and so on) by attaching
Android's built-in `DynamicsProcessing` effect to the global output mix (audio session 0).
It adds about 10 ms of latency, so lip sync isn't affected.

## How it works

```
input gain → pre-EQ → 3-band compressor → peak limiter
```

| Stage | What it does |
|---|---|
| Pre-EQ | Optional bass cut below 250 Hz (low end travels through walls) and a voice lift at 1–4 kHz |
| Compressor | 3 bands (<250 Hz, 250 Hz–4 kHz, >4 kHz). Pulls loud passages down, then makeup gain raises everything, so quiet dialogue comes up |
| Limiter | Linked stereo, catches any peaks that are left |

Movie soundtracks are usually 5.1 mixes, and most apps downmix them to stereo. That makes
dialogue quieter than everything else. This app can't separate the dialogue back out after
that downmix, so it relies on compression plus EQ instead.

## Requirements and setup

- Android 9+ (API 28). Tested on a **Google TV Streamer (Android 14)** driving TV speakers.
- The box must output **PCM**, not Dolby/DTS bitstream. If compressed audio is passed straight
  through to the TV or soundbar, Android never decodes it and can't process it. On Google TV:
  **Settings → Display & Sound → Audio**
  - Surround sound: **Never**
  - Output format: **PCM stereo** (if offered)
- Turn **off** any built-in "volume leveller" or "dialogue enhancer" on the box. Stacking them
  with this app made the sound thin and tinny.

Global (session 0) effects are deprecated in Android, but they still work on many devices.
Some firmware may ignore them, or route certain apps through a direct or tunnelled output that
skips the mixer. To check, see [Verifying](#verifying).

## Install

Download or build `dialogue-mode.apk`, then either:

- **adb** (Developer options → Wireless debugging):
  ```
  adb pair <ip>:<pairing-port> <code>
  adb connect <ip>:<port>
  adb install -r dialogue-mode.apk
  ```
- or sideload it with an app such as *Downloader*.

Open **Dialogue Mode** from the apps row once. After that it runs as a foreground service and
starts again after a reboot or app update.

## Using it

The settings screen works with the remote. Press left/right on a row to change it.

| Control | Options |
|---|---|
| Dialogue Mode | On / Off (for A/B comparison) |
| Evening strength | Light, Medium, Strong, **Max** (default) |
| Voice boost | 0 to +9 dB (default +2) |
| Bass cut | 0 to -12 dB (default -3) |

The defaults were tuned by ear for TV speakers. Use Max for films and Strong or Medium for
TV shows that are already mixed loud.

## Remote tuning over adb

The service accepts parameters from `adb shell`. It is exported behind the `DUMP` permission,
so other apps can't call it.

```
adb shell am start-foreground-service -n local.dialoguemode/.DialogueService \
    --ef comp_thr -38 --ef comp_ratio 6 --ef comp_makeup 16 --ef lift 2
```

| Key | Meaning | Default |
|---|---|---|
| `comp_thr` | compressor threshold, dBFS | -38 |
| `comp_ratio` | compression ratio N:1 | 6 |
| `comp_makeup` | makeup gain, dB | 16 |
| `comp_attack` / `comp_release` | ms | 20 / 250 |
| `bass_thr_offset` | bass band threshold relative to `comp_thr`, dB | -3 |
| `bass_ratio_mult` | bass band ratio multiplier | 1.0 |
| `lift` | voice EQ boost, dB | 2 |
| `bass_cut` | gain below 100 Hz, dB | -3 |
| `lim_thr` | limiter threshold, dBFS | -2 |
| `input_gain` | dB | 0 |

Also: `--ei preset 0..3`, `--ez enabled true|false`, `--ez restart true` (re-attach).
Changes are saved on the device.

## Verifying

```
adb logcat -s DialogueMode
adb shell dumpsys media.audio_flinger
```

While something is playing, the output thread that holds the active track should list
`1 effects for session 0` with `Dynamics Processing`.

## Building

You don't need Gradle. The build uses the raw SDK tools:

- JDK 11+ (`javac`, `keytool`), `python3`
- Android SDK with `platforms;android-34` and a recent `build-tools` (37.0.0 tested; the d8 in
  build-tools 34 crashes on very new JDKs)

```
export ANDROID_HOME=~/android-sdk     # default if unset
./build.sh                            # → build/dialogue-mode.apk
```

On the first build, `build.sh` creates a local signing key, `keystore.jks`. It is git-ignored.
Keep it: an installed copy can only be updated by an APK signed with the same key.

## License

MIT. See [LICENSE](LICENSE).
