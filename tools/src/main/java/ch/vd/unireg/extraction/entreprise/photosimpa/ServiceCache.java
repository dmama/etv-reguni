package ch.vd.unireg.extraction.entreprise.photosimpa;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.common.LockHelper;

public abstract class ServiceCache {

	private ServiceCache() {
	}

	/**
	 * Construction d'un cache sur l'interface donnée de la target
	 * @param cachedInterface interface à cacher
	 * @param target target à appeler pour construire le cache peu à peu
	 * @param <T> type de l'interface à cacher
	 * @return une implémentation du cache
	 */
	public static <T> T of(Class<T> cachedInterface, T target) {
		if (!cachedInterface.isInterface()) {
			throw new IllegalArgumentException("Class " + cachedInterface.getName() + " should be an interface!");
		}
		if (target == null) {
			throw new NullPointerException("Target should not be null");
		}
		if (!cachedInterface.isAssignableFrom(target.getClass())) {
			throw new IllegalArgumentException("Target should implement the " + cachedInterface.getName() + " interface!");
		}
		//noinspection unchecked
		return (T) Proxy.newProxyInstance(cachedInterface.getClassLoader(), new Class[] {cachedInterface}, new Handler<>(target));
	}

	/**
	 * La clé qui détermine si un appel donné (= méthode + paramètres) a déjà été fait
	 */
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

	private static class Handler<T> implements InvocationHandler {

		private final T target;
		private final Map<Key, Object> cache = new HashMap<>();
		private final LockHelper lockHelper = new LockHelper();

		public Handler(T target) {
			this.target = target;
		}

		/**
		 * Invocation de l'implémentation "target" si la valeur demandée n'est pas déjà en cache (sinon, on la prend du cache, justement)
		 * @param proxy instance de proxy
		 * @param method la méthode à appeler
		 * @param args les arguments à passer à la méthode en question
		 * @return la valeur renvoyée par l'implémentation "target" ou directement prise dans le cache
		 * @throws Throwable l'exception renvoyée, éventuellement, par l'implémentation "target"
		 */
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
			return lockHelper.doInReadLock(() -> {
				final Supplier<Object> supplier;
				if (cache.containsKey(key)) {
					final Object value = cache.get(key);
					supplier = () -> value;
				}
				else {
					supplier = null;
				}
				return supplier;
			});
		}

		private void addToCache(Key key, Object value) {
			lockHelper.doInWriteLock(() -> cache.put(key, value));
		}
	}
}
