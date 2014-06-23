package ch.vd.uniregctb.common;

import java.lang.reflect.InvocationHandler;
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

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.load.BasicLoadMonitor;
import ch.vd.uniregctb.load.LoadAverager;
import ch.vd.uniregctb.load.LoadMonitorable;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceLoadLimitatorFactoryBean<T> implements FactoryBean<T>, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(ServiceLoadLimitatorFactoryBean.class);
	private static final long WAITING_TIME_LOG_THRESHOLD_MS = 1000;     // millisecondes

	private Class<T> serviceInterfaceClass;
	private int nbThreadsMin;
	private int nbThreadsMax;
	private long keepAliveTime = TimeUnit.SECONDS.toMillis(30);
	private String serviceName;
	private Object target;
	private StatsService statsService;

	private final AtomicInteger load = new AtomicInteger(0);
	private ExecutorService executor;
	private T proxy;
	private LoadAverager runningAverager;
	private LoadAverager waitingAverager;

	public void setServiceInterfaceClass(Class<T> serviceInterfaceClass) {
		this.serviceInterfaceClass = serviceInterfaceClass;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setNbThreadsMin(int nbThreadsMin) {
		this.nbThreadsMin = nbThreadsMin;
	}

	public void setNbThreadsMax(int nbThreadsMax) {
		this.nbThreadsMax = nbThreadsMax;
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
		if (nbThreadsMin < 0 || nbThreadsMax < 1 || nbThreadsMin > nbThreadsMax) {
			throw new IllegalArgumentException(String.format("Invalid pair min/max threads : %d/%d", nbThreadsMin, nbThreadsMax));
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
		this.executor = new ThreadPoolExecutor(nbThreadsMin, nbThreadsMax, keepAliveTime, TimeUnit.MILLISECONDS, workQueue, new DefaultThreadFactory(new DefaultThreadNameGenerator(serviceName)));

		//noinspection unchecked
		this.proxy = (T) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[] {serviceInterfaceClass}, new ServiceInvocationHandler());

		if (statsService != null) {
			// 1. le nombre d'appels en cours
			{
				final String name = buildRunningServiceName();
				final LoadMonitorable service = new LoadMonitorable() {
					@Override
					public int getLoad() {
						return load.get();
					}
				};
				runningAverager = new LoadAverager(service, name, 600, 500);
				runningAverager.start();
				statsService.registerLoadMonitor(name, new BasicLoadMonitor(service, runningAverager));
			}

			// 2. le nombre d'appels en attente
			{
				final String name = buildWaitingServiceName();
				final LoadMonitorable service = new LoadMonitorable() {
					@Override
					public int getLoad() {
						return workQueue.size();
					}
				};
				waitingAverager = new LoadAverager(service, name, 600, 500);
				waitingAverager.start();
				statsService.registerLoadMonitor(name, new BasicLoadMonitor(service, waitingAverager));
			}
		}
	}

	private String buildWaitingServiceName() {
		return String.format("%s-Waiting", serviceName);
	}

	private String buildRunningServiceName() {
		return String.format("%s-Running", serviceName);
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
		if (executor != null) {
			executor.shutdownNow();
			while (!executor.isTerminated()) {
				executor.awaitTermination(1, TimeUnit.SECONDS);
			}
		}
	}

	private final class Invocator implements Callable<Object>, Dated {

		private final long birth = getNowSystemNanos();
		private final Method method;
		private final Object[] args;

		private Invocator(Method method, Object[] args) {
			this.method = method;
			this.args = args;
		}

		@Override
		public Object call() throws Exception {
			// délai de plus d'une seconde, on le note
			final long waitingTime = getAge(TimeUnit.MILLISECONDS);
			if (waitingTime >= WAITING_TIME_LOG_THRESHOLD_MS) {
				LOGGER.warn(String.format("Attente constatée de %d ms avant l'appel à la méthode %s du service %s", waitingTime, method.getName(), serviceName));
			}

			// maintenant, on fait effectivement l'appel...
			load.incrementAndGet();
			try {
				return method.invoke(target, args);
			}
			finally {
				load.decrementAndGet();
			}
		}

		@Override
		public long getAge(@NotNull TimeUnit unit) {
			final long now = getNowSystemNanos();
			return unit.convert(now - birth, TimeUnit.NANOSECONDS);
		}
	}

	private static long getNowSystemNanos() {
		return System.nanoTime();
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
