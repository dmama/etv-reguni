package ch.vd.uniregctb.documentfiscal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.shared.batchtemplate.ProgressMonitor;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplateWithResults;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.hibernate.HibernateCallback;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.type.TypeEtatAutreDocumentFiscal;

public class RappelLettresBienvenueProcessor {

	private static final int BATCH_SIZE = 100;
	private static final Logger LOGGER = LoggerFactory.getLogger(RappelLettresBienvenueProcessor.class);

	private final ParametreAppService parametreAppService;
	private final HibernateTemplate hibernateTemplate;
	private final PlatformTransactionManager transactionManager;
	private final AutreDocumentFiscalService autreDocumentFiscalService;
	private final DelaisService delaisService;

	public RappelLettresBienvenueProcessor(ParametreAppService parametreAppService, HibernateTemplate hibernateTemplate, PlatformTransactionManager transactionManager, AutreDocumentFiscalService autreDocumentFiscalService, DelaisService delaisService) {
		this.parametreAppService = parametreAppService;
		this.hibernateTemplate = hibernateTemplate;
		this.transactionManager = transactionManager;
		this.autreDocumentFiscalService = autreDocumentFiscalService;
		this.delaisService = delaisService;
	}

	public RappelLettresBienvenueResults run(final RegDate dateTraitement, StatusManager statusManager) {

		final StatusManager status = statusManager != null ? statusManager : new LoggingStatusManager(LOGGER);

		// récupération des identifiants des lettres de bienvenue dont le délai est passé et qui n'ont pas encore été rappelées
		status.setMessage("Récupération des cas à traiter...");
		final List<Long> idsLettres = fetchIdsLettres(dateTraitement);

		final RappelLettresBienvenueResults rapportFinal = new RappelLettresBienvenueResults(dateTraitement);
		final ProgressMonitor progressMonitor = new SimpleProgressMonitor();
		final BatchTransactionTemplateWithResults<Long, RappelLettresBienvenueResults> template = new BatchTransactionTemplateWithResults<>(idsLettres, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, status);
		template.execute(rapportFinal, new BatchWithResultsCallback<Long, RappelLettresBienvenueResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, RappelLettresBienvenueResults rapport) throws Exception {
				traiterBatch(batch, rapport);
				return !rapport.isInterrompu();
			}

			@Override
			public RappelLettresBienvenueResults createSubRapport() {
				return new RappelLettresBienvenueResults(dateTraitement);
			}
		}, progressMonitor);

		status.setMessage("Envoi des rappels terminé.");

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiterBatch(List<Long> idsLettres, RappelLettresBienvenueResults rapport) throws AutreDocumentFiscalException {
		for (Long idLettre : idsLettres) {
			final LettreBienvenue lettre = hibernateTemplate.get(LettreBienvenue.class, idLettre);
			final Entreprise entreprise = lettre.getEntreprise();

			// 1. vérification de l'état de la lettre
			if (lettre.getEtat() == TypeEtatAutreDocumentFiscal.RAPPELE) {
				rapport.addLettreIgnoree(entreprise.getNumero(), lettre.getDateEnvoi(), RappelLettresBienvenueResults.RaisonIgnorement.LETTRE_DEJA_RAPPELEE);
			}
			else if (lettre.getEtat() == TypeEtatAutreDocumentFiscal.RETOURNE) {
				rapport.addLettreIgnoree(entreprise.getNumero(), lettre.getDateEnvoi(), RappelLettresBienvenueResults.RaisonIgnorement.LETTRE_DEJA_RETOURNEE);
			}
			else if (lettre.getEtat() != TypeEtatAutreDocumentFiscal.EMIS) {
				rapport.addRappelErreur(entreprise.getNumero(), lettre.getId(), "Etat de lettre inconnu : " + lettre.getEtat());
			}
			else {
				// vérfication du délai administratif
				final RegDate delaiEffectif = delaisService.getFinDelai(lettre.getDelaiRetour(), parametreAppService.getDelaiEnvoiRappelLettreBienvenue());
				if (rapport.dateTraitement.isBeforeOrEqual(delaiEffectif)) {
					rapport.addLettreIgnoree(entreprise.getNumero(), lettre.getDateEnvoi(), RappelLettresBienvenueResults.RaisonIgnorement.DELAI_ADMINISTRATIF_NON_ECHU);
				}
				else {
					// tout est bon, on peut envoyer la sauce
					final RegDate dateEnvoiRappel = delaisService.getDateFinDelaiCadevImpressionLettreBienvenue(rapport.dateTraitement);
					lettre.setDateRappel(dateEnvoiRappel);
					autreDocumentFiscalService.envoyerRappelLettreBienvenueBatch(lettre, rapport.dateTraitement);
					rapport.addRappelEnvoye(entreprise.getNumero(), lettre.getDateEnvoi());
				}
			}
		}
	}

	private List<Long> fetchIdsLettres(final RegDate dateTraitement) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				return hibernateTemplate.executeWithNewSession(new HibernateCallback<List<Long>>() {
					@Override
					public List<Long> doInHibernate(Session session) throws HibernateException, SQLException {
						final String hql = "select distinct lb.id from LettreBienvenue as lb where lb.annulationDate is null and lb.dateRetour is null and lb.dateRappel is null and lb.delaiRetour < :dateTraitement order by lb.id";
						final Query query = session.createQuery(hql);
						query.setParameter("dateTraitement", dateTraitement);
						//noinspection unchecked
						return query.list();
					}
				});
			}
		});
	}
}
