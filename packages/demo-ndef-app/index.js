/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';
import { createBackgroundHCE } from '@icedevml/react-native-host-card-emulation/js/hceBackground';
import runBackgroundHCETask from './background';

AppRegistry.registerComponent(appName, () => App);
AppRegistry.registerHeadlessTask('handleBackgroundHCECall', () => {
  return async (taskData) => {
    return await runBackgroundHCETask(createBackgroundHCE(taskData));
  }
});
