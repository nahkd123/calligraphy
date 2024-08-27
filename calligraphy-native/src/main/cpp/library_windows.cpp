#include <iostream>
#include <jni.h>
#include <rtscom.h>
#include <rtscom_i.c>

#include "library_base.h"

bool windowsInitialized = false;
JavaVM *windowsJVM;
jobject windowsJCallback;
jmethodID windowsJCallbackMethod;

// Store previous packet
PenPacketData windowsPreviousPkt = { 0, 1, 0, 0, 0 };

void windowsEmitPackets(
		bool hovering,
		IRealTimeStylus *rts,
		const StylusInfo *stylus,
		ULONG packetsCount, ULONG bufferLength, LONG *buffer,
		ULONG *outPacketsCount, LONG **outBuffer) {
	if (!windowsInitialized) return;

	// #region Read maximum pressure
	ULONG propertiesCount = bufferLength / packetsCount;
	IInkTablet *tablet;
	float scaleX, scaleY;
	LONG maxPressure = 1;
	ULONG specPropsCount;
	PACKET_PROPERTY *specProps;
	rts->GetPacketDescriptionData(stylus->tcid, &scaleX, &scaleY, &specPropsCount, &specProps);

	for (ULONG i = 0; i < specPropsCount; i++) {
		if (specProps[i].guid != GUID_PACKETPROPERTY_GUID_NORMAL_PRESSURE) continue;
		maxPressure = specProps[i].PropertyMetrics.nLogicalMax;
		break;
	}
	// #endregion

	// #region Emit events
	JNIEnv *jEnv;
	if (windowsJVM->AttachCurrentThread((void**)&jEnv, NULL)) return;

	for (ULONG i = 0; i < packetsCount; i += propertiesCount) {
		PenPacketData nextPkt = {
			propertiesCount >= 3 ? buffer[i + 2] : hovering ? 0 : 1,
			maxPressure,
			propertiesCount >= 4 ? buffer[i + 3] : 0,
			propertiesCount >= 5 ? buffer[i + 4] : 0,
			0
		};

		if (windowsPreviousPkt == nextPkt) continue;
		windowsPreviousPkt = nextPkt;
		jEnv->CallVoidMethod(
				windowsJCallback,
				windowsJCallbackMethod,
				(jshort)nextPkt.pressure,
				(jshort)maxPressure,
				(jshort)nextPkt.tiltX,
				(jshort)nextPkt.tiltY,
				(jlong)0);
	}

	windowsJVM->DetachCurrentThread();
	// #endregion
}

/*void windowsEmitPackets(const PenPacketData &data) {
	if (!windowsInitialized) return;
	JNIEnv *jEnv;
	if (windowsJVM->AttachCurrentThread((void**)&jEnv, NULL)) return;
	jEnv->CallVoidMethod(
			windowsJCallback,
			windowsJCallbackMethod,
			(jshort) data.pressure,
			(jshort) data.maxPressure,
			(jshort) data.tiltX,
			(jshort) data.tiltY,
			(jlong) data.flags);
	windowsJVM->DetachCurrentThread();
}*/

class CalligraphyStylusPlugin : public IStylusAsyncPlugin {
private:
	LONG references;
public:
	CalligraphyStylusPlugin() : references(1) {}
	virtual ~CalligraphyStylusPlugin() {}

	STDMETHOD_ (ULONG, AddRef)() { return InterlockedIncrement(&references); }
	STDMETHOD_ (ULONG, Release)() {
		ULONG nNewRef = InterlockedDecrement(&references);
		if (nNewRef == 0) delete this;
		return nNewRef;
	}
	STDMETHOD (QueryInterface)(REFIID riid, LPVOID *ppvObj) {
		if ((riid == IID_IStylusAsyncPlugin) || (riid == IID_IUnknown)) {
			*ppvObj = this;
			AddRef();
			return S_OK;
		}

		*ppvObj = NULL;
		return E_NOINTERFACE;
	}

	STDMETHOD (Packets)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG packetsCount, ULONG bufferLength, LONG *buffer,
			ULONG *outPacketsCount, LONG **outBuffer) {
		windowsEmitPackets(false, rts, stylus, packetsCount, bufferLength, buffer, outPacketsCount, outBuffer);
		return S_OK;
	}

	STDMETHOD (InAirPackets)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG packetsCount, ULONG bufferLength, LONG *buffer,
			ULONG *outPacketsCount, LONG **outBuffer) {
		windowsEmitPackets(true, rts, stylus, packetsCount, bufferLength, buffer, outPacketsCount, outBuffer);
		return S_OK;
	}

	STDMETHOD (StylusDown)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG bufferLength, LONG *buffer,
			LONG **outBuffer) {
		ULONG temp;
		windowsEmitPackets(false, rts, stylus, 1, bufferLength, buffer, &temp, outBuffer);
		return S_OK;
	}

	STDMETHOD (StylusUp)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG bufferLength,
			LONG *buffer,
			LONG **outBuffer) {
		ULONG temp;
		windowsEmitPackets(true, rts, stylus, 1, bufferLength, buffer, &temp, outBuffer);
		return S_OK;
	}

	STDMETHOD (DataInterest)(RealTimeStylusDataInterest *pEventInterest) {
		*pEventInterest = (RealTimeStylusDataInterest)(
				RTSDI_Packets |
				RTSDI_InAirPackets |
				RTSDI_StylusUp |
				RTSDI_StylusDown);
		return S_OK;
	}

	STDMETHOD (RealTimeStylusEnabled)(IRealTimeStylus*, ULONG, const TABLET_CONTEXT_ID*) { return S_OK; }
	STDMETHOD (RealTimeStylusDisabled)(IRealTimeStylus*, ULONG, const TABLET_CONTEXT_ID*) { return S_OK; }
	STDMETHOD (StylusInRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
	STDMETHOD (StylusOutOfRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
	STDMETHOD (StylusButtonUp)(IRealTimeStylus*, STYLUS_ID, const GUID*, POINT*) { return S_OK; }
	STDMETHOD (StylusButtonDown)(IRealTimeStylus*, STYLUS_ID, const GUID*, POINT*) { return S_OK; }
	STDMETHOD (SystemEvent)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID, SYSTEM_EVENT, SYSTEM_EVENT_DATA) { return S_OK; }
	STDMETHOD (TabletAdded)(IRealTimeStylus*, IInkTablet*) { return S_OK; }
	STDMETHOD (TabletRemoved)(IRealTimeStylus*, LONG) { return S_OK; }
	STDMETHOD (CustomStylusDataAdded)(IRealTimeStylus*, const GUID*, ULONG, const BYTE*) { return S_OK; }
	STDMETHOD (Error)(IRealTimeStylus*, IStylusPlugin*, RealTimeStylusDataInterest, HRESULT, LONG_PTR*) { return S_OK; }
	STDMETHOD (UpdateMapping)(IRealTimeStylus*) { return S_OK; }
};

IRealTimeStylus *windowsRts;
CalligraphyStylusPlugin *windowsRtsPlugin;

bool calligraphyIsSupported() {
	return true;
}

bool calligraphyIsInitialized() {
	return windowsInitialized;
}

void calligraphyFree(JNIEnv *jEnv) {
	if (!windowsInitialized) return;
	windowsInitialized = false;
	jEnv->DeleteLocalRef(windowsJCallback);
	windowsJVM = nullptr;

	if (windowsRts) {
		windowsRts->Release();
		windowsRts = NULL;
	}

	if (windowsRtsPlugin) {
		windowsRtsPlugin->Release();
		windowsRtsPlugin = NULL;
	}
}

int calligraphyInit(JNIEnv *jEnv, jobject jCallback, void *handle) {
	if (windowsInitialized) calligraphyFree(jEnv);
	windowsInitialized = true;
	jEnv->GetJavaVM(&windowsJVM);
	windowsJCallback = jEnv->NewGlobalRef(jCallback);
	jclass jClass = jEnv->GetObjectClass(jCallback);
	windowsJCallbackMethod = jEnv->GetMethodID(jClass, "callback", "(SSSSJ)V");

	// Setup RealTimeStylus
	HRESULT hr = CoCreateInstance(CLSID_RealTimeStylus, NULL, CLSCTX_ALL, IID_PPV_ARGS(&windowsRts));
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Native Code: Error while creating RealTimeStylus" << std::endl;
		return hr;
	}

	hr = windowsRts->put_HWND((HANDLE_PTR)handle);
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Native Code: Error while attaching HWND (do you have permission?)" << std::endl;
		calligraphyFree(jEnv);
		return hr;
	}

	windowsRtsPlugin = new CalligraphyStylusPlugin();
	hr = windowsRts->AddStylusAsyncPlugin(0, windowsRtsPlugin);
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Native Code: Error while adding async stylus plugin" << std::endl;
		calligraphyFree(jEnv);
		return hr;
	}

	GUID props[] = {
			GUID_PACKETPROPERTY_GUID_NORMAL_PRESSURE,
			GUID_PACKETPROPERTY_GUID_X_TILT_ORIENTATION,
			GUID_PACKETPROPERTY_GUID_Y_TILT_ORIENTATION
	};

	windowsRts->SetDesiredPacketDescription(3, props);
	windowsRts->put_Enabled(true);
	return hr;
}
