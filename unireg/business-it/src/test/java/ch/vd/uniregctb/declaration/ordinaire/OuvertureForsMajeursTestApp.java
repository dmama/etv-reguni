package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Date;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.document.MajoriteRapport;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.OuvertureForsResults;
import ch.vd.uniregctb.rapport.RapportService;

/**
 * Programme de test des performances de l'ouverture des fors principaux des habitants majeurs. Il s'agit d'un programme stand-alone car le
 * plugin jProfiler dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class OuvertureForsMajeursTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(OuvertureForsMajeursTestApp.class);

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
		AuthenticationHelper.setPrincipal("[OuvertureForsMajeursTestApp]");

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

		AuthenticationHelper.resetAuthentication();
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
