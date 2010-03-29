package ch.vd.uniregctb.tiers.rattrapage.flaghabitant;

import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Processeur utilisé lors des tentatives de remettre d'aplomb les
 * flags "habitant" des contribuables en fonction de leur for principal actif
 */
public class CorrectionFlagHabitantProcessor {

	private static final int TAILLE_LOT = 10;

	private final HibernateTemplate hibernateTemplate;
	private final TiersService tiersService;
	private final StatusManager statusManager;
	private final PlatformTransactionManager transactionManager;

	public CorrectionFlagHabitantProcessor(HibernateTemplate hibernateTemplate, TiersService tiersService, PlatformTransactionManager transactionManager, StatusManager statusManager) {
		this.hibernateTemplate = hibernateTemplate;
		this.tiersService = tiersService;
		this.transactionManager = transactionManager;
		this.statusManager = statusManager;
	}

	@SuppressWarnings({"unchecked"})
	private List<Long> getIdsPPAvecFlagHabitantPasDroit() {
		final String hql = "select ff.tiers.id from ForFiscalPrincipal ff"
						 + " where ff.dateFin is null and ff.annulationDate is null"
						 + " and ff.tiers.class = PersonnePhysique"
						 + " and ff.tiers.annulationDate is null"
						 + " and ((ff.tiers.habitant = 0 and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD')"
						 + " or (ff.tiers.habitant = 1 and ff.typeAutoriteFiscale != 'COMMUNE_OU_FRACTION_VD'))";

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (List<Long>) template.execute(new TransactionCallback() {
			public List<Long> doInTransaction(TransactionStatus status) {
				return (List<Long>) hibernateTemplate.find(hql);
			}
		});
	}

	public CorrectionFlagHabitantSurPersonnesPhysiquesResults corrigeFlagSurPersonnesPhysiques(int nbThreads) {

		final CorrectionFlagHabitantSurPersonnesPhysiquesResults rapportFinal = new CorrectionFlagHabitantSurPersonnesPhysiquesResults();

		statusManager.setMessage("Phase 1 : Identification des personnes physiques concernées");
		final List<Long> ids = getIdsPPAvecFlagHabitantPasDroit();
		if (ids != null && ids.size() > 0) {

			final String messageStatus = String.format("Phase 1 : Traitement de %d personnes physiques", ids.size());
			statusManager.setMessage(messageStatus, 0);

			final ParallelBatchTransactionTemplate<Long, CorrectionFlagHabitantSurPersonnesPhysiquesResults> template
					= new ParallelBatchTransactionTemplate<Long, CorrectionFlagHabitantSurPersonnesPhysiquesResults>(ids, TAILLE_LOT, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, hibernateTemplate);
			template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, CorrectionFlagHabitantSurPersonnesPhysiquesResults>() {

				@Override
				public CorrectionFlagHabitantSurPersonnesPhysiquesResults createSubRapport() {
					return new CorrectionFlagHabitantSurPersonnesPhysiquesResults();
				}

				@Override
				public boolean doInTransaction(List<Long> batch, CorrectionFlagHabitantSurPersonnesPhysiquesResults rapport) throws Exception {
					for (Long id : batch) {
						final PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(id);
						final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
						if (pp.isHabitant() && ffp.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							tiersService.changeHabitantenNH(pp);
							rapport.addHabitantChangeEnNonHabitant(pp);
						}
						else if (!pp.isHabitant() && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
							if (pp.getNumeroIndividu() != null) {
								tiersService.changeNHenHabitant(pp, pp.getNumeroIndividu(), null);
								rapport.addNonHabitantChangeEnHabitant(pp);
							}
							else {
								rapport.addNonHabitantForVaudoisSansNumeroIndividu(pp);
							}
						}
						if (statusManager.interrupted()) {
							break;
						}
					}
					return !statusManager.interrupted();
				}

				@Override
				public void afterTransactionCommit() {
					super.afterTransactionCommit();

					final String message = String.format("Phase 1 : %d personne(s) physique(s) inspectée(s) (sur un total de %d), %d modification(s), %d erreur(s)",
							rapportFinal.getNombreElementsInspectes(), ids.size(),
							rapportFinal.getNombrePersonnesPhysiquesModifiees(), rapportFinal.getErreurs().size());
					final int progression = rapportFinal.getNombreElementsInspectes() * 100 / ids.size();
					statusManager.setMessage(message, progression);
				}
			});
		}
		rapportFinal.setInterrupted(statusManager.interrupted());
		rapportFinal.end();
		return rapportFinal;
	}

	public CorrectionFlagHabitantSurMenagesResults corrigeFlagSurMenages(int nbThreads) {

		final CorrectionFlagHabitantSurMenagesResults rapportFinal = new CorrectionFlagHabitantSurMenagesResults();

		// la phase 1 est "PP seules"
		traiteMenagesVaudoisSansMembreHabitant(nbThreads, rapportFinal, 2);
		traiteMenagesNonVaudoisAvecMembreHabitant(nbThreads, rapportFinal, 3);

		rapportFinal.end();
		return rapportFinal;
	}

	private static interface TraitementMenages {

		/**
		 * @return le message à afficher pendant la requête SQL initiale de chargement des identifiants
		 */
		String getMessageInitial();

		/**
		 * @return la liste complète des identifiants des ménages communs à traiter
		 */
		List<Long> getIdsMenagesATraiter();

		/**
		 * Traitement du ménage commun donné
		 * @param rapport
		 * @param ctbId
		 */
		void traite(CorrectionFlagHabitantSurMenagesResults rapport, long ctbId);
	}

	private void traiteMenages(int nbThreads, final CorrectionFlagHabitantSurMenagesResults rapportFinal, final int numeroPhase, final TraitementMenages traitement)  {

		if (!statusManager.interrupted()) {
			
			statusManager.setMessage(String.format("Phase %d : %s", numeroPhase, traitement.getMessageInitial()));
			final List<Long> ids = traitement.getIdsMenagesATraiter();

			if (ids != null && ids.size() > 0 && !statusManager.interrupted()) {

				final String messageStatus = String.format("Phase %d : Traitement de %d ménages communs", numeroPhase, ids.size());
				statusManager.setMessage(messageStatus, 0);

				final ParallelBatchTransactionTemplate<Long, CorrectionFlagHabitantSurMenagesResults> template
						= new ParallelBatchTransactionTemplate<Long, CorrectionFlagHabitantSurMenagesResults>(ids, TAILLE_LOT, nbThreads, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, statusManager, hibernateTemplate);
				template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<Long, CorrectionFlagHabitantSurMenagesResults>() {

					@Override
					public CorrectionFlagHabitantSurMenagesResults createSubRapport() {
						return new CorrectionFlagHabitantSurMenagesResults();
					}

					@Override
					public void afterTransactionCommit() {
						super.afterTransactionCommit();

						final String message = String.format("Phase %d : %d ménages inspecté(s) (sur un total de %d)",
								numeroPhase, rapportFinal.getNombreElementsInspectes(), ids.size());
						final int progression = rapportFinal.getNombreElementsInspectes() * 100 / ids.size();
						statusManager.setMessage(message, progression);
					}

					@Override
					public boolean doInTransaction(List<Long> batch, CorrectionFlagHabitantSurMenagesResults rapport) throws Exception {
						for (Long id : batch) {
							traitement.traite(rapport, id);
							if (statusManager.interrupted()) {
								break;
							}
						}
						return !statusManager.interrupted();
					}
				});
			}
		}

		rapportFinal.setInterrupted(statusManager.interrupted());
	}

	private void traiteMenagesVaudoisSansMembreHabitant(int nbThreads, CorrectionFlagHabitantSurMenagesResults rapportFinal, int numeroPhase) {

		traiteMenages(nbThreads, rapportFinal, numeroPhase, new TraitementMenages() {
			public String getMessageInitial() {
				return "Identification des ménages communs vaudois sans membre habitant";
			}

			public List<Long> getIdsMenagesATraiter() {
				return getIdsMCsurVDsansHabitant();
			}

			public void traite(CorrectionFlagHabitantSurMenagesResults rapport, long ctbId) {
				rapport.addMenageVaudoisSansHabitant(ctbId);
			}
		});
	}

	private void traiteMenagesNonVaudoisAvecMembreHabitant(int nbThreads, CorrectionFlagHabitantSurMenagesResults rapportFinal, int numeroPhase) {

		traiteMenages(nbThreads, rapportFinal, numeroPhase, new TraitementMenages() {
			public String getMessageInitial() {
				return "Identification des ménages communs non-vaudois avec membre habitant";
			}

			public List<Long> getIdsMenagesATraiter() {
				return getIdsMCnonVaudoisAvecHabitant();
			}

			public void traite(CorrectionFlagHabitantSurMenagesResults rapport, long ctbId) {
				rapport.addMenageNonVaudoisAvecHabitant(ctbId);
			}
		});
	}

	@SuppressWarnings({"unchecked"})
	private List<Long> getIdsMCsurVDsansHabitant() {

//		final String sqlQuery = "SELECT FF.TIERS_ID FROM FOR_FISCAL FF"
//							+ " JOIN TIERS T ON T.NUMERO = FF.TIERS_ID"
//							+ " WHERE FF.FOR_TYPE = 'ForFiscalPrincipal'"
//							+ " AND T.TIERS_TYPE = 'MenageCommun' AND T.ANNULATION_DATE IS NULL"
//							+ " AND FF.ANNULATION_DATE IS NULL"
//							+ " AND FF.TYPE_AUT_FISC = 'COMMUNE_OU_FRACTION_VD'"
//							+ " AND EXISTS (SELECT R.TIERS_OBJET_ID FROM RAPPORT_ENTRE_TIERS R WHERE R.RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage' AND R.TIERS_OBJET_ID = FF.TIERS_ID AND R.ANNULATION_DATE IS NULL AND R.DATE_FIN IS NULL)"
//							+ " AND NOT EXISTS (SELECT R.TIERS_OBJET_ID FROM RAPPORT_ENTRE_TIERS R JOIN TIERS PP ON PP.NUMERO = R.TIERS_SUJET_ID"
//							+ "                 WHERE R.RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage' AND FF.TIERS_ID = R.TIERS_OBJET_ID AND R.ANNULATION_DATE IS NULL AND R.DATE_FIN IS NULL"
//							+ "                 AND PP.TIERS_TYPE = 'PersonnePhysique' AND PP.PP_HABITANT=1)";
//
//		return (List<Long>) hibernateTemplate.executeWithNewSession(new HibernateCallback() {
//			public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
//				return session.createSQLQuery(sqlQuery).list();
//			}
//		});

		final String hql = "select ff.tiers.id from ForFiscalPrincipal ff"
				+ " where ff.tiers.class = MenageCommun and ff.tiers.annulationDate is null"
				+ " and ff.annulationDate is null and ff.dateFin is null"
				+ " and ff.typeAutoriteFiscale = 'COMMUNE_OU_FRACTION_VD'"
				+ " and exists (select am.objet.id from AppartenanceMenage am where ff.tiers = am.objet and am.annulationDate is null and am.dateFin is null)"
				+ " and not exists (select am.objet.id from AppartenanceMenage am where ff.tiers.id = am.objet.id and am.annulationDate is null and am.dateFin is null"
					+ " and am.sujet.class = PersonnePhysique and am.sujet.habitant = 1)";

		return (List<Long>) hibernateTemplate.find(hql);
	}

	@SuppressWarnings({"unchecked"})
	private List<Long> getIdsMCnonVaudoisAvecHabitant() {
		final String hql = "select ff.tiers.id from ForFiscalPrincipal ff"
				+ " where ff.tiers.class = MenageCommun and ff.tiers.annulationDate is null"
				+ " and ff.annulationDate is null and ff.dateFin is null"
				+ " and ff.typeAutoriteFiscale != 'COMMUNE_OU_FRACTION_VD'"
				+ " and exists (select am.objet.id from AppartenanceMenage am where ff.tiers = am.objet and am.annulationDate is null and am.dateFin is null"
					+ " and am.sujet.class = PersonnePhysique and am.sujet.habitant = 1)";

		return (List<Long>) hibernateTemplate.find(hql);
	}
}
