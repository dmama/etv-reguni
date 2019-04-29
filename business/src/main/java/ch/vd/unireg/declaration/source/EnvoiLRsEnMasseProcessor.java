package ch.vd.unireg.declaration.source;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TicketService;
import ch.vd.unireg.common.TicketTimeoutException;
import ch.vd.unireg.declaration.DeclarationException;
import ch.vd.unireg.declaration.DeclarationGenerationOperation;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;

public class EnvoiLRsEnMasseProcessor {

	private final Logger LOGGER = LoggerFactory.getLogger(EnvoiLRsEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ListeRecapService lrService;
	private final TiersService tiersService;
	private final AdresseService adresseService;
	private final TicketService ticketService;

	public EnvoiLRsEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                ListeRecapService lrService, TiersService tiersService, AdresseService adresseService, TicketService ticketService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
		this.ticketService = ticketService;
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
				return !s.isInterrupted();
			}

			@Override
			public void afterTransactionCommit() {
				final int percent = (100 * rapportFinal.nbDPIsTotal) / list.size();
				s.setMessage(String.format("%d sur %d débiteurs traités", rapportFinal.nbDPIsTotal, list.size()), percent);
			}
		}, null);

		if (status.isInterrupted()) {
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
			if (status.isInterrupted()) {
				break;
			}
			traiteDebiteur(id, dateFinPeriode, rapport);
		}
	}

	private void traiteDebiteur(Long id, RegDate dateFinPeriode, EnvoiLRsResults rapport) throws Exception {
		final DeclarationGenerationOperation tickettingKey = new DeclarationGenerationOperation(id);
		try {
			final TicketService.Ticket ticket = ticketService.getTicket(tickettingKey, Duration.ofMillis(500));
			try {
				final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, id);
				traiteDebiteur(dpi, dateFinPeriode, rapport);
				rapport.addDebiteur(dpi);
			}
			finally {
				ticket.release();
			}
		}
		catch (TicketTimeoutException e) {
			throw new DeclarationException(String.format("Une LR est actuellement déjà en cours d'émission pour le débiteur %d.", id), e);
		}
	}

	private void traiteDebiteur(DebiteurPrestationImposable dpi, RegDate dateFinPeriode, EnvoiLRsResults rapport) throws Exception {

		final List<DateRange> lrTrouvees = new ArrayList<>();
		final List<DateRange> lrManquantes = lrService.findLRsManquantes(dpi, dateFinPeriode, lrTrouvees);
		if (lrManquantes != null) {
			final SendingTimeStrategy sts = getSendingTimeStrategy(dpi);
			for (DateRange lrPourCreation : lrManquantes) {
				final Periodicite periodicite = dpi.findPeriodicite(lrPourCreation.getDateDebut(), lrPourCreation.getDateFin());
				if (sts.isRightMoment(dateFinPeriode, lrPourCreation, periodicite.getPeriodiciteDecompte(), periodicite.getPeriodeDecompte())) {
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

	private static SendingTimeStrategy getSendingTimeStrategy(DebiteurPrestationImposable dpi) {
		if (dpi.getCategorieImpotSource() == CategorieImpotSource.EFFEUILLEUSES) {
			return SendingTimeStrategy.PERIOD_MIDDLE;
		}
		else {
			return SendingTimeStrategy.PERIOD_END;
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

		final List<Long> i = template.execute(status -> hibernateTemplate.execute(new HibernateCallback<List<Long>>() {
			@Override
			public List<Long> doInHibernate(Session session) throws HibernateException {
				final String queryDPI =
						"SELECT dpi.id FROM DebiteurPrestationImposable AS dpi " +
								"WHERE dpi.annulationDate IS NULL AND dpi.sansListeRecapitulative = false";
				final Query queryObject = session.createQuery(queryDPI);
				//noinspection unchecked
				return queryObject.list();
			}
		}));

		return i;
	}
}
