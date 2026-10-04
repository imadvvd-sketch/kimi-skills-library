import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Search, Users, Camera, AtSign, MessagesSquare, Target } from "lucide-react";
import { SceneFrame, Stage } from "../components/SceneFrame";
import { Counter } from "../components/Counter";
import { pop, range } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

// Typed platform names with neutral, generic icons (no official logos).
const PLATFORMS = [
  { name: "Google", icon: Search, color: palette.primary },
  { name: "Meta", icon: Users, color: palette.secondary },
  { name: "Snapchat", icon: Camera, color: palette.highlight },
  { name: "X", icon: AtSign, color: palette.text },
  { name: "Reddit", icon: MessagesSquare, color: palette.accent },
];

const Funnel: React.FC<{ fill: number; frame: number }> = ({ fill, frame }) => {
  const layers = [
    { label: "Reach", w: 520 },
    { label: "Clicks", w: 400 },
    { label: "Leads", w: 280 },
  ];
  return (
    <div style={{ position: "relative", display: "flex", flexDirection: "column", alignItems: "center", gap: 10 }}>
      {layers.map((l, i) => {
        const p = Math.min(1, Math.max(0, fill * 3 - i));
        const next = layers[i + 1]?.w ?? 170;
        return (
          <div key={l.label} style={{ position: "relative", width: l.w, height: 96 }}>
            <svg width={l.w} height={96} style={{ position: "absolute", inset: 0 }}>
              <defs>
                <linearGradient id={`fn${i}`} x1="0" x2="1">
                  <stop offset="0" stopColor={palette.primary} />
                  <stop offset="1" stopColor={palette.secondary} />
                </linearGradient>
              </defs>
              <path
                d={`M0,0 L${l.w},0 L${(l.w + next) / 2},96 L${(l.w - next) / 2},96 Z`}
                fill={`url(#fn${i})`}
                fillOpacity={0.15 + 0.55 * p}
                stroke={palette.accent}
                strokeOpacity={0.4 + 0.6 * p}
                strokeWidth={2}
              />
            </svg>
            <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center", fontFamily: fonts.body, fontWeight: 700, fontSize: 28, color: palette.text, opacity: 0.5 + 0.5 * p }}>
              {l.label}
            </div>
          </div>
        );
      })}
      {/* particles dropping through the funnel */}
      {Array.from({ length: 8 }, (_, i) => {
        const t = ((frame * 1.6 + i * 13) % 100) / 100;
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              top: t * 320 - 20,
              left: "50%",
              width: 12,
              height: 12,
              borderRadius: 99,
              background: palette.accent,
              boxShadow: `0 0 12px ${palette.accent}`,
              transform: `translateX(${Math.sin(i * 2.1) * 200 * (1 - t)}px)`,
              opacity: fill > 0.2 ? (1 - t) * 0.9 : 0,
            }}
          />
        );
      })}
    </div>
  );
};

export const Scene06Ads: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v } = useLayout();
  const fill = range(frame, 30, 30 + 50);
  const counterAt = cue("ads", 1) - 4;
  const counterIn = pop(frame, fps, counterAt - 4, 14);
  return (
    <SceneFrame duration={duration} seed={5} kicker="04  /  ADS" title="PAID ADVERTISING" highlight={["advertising"]}>
      <Stage gap={v(110, 50)} top={v(260, 450)}>
        <div style={{ display: "flex", flexDirection: v("column", "row"), flexWrap: "wrap", justifyContent: "center", gap: v(16, 16), width: v(undefined, 920) }}>
          {PLATFORMS.map((p, i) => {
            const s = pop(frame, fps, 4 + i * 4, 14);
            const Icon = p.icon;
            return (
              <div
                key={p.name}
                style={{
                  width: v(380, 290),
                  height: v(88, 84),
                  display: "flex",
                  alignItems: "center",
                  gap: 18,
                  padding: "0 24px",
                  borderRadius: 20,
                  background: "rgba(255,255,255,0.06)",
                  border: `1.5px solid ${p.color}66`,
                  boxShadow: `0 0 30px ${p.color}22`,
                  transform: `translateX(${(1 - s) * -200}px)`,
                  opacity: s,
                  fontFamily: fonts.body,
                  fontWeight: 700,
                  fontSize: v(34, 32),
                  color: palette.text,
                }}
              >
                <div style={{ width: 52, height: 52, borderRadius: 14, background: `${p.color}26`, display: "flex", alignItems: "center", justifyContent: "center" }}>
                  <Icon size={28} color={p.color} />
                </div>
                {p.name}
                <div style={{ marginLeft: "auto", width: 10, height: 10, borderRadius: 99, background: palette.success, boxShadow: `0 0 10px ${palette.success}`, opacity: 0.5 + 0.5 * Math.sin(frame / 5 + i) }} />
              </div>
            );
          })}
        </div>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 28 }}>
          <Funnel fill={fill} frame={frame} />
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 18,
              padding: "16px 30px",
              borderRadius: 22,
              background: "rgba(10,14,26,0.9)",
              border: `1.5px solid ${palette.highlight}88`,
              boxShadow: `0 0 40px ${palette.highlight}33`,
              opacity: counterIn,
              transform: `scale(${0.85 + 0.15 * counterIn})`,
            }}
          >
            <Target size={34} color={palette.highlight} />
            <div style={{ display: "flex", flexDirection: "column" }}>
              <div style={{ fontFamily: fonts.body, fontSize: 22, color: palette.textMuted, fontWeight: 600, letterSpacing: "0.06em" }}>CONVERSION TRACKING</div>
              <Counter
                from={0}
                to={1284}
                start={counterAt}
                duration={40}
                style={{ fontFamily: fonts.heading, fontWeight: 700, fontSize: 52, color: palette.highlight, lineHeight: 1.05 }}
              />
            </div>
          </div>
        </div>
      </Stage>
      {/* tiny disclaimer: the number is illustrative */}
      <div style={{ position: "absolute", right: v(80, 80), bottom: v(150, 290), fontFamily: fonts.body, fontSize: 16, color: palette.textMuted, opacity: interpolate(counterIn, [0, 1], [0, 0.7]) }}>
        Illustrative animation
      </div>
    </SceneFrame>
  );
};
