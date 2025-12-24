plugins {
    alias(libs.plugins.nmcp)
    `maven-publish`
    `eclipse`
}

allprojects {
    group = "dev.kastle.webrtc"
    version = rootProject.property("version") as String

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