import React from "react";
import { AbsoluteFill, useCurrentFrame } from "remotion";
import { palette } from "../theme";
import { BrandSwirl } from "./BrandLogo";

// Tiny tiling noise texture for film grain (generated SVG, no external image).
const GRAIN = `url("data:image/svg+xml;utf8,${encodeURIComponent(
  `<svg xmlns='http://www.w3.org/2000/svg' width='220' height='220'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/><feColorMatrix values='0 0 0 0 1  0 0 0 0 1  0 0 0 0 1  0 0 0 0.9 0'/></filter><rect width='100%' height='100%' filter='url(#n)'/></svg>`,
)}")`;

/**
 * Animated gradient mesh + drifting grid + grain. `seed` shifts the blob
 * layout per scene so consecutive scenes don't look identical.
 */
export const Background: React.FC<{ seed?: number; intensity?: number }> = ({
  seed = 0,
  intensity = 1,
}) => {
  const frame = useCurrentFrame();
  const t = frame / 30 + seed * 3.1;
  const blob = (x: number, y: number, color: string, size: number, alpha: string) =>
    `radial-gradient(${size}px circle at ${x}% ${y}%, ${color}${alpha} 0%, transparent 70%)`;
  const mesh = [
    blob(20 + 10 * Math.sin(t * 0.35), 25 + 12 * Math.cos(t * 0.28), palette.primary, 900, "55"),
    blob(80 + 8 * Math.cos(t * 0.3), 70 + 10 * Math.sin(t * 0.4), palette.secondary, 1000, "4d"),
    blob(55 + 14 * Math.sin(t * 0.22 + 1), 105 + 6 * Math.sin(t * 0.5), palette.accent, 700, "26"),
  ].join(",");
  // grain jitters every frame so it reads as film texture
  const gx = (frame * 37) % 220;
  const gy = (frame * 53) % 220;
  return (
    <AbsoluteFill style={{ background: palette.bgDark, overflow: "hidden" }}>
      <AbsoluteFill style={{ backgroundImage: mesh, opacity: 0.75 * intensity }} />
      {/* grid drifts slower than foreground = parallax depth */}
      <AbsoluteFill
        style={{
          inset: -200,
          backgroundImage:
            "linear-gradient(rgba(255,255,255,0.045) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.045) 1px, transparent 1px)",
          backgroundSize: "80px 80px",
          transform: `translate(${(frame * 0.25) % 80}px, ${(frame * 0.4) % 80}px)`,
          maskImage: "radial-gradient(ellipse 70% 65% at 50% 50%, black 30%, transparent 85%)",
          WebkitMaskImage:
            "radial-gradient(ellipse 70% 65% at 50% 50%, black 30%, transparent 85%)",
        }}
      />
      {/* the logo swirl as a huge, slowly turning motif */}
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <BrandSwirl
          size={1500}
          rotate={frame * 0.6 + seed * 47}
          opacity={0.055 * intensity}
          style={{
            position: "absolute",
            left: `${[62, -18, 70, -10, 55, -22, 64, 20, -15, 30][seed % 10]}%`,
            top: `${[-30, 20, 30, -35, 35, -10, -40, 40, 25, -20][seed % 10]}%`,
          }}
        />
      </AbsoluteFill>
      <AbsoluteFill
        style={{
          backgroundImage: GRAIN,
          backgroundPosition: `${gx}px ${gy}px`,
          opacity: 0.06,
          mixBlendMode: "overlay",
        }}
      />
      <AbsoluteFill
        style={{
          background:
            "radial-gradient(ellipse 85% 80% at 50% 50%, transparent 55%, rgba(3,5,12,0.75) 100%)",
        }}
      />
    </AbsoluteFill>
  );
};
