# Dialogue Mode for Android TV

A system-wide "night mode" for Android TV / Google TV boxes. It evens out movie audio so
dialogue is no longer too quiet while music and explosions are too loud. One volume setting
then works for both, which helps in apartments or late at night.

It processes **every app's audio** (Netflix, Stremio, Plex, local files and so on) by attaching
Android's built-in `DynamicsProcessing` effect to the global output mix (audio session 0).
It adds about 10 ms of latency, so lip sync isn't affected.

> **AI disclaimer:** This was vibe coded: I'm not a software developer, and I built it with
> Claude Code. If you're against AI-made software, no hard feelings; feel free to skip it.
> If not, I hope it helps.

## Install on your TV (no computer needed)

Takes about 10 minutes, using the TV remote only.

1. **Install Downloader.** On the TV, open the Google Play Store, search for **Downloader**
   (by AFTVnews) and install it.
2. **Allow Downloader to install apps.** Go to **Settings → Apps → Security & restrictions →
   Unknown sources** and turn on **Downloader**.
   - Can't see *Unknown sources*? On Google TV it's hidden until developer mode is on. Go to
     **Settings → System → About** and click **Android TV OS build** 7 times, then look again.
3. **Download the app.** Open Downloader, select the address box and type:
   ```
   github.com/colsonj/dialogue-mode/releases/latest/download/dialogue-mode.apk
   ```
   Then press **Go**. This link always gets the newest version.
4. **Install.** When asked, choose **Install**.
   - Google Play Protect may warn that the app is from an unknown developer, or block it. This
     happens to any app that isn't from the Play Store. Choose **More details → Install anyway**.
     The full source code is in this repository if you want to check what it does.
   - Downloader may offer to delete the downloaded file afterwards. That's fine; the app stays
     installed.
5. **Open Dialogue Mode** from your apps list once. It starts working straight away and starts
   itself again after a restart.
6. **Set up your box's audio settings** as described in
   [Google TV Streamer settings](#google-tv-streamer-settings-tested), then press **Check TV
   box settings** in the app to confirm.

Using a soundbar or surround system? Read
[Soundbars and surround systems](#soundbars-and-surround-systems) first.

To **update**, repeat step 3. Your settings are kept. To **remove** it, uninstall it like any
other app.

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
- Apps must hand the box **decoded PCM** audio. If an app is allowed to send compressed Dolby
  (AC-3 / E-AC-3), that audio skips the mixer the effect sits on, and Dialogue Mode does nothing.
- Turn **off** any built-in volume leveller or dialogue enhancer. Stacking them with this app
  made the sound thin and tinny.

### Google TV Streamer settings (tested)

**Settings → Display and sound**, under **SOUND**:

| Setting | Value | Why |
|---|---|---|
| System sounds | **Off** | The app's makeup gain (up to +16 dB) makes menu clicks loud |
| Audio options → Volume leveller | **Off** | Conflicts with the app's compressor |
| Audio options → Dialogue enhancer | **Off** | Stacks with the app's voice EQ and sounds tinny |
| Audio options → Output format | **PCM stereo** | The box sends decoded stereo to the TV |
| Surround sound settings | **None** ("Never use surround sound") | Makes every app decode to PCM so the effect always applies. *Automatic* lets apps such as Netflix send Dolby, which bypasses it |

Other Android TV boxes have similar settings under different names. The important ones are
surround sound / passthrough set to never or off, and PCM output.

Global (session 0) effects are deprecated in Android, but they still work on many devices.
Some firmware may ignore them, or route certain apps through a direct or tunnelled output that
skips the mixer. To check, see [Verifying](#verifying).

### Soundbars and surround systems

The app can only process audio the box decodes itself. That means it outputs **stereo only**.
Whether that's a good trade depends on your setup.

**TV speakers:** use the settings above. This is what the app was tuned for.

**Stereo soundbar, or a soundbar with a subwoofer / virtual surround:** recommended. Use the
same box settings (Surround sound **None**, Output format **PCM stereo**), then:

| Setting | Suggestion |
|---|---|
| Soundbar night mode / voice mode | **Off** to start, so they don't stack with the app. Re-try them once you've tuned it |
| Soundbar virtual surround / upmix | Fine to leave on; it works on the stereo signal it receives |
| Evening strength | **Strong** to start. Soundbars handle peaks better than TV speakers |
| Bass cut | **-3 dB or more** in an apartment (subwoofers carry through walls), otherwise **0** |
| Voice boost | **0 to +2 dB**. Most soundbars already have a forward midrange |

You give up Dolby decoding on the soundbar. On a basic bar that hardly matters. On a good
Atmos bar it does, so see below.

**AV receiver with real 5.1/7.1 speakers, or a high-end Atmos soundbar: not recommended.**
Surround sound **None** would reduce everything to stereo. Use the receiver's or soundbar's
own tools instead. They work on the original Dolby audio and lose nothing:

- **Night mode / DRC / dynamic range "Auto"** uses compression data the film's mixers built
  into Dolby audio.
- **Centre channel level +3 to +6 dB** lifts only the dialogue channel. This is a real dialogue
  boost, which a stereo app can't do.
- **Dynamic volume** features, such as Audyssey Dynamic Volume or Sonos Night Sound plus
  Speech Enhancement.

Multichannel support (processing 5.1 PCM, with a proper centre-channel boost) isn't implemented.
It would need eARC or a receiver that accepts multichannel PCM. It's also untested whether the
box routes multichannel audio through the effect at all.

## Install with adb (for developers)

```
adb pair <ip>:<pairing-port> <code>      # Developer options → Wireless debugging
adb connect <ip>:<port>
adb install -r dialogue-mode.apk
adb shell am start -n local.dialoguemode/.MainActivity
```

After it has been opened once, it runs as a foreground service and starts again after a reboot
or app update. Both were tested on the Google TV Streamer.

Wireless debugging switches itself off when the box restarts, and comes back on a new port when
you turn it on again. Pairing is remembered, so only `adb connect` with the new port is needed.

## Using it

The settings screen works with the remote. Press left/right on a row to change it;
it stops at either end of the range.

| Control | Options |
|---|---|
| Dialogue Mode | On / Off (for A/B comparison) |
| Evening strength | Light, Medium, Strong, **Max** (default) |
| Voice boost | 0 to +9 dB (default +2) |
| Bass cut | 0 to -12 dB (default -3) |

**Check TV box settings** reads the box's audio settings and marks each one ✓ or ⚠. It only
reads; it never changes anything. Android 12+ hides Google's Dolby settings (volume leveller,
dialogue enhancer, output format) from apps, so for those it shows where to check by hand.

The defaults were tuned by ear for TV speakers. Use Max for films and Strong or Medium for
TV shows that are already mixed loud.

## Troubleshooting

| Problem | Try |
|---|---|
| No difference with it on or off | Press **Check TV box settings** and fix anything marked ⚠. Surround sound must be **None**, otherwise Dolby audio bypasses the effect. Then press **Re-attach effect** |
| Works in some apps but not others | That app is probably sending Dolby or using a direct output. Make sure Surround sound is **None**, then restart the app |
| Thin or tinny sound | Turn off the box's own Volume leveller and Dialogue enhancer (and any soundbar voice mode). Lower Voice boost and Bass cut |
| Squashed, flat or "pumping" sound | Lower Evening strength (Strong or Medium) |
| Loud parts still too loud | Raise Evening strength to Max |
| Everything is louder when it's on | Expected: makeup gain lifts quiet parts. Turn the TV down a notch once |
| Stopped working after a restart or update | Open Dialogue Mode once; the status should say *Attached to global output* |

## Privacy

The app has **no internet permission**, so it can't send or receive anything. It stores only its
own settings on the device. Its only audio permission is `MODIFY_AUDIO_SETTINGS`, which it needs
to attach the effect. It has no microphone or recording permission and can't capture audio.

## Known limitations

- **Tested on one device** (Google TV Streamer, Android 14). Other boxes should work if they
  allow global audio effects, but that isn't guaranteed.
- It relies on attaching effects to the **global output mix**. Android has deprecated this, so a
  future system update could stop it working.
- **Stereo only.** See [Soundbars and surround systems](#soundbars-and-surround-systems).
- It can't separate dialogue from music once an app has downmixed to stereo. It evens out
  loudness and lifts the speech frequencies, which helps a lot, but it isn't a true centre
  channel boost.
- Changing **Evening strength** resets the compressor settings to that preset. Voice boost and
  Bass cut are kept.

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

When publishing a release, attach the APK twice: once as `dialogue-mode-<version>.apk` and once
as plain `dialogue-mode.apk`. The plain name is what the short "latest" install link points to.

On the first build, `build.sh` creates a local signing key, `keystore.jks`. It is git-ignored.
Keep it: an installed copy can only be updated by an APK signed with the same key.

## License

MIT. See [LICENSE](LICENSE).
