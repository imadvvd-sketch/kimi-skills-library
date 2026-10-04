import { loadFont as loadInter } from "@remotion/google-fonts/Inter";
import { loadFont as loadSpaceGrotesk } from "@remotion/google-fonts/SpaceGrotesk";
import { staticFile } from "remotion";

/**
 * BRAND PALETTE (from the official logo supplied by the client)
 *
 * The two brand inks were sampled from the logo file (median of the saturated
 * pixels of each ink, see scripts/trace_logo.py). Pixel counts give the rank.
 * The live site could not be fetched from the build environment, so the
 * dark-background tints below are derived from those two inks.
 *
 * | Role             | Hex      | Source                                | Rank |
 * |------------------|----------|---------------------------------------|------|
 * | brand green      | #63AF45  | logo ("Leading", inner swirl)         | 1    |
 * | brand navy       | #2D2F7A  | logo ("The", "Link", outer ring, tag) | 2    |
 * | logo background  | #FAFAFA  | logo file background                  | -    |
 * | primary (glow)   | #5157D6  | brand navy, lifted for dark screens   | -    |
 * | accent           | #8CD66B  | brand green, lifted for dark screens  | -    |
 * | background dark  | #06071C  | brand navy, darkened                  | -    |
 * | background light | #12143D  | brand navy, darkened                  | -    |
 * | text             | #F5F7FF  | near white for contrast               | -    |
 */
export const palette = {
  brandNavy: "#2D2F7A",
  brandGreen: "#63AF45",
  paper: "#FAFAFA",
  primary: "#5157D6",
  secondary: "#63AF45",
  accent: "#8CD66B",
  highlight: "#8CD66B",
  bgDark: "#06071C",
  bgLight: "#12143D",
  text: "#F5F7FF",
  textMuted: "#AEB3DA",
  success: "#63AF45",
} as const;

export const gradients = {
  primary: `linear-gradient(100deg, ${palette.brandNavy} 0%, ${palette.primary} 40%, ${palette.brandGreen} 100%)`,
  primaryAccent: `linear-gradient(100deg, ${palette.accent} 0%, ${palette.brandGreen} 45%, ${palette.primary} 100%)`,
  cta: `linear-gradient(100deg, ${palette.brandGreen} 0%, ${palette.accent} 100%)`,
};

export const glass = {
  background: "rgba(255,255,255,0.055)",
  border: "1px solid rgba(255,255,255,0.14)",
};

const inter = loadInter("normal", {
  weights: ["400", "500", "600", "700"],
  subsets: ["latin"],
});
const grotesk = loadSpaceGrotesk("normal", {
  weights: ["500", "700"],
  subsets: ["latin"],
});

export const fonts = {
  body: inter.fontFamily,
  heading: grotesk.fontFamily,
};

export const brand = {
  name: "The Leading Link",
  wordmark: "THE LEADING LINK",
  tagline: "Performance-led Digital Solutions",
  logoTagline: "Connecting Vision with Action",
  url: "theleadinglink.ae",
};

/** Safe-area margin in px; key content stays inside it. */
export const SAFE = 80;

// ---------------------------------------------------------------------------
// AUDIO
// Swap the music by dropping a file in public/audio/music/ and changing this.
// The bundled bed.mp3 is synthesized by scripts/generate_music.py (license-free).
export const MUSIC_FILE = staticFile("audio/music/bed.mp3");
export const MUSIC_VOLUME = 0.2; // between voiceover lines
export const MUSIC_DUCKED_VOLUME = 0.1; // while the voiceover speaks
export const SFX_WHOOSH = staticFile("audio/sfx/whoosh.mp3");
export const SFX_VOLUME = 0.22;
export const SFX_HIT = staticFile("audio/sfx/hit.mp3");
export const SFX_BOOM = staticFile("audio/sfx/boom.mp3");
export const SFX_RISER = staticFile("audio/sfx/riser.mp3");
export const IMPACT_VOLUME = 0.55;
export const VO_VOLUME = 1;
