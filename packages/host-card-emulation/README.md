# React Native Package: @icedevml/react-native-host-card-emulation

Host Card Emulation Module for React Native on iOS and Android platforms.

> [!NOTE]
> Pull requests are welcome! Please [report an issue](https://github.com/icedevml/react-native-host-card-emulation/issues) if you encounter any difficulties.

> [!IMPORTANT]  
> As per Apple's policy, **Host Card Emulation feature is only available for users based in the European
> Economic Area (EEA)** with an iPhone running iOS 17.4 or later. Applications require a special HCE entitlement
> in order to use this feature, which is managed by Apple. Read more in the official support article:
> "[HCE-based contactless NFC transactions for apps in the European Economic Area (EEA)](https://developer.apple.com/support/hce-transactions-in-apps/)".
>
> However, you can still use this package without any limitations if you only want to cover for Android implementation.

## Prerequisites
This package was designed for React Native [New Architecture](https://reactnative.dev/architecture/landing-page) only.
Make sure that your project has it enabled. Note that the New Architecture is enabled by default for React Native 0.76+ projects.

### iOS
1. Ensure that you are enrolled as organization (not individual) in Apple Developer Program, and that you are established in the European Economic Area (EEA).
2. Ensure that you meet other [non-technical requirements imposed by Apple](https://developer.apple.com/support/hce-transactions-in-apps/#requirements-and-availability).
3. Find out the Bundle ID of your existing application or [register a new Bundle ID](https://developer.apple.com/account/resources/identifiers/bundleId/add/bundle).
4. [Request the HCE Entitlement](https://developer.apple.com/contact/request/hce-entitlement/) for your application's bundle ID.
5. Wait until the entitlement is approved by Apple.

### Android
1. Ensure that your app's `minSdkVersion` is at least 24 (Android 7.0). This is the floor imposed by React Native's New Architecture; the Host Card Emulation APIs used by this package are available since Android 5.1.
2. Note that HCE availability also depends on the device hardware - always check `isPlatformSupported()` before starting a session.

## Installation
Install this package within your React Native project:

```
yarn add @icedevml/react-native-host-card-emulation
```
or using npm
```
npm install --save @icedevml/react-native-host-card-emulation
```

Afterwards, follow the subsections below for each platform that you need to support.

### iOS
1. Make sure to set up a proper signing team and bundle identifier in "Signing & Capabilities" configuration section of your project.
2. Add Host Card Emulation (HCE) entitlement with desired AIDs. This could be done either through XCode (by clicking "+ Capability") or within the entitlements XML file directly. Example:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>com.apple.developer.nfc.hce</key>
        <true/>
        <key>com.apple.developer.nfc.hce.iso7816.select-identifier-prefixes</key>
        <array>
            <string>F001020304</string>
            <string>F00102030405</string>
            <string>F0010203040506</string>
        </array>
    </dict>
    </plist>
    ```
   Replace `F001020304`, `F00102030405`, `F0010203040506` with the ISO7816 AIDs that you want to support.
3. In the `ios/` app's subdirectory, run `bundle install && bundle exec pod install`.

### Android
1. In `android/app/src/main/AndroidManifest.xml` add:
   ```diff
    <manifest xmlns:android="http://schemas.android.com/apk/res/android">
      ...

   +  <uses-permission android:name="android.permission.NFC" />
   +  <!-- set required="true" if your app would be entirely useless on devices without HCE support -->
   +  <uses-feature android:name="android.hardware.nfc.hce" android:required="false" />

      ...

      <application
        ...
        >

        ...
   
   +    <service
   +      android:name="com.itsecrnd.rtnhceandroid.HCEService"
   +      android:exported="true"
   +      android:enabled="true"
   +      android:permission="android.permission.BIND_NFC_SERVICE">
   +      <intent-filter>
   +        <action android:name="android.nfc.cardemulation.action.HOST_APDU_SERVICE" />
   +        <category android:name="android.intent.category.DEFAULT"/>
   +      </intent-filter>
   +
   +      <meta-data
   +        android:name="android.nfc.cardemulation.host_apdu_service"
   +        android:resource="@xml/aid_list" />
   +    </service>
      </application>
    </manifest>
   ```
2. Create file `android/app/src/main/res/xml/aid_list.xml`:
   ```xml
   <host-apdu-service xmlns:android="http://schemas.android.com/apk/res/android"
     android:description="@string/app_name"
     android:requireDeviceUnlock="false"
     android:requireDeviceScreenOn="true">
     <aid-group android:category="other"
       android:description="@string/app_name">
       <aid-filter android:name="F001020304" />
       <aid-filter android:name="F00102030405" />
       <aid-filter android:name="F0010203040506" />
     </aid-group>
   </host-apdu-service>
   ```
   Replace `F001020304`, `F00102030405`, `F0010203040506` with the ISO7816 AIDs that you want to support.

   Keep `android:requireDeviceUnlock` set to `false` in any scenario, this is the way to avoid having system prompts
   displayed by Android whenever the app is interacted with when the phone is locked. On the JavaScript/TypeScript side,
   you will receive `deviceLocked`/`deviceUnlocked` events that will let you decide whether you want to allow the NFC
   interaction or not.

   For `android:requireDeviceScreenOn` - set it according to your use case.

## API Specification & Demo App

This module provides a uniform low-level HCE API for both mobile platforms.

> [!NOTE]
> Raw native module's API specification is available in [packages/host-card-emulation/js](https://github.com/icedevml/react-native-host-card-emulation/tree/master/packages/host-card-emulation/js) subdirectory.
> Check it out in order to understand what methods you can call against the module and what are the expected parameters/return values.

> [!NOTE]
> See [Source Code for Demo Application: NDEF NFC Type 4 Tag Emulator (for iOS/Android)](https://github.com/icedevml/react-native-host-card-emulation/tree/master/packages/demo-ndef-app) example for more insights about the library's API.

### Quick start guide

1. Subscribe to the event stream. There is a single global event stream, so you can subscribe early and remain subscribed to events for the entire application's lifetime.
   ```typescript
   useEffect(() => {
       const subscription = NativeHCEModule.onEvent(async (event: HCEModuleEvent) => {
           switch (event.type) {
               /* ... implement event handlers here ... */
           }
       });

       // cleanup the event listener when the effect is unmounted
       return () => {
           subscription.remove();
       }
   }, []);
   ```
2. When the user indicates that he/she wants to perform the HCE action, call the following function from the button's onClick routine:
   ```typescript
   await NativeHCEModule.beginSession();
   ```
   This will emit `sessionStarted` event right after the function call, on both iOS and Android platforms. After the session is started, you can decide to start HCE emulation right away in the event handler:
   ```typescript
   // inside NativeHCEModule.onEvent handler's switch
   case 'sessionStarted':
       NativeHCEModule.setSessionAlertMessage('Tap towards the reader');  // only for iOS, no-op in Android
       await NativeHCEModule.startHCE();
       break;
   ```
   Calling `await NativeHCEModule.startHCE()` causes:
   * iOS: Your smartphone will start listening for C-APDUs (Command APDUs originating from reader devices) right after that function is called, which will be additionally indicated by the operating system popping out the NFC scanning user interface prompt.
   * Android: HCE commands will be forwarded to your application from that point on. No specific user interface is displayed (you have to implement it on your own, within your app).
3. Optionally, you can handle `readerDetected` and `readerDeselected` events to enhance user's experience.
   ```typescript
   // inside NativeHCEModule.onEvent handler's switch
   case 'readerDetected':
       NativeHCEModule.setSessionAlertMessage('Reader detected');  // only for iOS, no-op in Android
       break;

    case 'readerDeselected':
       NativeHCEModule.setSessionAlertMessage('Lost reader');  // only for iOS, no-op in Android
       break;
   ```
   For those events, trigger mechanisms are platform dependent:
   * iOS: The `readerDetected` event will be emitted as soon as the NFC reader's field presence is observed. The `readerDeselected` event will be emitted if the reader is physically disconnected or a non-matching AID is selected by the reader.
   * Android: The `readerDetected` event will be emitted as soon as the first matching SELECT AID command is observed. The `readerDeselected` event will be emitted if the reader is physically disconnected or a non-matching AID is selected by the reader.

   The device lock state is reported through the `deviceLocked` and `deviceUnlocked` events, which are the only source of truth about it. One of them is always emitted before the `received` event of the first C-APDU of a session, so the state is known upfront.
   ```typescript
   case 'deviceLocked':
       setDeviceLocked(true);
       break;

   case 'deviceUnlocked':
       setDeviceLocked(false);
       break;
   ```
   For the lock state events, trigger mechanisms are platform dependent:
   * iOS: The event is always `deviceUnlocked` - it's impossible to communicate over HCE while the device is locked, so the state cannot change during a session.
   * Android: The state is re-sampled whenever a C-APDU arrives and, if it differs from the one reported so far, the corresponding event is emitted straight before the `received` event of that C-APDU. C-APDUs are always forwarded - it is up to you to decide how to treat a locked device.
4. Receive incoming C-APDU and respond to it:
   ```typescript
   // inside NativeHCEModule.onEvent handler's switch
   case 'received':
       NativeHCEModule.setSessionAlertMessage('Keep holding the tag');  // only for iOS, no-op on Android

       // decode incoming C-APDU to bytes
       const capdu = Buffer.from(event.arg!, 'hex');
       console.log('Received C-APDU, capdu.toString('hex'));
   
       // for the demo purposes, we always want to respond with [0x0A] + status code 0x9000 (success)
       await NativeHCEModule.respondAPDU(null, Buffer.from([0x0A, 0x90, 0x00]).toString("hex"));
       break;
   ```
   You don't have to respond to the APDU right away from within the event handler, but please remember that the reader might time out if you will be lingering with the response for too long.

### iOS: Acquiring exclusive NFC access

If you need to utilize `NFCPresentmentIntentAssertion` for enhanced user experience, call:
```typescript
await NativeHCEModule.acquireExclusiveNFC();
```
This function will acquire an exclusive NFC access for 15 seconds. No system services or other applications will be able to interfere with NFC during that period. For example, the NFC background tag reading will be disabled so it would not generate any distracting notifications.

While both session and exclusive NFC access are active, you will receive `readerDetected` events even when the HCE emulation is not started at the time (without any explicit UI being displayed by the system). You can then make a call to `startHCE` to initiate the interaction automatically, without needing the user to click anything beforehand.

This function will throw an exception if:
* the presentment intent assertion was already acquired and is still active;
* you are in the cooldown period where you are not allowed to acquire the presentment intent assertion (cooldown is 15 seconds after the previous assertion had expired);
* the feature is not supported or the device is not eligible for whatever reason;

You can check if you are holding an exclusive NFC access by calling:
```typescript
NativeHCEModule.isExclusiveNFC()
```

Whenever you don't need the exclusive NFC access anymore, call the following function to release the `NFCPresentmentIntentAssertion` object:
```typescript
NativeHCEModule.releaseExclusiveNFC();
```

> [!TIP]
> Your exclusive NFC access might time out after exceeding certain allowance period imposed by iOS. Thus, you should periodically poll
> the `NativeHCEModule.isExclusiveNFC()` value to see if it's still granted. You might decide to navigate into a different UI view
> once your assertion has timed out, or indicate that in some different way.

> [!TIP]
> Use the React's [AppState](https://reactnative.dev/docs/appstate) listener to see when your app goes out to background.
> In such event, call `NativeHCEModule.releaseExclusiveNFC()` to explicitly release the exclusive NFC assertion.
>
> You must do so in order to avoid running into ambigous states, since the operating system is not going to enforce your
> exclusive NFC access anyway, as your app has gone to background.
> 
> You can attempt to acquire the exclusive access assertion again once your app has gone back to foreground (active state).

### Android: Handle HCE calls when the app is not running

Exclusively on Android platform it is possible to handle Host Card Emulation even when the app is not running in foreground.
In such case, no user interface will be available, the handler will run in headless mode. This is still extremely useful in certain cases,
for instance - your app may emulate an NDEF tag even when it's not launched on the foreground.

1. Register `handleBackgroundHCECall` headless task in your app's `index.js`:
   ```typescript
   import { BackgroundHCETaskData, createBackgroundHCE } from '@icedevml/react-native-host-card-emulation/js/hceBackground';

   AppRegistry.registerHeadlessTask('handleBackgroundHCECall', () => {
      return async (taskData: BackgroundHCETaskData) => {
         return await runBackgroundHCETask(createBackgroundHCE(taskData));
      }
   });
   ```
   Pass the whole `taskData` object to `createBackgroundHCE()`, so that it could be handed over to your event handler as its third argument. The device lock state is reported to your handler as `deviceLocked` and `deviceUnlocked` events: always before the `received` event of the first C-APDU of a session, and then again straight before the `received` event of any C-APDU during which a change of that state was noticed. C-APDUs are always forwarded - it is up to you to decide how to treat a locked device.
2. Implement your background HCE handler by passing an event callback to `processBackgroundHCE()`:
   ```typescript
   import { Buffer } from 'buffer/';
   import {
     ProcessBackgroundHCEFunc,
   } from '@icedevml/react-native-host-card-emulation/js/hceBackground';

   export default async function runBackgroundHCETask(processBackgroundHCE: ProcessBackgroundHCEFunc) {
      // ... initialize any state keeping variables here ...

      processBackgroundHCE(async (event, respondAPDU, taskData) => {
         switch (event.type) {
            /* ... background HCE event handler here ... */
         }
      });
   }
   ```
3. Whenever you receive `readerDeselected` event, cleanup any side effects (deallocate resources, reset the state back to initial) -- this is obligatory because lack of proper cleanup will result in bugs and unexpected behavior.
   ```typescript
   // inside processBackgroundHCE handler's switch
   case 'readerDeselected':
       // our application was deselected or the reader field was lost
       // perform a cleanup of any resources here
       break;
   ```
4. Implement the actual APDU handler that will process the C-APDU and generate a response.
   ```typescript
   // inside processBackgroundHCE handler's switch
   case 'received':
       // decode incoming C-APDU to bytes
       const capdu = Buffer.from(event.arg!, 'hex');
       console.log('Received C-APDU, capdu.toString('hex'));
      
       // for the demo purposes, we always want to respond with [0x0A] + status code 0x9000 (success)
       await respondAPDU(Buffer.from([0x0A, 0x90, 0x00]).toString("hex"));
       break;
   ```

> [!WARNING]
> Please note that under some circumstances you might find that your `runBackgroundHCETask()` is running in a few instances concurrently.
> An example of such situation could be when you receive a C-APDU and the reader rapidly restarts the NFC field and reselects your application
> before you manage to respond to the original command. Thus, you should not use any global variables to track the state of your app.
>
> The library automatically identifies stale (deselected) background tasks. For such stale tasks, any subsequent calls to `await respondAPDU()`
> will throw an error to prevent communication glitches.

### More resources

* [Module's API specification](https://github.com/icedevml/react-native-host-card-emulation/tree/master/packages/host-card-emulation/js)
* [Source Code for Demo Application: NDEF NFC Type 4 Tag Emulator (for iOS/Android)](https://github.com/icedevml/react-native-host-card-emulation/tree/master/packages/demo-ndef-app)
