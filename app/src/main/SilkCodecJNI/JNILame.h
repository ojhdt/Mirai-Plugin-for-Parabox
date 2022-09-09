#ifndef SILKCODEC_JNILAME_H
#define SILKCODEC_JNILAME_H

#include "global.h"
#include "lame.h"
#include "moe_ore_silk_LameCoder.h"

lame_global_flags *initializeDefault(
        JNIEnv *env);

lame_global_flags *initialize(
        JNIEnv *env,

        jint inSamplerate, jint outChannel,
        jint outSamplerate, jint outBitrate, jfloat scaleInput, jint mode, jint vbrMode,
        jint quality, jint vbrQuality, jint abrMeanBitrate, jint lowpassFreq, jint highpassFreq,
        jstring id3tagTitle, jstring id3tagArtist, jstring id3tagAlbum,
        jstring id3tagYear, jstring id3tagComment);

void close(
        lame_global_flags *glf);

#endif //SILKCODEC_JNILAME_H
