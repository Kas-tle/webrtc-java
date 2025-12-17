pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "webrtc-java-parent"

include("webrtc-jni")
include("webrtc")
