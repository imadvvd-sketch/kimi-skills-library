import React from "react";
import { Composition } from "remotion";
import { LeadingLinkAd, adSchema } from "./LeadingLinkAd";
import { FPS, TOTAL_FRAMES, WIDTH, HEIGHT } from "./timing";

export const RemotionRoot: React.FC = () => (
  <>
    <Composition
      id="LeadingLinkAd"
      component={LeadingLinkAd}
      schema={adSchema}
      durationInFrames={TOTAL_FRAMES}
      fps={FPS}
      width={WIDTH}
      height={HEIGHT}
      defaultProps={{ showCaptions: false }}
    />
    {/* Same scenes, responsive layout (see src/layout.ts) */}
    <Composition
      id="LeadingLinkAdVertical"
      component={LeadingLinkAd}
      schema={adSchema}
      durationInFrames={TOTAL_FRAMES}
      fps={FPS}
      width={HEIGHT}
      height={WIDTH}
      defaultProps={{ showCaptions: false }}
    />
  </>
);
