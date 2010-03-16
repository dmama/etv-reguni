package ch.vd.uniregctb.rattrapage;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.common.LoggingStatusManager;

public class RattrapageMigrationMain {
	private static final Logger LOGGER = Logger.getLogger(RattrapageMigrationMain.class);

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
		// Récupération des paramètres
		final String objetATraiter = args[0];
		LoggingStatusManager statutManager =new LoggingStatusManager(LOGGER);

		LOGGER.info("Debut Rattrapage Migration :"+objetATraiter);
		String[] files = {
				ClientConstants.UNIREG_CORE_DAO, ClientConstants.UNIREG_CORE_SF, ClientConstants.UNIREG_BUSINESS_SERVICES,
				ClientConstants.UNIREG_BUSINESS_INTERFACES, "classpath:unireg-business-rattrapage.xml",
				"classpath:unireg-rattrapage-cache.xml", "classpath:unireg-rattrapage-services.xml",
				"classpath:unireg-rattrapage-database.xml", "classpath:unireg-rattrapage-interfaces.xml",
				"classpath:unireg-rattrapage-main.xml", "classpath:unireg-business-apireg.xml"
		};
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(files);
		ctx.registerShutdownHook();

		LOGGER.info("Chargement du context terminé");

		AuthenticationHelper.setPrincipal("[Rattrapage-MigrationSource]");
		ForManager forManager = (ForManager) ctx.getBean("forManager");
		ContribuableManager contribuableManager = (ContribuableManager) ctx.getBean("contribuableManager");

		//Traitement$
		if ("FORS".equals(objetATraiter)) {
			forManager.rattraperForSourcier(statutManager);
		}
		else if ("DOUBLON".equals(objetATraiter)) {
			contribuableManager.rattraperDoublont(statutManager);
		}
		else if ("ALL".equals(objetATraiter)) {
			contribuableManager.rattraperDoublont(statutManager);
			forManager.rattraperForSourcier(statutManager);

		}
		else{
			LOGGER.error("USAGE: DOUBLON  or FORS");
		}



		AuthenticationHelper.resetAuthentication();

		LOGGER.info(">>> Rattrapage terminé.");

		// Workaround parce que la JVM s'arrete pas toute seule
		System.exit(0);

	}

}
