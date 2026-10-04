export type SceneProps = {
  /** frames until the next scene starts */
  slot: number;
  /** frames the scene is mounted (slot + outgoing transition overlap) */
  duration: number;
};
