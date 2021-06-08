package io.ionic.starter;

import android.Manifest;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Endpoint.Participant;

import io.ionic.starter.utils.Logger;

@CapacitorPlugin(name = "VidyoPlatform",
  permissions = {
    @Permission(
      alias = "camera",
      strings = {Manifest.permission.CAMERA}
    ),
    @Permission(
      alias = "audio",
      strings = {Manifest.permission.RECORD_AUDIO}
    )
  })
public class VidyoPlatformPlugin extends Plugin implements IPluginEventHandler {

  private static final String PLUGIN_EVENT_CALLBACK = "VidyoEventCallback";
  private static final String PLUGIN_EVENT_TYPE = "type";
  private static final String PLUGIN_EVENT_STATUS = "status";
  private static final String PLUGIN_EVENT_REASON = "reason";
  private static final String PLUGIN_EVENT_ACTION = "action";
  private static final String PLUGIN_EVENT_NAME = "name";

  private VideoFragment fragment;

  private final int containerViewId = 0x14;
  private boolean initialized = false;

  @PluginMethod()
  public void openConference(PluginCall call) {
    Logger.i("Plugin: OpenConf call: " + call.getData());

    if (getPermissionState("camera") != PermissionState.GRANTED && getPermissionState("audio") != PermissionState.GRANTED) {
      requestAllPermissions(call, "requiredPermissionsCallback");
      return;
    }

    initializeVidyo(call);
  }

  @PluginMethod()
  public void connect(PluginCall call) {
    try {
      if (fragment.connectOrDisconnect(true))
        call.resolve();
      else
        call.reject("Failed");
    } catch (Exception e) {
      call.reject("Failed to connect");
    }
  }

  @PluginMethod()
  public void setPrivacy(PluginCall call) {
    try {
      String device = call.getString("device");
      Boolean privacy = call.getBoolean("privacy", false);

      switch (device) {
        case "camera":
          fragment.setCameraPrivacy(privacy);
          break;
        case "microphone":
          fragment.setMicrophonePrivacy(privacy);
          break;
      }

      call.resolve();
    } catch (Exception e) {
      call.reject("Failed to set privacy");
    }
  }

  @PluginMethod()
  public void cycleCamera(PluginCall call) {
    try {
      fragment.cycleCamera();
      call.resolve();
    } catch (Exception e) {
      call.reject("Failed to cycle camera");
    }
  }

  @PluginMethod()
  public void disconnect(PluginCall call) {
    try {
      if (fragment.connectOrDisconnect(false))
        call.resolve();
      else
        call.reject("Failed");
    } catch (Exception e) {
      call.reject("Failed to disconnect");
    }
  }

  @PluginMethod()
  public void closeConference(PluginCall call) {
    getActivity().runOnUiThread(() -> {
      FrameLayout containerView = getBridge().getActivity().findViewById(containerViewId);
      if (containerView != null) {
        ((ViewGroup) getBridge().getWebView().getParent()).removeView(containerView);

        if (fragment != null) {
          fragment.registerPluginEventHandler(null);

          FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
          FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
          fragmentTransaction.remove(fragment);
          fragmentTransaction.commit();
          fragment = null;
        }

        call.resolve();
      } else {
        call.reject("Failed to close the conference");
      }
    });
  }

  private void initializeVidyo(PluginCall call) {
    call.resolve();

    if (!this.initialized)
      this.initialized = ConnectorPkg.initialize();

    String portal = call.getString("portal");
    String roomKey = call.getString("roomKey");
    String pin = call.getString("pin");
    String name = call.getString("name");

    Integer maxParticipants = call.getInt("maxParticipants");
    String logLevel = call.getString("logLevel");

    Boolean debug = call.getBoolean("debug");

    fragment = VideoFragment.open(portal, roomKey, pin, name, maxParticipants, logLevel, debug);
    fragment.registerPluginEventHandler(this);

    bridge.getActivity().runOnUiThread(() -> {
      FrameLayout containerView = getBridge().getActivity().findViewById(containerViewId);

      if (containerView == null) {
        containerView = new FrameLayout(getActivity().getApplicationContext());
        containerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        containerView.setId(containerViewId);

        getBridge().getWebView().setBackgroundColor(Color.TRANSPARENT);
        ((ViewGroup) getBridge().getWebView().getParent()).addView(containerView);

        getBridge().getWebView().getParent().bringChildToFront(getBridge().getWebView());

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(containerView.getId(), fragment);
        fragmentTransaction.commit();

        call.resolve();
      } else {
        call.reject("Conference already opened.");
      }
    });
  }

  @PermissionCallback()
  private void requiredPermissionsCallback(PluginCall call) {
    Logger.i("Plugin: Permissions callback: " + call.getCallbackId() + "/ Data: " + call.getData());
    initializeVidyo(call);
  }

  @Override
  public void onInitialized(boolean status) {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "init");
    notifyObj.put(PLUGIN_EVENT_STATUS, status);
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }

  @Override
  public void onConnected() {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "connected");
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }

  @Override
  public void onDisconnected(String reason) {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "disconnected");
    notifyObj.put(PLUGIN_EVENT_REASON, reason);
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }

  @Override
  public void onFailure(String reason) {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "failed");
    notifyObj.put(PLUGIN_EVENT_REASON, reason);
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }

  @Override
  public void onParticipantJoined(Participant participant) {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "participant");
    notifyObj.put(PLUGIN_EVENT_ACTION, "joined");
    notifyObj.put(PLUGIN_EVENT_NAME, participant.getName());
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }

  @Override
  public void onParticipantLeft(Participant participant) {
    JSObject notifyObj = new JSObject();
    notifyObj.put(PLUGIN_EVENT_TYPE, "participant");
    notifyObj.put(PLUGIN_EVENT_ACTION, "left");
    notifyObj.put(PLUGIN_EVENT_NAME, participant.getName());
    notifyListeners(PLUGIN_EVENT_CALLBACK, notifyObj);
  }
}
