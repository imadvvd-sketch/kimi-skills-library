import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { pop } from "./anim";
import { glass, palette } from "../theme";

type Props = {
  children: React.ReactNode;
  delay?: number;
  glow?: string;
  width?: number | string;
  height?: number | string;
  padding?: number;
  radius?: number;
  style?: React.CSSProperties;
  /** entrance direction */
  from?: "below" | "left" | "right" | "scale";
};

/** Glassmorphism panel with a soft neon edge glow and a spring entrance. */
export const GlowCard: React.FC<Props> = ({
  children,
  delay = 0,
  glow = palette.primary,
  width,
  height,
  padding = 32,
  radius = 28,
  style,
  from = "below",
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = pop(frame, fps, delay, 14);
  const offset = (1 - p) * 80;
  const transform =
    from === "left"
      ? `translateX(${-offset}px)`
      : from === "right"
        ? `translateX(${offset}px)`
        : from === "scale"
          ? `scale(${0.85 + 0.15 * p})`
          : `translateY(${offset}px)`;
  return (
    <div
      style={{
        position: "relative",
        width,
        height,
        padding,
        borderRadius: radius,
        background: glass.background,
        border: glass.border,
        backdropFilter: "blur(18px)",
        boxShadow: `0 0 0 1px ${glow}22, 0 20px 60px rgba(0,0,0,0.45), 0 0 60px ${glow}33`,
        opacity: Math.min(1, p * 1.3),
        transform,
        overflow: "hidden",
        ...style,
      }}
    >
      {/* top edge highlight */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          borderRadius: radius,
          background: `linear-gradient(180deg, ${glow}1f 0%, transparent 35%)`,
          pointerEvents: "none",
        }}
      />
      <div style={{ position: "relative", height: "100%" }}>{children}</div>
    </div>
  );
};
