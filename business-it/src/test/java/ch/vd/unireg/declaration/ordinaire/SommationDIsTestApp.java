package ch.vd.unireg.declaration.ordinaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BusinessItTestApplication;
import ch.vd.unireg.common.LoggingStatusManager;

/**
 * Programme de test des performances des batch de sommation des déclaration d'impôt ordinaires. Il s'agit d'un programme stand-alone car le
 * plugin jProfiler dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class SommationDIsTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(SommationDIsTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "SommationDIsTestApp.zip";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		SommationDIsTestApp app = new SommationDIsTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {
		super.run();
		AuthenticationHelper.pushPrincipal("[SommationDIsTestApp]");
		try {
			LOGGER.info("***** START SommationDIsTestApp Main *****");
			service = (DeclarationImpotService) context.getBean("diService");

			clearDatabase();
			loadDatabase(DB_UNIT_DATA_FILE);
			sommerLesDIs();
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
		LOGGER.info("***** END SommationDIsTestApp Main *****");
	}

	private void sommerLesDIs() throws Exception {

		final long start = System.currentTimeMillis();
		LOGGER.info("Sommation de toutes les DIs ...");

		service.envoyerSommationsPP(RegDate.get(2009, 6, 5), false, 0, new LoggingStatusManager(LOGGER));

		long duree = (System.currentTimeMillis() - start);
		LOGGER.info("Sommation terminée : " + (duree / 1000) + " secondes.");
	}
}
