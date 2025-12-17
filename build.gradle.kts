plugins {
    alias(libs.plugins.nmcp)
}

allprojects {
    group = "dev.onvoid.webrtc"
    version = "0.15.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

nmcp {
    publishAggregation {
        project(":webrtc")
        
        username.set(System.getenv("MAVEN_CENTRAL_USERNAME"))
        password.set(System.getenv("MAVEN_CENTRAL_PASSWORD"))
        
        publicationType.set("AUTOMATIC")
    }
}