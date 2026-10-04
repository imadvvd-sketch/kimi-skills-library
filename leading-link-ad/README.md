# The Leading Link: 60-second motion graphics ad

A Remotion (React + TypeScript) project that renders a 60-second ad for
**The Leading Link** (theleadinglink.ae, Dubai):

| Composition | Size | Output |
|---|---|---|
| `LeadingLinkAd` | 1920x1080, 30 fps, 1800 frames | `out/leading-link-ad.mp4` |
| `LeadingLinkAdVertical` | 1080x1920, 30 fps, 1800 frames | `out/leading-link-ad-vertical.mp4` |

Both compositions use the same scenes. The layout adapts to the orientation
through `src/layout.ts`.

## Quick start

```bash
npm install
npm run dev              # Remotion Studio (live preview, prop editor)
npm run render           # -> out/leading-link-ad.mp4
npm run render:vertical  # -> out/leading-link-ad-vertical.mp4
npm run stills           # mid-scene stills of both versions -> stills/
npm run typecheck
```

Captions are off by default. To burn them in, set the `showCaptions` prop in
the Studio, or render with `--props='{"showCaptions":true}'`.

## Project layout

```
src/
  timing.ts              all scene timings, transitions, caption timing, impacts
  theme.ts               palette, fonts, brand strings, audio file and volume constants
  layout.ts              16:9 / 9:16 responsive helpers
  LeadingLinkAd.tsx      the composition: TransitionSeries + audio + captions
  content/voiceover.json script lines and phrases (one source of truth)
  content/vo-timeline.json   where each phrase starts in the take (generated)
  brand/logoPaths.ts     traced logo paths (generated)
  components/            AnimatedText, GlowCard, IconBadge, Counter, CaptionBar,
                         Chip, BrandLogo, Background, SceneFrame, Fx, zoomThrough
  scenes/                Scene01Hook ... Scene10Cta
scripts/
  generate_voiceover.py  generates a take with ElevenLabs or Kokoro (optional)
  align_voiceover.py     finds every phrase in the take -> vo-timeline.json
  generate_music.py      synthesizes the music bed and SFX (license-free)
  trace_logo.py          vectorizes the supplied logo
  render-stills.mjs      renders verification stills
```

## Changing texts

* **Voiceover script:** edit `src/content/voiceover.json`. `tts` is the text a
  voice engine reads. `captions` is the list of phrases that
  `npm run align` looks for in the take. After changing the script, record or
  generate a new take, then run `npm run align`.
* **On-screen titles and labels:** these are in each scene file under
  `src/scenes/`. Brand name, tagline and URL are in `brand` in `src/theme.ts`.

## Timing

The voiceover drives the timing. The video plays one continuous take,
`public/audio/vo/voiceover.mp3`, from frame 0.

* **Scene starts:** each scene cuts in `SCENE_LEAD` frames before its line
  starts in the take.
* **Animations:** these sync to individual phrases with
  `cue(sceneId, phraseIndex)`.
* **Total length:** stays at exactly `TOTAL_FRAMES` (1800). The last scene
  holds the end card until the end.

Phrase positions come from `src/content/vo-timeline.json`, which
`npm run align` (`scripts/align_voiceover.py`) produces as follows:

1. Synthesize a reference read of the script phrases with Kokoro.
2. Align that reference to the real take with DTW on MFCC features.
3. Snap each boundary to the nearest pause in the take.

You can also adjust the start times in `vo-timeline.json` by hand.
Transitions are set in `TRANSITIONS` in `src/timing.ts`.

## The voice

The current take is an **ElevenLabs** read ("Marcus - Bright, Upbeat and
Clear"), supplied by the client. The original is in
`public/audio/vo/source/elevenlabs-marcus.mp3`. A copy loudness-normalized
to -16 LUFS is in `public/audio/vo/voiceover.mp3`.

To swap it:

* **Another recording:** replace `public/audio/vo/voiceover.mp3`. Normalize
  it first, for example with
  `ffmpeg -i in.mp3 -af loudnorm=I=-16:TP=-1.5 -ar 44100 -ac 2 voiceover.mp3`.
  Then run `npm run align`. The take must be shorter than 60 s.
* **Generate one:** run `npm run voiceover`. It uses ElevenLabs if
  `ELEVENLABS_API_KEY` is set, otherwise the free local Kokoro model. Then
  run `npm run align`. The aligner needs
  `pip install librosa soundfile kokoro-onnx` and the Kokoro model in
  `.tts-models/`, which `npm run voiceover` downloads.

## Swapping the music

The bundled `public/audio/music/bed.mp3` is synthesized from scratch by
`scripts/generate_music.py`, so there are no rights issues. To use your own
licensed track:

1. Put it in `public/audio/music/`.
2. Point `MUSIC_FILE` in `src/theme.ts` at it.

The music plays at `MUSIC_VOLUME` (0.15) and ducks to `MUSIC_DUCKED_VOLUME`
under the voice. It fades out over the final 20 frames.

## Brand: logo and colors

* **Logo:** `public/brand/logo-source.jpg` is the client-supplied logo.
  `scripts/trace_logo.py` vectorizes it with potrace into
  `src/brand/logoPaths.ts` and `public/brand/logo.svg`. It produces one
  compound path per brand ink. `components/BrandLogo.tsx` animates it in
  parts: the swirl spins in, then The / Leading / Link / .ae rise in, then
  the tagline writes on. It has two variants:
  * `color` for light backgrounds, used on the white end card
  * `reverse` for dark backgrounds, where the navy is swapped for white
  If you get the official vector logo, replace the paths in
  `src/brand/logoPaths.ts`.
* **Colors:** brand green `#63AF45` and brand navy `#2D2F7A`, sampled from
  the logo. The tints used on dark screens are derived from these two inks.
  The full table is in `src/theme.ts`. The live site could not be reached
  from the build environment, so the colors come from the logo file.

## Effects and sound design

* **Impacts:** `IMPACTS` in `src/timing.ts` lists every scene change and the
  big beats of the voiceover: the logo landing, "We architect growth", and
  each slogan line. On each impact the picture flashes and shakes, a light
  sweep crosses the frame, and a hit plays. A sub "boom" plays on the two
  logo reveals, with a riser leading into each.
* **Sound files:** all sound effects and the music bed are synthesized by
  `scripts/generate_music.py`, so there are no rights issues.

## Notes

* Platform names appear as plain text with generic icons. No third-party
  logos are used.
* The conversion counter in scene 6 is decorative and is labeled
  "Illustrative animation". It is not a client result.
* Rendering in a sandbox: `remotion.config.ts` uses a local Chromium headless
  shell when one exists at `/opt/pw-browsers`. Otherwise Remotion downloads
  its own.
