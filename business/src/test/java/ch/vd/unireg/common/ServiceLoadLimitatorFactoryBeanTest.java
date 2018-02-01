package ch.vd.unireg.common;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class ServiceLoadLimitatorFactoryBeanTest extends WithoutSpringTest {

	private static <T> ServiceLoadLimitatorFactoryBean<T> buildLimitatorFactory(Class<T> interfaceClass, T hiddenImplementation, int poolSize, String serviceName) throws Exception {
		final ServiceLoadLimitatorFactoryBean<T> factory = new ServiceLoadLimitatorFactoryBean<>();
		factory.setPoolSize(poolSize);
		factory.setServiceInterfaceClass(interfaceClass);
		factory.setServiceName(serviceName);
		factory.setTarget(hiddenImplementation);
		factory.afterPropertiesSet();
		return factory;
	}

	private static final class CheckedTestException extends Exception {
		private CheckedTestException(String message) {
			super(message);
		}
	}

	private static final class UncheckedTestException extends RuntimeException {
		private UncheckedTestException(String message) {
			super(message);
		}
	}

	private interface TestService {
		String doJob(int seed);
	}

	private interface TestServiceWithCheckedException {
		String doJobWithException(int seed) throws CheckedTestException;
	}

	@Test
	public void testCallTransmission() throws Exception {
		final TestService myImplementation = new TestService() {
			@Override
			public String doJob(int seed) {
				return Integer.toString(seed) + ". Done.";
			}
		};

		final ServiceLoadLimitatorFactoryBean<TestService> factory = buildLimitatorFactory(TestService.class, myImplementation, 1, "TestService");
		try {
			final TestService service = factory.getObject();
			Assert.assertEquals("42. Done.", service.doJob(42));
			Assert.assertEquals("56. Done.", service.doJob(56));
			Assert.assertEquals("-1247. Done.", service.doJob(-1247));
		}
		finally {
			factory.destroy();
		}
	}

	@Test
	public void testCallParallelismLimitation() throws Exception {
		final AtomicInteger load = new AtomicInteger(0);
		final int POOL_SIZE = 10;
		final int NB_CLIENTS = POOL_SIZE * 10;
		final TestService myImplementation = new TestService() {
			@Override
			public String doJob(int seed) {
				final int currentLoad = load.incrementAndGet();
				try {
					Thread.sleep(100);
					Assert.assertTrue("Valeur vue : " + currentLoad, currentLoad <= POOL_SIZE);
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				finally {
					load.decrementAndGet();
				}
				return "OK";
			}
		};

		final ServiceLoadLimitatorFactoryBean<TestService> factory = buildLimitatorFactory(TestService.class, myImplementation, POOL_SIZE, "TestService");
		try {
			final TestService service = factory.getObject();

			// envoi de la sauce
			final ExecutorService executor = Executors.newFixedThreadPool(NB_CLIENTS);
			final List<Future<?>> tasks = new LinkedList<>();
			try {
				for (int i = 0 ; i < NB_CLIENTS ; ++ i) {
					final int index = i;
					tasks.add(executor.submit(new Runnable() {
						@Override
						public void run() {
							service.doJob(index);
						}
					}));
				}
			}
			finally {
				executor.shutdown();
			}

			// vérification que rien n'a sauté
			for (Future<?> task : tasks) {
				task.get();
			}
		}
		finally {
			factory.destroy();
		}
	}

	@Test
	public void testThrownCheckedException() throws Exception {
		final TestServiceWithCheckedException myImplementation = new TestServiceWithCheckedException() {
			@Override
			public String doJobWithException(int seed) throws CheckedTestException {
				throw new CheckedTestException("Boom!!!");
			}
		};

		final ServiceLoadLimitatorFactoryBean<TestServiceWithCheckedException> factory = buildLimitatorFactory(TestServiceWithCheckedException.class, myImplementation, 1, "TestService");
		try {
			final TestServiceWithCheckedException service = factory.getObject();
			service.doJobWithException(0);
			Assert.fail("Une exception aurait dû sauter...");
		}
		catch (CheckedTestException e) {
			Assert.assertEquals("Boom!!!", e.getMessage());
		}
		finally {
			factory.destroy();
		}
	}

	@Test
	public void testThrownUncheckedException() throws Exception {
		final TestService myImplementation = new TestService() {
			@Override
			public String doJob(int seed) {
				throw new UncheckedTestException("Badaboom!!!");
			}
		};

		final ServiceLoadLimitatorFactoryBean<TestService> factory = buildLimitatorFactory(TestService.class, myImplementation, 1, "TestService");
		try {
			final TestService service = factory.getObject();
			service.doJob(0);
			Assert.fail("Une exception aurait dû sauter...");
		}
		catch (UncheckedTestException e) {
			Assert.assertEquals("Badaboom!!!", e.getMessage());
		}
		finally {
			factory.destroy();
		}
	}
}