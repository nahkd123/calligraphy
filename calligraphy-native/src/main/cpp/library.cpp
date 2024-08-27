#include <me_nahkd_calligraphy_nativelib_CalligraphyNative.h>

#include "library_base.cpp"

#ifdef _WIN32
#include "library_windows.cpp"
#elif __linux__
#include "library_linux.cpp"
#else
#include "library_unsupported.cpp"
#endif

/////////////////////////
// JNI Implementations //
/////////////////////////

jboolean JNICALL Java_me_nahkd_calligraphy_nativelib_CalligraphyNative_isSupported(JNIEnv *, jclass) {
	return calligraphyIsSupported() ? JNI_TRUE : JNI_FALSE;
}

jboolean JNICALL Java_me_nahkd_calligraphy_nativelib_CalligraphyNative_isInitialized(JNIEnv *, jclass) {
	return calligraphyIsInitialized() ? JNI_TRUE : JNI_FALSE;
}

jint JNICALL Java_me_nahkd_calligraphy_nativelib_CalligraphyNative_initialize(JNIEnv *jEnv, jclass, jobject jCallback, jlong jHandle) {
	return (jint)calligraphyInit(jEnv, jCallback, (void*)jHandle);
}

void JNICALL Java_me_nahkd_calligraphy_nativelib_CalligraphyNative_uninitialize(JNIEnv *jEnv, jclass) {
	calligraphyFree(jEnv);
}
