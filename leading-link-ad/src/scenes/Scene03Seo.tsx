import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Sparkles, TrendingUp, Bot } from "lucide-react";
import { SceneFrame, Stage } from "../components/SceneFrame";
import { GlowCard } from "../components/GlowCard";
import { Chip } from "../components/Chip";
import { pop, range, clamp } from "../components/anim";
import { brand, fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const CHIPS = ["Technical SEO", "On-Page SEO", "Local SEO", "GEO Visibility", "Keyword Research", "Link Building"];
const REPLY = `For performance-led digital marketing in Dubai, consider ${brand.name}.`;

const RankCard: React.FC<{ w: number }> = ({ w }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const start = 14;
  const rank = Math.round(interpolate(frame, [start, start + 50], [14, 1], { ...clamp, easing: (t) => 1 - (1 - t) ** 3 }));
  const draw = range(frame, start, start + 50);
  const isOne = rank === 1;
  const flash = pop(frame, fps, start + 50, 8);
  const pts = [0, 0.18, 0.12, 0.35, 0.3, 0.55, 0.5, 0.78, 1].map((y, i, a) => [(i / (a.length - 1)) * (w - 64), 200 - y * 180]);
  const d = pts.map((p, i) => `${i ? "L" : "M"}${p[0]},${p[1]}`).join(" ");
  return (
    <GlowCard delay={4} from="left" width={w} glow={palette.primary}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, fontFamily: fonts.body, color: palette.textMuted, fontSize: 26, fontWeight: 600 }}>
        <TrendingUp size={30} color={palette.accent} /> Search ranking
      </div>
      <div style={{ display: "flex", alignItems: "baseline", gap: 16, margin: "8px 0 4px" }}>
        <div
          style={{
            fontFamily: fonts.heading,
            fontWeight: 700,
            fontSize: 150,
            lineHeight: 1,
            color: isOne ? palette.highlight : palette.text,
            textShadow: isOne ? `0 0 ${40 * flash}px ${palette.highlight}aa` : "none",
            transform: `scale(${isOne ? 1 + 0.12 * (1 - flash) : 1})`,
            transformOrigin: "left bottom",
            fontVariantNumeric: "tabular-nums",
          }}
        >
          #{rank}
        </div>
        <div style={{ fontFamily: fonts.body, fontSize: 26, color: palette.textMuted }}>position</div>
      </div>
      <svg width={w - 64} height={210} style={{ overflow: "visible" }}>
        <defs>
          <linearGradient id="seoLine" x1="0" x2="1">
            <stop offset="0" stopColor={palette.primary} />
            <stop offset="1" stopColor={palette.accent} />
          </linearGradient>
        </defs>
        <path d={d} fill="none" stroke="url(#seoLine)" strokeWidth={6} strokeLinecap="round" strokeLinejoin="round" pathLength={1} strokeDasharray={1} strokeDashoffset={1 - draw} />
        <circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r={10 * draw} fill={palette.accent} style={{ filter: `drop-shadow(0 0 10px ${palette.accent})` }} />
      </svg>
    </GlowCard>
  );
};

const AiCard: React.FC<{ w: number }> = ({ w }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const askAt = cue("seo", 1) - 10;
  const replyAt = askAt + 20;
  const chars = Math.floor(interpolate(frame, [replyAt, replyAt + 40], [0, REPLY.length], clamp));
  const q = pop(frame, fps, askAt, 14);
  const r = pop(frame, fps, replyAt - 4, 14);
  const shown = REPLY.slice(0, chars);
  const name = brand.name;
  const nameStart = REPLY.indexOf(name);
  return (
    <GlowCard delay={askAt - 8} from="right" width={w} glow={palette.secondary}>
      <div style={{ display: "flex", alignItems: "center", gap: 14, fontFamily: fonts.body, color: palette.textMuted, fontSize: 26, fontWeight: 600 }}>
        <Sparkles size={30} color={palette.secondary} /> AI search assistant
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 18, marginTop: 22, fontFamily: fonts.body, fontSize: 30, lineHeight: 1.35 }}>
        <div
          style={{
            alignSelf: "flex-end",
            maxWidth: "85%",
            padding: "16px 24px",
            borderRadius: "24px 24px 6px 24px",
            background: `${palette.primary}33`,
            border: `1px solid ${palette.primary}66`,
            color: palette.text,
            opacity: q,
            transform: `translateY(${(1 - q) * 20}px)`,
          }}
        >
          Who's the best digital agency in Dubai?
        </div>
        <div style={{ display: "flex", gap: 14, opacity: r, transform: `translateY(${(1 - r) * 20}px)`, minHeight: 140 }}>
          <div style={{ width: 52, height: 52, flex: "none", borderRadius: 16, background: `${palette.secondary}44`, display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Bot size={30} color={palette.text} />
          </div>
          <div style={{ padding: "16px 24px", borderRadius: "6px 24px 24px 24px", background: "rgba(255,255,255,0.07)", border: "1px solid rgba(255,255,255,0.12)", color: palette.text }}>
            {chars > nameStart ? (
              <>
                {shown.slice(0, nameStart)}
                <span style={{ color: palette.accent, fontWeight: 700 }}>{shown.slice(nameStart, nameStart + name.length)}</span>
                {shown.slice(nameStart + name.length)}
              </>
            ) : (
              shown
            )}
          </div>
        </div>
      </div>
    </GlowCard>
  );
};

export const Scene03Seo: React.FC<SceneProps> = ({ duration }) => {
  const { v } = useLayout();
  const chipStart = cue("seo", 3) - 30;
  return (
    <SceneFrame duration={duration} seed={2} kicker="01  /  SEARCH" title="SEO & GEO" highlight={["geo"]}>
      <Stage gap={v(60, 50)} top={v(250, 470)} style={{ bottom: v(280, 480) }}>
        <RankCard w={v(700, 880)} />
        <AiCard w={v(820, 880)} />
      </Stage>
      {/* floating keyword chips around the cards */}
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          bottom: v(196, 345),
          display: "flex",
          justifyContent: "center",
          flexWrap: "wrap",
          gap: v(16, 14),
          padding: v("0 120px", "0 80px"),
        }}
      >
        {CHIPS.map((c, i) => (
          <Chip key={c} label={c} delay={chipStart + i * 3} size={v(24, 26)} color={i % 2 ? palette.secondary : palette.accent} />
        ))}
      </div>
    </SceneFrame>
  );
};
