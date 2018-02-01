package ch.vd.unireg.documentfiscal;

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
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

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
			if (lettre.getEtat() == TypeEtatDocumentFiscal.RAPPELE) {
				rapport.addLettreIgnoree(entreprise.getNumero(), lettre.getDateEnvoi(), RappelLettresBienvenueResults.RaisonIgnorement.LETTRE_DEJA_RAPPELEE);
			}
			else if (lettre.getEtat() == TypeEtatDocumentFiscal.RETOURNE) {
				rapport.addLettreIgnoree(entreprise.getNumero(), lettre.getDateEnvoi(), RappelLettresBienvenueResults.RaisonIgnorement.LETTRE_DEJA_RETOURNEE);
			}
			else if (lettre.getEtat() != TypeEtatDocumentFiscal.EMIS) {
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
					autreDocumentFiscalService.envoyerRappelLettreBienvenueBatch(lettre, rapport.dateTraitement, dateEnvoiRappel);
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
						final String hql = "select distinct lb.id from LettreBienvenue as lb" +
								" where lb.annulationDate is null" +
								" and exists (select etat.documentFiscal.id from EtatDocumentFiscal as etat where lb.id = etat.documentFiscal.id and etat.annulationDate is null and etat.etat = 'EMIS')" +
								" and not exists (select etat.documentFiscal.id from EtatDocumentFiscal as etat where lb.id = etat.documentFiscal.id and etat.annulationDate is null and etat.etat in ('RAPPELE', 'RETOURNE'))" +
								" and exists (select delai.documentFiscal.id from DelaiDocumentFiscal as delai where lb.id = delai.documentFiscal.id and delai.annulationDate is null and delai.delaiAccordeAu is not null and delai.etat = 'ACCORDE'" +
								"              group by delai.documentFiscal.id having max(delai.delaiAccordeAu) < :dateTraitement)" +
								" order by lb.id asc";
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
