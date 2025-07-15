import { registerPlugin } from '@capacitor/core';

import type { CustomCameraPlugin } from './definitions';

const CustomCamera = registerPlugin<CustomCameraPlugin>('CustomCamera', {
  web: () => import('./web').then((m) => new m.CustomCameraWeb()),
});

export * from './definitions';
export { CustomCamera };
