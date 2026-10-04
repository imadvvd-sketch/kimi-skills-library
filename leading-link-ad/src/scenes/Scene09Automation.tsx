import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { Bot, UserRound, MessageCircle, Database, BarChart3, Moon, Zap } from "lucide-react";
import { SceneFrame, Stage } from "../components/SceneFrame";
import { GlowCard } from "../components/GlowCard";
import { IconBadge } from "../components/IconBadge";
import { pop, range } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const STEPS = [
  { label: "Lead", icon: UserRound, color: palette.accent },
  { label: "WhatsApp", icon: MessageCircle, color: palette.success },
  { label: "CRM", icon: Database, color: palette.primary },
  { label: "Report", icon: BarChart3, color: palette.secondary },
];

const Chat: React.FC<{ w: number }> = ({ w }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const msg = pop(frame, fps, 6, 14);
  const typingOn = frame > 14 && frame < 28;
  const reply = pop(frame, fps, 28, 14);
  const bubble: React.CSSProperties = { padding: "14px 22px", fontFamily: fonts.body, fontSize: 31, lineHeight: 1.3, color: palette.text, maxWidth: "80%" };
  return (
    <GlowCard delay={0} width={w} glow={palette.success} from="left">
      <div style={{ display: "flex", alignItems: "center", gap: 12, fontFamily: fonts.body, fontWeight: 600, fontSize: 24, color: palette.textMuted, marginBottom: 20 }}>
        <Bot size={28} color={palette.success} /> Chat assistant
        <span style={{ marginLeft: "auto", fontSize: 18, color: palette.success }}>● online</span>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 16, minHeight: 230 }}>
        <div style={{ ...bubble, alignSelf: "flex-end", borderRadius: "22px 22px 6px 22px", background: `${palette.primary}33`, border: `1px solid ${palette.primary}66`, opacity: msg, transform: `translateY(${(1 - msg) * 20}px)` }}>
          Hi! Can I book a call for tomorrow?
        </div>
        {typingOn ? (
          <div style={{ ...bubble, alignSelf: "flex-start", borderRadius: "6px 22px 22px 22px", background: "rgba(255,255,255,0.07)", display: "flex", gap: 8 }}>
            {[0, 1, 2].map((i) => (
              <div key={i} style={{ width: 12, height: 12, borderRadius: 99, background: palette.textMuted, opacity: 0.4 + 0.6 * Math.abs(Math.sin((frame - i * 3) / 4)) }} />
            ))}
          </div>
        ) : null}
        <div style={{ ...bubble, alignSelf: "flex-start", borderRadius: "6px 22px 22px 22px", background: `${palette.success}22`, border: `1px solid ${palette.success}66`, opacity: reply, transform: `translateY(${(1 - reply) * 20}px)` }}>
          Of course! Here are tomorrow's free slots.
          <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 8, fontSize: 18, color: palette.success, fontWeight: 600 }}>
            <Zap size={16} /> Instant auto-reply
          </div>
        </div>
      </div>
    </GlowCard>
  );
};

export const Scene09Automation: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v, vertical } = useLayout();
  const flowAt = cue("automation", 1) - 10;
  const lit = (i: number) => range(frame, flowAt + i * 9, flowAt + i * 9 + 8);
  const moon = pop(frame, fps, cue("automation", 2) - 4, 12);
  return (
    <SceneFrame duration={duration} seed={8} kicker="07  /  AUTOMATION" title="AI AUTOMATION" highlight={["ai"]}>
      <Stage gap={v(80, 60)} top={v(250, 470)}>
        <Chat w={v(720, 860)} />
        <div style={{ position: "relative", display: "flex", flexDirection: "column", alignItems: "center", gap: 30 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 0, flexWrap: vertical ? "wrap" : "nowrap", justifyContent: "center" }}>
            {STEPS.map((s, i) => (
              <React.Fragment key={s.label}>
                <IconBadge icon={s.icon} label={s.label} color={s.color} size={v(124, 124)} delay={flowAt - 16 + i * 3} active={lit(i)} />
                {i < STEPS.length - 1 ? (
                  <div style={{ width: v(54, 40), height: 4, margin: "0 8px 44px", borderRadius: 4, background: "rgba(255,255,255,0.12)", overflow: "hidden" }}>
                    <div style={{ width: `${lit(i + 1) * 100}%`, height: "100%", background: `linear-gradient(90deg, ${s.color}, ${STEPS[i + 1].color})`, boxShadow: `0 0 10px ${s.color}` }} />
                  </div>
                ) : null}
              </React.Fragment>
            ))}
          </div>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: 14,
              padding: "12px 26px",
              borderRadius: 99,
              background: "rgba(10,14,26,0.9)",
              border: `1.5px solid ${palette.highlight}88`,
              fontFamily: fonts.heading,
              fontWeight: 700,
              fontSize: 34,
              color: palette.text,
              opacity: moon,
              transform: `scale(${0.8 + 0.2 * moon})`,
            }}
          >
            <Moon size={30} color={palette.highlight} fill={palette.highlight} /> 24/7
          </div>
        </div>
      </Stage>
    </SceneFrame>
  );
};
