package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationEchue;
import ch.vd.uniregctb.declaration.ListeRecapitulativeDAO;
import ch.vd.uniregctb.evenement.fiscal.EvenementFiscalService;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.CategorieImpotSource;

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
	private final AdresseService adresseService;

	public DeterminerLRsEchuesProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate, ListeRecapService lrService,
	                                    DelaisService delaisService, TiersDAO tiersDAO, ListeRecapitulativeDAO lrDAO, EvenementFiscalService evenementFiscalService, TiersService tiersService,
	                                    AdresseService adresseService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.delaisService = delaisService;
		this.tiersDAO = tiersDAO;
		this.lrDAO = lrDAO;
		this.evenementFiscalService = evenementFiscalService;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	public DeterminerLRsEchuesResults run(final Integer periodeFiscale, final RegDate dateTraitement, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		s.setMessage("Récupération des listes récapitulatives...");

		final DeterminerLRsEchuesResults rapportFinal = new DeterminerLRsEchuesResults(periodeFiscale, dateTraitement, tiersService, adresseService);

		// liste de toutes les débiteurs à passer en revue
		final List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> list = getListeInfosSurCandidats(periodeFiscale, dateTraitement);

		// passage en revue par groupe transactionnel
		final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults>
				template = new BatchTransactionTemplateWithResults<>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue, DeterminerLRsEchuesResults>() {
			@Override
			public boolean doInTransaction(List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> batch, DeterminerLRsEchuesResults rapport) throws Exception {

				s.setMessage(String.format("Débiteurs analysés : %d/%d...", rapportFinal.getNbDebiteursAnalyses(), list.size()), progressMonitor.getProgressInPercent());

				for (DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue debiteur : batch) {
					traiteDebiteur(rapport, debiteur, dateTraitement);
					if (s.interrupted()) {
						break;
					}
				}
				return !s.interrupted();
			}

			@Override
			public DeterminerLRsEchuesResults createSubRapport() {
				return new DeterminerLRsEchuesResults(periodeFiscale, dateTraitement, tiersService, adresseService);
			}
		}, progressMonitor);

		final String baseMessage = String.format("La génération de la liste des débiteurs ayant au moins une LR échue%s au %s",
		                                         periodeFiscale != null ? String.format(" pour la période fiscale %d", periodeFiscale) : StringUtils.EMPTY,
		                                         RegDateHelper.dateToDisplayString(dateTraitement));

		if (status.interrupted()) {
			status.setMessage(String.format("%s a été interrompue. Nombre de listes identifiées au moment de l'interruption : %d", baseMessage, rapportFinal.lrEchues.size()));
			rapportFinal.setInterrompu(true);
		}
		else {
			status.setMessage(String.format("%s est terminée. Nombre de listes concernées : %d (%d erreur(s))", baseMessage, rapportFinal.lrEchues.size(), rapportFinal.erreurs.size()));
		}

		rapportFinal.end();
		return rapportFinal;
	}

	/**
	 * Traite un débiteur : vérifie s'il a encore des LR qui doivent être émises et, si ce n'est pas le cas
	 * envoie des événements fiscaux "LR_MANQUANTE" pour toutes les LR échues
	 * @param rapport
	 * @param infoDebiteur
	 * @param dateTraitement
	 */
	private void traiteDebiteur(DeterminerLRsEchuesResults rapport, DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue infoDebiteur, RegDate dateTraitement) {
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(infoDebiteur.idDebiteur, true);
		final SortedSet<Integer> pfConcernees = infoDebiteur.getPfConcernees();
		final int lastPeriodeFiscale = pfConcernees.last();

		final List<DateRange> emises = new LinkedList<>();
		final List<DateRange> nonEmises = lrService.findLRsManquantes(dpi, RegDate.get(lastPeriodeFiscale, 12, 31), emises);

		for (int pf : pfConcernees) {
			// on se concentre uniquement sur la période fiscale donnée : si toutes les LR ont été émises
			// alors on peut échoir les LR de cette période (SIFISC-7709 : valable seulement pour les réguliers)
			final DateRange periodeFiscale = new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31));
			final List<DateRange> intersection = DateRangeHelper.intersections(periodeFiscale, nonEmises);
			if (intersection != null && !intersection.isEmpty() && dpi.getCategorieImpotSource() == CategorieImpotSource.REGULIERS) {
				rapport.addDebiteurIgnoreResteLrAEmettre(dpi, intersection);
			}
			else {
				for (DeterminerLRsEchuesResults.InfoLrEchue infoLrEchue : infoDebiteur.getLrEchues(pf)) {
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

		// comptabilisation des débiteurs passés en revue
		rapport.newDebiteurAnalyse();
	}

	/**
	 * Renvoi une liste d'information sur les LR sommées (non-retournées) qui concernent la période fiscale donnée, et
	 * pour lesquelle la date de sommation est "assez loin" dans le passé de la date de traitement
	 */
	private List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> getListeInfosSurCandidats(final Integer periodeFiscale, final RegDate dateTraitement) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final StringBuilder b = new StringBuilder();
		b.append("SELECT LR.ID, LR.TIERS_ID, LR.DATE_DEBUT, LR.DATE_FIN, ES.DATE_OBTENTION FROM DECLARATION LR");
		b.append(" JOIN ETAT_DECLARATION ES ON ES.DECLARATION_ID = LR.ID AND ES.ANNULATION_DATE IS NULL AND ES.TYPE='SOMMEE'");
		if (periodeFiscale != null) {
			b.append(" JOIN PERIODE_FISCALE PF ON LR.PERIODE_ID = PF.ID AND PF.ANNEE=:pf");
		}
		b.append(" WHERE LR.DOCUMENT_TYPE='LR' AND LR.ANNULATION_DATE IS NULL");
		b.append(" AND NOT EXISTS (SELECT 1 FROM ETAT_DECLARATION ED WHERE ED.DECLARATION_ID = LR.ID AND ED.ANNULATION_DATE IS NULL AND ED.TYPE IN ('RETOURNEE', 'ECHUE'))");
		b.append(" ORDER BY LR.TIERS_ID, LR.DATE_DEBUT");
		final String sql = b.toString();

		final List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> infos = template.execute(new TransactionCallback<List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>>() {
			@Override
			public List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue>>() {
					@Override
					public List<DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> doInHibernate(Session session) throws HibernateException {

						final Query query = session.createSQLQuery(sql);
						if (periodeFiscale != null) {
							query.setParameter("pf", periodeFiscale);
						}

						@SuppressWarnings({"unchecked"}) final List<Object[]> rows = query.list();
						if (rows != null && !rows.isEmpty()) {
							final Map<Long, DeterminerLRsEchuesResults.InfoDebiteurAvecLrEchue> infos = new HashMap<>(rows.size());
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
							return new ArrayList<>(infos.values());
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
