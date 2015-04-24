package ch.vd.uniregctb.migration.pm;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
		                        DependencyInjectionTestExecutionListener.class,
		                        DirtiesContextTestExecutionListener.class,
		                        TransactionalTestExecutionListener.class
                        })
public abstract class AbstractSpringTest implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private PlatformTransactionManager regpmTransactionManager;
	private SessionFactory regpmSessionFactory;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getBean(Class<T> clazz, String name) {
		return (T) applicationContext.getBean(name);
	}

	@Before
	public final void setup() throws Exception {
		this.regpmTransactionManager = getBean(PlatformTransactionManager.class, "regpmTransactionManager");
		this.regpmSessionFactory = getBean(SessionFactory.class, "regpmSessionFactory");
		onSetup();
	}

	protected void onSetup() throws Exception {
		// à surcharger si nécessaire
	}

	@After
	public final void tearDown() throws Exception {
		onTearDown();
	}

	protected void onTearDown() throws Exception {
		// à surcharger si nécessaire
	}

	protected final PlatformTransactionManager getRegpmTransactionManager() {
		return regpmTransactionManager;
	}

	protected final SessionFactory getRegpmSessionFactory() {
		return regpmSessionFactory;
	}

	protected final <T> T doInRegpmTransaction(TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
		template.setReadOnly(true);     // on n'écrit jamais rien dans regpm!
		return template.execute(callback);
	}

	protected static <T> void serialize(T toSerialize, String filename) throws IOException {
		try (FileOutputStream fos = new FileOutputStream(filename); BufferedOutputStream bos = new BufferedOutputStream(fos); ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(toSerialize);
			oos.flush();
		}
	}

	protected static <T> T unserialize(Class<T> clazz, String filename) throws IOException, ClassNotFoundException, ClassCastException {
		try (FileInputStream fis = new FileInputStream(filename); BufferedInputStream bis = new BufferedInputStream(fis); ObjectInputStream ois = new ObjectInputStream(bis)) {
			final Object o = ois.readObject();
			if (o != null && !clazz.isAssignableFrom(o.getClass())) {
				throw new ClassCastException("Wrong class: expected " + clazz.getName() + ", found " + o.getClass().getName());
			}
			//noinspection unchecked
			return (T) o;
		}
	}
}
