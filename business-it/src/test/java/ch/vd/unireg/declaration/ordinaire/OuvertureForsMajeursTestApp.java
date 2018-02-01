package ch.vd.unireg.declaration.ordinaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BusinessItTestApplication;
import ch.vd.unireg.document.MajoriteRapport;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.metier.OuvertureForsResults;
import ch.vd.unireg.rapport.RapportService;

/**
 * Programme de test des performances de l'ouverture des fors principaux des habitants majeurs. Il s'agit d'un programme stand-alone car le
 * plugin jProfiler dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class OuvertureForsMajeursTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(OuvertureForsMajeursTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "OuvertureForsMajeurs.zip";

	private MetierService service;
	private RapportService rapportService;

	public static void main(String[] args) throws Exception {

		OuvertureForsMajeursTestApp app = new OuvertureForsMajeursTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		super.run();
		AuthenticationHelper.pushPrincipal("[OuvertureForsMajeursTestApp]");
		try {
			LOGGER.info("***** START OuvertureForsMajeursTestApp Main *****");
			service = (MetierService) context.getBean("metierService");
			rapportService = (RapportService) context.getBean("rapportService");

			LOGGER.info("==> chargement de la base de données");
			clearDatabase();
			loadDatabase(DB_UNIT_DATA_FILE);

			LOGGER.info("==> réindexation des données");
			reindexDatabase();

			LOGGER.info("==> ouverture des fors");
			ouvrirLesFors();
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
		LOGGER.info("***** END OuvertureForsMajeursTestApp Main *****");
	}

	private void ouvrirLesFors() throws Exception {

		final long start = System.currentTimeMillis();

		// Ouverture des fors
		LOGGER.info("Ouverture des fors fiscaux principaux...");
		OuvertureForsResults results = service.ouvertureForsContribuablesMajeurs(RegDate.get(), null);

		// Génération du rapport
		LOGGER.info("Génération du rapport...");
		MajoriteRapport rapport = rapportService.generateRapport(results, null);

		long duree = (System.currentTimeMillis() - start);
		String message = "Ouverture terminée : " + (duree / 1000) + " secondes.";
		Audit.success(message, rapport);
		LOGGER.info(message);
	}
}
