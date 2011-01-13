package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * Processeur chargé de la détermination des LR échues
 */
public class DeterminerLRsEchuesProcessor {

	private static final int BATCH_SIZE = 100;
	private static final Logger LOGGER = Logger.getLogger(DeterminerLRsEchuesProcessor.class);

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ListeRecapService lrService;
	private final DelaisService delaisService;
	private final TiersDAO tiersDAO;
	private final ListeRecapitulativeDAO lrDAO;
	private final EvenementFiscalService evenementFiscalService;
	private final TiersService tiersService;

	public DeterminerLRsEchuesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, ListeRecapService lrService,
	                                    DelaisService delaisService, TiersDAO tiersDAO, ListeRecapitulativeDAO lrDAO, EvenementFiscalService evenementFiscalService, TiersService tiersService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.delaisService = delaisService;
		this.tiersDAO = tiersDAO;
		this.lrDAO = lrDAO;
		this.evenementFiscalService = evenementFiscalService;
		this.tiersService = tiersService;
	}

	public DeterminerLRsEchuesResults run(final int periodeFiscale, final RegDate dateTraitement, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final DeterminerLRsEchuesResults rapportFinal = new DeterminerLRsEchuesResults(periodeFiscale, dateTraitement, tiersService);
		final DateRange pf = new DateRangeHelper.Range(RegDate.get(periodeFiscale, 1, 1), RegDate.get(periodeFiscale, 12, 31));

		// liste de toutes les débiteurs à passer en revue
		final List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> list = getListeInfosSurCandidats(periodeFiscale, dateTraitement);

		// passage en revue par groupe transactionnel
		final BatchTransactionTemplate<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults> template = new BatchTransactionTemplate<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults>(list, BATCH_SIZE, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE, transactionManager, s, hibernateTemplate);
		template.execute(rapportFinal, new BatchTransactionTemplate.BatchCallback<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults>() {
			@Override
			public boolean doInTransaction(List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> batch, DeterminerLRsEchuesResults rapport) throws Exception {
				for (DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue debiteur : batch) {
					traiteDebiteur(rapport, debiteur, pf, dateTraitement);
					if (s.interrupted()) {
						break;
					}
				}
				return !s.interrupted();
			}

			@Override
			public DeterminerLRsEchuesResults createSubRapport() {
				return new DeterminerLRsEchuesResults(periodeFiscale, dateTraitement, tiersService);
			}
		});

		if (status.interrupted()) {
			status.setMessage(String.format(
					"La génération de la liste des débiteurs ayant au moins une LR échue pour la période fiscale %d au %s a été interrompue. Nombre de débiteurs identifiés au moment de l'interruption : %d",
					periodeFiscale, RegDateHelper.dateToDisplayString(dateTraitement), rapportFinal.lrEchues.size()));
			rapportFinal.setInterrompu(true);
		}
		else {
			status.setMessage(String.format(
					"La génération de la liste des débiteurs ayant au moins une LR échue pour la période fiscale %d au %s est terminée. Nombre de débiteurs concernés : %d (%s erreur(s))",
					periodeFiscale, RegDateHelper.dateToDisplayString(dateTraitement), rapportFinal.lrEchues.size(), rapportFinal.erreurs.size()));
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite un débiteur : vérifie s'il a encore des LR qui doivent être émises et, si ce n'est pas le cas
	 * envoie des événements fiscaux "LR_MANQUANTE" pour toutes les LR échues
	 * @param rapport
	 * @param infoDebiteur
	 * @param periodeFiscale
	 * @param dateTraitement
	 */
	private void traiteDebiteur(DeterminerLRsEchuesResults rapport, DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue infoDebiteur, DateRange periodeFiscale, RegDate dateTraitement) {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(infoDebiteur.idDebiteur, true);
		final List<DateRange> emises = new ArrayList<DateRange>(6);
		final List<DateRange> nonEmises = lrService.findLRsManquantes(dpi, periodeFiscale.getDateFin(), emises);

		// on se concentre uniquement sur la période fiscale donnée : si toutes les LR ont été émises
		// alors on peut échoir les LR
		final List<DateRange> intersection = DateRangeHelper.intersections(periodeFiscale, nonEmises);
		if (intersection != null && intersection.size() > 0) {
			rapport.addDebiteurIgnoreResteLrAEmettre(dpi, intersection);
		}
		else {
			for (DeterminerLRsEchuesResults.InfoLrEchue infoLrEchue : infoDebiteur.getLrEchues()) {
				final DeclarationImpotSource lr = lrDAO.get(infoLrEchue.id);

				// création d'un état "ECHUE"
				final EtatDeclaration etat = new EtatDeclarationEchue(RegDate.get());
				lr.addEtat(etat);

				// publication d'un événement fiscal
				evenementFiscalService.publierEvenementFiscalLRManquante(dpi, lr, dateTraitement);

				// génération du rapport d'exécution
				rapport.addLrEchue(dpi, lr);
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	/**
	 * Renvoi une liste d'information sur les LR sommées (non-retournées) qui concernent la période fiscale donnée, et
	 * pour lesquelle la date de sommation est "assez loin" dans le passé de la date de traitement
	 */
	private List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> getListeInfosSurCandidats(final int periodeFiscale, final RegDate dateTraitement) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final StringBuilder b = new StringBuilder();
		b.append("SELECT LR.ID, LR.TIERS_ID, LR.DATE_DEBUT, LR.DATE_FIN, ES.DATE_OBTENTION FROM DECLARATION LR");
		b.append(" JOIN ETAT_DECLARATION ES ON ES.DECLARATION_ID = LR.ID AND ES.ANNULATION_DATE IS NULL AND ES.TYPE='SOMMEE'");
		b.append(" JOIN PERIODE_FISCALE PF ON LR.PERIODE_ID = PF.ID AND PF.ANNEE=:pf");
		b.append(" WHERE LR.DOCUMENT_TYPE='LR' AND LR.ANNULATION_DATE IS NULL");
		b.append(" AND NOT EXISTS (SELECT 1 FROM ETAT_DECLARATION ER WHERE ER.DECLARATION_ID = LR.ID AND ER.ANNULATION_DATE IS NULL AND ER.TYPE='RETOURNEE')");
		b.append(" AND NOT EXISTS (SELECT 1 FROM ETAT_DECLARATION EE WHERE EE.DECLARATION_ID = LR.ID AND EE.ANNULATION_DATE IS NULL AND EE.TYPE='ECHUE')");
		b.append(" ORDER BY LR.TIERS_ID, LR.DATE_DEBUT");
		final String sql = b.toString();

		final List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> infos = (List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>) template.execute(new TransactionCallback() {
			public List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> doInTransaction(TransactionStatus status) {
				return (List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>) hibernateTemplate.execute(new HibernateCallback() {
					public List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> doInHibernate(Session session) throws HibernateException {

						final Query query = session.createSQLQuery(sql);
						query.setParameter("pf", periodeFiscale);

						final List<Object[]> rows = query.list();
						if (rows != null && rows.size() > 0) {
							final Map<Long, DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> infos = new HashMap<Long, DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>(rows.size());
							for (Object[] row : rows) {
								final int indexSommation = ((Number) row[4]).intValue();
								final RegDate sommation = RegDate.fromIndex(indexSommation, false);
								final RegDate echeanceReelle = getSeuilEcheanceSommation(sommation);
								if (dateTraitement.isAfter(echeanceReelle)) {
									final long idDebiteur = ((Number) row[1]).longValue();
									DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue infoDebiteur = infos.get(idDebiteur);
									if (infoDebiteur == null) {
										infoDebiteur = new DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue(idDebiteur);
										infos.put(idDebiteur, infoDebiteur);
									}

									final long id = ((Number) row[0]).longValue();
									final int indexDebut = ((Number) row[2]).intValue();
									final Number indexFin = ((Number) row[3]);

									final RegDate debut = RegDate.fromIndex(indexDebut, false);
									final RegDate fin = indexFin != null ? RegDate.fromIndex(indexFin.intValue(), false) : null;
									infoDebiteur.addLrEchue(id, debut, fin, sommation);
								}
							}
							return new ArrayList<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>(infos.values());
						}
						else {
							return Collections.emptyList();
						}
					}
				});
			}
		});

		return infos;
	}

	private RegDate getSeuilEcheanceSommation(RegDate dateSommation) {
		// il faut ajouter le délai de retour de la sommation et le délai d'échéance (administratif)
		final RegDate retourSommation = delaisService.getDateFinDelaiRetourSommationListeRecapitulative(dateSommation);
		return delaisService.getDateFinDelaiEcheanceSommationListeRecapitualtive(retourSommation);
	}
}
