import * as admin from 'firebase-admin';

admin.initializeApp();

export { triggerSos } from './triggerSos';
export { resolveSos } from './resolveSos';
export { syncMap } from './syncMap';
export { cleanupCheckins } from './cleanupCheckins';
