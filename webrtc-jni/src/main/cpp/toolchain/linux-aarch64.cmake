set(CMAKE_SYSTEM_NAME       Linux)
set(CMAKE_SYSTEM_PROCESSOR  aarch64)

find_program(CLANG_BIN clang
    HINTS /opt/clang/bin /usr/bin /usr/local/bin
    DOC "Path to clang executable"
)
find_program(CLANGXX_BIN clang++
    HINTS /opt/clang/bin /usr/bin /usr/local/bin
    DOC "Path to clang++ executable"
)

if(NOT CLANG_BIN OR NOT CLANGXX_BIN)
    message(FATAL_ERROR "Clang not found. Please install clang.")
endif()

set(CMAKE_C_COMPILER        ${CLANG_BIN})
set(CMAKE_CXX_COMPILER      ${CLANGXX_BIN})

find_program(LLVM_AR_BIN llvm-ar
    HINTS /opt/clang/bin /usr/bin /usr/local/bin
)
if(LLVM_AR_BIN)
    set(CMAKE_AR ${LLVM_AR_BIN})
    find_program(LLVM_RANLIB_BIN llvm-ranlib HINTS /opt/clang/bin /usr/bin /usr/local/bin)
    if(LLVM_RANLIB_BIN)
        set(CMAKE_RANLIB ${LLVM_RANLIB_BIN})
    endif()
endif()

find_program(LLD_BIN ld.lld
    HINTS /opt/clang/bin /usr/bin /usr/local/bin
)

set(TARGET_TRIPLE           aarch64-linux-gnu)

set(CMAKE_C_FLAGS           "--target=${TARGET_TRIPLE}")
if(LLD_BIN)
    set(CMAKE_C_FLAGS       "${CMAKE_C_FLAGS} -fuse-ld=lld")
endif()

set(CMAKE_CXX_FLAGS         "${CMAKE_C_FLAGS}")
set(CMAKE_EXE_LINKER_FLAGS  "${CMAKE_EXE_LINKER_FLAGS} -v -Wl,--verbose")

get_filename_component(TOOLCHAIN_DIR ${CMAKE_CURRENT_LIST_FILE} DIRECTORY)
set(LOCAL_SYSROOT_PATH "${TOOLCHAIN_DIR}/../dependencies/webrtc/linux")

file(GLOB LINUX_SYSROOT 
    "/opt/sysroot/debian*arm64*"
    "${LOCAL_SYSROOT_PATH}/debian*arm64*"
)

if(LINUX_SYSROOT)
    list(GET LINUX_SYSROOT 0 SELECTED_SYSROOT)
    message(STATUS "Found Sysroot: ${SELECTED_SYSROOT}")
    set(CMAKE_SYSROOT ${SELECTED_SYSROOT})
    set(DEFERRED_SYSROOT ${SELECTED_SYSROOT})
else()
    message(FATAL_ERROR "No arm64 sysroot found. Please run 'python3 install-sysroot.py --arch=arm64'")
endif()

set(TARGET_CPU "arm64")