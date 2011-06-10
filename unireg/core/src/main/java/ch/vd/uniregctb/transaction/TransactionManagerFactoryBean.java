package ch.vd.uniregctb.transaction;

import org.apache.geronimo.transaction.log.HOWLLog;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactory;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.jencks.factory.GeronimoDefaults;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Classe recopiée et modifiée depuis {@link org.jencks.factory.TransactionManagerFactoryBean} afin de pouvoir
 * générer une instance de TransactionManager dont les temps d'accès sont loggués
 */
public class TransactionManagerFactoryBean implements FactoryBean, InitializingBean, DisposableBean {

	private TransactionManager transactionManager;

	private int defaultTransactionTimeoutSeconds = 600;

	private XidFactory xidFactory;

	private TransactionLog transactionLog;
	private String transactionLogDir;

	private boolean createdTransactionLog;

	@Override
	public Object getObject() throws Exception {
		if (transactionManager == null) {
			transactionManager = new TransactionManager(defaultTransactionTimeoutSeconds, xidFactory, transactionLog);
		}
		return transactionManager;
	}

	@Override
	public Class getObjectType() {
		return TransactionManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() throws Exception {
		if (createdTransactionLog && transactionLog instanceof HOWLLog) {
		    ((HOWLLog)transactionLog).doStop();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (transactionLog == null) {
		    transactionLog = GeronimoDefaults.createTransactionLog(xidFactory, transactionLogDir);
		    createdTransactionLog = true;
		}
		if (xidFactory == null) {
		    xidFactory = new XidFactoryImpl();
		}
	}

	public void setDefaultTransactionTimeoutSeconds(int defaultTransactionTimeoutSeconds) {
		this.defaultTransactionTimeoutSeconds = defaultTransactionTimeoutSeconds;
	}

	public void setXidFactory(XidFactory xidFactory) {
		this.xidFactory = xidFactory;
	}

	public void setTransactionLog(TransactionLog transactionLog) {
		this.transactionLog = transactionLog;
	}

	public void setTransactionLogDir(String transactionLogDir) {
		this.transactionLogDir = transactionLogDir;
	}
}
