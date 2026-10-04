import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { LOGO_COLORS, LOGO_LAYERS, LOGO_VIEWBOX } from "../brand/logoPaths";
import { pop, range, clamp } from "./anim";

/**
 * Regions of the traced logo (in source-image pixels, 703x211). Each part is
 * clipped out of the two color layers so it can be animated on its own.
 */
const PARTS = {
  swirl: [],
  the: [[24, 98, 117, 69]],
  leading: [[141, 113, 272, 54], [141, 98, 44, 15], [285, 98, 128, 15]],
  link: [[413, 98, 150, 62]],
  ae: [[563, 98, 80, 62]],
  tagline: [[0, 167, 703, 44], [413, 160, 290, 7]],
} as const;
type Part = keyof typeof PARTS;
/** The swirl is clipped with a circle so letters can't rotate into view. */
export const SWIRL_CENTER = { x: 226.5, y: 60.5, r: 49.5 };

type Variant = "color" | "reverse";

const Layers: React.FC<{ variant: Variant }> = ({ variant }) => (
  <>
    <path
      transform={LOGO_LAYERS.navy.transform}
      d={LOGO_LAYERS.navy.d}
      fill={variant === "color" ? LOGO_COLORS.navy : "#FFFFFF"}
    />
    <path transform={LOGO_LAYERS.green.transform} d={LOGO_LAYERS.green.d} fill={LOGO_COLORS.green} />
  </>
);

/**
 * The real The Leading Link logo with an animated build:
 * swirl spins in -> words rise in one by one -> tagline writes on.
 * `variant="reverse"` swaps navy for white so it reads on dark backgrounds.
 * `delay` < -100 renders the finished logo with no animation.
 */
export const BrandLogo: React.FC<{
  width: number;
  delay?: number;
  variant?: Variant;
  showTagline?: boolean;
  glow?: number;
  /** speed multiplier for the build */
  pace?: number;
}> = ({ width, delay = 0, variant = "reverse", showTagline = true, glow = 0.6, pace = 1 }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const id = React.useId().replace(/:/g, "");
  const f = (frame - delay) * pace;
  const swirl = pop(f, fps, 0, 13);
  const spin = interpolate(f, [0, 26], [-220, 0], { ...clamp, easing: (t) => 1 - (1 - t) ** 3 });
  const words: Part[] = ["the", "leading", "link", "ae"];
  const tag = range(f, 30, 52);
  const H = LOGO_VIEWBOX.height;
  const visibleH = showTagline ? H : 167;
  return (
    <svg
      width={width}
      height={(width * visibleH) / LOGO_VIEWBOX.width}
      viewBox={`0 0 ${LOGO_VIEWBOX.width} ${visibleH}`}
      style={{
        overflow: "visible",
        filter: glow > 0 ? `drop-shadow(0 0 ${18 * glow}px rgba(99,175,69,${0.55 * glow}))` : undefined,
      }}
    >
      <defs>
        <clipPath id={`${id}-swirlc`}>
          <circle cx={SWIRL_CENTER.x} cy={SWIRL_CENTER.y} r={SWIRL_CENTER.r} />
        </clipPath>
        {(Object.keys(PARTS) as Part[]).filter((p) => p !== "swirl").map((p) => (
          <clipPath id={`${id}-${p}`} key={p}>
            {PARTS[p].map(([x, y, w, h], i) => (
              <rect key={i} x={x} y={y} width={p === "tagline" ? w * tag : w} height={h} />
            ))}
          </clipPath>
        ))}
      </defs>
      {/* swirl */}
      <g clipPath={`url(#${id}-swirlc)`}>
        <g
          transform={`rotate(${spin} ${SWIRL_CENTER.x} ${SWIRL_CENTER.y}) translate(${SWIRL_CENTER.x} ${SWIRL_CENTER.y}) scale(${Math.max(0.001, swirl)}) translate(${-SWIRL_CENTER.x} ${-SWIRL_CENTER.y})`}
          opacity={Math.min(1, swirl * 1.5)}
        >
          <Layers variant={variant} />
        </g>
      </g>
      {/* wordmark, word by word: each rises out of its own clip box */}
      {words.map((w, i) => {
        const p = pop(f, fps, 12 + i * 4, 14);
        return (
          <g key={w} clipPath={`url(#${id}-${w})`}>
            <g transform={`translate(0 ${(1 - p) * 70})`} opacity={Math.min(1, p * 1.6)}>
              <Layers variant={variant} />
            </g>
          </g>
        );
      })}
      {/* tagline writes on left to right */}
      {showTagline ? (
        <g clipPath={`url(#${id}-tagline)`}>
          <Layers variant={variant} />
        </g>
      ) : null}
    </svg>
  );
};

/** Just the swirl mark, used as a recurring motif and as an icon. */
export const BrandSwirl: React.FC<{
  size: number;
  variant?: Variant;
  rotate?: number;
  opacity?: number;
  style?: React.CSSProperties;
}> = ({ size, variant = "reverse", rotate = 0, opacity = 1, style }) => {
  const id = React.useId().replace(/:/g, "");
  const { x, y } = SWIRL_CENTER;
  const half = 52;
  return (
    <svg
      width={size}
      height={size}
      viewBox={`${x - half} ${y - half} ${half * 2} ${half * 2}`}
      style={{ overflow: "visible", opacity, ...style }}
    >
      <defs>
        <clipPath id={`${id}-s`}>
          <circle cx={x} cy={y} r={SWIRL_CENTER.r} />
        </clipPath>
      </defs>
      <g clipPath={`url(#${id}-s)`}>
        <g transform={`rotate(${rotate} ${x} ${y})`}>
          <Layers variant={variant} />
        </g>
      </g>
    </svg>
  );
};
