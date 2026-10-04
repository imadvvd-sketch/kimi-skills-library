import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame } from "remotion";
import { Background } from "./Background";
import { AnimatedText } from "./AnimatedText";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { range } from "./anim";

/**
 * Shared scene wrapper: opaque animated background, a continuous slow camera
 * drift (scale 1.0 -> 1.05) on the foreground, and an optional title block.
 */
export const SceneFrame: React.FC<{
  children?: React.ReactNode;
  /** total frames the scene is on screen (slot + outgoing transition) */
  duration: number;
  seed?: number;
  kicker?: string;
  title?: string;
  highlight?: string[];
  titleDelay?: number;
  titleSize?: number;
}> = ({ children, duration, seed = 0, kicker, title, highlight, titleDelay = 0, titleSize }) => {
  const frame = useCurrentFrame();
  const { safe, v } = useLayout();
  const drift = interpolate(frame, [0, duration], [1, 1.05], {
    extrapolateRight: "clamp",
  });
  const k = range(frame, titleDelay, titleDelay + 12);
  return (
    <AbsoluteFill>
      {/* background drifts less than the foreground -> parallax */}
      <AbsoluteFill style={{ transform: `scale(${1 + (drift - 1) * 0.4})` }}>
        <Background seed={seed} />
      </AbsoluteFill>
      <AbsoluteFill style={{ transform: `scale(${drift})` }}>
        {title ? (
          <div
            style={{
              position: "absolute",
              top: v(safe + 4, safe + 90),
              left: safe,
              right: safe,
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 12,
            }}
          >
            {kicker ? (
              <div
                style={{
                  fontFamily: fonts.body,
                  fontWeight: 600,
                  fontSize: v(24, 28),
                  letterSpacing: "0.32em",
                  color: palette.accent,
                  opacity: k,
                  transform: `translateY(${(1 - k) * 12}px)`,
                }}
              >
                {kicker}
              </div>
            ) : null}
            <AnimatedText
              text={title}
              delay={titleDelay + 3}
              highlight={highlight}
              size={titleSize ?? v(92, 84)}
              stagger={3}
            />
          </div>
        ) : null}
        {children}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

/** Content area below the title and above the captions. */
export const Stage: React.FC<{
  children: React.ReactNode;
  gap?: number;
  /** force a direction; default is row in 16:9 and column in 9:16 */
  direction?: "row" | "column";
  top?: number;
  style?: React.CSSProperties;
}> = ({ children, gap = 60, direction, top, style }) => {
  const { safe, v, captionSpace } = useLayout();
  return (
    <div
      style={{
        position: "absolute",
        top: top ?? v(270, 500),
        bottom: captionSpace,
        left: safe,
        right: safe,
        display: "flex",
        flexDirection: direction ?? v("row", "column"),
        alignItems: "center",
        justifyContent: "center",
        gap,
        ...style,
      }}
    >
      {children}
    </div>
  );
};
