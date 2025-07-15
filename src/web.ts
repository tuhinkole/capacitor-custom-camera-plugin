import { WebPlugin } from '@capacitor/core';

import type { CustomCameraPlugin } from './definitions';

export class CustomCameraWeb extends WebPlugin implements CustomCameraPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
