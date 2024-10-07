#include "library.h"
#include "utils.cpp"

// Make sure to change the number 8 to the count of props!
GUID desiredProps[] = {
		GUID_PACKETPROPERTY_GUID_X, // 0
		GUID_PACKETPROPERTY_GUID_Y, // 1
		GUID_PACKETPROPERTY_GUID_Z, // 2
		GUID_PACKETPROPERTY_GUID_PACKET_STATUS, // 3
		GUID_PACKETPROPERTY_GUID_NORMAL_PRESSURE, // 4
		GUID_PACKETPROPERTY_GUID_X_TILT_ORIENTATION, // 5
		GUID_PACKETPROPERTY_GUID_Y_TILT_ORIENTATION, // 6
		GUID_PACKETPROPERTY_GUID_TWIST_ORIENTATION, // 7
};

const int desiredPropsCount = 8;

class WininkRtsAsyncPlugin : public IStylusAsyncPlugin {
private:
	LONG references;
	JavaVM *jvm;
	jobject javaDriver;
	jmethodID nativeRtsEnable;
	jmethodID nativeTabletAdd;
	jmethodID nativeTabletDisconnect;
	jmethodID nativeTabletPacket;

	void tryAddTablet(IRealTimeStylus *rts, TABLET_CONTEXT_ID tcid, IInkTablet *tablet) {
		JNIEnv *env;
		if (jvm->AttachCurrentThread((void**)&env, NULL) != 0) return;

		debug("Adding tablet with tcid = %x", tcid);
		debug("  Extracing tablet GUID and name...");
		BSTR tabletGuid, tabletName;
		if (FAILED(tablet->get_PlugAndPlayId(&tabletGuid))) { jvm->DetachCurrentThread(); return; }
		if (FAILED(tablet->get_Name(&tabletName))) { jvm->DetachCurrentThread(); SysFreeString(tabletGuid); return; }
		_bstr_t cTabletGuid(tabletGuid, true);
		_bstr_t cTabletName(tabletName, true);

		float scaleX, scaleY;
		ULONG propsCount;
		PACKET_PROPERTY *props;

		if (FAILED(rts->GetPacketDescriptionData(tcid, &scaleX, &scaleY, &propsCount, &props))) {
			jvm->DetachCurrentThread();
			return;
		}

		jstring jTabletGuid = env->NewStringUTF(cTabletGuid);
		jstring jTabletName = env->NewStringUTF(cTabletName);
		jlong *layout = new jlong[propsCount * 3];

		debug("  This tablet have %i properties in packet", propsCount);
		for (ULONG i = 0; i < propsCount; i++) {
			for (int j = 0; j < desiredPropsCount; j++) {
				if (props[i].guid == desiredProps[j]) {
					debug("    Property %i map to buffer index %i", j, i);
					layout[i * 3] = j;

					if (props[i].guid == GUID_PACKETPROPERTY_GUID_PACKET_STATUS) {
						layout[i * 3 + 1] = 0;
						layout[i * 3 + 2] = 0;
					} else {
						layout[i * 3 + 1] = props[i].PropertyMetrics.nLogicalMin;
						layout[i * 3 + 2] = props[i].PropertyMetrics.nLogicalMax;
					}

					break;
				}
			}
		}

		jlongArray jlayout = env->NewLongArray(propsCount * 3);
		env->SetLongArrayRegion(jlayout, 0, propsCount * 3, layout);
		delete[] layout;

		debug("  Submitting tablet (%s:%s)...", (const char*)cTabletGuid, (const char*)cTabletName);
		env->CallVoidMethod(javaDriver, nativeTabletAdd, (jlong)tcid, jTabletGuid, jTabletName, (jdouble)scaleX, (jdouble)scaleY, jlayout);
		env->DeleteLocalRef(jTabletGuid);
		env->DeleteLocalRef(jTabletName);
		env->DeleteLocalRef(jlayout);
		jvm->DetachCurrentThread();
	}

	void tryAddTablet(IRealTimeStylus *rts, IInkTablet *tablet) {
		TABLET_CONTEXT_ID tcid;
		if (FAILED(rts->GetTabletContextIdFromTablet(tablet, &tcid))) return;
		tryAddTablet(rts, tcid, tablet);
	}

	void tryAddTablet(IRealTimeStylus *rts, TABLET_CONTEXT_ID tcid) {
		IInkTablet *tablet;
		if (FAILED(rts->GetTabletFromTabletContextId(tcid, &tablet))) return;
		tryAddTablet(rts, tcid, tablet);
	}

	void sendPackets(const StylusInfo *stylus, ULONG packetsCount, ULONG bufferLength, LONG *buffer) {
		JNIEnv *env;
		if (jvm->AttachCurrentThread((void**)&env, NULL) != 0) return;
		ULONG propertiesCount = bufferLength / packetsCount;
		jlong *nbuf = new jlong[propertiesCount];

		for (ULONG i = 0; i < packetsCount; i += propertiesCount) {
			jlongArray jbuf = env->NewLongArray(propertiesCount);
			for (int j = 0; j < propertiesCount; j++) nbuf[j] = buffer[i + j];
			env->SetLongArrayRegion(jbuf, 0, propertiesCount, nbuf);
			env->CallVoidMethod(javaDriver, nativeTabletPacket, (jlong)stylus->tcid, jbuf);
			env->DeleteLocalRef(jbuf);
		}

		delete[] nbuf;
		jvm->DetachCurrentThread();
	}
public:
	WininkRtsAsyncPlugin(JNIEnv *env, jobject driver) : references(1) {
		debug("Creating plugin...");
		env->GetJavaVM(&jvm);
		javaDriver = env->NewGlobalRef(driver);
		jclass javaWindowsInkDriver = env->GetObjectClass(javaDriver);
		nativeRtsEnable = env->GetMethodID(javaWindowsInkDriver, "nativeRtsEnable", "()V");
		nativeTabletAdd = env->GetMethodID(javaWindowsInkDriver, "nativeTabletAdd", "(JLjava/lang/String;Ljava/lang/String;DD[J)V");
		nativeTabletDisconnect = env->GetMethodID(javaWindowsInkDriver, "nativeTabletDisconnect", "(J)V");
		nativeTabletPacket = env->GetMethodID(javaWindowsInkDriver, "nativeTabletPacket", "(J[J)V");
		debug("Created plugin!");
	}

	virtual ~WininkRtsAsyncPlugin() {
		debug("Cleaning up plugin...");
		JNIEnv *env;

		if (jvm->AttachCurrentThread((void**)&env, NULL)) {
			debug("Unable to attach current thread - expecting memory leak then I guess?");
			return;
		}

		env->DeleteGlobalRef(javaDriver);
		jvm->DetachCurrentThread();
		debug("Cleaned up plugin!");
	}

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
				RTSDI_StylusDown |
				// RTSDI_TabletAdded |
				RTSDI_TabletRemoved |
				RTSDI_RealTimeStylusEnabled |
				RTSDI_RealTimeStylusDisabled);
		return S_OK;
	}

	STDMETHOD (Packets)(IRealTimeStylus *rts, const StylusInfo *stylus, ULONG packetsCount, ULONG bufferLength, LONG *buffer, ULONG *outPacketsCount, LONG **outBuffer) {
		sendPackets(stylus, packetsCount, bufferLength, buffer);
		return S_OK;
	}

	STDMETHOD (InAirPackets)(IRealTimeStylus *rts, const StylusInfo *stylus, ULONG packetsCount, ULONG bufferLength, LONG *buffer, ULONG *outPacketsCount, LONG **outBuffer) {
		sendPackets(stylus, packetsCount, bufferLength, buffer);
		return S_OK;
	}

	STDMETHOD (StylusUp)(IRealTimeStylus *rts, const StylusInfo *stylus, ULONG bufferLength, LONG *buffer, LONG **outBuffer) {
		sendPackets(stylus, 1, bufferLength, buffer);
		return S_OK;
	}

	STDMETHOD (StylusDown)(IRealTimeStylus *rts, const StylusInfo *stylus, ULONG bufferLength, LONG *buffer, LONG **outBuffer) {
		sendPackets(stylus, 1, bufferLength, buffer);
		return S_OK;
	}

	STDMETHOD (TabletAdded)(IRealTimeStylus *rts, IInkTablet *tablet) { return S_OK; }
	STDMETHOD (TabletRemoved)(IRealTimeStylus *rts, LONG tabletIndex) {
		JNIEnv *env;
		if (jvm->AttachCurrentThread((void**)&env, NULL) == 0) {
			ULONG tcidCount;
			TABLET_CONTEXT_ID *tcids;
			if (FAILED(rts->GetAllTabletContextIds(&tcidCount, &tcids))) { jvm->DetachCurrentThread(); return S_OK; }
			if (tabletIndex >= tcidCount) { debug("TabletRemoved(): Index out of bounds: %i/%i", tabletIndex, tcidCount); jvm->DetachCurrentThread(); return S_OK; }
			env->CallVoidMethod(javaDriver, nativeTabletDisconnect, (jlong)tcids[tabletIndex]);
			jvm->DetachCurrentThread();
		}
		return S_OK;
	}

	STDMETHOD (RealTimeStylusEnabled)(IRealTimeStylus *rts, ULONG tcidCount, const TABLET_CONTEXT_ID *tcids) {
		debug("RTS enabled with %i tablets", tcidCount);
		JNIEnv *env;
		if (jvm->AttachCurrentThread((void**)&env, NULL) == 0) {
			env->CallVoidMethod(javaDriver, nativeRtsEnable);
			jvm->DetachCurrentThread();
		}
		for (ULONG i = 0; i < tcidCount; i++) tryAddTablet(rts, (jlong)tcids[i]);
		return S_OK;
	}

	STDMETHOD (RealTimeStylusDisabled)(IRealTimeStylus*, ULONG tcidCount, const TABLET_CONTEXT_ID *tcids) {
		debug("RTS disabled with %i tablets", tcidCount);
		JNIEnv *env;
		if (jvm->AttachCurrentThread((void**)&env, NULL) == 0) {
			for (ULONG i = 0; i < tcidCount; i++) env->CallVoidMethod(javaDriver, nativeTabletDisconnect, (jlong)tcids[i]);
			jvm->DetachCurrentThread();
		}
		return S_OK;
	}

	STDMETHOD (StylusButtonUp)(IRealTimeStylus *rts, STYLUS_ID sid, const GUID *guid, POINT*) { return S_OK; }
	STDMETHOD (StylusButtonDown)(IRealTimeStylus *rts, STYLUS_ID sid, const GUID *guid, POINT*) { return S_OK; }
	STDMETHOD (StylusInRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
	STDMETHOD (StylusOutOfRange)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID) { return S_OK; }
	STDMETHOD (SystemEvent)(IRealTimeStylus*, TABLET_CONTEXT_ID, STYLUS_ID, SYSTEM_EVENT, SYSTEM_EVENT_DATA) { return S_OK; }
	STDMETHOD (CustomStylusDataAdded)(IRealTimeStylus*, const GUID*, ULONG, const BYTE*) { return S_OK; }
	STDMETHOD (Error)(IRealTimeStylus*, IStylusPlugin*, RealTimeStylusDataInterest, HRESULT, LONG_PTR*) { return S_OK; }
	STDMETHOD (UpdateMapping)(IRealTimeStylus*) { return S_OK; }
};

/**
 * Instance of Windows Ink driver.
 */
class WininkDriver {
public:
	IRealTimeStylus *rts;
	WininkRtsAsyncPlugin *plugin;

	WininkDriver(IRealTimeStylus *rts, WininkRtsAsyncPlugin *plugin)
	: rts(rts), plugin(plugin) {}

	~WininkDriver() {
		debug("WininkDriver destructor called");

		if (rts) {
			rts->Release();
			rts = nullptr;
			debug("  Released RealTimeStylus");
		}

		if (plugin) {
			plugin->Release();
			plugin = nullptr;
			debug("  Released RTS Plugin");
		}

		debug("WininkDriver destructor finished");
	}
};

/*
 * Create new instance.
 */
WininkDriver* newInsn(JNIEnv *env, jlong hwnd, jobject driver) {
	IRealTimeStylus *rts;
	HRESULT hr = CoCreateInstance(CLSID_RealTimeStylus, NULL, CLSCTX_ALL, IID_PPV_ARGS(&rts));
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Windows Native: Error while creating RealTimeStylus" << std::endl;
		return nullptr;
	}

	hr = rts->put_HWND((HANDLE_PTR)hwnd);
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Windows Native: Error while attaching HWND (do you have permission?)" << std::endl;
		rts->Release();
		return nullptr;
	}

	WininkRtsAsyncPlugin *plugin = new WininkRtsAsyncPlugin(env, driver);
	hr = rts->AddStylusAsyncPlugin(0, plugin);
	if (FAILED(hr)) {
		std::cerr << "Calligraphy Native Code: Error while adding async stylus plugin" << std::endl;
		rts->Release();
		plugin->Release();
		return nullptr;
	}

	rts->SetDesiredPacketDescription(desiredPropsCount, desiredProps);
	rts->put_Enabled(true);

	WininkDriver *insn = new WininkDriver(rts, plugin);
	debug("Created new instance with ID %p", insn);
	return insn;
}

jlong JNICALL Java_me_nahkd_calligraphy_windows_WindowsInkDriver_nativeInit(JNIEnv *env, jobject self, jlong hwnd) {
	debug("pre long nativeInit(long)");
	return (jlong)newInsn(env, hwnd, self);
	debug("post long nativeInit(long)");
}

void JNICALL Java_me_nahkd_calligraphy_windows_WindowsInkDriver_nativeDestroy(JNIEnv *env, jobject self, jlong nativeId) {
	debug("pre void nativeDestroy(long)");
	WininkDriver* insn = (WininkDriver*)nativeId;
	delete insn;
	debug("post void nativeDestroy(long)");
}
