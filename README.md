# VidyoPlatform Ionic sample with Capacitor

### Clone 

    $ git clone https://github.com/tmelko-vidyo/vidyoplatform-connector-ionic-capacitor.git

### Add VidyoClient SDK

1. Download and unzip 22.6.1.14 SDK Version: [Android](https://static.vidyo.io/22.6.1.14/package/VidyoClient-AndroidSDK.zip) & [iOS](https://static.vidyo.io/22.6.1.14/package/VidyoClient-iOSSDK.zip)
2. Copy /VidyoClient-AndroidSDK/lib/android content to /vidyoplatform-connector-ionic-capacitor/android/app/lib/
3. Copy /VidyoClient-iOSSDK/lib/ios/VidyoClientIOS.framework to /vidyoplatform-ionic-capacitor/ios/App/


## Build/Run Ionic App

     $ ionic capacitor build
     
That will open Xcode or Android Studio.

### Specifics

All UI is built on Ionic TS/HTML/SCSS end and placed on top of VidyoClient rendering native View. Custom Code provides rendering window (placed beneath WebView) and basic API's like: connect, disconnect, change device privacy and event callbacks. Since it's placed under WebView your content's background above is transparent:

    ion-content {
       --background: none;
    }

and 

    background-color: transparent;
     
and go to src/theme/variables.scss and change .ios body { }:
    
     .ios body {
        --ion-background-color: transparent;
        // remove RGB background
        ...

### API

This Plugin has been placed as "Custom Code" since npm install is not working with iOS 3rd party framework.
https://github.com/tmelko-vidyo/vidyoplatform-connector-plugin
Don't use it. It's just an API reference for in-built plugin following this doc:
https://capacitorjs.com/docs/ios/custom-code
https://capacitorjs.com/docs/android/custom-code
