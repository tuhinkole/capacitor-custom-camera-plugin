export interface CustomCameraPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
