package ch.vd.uniregctb.migration.pm.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public final class CacheFactory<T> implements FactoryBean<T>, InitializingBean {

	private final Class<T> targetInterface;
	private final T target;

	private T proxy;

	public CacheFactory(Class<T> targetInterface, T target) {
		this.targetInterface = targetInterface;
		this.target = target;
		if (targetInterface == null || !targetInterface.isInterface()) {
			throw new IllegalArgumentException("targetInterface must be an interface!");
		}
		if (target == null) {
			throw new IllegalArgumentException("target must not be null!");
		}
		if (!targetInterface.isAssignableFrom(target.getClass())) {
			throw new IllegalArgumentException("target must implement " + targetInterface.getName() + " interface!");
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		//noinspection unchecked
		this.proxy = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{targetInterface}, new Handler(target));
	}

	@Override
	public T getObject() throws Exception {
		return proxy;
	}

	@Override
	public Class<T> getObjectType() {
		return targetInterface;
	}

	@Override
	public boolean isSingleton() {
		return true;
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
			final Supplier<Object> inCache = findInCache(key);
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
				return inCache.get();
			}
		}

		@Nullable
		private Supplier<Object> findInCache(Key key) {
			final Lock lock = rwLock.readLock();
			lock.lock();
			try {
				if (cache.containsKey(key)) {
					final Object value = cache.get(key);
					return () -> value;
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
