/**
 * All timing lives here. Scene timing follows the recorded voiceover: every
 * scene starts just before its line in the take (src/content/vo-timeline.json,
 * produced by `npm run align`), and the total always stays TOTAL_FRAMES.
 */
import timeline from "./content/vo-timeline.json";

export const FPS = 30;
export const TOTAL_FRAMES = 1800;
export const WIDTH = 1920;
export const HEIGHT = 1080;

/** The single voiceover take, placed at this frame. */
export const VO_FILE = timeline.file;
export const VO_OFFSET = 0;
/** A scene cuts in this many frames before its line starts. */
const SCENE_LEAD = 8;

export type SceneId =
  | "hook" | "brand" | "seo" | "web" | "marketing"
  | "ads" | "branding" | "video" | "automation" | "cta";

export type TransitionKind =
  | "fade" | "slide-left" | "slide-up" | "wipe-right" | "wipe-up"
  | "clock" | "flip" | "zoom";

/** Transition INTO scene i+1 (index i). Varied on purpose, 8-12 frames. */
export const TRANSITIONS: { kind: TransitionKind; frames: number }[] = [
  { kind: "zoom", frames: 10 }, // glitch-out hook -> punch into the logo
  { kind: "wipe-right", frames: 12 },
  { kind: "zoom", frames: 10 },
  { kind: "slide-left", frames: 10 },
  { kind: "zoom", frames: 10 },
  { kind: "wipe-up", frames: 12 },
  { kind: "flip", frames: 12 },
  { kind: "slide-left", frames: 10 },
  { kind: "zoom", frames: 12 },
];

const sec = (t: number) => VO_OFFSET + Math.round(t * FPS);

if (sec(timeline.durationSec) > TOTAL_FRAMES) {
  throw new Error(`Voiceover (${timeline.durationSec}s) is longer than ${TOTAL_FRAMES} frames`);
}

export type SceneTiming = {
  id: SceneId;
  index: number;
  /** absolute first frame */
  start: number;
  /** frames until the next scene starts */
  slot: number;
  /** frames of overlap with the next scene (0 for the last scene) */
  outFrames: number;
  /** absolute frame where the voiceover line starts / ends */
  voStart: number;
  voEnd: number;
};

export const SCENES: SceneTiming[] = timeline.lines.map((line, index, all) => {
  const start = index === 0 ? 0 : sec(line.start) - SCENE_LEAD;
  const next = index + 1 < all.length ? sec(all[index + 1].start) - SCENE_LEAD : TOTAL_FRAMES;
  return {
    id: line.id as SceneId,
    index,
    start,
    slot: next - start,
    outFrames: index + 1 < all.length ? TRANSITIONS[index].frames : 0,
    voStart: sec(line.start),
    voEnd: sec(line.end),
  };
});

export const sceneById = (id: SceneId) => SCENES.find((s) => s.id === id)!;

// ---------------------------------------------------------------------------
// Captions: one per script phrase, from where it starts in the take until the
// next phrase starts (the last phrase of a line ends with the line).
export type Caption = { text: string; start: number; end: number; sceneId: SceneId };

export const CAPTIONS: Caption[] = timeline.lines.flatMap((line) =>
  line.phrases.map((p, i) => ({
    text: p.text,
    start: sec(p.start),
    end: i + 1 < line.phrases.length ? sec(line.phrases[i + 1].start) : sec(line.end) + 10,
    sceneId: line.id as SceneId,
  })),
);

/**
 * Frame (relative to the scene's own start) at which caption phrase `i` of a
 * scene begins. Scenes use this to sync visuals to the voiceover.
 */
export const cue = (id: SceneId, i: number) => {
  const c = CAPTIONS.filter((x) => x.sceneId === id)[i];
  return c ? c.start - sceneById(id).start : 0;
};

// ---------------------------------------------------------------------------
// Impacts: frames where the picture flashes and shakes and an impact sound
// hits. Every scene change is one, plus the big beats of the voiceover.
export type Impact = { frame: number; strength: number; sound: "hit" | "boom" };

/** CTA slogan beats: "One agency." / "Every channel." / "Real growth." */
export const CTA_BEATS = [0, 1, 2].map((i) => cue("cta", i));

export const IMPACTS: Impact[] = ([
  ...SCENES.slice(1).map((s) => ({ frame: s.start, strength: 0.55, sound: "hit" as const })),
  { frame: sceneById("hook").start + cue("hook", 1) - 2, strength: 0.8, sound: "hit" },
  // logo lands
  { frame: sceneById("brand").start + 24, strength: 1, sound: "boom" },
  { frame: sceneById("brand").start + cue("brand", 3) - 2, strength: 0.9, sound: "hit" },
  ...CTA_BEATS.slice(1).map((f) => ({ frame: sceneById("cta").start + f - 2, strength: 0.7, sound: "hit" as const })),
  // final logo on the white end card ("The Leading Link.")
  { frame: sceneById("cta").start + cue("cta", 3) - 4, strength: 1, sound: "boom" },
] as Impact[]).sort((a, b) => a.frame - b.frame);
