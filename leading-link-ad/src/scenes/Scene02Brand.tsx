import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { SceneFrame } from "../components/SceneFrame";
import { AnimatedText } from "../components/AnimatedText";
import { Logo } from "../components/Logo";
import { pop, range, clamp } from "../components/anim";
import { palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

// Node constellation that assembles around the logo before it draws on.
const NODES = Array.from({ length: 14 }, (_, i) => {
  const a = (i / 14) * Math.PI * 2;
  return {
    // scattered start
    sx: Math.cos(a * 3.3 + 1) * 820,
    sy: Math.sin(a * 2.1 + 0.4) * 460,
    // ring around the mark
    ex: Math.cos(a) * 330,
    ey: Math.sin(a) * 200,
  };
});

export const Scene02Brand: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v, vertical } = useLayout();
  const growthAt = cue("brand", 2) - 6; // "We don't just run campaigns."
  const gather = range(frame, 0, 26);
  const nodesOut = range(frame, 30, 44);
  // logo lifts up to make room for the headline
  const lift = range(frame, growthAt, growthAt + 18);
  return (
    <SceneFrame duration={duration} seed={1}>
      {/* skyline bars rise behind "WE ARCHITECT GROWTH" */}
      <AbsoluteFill style={{ justifyContent: "flex-end", alignItems: "center", paddingBottom: v(180, 330) }}>
        <div style={{ display: "flex", alignItems: "flex-end", gap: v(18, 12), height: v(520, 760), opacity: 0.9 }}>
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
                  background: `linear-gradient(180deg, ${i % 3 === 0 ? palette.accent : palette.primary}cc, ${palette.secondary}22)`,
                  boxShadow: `0 0 30px ${palette.primary}44`,
                  opacity: 0.35 + 0.35 * center,
                }}
              />
            );
          })}
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
        <div
          style={{
            position: "relative",
            transform: `translateY(${lift * v(-200, -330)}px) scale(${1 - 0.22 * lift})`,
          }}
        >
          {/* glowing nodes + links converging */}
          <svg
            width={1800}
            height={1000}
            viewBox="-900 -500 1800 1000"
            style={{ position: "absolute", left: "50%", top: "50%", transform: "translate(-50%,-50%)", opacity: 1 - nodesOut, overflow: "visible" }}
          >
            {NODES.map((n, i) => {
              const x = interpolate(gather, [0, 1], [n.sx, n.ex]);
              const y = interpolate(gather, [0, 1], [n.sy, n.ey]);
              const nx = NODES[(i + 1) % NODES.length];
              const x2 = interpolate(gather, [0, 1], [nx.sx, nx.ex]);
              const y2 = interpolate(gather, [0, 1], [nx.sy, nx.ey]);
              return (
                <g key={i}>
                  <line x1={x} y1={y} x2={x2} y2={y2} stroke={palette.accent} strokeOpacity={0.5 * gather} strokeWidth={2} />
                  <circle cx={x} cy={y} r={7} fill={palette.accent} style={{ filter: `drop-shadow(0 0 10px ${palette.accent})` }} />
                </g>
              );
            })}
          </svg>
          <Logo delay={18} showTagline taglineDelay={cue("brand", 1) - 18} vertical={vertical} scale={v(1, 0.95)} />
        </div>
      </AbsoluteFill>

      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", paddingTop: v(260, 380) }}>
        <div style={{ opacity: interpolate(frame, [growthAt, growthAt + 4], [0, 1], clamp) }}>
          <AnimatedText
            text="WE ARCHITECT GROWTH"
            delay={cue("brand", 3) - 6}
            highlight={["growth"]}
            size={v(130, 120)}
            stagger={5}
            style={{ maxWidth: v(1700, 920) }}
          />
        </div>
      </AbsoluteFill>
    </SceneFrame>
  );
};
