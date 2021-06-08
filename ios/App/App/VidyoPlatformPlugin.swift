import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(VidyoPlatformPlugin)
public class VidyoPlatformPlugin: CAPPlugin {
    
    private static let PLUGIN_EVENT_CALLBACK = "VidyoEventCallback";
    
    private static let PLUGIN_EVENT_TYPE = "type";
    private static let PLUGIN_EVENT_STATUS = "status";
    private static let PLUGIN_EVENT_REASON = "reason";
    private static let PLUGIN_EVENT_ACTION = "action";
    private static let PLUGIN_EVENT_NAME = "name";
    
    var initialized = false
    
    var vidyoClientWrapper: VidyoClientWrapper?
    var videoView: UIView?
    
    @objc func openConference(_ call: CAPPluginCall) {
        let portal = call.getString("portal") ?? ""
        let roomKey = call.getString("roomKey") ?? ""
        let pin = call.getString("pin") ?? ""
        let name = call.getString("name") ?? ""
        
        let maxParticipants = call.getInt("maxParticipants") ?? 8
        let logLevel = call.getString("logLevel") ?? "debug@VidyoClient debug@VidyoConnector info warning"
        let debug = call.getBool("debug") ?? false
        
        let options = ConnectOptions(portal: portal, roomKey: roomKey, pin: pin, name: name, maxParticipants: maxParticipants, logLevel: logLevel, debug: debug)
        
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else {
                print("Error: Can't maintain self reference.")
                call.reject("Failed to initialize!")
                return
            }
            
            if !this.initialized {
                this.initialized = VCConnectorPkg.vcInitialize()
            }
            
            this.attachView()
            this.vidyoClientWrapper = VidyoClientWrapper(view: &this.videoView!, delegate: this, options: options)
            
            call.resolve()
        }
    }
    
    @objc func closeConference(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else {
                print("Error: Can't maintain self reference.")
                return
            }
            
            this.vidyoClientWrapper?.destroy()
            this.detachView()
            
            call.resolve()
        }
    }
    
    @objc func connect(_ call: CAPPluginCall) {
        if self.vidyoClientWrapper?.connectOrDisconnect(state: true) ?? false {
            call.resolve()
        } else {
            call.reject("Failed to connect")
        }
    }
    
    @objc func disconnect(_ call: CAPPluginCall) {
        if self.vidyoClientWrapper?.connectOrDisconnect(state: false) ?? false {
            call.resolve()
        } else {
            call.reject("Failed to disconnect")
        }
    }
    
    @objc func setPrivacy(_ call: CAPPluginCall) {
        let device = call.getString("device");
        let privacy = call.getBool("privacy", false);
        
        switch device {
        case "camera":
            self.vidyoClientWrapper?.setCameraPrivacy(privacy: privacy)
            break
        case "microphone":
            self.vidyoClientWrapper?.setMicrophonePrivacy(privacy: privacy)
            break
        default:
            break
        }
        
        call.resolve()
    }
    
    @objc func cycleCamera(_ call: CAPPluginCall) {
        self.vidyoClientWrapper?.cycleCamera()
        call.resolve()
    }
    
    func attachView() {
        let rect = self.webView?.bounds ?? CGRect(x: 0, y: 0, width:  UIScreen.main.bounds.width, height: UIScreen.main.bounds.height)
        print("Video view rect prepared: \(rect)")
        
        self.videoView = UIView(frame: rect)
        self.webView?.isOpaque = false
        self.webView?.backgroundColor = UIColor.clear
        self.webView?.scrollView.backgroundColor = UIColor.clear
        self.webView?.superview?.insertSubview(self.videoView!, belowSubview: self.webView!)
    }
    
    func detachView() {
        self.videoView?.removeFromSuperview()
        self.webView?.isOpaque = true
    }
}

extension VidyoPlatformPlugin: IPluginEventHandler {
    
    func onInitialized(status: Bool) {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "init",
            VidyoPlatformPlugin.PLUGIN_EVENT_STATUS : status
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
    
    func onConnected() {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "connected"
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
    
    func onDisconnected(reason: String) {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "disconnected",
            VidyoPlatformPlugin.PLUGIN_EVENT_REASON : reason
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
    
    func onFailure(reason: String) {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "failed",
            VidyoPlatformPlugin.PLUGIN_EVENT_REASON : reason
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
    
    func onParticipantJoined(participant: VCParticipant) {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "participant",
            VidyoPlatformPlugin.PLUGIN_EVENT_ACTION : "joined",
            VidyoPlatformPlugin.PLUGIN_EVENT_NAME : participant.getName() ?? "noname",
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
    
    func onParticipantLeft(participant: VCParticipant) {
        let data: [String : Any] = [
            VidyoPlatformPlugin.PLUGIN_EVENT_TYPE : "participant",
            VidyoPlatformPlugin.PLUGIN_EVENT_ACTION : "left",
            VidyoPlatformPlugin.PLUGIN_EVENT_NAME : participant.getName() ?? "noname",
        ]
        
        self.notifyListeners(VidyoPlatformPlugin.PLUGIN_EVENT_CALLBACK, data: data)
    }
}
