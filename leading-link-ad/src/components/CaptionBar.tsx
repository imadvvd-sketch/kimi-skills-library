import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { CAPTIONS } from "../timing";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { clamp } from "./anim";
import { interpolate } from "remotion";

/** Burned-in captions, synced to the voiceover phrases in timing.ts. */
export const CaptionBar: React.FC = () => {
  const frame = useCurrentFrame();
  const { vertical } = useLayout();
  const cap = CAPTIONS.find((c) => frame >= c.start && frame < c.end);
  if (!cap) return null;
  const local = frame - cap.start;
  const inP = interpolate(local, [0, 5], [0, 1], clamp);
  const outP = interpolate(frame, [cap.end - 4, cap.end], [1, 0], clamp);
  const o = Math.min(inP, outP);
  return (
    <AbsoluteFill
      style={{
        justifyContent: "flex-end",
        alignItems: "center",
        paddingBottom: vertical ? 170 : 56,
        pointerEvents: "none",
      }}
    >
      <div
        style={{
          maxWidth: vertical ? 900 : 1500,
          padding: vertical ? "18px 34px" : "14px 34px",
          borderRadius: 999,
          background: "rgba(4,7,16,0.86)",
          border: "1px solid rgba(255,255,255,0.12)",
          boxShadow: "0 10px 40px rgba(0,0,0,0.5)",
          fontFamily: fonts.body,
          fontWeight: 600,
          fontSize: vertical ? 42 : 38,
          lineHeight: 1.25,
          color: palette.text,
          textAlign: "center",
          opacity: o,
          transform: `translateY(${(1 - inP) * 10}px)`,
          // 2 lines maximum
          display: "-webkit-box",
          WebkitLineClamp: 2,
          WebkitBoxOrient: "vertical",
          overflow: "hidden",
        }}
      >
        {cap.text}
      </div>
    </AbsoluteFill>
  );
};
