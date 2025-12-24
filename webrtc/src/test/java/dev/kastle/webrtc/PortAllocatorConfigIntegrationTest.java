/*
 * Copyright 2025 Alex Andres
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Integration test for PortAllocatorConfig using an actual peer connection.
 * It verifies that ICE candidates are produced according to the allocator flags
 * and that a constrained port range is respected for HOST candidates.
 */
class PortAllocatorConfigIntegrationTest extends TestBase {

	@Test
	void iceCandidatesRespectPortAllocatorConfig() throws Exception {
		// Constrain ephemeral port range and disable TCP candidates.
		int minPort = 50000;
		int maxPort = 50200;

		RTCConfiguration cfg = new RTCConfiguration();
		cfg.portAllocatorConfig.minPort = minPort;
		cfg.portAllocatorConfig.maxPort = maxPort;
		cfg.portAllocatorConfig.setDisableTcp(true);

		// Use defaults for UDP/STUN/Relay; we don't add STUN/TURN, so only HOST candidates are expected.

		RTCConfiguration cfg2 = new RTCConfiguration();
		cfg2.portAllocatorConfig.minPort = minPort;
		cfg2.portAllocatorConfig.maxPort = maxPort;
		cfg2.portAllocatorConfig.setDisableTcp(true);

		AllocPeer caller = new AllocPeer(factory, cfg);
		AllocPeer callee = new AllocPeer(factory, cfg2);

		caller.setRemotePeer(callee);
		callee.setRemotePeer(caller);

		// Create an SDP offer /answer and set descriptions.
		callee.setRemoteDescription(caller.createOffer());
		caller.setRemoteDescription(callee.createAnswer());

		// Wait until ICE gathering is complete. This verifies the allocator works
		// without depending on successful connection establishment which can be flaky in CI/restricted envs.
		assertTrue(caller.awaitGatheringComplete(30, TimeUnit.SECONDS), "Caller gathering timed out");
		assertTrue(callee.awaitGatheringComplete(30, TimeUnit.SECONDS), "Callee gathering timed out");

		// Basic expectations: Some candidates gathered on both sides.
		assertFalse(caller.candidates.isEmpty(), "Caller gathered no ICE candidates");
		assertFalse(callee.candidates.isEmpty(), "Callee gathered no ICE candidates");

		// Check protocol and port range for HOST candidates.
		for (String c : caller.candidates) {
			assertFalse(c.contains(" tcp ") || c.contains(" tcp\n"),
					"TCP candidate appeared despite TCP being disabled: " + c);

			if (c.contains(" typ host")) {
				int port = parsePortFromCandidate(c);
				assertTrue(port >= minPort && port <= maxPort,
						"Host candidate port out of range: " + port + " in " + c);
			}
		}
		for (String c : callee.candidates) {
			assertFalse(c.contains(" tcp ") || c.contains(" tcp\n"),
					"TCP candidate appeared despite TCP being disabled: " + c);

			if (c.contains(" typ host")) {
				int port = parsePortFromCandidate(c);
				assertTrue(port >= minPort && port <= maxPort,
						"Host candidate port out of range: " + port + " in " + c);
			}
		}

		caller.close();
		callee.close();
	}

	private static int parsePortFromCandidate(String sdp) {
		// Typical candidate line:
		// candidate:<foundation> <component> <protocol> <priority> <ip> <port> typ <type> ...
		String[] parts = sdp.trim().split("\\s+");
		if (parts.length >= 6) {
			try {
				return Integer.parseInt(parts[5]);
			}
			catch (NumberFormatException ignored) { }
		}
		fail("Could not parse port from candidate: " + sdp);
		return -1; // unreachable due to fail
	}



	private static class AllocPeer implements PeerConnectionObserver {

		private final RTCPeerConnection pc;
		private RTCPeerConnection remote;
		private final CountDownLatch gatheringComplete = new CountDownLatch(1);
		final List<String> candidates = new ArrayList<>();


		AllocPeer(PeerConnectionFactory factory, RTCConfiguration cfg) {
			pc = factory.createPeerConnection(cfg, this);
			// Create a data channel to ensure ICE/RTP transceivers are set up.
			pc.createDataChannel("dc", new RTCDataChannelInit());
		}

		void setRemotePeer(AllocPeer other) {
			this.remote = other.pc;
		}

		RTCSessionDescription createOffer() throws Exception {
			TestCreateDescObserver create = new TestCreateDescObserver();
			TestSetDescObserver set = new TestSetDescObserver();
			pc.createOffer(new RTCOfferOptions(), create);
			RTCSessionDescription offer = create.get();
			pc.setLocalDescription(offer, set);
			set.get();
			return offer;
		}

		RTCSessionDescription createAnswer() throws Exception {
			TestCreateDescObserver create = new TestCreateDescObserver();
			TestSetDescObserver set = new TestSetDescObserver();
			pc.createAnswer(new RTCAnswerOptions(), create);
			RTCSessionDescription answer = create.get();
			pc.setLocalDescription(answer, set);
			set.get();
			return answer;
		}

		void setRemoteDescription(RTCSessionDescription description) throws Exception {
			TestSetDescObserver set = new TestSetDescObserver();
			pc.setRemoteDescription(description, set);
			set.get();
		}

		boolean awaitGatheringComplete(long timeout, TimeUnit unit) throws InterruptedException {
			return gatheringComplete.await(timeout, unit);
		}

		void close() {
			pc.close();
		}

		@Override
		public void onIceCandidate(RTCIceCandidate candidate) {
			candidates.add(candidate.sdp);
			if (remote != null) {
				remote.addIceCandidate(candidate);
			}
		}

		@Override
		public void onIceGatheringChange(RTCIceGatheringState state) {
			if (state == RTCIceGatheringState.COMPLETE) {
				gatheringComplete.countDown();
			}
		}
	}
}
