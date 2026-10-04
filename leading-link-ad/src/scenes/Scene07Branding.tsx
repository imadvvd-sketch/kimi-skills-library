import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Newspaper } from "lucide-react";
import { SceneFrame, Stage } from "../components/SceneFrame";
import { GlowCard } from "../components/GlowCard";
import { Chip } from "../components/Chip";
import { pop, range } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const N = 72;
// rough hand-drawn blob vs. a polished rounded-hexagon mark; same point count
// so the path can be morphed point by point
const rough = Array.from({ length: N }, (_, i) => {
  const a = (i / N) * Math.PI * 2;
  const r = 150 + 22 * Math.sin(a * 3 + 0.7) + 14 * Math.sin(a * 7) + 8 * Math.cos(a * 11);
  return [Math.cos(a) * r, Math.sin(a) * r];
});
const polished = Array.from({ length: N }, (_, i) => {
  const a = (i / N) * Math.PI * 2;
  // superellipse-ish hexagon
  const k = Math.cos(Math.PI / 6) / Math.cos(((a % (Math.PI / 3)) - Math.PI / 6));
  const r = 160 * (0.82 * k + 0.18);
  return [Math.cos(a - Math.PI / 2) * r, Math.sin(a - Math.PI / 2) * r];
});
const PALETTE = [palette.bgDark, palette.primary, palette.secondary, palette.accent, palette.highlight];

export const Scene07Branding: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v } = useLayout();
  const m = range(frame, 14, 44);
  const d =
    rough
      .map((p, i) => {
        const x = interpolate(m, [0, 1], [p[0], polished[i][0]]);
        const y = interpolate(m, [0, 1], [p[1], polished[i][1]]);
        return `${i ? "L" : "M"}${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(" ") + " Z";
  const draw = range(frame, 0, 16);
  const inner = pop(frame, fps, 40, 12);
  const prAt = cue("branding", 1) - 6;
  const sweep = range(frame, 30, 60);
  return (
    <SceneFrame duration={duration} seed={6} kicker="05  /  BRAND" title="BRANDING & IDENTITY" highlight={["identity"]} titleSize={v(88, 84)}>
      <Stage gap={v(90, 50)} top={v(250, 470)}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 30 }}>
          <svg width={380} height={380} viewBox="-190 -190 380 380" style={{ overflow: "visible" }}>
            <defs>
              <linearGradient id="br" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0" stopColor={palette.accent} />
                <stop offset="0.5" stopColor={palette.primary} />
                <stop offset="1" stopColor={palette.secondary} />
              </linearGradient>
            </defs>
            <path
              d={d}
              fill="url(#br)"
              fillOpacity={m * 0.9}
              stroke={m < 0.5 ? palette.textMuted : "url(#br)"}
              strokeWidth={m < 0.5 ? 4 : 3}
              strokeDasharray={m < 0.6 ? "14 10" : undefined}
              pathLength={m < 0.6 ? undefined : 1}
              strokeDashoffset={0}
              opacity={draw}
              style={{ filter: `drop-shadow(0 0 ${30 * m}px ${palette.primary}aa)` }}
            />
            {/* a generic client mark appears once the shape is polished */}
            <g opacity={inner} transform={`scale(${0.7 + 0.3 * inner})`}>
              <circle r={62} fill="none" stroke="#fff" strokeWidth={14} />
              <circle r={18} fill="#fff" />
            </g>
          </svg>
          {/* palette swatches sweep in */}
          <div style={{ display: "flex", gap: 12 }}>
            {PALETTE.map((c, i) => {
              const s = Math.min(1, Math.max(0, sweep * 6 - i));
              return (
                <div
                  key={c}
                  style={{
                    width: 64,
                    height: 64,
                    borderRadius: 18,
                    background: c,
                    border: "1.5px solid rgba(255,255,255,0.25)",
                    transform: `translateX(${(1 - s) * -60}px) scale(${0.6 + 0.4 * s})`,
                    opacity: s,
                    boxShadow: `0 0 24px ${c}66`,
                  }}
                />
              );
            })}
          </div>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 26, alignItems: v("flex-start", "center") }}>
          <GlowCard delay={prAt} width={v(640, 860)} from="scale" glow={palette.highlight}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, fontFamily: fonts.body, fontWeight: 700, fontSize: 24, color: palette.highlight, letterSpacing: "0.1em" }}>
              <Newspaper size={28} /> PR & OUTREACH
            </div>
            <div style={{ fontFamily: fonts.heading, fontWeight: 700, fontSize: v(50, 52), color: palette.text, lineHeight: 1.1, marginTop: 16 }}>
              Your brand, in the headlines.
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10, marginTop: 22 }}>
              <div style={{ width: "92%", height: 12, borderRadius: 99, background: "rgba(255,255,255,0.18)" }} />
              <div style={{ width: "76%", height: 12, borderRadius: 99, background: "rgba(255,255,255,0.12)" }} />
            </div>
          </GlowCard>
          <div style={{ display: "flex", gap: 16 }}>
            <Chip label="Brand Strategy" delay={20} size={28} />
            <Chip label="Visual Identity" delay={26} size={28} color={palette.secondary} />
          </div>
        </div>
      </Stage>
    </SceneFrame>
  );
};
