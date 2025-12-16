cmake_minimum_required(VERSION 3.13)

if(NOT WEBRTC_TOOLCHAIN_FILE STREQUAL "")
    message(STATUS "Toolchain file: ${WEBRTC_TOOLCHAIN_FILE}")
    set(CMAKE_TOOLCHAIN_FILE "${WEBRTC_TOOLCHAIN_FILE}")
endif()

project(webrtc-java)

if(CMAKE_CXX_COMPILER_ID MATCHES "Clang" AND LINUX)
    find_program(LLD_PATH ld.lld 
        HINTS /opt/clang/bin /usr/bin /usr/local/bin
        DOC "Path to lld linker"
    )
    if(LLD_PATH)
        message(STATUS "Found LLD: ${LLD_PATH}")
        add_link_options(-fuse-ld=lld)
    else()
        message(WARNING "LLD not found! Linker errors may occur.")
    endif()
    if(TARGET_CPU STREQUAL "x64")
        add_compile_options(-nostdinc++ -D_LIBCPP_HARDENING_MODE=_LIBCPP_HARDENING_MODE_EXTENSIVE)
        add_link_options(-stdlib=libc++ -v -Wl,--verbose)
    endif()
endif()

if(UNIX AND NOT APPLE)
    # Apply the sysroot configuration for the actual build
    if(DEFERRED_SYSROOT)
        # Configure sysroot for all subsequent operations
        set(CMAKE_SYSROOT ${DEFERRED_SYSROOT})
        set(CMAKE_FIND_ROOT_PATH ${DEFERRED_SYSROOT})

        # Set pkg-config environment
        set(ENV{PKG_CONFIG_SYSROOT_DIR} "${DEFERRED_SYSROOT}")
        set(ENV{PKG_CONFIG_PATH} "${DEFERRED_SYSROOT}/usr/lib/pkgconfig:${DEFERRED_SYSROOT}/usr/share/pkgconfig")

        # Restrict library and header searches to sysroot only
        set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
        set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
        set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
    endif()

    set(LINUX TRUE)
endif()

if(APPLE)
    set(CMAKE_CXX_VISIBILITY_PRESET hidden)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s")
    set(SOURCE_TARGET macos)
elseif(LINUX)
    set(CMAKE_POSITION_INDEPENDENT_CODE ON)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -s")
    set(SOURCE_TARGET linux)
elseif(WIN32)
    set(CMAKE_CXX_FLAGS_RELEASE "/MT")
    set(CMAKE_CXX_FLAGS_DEBUG "/MTd")
    set(SOURCE_TARGET windows)
endif()

if (CMAKE_CXX_COMPILER_ID STREQUAL Clang AND CMAKE_CXX_COMPILER_FRONTEND_VARIANT STREQUAL "MSVC")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /showFilenames")
endif()

add_subdirectory(dependencies/webrtc)
add_subdirectory(dependencies/jni-voithos)

file(GLOB SOURCES_ROOT "src/*.cpp")
file(GLOB SOURCES_API "src/api/*.cpp")
file(GLOB SOURCES_RTC "src/rtc/*.cpp")

list(APPEND SOURCES
    ${SOURCES_ROOT}
    ${SOURCES_API}
    ${SOURCES_RTC}
)

add_library(${PROJECT_NAME} SHARED ${SOURCES})

# Introduced since WebRTC branch 7204 and Clang 20.
target_compile_options(${PROJECT_NAME} PRIVATE -Wno-nullability-completeness)

target_include_directories(${PROJECT_NAME}
    PRIVATE
        include
        include/api
        include/rtc
)

set_target_properties(${PROJECT_NAME} PROPERTIES
    OUTPUT_NAME "${PROJECT_NAME}-${OUTPUT_NAME_SUFFIX}"
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED ON
    CXX_EXTENSIONS OFF
)

target_link_libraries(${PROJECT_NAME} jni-voithos)
target_link_libraries(${PROJECT_NAME} webrtc)

if(APPLE)
    set_source_files_properties(${SOURCES} PROPERTIES COMPILE_FLAGS "-x objective-c++")
    target_link_options(${PROJECT_NAME} PRIVATE "-ObjC")
    target_link_libraries(${PROJECT_NAME} "-framework Foundation" "-framework AVFoundation" "-framework CoreMedia" "-framework CoreVideo")
elseif(LINUX)
    if(NOT TARGET_CPU MATCHES "^arm")
        set(CXX_LIBS "-static-libgcc -stdlib=libc++ -lc++ -lc++abi")
    else()
        set(CXX_LIBS "-static-libgcc")
    endif()

    target_link_libraries(${PROJECT_NAME} ${CXX_LIBS})
elseif(WIN32)
    target_link_libraries(${PROJECT_NAME})
endif()

install(TARGETS ${PROJECT_NAME}
    RUNTIME DESTINATION ${CMAKE_INSTALL_PREFIX} COMPONENT Runtime
    LIBRARY DESTINATION ${CMAKE_INSTALL_PREFIX} COMPONENT Runtime
)

if (PROJECT_IS_TOP_LEVEL)
    get_filename_component(TARGET_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../.." ABSOLUTE)
    
    set(COMPILE_COMMANDS_SRC "${CMAKE_BINARY_DIR}/compile_commands.json")
    set(COMPILE_COMMANDS_DST "${TARGET_DIR}/compile_commands.json")

    if (WIN32)
        # Windows: Copy instead of symlink to avoid permission issues
        execute_process(
            COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "${COMPILE_COMMANDS_SRC}"
                "${COMPILE_COMMANDS_DST}"
        )
        message(STATUS "Copied compile_commands.json to: ${TARGET_DIR}")
    else()
        # Unix: Symlink
        execute_process(
            COMMAND ${CMAKE_COMMAND} -E create_symlink
                "${COMPILE_COMMANDS_SRC}"
                "${COMPILE_COMMANDS_DST}"
        )
        message(STATUS "Created compile_commands.json symlink in: ${TARGET_DIR}")
    endif()
endif()