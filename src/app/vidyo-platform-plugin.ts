import { registerPlugin } from '@capacitor/core';

export interface VidyoPlatformPlugin {
  openConference(option: ConferenceOptions): Promise<void>;
  closeConference(): Promise<void>;

  connect(): Promise<void>;
  disconnect(): Promise<void>;

  setPrivacy(option: PrivacyOptions): Promise<void>;
  cycleCamera(): Promise<void>;
}

export interface ConferenceOptions {
  portal: string;
  roomKey: string;
  pin: string;
  name: string;

  maxParticipants: number;
  logLevel: string;
  debug: boolean;
}

export interface PrivacyOptions {
  device: string;
  privacy: boolean;
}

const VidyoPlatform = registerPlugin<VidyoPlatformPlugin>('VidyoPlatform');

export default VidyoPlatform;
