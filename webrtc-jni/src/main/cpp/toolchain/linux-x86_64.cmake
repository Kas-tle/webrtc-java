set(CMAKE_SYSTEM_NAME       Linux)
set(CMAKE_SYSTEM_PROCESSOR  x86_64)

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
    DOC "Path to llvm-ar executable"
)

if(LLVM_AR_BIN)
    set(CMAKE_AR ${LLVM_AR_BIN})
    find_program(LLVM_RANLIB_BIN llvm-ranlib
        HINTS /opt/clang/bin /usr/bin /usr/local/bin
    )
    if(LLVM_RANLIB_BIN)
        set(CMAKE_RANLIB ${LLVM_RANLIB_BIN})
    endif()
else()
    message(WARNING "llvm-ar not found. Using system default ar.")
endif()

get_filename_component(TOOLCHAIN_DIR ${CMAKE_CURRENT_LIST_FILE} DIRECTORY)
set(LOCAL_SYSROOT_PATH "${TOOLCHAIN_DIR}/../dependencies/webrtc/linux")

file(GLOB LINUX_SYSROOT 
    "/opt/sysroot/debian*amd64*"
    "${LOCAL_SYSROOT_PATH}/debian*amd64*"
)

if(LINUX_SYSROOT)
    list(GET LINUX_SYSROOT 0 SELECTED_SYSROOT)
    message(STATUS "Found Sysroot: ${SELECTED_SYSROOT}")
    set(DEFERRED_SYSROOT ${SELECTED_SYSROOT}) 
else()
    message(WARNING "No sysroot found. Build may depend on system libraries.")
endif()

set(TARGET_CPU "x64")

