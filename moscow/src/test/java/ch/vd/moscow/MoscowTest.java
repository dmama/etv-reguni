package ch.vd.moscow;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;

@ContextConfiguration(locations = {"classpath:moscow-database.xml", "classpath:moscow-hibernate.xml", "classpath:ut/moscow-properties-ut.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({
		DependencyInjectionTestExecutionListener.class,
		DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class})
public abstract class MoscowTest implements ApplicationContextAware {

	private static final Calendar CAL = GregorianCalendar.getInstance();

	static {
		try {
			Log4jConfigurer.initLogging("classpath:log4j.xml");
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected ApplicationContext context;

	protected static Date date(int year, int month, int day) {
		CAL.set(year, month + 1, day);
		return CAL.getTime();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	protected String getFilepath(String filename) throws IOException {
		final File file = getFile(filename);
		if (file == null) {
			return null;
		}
		else {
			return file.getAbsolutePath();
		}
	}

	/**
	 * Retourne le fichier spécifié par son nom en cherchant : <ul> <li>dans les ressources</li> <li>dans le classpath</li> <li>dans le package</li> </ul>
	 *
	 * @param filename un nom de fichier
	 * @return un fichier; ou <b>null</b> si le fichier n'a pas été trouvé
	 */
	protected File getFile(String filename) {
		File file = null;

		// Essaie d'abord tel-quel
		try {
			file = ResourceUtils.getFile(filename);
		}
		catch (Exception ignored) {
			// La variable file est nulle, ca nous suffit
		}

		// Ensuite avec classpath: devant
		if (file == null || !file.exists()) {
			try {
				String name = "classpath:" + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		// Ensuite avec classpath: et le chemin du package devant
		if (file == null || !file.exists()) {
			try {
				String packageName = getClass().getPackage().getName();
				packageName = packageName.replace('.', '/');

				String name = "classpath:" + packageName + '/' + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		if (!file.exists()) {
			return null;
		}

		return file;
	}
}
