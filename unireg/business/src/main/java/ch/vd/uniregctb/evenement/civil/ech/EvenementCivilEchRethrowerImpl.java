package ch.vd.uniregctb.evenement.civil.ech;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class EvenementCivilEchRethrowerImpl implements EvenementCivilEchRethrower {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchRethrowerImpl.class);

	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	private EvenementCivilEchReceptionHandler receptionHandler;

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setEvtCivilDAO(EvenementCivilEchDAO evtCivilDAO) {
		this.evtCivilDAO = evtCivilDAO;
	}

	public void setReceptionHandler(EvenementCivilEchReceptionHandler receptionHandler) {
		this.receptionHandler = receptionHandler;
	}

	@Override
	public void fetchAndRethrowEvents() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Récupération des événements civils e-CH à relancer");
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final List<EvenementCivilEch> relanceables = template.execute(new TransactionCallback<List<EvenementCivilEch>>() {
			@Override
			public List<EvenementCivilEch> doInTransaction(TransactionStatus status) {
				return evtCivilDAO.getEvenementsCivilsARelancer();
			}
		});

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Trouvé %d événement(s) à relancer", relanceables == null ? 0 : relanceables.size()));
		}

		if (relanceables != null && relanceables.size() > 0) {
			for (EvenementCivilEch evt : relanceables) {
				try {
					receptionHandler.handleEvent(evt);
				}
				catch (Exception e) {
					LOGGER.error(String.format("Erreur lors de la relance de l'événement civil %d", evt.getId()), e);
				}
			}

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Relance des événements civils terminée");
			}
		}
	}
}
