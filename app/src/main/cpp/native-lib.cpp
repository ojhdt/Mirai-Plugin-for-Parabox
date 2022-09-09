#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_ojhdtapp_miraipluginforparabox_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_ojhdtapp_miraipluginforparabox_domain_service_ConnService_stringFromJNI(
        JNIEnv *env,
        jobject thiz) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}