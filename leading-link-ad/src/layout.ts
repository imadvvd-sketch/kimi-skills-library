import { useVideoConfig } from "remotion";
import { SAFE } from "./theme";

/** Responsive helpers: the same scenes render in 16:9 and 9:16. */
export const useLayout = () => {
  const { width, height } = useVideoConfig();
  const vertical = height > width;
  return {
    vertical,
    width,
    height,
    safe: SAFE,
    /** pick a value per orientation */
    v: <T,>(landscape: T, portrait: T): T => (vertical ? portrait : landscape),
    /** Vertical space reserved at the bottom for captions. */
    captionSpace: vertical ? 330 : 190,
  };
};
