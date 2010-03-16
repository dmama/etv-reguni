package ch.vd.uniregctb.declaration.ordinaire;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessItTestApplication;
import ch.vd.uniregctb.declaration.DeclarationException;

/**
 * Programme de test des performances des batch de traitement des déclaration. Il s'agit d'un programme stand-alone car le plugin jProfiler
 * dans eclipse ne fonctionne pas avec les tests JUnits.
 */
public class DetermineDIsAEmettreTestApp extends BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(DetermineDIsAEmettreTestApp.class);

	private final static String DB_UNIT_DATA_FILE = "DetermineDIsAEmettreTestApp.xml";

	private DeclarationImpotService service;

	public static void main(String[] args) throws Exception {

		DetermineDIsAEmettreTestApp app = new DetermineDIsAEmettreTestApp();
		app.run();

		System.exit(0);
	}

	@Override
	protected void run() throws Exception {

		AuthenticationHelper.setPrincipal("[DetermineDIsAEmettreTestApp]");
		super.run();

		LOGGER.info("***** START DetermineDIsAEmettreTestApp Main *****");
		service = (DeclarationImpotService) context.getBean("diService");

		loadDatabase(DB_UNIT_DATA_FILE);
		determineDIsAEmettre();

		AuthenticationHelper.resetAuthentication();
		LOGGER.info("***** END DetermineDIsAEmettreTestApp Main *****");
	}

	private void determineDIsAEmettre() throws Exception {

		LOGGER.info("Running job...");

		// Exécution de l'envoi dans une transaction.
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					return service.determineDIsAEmettre(2008, RegDate.get(2009, 1, 15), null);
				}
				catch (DeclarationException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
}
