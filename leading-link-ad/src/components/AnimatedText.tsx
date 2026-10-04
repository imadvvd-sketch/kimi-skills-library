import React from "react";
import { useCurrentFrame, useVideoConfig } from "remotion";
import { pop } from "./anim";
import { fonts, gradients, palette } from "../theme";

type Props = {
  text: string;
  /** frame (relative to the parent sequence) to start the reveal */
  delay?: number;
  /** frames between words */
  stagger?: number;
  /** words (case-insensitive, punctuation ignored) rendered in the gradient */
  highlight?: string[];
  size?: number;
  weight?: number;
  color?: string;
  align?: "left" | "center";
  font?: "heading" | "body";
  letterSpacing?: string;
  lineHeight?: number;
  style?: React.CSSProperties;
  /** emphasise highlighted words with a short kinetic punch */
  kinetic?: boolean;
};

/** Word-by-word mask reveal: each word slides up out of its own clip box. */
export const AnimatedText: React.FC<Props> = ({
  text,
  delay = 0,
  stagger = 4,
  highlight = [],
  size = 96,
  weight = 700,
  color = palette.text,
  align = "center",
  font = "heading",
  letterSpacing = "-0.02em",
  lineHeight = 1.05,
  style,
  kinetic = true,
}) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const hl = highlight.map((w) => w.toLowerCase());
  const words = text.split(" ");
  return (
    <div
      style={{
        fontFamily: font === "heading" ? fonts.heading : fonts.body,
        fontSize: size,
        fontWeight: weight,
        color,
        letterSpacing,
        lineHeight,
        textAlign: align,
        display: "flex",
        flexWrap: "wrap",
        justifyContent: align === "center" ? "center" : "flex-start",
        columnGap: "0.26em",
        ...style,
      }}
    >
      {words.map((word, i) => {
        const p = pop(frame, fps, delay + i * stagger, 14);
        const isHl = hl.includes(word.toLowerCase().replace(/[^a-z0-9&#]/g, ""));
        // highlighted words get a brief scale punch after they land
        const punch = isHl && kinetic ? 1 + 0.08 * Math.max(0, 1 - Math.abs(frame - (delay + i * stagger + 10)) / 8) : 1;
        return (
          <span
            key={i}
            style={{
              display: "inline-block",
              overflow: "hidden",
              // extra room so descenders/overshoot aren't clipped by the mask
              padding: "0.08em 0.04em 0.12em",
              margin: "-0.08em -0.04em -0.12em",
            }}
          >
            <span
              style={{
                display: "inline-block",
                transform: `translateY(${(1 - p) * 110}%) scale(${punch})`,
                opacity: Math.min(1, p * 1.4),
                ...(isHl
                  ? {
                      backgroundImage: gradients.primaryAccent,
                      WebkitBackgroundClip: "text",
                      backgroundClip: "text",
                      color: "transparent",
                    }
                  : null),
              }}
            >
              {word}
            </span>
          </span>
        );
      })}
    </div>
  );
};
