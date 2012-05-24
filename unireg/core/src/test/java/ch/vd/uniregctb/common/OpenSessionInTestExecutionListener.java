package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 *  * <code>OpenSessionInTestExecutionListener</code> which provides support to binds a
 *  Hibernate <code>Session</code> to the thread for the entire processing of the test.
 * @author xcicfh
 *
 */
public class OpenSessionInTestExecutionListener extends AbstractTestExecutionListener {

	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	private final String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;

	private FlushMode flushMode = FlushMode.AUTO;
	private boolean singleSession = true;

	private static final Logger logger = Logger.getLogger(OpenSessionInTestExecutionListener.class);

	private boolean participate = false;

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		super.beforeTestMethod(testContext);

		BeanFactory beanFactory = testContext.getApplicationContext();
		participate = false;
		SessionFactory sessionFactory = lookupSessionFactory(beanFactory);
		if (isSingleSession()) {
			if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
				// Do not modify the Session: just set the participate // flag.
				participate = true;
			}
			else {
				logger.debug("Opening single Hibernate Session in OpenSessionInTestExecutionListener");
				Session session = getSession(sessionFactory);
				TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
			}
		}
		else {
			// deferred close mode
			if (SessionFactoryUtils.isDeferredCloseActive(sessionFactory)) {
				// Do not modify deferred close: just set the participate flag.
				participate = true;
			}
			else {
				SessionFactoryUtils.initDeferredClose(sessionFactory);
			}
		}

	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		super.afterTestMethod(testContext);
		if (!participate) {
			BeanFactory beanFactory = testContext.getApplicationContext();
			SessionFactory sessionFactory = lookupSessionFactory(beanFactory);
			// single session mode
			SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
			logger.debug("Closing single Hibernate Session in OpenSessionInTestExecutionListener");
			closeSession(sessionHolder.getSession(), sessionFactory);
		}
	}

	/**
	 * Return the bean name of the SessionFactory to fetch from Spring's root application context.
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}

	protected SessionFactory lookupSessionFactory(BeanFactory beanFactory) {
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() + "' for OpenSessionInTestExecutionListener");
		}
		return beanFactory.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	protected Session getSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		Session session = SessionFactoryUtils.getSession(sessionFactory, true);
		FlushMode flushMode = getFlushMode();
		if (flushMode != null) {
			session.setFlushMode(flushMode);
		}
		return session;
	}

	/**
	 * Close the given Session. Note that this just applies in single session mode!
	 * <p>
	 * Can be overridden in subclasses, e.g. for flushing the Session before closing it. See class-level javadoc for a discussion of flush
	 * handling. Note that you should also override getSession accordingly, to set the flush mode to something else than NEVER.
	 *
	 * @param session
	 *            the Session used for filtering
	 * @param sessionFactory
	 *            the SessionFactory that this filter uses
	 */
	protected void closeSession(Session session, SessionFactory sessionFactory) {
		SessionFactoryUtils.closeSession(session);
	}

	/**
	 * Specify the Hibernate FlushMode to apply to this filter's {@link org.hibernate.Session}. Only applied in single session mode.
	 * <p>
	 * Can be populated with the corresponding constant name in XML bean definitions: e.g. "AUTO".
	 * <p>
	 * The default is "NEVER". Specify "AUTO" if you intend to use this filter without service layer transactions.
	 *
	 * @see org.hibernate.Session#setFlushMode
	 * @see org.hibernate.FlushMode#NEVER
	 * @see org.hibernate.FlushMode#AUTO
	 */
	public void setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
	}

	/**
	 * Return the Hibernate FlushMode that this filter applies to its {@link org.hibernate.Session} (in single session mode).
	 */
	protected FlushMode getFlushMode() {
		return this.flushMode;
	}

	/**
	 * Set whether to use a single session for each request. Default is "true".
	 * <p>
	 * If set to false, each data access operation or transaction will use its own session (like without Open Session in Test). Each of
	 * those sessions will be registered for deferred close, though, actually processed at request completion.
	 *
	 * @see SessionFactoryUtils#initDeferredClose
	 * @see SessionFactoryUtils#processDeferredClose
	 */
	public void setSingleSession(boolean singleSession) {
		this.singleSession = singleSession;
	}

	/**
	 * Return whether to use a single session for each request.
	 */
	protected boolean isSingleSession() {
		return singleSession;
	}
}
