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
// We don't send duplicated packets to avoid overloading JVM
PenPacketData windowsPreviousPkt = { 0, 1, 0, 0, 0 };

// Struct for mapping the layout of packet
typedef struct {
	int pressure;
	int status;
	int tiltX;
	int tiltY;
	LONG specMaxPressure;
} WinInkPacketLayout;

class CalligraphyStylusPlugin : public IStylusAsyncPlugin {
private:
	LONG references;

	WinInkPacketLayout mapLayout(IRealTimeStylus *rts, TABLET_CONTEXT_ID tcid) {
		float scaleX, scaleY;
		ULONG propsCount;
		PACKET_PROPERTY *props;
		WinInkPacketLayout layout = { -1, -1, -1, -1, 1 };
		if (FAILED(rts->GetPacketDescriptionData(tcid, &scaleX, &scaleY, &propsCount, &props))) return layout;

		for (ULONG i = 0; i < propsCount; i++) {
			if (props[i].guid == GUID_PACKETPROPERTY_GUID_NORMAL_PRESSURE) {
				layout.pressure = (int)i;
				layout.specMaxPressure = props[i].PropertyMetrics.nLogicalMax;
			}

			if (props[i].guid == GUID_PACKETPROPERTY_GUID_PACKET_STATUS) layout.status = (int)i;
			if (props[i].guid == GUID_PACKETPROPERTY_GUID_X_TILT_ORIENTATION) layout.tiltX = (int)i;
			if (props[i].guid == GUID_PACKETPROPERTY_GUID_Y_TILT_ORIENTATION) layout.tiltY = (int)i;
		}

		return layout;
	}

	void emitPackets(
			bool hovering,
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG packetsCount, ULONG bufferLength, LONG *buffer,
			ULONG *outPacketsCount, LONG **outBuffer) {
		if (!windowsInitialized) return;
		ULONG propertiesCount = bufferLength / packetsCount;
		WinInkPacketLayout layout = mapLayout(rts, stylus->tcid);

		// #region Emit events
		JNIEnv *jEnv;
		if (windowsJVM->AttachCurrentThread((void**)&jEnv, NULL)) return;

		for (ULONG i = 0; i < packetsCount; i += propertiesCount) {
			// 0000[isBarrel]0[isEraser][isDown]
			LONG packetStatus = layout.status != -1 ? buffer[i + layout.status] : hovering ? 0 : 1;
			bool barrel = packetStatus & 8;
			bool eraser = packetStatus & 2;

			PenPacketData nextPkt = {
				layout.pressure != -1 ? buffer[i + layout.pressure] : hovering ? 0 : 1,
				layout.specMaxPressure,
				layout.tiltX != -1 ? buffer[i + layout.tiltX] : 0,
				layout.tiltY != -1 ? buffer[i + layout.tiltY] : 0,
				(eraser ? FLAG_ERASER : 0ull) |
				(barrel ? FLAG_AUXCLICK : 0ull)
			};

			if (windowsPreviousPkt == nextPkt) continue;
			windowsPreviousPkt = nextPkt;
			jEnv->CallVoidMethod(
					windowsJCallback,
					windowsJCallbackMethod,
					(jshort)nextPkt.pressure,
					(jshort)nextPkt.maxPressure,
					(jshort)nextPkt.tiltX,
					(jshort)nextPkt.tiltY,
					(jlong)nextPkt.flags);
		}

		windowsJVM->DetachCurrentThread();
		// #endregion
	}
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

	STDMETHOD (DataInterest)(RealTimeStylusDataInterest *pEventInterest) {
		*pEventInterest = (RealTimeStylusDataInterest)(
				RTSDI_Packets |
				RTSDI_InAirPackets |
				RTSDI_StylusUp |
				RTSDI_StylusDown);
		return S_OK;
	}

	STDMETHOD (Packets)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG packetsCount, ULONG bufferLength, LONG *buffer,
			ULONG *outPacketsCount, LONG **outBuffer) {
		emitPackets(false, rts, stylus, packetsCount, bufferLength, buffer, outPacketsCount, outBuffer);
		return S_OK;
	}

	STDMETHOD (InAirPackets)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG packetsCount, ULONG bufferLength, LONG *buffer,
			ULONG *outPacketsCount, LONG **outBuffer) {
		emitPackets(true, rts, stylus, packetsCount, bufferLength, buffer, outPacketsCount, outBuffer);
		return S_OK;
	}

	STDMETHOD (StylusDown)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG bufferLength, LONG *buffer,
			LONG **outBuffer) {
		ULONG temp;
		emitPackets(false, rts, stylus, 1, bufferLength, buffer, &temp, outBuffer);
		return S_OK;
	}

	STDMETHOD (StylusUp)(
			IRealTimeStylus *rts,
			const StylusInfo *stylus,
			ULONG bufferLength,
			LONG *buffer,
			LONG **outBuffer) {
		ULONG temp;
		emitPackets(true, rts, stylus, 1, bufferLength, buffer, &temp, outBuffer);
		return S_OK;
	}

	STDMETHOD (StylusButtonUp)(IRealTimeStylus *rts, STYLUS_ID sid, const GUID *guid, POINT*) { return S_OK; }
	STDMETHOD (StylusButtonDown)(IRealTimeStylus *rts, STYLUS_ID sid, const GUID *guid, POINT*) { return S_OK; }
	STDMETHOD (RealTimeStylusEnabled)(IRealTimeStylus*, ULONG, const TABLET_CONTEXT_ID*) { return S_OK; }
	STDMETHOD (RealTimeStylusDisabled)(IRealTimeStylus*, ULONG, const TABLET_CONTEXT_ID*) { return S_OK; }
	STDMETHOD (StylusInRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
	STDMETHOD (StylusOutOfRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
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
			GUID_PACKETPROPERTY_GUID_PACKET_STATUS,
			GUID_PACKETPROPERTY_GUID_X_TILT_ORIENTATION,
			GUID_PACKETPROPERTY_GUID_Y_TILT_ORIENTATION
	};

	windowsRts->SetDesiredPacketDescription(4, props);
	windowsRts->put_Enabled(true);
	return hr;
}
