package ch.vd.uniregctb.documentfiscal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ProgressMonitor;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.MovingWindow;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Processeur d'envoi en masse des lettres de bienvenue PM
 */
public class EnvoiLettresBienvenueProcessor {

	private static final int BATCH_SIZE = 100;
	private static final Logger LOGGER = LoggerFactory.getLogger(EnvoiLettresBienvenueProcessor.class);

	private final ParametreAppService parametreAppService;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final TiersService tiersService;
	private final AssujettissementService assujettissementService;
	private final AutreDocumentFiscalService autreDocumentFiscalService;

	public EnvoiLettresBienvenueProcessor(ParametreAppService parametreAppService,
	                                      HibernateTemplate hibernateTemplate,
	                                      PlatformTransactionManager transactionManager,
	                                      TiersService tiersService,
	                                      AssujettissementService assujettissementService,
	                                      AutreDocumentFiscalService autreDocumentFiscalService) {
		this.parametreAppService = parametreAppService;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.tiersService = tiersService;
		this.assujettissementService = assujettissementService;
		this.autreDocumentFiscalService = autreDocumentFiscalService;
	}

	public EnvoiLettresBienvenueResults run(final RegDate dateTraitement, final int delaiCarence, StatusManager statusManager) {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);

		// on recherche d'abord la liste des entreprises qui ont un for VD qui s'ouvre après la date fatidique
		// du début des opérations (pour gérer la transition RegPM -> Unireg)

		status.setMessage("Récupération des identifiants des entreprises...");

		final RegDate dateOrigine = getDateOrigine();
		final List<Long> ids = fetchIds(dateOrigine);

		final int tailleTrouAssujettissement = parametreAppService.getTailleTrouAssujettissementPourNouvelleLettreBienvenue();
		final EnvoiLettresBienvenueResults rapportFinal = new EnvoiLettresBienvenueResults(dateTraitement, delaiCarence, dateOrigine, tailleTrouAssujettissement);
		final ProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, EnvoiLettresBienvenueResults> template = new BatchTransactionTemplateWithResults<>(ids, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, EnvoiLettresBienvenueResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiLettresBienvenueResults rapport) throws Exception {
				traiterBatch(batch, rapport);
				return !status.interrupted();
			}

			@Override
			public EnvoiLettresBienvenueResults createSubRapport() {
				return new EnvoiLettresBienvenueResults(dateTraitement, delaiCarence, dateOrigine, tailleTrouAssujettissement);
			}
		}, progressMonitor);

		status.setMessage("Envoi terminé.");

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<Long> batch, EnvoiLettresBienvenueResults rapport) throws AssujettissementException, AutreDocumentFiscalException {
		for (Long id : batch) {
			final Entreprise e = (Entreprise) tiersService.getTiers(id);
			final List<Assujettissement> assujettissements = assujettissementService.determine(e);

			// pas d'assujettissement maintenant, aucune raison d'avoir une lettre de bienvenue...
			final RegDate dateReferencePourAssujettissement = rapport.dateTraitement.addDays(-rapport.delaiCarence);
			if (assujettissements == null || assujettissements.isEmpty() || DateRangeHelper.rangeAt(assujettissements, dateReferencePourAssujettissement) == null) {
				rapport.addIgnoreNonAssujetti(id);
			}
			else {
				// on projette les assujettissement à plat
				final List<DateRange> ranges = DateRangeHelper.merge(assujettissements);
				final List<LettreBienvenue> lettresDejaEnvoyees = getLettresBienvenueNonAnnuleesTriees(e);

				// si on a une période d'assujettissement continu qui commence après la date origine, c'est un candidat à la lettre de bienvenue
				final MovingWindow<DateRange> movingWindow = new MovingWindow<>(ranges);
				boolean trouveNouvelAssujettissementSansLettre = false;
				RegDate dateDebutNouvelAssujettissement = null;
				while (movingWindow.hasNext()) {
					final MovingWindow.Snapshot<DateRange> snapshot = movingWindow.next();
					final DateRange current = snapshot.getCurrent();
					final RegDate debutAssujettissement = current.getDateDebut();
					if (debutAssujettissement.isAfterOrEqual(rapport.dateOrigine)) {
						final DateRange precedent = snapshot.getPrevious();
						if (precedent == null || precedent.getDateFin().addDays(rapport.tailleMinimaleTrouAssujettissement).isBefore(debutAssujettissement)) {
							// y a-t-il déjà une lettre de bienvenue pour cet assujettissement (ou un autre ultérieur ?)
							boolean dejaEnvoyee = false;
							for (LettreBienvenue lettre : lettresDejaEnvoyees) {
								if (lettre.getDateEnvoi().isAfterOrEqual(debutAssujettissement)) {
									// déja envoyé... on continue
									dejaEnvoyee = true;
									break;
								}
							}

							// si on a déjà envoyé une lettre, on continue pour la période d'assujettissement suivant (sinon, il faut en envoyer une nouvelle...)
							if (!dejaEnvoyee) {
								trouveNouvelAssujettissementSansLettre = true;
								dateDebutNouvelAssujettissement = debutAssujettissement;
								break;
							}
						}
					}
				}

				// si on a trouvé un assujettissement sans lettre, alors c'est parti
				if (!trouveNouvelAssujettissementSansLettre) {
					rapport.addIgnoreLettreDejaEnvoyee(id);
				}
				else {
					final LettreBienvenue lettre = autreDocumentFiscalService.envoyerLettreBienvenueBatch(e, rapport.dateTraitement, dateDebutNouvelAssujettissement);
					rapport.addLettreEnvoyee(id, lettre.getType());
				}
			}
		}
	}

	@NotNull
	private List<LettreBienvenue> getLettresBienvenueNonAnnuleesTriees(Entreprise entreprise) {
		final Set<AutreDocumentFiscal> documents = entreprise.getAutresDocumentsFiscaux();
		if (documents == null || documents.isEmpty()) {
			return Collections.emptyList();
		}

		final List<LettreBienvenue> liste = new ArrayList<>(documents.size());
		for (AutreDocumentFiscal document : documents) {
			if (!document.isAnnule() && document instanceof LettreBienvenue) {
				liste.add((LettreBienvenue) document);
			}
		}
		if (liste.isEmpty()) {
			return Collections.emptyList();
		}
		Collections.sort(liste, (l1, l2) -> NullDateBehavior.EARLIEST.compare(l1.getDateEnvoi(), l2.getDateEnvoi()));
		return liste;
	}

	/**
	 * @return la date d'origine avant laquelle on n'envoie pas de lettre de bienvenue
	 */
	private RegDate getDateOrigine() {
		final Integer[] parts = parametreAppService.getDateDebutEnvoiLettresBienvenue();
		return RegDate.get(parts[2], parts[1], parts[0]);
	}

	private List<Long> fetchIds(final RegDate dateOrigine) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final String hql = "select distinct ff.tiers.numero from ForFiscal as ff where ff.annulationDate is null and ff.typeAutoriteFiscale=:taf and ff.genreImpot=:gi and ff.dateDebut>=:seuil and ff.tiers.class='Entreprise' order by ff.tiers.numero";
						final Query query = session.createQuery(hql);
						query.setParameter("taf", TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
						query.setParameter("gi", GenreImpot.BENEFICE_CAPITAL);
						query.setParameter("seuil", dateOrigine);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}
}
