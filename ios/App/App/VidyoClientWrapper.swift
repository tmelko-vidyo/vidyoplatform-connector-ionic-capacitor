import Foundation
import UIKit

struct CallState {
    var hasDevicesSelected = true
    var cameraMuted = false
    
    var connected = false
}

struct ConnectOptions {
    
    let portal: String
    let roomKey: String
    let pin: String
    let name: String
    
    let maxParticipants: Int
    let logLevel: String
    let debug: Bool
}

class VidyoClientWrapper : NSObject {
    
    var connector: VCConnector?
    var videoView: UIView?
    
    var options: ConnectOptions!
    
    var callState = CallState()
    
    weak var delegate: IPluginEventHandler?
    
    init(view: inout UIView, delegate: IPluginEventHandler, options: ConnectOptions) {
        super.init()
        
        self.videoView = view
        self.delegate = delegate
        self.options = options
        
        connector = VCConnector(&view,
                                viewStyle: .default,
                                remoteParticipants: UInt32(options.maxParticipants),
                                logFileFilter: options.logLevel.cString(using: .utf8),
                                logFileName: "".cString(using: .utf8),
                                userData: 0)
        
        // Orientation change observer
        NotificationCenter.default.addObserver(self, selector: #selector(onOrientationChanged),
                                               name: UIDevice.orientationDidChangeNotification, object: nil)
        
        // Foreground mode observer
        NotificationCenter.default.addObserver(self, selector: #selector(onForeground),
                                               name: UIApplication.didBecomeActiveNotification, object: nil)
        
        // Background mode observer
        NotificationCenter.default.addObserver(self, selector: #selector(onBackground),
                                               name: UIApplication.willResignActiveNotification, object: nil)
        
        if options.debug {
            self.connector?.registerLogEventListener(self, filter: options.logLevel)
        }
        
        self.connector?.registerParticipantEventListener(self)
        self.connector?.reportLocalParticipant(onJoined: true)
        
        self.refreshUI()
        
        self.delegate?.onInitialized(status: true)
    }
    
    @objc func onForeground() {
        guard let connector = connector else {
            return
        }
        
        if !callState.hasDevicesSelected {
            callState.hasDevicesSelected = true
            
            connector.selectDefaultCamera()
            connector.selectDefaultMicrophone()
            connector.selectDefaultSpeaker()
        }
        
        connector.setMode(.foreground)
        connector.setCameraPrivacy(callState.cameraMuted)
    }
    
    @objc func onBackground() {
        guard let connector = connector else {
            return
        }
        
        if isInCallingState() {
            connector.setCameraPrivacy(true)
        } else {
            callState.hasDevicesSelected = false
            
            connector.select(nil as VCLocalCamera?)
            connector.select(nil as VCLocalMicrophone?)
            connector.select(nil as VCLocalSpeaker?)
        }
        
        connector.setMode(.background)
    }
    
    @objc func onOrientationChanged() {
        self.refreshUI();
    }
}

// MARK: Public API

extension VidyoClientWrapper {
    
    func connectOrDisconnect(state: Bool) -> Bool {
        if (state) {
            print("Start connection: \(options.portal):\(options.roomKey) with pin '\(options.pin)' and name: \(options.name)");
            
            return self.connector?.connectToRoom(asGuest: options.portal,
                                                 displayName: options.name,
                                                 roomKey: options.roomKey,
                                                 roomPin: options.pin,
                                                 connectorIConnect: self) ?? false
        } else {
            return self.connector?.disconnect() ?? false
        }
    }
    
    func setCameraPrivacy(privacy: Bool) {
        self.connector?.setCameraPrivacy(privacy)
        self.callState.cameraMuted = privacy
    }
    
    func setMicrophonePrivacy(privacy: Bool) {
        self.connector?.setMicrophonePrivacy(privacy)
    }
    
    func cycleCamera() {
        self.connector?.cycleCamera()
    }
    
    func destroy() {
        self.delegate = nil
        
        connector?.select(nil as VCLocalCamera?)
        connector?.select(nil as VCLocalMicrophone?)
        connector?.select(nil as VCLocalSpeaker?)
        
        connector?.unregisterLogEventListener()
        connector?.unregisterParticipantEventListener()
        
        connector?.hideView(&videoView)
        connector?.disable()
        
        connector = nil
        
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: IConnect Interface

extension VidyoClientWrapper: VCConnectorIConnect {
    
    func onSuccess() {
        print("Connection Successful.")
        
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else {
                print("Error: Can't maintain self reference.")
                return
            }
            
            this.delegate?.onConnected()
        }
    }
    
    func onFailure(_ reason: VCConnectorFailReason) {
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else {
                print("Error: Can't maintain self reference.")
                return
            }
            
            this.delegate?.onFailure(reason: "\(reason)")
            this.callState.connected = false
        }
    }
    
    func onDisconnected(_ reason: VCConnectorDisconnectReason) {
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else {
                print("Error: Can't maintain self reference.")
                return
            }
            
            this.delegate?.onDisconnected(reason: "\(reason)")
            this.callState.connected = false
        }
    }
}

// MARK: Participant Events Callback

extension VidyoClientWrapper: VCConnectorIRegisterParticipantEventListener {
    
    func onParticipantJoined(_ participant: VCParticipant!) {
        self.delegate?.onParticipantJoined(participant: participant)
    }
    
    func onParticipantLeft(_ participant: VCParticipant!) {
        self.delegate?.onParticipantLeft(participant: participant)
    }
    
    func onDynamicParticipantChanged(_ participants: NSMutableArray!) {}
    
    func onLoudestParticipantChanged(_ participant: VCParticipant!, audioOnly: Bool) {}
}

// MARK: Private API

extension VidyoClientWrapper {
    
    private func refreshUI() {
        DispatchQueue.main.async {
            [weak self] in
            guard let this = self else { return }
            
            let width = UInt32(this.videoView?.frame.size.width ?? 0)
            let height = UInt32(this.videoView?.frame.size.height ?? 0)
            print("ShowViewAt wxh: \(width)x\(height)")
            
            this.connector?.showView(at: &this.videoView,
                                     x: 0,
                                     y: 0,
                                     width: width,
                                     height: height)
        }
    }
    
    private func isInCallingState() -> Bool {
        if let connector = connector {
            let state = connector.getState()
            return state != .idle && state != .ready
        }
        
        return false
    }
}

// MARK: Log Event Listener - Required for Debug Console logging

extension VidyoClientWrapper: VCConnectorIRegisterLogEventListener {
    
    func onLog(_ logRecord: VCLogRecord!) {}
}
