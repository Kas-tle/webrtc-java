/*
 * Copyright 2019 Alex Andres
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.kastle.webrtc;

/**
 * RTCPeerConnection callback interface.
 *
 * @author Alex Andres
 */
public interface PeerConnectionObserver {

	/**
	 * The signaling state has changed. This state change is the result of
	 * either setLocalDescription or setRemoteDescription being invoked.
	 *
	 * @param state The new signaling state.
	 */
	default void onSignalingChange(RTCSignalingState state) {
	}

	/**
	 * The RTCPeerConnection connectionState has changed.
	 *
	 * @param state The new connection state.
	 */
	default void onConnectionChange(RTCPeerConnectionState state) {
	}

	/**
	 * The RTCPeerConnection's ICE connection state has changed.
	 *
	 * @param state The new ICE connection state.
	 */
	default void onIceConnectionChange(RTCIceConnectionState state) {
	}

	/**
	 * Called any time the standards-compliant RTCIceConnectionState changes.
	 *
	 * @param state The new ICE connection state.
	 */
	default void onStandardizedIceConnectionChange(RTCIceConnectionState state) {
	}

	/**
	 * The ICE connection is receiving status changes.
	 *
	 * @param receiving True if receiving status changes, false otherwise.
	 */
	default void onIceConnectionReceivingChange(boolean receiving) {
	}

	/**
	 * The RTCPeerConnection's ICE gathering state has changed.
	 *
	 * @param state The new ICE gathering state.
	 */
	default void onIceGatheringChange(RTCIceGatheringState state) {
	}

	/**
	 * A new RTCIceCandidate is made available to the application.
	 *
	 * @param candidate The new ICE candidate.
	 */
	void onIceCandidate(RTCIceCandidate candidate);

	/**
	 * A failure occurred when gathering ICE candidates.
	 *
	 * @param event The ICE gathering error event.
	 */
	default void onIceCandidateError(RTCPeerConnectionIceErrorEvent event) {
	}

	/**
	 * ICE candidates have been removed.
	 *
	 * @param candidates The removed ICE candidates.
	 */
	default void onIceCandidatesRemoved(RTCIceCandidate[] candidates) {
	}

	/**
	 * The remote peer has opened a RTCDataChannel.
	 *
	 * @param dataChannel The opened data channel.
	 */
	default void onDataChannel(RTCDataChannel dataChannel) {
	}

	/**
	 * Triggered to inform the application that session negotiation needs to be
	 * done (i.e. a createOffer call followed by setLocalDescription).
	 */
	default void onRenegotiationNeeded() {
	}

}
