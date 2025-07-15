export interface CustomCameraPlugin {
  startCamera(): Promise<void>;
  startRecording(): Promise<void>;
  takePictureWithEffect(): Promise<void>;
  stopRecording(): Promise<{ videoPath: string, images: string[] }>;
}
