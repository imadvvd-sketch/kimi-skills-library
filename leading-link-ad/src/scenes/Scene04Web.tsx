import React from "react";
import { interpolate, useCurrentFrame, useVideoConfig } from "remotion";
import { ShoppingCart, Lock } from "lucide-react";
import { SceneFrame, Stage } from "../components/SceneFrame";
import { GlowCard } from "../components/GlowCard";
import { Chip } from "../components/Chip";
import { pop, range } from "../components/anim";
import { fonts, gradients, palette } from "../theme";
import { useLayout } from "../layout";
import { cue } from "../timing";
import type { SceneProps } from "./types";

/**
 * A block that first appears as a dashed wireframe, then "styles" itself:
 * `appear` is when the outline pops in, `style` 0..1 blends to the final look.
 */
const Block: React.FC<{
  appear: number;
  style: number;
  h: number | string;
  w?: number | string;
  fill: string;
  radius?: number;
  children?: React.ReactNode;
  flex?: number;
}> = ({ appear, style, h, w = "100%", fill, radius = 14, children, flex }) => (
  <div
    style={{
      position: "relative",
      height: h,
      width: w,
      flex,
      borderRadius: radius,
      border: `2px dashed rgba(169,180,208,${0.6 * (1 - style)})`,
      opacity: appear,
      transform: `scale(${0.9 + 0.1 * appear})`,
      overflow: "hidden",
    }}
  >
    <div style={{ position: "absolute", inset: 0, background: fill, opacity: style }} />
    <div style={{ position: "relative", height: "100%", opacity: style }}>{children}</div>
  </div>
);

const Line: React.FC<{ w: string; h?: number; c?: string }> = ({ w, h = 12, c = "rgba(255,255,255,0.55)" }) => (
  <div style={{ width: w, height: h, borderRadius: 99, background: c }} />
);

export const Scene04Web: React.FC<SceneProps> = ({ duration }) => {
  const frame = useCurrentFrame();
  const { fps } = useVideoConfig();
  const { v } = useLayout();
  const a = (i: number) => pop(frame, fps, 8 + i * 5, 16);
  const styled = range(frame, 42, 62);
  const cartAt = cue("web", 1) + 4;
  const cart = pop(frame, fps, cartAt, 9);
  const toast = pop(frame, fps, cartAt + 8, 13);
  const W = v(1060, 900);
  return (
    <SceneFrame duration={duration} seed={3} kicker="02  /  WEB" title="WEBSITE DEVELOPMENT" highlight={["development"]} titleSize={v(88, 72)}>
      <Stage gap={v(56, 44)} top={v(250, 560)}>
        <div style={{ position: "relative" }}>
          <GlowCard delay={2} width={W} padding={0} radius={24} from="scale" glow={palette.accent}>
            {/* browser chrome */}
            <div style={{ height: 58, display: "flex", alignItems: "center", gap: 10, padding: "0 22px", borderBottom: "1px solid rgba(255,255,255,0.1)" }}>
              {["#F87171", "#FBBF24", "#34D399"].map((c) => (
                <div key={c} style={{ width: 14, height: 14, borderRadius: 99, background: c, opacity: 0.85 }} />
              ))}
              <div style={{ marginLeft: 18, flex: 1, height: 34, borderRadius: 99, background: "rgba(255,255,255,0.07)", display: "flex", alignItems: "center", gap: 8, padding: "0 16px", fontFamily: fonts.body, fontSize: 20, color: palette.textMuted }}>
                <Lock size={16} /> yourbrand.ae
              </div>
              {/* cart icon pops when the order lands */}
              <div style={{ position: "relative", marginLeft: 14, transform: `scale(${1 + 0.35 * Math.max(0, 1 - Math.abs(frame - cartAt - 6) / 8)})` }}>
                <ShoppingCart size={30} color={palette.text} />
                <div
                  style={{
                    position: "absolute",
                    top: -10,
                    right: -12,
                    width: 24,
                    height: 24,
                    borderRadius: 99,
                    background: palette.highlight,
                    color: "#1a1204",
                    fontFamily: fonts.body,
                    fontWeight: 700,
                    fontSize: 15,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    transform: `scale(${cart})`,
                  }}
                >
                  1
                </div>
              </div>
            </div>
            <div style={{ padding: 26, display: "flex", flexDirection: "column", gap: 18 }}>
              {/* nav */}
              <Block appear={a(0)} style={styled} h={44} fill="transparent">
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", height: "100%", padding: "0 6px" }}>
                  <div style={{ width: 120, height: 22, borderRadius: 8, background: gradients.primary }} />
                  <div style={{ display: "flex", gap: 22 }}>
                    <Line w="70px" /> <Line w="70px" /> <Line w="70px" />
                  </div>
                </div>
              </Block>
              {/* hero */}
              <Block appear={a(1)} style={styled} h={v(220, 210)} fill={`linear-gradient(120deg, ${palette.primary}55, ${palette.secondary}66)`}>
                <div style={{ padding: 30, display: "flex", flexDirection: "column", gap: 16 }}>
                  <Line w="58%" h={30} c="#fff" />
                  <Line w="40%" h={16} />
                  <div style={{ marginTop: 10, width: 170, height: 48, borderRadius: 99, background: palette.highlight }} />
                </div>
              </Block>
              {/* products */}
              <div style={{ display: "flex", gap: 18 }}>
                {[0, 1, 2].map((i) => (
                  <Block key={i} appear={a(2 + i)} style={styled} h={v(170, 160)} flex={1} fill="rgba(255,255,255,0.06)">
                    <div style={{ height: "62%", background: `linear-gradient(135deg, ${[palette.accent, palette.primary, palette.secondary][i]}88, transparent)` }} />
                    <div style={{ padding: 12, display: "flex", flexDirection: "column", gap: 8 }}>
                      <Line w="70%" />
                      <Line w="35%" c={palette.highlight} />
                    </div>
                  </Block>
                ))}
              </div>
              <Block appear={a(5)} style={styled} h={30} fill="rgba(255,255,255,0.04)" />
            </div>
          </GlowCard>
          {/* "+1 order" notification */}
          <div
            style={{
              position: "absolute",
              right: v(-70, -20),
              bottom: v(40, 30),
              padding: "18px 26px",
              borderRadius: 20,
              display: "flex",
              alignItems: "center",
              gap: 14,
              background: "rgba(10,14,26,0.92)",
              border: `1.5px solid ${palette.success}99`,
              boxShadow: `0 0 40px ${palette.success}44`,
              fontFamily: fonts.body,
              color: palette.text,
              fontSize: 28,
              fontWeight: 700,
              opacity: toast,
              transform: `translateY(${interpolate(toast, [0, 1], [-30, 0])}px) scale(${0.8 + 0.2 * toast})`,
            }}
          >
            <div style={{ width: 44, height: 44, borderRadius: 14, background: `${palette.success}33`, display: "flex", alignItems: "center", justifyContent: "center" }}>
              <ShoppingCart size={24} color={palette.success} />
            </div>
            +1 order
          </div>
        </div>
        <div style={{ display: "flex", flexDirection: v("column", "row"), gap: 20, alignItems: v("flex-start", "center"), flexWrap: "wrap", justifyContent: "center" }}>
          {["Design", "Hosting", "E-Commerce"].map((c, i) => (
            <Chip key={c} label={c} delay={30 + i * 6} size={v(32, 28)} color={[palette.accent, palette.primary, palette.secondary][i]} />
          ))}
        </div>
      </Stage>
    </SceneFrame>
  );
};
