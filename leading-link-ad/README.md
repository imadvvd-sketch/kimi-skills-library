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
  content/voiceover.json voiceover lines and caption phrases (one source of truth)
  content/vo-durations.json  measured voiceover lengths (generated)
  brand/logoPaths.ts     traced logo paths (generated)
  components/            AnimatedText, GlowCard, IconBadge, Counter, CaptionBar,
                         Chip, BrandLogo, Background, SceneFrame, Fx, zoomThrough
  scenes/                Scene01Hook ... Scene10Cta
scripts/
  generate_voiceover.py  makes public/audio/vo/*.mp3 and vo-durations.json
  generate_music.py      synthesizes the music bed and SFX (license-free)
  trace_logo.py          vectorizes the supplied logo
  render-stills.mjs      renders verification stills
```

## Changing texts

* **Voiceover and captions:** edit `src/content/voiceover.json`. `tts` is the
  text the voice reads, and `captions` is the list of on-screen caption
  phrases. Then run `npm run voiceover`.
* **On-screen titles and labels:** these are in each scene file under
  `src/scenes/`. Brand name, tagline and URL are in `brand` in `src/theme.ts`.

## Changing timings

Edit `STORYBOARD` and `TRANSITIONS` in `src/timing.ts`. Each scene's slot is
calculated as follows:

1. Start from the storyboard length.
2. Stretch a scene if its measured voiceover line plus padding needs more
   room (`VO_LEAD`, `MIN_TAIL`).
3. Take the extra frames back from scenes that have slack, so the video always
   stays at exactly `TOTAL_FRAMES` (1800).

Scene animations sync to the voiceover with `cue(sceneId, phraseIndex)`, so
they follow automatically when you retime or re-record.

## Swapping the voice

`npm run voiceover` picks its engine automatically:

* **ElevenLabs** (recommended for a cinematic read): run
  `ELEVENLABS_API_KEY=... [ELEVENLABS_VOICE_ID=...] npm run voiceover`.
* **Kokoro** (default, local and free): an Apache-2.0 open-weight neural TTS.
  Its model (~350 MB) downloads into `.tts-models/` the first time it runs.
  Change the voice with `KOKORO_VOICE` (for example `am_michael`,
  `bm_george` or `af_heart`) and the speed with `KOKORO_SPEED`.
  It needs `pip install kokoro-onnx soundfile`.
* **Your own recordings:** replace `public/audio/vo/NN-<id>.mp3` with your
  files. Then update the durations in `src/content/vo-durations.json`, which
  you can measure with `ffprobe`.

Each line is trimmed of silence and loudness-normalized (-16 LUFS).

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
