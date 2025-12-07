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

#include "JNI_PeerConnectionFactory.h"
#include "api/PeerConnectionObserver.h"
#include "api/RTCConfiguration.h"
#include "JavaError.h"
#include "JavaFactories.h"
#include "JavaNullPointerException.h"
#include "JavaRuntimeException.h"
#include "JavaUtils.h"

#include "api/create_peerconnection_factory.h"
#include "api/environment/environment_factory.h"
#include "api/HeadlessAudioDeviceModule.h"

JNIEXPORT void JNICALL Java_dev_onvoid_webrtc_PeerConnectionFactory_initialize
(JNIEnv * env, jobject caller)
{
	try {
        auto networkThread = webrtc::Thread::CreateWithSocketServer();
        networkThread->SetName("webrtc_jni_network_thread", nullptr);

        auto signalingThread = webrtc::Thread::Create();
        signalingThread->SetName("webrtc_jni_signaling_thread", nullptr);

        auto workerThread = webrtc::Thread::Create();
        workerThread->SetName("webrtc_jni_worker_thread", nullptr);

        if (!networkThread->Start()) {
            throw jni::Exception("Start network thread failed");
        }
        if (!signalingThread->Start()) {
            throw jni::Exception("Start signaling thread failed");
        }
        if (!workerThread->Start()) {
            throw jni::Exception("Start worker thread failed");
        }

        webrtc::EnvironmentFactory env_factory;
        webrtc::Environment rtc_env = env_factory.Create();

        auto adm = jni::HeadlessAudioDeviceModule::Create(rtc_env);

        webrtc::PeerConnectionFactoryDependencies dependencies;
        dependencies.network_thread = networkThread.release();
        dependencies.worker_thread = workerThread.release();
        dependencies.signaling_thread = signalingThread.release();
        dependencies.adm = std::move(adm);

        auto factory = webrtc::CreateModularPeerConnectionFactory(std::move(dependencies));

        if (factory != nullptr) {
            SetHandle(env, caller, factory.release());
            SetHandle(env, caller, "networkThreadHandle", nullptr);
            SetHandle(env, caller, "signalingThreadHandle", nullptr);
            SetHandle(env, caller, "workerThreadHandle", nullptr);
        }
        else {
            throw jni::Exception("Create PeerConnectionFactory failed");
        }
	}
	catch (...) {
		ThrowCxxJavaException(env);
	}
}

JNIEXPORT void JNICALL Java_dev_onvoid_webrtc_PeerConnectionFactory_dispose
(JNIEnv * env, jobject caller)
{
	webrtc::PeerConnectionFactoryInterface * factory = GetHandle<webrtc::PeerConnectionFactoryInterface>(env, caller);
	CHECK_HANDLE(factory);

	webrtc::RefCountReleaseStatus status = factory->Release();

	if (status != webrtc::RefCountReleaseStatus::kDroppedLastRef) {
		env->Throw(jni::JavaError(env, "Native object was not deleted. A reference is still around somewhere."));
	}

	SetHandle<std::nullptr_t>(env, caller, nullptr);
}

JNIEXPORT jobject JNICALL Java_dev_onvoid_webrtc_PeerConnectionFactory_createPeerConnection
(JNIEnv * env, jobject caller, jobject jConfig, jobject jobserver)
{
	if (jConfig == nullptr) {
		env->Throw(jni::JavaNullPointerException(env, "RTCConfiguration is null"));
		return nullptr;
	}
	if (jobserver == nullptr) {
		env->Throw(jni::JavaNullPointerException(env, "PeerConnectionObserver is null"));
		return nullptr;
	}

	webrtc::PeerConnectionFactoryInterface * factory = GetHandle<webrtc::PeerConnectionFactoryInterface>(env, caller);
	CHECK_HANDLEV(factory, nullptr);

	webrtc::PeerConnectionInterface::RTCConfiguration configuration = jni::RTCConfiguration::toNative(env, jni::JavaLocalRef<jobject>(env, jConfig));
	webrtc::PeerConnectionObserver * observer = new jni::PeerConnectionObserver(env, jni::JavaGlobalRef<jobject>(env, jobserver));
	webrtc::PeerConnectionDependencies dependencies(observer);

	auto result = factory->CreatePeerConnectionOrError(configuration, std::move(dependencies));

	if (!result.ok()) {
		env->Throw(jni::JavaRuntimeException(env, "Create PeerConnection failed: %s %s",
			ToString(result.error().type()), result.error().message()));

		return nullptr;
	}

	auto pc = result.MoveValue();

	if (pc != nullptr) {
		auto javaPeerConnection = jni::JavaFactories::create(env, pc.release());

		SetHandle(env, javaPeerConnection.get(), "observerHandle", observer);

		return javaPeerConnection.release();
	}

	return nullptr;
}