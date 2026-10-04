// Renders a still at the middle of every scene (both compositions) into ./stills
// Usage: node scripts/render-stills.mjs [LeadingLinkAd|LeadingLinkAdVertical] [--frames=a,b,c]
import { bundle } from "@remotion/bundler";
import { renderStill, selectComposition } from "@remotion/renderer";
import { existsSync, mkdirSync } from "node:fs";
import path from "node:path";

const root = path.resolve(path.dirname(new URL(import.meta.url).pathname), "..");
const LOCAL_SHELL = "/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell";
const browserExecutable = existsSync(LOCAL_SHELL) ? LOCAL_SHELL : null;

const args = process.argv.slice(2);
const ids = args.filter((a) => !a.startsWith("--"));
const framesArg = args.find((a) => a.startsWith("--frames="));
const compositions = ids.length ? ids : ["LeadingLinkAd", "LeadingLinkAdVertical"];

const serveUrl = await bundle({ entryPoint: path.join(root, "src/index.ts") });
mkdirSync(path.join(root, "stills"), { recursive: true });

// Scene midpoints come from the same timing module the video uses
// (compiled on the fly with esbuild, which ships with @remotion/bundler).
const esbuild = await import("esbuild");
const tmp = path.join(root, "node_modules/.cache/timing.mjs");
await esbuild.build({ entryPoints: [path.join(root, "src/timing.ts")], bundle: true, format: "esm", outfile: tmp, logLevel: "silent" });
const { SCENES } = await import(tmp);
const mids = SCENES.map((s) => ({ f: s.start + Math.floor(s.slot / 2), name: `${String(s.index + 1).padStart(2, "0")}-${s.id}` }));
console.table(SCENES.map((s) => ({ id: s.id, start: s.start, slot: s.slot, voStart: s.voStart, voEnd: s.voEnd })));

for (const id of compositions) {
  const composition = await selectComposition({ serveUrl, id, inputProps: {}, browserExecutable });
  const frames = framesArg
    ? framesArg.slice(9).split(",").map(Number).map((f) => ({ f, name: `f${f}` }))
    : mids;
  for (const { f, name } of frames) {
    const output = path.join(root, "stills", `${id === "LeadingLinkAd" ? "h" : "v"}-${name}.jpg`);
    await renderStill({ composition, serveUrl, frame: f, output, browserExecutable, inputProps: {}, imageFormat: "jpeg", jpegQuality: 88 });
    console.log("wrote", path.relative(root, output));
  }
}
