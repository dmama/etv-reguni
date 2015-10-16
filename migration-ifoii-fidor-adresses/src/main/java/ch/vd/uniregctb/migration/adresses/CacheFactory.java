package ch.vd.uniregctb.migration.adresses;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class CacheFactory<T> {

	private final Class<T> targetInterface;

	public CacheFactory(Class<T> targetInterface) {
		this.targetInterface = targetInterface;
		if (targetInterface == null || !targetInterface.isInterface()) {
			throw new IllegalArgumentException("targetInterface must be an interface!");
		}
	}

	public T buildCache(T target) {
		//noinspection unchecked
		return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {targetInterface}, new Handler(target));
	}

	private static final class Key {
		private final Method method;
		private final Object[] args;

		private Key(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Key key = (Key) o;
			return Arrays.equals(args, key.args) && method.equals(key.method);
		}

		@Override
		public int hashCode() {
			int result = method.hashCode();
			result = 31 * result + (args != null ? Arrays.hashCode(args) : 0);
			return result;
		}
	}

	private static interface CachedElement {
		Object getPayload();
	}

	private class Handler implements InvocationHandler {

		private final T target;
		private final Map<Key, Object> cache = new HashMap<>();
		private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

		private Handler(T target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			final Key key = new Key(method, args);
			final CachedElement inCache = findInCache(key);
			if (inCache == null) {
				try {
					final Object value = method.invoke(target, args);
					addToCache(key, value);
					return value;
				}
				catch (InvocationTargetException e) {
					throw e.getCause();
				}
			}
			else {
				return inCache.getPayload();
			}
		}

		private CachedElement findInCache(Key key) {
			final Lock lock = rwLock.readLock();
			lock.lock();
			try {
				if (cache.containsKey(key)) {
					final Object value = cache.get(key);
					return new CachedElement() {
						@Override
						public Object getPayload() {
							return value;
						}
					};
				}
				else {
					return null;
				}
			}
			finally {
				lock.unlock();
			}
		}

		private void addToCache(Key key, Object value) {
			final Lock lock = rwLock.writeLock();
			lock.lock();
			try {
				cache.put(key, value);
			}
			finally {
				lock.unlock();
			}
		}
	}
}
