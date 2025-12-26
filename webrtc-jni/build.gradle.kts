import org.gradle.internal.os.OperatingSystem

plugins {
    `java`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val currentOs = OperatingSystem.current()

var targetPlatform = System.getenv("WEBRTC_PLATFORM") as? String

if (targetPlatform == null) {
    val rawArch = System.getProperty("os.arch").lowercase().trim()

    val osFamily = when {
        currentOs.isLinux -> "linux"
        currentOs.isMacOsX -> "macos"
        currentOs.isWindows -> "windows"
        else -> error("Unsupported OS: ${currentOs.name}")
    }

    val osArch = when {
        rawArch == "amd64" || rawArch == "x86_64" || rawArch == "x86-64" -> "x86_64"
        rawArch == "aarch64" || rawArch == "arm64" -> "aarch64"
        rawArch.startsWith("arm") -> "aarch32"
        else -> error("Unsupported Architecture: $rawArch")
    }

    targetPlatform = "$osFamily-$osArch"
}

logger.lifecycle("Configuring webrtc-jni for Platform: $targetPlatform")

val toolchainFile = file("src/main/cpp/toolchain").resolve(
    when {
        targetPlatform == "linux-x86_64"   -> "x86_64-linux-clang.cmake"
        targetPlatform == "linux-aarch64"  -> "aarch64-linux-clang.cmake"
        targetPlatform == "linux-aarch32"  -> "aarch32-linux-clang.cmake"
        targetPlatform == "windows-x86_64" -> "x86_64-windows-clang.cmake"
        targetPlatform == "macos-x86_64"   -> "x86_64-macos-cross.cmake"
        targetPlatform == "macos-aarch64"  -> "aarch64-macos-clang.cmake"
        else -> "unknown-toolchain.cmake"
    }
)

val cmakeBuildDir = layout.buildDirectory.dir("cmake/$targetPlatform")

val configureNative by tasks.registering(Exec::class) {
    group = "build"
    workingDir = file("src/main/cpp")
    
    doFirst {
        cmakeBuildDir.get().asFile.mkdirs()
    }

    commandLine("cmake")
    args("-S", ".", "-B", cmakeBuildDir.get().asFile.absolutePath)
    args("-DCMAKE_BUILD_TYPE=Release")

    if (toolchainFile.exists()) {
        logger.lifecycle("Using Toolchain file: ${toolchainFile.absolutePath}")
        args("-DWEBRTC_TOOLCHAIN_FILE=${toolchainFile.absolutePath}")
    } else {
        logger.warn("Toolchain file not found for platform $targetPlatform: ${toolchainFile.absolutePath}")
    }
    
    val webrtcBranch = project.property("webrtc.branch") as String? ?: "master"
    logger.lifecycle("Using WebRTC Branch: $webrtcBranch")

    args("-DWEBRTC_BRANCH=$webrtcBranch")
    args("-DOUTPUT_NAME_SUFFIX=$targetPlatform")
    args("-DCMAKE_EXPORT_COMPILE_COMMANDS=1")
}

val buildNative by tasks.registering(Exec::class) {
    group = "build"
    dependsOn(configureNative)
    
    commandLine("cmake")
    args("--build", cmakeBuildDir.get().asFile.absolutePath)
    args("--config", "Release")
    
    if (!currentOs.isWindows) {
        args("-j", Runtime.getRuntime().availableProcessors())
    }
}

val copyNativeLibs by tasks.registering(Copy::class) {
    dependsOn(buildNative)
    
    from(fileTree(cmakeBuildDir).matching {
        include("**/*.so", "**/*.dll", "**/*.dylib")
        exclude("**/*.lib", "**/*.exp", "**/obj/**", "**/CMakeFiles/**")
    })
    
    into(layout.buildDirectory.dir("resources/main"))
    
    rename { filename ->
        if (filename.contains("webrtc-java")) {
            val ext = if (filename.endsWith(".dll")) "dll" else if (filename.endsWith(".dylib")) "dylib" else "so"
            val prefix = if (ext == "dll") "" else "lib"
            "${prefix}webrtc-java-${targetPlatform}.${ext}"
        } else {
            filename
        }
    }
    
    eachFile { relativePath = RelativePath(true, name) }
}

tasks.named("processResources") {
    dependsOn(copyNativeLibs)
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("webrtc-java")
    archiveClassifier.set(targetPlatform)
}

tasks.withType<Javadoc> { enabled = false }