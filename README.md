# VidyoPlatform Ionic sample with Capacitor

## How to use

Clone & Prepare & Build Capcitor VidyoPlatform plugin for local linking. Follow the steps below.

All UI is built from Ionic TS/HTML end and placed on top of VidyoClient rendering native View. VidyoPlatfrom Capcitor Plugin provides rendering window (placed beneath WebView) and basic API's like: connect, disconnect, change device privacy and event callbacks. Since it's placed under WebView your content's background above should be transparent:

    ion-content {
       --background: none;
    }

and 

    background-color: transparent;

### Clone 

    $ git clone https://github.com/tmelko-vidyo/vidyoplatform-connector-plugin.git

### Add VidyoClient SDK

1. Download and unzip Android & iOS https://developer.vidyo.io/#/packages
2. Copy /VidyoClient-AndroidSDK/lib/android content to /vidyoplatform-connector-plugin/android/lib/
3. Copy /VidyoClient-iOSSDK/lib/ios/VidyoClientIOS.framework to /vidyoplatform-connector-plugin/ios
4. Build plugin:

       $ cd vidyoplatform-connector-plugin
       $ npm run build

> Note: now VidyoPlatform Capacitor plugin "vidyoplatform-connector-plugin" is prepared.

### Add Android & iOS platform to this project

      $ cd vidyoplatform-connector-ionic-capacitor
      $ ionic capacitor add android
      $ ionic capacitor add ios

> Note: If you configure your Ionic project with Capacitor you can skip this step.

## Link Plugin with the Ionic

Consider, prepared capcitor plugin is located at level up folder. So, let's add it to the project via package manager.

     $ npm install ../vidyoplatform-connector-plugin
     
Now sync up project with linked plugin:

     $ npx cap sync

You'll get plugin listed like: vidyoplatform-connector-plugin@0.0.1

> Note: Probably you'll need to correct VidyoPlatformPlugin import inside home.page.ts:
> *import {
  ConferenceOptions,
  PrivacyOptions,
  VidyoPlatform,
} from '../../../../vidyoplatform-connector-plugin/dist/esm';* since it's linked from plugin local build folder 

## Run Ionic App

     $ ionic capacitor run
     
    

