import { loadFont as loadInter } from "@remotion/google-fonts/Inter";
import { loadFont as loadSpaceGrotesk } from "@remotion/google-fonts/SpaceGrotesk";
import { staticFile } from "remotion";

/**
 * BRAND PALETTE
 *
 * Extraction status: FAILED. theleadinglink.ae (logo SVGs, stylesheets and a
 * Playwright screenshot) is blocked by the network policy of the environment
 * this was built in, so none of the real brand colors could be read.
 * Per the brief, the default fallback palette is used below.
 *
 * | Role             | Hex      | Source                      | Rank (usage) |
 * |------------------|----------|-----------------------------|--------------|
 * | primary          | #3B82F6  | fallback (brief default)    | 1            |
 * | secondary        | #8B5CF6  | fallback (brief default)    | 2            |
 * | accent           | #22D3EE  | fallback (brief default)    | 3            |
 * | highlight        | #F59E0B  | fallback (brief default)    | 5 (sparing)  |
 * | background dark  | #0A0E1A  | fallback (brief default)    | base         |
 * | background light | #131A2E  | derived from background     | 4            |
 * | text             | #F5F7FF  | chosen for AA contrast      | -            |
 *
 * To apply the real brand colors later, change the hex values here; every
 * scene reads from this object.
 */
export const palette = {
  primary: "#3B82F6",
  secondary: "#8B5CF6",
  accent: "#22D3EE",
  highlight: "#F59E0B",
  bgDark: "#0A0E1A",
  bgLight: "#131A2E",
  text: "#F5F7FF",
  textMuted: "#A9B4D0",
  success: "#34D399",
} as const;

export const gradients = {
  primary: `linear-gradient(100deg, ${palette.primary} 0%, ${palette.secondary} 100%)`,
  primaryAccent: `linear-gradient(100deg, ${palette.accent} 0%, ${palette.primary} 45%, ${palette.secondary} 100%)`,
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
  url: "theleadinglink.ae",
};

/** Safe-area margin in px; key content stays inside it. */
export const SAFE = 80;

// ---------------------------------------------------------------------------
// AUDIO
// Swap the music by dropping a file in public/audio/music/ and changing this.
// The bundled bed.mp3 is synthesized by scripts/generate_music.py (license-free).
export const MUSIC_FILE = staticFile("audio/music/bed.mp3");
export const MUSIC_VOLUME = 0.15; // between voiceover lines
export const MUSIC_DUCKED_VOLUME = 0.08; // while the voiceover speaks
export const SFX_WHOOSH = staticFile("audio/sfx/whoosh.mp3");
export const SFX_VOLUME = 0.22;
export const VO_VOLUME = 1;
