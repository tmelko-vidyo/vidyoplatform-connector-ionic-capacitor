//
//  IPluginEventHandler.swift
//  App
//
//  Created by taras.melko on 08.06.2021.
//

import Foundation

protocol IPluginEventHandler: class {

    func onInitialized(status: Bool)

    func onConnected()

    func onDisconnected(reason: String)

    func onFailure(reason: String)

    func onParticipantJoined(participant: VCParticipant)

    func onParticipantLeft(participant: VCParticipant)
}
