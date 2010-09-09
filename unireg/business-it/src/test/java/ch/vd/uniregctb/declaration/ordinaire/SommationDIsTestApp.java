package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Date;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.common.LoggingStatusManager;

/**
 * Programme de test des performances des batch de sommation des déclaration d'impôt ordinaires. Il s'agit d'un programme stand-alone car le
 * plugin jProfiler dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class SommationDIsTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(SommationDIsTestApp.class);

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
		AuthenticationHelper.setPrincipal("[SommationDIsTestApp]");

		LOGGER.info("***** START SommationDIsTestApp Main *****");
		service = (DeclarationImpotService) context.getBean("diService");

		clearDatabase();
		loadDatabase(DB_UNIT_DATA_FILE);
		sommerLesDIs();

		AuthenticationHelper.resetAuthentication();
		LOGGER.info("***** END SommationDIsTestApp Main *****");
	}

	private void sommerLesDIs() throws Exception {

		final long start = System.currentTimeMillis();
		LOGGER.info("Sommation de toutes les DIs ...");

		service.envoyerSommations(RegDate.get(2009, 6, 5), false, 0, new LoggingStatusManager(LOGGER));

		long duree = (System.currentTimeMillis() - start);
		LOGGER.info("Sommation terminée : " + (duree / 1000) + " secondes.");
	}
}
