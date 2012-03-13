package ch.vd.uniregctb.cache;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("JavaDoc")
public class CacheValueWithPartsTest extends WithoutSpringTest {

	private final Logger LOGGER = Logger.getLogger(CacheValueWithPartsTest.class);

	private static final int ITERATIONS = 1000;

	/**
	 * [SIFISC-4452] Test non-déterministe pour s'assurer qu'il n'y a pas de problème de thread-safety sur l'ajout et la demande de parts sur la classe CacheValueWithParts.
	 * En cas de bug, le symptôme le plus courant est un ou plusieurs threads qui partent en boucle infinie.
	 */
	@Test(timeout = 60000)
	public void testPartsManipulationThreadSafety() throws Exception {

		for (int i = 0; i < 100; ++i) {
			try {
				++i;
//				System.out.println("*** RUN " + i + " ***");
				run();
			}
			catch (Exception e) {
				LOGGER.error("Run " + i + ": " + e.getMessage(), e);
			}
		}
	}

	private void run() throws Exception {
		final Set<Integer> parts = new HashSet<Integer>();
		parts.add(1);

		final CacheValueWithParts<Data, Integer> value = new CacheValueWithParts<Data, Integer>(parts, new Data(5)) {
			@Override
			protected void copyParts(Set<Integer> parts, Data from, Data to) {
			}

			@Override
			protected Data restrictTo(Data value, Set<Integer> parts) {
				return value;
			}
		};


		Thread writer1 = new Thread() {
			@Override
			public void run() {
				final Random rand = new Random(System.currentTimeMillis());
				setName("writer1");
//				LOGGER.warn("-- writer1 starts ---");
				try {
					for (int i = 0; i < ITERATIONS; ++i) {
						final Set<Integer> parts = new HashSet<Integer>();
						parts.add(rand.nextInt());
						value.addParts(parts, new Data(5));
					}
				}
				finally {
//					LOGGER.warn("-- writer1 ends ---");
				}
			}
		};

		Thread writer2 = new Thread() {
			@Override
			public void run() {
				final Random rand = new Random(System.currentTimeMillis());
				setName("writer2");
//				LOGGER.warn("-- writer2 starts ---");
				try {
					for (int i = 0; i < ITERATIONS; ++i) {
						final Set<Integer> parts = new HashSet<Integer>();
						parts.add(rand.nextInt());
						value.addParts(parts, new Data(5));
					}
				}
				finally {
//					LOGGER.warn("-- writer2 ends ---");
				}
			}
		};

		Thread reader = new Thread() {
			@Override
			public void run() {
				final Random rand = new Random(System.currentTimeMillis());
				setName("reader");
//				LOGGER.warn("-- reader starts ---");
				try {
					for (int i = 0; i < ITERATIONS * 10; ++i) {
						final Set<Integer> parts = new HashSet<Integer>();
						parts.add(rand.nextInt());
						try {
							assertEquals(5, value.getValueForParts(parts).i);
						}
						catch (IllegalArgumentException e) {
							// on ignore cette exception, le but n'est pas de tester la cohérence des parts, mais l'appel à this.parts.containsAll()
						}
					}
				}
				finally {
//					LOGGER.warn("-- reader ends ---");
				}
			}
		};

		final ExceptionHandler readerExceptionHandler = new ExceptionHandler();
		final ExceptionHandler writer1ExceptionHandler = new ExceptionHandler();
		final ExceptionHandler writer2ExceptionHandler = new ExceptionHandler();
		reader.setUncaughtExceptionHandler(readerExceptionHandler);
		writer1.setUncaughtExceptionHandler(writer1ExceptionHandler);
		writer2.setUncaughtExceptionHandler(writer2ExceptionHandler);

		writer1.start();
		writer2.start();
		reader.start();

		waitForThread(writer1, writer1ExceptionHandler);
		waitForThread(writer2, writer2ExceptionHandler);
		waitForThread(reader, readerExceptionHandler);
	}

	private void waitForThread(Thread thread, ExceptionHandler exceptionHandler) throws Exception {
		while (thread.isAlive()) {
			thread.join(10);
		}
		Exception exception = exceptionHandler.getException();
		if (exception != null) {
			throw exception;
		}
	}

	private static class ExceptionHandler implements Thread.UncaughtExceptionHandler {

		private final Logger LOGGER = Logger.getLogger(ExceptionHandler.class);

		private Exception exception = null;

		@Override
		public void uncaughtException(Thread t, Throwable e) {
//			LOGGER.error(e.getMessage(), e);
			exception = new RuntimeException(e);
		}

		public Exception getException() {
			return exception;
		}
	}

	/**
	 * Une classe de donnée calibrée pour provoquer beaucoup de collisions lors du hachage.
	 */
	private static class Data {

		private int i;

		private Data(int i) {
			this.i = i;
		}

		@Override
		public int hashCode() {
			return Integer.valueOf(i).hashCode() % 100;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final Data data = (Data) o;
			return i == data.i;
		}
	}

//	@Test
//	public void testHashMap() throws Exception {
//
//		final Set<Data> set = new HashSet<Data>();
//		set.add(new Data(1));
//		set.add(new Data(2));
//		set.add(new Data(3));
//
//		Thread writer = new Thread() {
//			@Override
//			public void run() {
//				setName("writer");
//				LOGGER.warn("-- writer starts ---");
//				try {
//					for (int i = 0; i < ITERATIONS; ++i) {
//						set.add(new Data(i));
//					}
//					for (int i = ITERATIONS - 1; i >= 0; --i) {
//						set.remove(new Data(i));
//					}
//				}
//				finally {
//					LOGGER.warn("-- writer ends ---");
//				}
//			}
//		};
//
//		Thread reader = new Thread() {
//			@Override
//			public void run() {
//				setName("reader");
//				LOGGER.warn("-- reader starts ---");
//				try {
//					int g = 0;
//					for (int i = 0; i < ITERATIONS * 8; ++i) {
//						boolean c = set.contains(new Data(i));
//						if (c) {
//							g++;
//						}
//					}
//					LOGGER.trace(g);
//				}
//				finally {
//					LOGGER.warn("-- reader ends ---");
//				}
//			}
//		};
//
//		final ExceptionHandler readerExceptionHandler = new ExceptionHandler();
//		final ExceptionHandler writerExceptionHandler = new ExceptionHandler();
//		reader.setUncaughtExceptionHandler(readerExceptionHandler);
//		writer.setUncaughtExceptionHandler(writerExceptionHandler);
//
//		writer.start();
//		reader.start();
//
//		waitForThread(writer, writerExceptionHandler);
//		waitForThread(reader, readerExceptionHandler);
//	}
}
