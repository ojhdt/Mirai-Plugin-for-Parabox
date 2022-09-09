#include "moe_ore_silk_AudioUtils.h"

JNIEXPORT jstring JNICALL Java_moe_ore_silk_AudioUtils_check
        (JNIEnv * env, jclass clz) {
    // GetHighResolutionTime();
    return (*env)->NewStringUTF(env, "silk");
}
