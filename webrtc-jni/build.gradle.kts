import io.github.tomaki19.gradle.cmake.extension.api.CMakeToolchain
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.cmake)
    id("base")
}

// Read the target platform passed from GitHub Actions (e.g., -Pplatform=linux_arm64)
val targetPlatform: String = (project.findProperty("platform") as? String) ?: run {
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch").lowercase().trim()

    val osName = when {
        os.isLinux -> "linux"
        os.isMacOsX -> "macos"
        os.isWindows -> "windows"
        else -> error("Unsupported OS: ${os.name}")
    }

    val archName = when {
        arch == "amd64" || arch == "x86_64" -> {
            if (os.isWindows) "x86_64" else "x86-64"
        }
        arch == "aarch64" || arch == "arm64" -> "arm64"
        arch.startsWith("arm") -> "arm"
        else -> error("Unsupported Architecture: $arch")
    }

    "${osName}_${archName}"
}

logger.lifecycle("Building webrtc-jni for platform: $targetPlatform")

cmake {
    toolchains {
        when (targetPlatform) {
            "linux_x86-64" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.LINUX)
                    architecture.set("x86_64")
                    toolchainFile.set(file("src/main/cpp/toolchain/x86_64-linux-clang.cmake"))
                }
            }
            "linux_arm64" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.LINUX)
                    architecture.set("aarch64")
                    toolchainFile.set(file("src/main/cpp/toolchain/aarch64-linux-clang.cmake"))
                }
            }
            "linux_arm" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.LINUX)
                    architecture.set("arm")
                    toolchainFile.set(file("src/main/cpp/toolchain/aarch32-linux-clang.cmake"))
                }
            }
            "windows_x86_64" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.WINDOWS)
                    architecture.set("x86_64")
                    generator.set("Visual Studio 17 2022")
                    toolchainFile.set(file("src/main/cpp/toolchain/x86_64-windows-clang.cmake"))
                }
            }
            "macos_x86-64" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.MAC_OS)
                    architecture.set("x86_64")
                    toolchainFile.set(file("src/main/cpp/toolchain/x86_64-macos-cross.cmake"))
                }
            }
            "macos_arm64" -> {
                register(targetPlatform) {
                    operatingSystem.set(OperatingSystem.MAC_OS)
                    architecture.set("aarch64")
                    toolchainFile.set(file("src/main/cpp/toolchain/aarch64-macos-clang.cmake"))
                }
            }
            else -> {
                error("Unsupported target platform: $targetPlatform")
            }
        }
    }

    libraries {
        register("webrtc-java") {
            sources.from(fileTree("src/main/cpp") {
                include("**/*.cpp")
                include("**/*.h")
            })
            
            if (targetPlatform != "host") {
                toolchains.add(targetPlatform)
            } else {
                toolchains.add("host")
            }
        }
    }
}

val nativeJar by tasks.registering(Jar::class) {
    val toolchainPart = targetPlatform.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
    
    dependsOn("cmakeAssemble${toolchainPart}Release")

    archiveBaseName.set("webrtc-java")
    
    val classifier = targetPlatform.replace("_", "-")
    archiveClassifier.set(classifier)

    from(layout.buildDirectory.dir("cmake/$targetPlatform/Release")) {
        include("**/*.so", "**/*.dll", "**/*.dylib")
    }
    
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

artifacts {
    add("archives", nativeJar)
    add("default", nativeJar)
}

tasks.named("assemble") {
    dependsOn(nativeJar)
}
