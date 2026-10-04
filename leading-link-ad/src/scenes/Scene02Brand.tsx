import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { SceneFrame } from "../components/SceneFrame";
import { AnimatedText } from "../components/AnimatedText";
import { BrandLogo } from "../components/BrandLogo";
import { pop, range, clamp } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

// Sparks that rush in and collapse into the swirl as it lands.
const SPARKS = Array.from({ length: 26 }, (_, i) => {
  const a = (i / 26) * Math.PI * 2 + (i % 3) * 0.3;
  return { a, r: 700 + (i % 5) * 140, size: 4 + (i % 4) * 2 };
});

const LOGO_DELAY = 10; // the swirl lands ~14 frames later, on the boom

export const Scene02Brand: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v } = useLayout();
  const growthAt = cue("brand", 2) - 6; // "We don't just run campaigns."
  const collapse = range(frame, 0, LOGO_DELAY + 14);
  const burst = interpolate(frame, [LOGO_DELAY + 10, LOGO_DELAY + 16, LOGO_DELAY + 50], [0, 1, 0.25], clamp);
  const lift = range(frame, growthAt, growthAt + 18);
  const logoW = v(1180, 900);
  const descriptor = pop(frame, fps, cue("brand", 1) - 4, 200);
  return (
    <SceneFrame duration={duration} seed={1}>
      {/* skyline bars rise behind "WE ARCHITECT GROWTH" */}
      <AbsoluteFill style={{ justifyContent: "flex-end", alignItems: "center", paddingBottom: v(110, 240) }}>
        <div style={{ display: "flex", alignItems: "flex-end", gap: v(18, 12), height: v(520, 760) }}>
          {Array.from({ length: v(22, 14) }, (_, i) => {
            const n = v(22, 14);
            const center = 1 - Math.abs(i - (n - 1) / 2) / ((n - 1) / 2);
            const h = (0.25 + 0.75 * center ** 1.4) * (0.75 + 0.25 * Math.sin(i * 2.3)) * v(520, 760);
            const p = pop(frame, fps, cue("brand", 3) - 10 + Math.abs(i - n / 2) * 1.5, 16);
            return (
              <div
                key={i}
                style={{
                  width: v(56, 52),
                  height: h * p,
                  borderRadius: "10px 10px 0 0",
                  background: `linear-gradient(180deg, ${i % 3 === 0 ? palette.accent : palette.primary}dd, ${palette.brandNavy}22)`,
                  boxShadow: `0 0 30px ${i % 3 === 0 ? palette.brandGreen : palette.primary}55`,
                  opacity: 0.3 + 0.4 * center,
                }}
              />
            );
          })}
        </div>
      </AbsoluteFill>

      {/* light burst + rays behind the logo */}
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <div
          style={{
            position: "absolute",
            width: 1600,
            height: 1600,
            borderRadius: 999,
            background: `radial-gradient(circle, rgba(255,255,255,0.5) 0%, ${palette.brandGreen}55 18%, ${palette.primary}33 38%, transparent 62%)`,
            opacity: burst * (1 - lift),
            transform: `scale(${0.6 + 0.6 * burst})`,
          }}
        />
        <div
          style={{
            position: "absolute",
            width: 2400,
            height: 2400,
            background: `repeating-conic-gradient(from ${frame * 0.8}deg, ${palette.brandGreen}22 0deg 4deg, transparent 4deg 18deg)`,
            maskImage: "radial-gradient(circle, black 0%, transparent 55%)",
            WebkitMaskImage: "radial-gradient(circle, black 0%, transparent 55%)",
            opacity: burst * 0.8 * (1 - lift),
          }}
        />
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <div
          style={{
            position: "relative",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 26,
            transform: `translateY(${lift * v(-250, -380)}px) scale(${1 - 0.3 * lift})`,
          }}
        >
          {/* sparks collapsing into the swirl */}
          <svg
            width={10}
            height={10}
            style={{ position: "absolute", left: logoW * (226 / 703), top: logoW * (60 / 703), overflow: "visible", opacity: 1 - collapse }}
          >
            {SPARKS.map((s, i) => {
              const r = s.r * (1 - collapse);
              return (
                <line
                  key={i}
                  x1={Math.cos(s.a) * r}
                  y1={Math.sin(s.a) * r}
                  x2={Math.cos(s.a) * (r + 60 * (1 - collapse))}
                  y2={Math.sin(s.a) * (r + 60 * (1 - collapse))}
                  stroke={i % 2 ? palette.accent : "#fff"}
                  strokeWidth={s.size}
                  strokeLinecap="round"
                />
              );
            })}
          </svg>
          <BrandLogo width={logoW} delay={LOGO_DELAY} variant="reverse" glow={0.4 + 0.6 * burst} />
          <div
            style={{
              fontFamily: fonts.body,
              fontWeight: 600,
              fontSize: v(32, 30),
              letterSpacing: "0.22em",
              color: palette.textMuted,
              textTransform: "uppercase",
              opacity: descriptor * (1 - lift),
              transform: `translateY(${(1 - descriptor) * 16}px)`,
            }}
          >
            Performance-led digital agency · Dubai
          </div>
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", paddingTop: v(250, 360) }}>
        <div style={{ opacity: interpolate(frame, [growthAt, growthAt + 4], [0, 1], clamp) }}>
          <AnimatedText
            text="WE ARCHITECT GROWTH"
            delay={cue("brand", 3) - 6}
            highlight={["growth"]}
            size={v(140, 124)}
            stagger={5}
            style={{ maxWidth: v(1760, 920) }}
          />
        </div>
      </AbsoluteFill>
    </SceneFrame>
  );
};
