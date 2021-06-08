package io.ionic.starter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Device.Device;
import com.vidyo.VidyoClient.Device.LocalCamera;
import com.vidyo.VidyoClient.Endpoint.LogRecord;
import com.vidyo.VidyoClient.Endpoint.Participant;

import java.util.ArrayList;

import io.ionic.starter.utils.Logger;

public class VideoFragment extends Fragment implements Connector.IConnect,
  Connector.IRegisterLocalCameraEventListener,
  Connector.IRegisterLogEventListener,
  Connector.IRegisterParticipantEventListener,
  ViewTreeObserver.OnGlobalLayoutListener {

  public static final String PORTAL_KEY = "portal.key";
  public static final String ROOM_KEY = "room.key";
  public static final String PIN_KEY = "pin.key";
  public static final String NAME_KEY = "name.key";

  public static final String MAX_PARTICIPANTS = "max.key";
  public static final String LOG_LEVEL = "level.key";
  public static final String IS_DEBUG = "is.debug.key";

  public static VideoFragment open(String portal, String roomKey, String pin, String name,
                                   int max, String level,
                                   boolean debug) {
    VideoFragment videoFragment = new VideoFragment();

    Bundle arg = new Bundle();

    arg.putString(PORTAL_KEY, portal);
    arg.putString(ROOM_KEY, roomKey);
    arg.putString(PIN_KEY, pin);
    arg.putString(NAME_KEY, name);

    arg.putInt(MAX_PARTICIPANTS, max);
    arg.putString(LOG_LEVEL, level);

    arg.putBoolean(IS_DEBUG, debug);

    videoFragment.setArguments(arg);
    return videoFragment;
  }

  private FrameLayout videoView;

  private Connector connector;

  private IPluginEventHandler pluginEventHandler;

  private boolean isCameraMuted = false;

  public void registerPluginEventHandler(IPluginEventHandler handler) {
    this.pluginEventHandler = handler;
  }

  public boolean connectOrDisconnect(boolean state) {
    if (state) {
      Bundle arg = getArguments();
      if (arg == null)
        throw new IllegalArgumentException("Null arguments passed");

      String portal = arg.getString(PORTAL_KEY);
      String room = arg.getString(ROOM_KEY);
      String pin = arg.getString(PIN_KEY);
      String name = arg.getString(NAME_KEY);

      Logger.i("Start connection: %s, %s, %s, %s", portal, room, pin, name);
      return connector.connectToRoomAsGuest(portal, name, room, pin, this);
    } else {
      if (connector != null)
        return connector.disconnect();
    }

    return false;
  }

  public void setCameraPrivacy(boolean privacy) {
    connector.setCameraPrivacy(privacy);
    this.isCameraMuted = privacy;
  }

  public void setMicrophonePrivacy(boolean privacy) {
    connector.setMicrophonePrivacy(privacy);
  }

  public void cycleCamera() {
    connector.cycleCamera();
  }

  @Override
  public void onStart() {
    super.onStart();
    if (connector != null) {
      connector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);
      connector.setCameraPrivacy(isCameraMuted);
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (connector != null) {
      connector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background);
      connector.setCameraPrivacy(true);
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ConnectorPkg.setApplicationUIContext(getActivity());
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_video_conference, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    boolean status;
    try {
      videoView = view.findViewById(R.id.video_frame);

      Bundle arg = getArguments();
      if (arg == null)
        throw new IllegalArgumentException("Null arguments passed");

      String logLevel = arg.getString(LOG_LEVEL, "debug@VidyoClient debug@VidyoConnector info warning");
      int maxParticipants = arg.getInt(MAX_PARTICIPANTS);

      boolean isDebug = arg.getBoolean(IS_DEBUG, false);

      connector = new Connector(videoView,
        Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
        maxParticipants,
        isDebug ? logLevel : "info@VidyoClient info@VidyoConnector info warning",
        "",
        0);

      Logger.i("Connector instance has been created.");

      connector.registerLocalCameraEventListener(this);
      connector.registerParticipantEventListener(this);
      connector.reportLocalParticipantOnJoined(true);

      if (isDebug)
        connector.registerLogEventListener(this, logLevel);

      /* Await view availability */
      if (videoView != null && videoView.getViewTreeObserver() != null)
        videoView.getViewTreeObserver().addOnGlobalLayoutListener(this);

      status = true;
    } catch (Exception e) {
      e.printStackTrace();
      status = false;
    }

    if (pluginEventHandler != null)
      pluginEventHandler.onInitialized(status);
  }

  @Override
  public void onGlobalLayout() {
    if (videoView == null || connector == null) {
      Logger.e("Failed to update the view");
      return;
    }

    int width = videoView.getWidth();
    int height = videoView.getHeight();

    connector.showViewAt(videoView, 0, 0, width, height);
    Logger.i("Show View at: " + width + ", " + height);
  }

  @Override
  public void onSuccess() {
    if (!isAdded() || getActivity() == null) return;
    getActivity().runOnUiThread(() -> {
      if (pluginEventHandler != null)
        pluginEventHandler.onConnected();
    });
  }

  @Override
  public void onFailure(final Connector.ConnectorFailReason connectorFailReason) {
    if (!isAdded() || getActivity() == null) return;
    getActivity().runOnUiThread(() -> {
      if (pluginEventHandler != null)
        pluginEventHandler.onFailure(connectorFailReason.name());
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    });
  }

  @Override
  public void onDisconnected(Connector.ConnectorDisconnectReason connectorDisconnectReason) {
    if (connector != null) connector.unregisterResourceManagerEventListener();
    if (!isAdded() || getActivity() == null) return;
    getActivity().runOnUiThread(() -> {
      getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      if (pluginEventHandler != null)
        pluginEventHandler.onDisconnected(connectorDisconnectReason.name());
    });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (connector != null) {
      connector.unregisterLocalCameraEventListener();

      connector.hideView(videoView);

      connector.disable();
      connector = null;
    }

    if (videoView != null && videoView.getViewTreeObserver() != null)
      videoView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

    ConnectorPkg.setApplicationUIContext(null);
    Logger.i("Connector instance has been released.");
  }

  @Override
  public void onLocalCameraAdded(LocalCamera localCamera) {
    if (localCamera != null) {
      Logger.i("onLocalCameraAdded: %s", localCamera.name);
    }
  }

  @Override
  public void onLocalCameraSelected(final LocalCamera localCamera) {
    if (localCamera != null) {
      Logger.i("onLocalCameraSelected: %s", localCamera.name);
    }
  }

  @Override
  public void onLocalCameraStateUpdated(LocalCamera localCamera, Device.DeviceState deviceState) {

  }

  @Override
  public void onLocalCameraRemoved(LocalCamera localCamera) {
    if (localCamera != null) {
      Logger.i("onLocalCameraRemoved: %s", localCamera.name);
    }
  }

  @Override
  public void onLog(LogRecord logRecord) {
    /* Write log into a custom file */
  }

  @Override
  public void onParticipantJoined(Participant participant) {
    Logger.i("Participant joined: %s", participant.getUserId());

    if (getActivity() != null)
      getActivity().runOnUiThread(() -> {
        if (pluginEventHandler != null)
          pluginEventHandler.onParticipantJoined(participant);
      });
  }

  @Override
  public void onParticipantLeft(Participant participant) {
    Logger.i("Participant left: %s", participant.getUserId());
    if (getActivity() != null)
      getActivity().runOnUiThread(() -> {
        if (pluginEventHandler != null)
          pluginEventHandler.onParticipantLeft(participant);
      });
  }

  @Override
  public void onDynamicParticipantChanged(ArrayList<Participant> arrayList) {
  }

  @Override
  public void onLoudestParticipantChanged(Participant participant, boolean b) {
  }
}
