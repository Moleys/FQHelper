#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "FQHelper", __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_com_xxhy_fqhelper_xposed_NativeLib_doSomething(JNIEnv*, jclass) {
    LOGI("Hello from C in Xposed module!");
}

}
