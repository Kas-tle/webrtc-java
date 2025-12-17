plugins {
    `java-library`
    `maven-publish`
    `signing`
}

dependencies {
    api(project(":webrtc-jni"))
    
    testImplementation(libs.bundles.junit)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(project(":webrtc-jni"))
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "webrtc-java"
            from(components["java"])

            pom {
                name.set("webrtc-java")
                description.set("Java native interface implementation based on the free, open WebRTC project.")
                url.set("https://github.com/Kas-tle/webrtc-java")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Kas-tle")
                        organization.set("Kas-tle")
                        organizationUrl.set("https://github.com/Kas-tle")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Kas-tle/webrtc-java.git")
                    url.set("https://github.com/Kas-tle/webrtc-java")
                }
            }

            // These jars are downloaded by the GitHub Actions "Publish" job into a folder
            val nativeLibsDir = project.findProperty("nativeLibsDir") as? String
            if (nativeLibsDir != null) {
                val dir = file(nativeLibsDir)
                if (dir.exists()) {
                    dir.listFiles()?.filter { it.name.endsWith(".jar") && it.name.contains("webrtc-java") }?.forEach { jarFile ->
                        // Don't re-attach the main jar
                        if (!jarFile.name.equals("webrtc-java-${version}.jar") && !jarFile.name.contains("-sources") && !jarFile.name.contains("-javadoc")) {
                            artifact(jarFile) {
                                // Extract classifier from filename: webrtc-java-0.15.0-linux-x86_64.jar -> linux-x86_64
                                val classifierStr = jarFile.name
                                    .replace("webrtc-java-${version}-", "")
                                    .replace(".jar", "")
                                classifier = classifierStr
                            }
                        }
                    }
                }
            }
        }
    }
}

signing {
    if (System.getenv("PGP_SECRET") != null) {
        useInMemoryPgpKeys(System.getenv("PGP_SECRET"), System.getenv("PGP_PASSPHRASE"))
        sign(publishing.publications["maven"])
    }
}
