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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RTCPeerConnectionTests extends TestBase {

	private RTCPeerConnection peerConnection;


	@BeforeEach
	void init() {
		RTCConfiguration config = new RTCConfiguration();
		PeerConnectionObserver observer = candidate -> { };

		peerConnection = factory.createPeerConnection(config, observer);
	}

	@AfterEach
	void dispose() {
		peerConnection.close();
	}

	@Test
	void configuration() {
		RTCIceServer iceServer = new RTCIceServer();
		iceServer.urls.add("stun:stun.l.google.com:19302");

		RTCConfiguration config = new RTCConfiguration();
		config.iceServers.add(iceServer);
		config.bundlePolicy = RTCBundlePolicy.MAX_BUNDLE;
		config.iceTransportPolicy = RTCIceTransportPolicy.RELAY;
		config.rtcpMuxPolicy = RTCRtcpMuxPolicy.NEGOTIATE;

		RTCPeerConnection peerConnection = factory.createPeerConnection(config, candidate -> {});
		RTCConfiguration peerConfig = peerConnection.getConfiguration();

		assertEquals(config.iceServers, peerConfig.iceServers);
		assertEquals(config.bundlePolicy, peerConfig.bundlePolicy);
		assertEquals(config.iceTransportPolicy, peerConfig.iceTransportPolicy);
		assertEquals(config.rtcpMuxPolicy, peerConfig.rtcpMuxPolicy);

		// Set / update configuration.

		assertThrows(NullPointerException.class, () -> {
			peerConnection.setConfiguration(null);
		});

		iceServer.urls.add("stun:stun4.l.google.com:19302");
		config.iceTransportPolicy = RTCIceTransportPolicy.ALL;

		peerConnection.setConfiguration(config);

		peerConfig = peerConnection.getConfiguration();

		assertEquals(config.iceServers, peerConfig.iceServers);
		assertEquals(config.bundlePolicy, peerConfig.bundlePolicy);
		assertEquals(config.iceTransportPolicy, peerConfig.iceTransportPolicy);
		assertEquals(config.rtcpMuxPolicy, peerConfig.rtcpMuxPolicy);

		peerConnection.close();
	}

	@Test
	void createDataChannel() {
		RTCDataChannelInit options = new RTCDataChannelInit();
		options.protocol = "app-protocol";
		options.maxPacketLifeTime = 5000;

		assertThrows(NullPointerException.class, () -> {
			peerConnection.createDataChannel(null, null);
		});

		assertThrows(NullPointerException.class, () -> {
			peerConnection.createDataChannel("dc", null);
		});

		RTCDataChannel channel = peerConnection.createDataChannel("dc", options);

		assertNotNull(channel);

		assertEquals("dc", channel.getLabel());
		assertEquals(RTCDataChannelState.CONNECTING, channel.getState());
		assertEquals(options.id, channel.getId());
		assertEquals(options.maxPacketLifeTime, channel.getMaxPacketLifeTime());
		assertEquals(options.protocol, channel.getProtocol());
	}

	@Test
	void createOfferNullParams() {
		assertThrows(NullPointerException.class, () -> {
			peerConnection.createOffer(null, null);
		});

		assertThrows(NullPointerException.class, () -> {
			peerConnection.createOffer(new RTCOfferOptions(), null);
		});
	}

	@Test
	void createAnswerNullParams() {
		assertThrows(NullPointerException.class, () -> {
			peerConnection.createAnswer(null, null);
		});

		assertThrows(NullPointerException.class, () -> {
			peerConnection.createAnswer(new RTCAnswerOptions(), null);
		});
	}

	@Test
	void offerAnswer() throws Exception {
		TestPeerConnection caller = new TestPeerConnection(factory);
		TestPeerConnection callee = new TestPeerConnection(factory);

		caller.setRemotePeerConnection(callee);
		callee.setRemotePeerConnection(caller);

		RTCPeerConnection callerConnection = caller.getPeerConnection();
		RTCPeerConnection calleeConnection = callee.getPeerConnection();

		RTCSessionDescription offerDesc = caller.createOffer();

		assertNotNull(offerDesc);
		assertNotNull(offerDesc.sdp);
		assertFalse(offerDesc.sdp.isEmpty());
		assertEquals(RTCSdpType.OFFER, offerDesc.sdpType);
		assertEquals(RTCSignalingState.HAVE_LOCAL_OFFER, callerConnection.getSignalingState());
		assertNotNull(callerConnection.getPendingLocalDescription());
		assertNotNull(callerConnection.getLocalDescription());

		callee.setRemoteDescription(offerDesc);

		assertEquals(RTCSignalingState.HAVE_REMOTE_OFFER, calleeConnection.getSignalingState());
		assertNotNull(calleeConnection.getPendingRemoteDescription());
		assertNotNull(calleeConnection.getRemoteDescription());

		RTCSessionDescription answerDesc = callee.createAnswer();

		assertNotNull(answerDesc);
		assertNotNull(answerDesc.sdp);
		assertFalse(answerDesc.sdp.isEmpty());
		assertEquals(RTCSdpType.ANSWER, answerDesc.sdpType);
		assertNull(calleeConnection.getPendingLocalDescription());
		assertNotNull(calleeConnection.getCurrentLocalDescription());
		assertNotNull(calleeConnection.getLocalDescription());
		assertNull(calleeConnection.getPendingRemoteDescription());
		assertNotNull(calleeConnection.getCurrentRemoteDescription());

		caller.setRemoteDescription(answerDesc);

		caller.waitUntilConnected();
		callee.waitUntilConnected();

		assertNull(callerConnection.getPendingLocalDescription());
		assertNotNull(callerConnection.getCurrentLocalDescription());
		assertNull(callerConnection.getPendingRemoteDescription());
		assertNotNull(callerConnection.getCurrentRemoteDescription());
		assertNotNull(callerConnection.getRemoteDescription());

		assertEquals(RTCPeerConnectionState.CONNECTED, callerConnection.getConnectionState());
		assertEquals(RTCSignalingState.STABLE, callerConnection.getSignalingState());
		assertEquals(RTCIceGatheringState.COMPLETE, callerConnection.getIceGatheringState());

		assertEquals(RTCPeerConnectionState.CONNECTED, calleeConnection.getConnectionState());
		assertEquals(RTCSignalingState.STABLE, calleeConnection.getSignalingState());
		assertEquals(RTCIceGatheringState.COMPLETE, calleeConnection.getIceGatheringState());

		Thread.sleep(1000);
	}

	@Test
	void getStats() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<RTCStatsReport> reportRef = new AtomicReference<>();

		peerConnection.getStats(report -> {
			reportRef.set(report);

			latch.countDown();
		});

		latch.await();

		RTCStatsReport statsReport = reportRef.get();

		assertNotNull(statsReport);
		assertNotNull(statsReport.getStats());
		assertFalse(statsReport.getStats().isEmpty());
	}

	@Test
	void statesWhenClosed() {
		RTCConfiguration config = new RTCConfiguration();
		PeerConnectionObserver observer = candidate -> { };

		RTCPeerConnection peerConnection = factory.createPeerConnection(config, observer);

		assertEquals(RTCPeerConnectionState.NEW, peerConnection.getConnectionState());
		assertEquals(RTCSignalingState.STABLE, peerConnection.getSignalingState());
		assertEquals(RTCIceGatheringState.NEW, peerConnection.getIceGatheringState());
		assertEquals(RTCIceConnectionState.NEW, peerConnection.getIceConnectionState());

		peerConnection.close();

		assertEquals(RTCPeerConnectionState.CLOSED, peerConnection.getConnectionState());
		assertEquals(RTCSignalingState.CLOSED, peerConnection.getSignalingState());
		assertEquals(RTCIceGatheringState.NEW, peerConnection.getIceGatheringState());
		assertEquals(RTCIceConnectionState.CLOSED, peerConnection.getIceConnectionState());
	}
}
