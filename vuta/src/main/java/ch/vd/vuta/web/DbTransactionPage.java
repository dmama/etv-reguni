package ch.vd.vuta.web;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

public abstract class DbTransactionPage extends Page {
	
	private static Logger LOGGER = Logger.getLogger(DbTransactionPage.class);
	
	private PlatformTransactionManager transactionManager;
	private TransactionStatus status = null;
	private boolean toRollback;
	

	public DbTransactionPage(ApplicationContext context) {
		super(context);

		transactionManager = (PlatformTransactionManager)context.getBean("transactionManager");
	}
	
	public void beforeProcess() {

		LOGGER.debug("Starting transaction...");
		status = transactionManager.getTransaction(new DefaultTransactionDefinition());
		toRollback = false;
	}

	public void afterProcess(boolean errorHappened) {

		if (errorHappened || toRollback) {
			LOGGER.info("Rollback transaction");
			transactionManager.rollback(status);
		}
		else {
			LOGGER.info("Commit transaction");
			transactionManager.commit(status);
		}
	}
	
	public void setToRollback(boolean toRollback) {
		this.toRollback = toRollback;
	}

}
