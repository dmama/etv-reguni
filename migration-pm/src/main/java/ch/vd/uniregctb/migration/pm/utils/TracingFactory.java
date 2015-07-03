package ch.vd.uniregctb.migration.pm.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public final class TracingFactory<T> implements FactoryBean<T>, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(TracingFactory.class);

	private final Class<T> targetInterface;
	private final T target;

	private T proxy;

	public TracingFactory(Class<T> targetInterface, T target) {
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
		this.proxy = (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{targetInterface}, this::invokeTarget);
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

	/**
	 * Invocation de l'implémentation "target" en prenant soin de mesurer le temps écoulé et de logguer les paramètres d'appel
	 * @param proxy instance de proxy
	 * @param method la méthode à appeler
	 * @param args les arguments à passer à la méthode en question
	 * @return la valeur renvoyée par l'implémentation "target"
	 * @throws Throwable l'exception renvoyée, éventuellement, par l'implémentation "target"
	 */
	private Object invokeTarget(Object proxy, Method method, Object[] args) throws Throwable {
		final long start = System.nanoTime();
		Throwable t = null;
		int nbItems = 0;
		try {
			final Object result = method.invoke(target, args);
			if (result != null) {
				if (result instanceof Collection) {
					nbItems = ((Collection) result).size();
				}
				else {
					nbItems = 1;
				}
			}
			return result;
		}
		catch (InvocationTargetException e) {
			t = e.getCause();
			throw t;
		}
		catch (Throwable e) {
			t = e;
			throw t;
		}
		finally {
			final long end = System.nanoTime();
			final String exceptionPart = t == null ? StringUtils.EMPTY : String.format(", %s thrown", t.getClass().getName());
			LOGGER.info(String.format("(%d ms) %s.%s%s => %d item(s)%s",
			                          TimeUnit.NANOSECONDS.toMillis(end - start),
			                          targetInterface.getSimpleName(), method.getName(), Arrays.toString(args),
			                          nbItems, exceptionPart));
		}
	}
}
