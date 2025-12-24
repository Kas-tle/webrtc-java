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

#include "WebRTCContext.h"
#include "api/RTCStats.h"
#include "Exception.h"
#include "JavaClassLoader.h"
#include "JavaError.h"
#include "JavaEnums.h"
#include "JavaFactories.h"
#include "JavaUtils.h"
#include "JNI_WebRTC.h"

#include "api/environment/environment_factory.h"
#include "api/peer_connection_interface.h"
#include "rtc_base/ssl_adapter.h"

namespace jni
{
	WebRTCContext::WebRTCContext(JavaVM * vm) :
		JavaContext(vm),
		webrtcEnv(webrtc::CreateEnvironment())
	{
	}

	void WebRTCContext::initialize(JNIEnv * env)
	{
		if (!webrtc::InitializeSSL()) {
			throw Exception("Initialize SSL failed");
		}
		
		JavaEnums::add<webrtc::LoggingSeverity>(env, PKG_LOG"Logging$Severity");
		JavaEnums::add<webrtc::DataChannelInterface::DataState>(env, PKG"RTCDataChannelState");
		JavaEnums::add<webrtc::DtlsTransportState>(env, PKG"RTCDtlsTransportState");
		JavaEnums::add<webrtc::DtxStatus>(env, PKG"RTCDtxStatus");
		JavaEnums::add<webrtc::PeerConnectionInterface::BundlePolicy>(env, PKG"RTCBundlePolicy");
		JavaEnums::add<webrtc::PeerConnectionInterface::IceConnectionState>(env, PKG"RTCIceConnectionState");
		JavaEnums::add<webrtc::PeerConnectionInterface::IceGatheringState>(env, PKG"RTCIceGatheringState");
		JavaEnums::add<webrtc::PeerConnectionInterface::IceTransportsType>(env, PKG"RTCIceTransportPolicy");
		JavaEnums::add<webrtc::PeerConnectionInterface::PeerConnectionState>(env, PKG"RTCPeerConnectionState");
		JavaEnums::add<webrtc::PeerConnectionInterface::RtcpMuxPolicy>(env, PKG"RTCRtcpMuxPolicy");
		JavaEnums::add<webrtc::PeerConnectionInterface::SignalingState>(env, PKG"RTCSignalingState");
		JavaEnums::add<webrtc::PeerConnectionInterface::TlsCertPolicy>(env, PKG"TlsCertPolicy");
		JavaEnums::add<webrtc::SdpType>(env, PKG"RTCSdpType");
		JavaEnums::add<jni::RTCStats::RTCStatsType>(env, PKG"RTCStatsType");

		JavaFactories::add<webrtc::DataChannelInterface>(env, PKG"RTCDataChannel");
		JavaFactories::add<webrtc::DtlsTransportInterface>(env, PKG"RTCDtlsTransport");
		JavaFactories::add<webrtc::IceTransportInterface>(env, PKG"RTCIceTransport");
		JavaFactories::add<webrtc::PeerConnectionInterface>(env, PKG"RTCPeerConnection");

		initializeClassLoader(env, PKG_INTERNAL"NativeClassLoader");
	}

	void WebRTCContext::initializeClassLoader(JNIEnv* env, const char * loaderName)
	{
		jclass javaClass = FindClass(env, loaderName);

		if (ExceptionCheck(env)) {
			return;
		}

		auto javaGet = GetStaticMethod(env, javaClass, "getClassLoader", "()Ljava/lang/ClassLoader;");

		auto loaderRef = jni::JavaGlobalRef<jobject>(env, env->CallStaticObjectMethod(javaClass, javaGet));

		InitClassLoader(env, loaderRef);
	}

	void WebRTCContext::destroy(JNIEnv * env)
	{
		if (!webrtc::CleanupSSL()) {
			env->Throw(jni::JavaError(env, "Cleanup SSL failed"));
		}
	}
}