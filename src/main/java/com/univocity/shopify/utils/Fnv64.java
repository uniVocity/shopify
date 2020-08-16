package com.univocity.shopify.utils;

public class Fnv64 {

	public static final long FNV_64_INIT = 0xcbf29ce484222325L;
	public static final long FNV_64_PRIME = 0x100000001b3L;

	public static final long hash(CharSequence buffer) {
		return hash(FNV_64_INIT, buffer);
	}

	public static final long hash(byte[] buffer) {
		return hash(FNV_64_INIT, buffer);
	}

	public static final long hash(char[] buffer) {
		return hash(FNV_64_INIT, buffer);
	}

	public static final long hash(long hash, CharSequence buffer) {
		return buffer != null ? hash(hash, buffer, 0, buffer.length()) : hash;
	}

	public static final long hash(long hash, byte[] buffer) {
		return buffer != null ? hash(hash, buffer, 0, buffer.length) : hash;
	}

	public static final long hash(long hash, char[] buffer) {
		return buffer != null ? hash(hash, buffer, 0, buffer.length) : hash;
	}

	public static final long hash(CharSequence buffer, int from, int to) {
		return hash(FNV_64_INIT, buffer, from, to);
	}

	public static final long hash(byte[] buffer, int from, int to) {
		return hash(FNV_64_INIT, buffer, from, to);
	}

	public static final long hash(char[] buffer, int from, int to) {
		return hash(FNV_64_INIT, buffer, from, to);
	}

	public static final long hash(long hash, char[] buffer, int from, int to) {
		final int tail = (to - from) & 3; //fast modulo operation: dividend & (divisor - 1);
		final int last = to - tail;

		while (true) {
			if (from >= last) {
				switch (tail) {
					case 3:
						return (hash ^ (buffer[last] | buffer[last + 1] << 8 | buffer[last + 2] << 16)) * FNV_64_PRIME;
					case 2:
						return (hash ^ (buffer[last] | buffer[last + 1] << 8)) * FNV_64_PRIME;
					case 1:
						return (hash ^ buffer[last]) * FNV_64_PRIME;
					default:
						return hash;
				}
			}
			hash = (hash ^ (buffer[from] | buffer[from + 1] << 8 | buffer[from + 2] << 16 | buffer[from + 3] << 24)) * FNV_64_PRIME;
			from += 4;
		}
	}

	public static final long hash(long hash, CharSequence buffer, int from, int to) {
		final int tail = (to - from) & 3; //fast modulo operation: dividend & (divisor - 1);
		final int last = to - tail;

		while (true) {
			if (from >= last) {
				switch (tail) {
					case 3:
						return (hash ^ (buffer.charAt(last) | buffer.charAt(last + 1) << 8 | buffer.charAt(last + 2) << 16)) * FNV_64_PRIME;
					case 2:
						return (hash ^ (buffer.charAt(last) | buffer.charAt(last + 1) << 8)) * FNV_64_PRIME;
					case 1:
						return (hash ^ buffer.charAt(last)) * FNV_64_PRIME;
					default:
						return hash;
				}
			}
			hash = (hash ^ (buffer.charAt(from) | buffer.charAt(from + 1) << 8 | buffer.charAt(from + 2) << 16 | buffer.charAt(from + 3) << 24)) * FNV_64_PRIME;
			from += 4;
		}
	}

	public static final long hash(long hash, byte[] buffer, int from, int to) {
		final int tail = (to - from) & 7; //fast modulo operation: dividend & (divisor - 1);
		final int last = to - tail;

		while (true) {
			if (from >= last) {
				switch (tail) {
					case 7:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8 | buffer[last + 3] << 12 | buffer[last + 4] << 16 | buffer[last + 5] << 20 | buffer[last + 6] << 24)) * FNV_64_PRIME;
					case 6:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8 | buffer[last + 3] << 12 | buffer[last + 4] << 16 | buffer[last + 5] << 20)) * FNV_64_PRIME;
					case 5:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8 | buffer[last + 3] << 12 | buffer[last + 4] << 16)) * FNV_64_PRIME;
					case 4:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8 | buffer[last + 3] << 12)) * FNV_64_PRIME;
					case 3:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8)) * FNV_64_PRIME;
					case 2:
						return (hash ^ (buffer[last] | buffer[last + 1] << 4)) * FNV_64_PRIME;
					case 1:
						return (hash ^ buffer[last]) * FNV_64_PRIME;
					default:
						return hash;
				}
			}
			hash = (hash ^ (buffer[last] | buffer[last + 1] << 4 | buffer[last + 2] << 8 | buffer[last + 3] << 12 | buffer[last + 4] << 16 | buffer[last + 5] << 20 | buffer[last + 6] << 24 | buffer[last + 7] << 28)) * FNV_64_PRIME;
			from += 8;
		}
	}
}
