cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(can_use_assembler TRUE)
enable_language(ASM)

set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(SOURCES)

file(GLOB_RECURSE SOURCES "*.c")

if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    list(APPEND SOURCES	memset32_neon.S)
endif(${ANDROID_ABI} STREQUAL "armeabi-v7a")

if(${ANDROID_ABI} STREQUAL "armeabi")
    list(APPEND SOURCES	memset.arm.S)
endif(${ANDROID_ABI} STREQUAL "armeabi")

add_library(droidsonroids_gif SHARED ${SOURCES})

set(LIBS)
list(APPEND LIBS
	jnigraphics
    android
    GLESv2
    log
)

target_link_libraries(droidsonroids_gif ${LIBS})
