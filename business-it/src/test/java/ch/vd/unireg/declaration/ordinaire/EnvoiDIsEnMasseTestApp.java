package ch.vd.unireg.declaration.ordinaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.BusinessItTestApplication;
import ch.vd.unireg.metier.assujettissement.CategorieEnvoiDIPP;

/**
 * Programme de test des performances des batch de traitement des déclaration. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class EnvoiDIsEnMasseTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiDIsEnMasseTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "EnvoiDIsEnMasseTestApp.zip";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		EnvoiDIsEnMasseTestApp app = new EnvoiDIsEnMasseTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		AuthenticationHelper.pushPrincipal("[EnvoiDIsEnMasseTestApp]");
		try {
			super.run();

			LOGGER.info("***** START EnvoiDIsEnMasseTestApp Main *****");
			service = (DeclarationImpotService) context.getBean("diService");

			clearDatabase();
			loadDatabase(DB_UNIT_DATA_FILE);
			envoyerDIsEnMasse();
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
		LOGGER.info("***** END EnvoiDIsEnMasseTestApp Main *****");
	}

	private void envoyerDIsEnMasse() throws Exception {

		final long start = System.currentTimeMillis();
		LOGGER.info("Envoi de toutes les DIs en masse...");
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDIPP.VAUDOIS_COMPLETE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDIPP.VAUDOIS_VAUDTAX);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDIPP.VAUDOIS_DEPENSE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDIPP.HC_IMMEUBLE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDIPP.HS_COMPLETE);

		long duree = (System.currentTimeMillis() - start);
		LOGGER.info("Envoi terminé : " + (duree / 1000) + " secondes.");
	}

	/**
	 * Exécution de l'envoi dans une transaction.
	 */
	private void envoyerDIsEnMasseEnTransaction(TransactionTemplate template, final CategorieEnvoiDIPP categorie) {

		template.execute(status -> {
			LOGGER.info("Envoi des DIS vaudois ordinaires...");
			service.envoyerDIsPPEnMasse(2008, categorie, null, null, 100000, RegDate.get(2009, 1, 15), false, 1, null);
			return null;
		});
	}
}
