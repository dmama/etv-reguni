package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AbstractSpringTest.TxCallback.TxCallbackException;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static junit.framework.Assert.failNotEquals;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(transactionManager = "transactionManager")
@TestExecutionListeners( {
		SingleContextTestExecutionListener.class /* <-- ce listener DOIT être le premier ! */,
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class
/*, OpenSessionInTestExecutionListener.class*/})
@Transactional(rollbackFor = Throwable.class)
public abstract class AbstractSpringTest implements ApplicationContextAware {

	protected PlatformTransactionManager transactionManager;

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The {@link ApplicationContext} that was injected into this test instance via {@link #setApplicationContext(ApplicationContext)}.
	 */
	private ApplicationContext applicationContext;

	private boolean onSetUpWasRun = false;

	/**
	 * Set the {@link ApplicationContext} to be used by this test instance, provided via {@link ApplicationContextAware} semantics.
	 */
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
		afterContextInitialization();
	}
	// To be overriden by sub-classes
	protected final void afterContextInitialization() {
	}

	@Before
	public final void beforeMethod() throws Exception {
		runOnSetUp();
	}
	@BeforeTransaction
	public void beforeTransaction() throws Exception {
		runOnSetUp();
	}

	/**
	 * S'assure que le onSetUp() n'est appelé qu'une fois, qu'on soit dans une méthode transactionnelle ou non
	 * @throws Exception
	 */
	protected void runOnSetUp() throws Exception {
		if (!onSetUpWasRun) {
			onSetUpWasRun = true;
			onSetUp();
		}
	}

	public void onSetUp() throws Exception {
		setAuthentication();

		transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
	}

	@After
	public void onTearDown() throws Exception {
		resetAuthentication();
	}

	/**
	 * @return the applicationContext
	 */
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clazz, String name) {
		return (T) applicationContext.getBean(name);
	}

	protected static List<String> asStringList(String[] fields) {
		return Arrays.asList(fields);
	}

	protected static String[] concatArrays(String[] fields1, String[] fields2) {
		List<String> list1 = asStringList(fields1);
		List<String> list2 = asStringList(fields2);
		list1.addAll(list2);
		return toStringArray(list1);
	}

	public static String[] toStringArray(List<String> list) {
		String[] tab = new String[list.size()];
		for (int i = 0; i < tab.length; i++) {
			String str = list.get(i);
			tab[i] = str;
		}
		return tab;
	}

	public static void assertEmpty(Collection<?> coll) {
		assertTrue(coll == null || coll.isEmpty());
	}

	public static void assertEmpty(String message, Collection<?> coll) {
		assertTrue(message, coll == null || coll.isEmpty());
	}

	public static void assertSameDay(Date left, Date right) {
		final boolean sameDay = sameDay(left, right);
		if (!sameDay) {
			fail(format(null, left, right));
		}
	}

	public static void assertSameDay(String message, Date left, Date right) {
		final boolean sameDay = sameDay(left, right);
		if (!sameDay) {
			fail(format(message, left, right));
		}
	}

	private static String format(String message, Date expected, Date actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(expected);
		cal2.setTime(actual);
		final String stringExpected = cal1.get(Calendar.YEAR) + "." + cal1.get(Calendar.MONTH) + "." + cal1.get(Calendar.DAY_OF_MONTH);
		final String stringActual = cal2.get(Calendar.YEAR) + "." + cal2.get(Calendar.MONTH) + "." + cal2.get(Calendar.DAY_OF_MONTH);
		return formatted + "expected:<" + stringExpected + "> but was:<" + stringActual + ">";
	}

	private static boolean sameDay(Date left, Date right) {
		final boolean sameDay;
		if (left != null && right != null) {
			Calendar cal1 = Calendar.getInstance();
			Calendar cal2 = Calendar.getInstance();
			cal1.setTime(left);
			cal2.setTime(right);

			sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
					&& cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
		}
		else {
			// assert both dates are null (or not null, but the case is treated above)
			sameDay = (left == null && right == null);
		}
		return sameDay;
	}

	public static void assertContains(String containee, String container) {

		if (container == null || containee == null || !container.contains(containee)) {
			failNotEquals("", containee, container);
		}
	}

	public static void assertContainsNoCase(String containee, String container) {

		container = container.toLowerCase();
		containee = containee.toLowerCase();
		if (!container.contains(containee)) {
			failNotEquals("", containee, container);
		}
	}

	protected static void assertInstanceOf(Class<?> clazz, Object object) {
		if (!clazz.isAssignableFrom(object.getClass())) {
			fail("expected instance of:<" + clazz.getName() + "> but was:<" + object.getClass().getName() + ">");
		}
	}

	protected static void assertRange(RegDate debut, RegDate fin, DateRange range) {
		assertNotNull(range);
		assertEquals(debut, range.getDateDebut());
		assertEquals(fin, range.getDateFin());
	}

	protected void setAuthentication() {

		// Le username c'est le nom de la classe
		String name = getDefaultOperateurName();
		setAuthentication(name);
	}

	protected String getDefaultOperateurName() {
		return "[UT] " + getClass().getSimpleName();
	}

	protected void setAuthentication(String username) {

		/* crée un objet Authentication */
		GrantedAuthority auth = new GrantedAuthorityImpl(username);
		GrantedAuthority[] authorities = new GrantedAuthority[] {
			auth
		};
		User user = new User(username, "noPwd", true, true, true, true, authorities);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd");

		/* Enregistre le context de sécurité */
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	protected void setAuthentication(String username, String[] roles) {

		/* crée un objet Authentication */
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new GrantedAuthorityImpl(username));
		for (String r : roles) {
			authorities.add(new GrantedAuthorityImpl(r));
		}
		GrantedAuthority[] array = authorities.toArray(new GrantedAuthority[authorities.size()]);
		User user = new User(username, "noPwd", true, true, true, true, array);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd", array);

		/* Enregistre le context de sécurité */
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	protected void resetAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	/**
	 * @return the transactionManager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	protected Object doInNewTransaction(TransactionCallback action) throws Exception {
		return doExecuteInTransaction(Propagation.REQUIRES_NEW, action);
	}
	protected Object doInTransaction(TransactionCallback action) throws Exception {
		return doExecuteInTransaction(Propagation.REQUIRED, action);
	}

	protected Object doExecuteInTransaction(Propagation propagation, TransactionCallback action) throws Exception {
		TransactionTemplate template = new TransactionTemplate(getTransactionManager());
		template.setPropagationBehavior(propagation.value());
		try {
			return template.execute(action);
		}
		catch (TxCallbackException e) {
			throw (Exception)e.getCause();
		}
	}

	public abstract class TxCallback implements TransactionCallback {

		public class TxCallbackException extends RuntimeException {

			private static final long serialVersionUID = -626013776510807208L;

			public TxCallbackException(Exception e) {
				super(e);
			}

		}

		public abstract Object execute(TransactionStatus status) throws Exception;

		public final Object doInTransaction(TransactionStatus status) {
			try {
				return execute(status);
			}
			catch (Exception e) {
				throw new TxCallbackException(e);
			}
		}

	}

}
