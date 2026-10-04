import React from "react";
import { AbsoluteFill, interpolate, random, useCurrentFrame, useVideoConfig } from "remotion";
import { Search } from "lucide-react";
import { SceneFrame } from "../components/SceneFrame";
import { AnimatedText } from "../components/AnimatedText";
import { pop, clamp, range } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const QUERY = "best digital agency in dubai";
const RESULTS = [
  { title: "Competitor Agency | Digital Marketing", url: "competitor-agency.com" },
  { title: "Another Agency - SEO & Ads Services", url: "another-agency.ae" },
  { title: "Top 10 Marketing Agencies (List)", url: "agency-directory.com" },
];

/** The search UI itself; rendered once normally, or 3x for the RGB split. */
const SearchUI: React.FC<{ frame: number; fps: number; slot: number }> = ({ frame, fps, slot }) => {
  const { v } = useLayout();
  const barIn = pop(frame, fps, 4, 14);
  const typeStart = 12;
  const chars = Math.floor(interpolate(frame, [typeStart, typeStart + 34], [0, QUERY.length], clamp));
  const caretOn = Math.floor(frame / 8) % 2 === 0 || chars < QUERY.length;
  const resultsAt = typeStart + 40;
  // cursor glides onto the first (competitor) result
  const hoverAt = resultsAt + 22;
  const cur = range(frame, hoverAt - 14, hoverAt + 4);
  const W = v(1100, 900);
  return (
    <div style={{ width: W, display: "flex", flexDirection: "column", gap: 18, position: "relative" }}>
      <div
        style={{
          height: 104,
          borderRadius: 999,
          display: "flex",
          alignItems: "center",
          gap: 22,
          padding: "0 40px",
          background: "rgba(16,22,42,0.9)",
          border: `2px solid ${palette.primary}aa`,
          boxShadow: `0 0 60px ${palette.primary}55, 0 0 0 6px ${palette.primary}14`,
          transform: `scale(${0.8 + 0.2 * barIn})`,
          opacity: barIn,
        }}
      >
        <Search size={44} color={palette.accent} strokeWidth={2.4} />
        <div style={{ fontFamily: fonts.body, fontSize: v(42, 40), color: palette.text, fontWeight: 500 }}>
          {QUERY.slice(0, chars)}
          <span style={{ opacity: caretOn ? 1 : 0, color: palette.accent }}>|</span>
        </div>
      </div>
      {RESULTS.map((r, i) => {
        const p = pop(frame, fps, resultsAt + i * 4, 16);
        const hovered = i === 0 && frame > hoverAt;
        return (
          <div
            key={r.url}
            style={{
              padding: "22px 36px",
              borderRadius: 22,
              background: hovered ? "rgba(59,130,246,0.16)" : "rgba(255,255,255,0.05)",
              border: `1px solid ${hovered ? palette.primary + "aa" : "rgba(255,255,255,0.10)"}`,
              opacity: p,
              transform: `translateY(${(1 - p) * 30}px)`,
              fontFamily: fonts.body,
            }}
          >
            <div style={{ fontSize: 22, color: palette.textMuted }}>{r.url}</div>
            <div style={{ fontSize: v(32, 30), color: hovered ? "#9CC2FF" : palette.text, fontWeight: 600, marginTop: 4 }}>
              {r.title}
            </div>
          </div>
        );
      })}
      {/* cursor */}
      <svg
        width={44}
        height={44}
        viewBox="0 0 24 24"
        style={{
          position: "absolute",
          left: interpolate(cur, [0, 1], [W * 0.9, W * 0.55]),
          top: interpolate(cur, [0, 1], [520, 178]),
          opacity: frame > hoverAt - 16 && frame < slot ? 1 : 0,
          filter: "drop-shadow(0 4px 8px rgba(0,0,0,0.6))",
        }}
      >
        <path d="M4 2l16 10-7 1.5L9.5 21z" fill="#fff" stroke="#0A0E1A" strokeWidth={1.2} />
      </svg>
    </div>
  );
};

const Content: React.FC<{ frame: number; fps: number; slot: number }> = (p) => {
  const { v } = useLayout();
  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: v(40, 70), paddingBottom: v(150, 260) }}>
      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6, minHeight: v(190, 330) }}>
        <AnimatedText text="YOUR CUSTOMERS ARE SEARCHING." size={v(84, 86)} delay={2} highlight={["searching"]} style={{ maxWidth: v(1700, 920) }} />
        <AnimatedText text="ARE THEY FINDING YOU?" size={v(84, 86)} delay={cue("hook", 1) - 4} highlight={["you?"]} style={{ maxWidth: v(1700, 920) }} />
      </div>
      <SearchUI {...p} />
    </div>
  );
};

export const Scene01Hook: React.FC<SceneProps> = ({ slot, duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  // RGB-split glitch over the last frames of the scene
  const g = interpolate(frame, [slot - 14, slot + 4], [0, 1], clamp);
  const jitter = g > 0 ? (random(`j${frame}`) - 0.5) * 60 * g : 0;
  const split = 26 * g;
  const centered: React.CSSProperties = { justifyContent: "center", alignItems: "center" };
  // each color channel is isolated with an SVG color matrix and offset sideways
  const layer = (channel: "r" | "g" | "b", dx: number) => (
    <AbsoluteFill
      style={{
        ...centered,
        transform: `translateX(${dx + jitter}px)`,
        filter: `url(#ch-${channel})`,
        mixBlendMode: "screen",
        opacity: 1 - g * 0.6,
      }}
    >
      <Content frame={frame} fps={fps} slot={slot} />
    </AbsoluteFill>
  );
  return (
    <SceneFrame duration={duration} seed={0}>
      <svg width={0} height={0} style={{ position: "absolute" }}>
        <filter id="ch-r"><feColorMatrix values="1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0" /></filter>
        <filter id="ch-g"><feColorMatrix values="0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 1 0" /></filter>
        <filter id="ch-b"><feColorMatrix values="0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 1 0" /></filter>
      </svg>
      {g > 0 ? (
        <AbsoluteFill style={{ isolation: "isolate" }}>
          {layer("r", -split)}
          {layer("g", 0)}
          {layer("b", split)}
          {/* horizontal tear bars */}
          {[0, 1, 2, 3].map((i) => (
            <div
              key={i}
              style={{
                position: "absolute",
                left: 0,
                right: 0,
                top: `${random(`t${i}${Math.floor(frame / 2)}`) * 100}%`,
                height: 6 + random(`h${i}${frame}`) * 40,
                background: i % 2 ? `${palette.accent}55` : `${palette.secondary}55`,
                transform: `translateX(${(random(`x${i}${frame}`) - 0.5) * 200}px)`,
                opacity: g,
              }}
            />
          ))}
        </AbsoluteFill>
      ) : (
        <AbsoluteFill style={centered}>
          <Content frame={frame} fps={fps} slot={slot} />
        </AbsoluteFill>
      )}
    </SceneFrame>
  );
};
