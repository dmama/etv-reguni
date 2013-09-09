package ch.vd.uniregctb.declaration.source;

import java.util.ArrayList;
import java.util.List;

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
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class EnvoiLRsEnMasseProcessor {

	private final Logger LOGGER = Logger.getLogger(EnvoiLRsEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ListeRecapService lrService;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public EnvoiLRsEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                ListeRecapService lrService, TiersService tiersService, AdresseService adresseService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	/**
	 * Exécute le traitement du processeur à la date de référence spécifiée.
	 */
	public EnvoiLRsResults run(final RegDate dateFinPeriode, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final RegDate dateTraitement = RegDate.get();
		final EnvoiLRsResults rapportFinal = new EnvoiLRsResults(dateTraitement, dateFinPeriode, tiersService, adresseService);

		// Liste de tous les DPI à passer en revue
		final List<Long> list = getListDPI();

		final BatchTransactionTemplateWithResults<Long, EnvoiLRsResults> template = new BatchTransactionTemplateWithResults<>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiLRsResults>() {

			@Override
			public EnvoiLRsResults createSubRapport() {
				return new EnvoiLRsResults(dateTraitement, dateFinPeriode, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiLRsResults r) throws Exception {
				traiteBatch(batch, dateFinPeriode, s, r);
				return !s.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				final int percent = (100 * rapportFinal.nbDPIsTotal) / list.size();
				s.setMessage(String.format("%d sur %d débiteurs traités", rapportFinal.nbDPIsTotal, list.size()), percent);
			}
		}, null);

		if (status.interrupted()) {
			status.setMessage("L'envoi des listes récapitulatives a été interrompu."
					+ " Nombre de listes récapitulatives envoyées au moment de l'interruption = " + rapportFinal.LRTraitees.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi des listes récapitulatives est terminé." + " Nombre de listes récapitulatives envoyées = "
					+ rapportFinal.LRTraitees.size() + ". Nombre d'erreurs = " + rapportFinal.LREnErreur.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, RegDate dateFinPeriode, StatusManager status, EnvoiLRsResults rapport) throws Exception {
		for (Long id : batch) {
			if (status.interrupted()) {
				break;
			}
			traiteDebiteur(id, dateFinPeriode, rapport);
		}
	}

	private void traiteDebiteur(Long id, RegDate dateFinPeriode, EnvoiLRsResults rapport) throws Exception {
		final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, id);
		if (isdebiteurSansPeriodiciteUnique(dpi)) {
			traiteDebiteur(dpi, dateFinPeriode, rapport);
			rapport.addDebiteur(dpi);
		}

	}

	private boolean isdebiteurSansPeriodiciteUnique(DebiteurPrestationImposable dpi) {
		Periodicite periodicite = dpi.getPeriodiciteAt(RegDate.get());
		return PeriodiciteDecompte.UNIQUE != periodicite.getPeriodiciteDecompte();
	}

	private void traiteDebiteur(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, EnvoiLRsResults rapport) throws Exception {

		final DateRange periodeInteressante = new DateRangeHelper.Range(null, dateFinPeriode);
		final List<DateRange> lrTrouvees = new ArrayList<>();
		final List<DateRange> lrPeriodiquesManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
		if (lrPeriodiquesManquantes != null) {
			for (DateRange lrPourCreation : lrPeriodiquesManquantes) {

				// on vérifie quand même que la période de la LR est échue à la date donnée
				if (periodeInteressante.isValidAt(lrPourCreation.getDateFin())) {
					if (DateRangeHelper.intersect(lrPourCreation, lrTrouvees)) {
						final String message = String.format("Le débiteur %s possède déjà une LR qui intersecte la période du %s au %s.",
								FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
								RegDateHelper.dateToDisplayString(lrPourCreation.getDateDebut()),
								RegDateHelper.dateToDisplayString(lrPourCreation.getDateFin()));
						rapport.addErrorLRCollision(dpi, message);
					}
					else {
						lrService.imprimerLR(dpi, lrPourCreation.getDateDebut(), lrPourCreation.getDateFin());
						rapport.addLrTraitee(dpi, lrPourCreation.getDateDebut(), lrPourCreation.getDateFin());
					}
				}
			}
		}

	}

	/**
	 * retourne la liste des dpi ayant une périodicité non ponctuelle
	 *
	 * @return itérateur sur les ids des dpi trouvés
	 */
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	protected List<Long> getListDPI() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<Long> i = template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException {
						final String queryDPI =
								"SELECT dpi.id FROM DebiteurPrestationImposable AS dpi " +
										"WHERE dpi.annulationDate IS NULL AND dpi.sansListeRecapitulative = false";
						final Query queryObject = session.createQuery(queryDPI);
						//noinspection unchecked
						return queryObject.list();
					}
				});
			}
		});

		return i;
	}
}
