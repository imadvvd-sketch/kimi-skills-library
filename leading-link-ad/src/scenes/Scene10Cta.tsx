import React from "react";
import { AbsoluteFill, interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Search, Globe, Share2, Target, Palette, Film, Bot, Mail, ArrowRight } from "lucide-react";
import { SceneFrame } from "../components/SceneFrame";
import { BrandLogo, BrandSwirl } from "../components/BrandLogo";
import { pop, range, clamp } from "../components/anim";
import { brand, fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { CTA_BEATS, cue } from "../timing";
import type { SceneProps } from "./types";

const ICONS = [Search, Globe, Share2, Target, Palette, Film, Bot, Mail];
const LINES = ["ONE AGENCY.", "EVERY CHANNEL.", "REAL GROWTH."];

/** One slogan line that slams in from big + blurred, then settles. */
const Slam: React.FC<{ text: string; at: number; size: number; green?: boolean }> = ({ text, at, size, green }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = pop(frame, fps, at, 16);
  const blur = interpolate(frame - at, [0, 6], [12, 0], clamp);
  return (
    <div
      style={{
        fontFamily: fonts.heading,
        fontWeight: 700,
        fontSize: size,
        lineHeight: 1,
        letterSpacing: "-0.02em",
        color: green ? palette.accent : palette.text,
        textShadow: green ? `0 0 40px ${palette.brandGreen}aa` : "0 0 30px rgba(255,255,255,0.15)",
        transform: `scale(${interpolate(p, [0, 1], [2.2, 1])})`,
        opacity: Math.min(1, p * 2),
        filter: `blur(${blur}px)`,
        whiteSpace: "nowrap",
      }}
    >
      {text}
    </div>
  );
};

export const Scene10Cta: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v, width, height } = useLayout();
  const logoAt = cue("cta", 3) - 4; // "The Leading Link."
  const urlAt = cue("cta", 4) - 6; // "Visit theleadinglink.ae"
  // icons fly in and get sucked into the center just before the reveal
  const converge = range(frame, logoAt - 20, logoAt);
  // white end card opens as an expanding circle from the center
  const open = range(frame, logoAt - 3, logoAt + 9);
  const radius = open * Math.hypot(width, height) * 0.6;
  const btn = pop(frame, fps, urlAt, 11);
  const url = pop(frame, fps, urlAt + 6, 200);
  const pulse = frame > urlAt + 12 ? 1 + 0.03 * Math.sin((frame - urlAt) / 5) : 1;
  const darkOut = 1 - range(frame, logoAt - 6, logoAt);
  return (
    <SceneFrame duration={duration} seed={9}>
      {/* --- dark phase: slogan slams --- */}
      <AbsoluteFill style={{ justifyContent: "center", alignItems: "center", opacity: darkOut }}>
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: v(26, 34) }}>
          {LINES.map((t, i) => (
            <Slam key={t} text={t} at={CTA_BEATS[i]} size={v(150, 118)} green={i === 2} />
          ))}
        </div>
      </AbsoluteFill>
      {ICONS.map((Icon, i) => {
        const a = (i / ICONS.length) * Math.PI * 2 + 0.4;
        const r = v(860, 640);
        const appear = pop(frame, fps, CTA_BEATS[1] + i * 2, 14);
        const x = width / 2 + Math.cos(a) * r * (1 - converge) * (0.6 + 0.4 * appear);
        const y = height / 2 + Math.sin(a) * r * v(0.52, 0.75) * (1 - converge) * (0.6 + 0.4 * appear);
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: x - 42,
              top: y - 42,
              width: 84,
              height: 84,
              borderRadius: 26,
              background: "rgba(13,15,45,0.85)",
              border: `1.5px solid ${i % 2 ? palette.brandGreen : palette.primary}aa`,
              boxShadow: `0 0 24px ${i % 2 ? palette.brandGreen : palette.primary}66`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              opacity: appear * (1 - converge) * darkOut,
              transform: `scale(${1 - 0.7 * converge}) rotate(${(1 - converge) * 30 - 15}deg)`,
            }}
          >
            <Icon size={42} color={palette.text} />
          </div>
        );
      })}

      {/* --- light phase: white end card with the full-color logo --- */}
      <AbsoluteFill
        style={{
          background: `radial-gradient(ellipse 80% 70% at 50% 45%, #FFFFFF 0%, ${palette.paper} 55%, #E9ECF5 100%)`,
          clipPath: `circle(${radius}px at 50% 50%)`,
        }}
      >
        {/* faint navy swirl watermark */}
        <AbsoluteFill style={{ justifyContent: "center", alignItems: "center" }}>
          <BrandSwirl size={v(1300, 1500)} variant="color" rotate={frame * 0.5} opacity={0.035} />
        </AbsoluteFill>
        <AbsoluteFill
          style={{
            justifyContent: "center",
            alignItems: "center",
            flexDirection: "column",
            gap: v(54, 70),
            paddingBottom: v(40, 120),
          }}
        >
          <BrandLogo width={v(1180, 900)} delay={logoAt + 2} variant="color" glow={0} pace={1.2} />
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              alignItems: "center",
              gap: 24,
              opacity: btn,
              transform: `translateY(${(1 - btn) * 30}px)`,
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 18,
                padding: v("24px 60px", "28px 66px"),
                borderRadius: 999,
                background: `linear-gradient(100deg, ${palette.brandGreen}, #4E9A33)`,
                boxShadow: `0 18px 50px ${palette.brandGreen}66, 0 0 0 ${10 * (pulse - 1) * 33}px ${palette.brandGreen}22, inset 0 1px 0 rgba(255,255,255,0.35)`,
                fontFamily: fonts.heading,
                fontWeight: 700,
                fontSize: v(50, 54),
                color: "#fff",
                transform: `scale(${pulse})`,
              }}
            >
              Start growing <ArrowRight size={48} strokeWidth={2.8} />
            </div>
            <div
              style={{
                fontFamily: fonts.heading,
                fontWeight: 700,
                fontSize: v(48, 54),
                color: palette.brandNavy,
                letterSpacing: "0.01em",
                opacity: url,
              }}
            >
              {brand.url}
            </div>
          </div>
        </AbsoluteFill>
      </AbsoluteFill>
    </SceneFrame>
  );
};
