import { Buffer } from 'buffer/';

import {
  ProcessBackgroundHCEFunc,
} from '@icedevml/react-native-host-card-emulation/js/hceBackground';
import { createNDEFType4TagApp } from './ndefType4TagApp.ts';


export default async function runBackgroundHCETask(processBackgroundHCE: ProcessBackgroundHCEFunc) {
  const [_getNDEFAppState, _resetNDEFAppState, handleCAPDU] = createNDEFType4TagApp()

  processBackgroundHCE(async (event, respondAPDU, taskData) => {
    console.log('background HCE event', event.type, 'handle', taskData.handle);

    switch (event.type) {
      case 'received':
        const capdu = Buffer.from(event.arg!, 'hex');
        await respondAPDU(handleCAPDU(capdu).toString("hex"));
        break;

      case 'readerDeselected':
        break;
    }
  });
}
