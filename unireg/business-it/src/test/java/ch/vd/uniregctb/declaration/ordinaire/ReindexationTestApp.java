package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Date;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;

/**
 * Programme de test des performances de la ré-indexation. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class ReindexationTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(ReindexationTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "EnvoiDIsEnMasseTestApp.xml";

	private GlobalTiersIndexer globalTiersIndexer;

	public static void main(String[] args) throws Exception {

		ReindexationTestApp app = new ReindexationTestApp();
		app.run();

		//System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		AuthenticationHelper.setPrincipal("[ReindexationTestApp]");
		super.run();

		LOGGER.info("***** START ReindexationTestApp Main *****");
		globalTiersIndexer = (GlobalTiersIndexer) context.getBean("globalTiersIndexer");

		loadDatabase(DB_UNIT_DATA_FILE);
		reindexerLaBase();

		AuthenticationHelper.resetAuthentication();
		LOGGER.info("***** END ReindexationTestApp Main *****");
	}

	private void reindexerLaBase() throws Exception {

		final long start = System.currentTimeMillis();
		LOGGER.info("Réindexation de la base de données...");
		globalTiersIndexer.indexAllDatabase();
		long duree = (System.currentTimeMillis() - start);
		LOGGER.info("Réindexation terminée : " + (duree / 1000) + " secondes.");
	}
}
