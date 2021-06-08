package io.ionic.starter;

import com.vidyo.VidyoClient.Endpoint.Participant;

public interface IPluginEventHandler {

    void onInitialized(boolean status);

    void onConnected();

    void onDisconnected(String reason);

    void onFailure(String reason);

    void onParticipantJoined(Participant participant);

    void onParticipantLeft(Participant participant);
}
