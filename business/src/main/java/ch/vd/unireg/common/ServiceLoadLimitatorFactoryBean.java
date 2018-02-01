package ch.vd.unireg.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.load.BasicLoadMonitor;
import ch.vd.unireg.load.LoadAverager;
import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

public class ServiceLoadLimitatorFactoryBean<T> implements FactoryBean<T>, InitializingBean, DisposableBean {

	private Class<T> serviceInterfaceClass;
	private int poolSize;
	private long keepAliveTime = TimeUnit.SECONDS.toMillis(30);
	private String serviceName;
	private Object target;
	private StatsService statsService;

	private final AtomicInteger load = new AtomicInteger(0);
	private ExecutorService executor;
	private T proxy;
	private LoadAverager runningAverager;
	private LoadAverager waitingAverager;
	private ServiceTracing waitingTracing;

	public void setServiceInterfaceClass(Class<T> serviceInterfaceClass) {
		this.serviceInterfaceClass = serviceInterfaceClass;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void setKeepAliveTime(long keepAliveTime) {
		this.keepAliveTime = keepAliveTime;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (poolSize < 1) {
			throw new IllegalArgumentException(String.format("Invalid pool size : %d", poolSize));
		}
		if (serviceInterfaceClass == null || !serviceInterfaceClass.isInterface()) {
			throw new IllegalArgumentException("Service interface class attribute should be set with an interface.");
		}
		if (target == null) {
			throw new IllegalArgumentException("Missing target!");
		}
		if (!serviceInterfaceClass.isAssignableFrom(target.getClass())) {
			throw new IllegalArgumentException("Target should implement the " + serviceInterfaceClass.getName() + " interface!");
		}

		final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();
		this.executor = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTime, TimeUnit.MILLISECONDS, workQueue, new DefaultThreadFactory(new DefaultThreadNameGenerator(serviceName)));

		//noinspection unchecked
		this.proxy = (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[] {serviceInterfaceClass}, new ServiceInvocationHandler());

		if (statsService != null) {
			// 1. le nombre d'appels en cours
			{
				final String name = buildRunningServiceName();
				final LoadMonitorable service = load::get;
				runningAverager = new LoadAverager(service, name, 600, 500);
				runningAverager.start();
				statsService.registerLoadMonitor(name, new BasicLoadMonitor(service, runningAverager));
			}

			// 2. le nombre d'appels en attente
			{
				final String name = buildWaitingServiceName();
				final LoadMonitorable service = workQueue::size;
				waitingAverager = new LoadAverager(service, name, 600, 500);
				waitingAverager.start();
				statsService.registerLoadMonitor(name, new BasicLoadMonitor(service, waitingAverager));
			}
		}

		// 3. le temps d'attente avant de faire l'appel pour cause de pool de threads plein
		{
			final String name = buildWaitingServiceName();
			waitingTracing = new ServiceTracing(name);
			if (statsService != null) {
				statsService.registerService(name, waitingTracing);
			}
		}
	}

	private String buildWaitingServiceName() {
		return String.format("%s-Wait", serviceName);
	}

	private String buildRunningServiceName() {
		return serviceName;
	}

	@Override
	public void destroy() throws Exception {
		if (runningAverager != null) {
			runningAverager.stop();
			statsService.unregisterLoadMonitor(buildRunningServiceName());
		}
		if (waitingAverager != null) {
			waitingAverager.stop();
			statsService.unregisterLoadMonitor(buildWaitingServiceName());
		}
		if (statsService != null) {
			statsService.unregisterService(buildWaitingServiceName());
		}
		if (executor != null) {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	private final class Invocator implements Callable<Object> {

		private final long birth = waitingTracing.start();
		private final Method method;
		private final Object[] args;

		private Invocator(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		@Override
		public Object call() throws Exception {
			// l'attente est terminée
			waitingTracing.end(birth);

			// maintenant, on fait effectivement l'appel...
			load.incrementAndGet();
			try {
				return method.invoke(target, args);
			}
			catch (InvocationTargetException e) {
				final Throwable t  = e.getCause();
				if (t instanceof Exception) {
					throw (Exception) t;
				}
				else if (t instanceof Error) {
					throw (Error) t;
				}
				else {
					// on laisse passer la InvocationTargetException car la cause n'est pas exposable ici...
					throw e;
				}
			}
			finally {
				load.decrementAndGet();
			}
		}
	}

	/**
	 * Séparation des appels sur un nombre fini de threads simultanés
	 */
	private final class ServiceInvocationHandler implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			// enregistrement de la demande d'appel au service
			final Future<?> future = executor.submit(new Invocator(method, args));

			try {
				// attente de la réponse du service
				return future.get();
			}
			catch (ExecutionException e) {
				// forcément contenu dans la liste des exceptions déclarées par la méthode, puisqu'envoyé directement depuis la méthode
			    throw e.getCause();
			}
			catch (InterruptedException e) {
				throw new RuntimeException("Interrupted thread", e);
			}
		}
	}

	@Override
	public T getObject() throws Exception {
		return proxy;
	}

	@Override
	public Class<T> getObjectType() {
		return serviceInterfaceClass;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
