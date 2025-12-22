plugins {
    alias(libs.plugins.nmcp)
    `maven-publish`
    `eclipse`
}

allprojects {
    group = "dev.onvoid.webrtc"
    version = "0.15.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

nmcpAggregation {
    centralPortal {
        project(":webrtc")
        
        username.set(System.getenv("MAVEN_CENTRAL_USERNAME"))
        password.set(System.getenv("MAVEN_CENTRAL_PASSWORD"))
        
        publishingType.set("AUTOMATIC")
    }
}