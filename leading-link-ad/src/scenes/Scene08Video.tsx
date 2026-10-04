import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Film, Box, Sparkles, Play } from "lucide-react";
import { SceneFrame } from "../components/SceneFrame";
import { AnimatedText } from "../components/AnimatedText";
import { Chip } from "../components/Chip";
import { pop, range } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

/** Play button that turns into a rotating CSS-3D cube. */
const PlayToCube: React.FC = () => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const play = pop(frame, fps, 2, 10);
  const morph = range(frame, 26, 40);
  const S = 210;
  const faces = [
    { icon: Film, t: `translateZ(${S / 2}px)`, c: palette.primary },
    { icon: Box, t: `rotateY(90deg) translateZ(${S / 2}px)`, c: palette.secondary },
    { icon: Sparkles, t: `rotateY(180deg) translateZ(${S / 2}px)`, c: palette.accent },
    { icon: Play, t: `rotateY(-90deg) translateZ(${S / 2}px)`, c: palette.primary },
    { icon: Box, t: `rotateX(90deg) translateZ(${S / 2}px)`, c: palette.secondary },
    { icon: Film, t: `rotateX(-90deg) translateZ(${S / 2}px)`, c: palette.accent },
  ];
  const rot = Math.max(0, frame - 26);
  return (
    <div style={{ position: "relative", width: 340, height: 340, display: "flex", alignItems: "center", justifyContent: "center" }}>
      {/* play button */}
      <div
        style={{
          position: "absolute",
          width: 230,
          height: 230,
          borderRadius: 999,
          background: `radial-gradient(circle at 30% 30%, ${palette.primary}, ${palette.secondary})`,
          boxShadow: `0 0 80px ${palette.primary}88`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          transform: `scale(${play * (1 - morph)})`,
          opacity: 1 - morph,
        }}
      >
        <Play size={100} color="#fff" fill="#fff" style={{ marginLeft: 14 }} />
      </div>
      {/* cube */}
      <div style={{ perspective: 1100, opacity: morph, transform: `scale(${0.4 + 0.6 * morph})` }}>
        <div
          style={{
            width: S,
            height: S,
            position: "relative",
            transformStyle: "preserve-3d",
            transform: `rotateX(${-22 + rot * 1.2}deg) rotateY(${30 + rot * 2.4}deg)`,
          }}
        >
          {faces.map((f, i) => {
            const Icon = f.icon;
            return (
              <div
                key={i}
                style={{
                  position: "absolute",
                  inset: 0,
                  transform: f.t,
                  borderRadius: 18,
                  background: `linear-gradient(135deg, ${f.c}dd, rgba(10,14,26,0.85))`,
                  border: `2px solid ${f.c}`,
                  boxShadow: `inset 0 0 40px ${f.c}66`,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  backfaceVisibility: "hidden",
                }}
              >
                <Icon size={84} color="#fff" strokeWidth={1.6} />
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

/** Isometric exhibition stand assembled piece by piece (pure SVG). */
const ExhibitionStand: React.FC<{ start: number }> = ({ start }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  // iso projection helpers
  const iso = (x: number, y: number, z: number) => [(x - y) * 0.866, (x + y) * 0.5 - z];
  const poly = (pts: number[][]) => pts.map((p) => iso(p[0], p[1], p[2]).join(",")).join(" ");
  const piece = (i: number) => pop(frame, fps, start + i * 5, 12);
  const drop = (i: number) => (1 - piece(i)) * -140;
  const pieces = [
    // floor
    { pts: [[0, 0, 0], [260, 0, 0], [260, 200, 0], [0, 200, 0]], fill: "#1B2440", stroke: palette.primary },
    // back wall
    { pts: [[0, 0, 0], [260, 0, 0], [260, 0, 170], [0, 0, 170]], fill: `${palette.primary}55`, stroke: palette.primary },
    // side wall
    { pts: [[0, 0, 0], [0, 200, 0], [0, 200, 170], [0, 0, 170]], fill: `${palette.secondary}55`, stroke: palette.secondary },
    // screen on back wall
    { pts: [[60, 1, 60], [200, 1, 60], [200, 1, 140], [60, 1, 140]], fill: `${palette.accent}cc`, stroke: palette.accent },
    // counter top
    { pts: [[90, 120, 55], [200, 120, 55], [200, 160, 55], [90, 160, 55]], fill: "#E8ECF8", stroke: "#fff" },
    // counter front
    { pts: [[90, 160, 0], [200, 160, 0], [200, 160, 55], [90, 160, 55]], fill: palette.highlight, stroke: palette.highlight },
    // header banner
    { pts: [[0, 0, 190], [260, 0, 190], [260, 0, 220], [0, 0, 220]], fill: palette.secondary, stroke: palette.secondary },
  ];
  return (
    <svg width={460} height={360} viewBox="-210 -260 460 360" style={{ overflow: "visible" }}>
      {pieces.map((p, i) => (
        <g key={i} transform={`translate(0 ${drop(i)})`} opacity={piece(i)}>
          <polygon points={poly(p.pts)} fill={p.fill} stroke={p.stroke} strokeWidth={2} strokeLinejoin="round" />
        </g>
      ))}
      {/* spotlights */}
      {[60, 200].map((x, i) => {
        const on = range(frame, start + 40 + i * 4, start + 50 + i * 4);
        const [sx, sy] = iso(x, 0, 230);
        const [ex, ey] = iso(x, 120, 0);
        return (
          <polygon
            key={x}
            points={`${sx - 6},${sy} ${sx + 6},${sy} ${ex + 60},${ey} ${ex - 60},${ey}`}
            fill="#fff"
            opacity={0.1 * on}
          />
        );
      })}
    </svg>
  );
};

export const Scene08Video: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { v, width, height, safe, captionSpace } = useLayout();
  const eventsAt = cue("video", 1) - 12;
  const divider = range(frame, 0, 20);
  const half: React.CSSProperties = {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: v(30, 22),
  };
  return (
    <SceneFrame duration={duration} seed={7}>
      <div
        style={{
          position: "absolute",
          top: safe,
          bottom: captionSpace - 20,
          left: safe,
          right: safe,
          display: "flex",
          flexDirection: v("row", "column"),
        }}
      >
        <div style={half}>
          <AnimatedText text="VIDEO PRODUCTION" size={v(68, 72)} delay={2} highlight={["video"]} />
          <PlayToCube />
          <div style={{ display: "flex", gap: 12, flexWrap: "wrap", justifyContent: "center" }}>
            <Chip label="Video" delay={30} size={24} />
            <Chip label="3D Animation" delay={34} size={24} color={palette.secondary} />
            <Chip label="AI Video" delay={38} size={24} color={palette.primary} />
          </div>
        </div>
        <div style={half}>
          <AnimatedText text="EVENTS & EXHIBITIONS" size={v(68, 72)} delay={eventsAt} highlight={["exhibitions"]} />
          <ExhibitionStand start={eventsAt + 6} />
        </div>
      </div>
      {/* glowing divider between the halves */}
      <div
        style={{
          position: "absolute",
          background: `linear-gradient(${v("180deg", "90deg")}, transparent, ${palette.accent}, transparent)`,
          boxShadow: `0 0 20px ${palette.accent}`,
          ...(v(true, false)
            ? { left: width / 2 - 1, top: height / 2 - (380 * divider), width: 2, height: 760 * divider }
            : { top: (height - captionSpace + safe) / 2 - 1, left: width / 2 - 420 * divider, height: 2, width: 840 * divider }),
          opacity: interpolate(divider, [0, 1], [0, 0.8]),
        }}
      />
    </SceneFrame>
  );
};
