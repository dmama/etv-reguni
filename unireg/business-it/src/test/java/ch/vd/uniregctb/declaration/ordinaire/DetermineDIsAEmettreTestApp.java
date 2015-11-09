package ch.vd.uniregctb.declaration.ordinaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.declaration.ordinaire.pp.DeterminationDIsPPResults;

/**
 * Programme de test des performances des batch de traitement des déclaration. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class DetermineDIsAEmettreTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(DetermineDIsAEmettreTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "DetermineDIsAEmettreTestApp.zip";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		DetermineDIsAEmettreTestApp app = new DetermineDIsAEmettreTestApp();
		try {
			app.run();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
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
		DeterminationDIsPPResults results = service.determineDIsPPAEmettre(2009, RegDate.get(2010, 1, 15), 1, null);
		for (DeterminationDIsPPResults.Erreur erreur : results.erreurs) {
			LOGGER.error(erreur.getDescriptionRaison() + ": " + erreur.details);
		}
	}
}
