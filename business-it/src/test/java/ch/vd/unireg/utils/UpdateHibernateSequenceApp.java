package ch.vd.unireg.utils;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.vd.unireg.common.CoreTestingConstants;
import ch.vd.unireg.database.DatabaseService;

/**
 * Programme qui met-à-jour la séquence hibernate d'une base de donnée suite à l'import de données.
 */
public class UpdateHibernateSequenceApp {

	public static void main(String[] args) throws Exception {

		UpdateHibernateSequenceApp app = new UpdateHibernateSequenceApp();
		app.run();
	}

	protected void run() throws Exception {

		String[] files = {
				CoreTestingConstants.UNIREG_CORE_SF,
				"ch/vd/unireg/utils/UpdateHibernateSequenceApp.xml",
		};

		System.out.println("=> initialisation d'Hibernate...");

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(files);
		context.registerShutdownHook();

		System.out.println("=> mise-à-jour de la séquence...");
		DatabaseService dbService = (DatabaseService) context.getBean("databaseService");
		dbService.ensureSequencesUpToDate(true, true, true, true, true);

		System.out.println("=> terminé.");
	}
}
