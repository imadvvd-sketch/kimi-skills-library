import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { pop } from "./anim";
import { fonts, palette } from "../theme";

/** Small floating pill label; bobs gently so it never sits perfectly still. */
export const Chip: React.FC<{
  label: string;
  delay?: number;
  color?: string;
  size?: number;
  bob?: number;
  style?: React.CSSProperties;
}> = ({ label, delay = 0, color = palette.accent, size = 26, bob = 6, style }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = pop(frame, fps, delay, 10);
  const y = Math.sin((frame + delay * 7) / 18) * bob;
  return (
    <div
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 10,
        padding: `${size * 0.42}px ${size * 0.85}px`,
        borderRadius: 999,
        background: "rgba(13,19,38,0.78)",
        border: `1.5px solid ${color}88`,
        boxShadow: `0 0 24px ${color}33`,
        fontFamily: fonts.body,
        fontWeight: 600,
        fontSize: size,
        color: palette.text,
        whiteSpace: "nowrap",
        transform: `translateY(${y + (1 - p) * 30}px) scale(${0.6 + 0.4 * p})`,
        opacity: Math.min(1, p * 1.4),
        ...style,
      }}
    >
      <span
        style={{
          width: size * 0.36,
          height: size * 0.36,
          borderRadius: 99,
          background: color,
          boxShadow: `0 0 12px ${color}`,
        }}
      />
      {label}
    </div>
  );
};
