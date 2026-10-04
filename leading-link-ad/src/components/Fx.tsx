import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame } from "remotion";
import { IMPACTS } from "../timing";
import { palette } from "../theme";
import { clamp } from "./anim";

/** 0..1 strength of the most recent impact (decays over `len` frames). */
export const impactAt = (frame: number, len = 10) =>
  IMPACTS.reduce((acc, im) => {
    const d = frame - im.frame;
    if (d < 0 || d > len) return acc;
    return Math.max(acc, im.strength * (1 - d / len) ** 2);
  }, 0);

/** Wraps the picture and shakes it on every impact. */
export const CameraShake: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const frame = useCurrentFrame();
  const s = impactAt(frame, 9);
  const x = (random(`sx${frame}`) - 0.5) * 34 * s;
  const y = (random(`sy${frame}`) - 0.5) * 24 * s;
  return (
    <AbsoluteFill style={{ transform: `translate(${x}px, ${y}px) scale(${1 + 0.025 * s})` }}>
      {children}
    </AbsoluteFill>
  );
};

/** Flash + diagonal light sweep layered over the picture on impacts. */
export const ImpactFlash: React.FC = () => {
  const frame = useCurrentFrame();
  const flash = impactAt(frame, 6);
  // light sweep: a soft diagonal band racing across after each impact
  const last = [...IMPACTS].reverse().find((im) => im.frame <= frame && frame - im.frame < 22);
  const sweep = last ? interpolate(frame - last.frame, [0, 22], [-0.4, 1.4], clamp) : -1;
  return (
    <AbsoluteFill style={{ pointerEvents: "none", mixBlendMode: "screen" }}>
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 80% 70% at 50% 50%, rgba(255,255,255,0.9), ${palette.accent}55 45%, transparent 75%)`,
          opacity: flash * 0.55,
        }}
      />
      {last ? (
        <AbsoluteFill
          style={{
            background: `linear-gradient(115deg, transparent ${sweep * 100 - 18}%, ${palette.brandGreen}33 ${sweep * 100 - 6}%, rgba(255,255,255,0.28) ${sweep * 100}%, ${palette.primary}33 ${sweep * 100 + 6}%, transparent ${sweep * 100 + 18}%)`,
            opacity: last.strength,
          }}
        />
      ) : null}
    </AbsoluteFill>
  );
};
