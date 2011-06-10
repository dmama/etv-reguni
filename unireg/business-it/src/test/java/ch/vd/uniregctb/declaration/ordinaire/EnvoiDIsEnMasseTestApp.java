package ch.vd.uniregctb.declaration.ordinaire;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.metier.assujettissement.CategorieEnvoiDI;

/**
 * Programme de test des performances des batch de traitement des déclaration. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class EnvoiDIsEnMasseTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(EnvoiDIsEnMasseTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "EnvoiDIsEnMasseTestApp.zip";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		EnvoiDIsEnMasseTestApp app = new EnvoiDIsEnMasseTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		AuthenticationHelper.setPrincipal("[EnvoiDIsEnMasseTestApp]");
		super.run();

		LOGGER.info("***** START EnvoiDIsEnMasseTestApp Main *****");
		service = (DeclarationImpotService) context.getBean("diService");

		clearDatabase();
		loadDatabase(DB_UNIT_DATA_FILE);
		envoyerDIsEnMasse();

		AuthenticationHelper.resetAuthentication();
		LOGGER.info("***** END EnvoiDIsEnMasseTestApp Main *****");
	}

	private void envoyerDIsEnMasse() throws Exception {

		final long start = System.currentTimeMillis();
		LOGGER.info("Envoi de toutes les DIs en masse...");
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDI.VAUDOIS_COMPLETE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDI.VAUDOIS_VAUDTAX);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDI.VAUDOIS_DEPENSE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDI.HC_IMMEUBLE);
		envoyerDIsEnMasseEnTransaction(template, CategorieEnvoiDI.HS_COMPLETE);

		long duree = (System.currentTimeMillis() - start);
		LOGGER.info("Envoi terminé : " + (duree / 1000) + " secondes.");
	}

	/**
	 * Exécution de l'envoi dans une transaction.
	 */
	private void envoyerDIsEnMasseEnTransaction(TransactionTemplate template, final CategorieEnvoiDI categorie) {

		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					LOGGER.info("Envoi des DIS vaudois ordinaires...");
					service.envoyerDIsEnMasse(2008, categorie, null, null, 100000, RegDate.get(2009, 1, 15), false, null);
					return null;
				}
				catch (DeclarationException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
