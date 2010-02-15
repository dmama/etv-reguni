package ch.vd.vuta;

import org.apache.log4j.Logger;
import org.springframework.test.annotation.AbstractAnnotationAwareTransactionalTests;
import org.springframework.transaction.PlatformTransactionManager;

public abstract class AbstractSmsgwTestCase extends AbstractAnnotationAwareTransactionalTests {

	protected static Logger LOGGER = Logger.getLogger(AbstractSmsgwTestCase.class);
	
	private PlatformTransactionManager tm;
	
	public AbstractSmsgwTestCase() {
	}

	
	public void onSetUp() {

		tm = (PlatformTransactionManager)getApplicationContext().getBean("transactionManager");
		setTransactionManager(tm);

		startNewTransaction();
	}

	public void onTearDown() {
		
		endTransaction();
	}

	public String[] getConfigLocations() {
		setAutowireMode(AUTOWIRE_NO);
		return new String[] {
				"vuta-common.xml",
				"ut/vutaut-common.xml",
			};
	}

}
