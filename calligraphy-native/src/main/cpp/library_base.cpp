#include "library_base.h"

bool operator ==(const PenPacketData &a, const PenPacketData &b) {
	return a.pressure == b.pressure &&
			a.maxPressure == b.maxPressure &&
			a.tiltX == b.tiltX &&
			a.tiltY == b.tiltY &&
			a.flags == b.flags;
}
