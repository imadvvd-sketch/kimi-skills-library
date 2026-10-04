import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Search, Globe, Share2, Target, Palette, Film, Bot, Mail, ArrowRight } from "lucide-react";
import { SceneFrame } from "../components/SceneFrame";
import { AnimatedText } from "../components/AnimatedText";
import { Logo } from "../components/Logo";
import { pop, range, clamp } from "../components/anim";
import { brand, fonts, gradients, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const ICONS = [Search, Globe, Share2, Target, Palette, Film, Bot, Mail];

export const Scene10Cta: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v, vertical, width, height } = useLayout();
  const logoAt = cue("cta", 1) - 6;
  const urlAt = cue("cta", 2) - 6;
  // icons fly in from the edges and converge into the logo
  const converge = range(frame, logoAt - 22, logoAt + 2);
  const flash = interpolate(frame, [logoAt - 2, logoAt + 4, logoAt + 16], [0, 0.9, 0], clamp);
  const btn = pop(frame, fps, urlAt, 11);
  const pulse = 1 + 0.035 * Math.sin((frame - urlAt) / 5) * (frame > urlAt + 10 ? 1 : 0);
  const logoY = v(height * 0.5 - 20, height * 0.47);
  return (
    <SceneFrame duration={duration} seed={9}>
      <div style={{ position: "absolute", top: v(110, 260), left: 80, right: 80 }}>
        <AnimatedText
          text="ONE AGENCY. EVERY CHANNEL. REAL GROWTH."
          size={v(76, 84)}
          stagger={v(5, 5)}
          delay={2}
          highlight={["growth."]}
          style={{ maxWidth: v(1760, 920), margin: "0 auto" }}
        />
      </div>
      {ICONS.map((Icon, i) => {
        const a = (i / ICONS.length) * Math.PI * 2 + 0.4;
        const r = v(900, 700);
        const appear = pop(frame, fps, 2 + i * 2, 14);
        const x = width / 2 + Math.cos(a) * r * (1 - converge) * (0.55 + 0.45 * appear);
        const y = logoY + Math.sin(a) * r * 0.6 * (1 - converge) * (0.55 + 0.45 * appear);
        return (
          <div
            key={i}
            style={{
              position: "absolute",
              left: x - 40,
              top: y - 40,
              width: 80,
              height: 80,
              borderRadius: 24,
              background: "rgba(13,19,38,0.85)",
              border: `1.5px solid ${palette.primary}88`,
              boxShadow: `0 0 24px ${palette.primary}55`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              opacity: appear * (1 - converge),
              transform: `scale(${1 - 0.6 * converge}) rotate(${(1 - converge) * 30 - 15}deg)`,
            }}
          >
            <Icon size={40} color={palette.text} />
          </div>
        );
      })}
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: logoY,
          display: "flex",
          justifyContent: "center",
          transform: "translateY(-50%)",
          opacity: frame >= logoAt - 2 ? 1 : 0,
        }}
      >
        <Logo delay={logoAt - 2} scale={v(1.05, 0.95)} vertical={vertical} />
      </div>
      {/* convergence flash */}
      <div
        style={{
          position: "absolute",
          left: width / 2 - 500,
          top: logoY - 500,
          width: 1000,
          height: 1000,
          borderRadius: 999,
          background: `radial-gradient(circle, ${palette.accent}cc 0%, ${palette.primary}44 30%, transparent 65%)`,
          opacity: flash,
        }}
      />
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: v(height * 0.5 + 150, height * 0.47 + 260),
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: 22,
          opacity: btn,
          transform: `translateY(${(1 - btn) * 30}px)`,
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 16,
            padding: v("22px 54px", "26px 60px"),
            borderRadius: 999,
            background: gradients.primary,
            boxShadow: `0 0 ${50 + 30 * (pulse - 1) * 28}px ${palette.primary}aa, inset 0 1px 0 rgba(255,255,255,0.3)`,
            fontFamily: fonts.heading,
            fontWeight: 700,
            fontSize: v(46, 50),
            color: "#fff",
            transform: `scale(${pulse})`,
          }}
        >
          Start growing <ArrowRight size={44} strokeWidth={2.6} />
        </div>
        <div style={{ fontFamily: fonts.body, fontWeight: 600, fontSize: v(44, 50), color: palette.text, letterSpacing: "0.02em" }}>
          {brand.url}
        </div>
      </div>
    </SceneFrame>
  );
};
