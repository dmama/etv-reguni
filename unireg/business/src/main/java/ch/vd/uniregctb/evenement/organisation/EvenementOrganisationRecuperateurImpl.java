package ch.vd.uniregctb.evenement.organisation;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.transaction.TransactionTemplate;

public class EvenementOrganisationRecuperateurImpl implements EvenementOrganisationRecuperateur {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationRecuperateurImpl.class);

	private PlatformTransactionManager transactionManager;
	private EvenementOrganisationDAO evtOrganisationDAO;
	private EvenementOrganisationReceptionHandler receptionHandler;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setEvtOrganisationDAO(EvenementOrganisationDAO evtOrganisationDAO) {
		this.evtOrganisationDAO = evtOrganisationDAO;
	}

	public void setReceptionHandler(EvenementOrganisationReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@Override
	public void recupererEvenementsOrganisation() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Récupération des événements organisation à relancer");
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List<EvenementOrganisation> relanceables = template.execute(new TransactionCallback<List<EvenementOrganisation>>() {
			@Override
			public List<EvenementOrganisation> doInTransaction(TransactionStatus status) {
				return evtOrganisationDAO.getEvenementsOrganisationARelancer();
			}
		});

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Trouvé %d événement(s) à relancer", relanceables == null ? 0 : relanceables.size()));
		}

		if (relanceables != null && relanceables.size() > 0) {
			for (final EvenementOrganisation evt : relanceables) {

				// [SIFISC-9534] introduction d'une transaction ici car cette méthode handleEvent doit être appelée
				// dans un contexte transactionnel depuis que l'on peut utiliser les dépendances d'un événement civil
				// pour récupérer un numéro d'individu associé
/* FIXME: peut-on vraiment se passer de transaction? [L'idée est que oui puisque les transactions organisation ne sont pas liées.]
				template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
*/
						try {
							// [SIFISC-9181] ici on ne demande que la récupération du numéro d'invidu, d'où le <code>null</code>
							receptionHandler.handleEvent(evt, null);
						}
						catch (Exception e) {
							LOGGER.error(String.format("Erreur lors de la relance de l'événement organisation %d", evt.getId()), e);
						}
/*
					}
				});
*/
			}

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Relance des événements organisation terminée");
			}
		}
	}
}
