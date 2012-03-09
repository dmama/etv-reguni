package ch.vd.uniregctb.testing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.Assert;

import ch.vd.uniregctb.scheduler.JobDefinition;

public class InContainerTestingJob extends JobDefinition {

	private static final Logger LOGGER = Logger.getLogger(InContainerTestingJob.class);

	public static final String NAME = "IT-InContainerTestingJob";
	private static final String CATEGORIE = "Test";

	private List<InContainerTest> tests;

	private PlatformTransactionManager transactionManager;

	private TransactionListener transactionListener = null;

	protected final PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public final void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public InContainerTestingJob(String name, int sortOrder, String descr) {
		super(name, CATEGORIE, sortOrder, descr);
	}

	@Override
	synchronized public void doExecute(Map<String, Object> params) throws Exception {
		transactionListener = new TransactionListener();
		boolean error = false;
		String methodsInError = "";
		try {
			for (InContainerTest test : tests) {
				boolean err = runMethod(test);
				if (err) {
					if (!methodsInError.isEmpty()) {
						methodsInError += " ";
					}
					methodsInError += test.getClass().getSimpleName();
				}
				error |= err;
			}
		}
		finally {
			transactionListener = null;
		}

		if (error) {
			throw new RuntimeException("Error while running tests: "+methodsInError);
		}
	}

	private boolean runMethod(InContainerTest test) throws Exception {
		LOGGER.info("Running test: " + test.getClass().getSimpleName());
		boolean error = false;
		test.setTransactionManager(getTransactionManager());
		Method[] methods = test.getClass().getMethods();
		for (Method method : methods) {
			if ( !method.isAnnotationPresent(Test.class) )
				continue;
			try {
				LOGGER.info("Run method: " + test.getClass().getSimpleName()+ '.' +method.getName());
				runBefore(test, method);
				LOGGER.info("Test SUCCESS: " + test.getClass().getSimpleName()+ '.' +method.getName());
			}
			catch (InvocationTargetException e) {
				error = true;
				LOGGER.error("ERROR: Method "+test.getClass().getSimpleName()+ '.' +method.getName(), e);
			}
			catch (Exception e) {
				error = true;
				LOGGER.error("Method "+test.getClass().getSimpleName()+ '.' +method.getName()+" : "+e.getMessage());
				LOGGER.debug(e, e);
			}
			finally {
				if (transactionListener != null)
					transactionListener.afterTestMethod(method);
				test.onTearDown();
			}
		}
		return error;
	}

	private void runBefore(InContainerTest test, Method testmethod) throws Exception {
		test.onSetUp();
		transactionListener.beforeTestMethod(testmethod);
		testmethod.invoke(test);
		LOGGER.info("Test SUCCESS: " + test.getClass().getSimpleName());
	}

	public void setTests(List<InContainerTest> tests) {
		this.tests = tests;
	}

	public class TransactionListener {

		private final Logger logger = Logger.getLogger(TransactionListener.class);

		private volatile int transactionsStarted = 0;

		private final Map<Method, TransactionContext> transactionContextCache = Collections
				.synchronizedMap(new IdentityHashMap<Method, TransactionContext>());

		public void beforeTestMethod(final Method testMethod) throws Exception {
			Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

			if (this.transactionContextCache.remove(testMethod) != null) {
				throw new IllegalStateException("Cannot start new transaction without ending existing transaction: "
						+ "Invoke endTransaction() before startNewTransaction().");
			}

			if (testMethod.isAnnotationPresent(NotTransactional.class)) {
				return;
			}

			DefaultTransactionAttribute transactionDefinition =  new DefaultTransactionAttribute ();
			transactionDefinition.setName(testMethod.getClass().getSimpleName()+ '.' +testMethod.getName());

			if (transactionDefinition != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Explicit transaction definition [" + transactionDefinition + "] found for test context [" + testMethod
							+ ']');
				}
				TransactionContext txContext = new TransactionContext(getTransactionManager(), transactionDefinition);
				startNewTransaction(testMethod, txContext);
				this.transactionContextCache.put(testMethod, txContext);
			}
		}

		public void afterTestMethod(Method testMethod) throws Exception {
			Assert.notNull(testMethod, "The test method of the supplied TestContext must not be null");

			// If the transaction is still active...
			TransactionContext txContext = this.transactionContextCache.remove(testMethod);
			if (txContext != null && !txContext.transactionStatus.isCompleted()) {
				endTransaction(testMethod, txContext);
			}
		}

		private void startNewTransaction(Method testMethod, TransactionContext txContext) throws Exception {
			txContext.startTransaction();
			++this.transactionsStarted;
			if (logger.isInfoEnabled()) {
				logger.info("Began transaction (" + this.transactionsStarted + "): transaction manager [" + txContext.transactionManager
						+ "]; rollback [" + isRollback(testMethod) + ']');
			}
		}

		private void endTransaction(Method testMethod, TransactionContext txContext) throws Exception {
			boolean rollback = isRollback(testMethod);
			if (logger.isTraceEnabled()) {
				logger.trace("Ending transaction for test context [" + testMethod + "]; transaction manager ["
						+ txContext.transactionStatus + "]; rollback [" + rollback + ']');
			}
			txContext.endTransaction(rollback);
			if (logger.isInfoEnabled()) {
				logger.info((rollback ? "Rolled back" : "Committed") + " transaction after test execution for test context [" + testMethod
						+ ']');
			}
		}

		protected final boolean isRollback(Method testMethod) throws Exception {
			boolean rollback = (Boolean) AnnotationUtils.getDefaultValue(Rollback.class);
			Rollback rollbackAnnotation = testMethod.getAnnotation(Rollback.class);
			if (rollbackAnnotation != null) {
				boolean rollbackOverride = rollbackAnnotation.value();
				if (logger.isDebugEnabled()) {
					logger.debug("Method-level @Rollback(" + rollbackOverride + ") overrides default rollback [" + rollback
							+ "] for test context [" + testMethod + ']');
				}
				rollback = rollbackOverride;
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("No method-level @Rollback override: using default rollback [" + rollback + "] for test context ["
							+ testMethod + ']');
				}
			}
			return rollback;
		}

	}

	/**
	 * Internal context holder for a specific test method.
	 */
	private static class TransactionContext {

		private final PlatformTransactionManager transactionManager;

		private final TransactionDefinition transactionDefinition;

		private TransactionStatus transactionStatus;

		public TransactionContext(PlatformTransactionManager transactionManager, TransactionDefinition transactionDefinition) {
			this.transactionManager = transactionManager;
			this.transactionDefinition = transactionDefinition;
		}

		public void startTransaction() {
			this.transactionStatus = this.transactionManager.getTransaction(this.transactionDefinition);
		}

		public void endTransaction(boolean rollback) {
			if (rollback) {
				this.transactionManager.rollback(this.transactionStatus);
			}
			else {
				this.transactionManager.commit(this.transactionStatus);
			}
		}
	}

	@Override
	public boolean isVisible() {
		return isTesting();
	}
}
