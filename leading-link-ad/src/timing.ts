/**
 * All timing lives here. Edit STORYBOARD to retime scenes; the voiceover
 * durations measured by `npm run voiceover` are folded in automatically so a
 * line never runs into the next scene, and the total always stays TOTAL_FRAMES.
 */
import voDurations from "./content/vo-durations.json";
import voiceover from "./content/voiceover.json";

export const FPS = 30;
export const TOTAL_FRAMES = 1800;
export const WIDTH = 1920;
export const HEIGHT = 1080;

/** Frames between a scene's first frame and its voiceover line starting. */
export const VO_LEAD = 6;
/** Minimum breathing room after a line ends before the next scene starts. */
const MIN_TAIL = 10;

export type SceneId =
  | "hook" | "brand" | "seo" | "web" | "marketing"
  | "ads" | "branding" | "video" | "automation" | "cta";

export type TransitionKind =
  | "fade" | "slide-left" | "slide-up" | "wipe-right" | "wipe-up"
  | "clock" | "flip" | "zoom";

type Board = { id: SceneId; frames: number; extraHold?: number };

/** The storyboard from the brief (frames at 30 fps, sums to 1800). */
const STORYBOARD: Board[] = [
  { id: "hook", frames: 150 },
  { id: "brand", frames: 210 },
  { id: "seo", frames: 240 },
  { id: "web", frames: 180 },
  { id: "marketing", frames: 180 },
  { id: "ads", frames: 180 },
  { id: "branding", frames: 180 },
  { id: "video", frames: 180 },
  { id: "automation", frames: 150 },
  // the final scene holds the URL on screen after the line ends
  { id: "cta", frames: 150, extraHold: 24 },
];

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

const voFrames = (id: SceneId) =>
  Math.ceil(((voDurations.seconds as Record<string, number>)[id] ?? 0) * FPS);

/**
 * Scene "slot" lengths. A slot is the time from a scene's start to the next
 * scene's start (the transition overlap is extra, see LeadingLinkAd.tsx), so
 * slots always sum to TOTAL_FRAMES.
 *
 * 1. each scene needs at least VO_LEAD + line + MIN_TAIL (+ extraHold)
 * 2. scenes that are too short are stretched to that minimum
 * 3. the frames this costs are taken from scenes with slack, in proportion to
 *    how much slack each one has
 */
function computeSlots(): number[] {
  const need = STORYBOARD.map(
    (s) => VO_LEAD + voFrames(s.id) + MIN_TAIL + (s.extraHold ?? 0),
  );
  const slots = STORYBOARD.map((s, i) => Math.max(s.frames, need[i]));
  let over = slots.reduce((a, b) => a + b, 0) - TOTAL_FRAMES;
  const slack = slots.map((v, i) => Math.max(0, v - need[i]));
  const totalSlack = slack.reduce((a, b) => a + b, 0);
  if (over > totalSlack) {
    throw new Error(
      `Voiceover is ${over - totalSlack} frames too long to fit ${TOTAL_FRAMES} frames`,
    );
  }
  const cut = slack.map((s) => Math.floor((s / (totalSlack || 1)) * over));
  for (let i = 0; i < slots.length; i++) slots[i] -= cut[i];
  over -= cut.reduce((a, b) => a + b, 0);
  // hand out rounding leftovers to the scenes with the most slack left
  while (over > 0) {
    const i = slots
      .map((v, idx) => [v - need[idx], idx])
      .sort((a, b) => b[0] - a[0])[0][1];
    slots[i]--;
    over--;
  }
  return slots;
}

const slots = computeSlots();

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

export const SCENES: SceneTiming[] = (() => {
  let t = 0;
  return STORYBOARD.map((s, index) => {
    const start = t;
    t += slots[index];
    return {
      id: s.id,
      index,
      start,
      slot: slots[index],
      outFrames: TRANSITIONS[index]?.frames ?? 0,
      voStart: start + VO_LEAD,
      voEnd: start + VO_LEAD + voFrames(s.id),
    };
  });
})();

export const sceneById = (id: SceneId) => SCENES.find((s) => s.id === id)!;

// ---------------------------------------------------------------------------
// Captions: each line's phrases are spread over the measured clip in
// proportion to their character count (a good proxy for speaking time).
export type Caption = { text: string; start: number; end: number; sceneId: SceneId };

export const CAPTIONS: Caption[] = SCENES.flatMap((scene) => {
  const line = voiceover.lines.find((l) => l.id === scene.id);
  if (!line) return [];
  const chars = line.captions.map((c) => c.length);
  const total = chars.reduce((a, b) => a + b, 0);
  const span = scene.voEnd - scene.voStart;
  let acc = 0;
  return line.captions.map((text, i) => {
    const start = scene.voStart + Math.round((acc / total) * span);
    acc += chars[i];
    const end =
      i === line.captions.length - 1
        ? Math.min(scene.voEnd + 10, scene.start + scene.slot)
        : scene.voStart + Math.round((acc / total) * span);
    return { text, start, end, sceneId: scene.id };
  });
});

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
export const CTA_BEATS = (() => {
  const cta = sceneById("cta");
  const start = cta.start + cue("cta", 0);
  const end = cta.start + cue("cta", 1);
  const parts = ["One agency. ", "Every channel. ", "Real growth."];
  const total = parts.join("").length;
  let acc = 0;
  return parts.map((t) => {
    const f = start + Math.round((acc / total) * (end - start));
    acc += t.length;
    return f - cta.start;
  });
})();

export const IMPACTS: Impact[] = ([
  ...SCENES.slice(1).map((s) => ({ frame: s.start, strength: 0.55, sound: "hit" as const })),
  { frame: sceneById("hook").start + cue("hook", 1) - 2, strength: 0.8, sound: "hit" },
  // logo lands
  { frame: sceneById("brand").start + 24, strength: 1, sound: "boom" },
  { frame: sceneById("brand").start + cue("brand", 3) - 2, strength: 0.9, sound: "hit" },
  ...CTA_BEATS.slice(1).map((f) => ({ frame: sceneById("cta").start + f - 2, strength: 0.7, sound: "hit" as const })),
  // final logo on the white end card
  { frame: sceneById("cta").start + cue("cta", 1) - 4, strength: 1, sound: "boom" },
] as Impact[]).sort((a, b) => a.frame - b.frame);
