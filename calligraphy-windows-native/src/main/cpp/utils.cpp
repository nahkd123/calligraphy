#include <vector>

#ifdef _DEBUGGING_
#include <stdio.h>
#define debug(...) { fprintf(stderr, "[Native Debug] "); fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); }
#else
#define debug(...)
#endif

template <typename T>
size_t binarySearch(std::vector<T> &vec, T key, int(*comparator)(T&, T&)) {
	size_t left = 0;
	size_t right = vec.size();
	size_t middle = (left + right) / 2;

	while ((right - left) > 1) {
		if (comparator(vec[left], key) == 0) return left;
		if (comparator(vec[right], key) == 0) return right;
		int compare = comparator(vec[middle], key);

		if (compare > 0) right = middle;
		else if (compare < 0) left = middle;
		else return middle;

		middle = (left + right) / 2;
	}

	if (comparator(vec[left], key) == 0) return left;
	if (comparator(vec[right], key) == 0) return right;
	return -1;
}

template <typename T>
class fixedstack {
private:
	T *bufferStart;
	int index;
public:
	fixedstack(int size) : bufferStart(new T[size]), index(0) {}
	~fixedstack() { delete[] bufferStart; }

	void push(T e) { bufferStart[index++] = e; }
	int size() const { return index; }

	T *pack() {
		T *out = new T[index];
		for (int i = 0; i < index; i++) out[i] = bufferStart[i];
		return out;
	}
};

template <typename K, typename V>
class hashmap {
private:
	struct Entry { Entry *next; K key; V value; };
	int bucketsCount;
	int size;
	Entry **buckets;
public:
	hashmap(int bucketsC) : bucketsCount(bucketsC), size(0), buckets(new Entry*[bucketsC]) {
		for (int i = 0; i < bucketsC; i++) buckets[i] = nullptr;
	}

	~hashmap() {
		for (int i = 0; i < bucketsCount; i++) {
			while (buckets[i] != nullptr) {
				Entry *e = buckets[i];
				buckets[i] = e->next;
				delete e;
				size--;
				if (size == 0) goto outer;
			}
		}

		outer:
		delete[] buckets;
	}

	void set(K key, V value) {
		int bucketId = key % bucketsCount;
		Entry **bucket = &buckets[bucketId];

		while (*bucket != nullptr) {
			if ((*bucket)->key == key) {
				(*bucket)->value = value;
				return;
			}

			bucket = &((*bucket)->next);
		}

		*bucket = new Entry;
		(*bucket)->next = nullptr;
		(*bucket)->key = key;
		(*bucket)->value = value;
		size++;
	}

	void remove(K key) {
		int bucketId = key % bucketsCount;
		Entry **bucket = &buckets[bucketId];

		while (*bucket != nullptr) {
			if ((*bucket)->key == key) {
				Entry *e = *bucket;
				*bucket = e->next;
				delete e;
				size--;
				return;
			}

			bucket = &((*bucket)->next);
		}
	}

	V *operator[](K key) {
		int bucketId = key % bucketsCount;
		Entry **bucket = &buckets[bucketId];

		while (*bucket != nullptr) {
			if ((*bucket)->key == key) return &((*bucket)->value);
			bucket = &((*bucket)->next);
		}

		return NULL;
	}
};
