package ch.vd.uniregctb.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Cette permet de retourner un proxy qui implémente l'interface d'un object, et qui synchronize toutes les méthodes publiques.
 *
 * @author http://stackoverflow.com/questions/743288/java-synchronization-utility
 */
public class SynchronizedFactory {

	private SynchronizedFactory() {
	}

	/**
	 * Crée un proxy qui synchronize toutes les méthodes de l'objet spécifié.
	 *
	 * @param ifCls  l'interface publique à implémenter
	 * @param object l'objet à synchroniser
	 * @param <T>    la classe concrète de l'objet
	 * @return un proxy dont les méthodes publiques sont synchronisées.
	 */
	@SuppressWarnings({"unchecked"})
	public static <T> T makeSynchronized(Class<?> ifCls, T object) {
		return (T) Proxy.newProxyInstance(
				object.getClass().getClassLoader(),
				new Class<?>[]{ifCls},
				new Handler(object));
	}

	static class Handler<T> implements InvocationHandler {
		private final T object;

		Handler(T object) {
			this.object = object;
		}

		public Object invoke(Object proxy, Method method,
		                     Object[] args) throws Throwable {
			synchronized (object) {
				return method.invoke(object, args);
			}
		}
	}

}
