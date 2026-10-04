import React from "react";
import { interpolate, useCurrentFrame, Easing } from "remotion";

type Props = {
  from: number;
  to: number;
  start?: number;
  duration?: number;
  prefix?: string;
  suffix?: string;
  style?: React.CSSProperties;
};

/**
 * Decorative ticking number. Values are illustrative only and are never
 * presented as real client results.
 */
export const Counter: React.FC<Props> = ({
  from,
  to,
  start = 0,
  duration = 40,
  prefix = "",
  suffix = "",
  style,
}) => {
  const frame = useCurrentFrame();
  const v = interpolate(frame, [start, start + duration], [from, to], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
    easing: Easing.out(Easing.cubic),
  });
  return (
    <span style={{ fontVariantNumeric: "tabular-nums", ...style }}>
      {prefix}
      {Math.round(v).toLocaleString("en-US")}
      {suffix}
    </span>
  );
};
