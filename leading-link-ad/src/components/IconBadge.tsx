import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import type { LucideIcon } from "lucide-react";
import { pop } from "./anim";
import { fonts, palette } from "../theme";

type Props = {
  icon: LucideIcon;
  size?: number;
  color?: string;
  delay?: number;
  label?: string;
  /** 0..1 extra glow, e.g. when a node "lights up" */
  active?: number;
  style?: React.CSSProperties;
};

/** Rounded icon tile with gradient rim and glow. Icons are generic lucide glyphs. */
export const IconBadge: React.FC<Props> = ({
  icon: Icon,
  size = 120,
  color = palette.primary,
  delay = 0,
  label,
  active = 1,
  style,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const p = pop(frame, fps, delay, 11);
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 14,
        transform: `scale(${p})`,
        opacity: Math.min(1, p * 1.5),
        ...style,
      }}
    >
      <div
        style={{
          width: size,
          height: size,
          borderRadius: size * 0.3,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: `linear-gradient(145deg, ${color}40, rgba(10,14,26,0.85) 70%)`,
          border: `1.5px solid ${color}${active > 0.5 ? "cc" : "55"}`,
          boxShadow: `0 0 ${20 + 40 * active}px ${color}${active > 0.5 ? "77" : "33"}, inset 0 1px 0 rgba(255,255,255,0.15)`,
        }}
      >
        <Icon size={size * 0.46} color={palette.text} strokeWidth={1.8} />
      </div>
      {label ? (
        <div
          style={{
            fontFamily: fonts.body,
            fontWeight: 600,
            fontSize: Math.max(22, size * 0.22),
            color: palette.text,
            whiteSpace: "nowrap",
          }}
        >
          {label}
        </div>
      ) : null}
    </div>
  );
};
