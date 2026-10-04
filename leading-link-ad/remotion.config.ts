import { Config } from "@remotion/cli/config";
import { existsSync } from "node:fs";

Config.setEntryPoint("./src/index.ts");
Config.setVideoImageFormat("jpeg");
Config.setOverwriteOutput(true);
Config.setConcurrency(4);

// Use a locally installed Chromium headless shell when present (CI / sandboxes
// without access to Remotion's browser download). Otherwise Remotion fetches
// its own.
const LOCAL_SHELL =
  "/opt/pw-browsers/chromium_headless_shell-1194/chrome-linux/headless_shell";
if (existsSync(LOCAL_SHELL)) {
  Config.setBrowserExecutable(LOCAL_SHELL);
}
