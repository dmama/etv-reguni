package ch.vd.uniregctb.migration.pm;


import javax.sql.DataSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.hibernate.config.DescriptiveSessionFactoryBean;
import ch.vd.uniregctb.common.AuthenticationHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class
})
@ContextConfiguration(locations = {
		"classpath:spring/regpm.xml",
		"classpath:spring/database.xml",
		"classpath:spring/validation.xml",
		"classpath:spring/interfaces.xml",
		"classpath:spring/migration.xml",
		"classpath:spring/services.xml",
		"classpath:spring/ut-database.xml",
		"classpath:spring/ut-properties.xml"
})
public abstract class AbstractSpringTest implements ApplicationContextAware {

	private ApplicationContext applicationContext;

	private PlatformTransactionManager regpmTransactionManager;
	private SessionFactory regpmSessionFactory;

	private PlatformTransactionManager uniregTransactionManager;
	private SessionFactory uniregSessionFactory;
	private DescriptiveSessionFactoryBean uniregSessionFactoryBean;
	private DataSource uniregDataSource;

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
		this.uniregTransactionManager = getBean(PlatformTransactionManager.class, "uniregTransactionManager");
		this.uniregSessionFactory = getBean(SessionFactory.class, "uniregSessionFactory");
		this.uniregSessionFactoryBean = getBean(DescriptiveSessionFactoryBean.class, "&uniregSessionFactory");
		this.uniregDataSource = getBean(DataSource.class, "uniregDataSource");
		if (wantsDatabaseTruncation()) {
			truncateDatabase();
		}
		onSetup();
	}

	/**
	 * A surcharger par les tests qui ne veulent pas vider la base de données de destination
	 * @return <code>true</code>
	 */
	protected boolean wantsDatabaseTruncation() {
		return true;
	}

	/**
	 * virtual method to truncate the database
	 */
	private void truncateDatabase() throws Exception {
		doInUniregTransaction(status -> {
			deleteFromTables(getTableNames(false));
			return null;
		});
	}

	@SuppressWarnings("unchecked")
	@NotNull
	private String[] getTableNames(boolean reverse) {
		final Set<String> t = new LinkedHashSet<>();
		final Iterator<Table> tables = uniregSessionFactoryBean.getConfiguration().getTableMappings();
		while (tables.hasNext()) {
			final Table table = tables.next();
			if (table.isPhysicalTable()) {
				addTableName(t, table);
			}
		}

		final List<String> names = new ArrayList<>(t);
		if (reverse) {
			Collections.reverse(names);
		}
		return names.toArray(new String[names.size()]);
	}

	/**
	 *
	 * @param tables
	 * @param table
	 */
	@SuppressWarnings("unchecked")
	private void addTableName(Set<String> tables, Table table) {
		if (tables.contains(table.getName())) {
			return;
		}
		final Iterator<Table> ts = uniregSessionFactoryBean.getConfiguration().getTableMappings();
		while (ts.hasNext()) {
			final Table t = ts.next();
			if (t.equals(table)) {
				continue;
			}
			final Iterator<ForeignKey> relationships = t.getForeignKeyIterator();
			while (relationships.hasNext()) {
				final ForeignKey fk = relationships.next();
				if (fk.getReferencedTable().equals(table)) {
					addTableName(tables, fk.getTable());
				}
			}
		}
		tables.add(table.getName());
	}

	/**
	 * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
	 *
	 * @param names
	 *            the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 */
	private int deleteFromTables(String... names) {
		final JdbcTemplate template = new JdbcTemplate(uniregDataSource);
		return JdbcTestUtils.deleteFromTables(template, names);
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

	protected final PlatformTransactionManager getUniregTransactionManager() {
		return uniregTransactionManager;
	}

	protected final SessionFactory getUniregSessionFactory() {
		return uniregSessionFactory;
	}

	/**
	 * Exécute le callback dans une transaction en lecture seule vers la base de données RegPM (= source de la migration)
	 * @param callback callback à exécuter
	 * @param <T> type du résultat renvoyé par le callback
	 * @return valeur retournée par le callback
	 */
	protected final <T> T doInRegpmTransaction(TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
		template.setReadOnly(true);     // on n'écrit jamais rien dans regpm!
		return template.execute(status -> {
			status.setRollbackOnly();       // comme ça, on est sûr !
			return callback.doInTransaction(status);
		});
	}

	/**
	 * Exécute le callback dans une transaction R/W vers la base de données Unireg (= destination de la migration)
	 * @param callback callback à exécuter
	 * @param <T> type du résultat renvoyé par le callback
	 * @return valeur retournée par le callback
	 */
	protected final <T> T doInUniregTransaction(TransactionCallback<T> callback) {
		return doInUniregTransaction(false, callback);
	}

	/**
	 * Exécute le callback dans une transaction qui peut être R/W ou R à choix vers la base de données Unireg (= destination de la migration)
	 * @param readOnly <code>true</code> si la transaction doit être en lecture seule, <code>false</code> si elle doit également autoriser l'écriture
	 * @param callback callback à exécuter
	 * @param <T> type du résultat renvoyé par le callback
	 * @return valeur retournée par le callback
	 */
	protected final <T> T doInUniregTransaction(boolean readOnly, TransactionCallback<T> callback) {
		final TransactionTemplate template = new TransactionTemplate(uniregTransactionManager);
		template.setReadOnly(readOnly);

		AuthenticationHelper.pushPrincipal(buildPrincipalName());
		try {
			return template.execute(status -> {
				if (readOnly) {
					status.setRollbackOnly();       // comme ça, on est sûr !
				}
				return callback.doInTransaction(status);
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	protected String buildPrincipalName() {
		return StringUtils.abbreviate(String.format("[UT] %s", getClass().getSimpleName()), 50);
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
