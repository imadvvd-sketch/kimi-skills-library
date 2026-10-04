import { interpolate, spring, Easing } from "remotion";

/** Entrance spring with a slight overshoot (the house motion style). */
export const pop = (frame: number, fps: number, delay = 0, damping = 12) =>
  spring({ frame: frame - delay, fps, config: { damping, stiffness: 140, mass: 0.7 } });

/** Smooth, non-overshooting spring. */
export const ease = (frame: number, fps: number, delay = 0, durationInFrames?: number) =>
  spring({ frame: frame - delay, fps, config: { damping: 200 }, durationInFrames });

export const clamp = { extrapolateLeft: "clamp", extrapolateRight: "clamp" } as const;

export const range = (frame: number, from: number, to: number, out: [number, number] = [0, 1]) =>
  interpolate(frame, [from, to], out, { ...clamp, easing: Easing.bezier(0.22, 1, 0.36, 1) });
