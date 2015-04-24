package ch.vd.uniregctb.migration.pm.utils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class TracingFactory<T> {

	private final Class<T> targetInterface;

	public TracingFactory(Class<T> targetInterface) {
		this.targetInterface = targetInterface;
		if (targetInterface == null || !targetInterface.isInterface()) {
			throw new IllegalArgumentException("targetInterface must be an interface!");
		}
	}

	public T buildTracing(Logger logger, T target) {
		//noinspection unchecked
		return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{targetInterface}, new Handler(logger, target));
	}

	private class Handler implements InvocationHandler {
		private final Logger logger;
		private final T target;

		public Handler(Logger logger, T target) {
			this.logger = logger;
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
				logger.info(String.format("(%d ms) %s.%s%s => %d item(s)%s",
				                          TimeUnit.NANOSECONDS.toMillis(end - start),
				                          targetInterface.getSimpleName(), method.getName(), Arrays.toString(args),
				                          nbItems, exceptionPart));
			}
		}
	}
}
