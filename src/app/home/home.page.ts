import { ChangeDetectorRef, Component } from '@angular/core';
import { WebPlugin } from '@capacitor/core';

import { Platform } from '@ionic/angular';
import {
  ConferenceOptions,
  PrivacyOptions,
  VidyoPlatform,
} from '../../../../vidyoplatform-connector-plugin/dist/esm';

const VidyoPluginListener = VidyoPlatform as unknown as WebPlugin;

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  styleUrls: ['home.page.scss'],
})
export class HomePage {
  connectOptions: ConferenceOptions = {
    portal: 'pls provide your portal',
    roomKey: 'pls provide your room key',
    pin: '',
    name: 'John Doe',
    maxParticipants: 8,
    logLevel: 'debug@VidyoClient debug@VidyoConnector info warning',
  };

  isActive = false;

  isConnected = false;
  isDisconnectBeforeQuit = false;

  isCameraMuted = false;
  isMicMuted = false;

  inProgress = false;
  status = '';

  constructor(
    private platform: Platform,
    private changeDet: ChangeDetectorRef
  ) {}

  ngOnInit() {
    VidyoPluginListener.addListener('VidyoEventCallback', (info: any) => {
      switch (info.type) {
        case 'init':
          console.log('Ionic Layer: Initialized: ' + JSON.stringify(info));
          break;
        case 'connected':
          this.isConnected = true;
          this.status = 'Connected';

          console.log('Ionic Layer: Connected to the conference!');
          break;
        case 'disconnected':
          this.isConnected = false;
          this.status = 'Disconnected';

          console.log('Ionic Layer: Disconnected: ' + JSON.stringify(info));

          if (this.isDisconnectBeforeQuit) {
            this.close();
          }
          break;
        case 'failed':
          this.isConnected = false;
          this.status = 'Failed: ' + info.reason;

          console.log(
            'Ionic Layer: Connection failed: ' + JSON.stringify(info)
          );
          break;

        case 'participant':
          console.log(
            'Ionic Layer: Participant has: ' +
              info.action +
              ' with name ' +
              info.name
          );
          break;
      }

      this.inProgress = false;
      this.changeDet.detectChanges();
    });

    this.platform.backButton.subscribeWithPriority(10, () => {
      console.log('Ionic Layer: Back was called!');
      this.close();
    });
  }

  open() {
    VidyoPlatform.openConference(this.connectOptions);
    this.isActive = true;
    this.changeDet.detectChanges();
  }

  connect() {
    if (this.isConnected) {
      VidyoPlatform.disconnect()
        .then(() => {
          console.log('Ionic Layer: Disconnect started');
          this.inProgress = true;
          this.status = 'Disconnecting';
        })
        .catch((error) => {
          console.log('Ionic Layer: Disconnect rejected Reason: ' + error);
        });
    } else {
      VidyoPlatform.connect()
        .then(() => {
          console.log('Ionic Layer: Connect started');
          this.inProgress = true;
          this.status = 'Connecting';
        })
        .catch((error) => {
          console.log('Ionic Layer: Connect rejected. Reason: ' + error);
        });
    }
  }

  changeCameraPrivacy() {
    this.isCameraMuted = !this.isCameraMuted;

    let options: PrivacyOptions = {
      device: 'camera',
      privacy: this.isCameraMuted,
    };

    VidyoPlatform.setPrivacy(options);
    console.log('Ionic Layer: Camera Privacy: ' + JSON.stringify(options));
  }

  changeMicPrivacy() {
    this.isMicMuted = !this.isMicMuted;

    let options: PrivacyOptions = {
      device: 'microphone',
      privacy: this.isMicMuted,
    };

    VidyoPlatform.setPrivacy(options);
    console.log('Ionic Layer: Mic Privacy: ' + JSON.stringify(options));
  }

  cycleCamera() {
    VidyoPlatform.cycleCamera();
    console.log('Ionic Layer: Cycle camera');
  }

  close() {
    /* Handle disconnect before quit in case of connected state */
    if (this.isConnected) {
      this.isDisconnectBeforeQuit = true;

      this.inProgress = true;
      this.status = 'Disconnecting';

      VidyoPlatform.disconnect();

      console.log(
        'Ionic Layer: Called disconnection in connected state from close'
      );
      return;
    }

    VidyoPlatform.closeConference();
    this.isActive = false;

    console.log('Ionic Layer: Called closeConference');
    this.changeDet.detectChanges();
  }
}
