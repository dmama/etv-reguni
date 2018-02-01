package ch.vd.unireg.declaration.source;

import java.util.ArrayList;
import java.util.Collections;
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
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.LoggingStatusManager;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.IdentifiantDeclaration;
import ch.vd.unireg.hibernate.HibernateCallback;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.parametrage.DelaisService;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EnvoiSommationLRsEnMasseProcessor {

private final Logger LOGGER = LoggerFactory.getLogger(EnvoiLRsEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ListeRecapService lrService;
	private final DelaisService delaisService;
	private final TiersService tiersService;
	private final AdresseService adresseService;

	public EnvoiSommationLRsEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
	                                         ListeRecapService lrService, DelaisService delaisService, TiersService tiersService, AdresseService adresseService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.delaisService = delaisService;
		this.tiersService = tiersService;
		this.adresseService = adresseService;
	}

	/**
	 * Exécute la sommation des LRs.
	 *
	 * @param categorie      la catégorie de débiteurs à traiter; ou <b>null</b> pour traiter tous les catégories de débiteurs
	 * @param dateFinPeriode paramètre optionnel qui - s'il est renseigné - est utilisé pour restreindre les sommations aux LRs dont la période finit à la date spécifiée. 
	 * @param dateTraitement la date de traitement
	 * @param status         un status manager; ou <b>null</b> pour logger la progression dans log4j.
	 * @return les résultats détaillés du run.
	 */
	public EnvoiSommationLRsResults run(final CategorieImpotSource categorie, final RegDate dateFinPeriode, final RegDate dateTraitement, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final EnvoiSommationLRsResults rapportFinal = new EnvoiSommationLRsResults(categorie, dateFinPeriode, dateTraitement, tiersService, adresseService);

		// liste de toutes les LR à passer en revue
		final List<IdentifiantDeclaration> list = getListIdLRs(dateFinPeriode, dateTraitement, categorie);

		final BatchTransactionTemplateWithResults<IdentifiantDeclaration, EnvoiSommationLRsResults> template = new BatchTransactionTemplateWithResults<>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE, transactionManager, s);
		template.execute(rapportFinal, new BatchWithResultsCallback<IdentifiantDeclaration, EnvoiSommationLRsResults>() {

			@Override
			public EnvoiSommationLRsResults createSubRapport() {
				return new EnvoiSommationLRsResults(categorie, dateFinPeriode, dateTraitement, tiersService, adresseService);
			}

			@Override
			public boolean doInTransaction(List<IdentifiantDeclaration> batch, EnvoiSommationLRsResults rapport) throws Exception {
				traiteBatch(batch, dateTraitement, s, rapport);
				return !s.isInterrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int percent = (100 * rapportFinal.nbLRsTotal) / list.size();
				s.setMessage(String.format("%d sur %d listes récapitulatives traitées (%d LR sommées)",
						rapportFinal.nbLRsTotal, list.size(), rapportFinal.lrSommees.size()), percent);
			}
		}, null);

		if (status.isInterrupted()) {
			status.setMessage("L'envoi des sommations de listes récapitulatives a été interrompu."
					+ " Nombre de listes récapitulatives sommées au moment de l'interruption = " + rapportFinal.lrSommees.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi des sommations de listes récapitulatives est terminé." + " Nombre de listes récapitulatives sommées = "
					+ rapportFinal.lrSommees.size() + ". Nombre d'erreurs = " + rapportFinal.sommationLREnErreurs.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<IdentifiantDeclaration> batch, RegDate dateTraitement, StatusManager status, EnvoiSommationLRsResults rapport) throws Exception {
		for (IdentifiantDeclaration id : batch) {
			if (status.isInterrupted()) {
				break;
			}
			traiteLR(id, dateTraitement, rapport);
		}
	}

	private void traiteLR(IdentifiantDeclaration id, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
		final DeclarationImpotSource lr = hibernateTemplate.get(DeclarationImpotSource.class, id.getIdDeclaration());

		// traitement de la LR
		traiteLR(lr, dateTraitement, rapport);

		++rapport.nbLRsTotal;
	}

	protected void traiteLR(DeclarationImpotSource lr, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
		if (lr.getDernierEtatDeclaration().getEtat() == TypeEtatDocumentFiscal.EMIS) {
			RegDate dateDelaiSommation;
			if (lr.getDelaiAccordeAu() == null) {
				final RegDate dateExpedition = lr.getDernierEtatDeclaration().getDateObtention();
				// Délai de retour des listes
				dateDelaiSommation = delaisService.getDateFinDelaiRetourListeRecapitulative(dateExpedition, lr.getDateFin());
				// Ajout du délai administratif avant l'envoi de la sommation
				dateDelaiSommation = delaisService.getDateFinDelaiEnvoiSommationListeRecapitulative(dateDelaiSommation);
			}
			else {
				// Ajout du délai administratif avant l'envoi de la sommation
				dateDelaiSommation = delaisService.getDateFinDelaiEnvoiSommationListeRecapitulative(lr.getDelaiAccordeAu());
			}
			if (dateTraitement.isAfter(dateDelaiSommation)) {
				lrService.imprimerSommationLR(lr, dateTraitement);
				rapport.addLRSommee((DebiteurPrestationImposable)lr.getTiers(), lr);
			}
		}
	}

	protected List<IdentifiantDeclaration> getListIdLRs(final RegDate dateFinPeriode, final RegDate dateLimite, final CategorieImpotSource categorie) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final List<IdentifiantDeclaration> ids = template.execute(new TransactionCallback<List<IdentifiantDeclaration>>() {
			@Override
			public List<IdentifiantDeclaration> doInTransaction(TransactionStatus status) {
				final List<Object[]> aSommer = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
					@Override
					public List<Object[]> doInHibernate(Session session) throws HibernateException {

						final StringBuilder b = new StringBuilder();
						b.append("SELECT lr.id, lr.tiers.id FROM DeclarationImpotSource AS lr");
						b.append(" WHERE lr.annulationDate IS NULL");
						if (dateFinPeriode != null) {
							b.append(" AND lr.dateFin <= :dateFin");
						}
						if (categorie != null) {
							b.append(" AND lr.tiers.categorieImpotSource = :categorie");
						}
						b.append(" AND EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE lr.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class = EtatDeclarationEmise)");
						b.append(" AND NOT EXISTS (SELECT etat.declaration.id FROM EtatDeclaration AS etat WHERE lr.id = etat.declaration.id AND etat.annulationDate IS NULL AND etat.class IN (EtatDeclarationRetournee, EtatDeclarationSommee, EtatDeclarationRappelee, EtatDeclarationSuspendue))");
						b.append(" AND EXISTS (SELECT delai.declaration.id FROM DelaiDeclaration AS delai WHERE lr.id = delai.declaration.id AND delai.annulationDate IS NULL AND delai.delaiAccordeAu IS NOT NULL AND delai.etat = 'ACCORDE'");
						b.append(" GROUP BY delai.declaration.id HAVING MAX(delai.delaiAccordeAu) < :dateLimite)");
						final String sql = b.toString();

						final Query query = session.createQuery(sql);
						if (dateFinPeriode != null) {
							query.setParameter("dateFin", dateFinPeriode);
						}
						if (categorie != null) {
							query.setParameter("categorie", categorie);
						}
						query.setParameter("dateLimite", dateLimite);
						//noinspection unchecked
						return query.list();
					}
				});

				final List<IdentifiantDeclaration> ids;
				if (aSommer != null && !aSommer.isEmpty()) {
					ids = new ArrayList<>(aSommer.size());
					for (Object[] elts : aSommer) {
						final long idLr = ((Number) elts[0]).longValue();
						final long idDebiteur = ((Number) elts[1]).longValue();
						ids.add(new IdentifiantDeclaration(idLr, idDebiteur));
					}
				}
				else {
					ids = Collections.emptyList();
				}
				return ids;
			}
		});

		return ids;
	}
}
