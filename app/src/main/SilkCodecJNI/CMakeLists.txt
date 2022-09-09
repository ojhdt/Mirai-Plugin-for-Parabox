cmake_minimum_required(VERSION 3.22)
project(silkcodec)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections -fPIC -O3 -fvisibility=hidden -w ")
set(CMAKE_SHARED_LINKER_FLAGS "-Wl,--gc-sections")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

ADD_DEFINITIONS(-DFIXED_POINT)
ADD_DEFINITIONS(-DUSE_SMALLFT)

aux_source_directory(libmp3lame LAME_SRC_DIR)
aux_source_directory(mpglib LAME_SRC_DIR)

aux_source_directory(src SRC_DIR)
# AUX_SOURCE_DIRECTORY(ffmpeg FFMPEG_SRC_DIR)

# FOR SILK
include_directories(src interface libmp3lame mpglib)

# For FFMPEG
# include_directories(ffmpeg)

add_library(silkcodec SHARED
        JNILame.h JNILame.c ${LAME_SRC_DIR}
        ${SRC_DIR} silkcodec.h silkcodec.c
        moe_ore_silk_AudioUtils.c moe_ore_silk_AudioUtils.h
        moe_ore_silk_SilkCodec.c moe_ore_silk_SilkCodec.h
        moe_ore_silk_LameCoder.h)
