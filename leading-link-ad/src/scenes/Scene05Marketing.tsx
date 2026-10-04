import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { Share2, Mail, Star, MessageCircle } from "lucide-react";
import { SceneFrame } from "../components/SceneFrame";
import { IconBadge } from "../components/IconBadge";
import { BrandSwirl } from "../components/BrandLogo";
import { pop, range, clamp } from "../components/anim";
import { fonts, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

const ITEMS = [
  { icon: Share2, label: "Social", color: palette.primary },
  { icon: Mail, label: "Email", color: palette.accent },
  { icon: Star, label: "Influencers", color: palette.highlight },
  { icon: MessageCircle, label: "WhatsApp", color: palette.success },
];

export const Scene05Marketing: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v, height, width } = useLayout();
  const connectAt = cue("marketing", 1) - 6; // "all working together as one"
  // orbit speed eases to a stop when the system connects
  const settle = range(frame, connectAt - 10, connectAt + 14);
  const angleOffset = interpolate(frame, [0, connectAt + 14], [-1.4, 0], { ...clamp, easing: (t) => 1 - (1 - t) ** 2 });
  const R = v(330, 330);
  const RY = v(250, 360);
  const cx = width / 2;
  const cy = v(height / 2 + 70, height / 2 + 40);
  const pos = ITEMS.map((_, i) => {
    const a = -Math.PI / 2 + (i / ITEMS.length) * Math.PI * 2 + Math.PI / 4 + angleOffset;
    return { x: cx + Math.cos(a) * R * (0.85 + 0.15 * settle), y: cy + Math.sin(a) * RY * (0.85 + 0.15 * settle) };
  });
  const lines = range(frame, connectAt, connectAt + 18);
  const ring = range(frame, connectAt + 10, connectAt + 30);
  const center = pop(frame, fps, 4, 12);
  const label = pop(frame, fps, connectAt + 18, 14);
  const pulse = 1 + 0.06 * Math.sin(frame / 6) * lines;
  return (
    <SceneFrame duration={duration} seed={4} kicker="03  /  MARKETING" title="DIGITAL MARKETING" highlight={["marketing"]}>
      <svg width={width} height={height} style={{ position: "absolute", inset: 0 }}>
        <defs>
          <linearGradient id="mk" x1="0" x2="1">
            <stop offset="0" stopColor={palette.accent} />
            <stop offset="1" stopColor={palette.secondary} />
          </linearGradient>
        </defs>
        {/* orbit path */}
        <ellipse cx={cx} cy={cy} rx={R} ry={RY} fill="none" stroke="rgba(255,255,255,0.12)" strokeDasharray="6 10" strokeWidth={2} />
        {/* outer ring joining the channels into one system */}
        <ellipse cx={cx} cy={cy} rx={R} ry={RY} fill="none" stroke="url(#mk)" strokeWidth={4} pathLength={1} strokeDasharray={1} strokeDashoffset={1 - ring} style={{ filter: `drop-shadow(0 0 10px ${palette.accent})` }} />
        {pos.map((p, i) => (
          <g key={i}>
            <line x1={cx} y1={cy} x2={cx + (p.x - cx) * lines} y2={cy + (p.y - cy) * lines} stroke="url(#mk)" strokeWidth={4} strokeLinecap="round" />
            {/* data pulse travelling along each spoke */}
            {lines >= 1 ? (
              <circle cx={cx + (p.x - cx) * (((frame + i * 9) % 24) / 24)} cy={cy + (p.y - cy) * (((frame + i * 9) % 24) / 24)} r={7} fill="#fff" style={{ filter: `drop-shadow(0 0 8px ${palette.accent})` }} />
            ) : null}
          </g>
        ))}
      </svg>
      {/* center hub */}
      <div
        style={{
          position: "absolute",
          left: cx - 90,
          top: cy - 90,
          width: 180,
          height: 180,
          borderRadius: 999,
          background: "rgba(10,14,26,0.9)",
          border: `2px solid ${palette.primary}aa`,
          boxShadow: `0 0 ${40 + 40 * lines}px ${palette.primary}88`,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          transform: `scale(${center * pulse})`,
        }}
      >
        <BrandSwirl size={140} rotate={frame * 2} />
      </div>
      {ITEMS.map((it, i) => (
        <div key={it.label} style={{ position: "absolute", left: pos[i].x, top: pos[i].y, transform: "translate(-50%, -38%)" }}>
          <IconBadge icon={it.icon} label={it.label} color={it.color} size={v(118, 128)} delay={8 + i * 5} active={0.4 + 0.6 * lines} />
        </div>
      ))}
      <div
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          top: cy + RY + v(30, 120),
          display: "flex",
          justifyContent: "center",
          opacity: label,
          transform: `translateY(${(1 - label) * 16}px)`,
        }}
      >
        <div style={{ padding: "10px 26px", borderRadius: 99, background: `${palette.primary}26`, border: `1px solid ${palette.primary}88`, fontFamily: fonts.body, fontWeight: 700, fontSize: 26, color: palette.text, letterSpacing: "0.04em" }}>
          Marketing Automation
        </div>
      </div>
    </SceneFrame>
  );
};
