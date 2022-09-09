#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#define JNIEXPORT __attribute__((visibility("default")))
#define JNIIMPORT
#define JNICALL

#if defined(__LP64__) && __LP64__ /* for -Wundef */
typedef int jint;
#else
typedef long jint;
#endif
typedef long long jlong;
typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
