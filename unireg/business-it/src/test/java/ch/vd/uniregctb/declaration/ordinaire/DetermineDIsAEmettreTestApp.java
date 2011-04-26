package ch.vd.uniregctb.declaration.ordinaire;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;

/**
 * Programme de test des performances des batch de traitement des déclaration. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class DetermineDIsAEmettreTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(DetermineDIsAEmettreTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "DetermineDIsAEmettreTestApp.zip";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		DetermineDIsAEmettreTestApp app = new DetermineDIsAEmettreTestApp();
		try {
			app.run();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
		}

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		AuthenticationHelper.pushPrincipal("[DetermineDIsAEmettreTestApp]");
		super.run();

		LOGGER.info("***** START DetermineDIsAEmettreTestApp Main *****");
		service = (DeclarationImpotService) context.getBean("diService");

		clearDatabase();
		loadDatabase(DB_UNIT_DATA_FILE);

		final long start = System.currentTimeMillis();
		determineDIsAEmettre();
		final long end = System.currentTimeMillis();
		LOGGER.info("Durée: "+((end - start) / 1000) + " secondes.");

		AuthenticationHelper.popPrincipal();
		LOGGER.info("***** END DetermineDIsAEmettreTestApp Main *****");
	}

	private void determineDIsAEmettre() throws Exception {
		LOGGER.info("Running job...");
		DeterminationDIsResults results = service.determineDIsAEmettre(2009, RegDate.get(2010, 1, 15), 1, null);
		for (DeterminationDIsResults.Erreur erreur : results.erreurs) {
			LOGGER.error(erreur.getDescriptionRaison() + ": " + erreur.details);
		}
	}
}
