package ch.vd.uniregctb.migreg;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ClientConstants;

public class HostMigrationMain {

	private static final Logger LOGGER = Logger.getLogger(HostMigrationMain.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		Assert.isTrue(args.length > 0 && args.length < 5);

		// Le run pour de vrai
		boolean fileNotFound = true;
		{
			File file = new File("log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("log4j.xml");
				fileNotFound = false;
			}
		}
		// Dans Eclipse
		if (fileNotFound) {
			File file = new File("src/main/resources/log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("src/main/resources/log4j.xml");
			}
			else {
				Assert.fail("Pas de fichier Log4j");
			}
		}



		LOGGER.debug("Start HostMigrationMain-main");
		String[] files = {	ClientConstants.UNIREG_CORE_DAO,
							ClientConstants.UNIREG_CORE_SF,
							ClientConstants.UNIREG_BUSINESS_SERVICES,
							ClientConstants.UNIREG_BUSINESS_INTERFACES,
							ClientConstants.UNIREG_BUSINESS_MIGREG,
							"classpath:unireg-migreg-cache.xml",
							"classpath:unireg-migreg-services.xml",
							"classpath:unireg-migreg-database.xml",
							"classpath:unireg-migreg-interfaces.xml",
							"classpath:unireg-migreg-main.xml",
							"classpath:unireg-business-apireg.xml"
						};
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(files);
		ctx.registerShutdownHook();

		HostMigrationManager migrManager = (HostMigrationManager) ctx.getBean("hostMigrationManager");
		String db2Schema = (String)ctx.getBean("db2Schema");

		AuthenticationHelper.setPrincipal("[HostMigrationMain]");

		// Récupération des paramètres
		final String limit = args[0];
		final Boolean overrideErrorsProcessing = (args.length >= 2 ? args[1].equals("true") : null);
		final Boolean overrideIndexationAtEnd = (args.length >= 3 ? args[2].equals("true") : null);
		final Boolean validationDisabled = (args.length >= 4 ? args[3].equals("true") : null);

		MigRegLimitsList limits = LimitsConfigurator.cfg(limit, db2Schema);
		if (overrideErrorsProcessing != null) {
			migrManager.setErrorsProcessing(overrideErrorsProcessing);
		}
		if (overrideIndexationAtEnd != null) {
			migrManager.setForceIndexationAtEnd(overrideIndexationAtEnd);
		}
		if (validationDisabled != null && validationDisabled) {
			LOGGER.warn("****************************************************************************");
			Audit.warn("ATTENTION ! La validation est désactivée pour cette migration. Les contribuables migrés comme non-valides devront être corrigés après-coup dans Unireg, car ils ne peuvent pas être processés par les batchs.");
			LOGGER.warn("****************************************************************************");
			migrManager.setValidationDisabled(validationDisabled);
		}
		migrManager.execute(limits, new MigregStatusManager());

		AuthenticationHelper.resetAuthentication();

		LOGGER.info(">>> Migration terminée.");

		// Workaround parce que la JVM s'arrete pas toute seule
		System.exit(0);
	}

}
