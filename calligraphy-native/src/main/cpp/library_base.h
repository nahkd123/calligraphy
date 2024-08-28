// Base header for library_windows and library_linux
// Ignore my janky C++ code.

#ifndef _LIBRARY_BASE_
#define _LIBRARY_BASE_

#include <jni.h>

/////////////
// DEFINES //
/////////////
#define FLAG_ERASER 1ull
#define FLAG_AUXCLICK 2ull

#define ERRNO_SUCCESS 0
#define ERRNO_UNKNOWN 1

/////////////
// STRUCTS //
/////////////
typedef struct {
	int pressure;
	int maxPressure;
	int tiltX;
	int tiltY;
	unsigned long long flags;
} PenPacketData;

///////////////
// FUNCTIONS //
///////////////
bool calligraphyIsSupported();
bool calligraphyIsInitialized();
int calligraphyInit(JNIEnv *jEnv, jobject jCallback, void *handle); // handle only used on Windows
void calligraphyFree(JNIEnv *jEnv);

#endif
