This is a fork of devopvoid's [webrtc-java](https://github.com/devopvoid/webrtc-java) library, a Java wrapper for the [WebRTC Native API](https://webrtc.github.io/webrtc-org/native-code/native-apis).

## Differences from the Original Project

This stripped down version of the library removes audio and video support, focusing solely on data channels for peer-to-peer data exchange. It is intended for use cases where only data transfer is required, such as multiplayer gaming or real-time data synchronization. The removal of media capabilities results in a significantly smaller library size and increased portability.

It also adds support for ARM Windows systems, and has testing coverage for all platforms via GitHub Actions.

## Usage

The base project is published under the group `dev.kastle.webrtc` and artifact `webrtc-java`. The native libraries are published under the same group and artifact with platform-specific classifiers:

- `linux-x86_64`
- `linux-aarch32`
- `linux-aarch64`
- `windows-x86_64`
- `windows-aarch64`
- `macos-x86_64`
- `macos-aarch64`

Note that the natives are not bundled with the main library, so you will need to appropriately include those that are needed for your project. If your project is an executable jar, you may want to read [JEP-472](https://openjdk.org/jeps/472). Because this project uses JNI, you may need to enable native access, depending on your Java version and execution environment. The main module is named `dev.kastle.webrtc`, and the natives modules are named `dev.kastle.webrtc.natives.<platform>` (e.g. `dev.kastle.webrtc.natives.linux.x86_64`), with any hyphens replaced by periods.

## Development

The project has a preconfigured `.vscode` setup for easy development. Make sure to install the recommended extensions when prompted. Run the `./gradlew build` command to build the project. Note that building the native libraries requires [clang](https://clang.llvm.org/get_started.html) and [cmake](https://cmake.org/download/) to be installed and on your path.

The initial build will take significant time to compile, as cloning the WebRTC source code brings in multiple other repo source trees from the Chromium project, requiring about 30 GB of disk space. On Windows, you may need to set `WEBRTC_CHECKOUT_FOLDER` to a shorter path to avoid exceeding file path length limits.

The version of WebRTC used can be changed by modifying the `webrtc.branch` property in [`webrtc-jni/gradle.properties`](webrtc-jni/gradle.properties). Note, however, that changing the version used will likely require updating the patches in [`webrtc-jni/src/main/cpp/dependencies/webrtc/patches/`](webrtc-jni/src/main/cpp/dependencies/webrtc/patches/) to ensure successful compilation.

## License

This copyright notice is maintained from the original project:

```md
Copyright (c) 2019 Alex Andres

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
```