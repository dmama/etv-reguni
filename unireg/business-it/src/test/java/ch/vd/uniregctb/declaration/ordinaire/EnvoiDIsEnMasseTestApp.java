package ch.vd.uniregctb.declaration.ordinaire;

import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.metier.assujettissement.TypeContribuableDI;

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

		final Date start = new Date();
		LOGGER.info("Envoi de toutes les DIs en masse...");
		TransactionTemplate template = new TransactionTemplate(transactionManager);

		envoyerDIsEnMasseEnTransaction(template, TypeContribuableDI.VAUDOIS_ORDINAIRE);
		envoyerDIsEnMasseEnTransaction(template, TypeContribuableDI.VAUDOIS_ORDINAIRE_VAUD_TAX);
		envoyerDIsEnMasseEnTransaction(template, TypeContribuableDI.VAUDOIS_DEPENSE);
		envoyerDIsEnMasseEnTransaction(template, TypeContribuableDI.HORS_CANTON);
		envoyerDIsEnMasseEnTransaction(template, TypeContribuableDI.HORS_SUISSE);

		long duree = (new Date().getTime() - start.getTime());
		LOGGER.info("Envoi terminé : " + (duree / 1000) + " secondes.");
	}

	/**
	 * Exécution de l'envoi dans une transaction.
	 */
	private void envoyerDIsEnMasseEnTransaction(TransactionTemplate template, final TypeContribuableDI type) {

		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					LOGGER.info("Envoi des DIS vaudois ordinaires...");
					service.envoyerDIsEnMasse(2008, type, null, null, 100000, RegDate.get(2009, 1, 15), null);
					return null;
				}
				catch (DeclarationException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
