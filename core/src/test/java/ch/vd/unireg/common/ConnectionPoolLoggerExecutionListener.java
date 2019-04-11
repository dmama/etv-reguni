package ch.vd.unireg.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Test Execution Listener qui logge l'état du pool de connection JDBC avant et après chaque test.
 */
public class ConnectionPoolLoggerExecutionListener extends AbstractTestExecutionListener {

	private static final Log LOGGER = LogFactory.getLog(ConnectionPoolLoggerExecutionListener.class);

	@Override
	public void beforeTestClass(TestContext testContext) {
		logConnectionPool("Before class (" + testContext.getTestClass().getSimpleName() + ")", testContext);
	}

	@Override
	public void beforeTestMethod(TestContext testContext) {
		logConnectionPool("Before test (" + testContext.getTestMethod().getName() + ")", testContext);
	}

	@Override
	public void afterTestMethod(TestContext testContext) {
		logConnectionPool("After test (" + testContext.getTestMethod().getName() + ")", testContext);
	}

	@Override
	public void afterTestClass(TestContext testContext) {
		logConnectionPool("After class (" + testContext.getTestClass().getSimpleName() + ")", testContext);
	}

	private static void logConnectionPool(String prefix, TestContext testContext) {
		final BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();
		final AbstractConnectionManager connectionManager = BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, AbstractConnectionManager.class, "jdbcConnectionManager");
		if (connectionManager != null) {
			final int usedCount = connectionManager.getConnectionCount() - connectionManager.getIdleConnectionCount();
			final int totalCount = connectionManager.getPartitionMaxSize();
			LOGGER.warn(prefix + " connection pool(" + connectionManager.hashCode() + ") = " + usedCount + "/" + totalCount);
		}
	}
}
