import React from "react";
import { AbsoluteFill } from "remotion";
import type { TransitionPresentation, TransitionPresentationComponentProps } from "@remotion/transitions";

type ZoomProps = { strength?: number };

/**
 * "Punch through" transition: the outgoing scene rushes toward the camera and
 * blurs out while the next one snaps in from slightly zoomed out.
 */
const ZoomThrough: React.FC<TransitionPresentationComponentProps<ZoomProps>> = ({
  children,
  presentationDirection,
  presentationProgress: p,
  passedProps,
}) => {
  const k = passedProps.strength ?? 1;
  const style: React.CSSProperties =
    presentationDirection === "exiting"
      ? {
          transform: `scale(${1 + 0.9 * k * p * p})`,
          filter: `blur(${14 * p}px) brightness(${1 + 0.8 * p})`,
          opacity: 1 - p,
        }
      : {
          transform: `scale(${1.25 - 0.25 * (1 - (1 - p) ** 3)})`,
          filter: `blur(${10 * (1 - p)}px)`,
          opacity: Math.min(1, p * 1.8),
        };
  return <AbsoluteFill style={style}>{children}</AbsoluteFill>;
};

export const zoomThrough = (props: ZoomProps = {}): TransitionPresentation<ZoomProps> => ({
  component: ZoomThrough,
  props,
});
