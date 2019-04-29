package ch.vd.unireg.tiers.rattrapage.flaghabitant;

import java.util.List;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

/**
 * Processeur utilisé lors des tentatives de remettre d'aplomb les flags "habitant" des personnes physiques en fonction de leurs adresses de résidences civiles
 */
public class CorrectionFlagHabitantProcessor {

	private static final int TAILLE_LOT = 10;

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final StatusManager statusManager;
	private final PlatformTransactionManager transactionManager;
	private final AdresseService adresseService;

	public CorrectionFlagHabitantProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService, PlatformTransactionManager transactionManager, StatusManager statusManager,
	                                       AdresseService adresseService) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
		this.adresseService = adresseService;
	}

	private List<Long> getIdsPP() {

		final String hql = "select pp.id from PersonnePhysique pp where pp.numeroIndividu is not null";

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> {
			//noinspection unchecked

			return hibernateTemplate.find(hql, null);
		});
	}

	public CorrectionFlagHabitantResults corrigeFlagSurPersonnesPhysiques(int nbThreads) {

		final CorrectionFlagHabitantResults rapportFinal = new CorrectionFlagHabitantResults(tiersService, adresseService);

		statusManager.setMessage("Phase 1 : Identification des personnes physiques concernées");
		final List<Long> ids = getIdsPP();
		if (ids != null && !ids.isEmpty()) {

			final String messageStatus = String.format("Phase 1 : Traitement de %d personnes physiques", ids.size());
			statusManager.setMessage(messageStatus, 0);

			final ParallelBatchTransactionTemplateWithResults<Long, CorrectionFlagHabitantResults> template =
					new ParallelBatchTransactionTemplateWithResults<>(ids, TAILLE_LOT, nbThreads, Behavior.REPRISE_AUTOMATIQUE, transactionManager,
					                                                  statusManager, AuthenticationInterface.INSTANCE);
			template.execute(rapportFinal, new BatchWithResultsCallback<Long, CorrectionFlagHabitantResults>() {

				@Override
				public CorrectionFlagHabitantResults createSubRapport() {
					return new CorrectionFlagHabitantResults(tiersService, adresseService);
				}

				@Override
				public boolean doInTransaction(List<Long> batch, CorrectionFlagHabitantResults rapport) throws Exception {
					for (Long id : batch) {

						rapport.incPPInespectee();

						final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
						final Long numeroIndividu = pp.getNumeroIndividu();
						if (numeroIndividu == null) {
							throw new IllegalArgumentException("Le personne physique n°" + id + " ne possède pas de numéro d'individu, elle n'aurait pas dû être traitée.");
						}

						final TiersService.UpdateHabitantFlagResultat res = tiersService.updateHabitantFlag(pp, numeroIndividu, null);
						switch (res) {
						case CHANGE_EN_HABITANT:
							rapport.addNonHabitantChangeEnHabitant(pp);
							break;
						case CHANGE_EN_NONHABITANT:
							rapport.addHabitantChangeEnNonHabitant(pp);
							break;
						}

						if (statusManager.isInterrupted()) {
							break;
						}
					}
					return !statusManager.isInterrupted();
				}

				@Override
				public void afterTransactionCommit() {
					super.afterTransactionCommit();

					final String message = String.format("Phase 1 : %d personne(s) physique(s) inspectée(s) (sur un total de %d), %d modification(s), %d erreur(s)",
					                                     rapportFinal.getNombrePPInspectees(), ids.size(),
					                                     rapportFinal.getNombrePersonnesPhysiquesModifiees(), rapportFinal.getErreurs().size());
					final int progression = rapportFinal.getNombrePPInspectees() * 100 / ids.size();
					statusManager.setMessage(message, progression);
				}
			}, null);
		}

		rapportFinal.setInterrupted(statusManager.isInterrupted());
		rapportFinal.end();
		return rapportFinal;
	}
}
