import React from "react";
import { AbsoluteFill, Html5Audio, Sequence, interpolate, staticFile, useCurrentFrame } from "remotion";
import { TransitionSeries, linearTiming, type TransitionPresentation } from "@remotion/transitions";
import { fade } from "@remotion/transitions/fade";
import { slide } from "@remotion/transitions/slide";
import { wipe } from "@remotion/transitions/wipe";
import { clockWipe } from "@remotion/transitions/clock-wipe";
import { flip } from "@remotion/transitions/flip";
import { z } from "zod";
import { CaptionBar } from "./components/CaptionBar";
import { SCENES, TRANSITIONS, TOTAL_FRAMES, type SceneId, type TransitionKind } from "./timing";
import {
  MUSIC_FILE,
  MUSIC_DUCKED_VOLUME,
  MUSIC_VOLUME,
  SFX_VOLUME,
  SFX_WHOOSH,
  VO_VOLUME,
  palette,
} from "./theme";
import { useLayout } from "./layout";
import type { SceneProps } from "./scenes/types";
import { Scene01Hook } from "./scenes/Scene01Hook";
import { Scene02Brand } from "./scenes/Scene02Brand";
import { Scene03Seo } from "./scenes/Scene03Seo";
import { Scene04Web } from "./scenes/Scene04Web";
import { Scene05Marketing } from "./scenes/Scene05Marketing";
import { Scene06Ads } from "./scenes/Scene06Ads";
import { Scene07Branding } from "./scenes/Scene07Branding";
import { Scene08Video } from "./scenes/Scene08Video";
import { Scene09Automation } from "./scenes/Scene09Automation";
import { Scene10Cta } from "./scenes/Scene10Cta";
import voiceover from "./content/voiceover.json";

export const adSchema = z.object({
  showCaptions: z.boolean(),
});

const SCENE_COMPONENTS: Record<SceneId, React.FC<SceneProps>> = {
  hook: Scene01Hook,
  brand: Scene02Brand,
  seo: Scene03Seo,
  web: Scene04Web,
  marketing: Scene05Marketing,
  ads: Scene06Ads,
  branding: Scene07Branding,
  video: Scene08Video,
  automation: Scene09Automation,
  cta: Scene10Cta,
};

const presentation = (kind: TransitionKind, w: number, h: number): TransitionPresentation<any> => {
  switch (kind) {
    case "fade":
      return fade();
    case "slide-left":
      return slide({ direction: "from-right" });
    case "slide-up":
      return slide({ direction: "from-bottom" });
    case "wipe-right":
      return wipe({ direction: "from-left" });
    case "wipe-up":
      return wipe({ direction: "from-bottom" });
    case "clock":
      return clockWipe({ width: w, height: h });
    case "flip":
      return flip({ direction: "from-right", perspective: 2400 });
  }
};

/** Voiceover intervals, used to duck the music under speech. */
const VO_RANGES = SCENES.map((s) => [s.voStart, s.voEnd] as const);

const musicVolume = (f: number) => {
  // smooth 6-frame duck around each voiceover line
  const ducked = VO_RANGES.reduce((acc, [a, b]) => {
    const d = Math.min(
      interpolate(f, [a - 6, a], [0, 1], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
      interpolate(f, [b, b + 8], [1, 0], { extrapolateLeft: "clamp", extrapolateRight: "clamp" }),
    );
    return Math.max(acc, d);
  }, 0);
  const base = MUSIC_VOLUME + (MUSIC_DUCKED_VOLUME - MUSIC_VOLUME) * ducked;
  const fadeIn = interpolate(f, [0, 12], [0, 1], { extrapolateRight: "clamp" });
  // fade the music out over the final 20 frames
  const fadeOut = interpolate(f, [TOTAL_FRAMES - 20, TOTAL_FRAMES], [1, 0], {
    extrapolateLeft: "clamp",
    extrapolateRight: "clamp",
  });
  return base * fadeIn * fadeOut;
};

export const LeadingLinkAd: React.FC<z.infer<typeof adSchema>> = ({ showCaptions }) => {
  const { width, height } = useLayout();
  useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: palette.bgDark }}>
      {/*
        Each scene is mounted for slot + outgoing transition frames. Because a
        transition overlaps the end of one scene with the start of the next,
        scene i+1 still starts exactly at SCENES[i+1].start and the whole
        series lasts TOTAL_FRAMES.
      */}
      <TransitionSeries>
        {SCENES.flatMap((s, i) => {
          const Comp = SCENE_COMPONENTS[s.id];
          const duration = s.slot + s.outFrames;
          const items = [
            <TransitionSeries.Sequence key={s.id} durationInFrames={duration} name={s.id}>
              <Comp slot={s.slot} duration={duration} />
            </TransitionSeries.Sequence>,
          ];
          const t = TRANSITIONS[i];
          if (t && i < SCENES.length - 1) {
            items.push(
              <TransitionSeries.Transition
                key={`${s.id}-t`}
                presentation={presentation(t.kind, width, height)}
                timing={linearTiming({ durationInFrames: t.frames })}
              />,
            );
          }
          return items;
        })}
      </TransitionSeries>

      {/* Voiceover: one file per scene, placed at the scene's cue. */}
      {SCENES.map((s, i) => (
        <Sequence key={`vo-${s.id}`} from={s.voStart} name={`VO ${s.id}`} layout="none">
          <Html5Audio
            src={staticFile(`audio/vo/${String(i + 1).padStart(2, "0")}-${voiceover.lines[i].id}.mp3`)}
            volume={VO_VOLUME}
          />
        </Sequence>
      ))}

      {/* Whoosh on each scene change, starting just before the cut. */}
      {SCENES.slice(1).map((s) => (
        <Sequence key={`sfx-${s.id}`} from={s.start - 6} durationInFrames={24} name="whoosh" layout="none">
          <Html5Audio src={SFX_WHOOSH} volume={SFX_VOLUME} />
        </Sequence>
      ))}

      <Html5Audio src={MUSIC_FILE} volume={musicVolume} />

      {showCaptions ? <CaptionBar /> : null}
    </AbsoluteFill>
  );
};
