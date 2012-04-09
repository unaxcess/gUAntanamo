package org.ua2.guantanamo.data;

@SuppressWarnings("serial")
public class CacheException extends RuntimeException {
	public CacheException(String msg) {
		super(msg);
	}

	public CacheException(String msg, Throwable t) {
		super(msg, t);
	}
}
