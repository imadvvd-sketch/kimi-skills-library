import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { pop, range } from "./anim";
import { brand, fonts, gradients, palette } from "../theme";

/**
 * Chain-link mark drawn in SVG. Used because the official logo SVG could not
 * be downloaded in the build environment; swap in public/brand/*.svg later.
 * `draw` 0..1 animates the stroke on.
 */
export const ChainLinkMark: React.FC<{ size?: number; draw?: number; glow?: number }> = ({
  size = 200,
  draw = 1,
  glow = 1,
}) => {
  const id = React.useId().replace(/:/g, "");
  const link = (x: number, extra?: React.SVGProps<SVGRectElement>) => (
    <rect
      x={x}
      y={72}
      width={108}
      height={56}
      rx={28}
      fill="none"
      stroke={`url(#g${id})`}
      strokeWidth={15}
      strokeLinecap="round"
      pathLength={1}
      strokeDasharray={1}
      strokeDashoffset={1 - draw}
      {...extra}
    />
  );
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 200 200"
      style={{ overflow: "visible", filter: `drop-shadow(0 0 ${18 * glow}px ${palette.primary}aa)` }}
    >
      <defs>
        <linearGradient id={`g${id}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor={palette.accent} />
          <stop offset="0.5" stopColor={palette.primary} />
          <stop offset="1" stopColor={palette.secondary} />
        </linearGradient>
        <clipPath id={`c${id}`}>
          <rect x="100" y="108" width="45" height="40" />
        </clipPath>
      </defs>
      <g transform="rotate(-40 100 100)">
        {link(22)}
        {link(70)}
        {/* redraw one crossing of the first link on top so the links interlock */}
        {link(22, { clipPath: `url(#c${id})` })}
      </g>
    </svg>
  );
};

/** Mark + "THE LEADING LINK" wordmark, with optional tagline. */
export const Logo: React.FC<{
  delay?: number;
  scale?: number;
  showTagline?: boolean;
  taglineDelay?: number;
  vertical?: boolean;
}> = ({ delay = 0, scale = 1, showTagline = false, taglineDelay = 30, vertical = false }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const draw = range(frame, delay, delay + 28);
  const letters = brand.wordmark.split("");
  const tag = pop(frame, fps, delay + taglineDelay, 200);
  return (
    <div
      style={{
        display: "flex",
        flexDirection: vertical ? "column" : "row",
        alignItems: "center",
        gap: 28 * scale,
      }}
    >
      <ChainLinkMark size={170 * scale} draw={draw} />
      <div style={{ display: "flex", flexDirection: "column", alignItems: vertical ? "center" : "flex-start" }}>
        <div
          style={{
            fontFamily: fonts.heading,
            fontWeight: 700,
            fontSize: 92 * scale,
            letterSpacing: "0.04em",
            color: palette.text,
            display: "flex",
            whiteSpace: "pre",
          }}
        >
          {letters.map((ch, i) => {
            const p = pop(frame, fps, delay + 12 + i * 1.2, 14);
            const inLink = i >= "THE LEADING ".length;
            return (
              <span
                key={i}
                style={{
                  display: "inline-block",
                  opacity: p,
                  transform: `translateY(${(1 - p) * 40}px)`,
                  ...(inLink
                    ? {
                        backgroundImage: gradients.primaryAccent,
                        WebkitBackgroundClip: "text",
                        backgroundClip: "text",
                        color: "transparent",
                      }
                    : null),
                }}
              >
                {ch}
              </span>
            );
          })}
        </div>
        {showTagline ? (
          <div
            style={{
              fontFamily: fonts.body,
              fontWeight: 500,
              fontSize: 34 * scale,
              color: palette.textMuted,
              letterSpacing: "0.12em",
              textTransform: "uppercase",
              marginTop: 6 * scale,
              opacity: tag,
              transform: `translateY(${(1 - tag) * 16}px)`,
            }}
          >
            {brand.tagline}
          </div>
        ) : null}
      </div>
    </div>
  );
};
