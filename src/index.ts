import { registerPlugin } from '@capacitor/core';

import type { CustomCameraPlugin } from './definitions';

const CustomCamera = registerPlugin<CustomCameraPlugin>('CustomCamera')

export * from './definitions';
export { CustomCamera };

