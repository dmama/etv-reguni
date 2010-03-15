package ch.vd.uniregctb.declaration.source;

import java.util.List;

import ch.vd.uniregctb.type.CategorieImpotSource;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.LoggingStatusManager;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.BatchTransactionTemplate.BatchCallback;
import ch.vd.uniregctb.common.BatchTransactionTemplate.Behavior;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.parametrage.DelaisService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EnvoiSommationLRsEnMasseProcessor {

private final Logger LOGGER = Logger.getLogger(EnvoiLRsEnMasseProcessor.class);

	private static final int BATCH_SIZE = 100;

	private final PlatformTransactionManager transactionManager;
	private final HibernateTemplate hibernateTemplate;
	private final ListeRecapService lrService;
	private final DelaisService delaisService;

	public EnvoiSommationLRsEnMasseProcessor(PlatformTransactionManager transactionManager, HibernateTemplate hibernateTemplate,
			ListeRecapService lrService, DelaisService delaisService) {
		this.transactionManager = transactionManager;
		this.hibernateTemplate = hibernateTemplate;
		this.lrService = lrService;
		this.delaisService = delaisService;
	}

	/**
	 * Exécute la sommation des LRs.
	 *
	 * @param categorie      la catégorie de débiteurs à traiter; ou <b>null</b> pour traiter tous les catégories de débiteurs
	 * @param dateTraitement la date de traitement
	 * @param status         un status manager; ou <b>null</b> pour logger la progression dans log4j.
	 * @return les résultats du traitement
	 */
	public EnvoiSommationLRsResults run(final CategorieImpotSource categorie, final RegDate dateTraitement, StatusManager status) {

		if (status == null) {
			status = new LoggingStatusManager(LOGGER);
		}
		final StatusManager s = status;

		final EnvoiSommationLRsResults rapportFinal = new EnvoiSommationLRsResults(categorie, dateTraitement);

		//liste de toutes les LR à passer en revue
		final List<Long> list = getListIdLRs(dateTraitement, categorie);

		BatchTransactionTemplate<Long, EnvoiSommationLRsResults> template = new BatchTransactionTemplate<Long, EnvoiSommationLRsResults>(list, BATCH_SIZE, Behavior.REPRISE_AUTOMATIQUE,
				transactionManager, s, hibernateTemplate);
		template.execute(rapportFinal, new BatchCallback<Long, EnvoiSommationLRsResults>() {

			@Override
			public EnvoiSommationLRsResults createSubRapport() {
				return new EnvoiSommationLRsResults(categorie, dateTraitement);
			}

			@Override
			public boolean doInTransaction(List<Long> batch, EnvoiSommationLRsResults rapport) throws Exception {
				traiteBatch(batch, dateTraitement, s, rapport);
				return !s.interrupted();
			}

			@Override
			public void afterTransactionCommit() {
				int percent = (100 * rapportFinal.nbLRsTotal) / list.size();
				s.setMessage(String.format("%d sur %d listes récapitulatives traitées (%d%%) : (%d LR sommées)",
						rapportFinal.nbLRsTotal, list.size(), percent, rapportFinal.LRSommees.size()));
			}
		});

		if (status.interrupted()) {
			status.setMessage("L'envoi des sommations de listes récapitulatives a été interrompu."
					+ " Nombre de listes récapitulatives sommées au moment de l'interruption = " + rapportFinal.LRSommees.size());
			rapportFinal.interrompu = true;
		}
		else {
			status.setMessage("L'envoi des sommations de listes récapitulatives est terminé." + " Nombre de listes récapitulatives sommées = "
					+ rapportFinal.LRSommees.size() + ". Nombre d'erreurs = " + rapportFinal.SommationLREnErrors.size());
		}

		rapportFinal.end();
		return rapportFinal;
	}

	private void traiteBatch(List<Long> batch, RegDate dateTraitement, StatusManager status, EnvoiSommationLRsResults rapport) throws Exception {
		for (Long id : batch) {
			if (status.interrupted()) {
				break;
			}
			traiteLR(id, dateTraitement, rapport);
		}
	}

	private void traiteLR(Long id, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
		DeclarationImpotSource lr = (DeclarationImpotSource) hibernateTemplate.get(DeclarationImpotSource.class, id);

		// traitement de la LR
		traiteLR(lr, dateTraitement, rapport);

		++rapport.nbLRsTotal;
	}

	private void traiteLR(DeclarationImpotSource lr, RegDate dateTraitement, EnvoiSommationLRsResults rapport) throws Exception {
		if (lr.getDernierEtat().getEtat().equals(TypeEtatDeclaration.EMISE)) {
			RegDate dateDelaiSommation;
			if (lr.getDelaiAccordeAu() == null) {
				RegDate dateExpedition = lr.getDernierEtat().getDateObtention();
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

	@SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
	protected List<Long> getListIdLRs(final RegDate dateLimite, final CategorieImpotSource categorie) {

		final List<Long> ids = (List<Long>) hibernateTemplate.execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {

				SQLQuery sqlQuery = session.createSQLQuery("alter session set optimizer_index_caching = 90");
				sqlQuery.executeUpdate();
				sqlQuery = session.createSQLQuery("alter session set optimizer_index_cost_adj = 10");
				sqlQuery.executeUpdate();

				String sql = "SELECT lr.id"
						+ " FROM DeclarationImpotSource AS lr"
						+ " WHERE lr.annulationDate IS NULL";
				if (categorie != null) {
					sql += " AND lr.tiers.categorieImpotSource = :categorie";
				}
				sql += " AND EXISTS ("
						+ "   SELECT etat1.declaration.id"
						+ "   FROM EtatDeclaration AS etat1"
						+ "   WHERE lr.id = etat1.declaration.id"
						+ "     AND etat1.annulationDate IS NULL"
						+ "     AND etat1.etat = 'EMISE'"
						+ "     AND etat1.dateObtention IN ("
						+ "       SELECT MAX(etat2.dateObtention) "
						+ "       FROM EtatDeclaration AS etat2 "
						+ "       WHERE etat1.declaration.id = etat2.declaration.id AND etat2.annulationDate IS NULL))"
						+ " AND EXISTS ( "
						+ "   SELECT delai.declaration.id  FROM DelaiDeclaration AS delai"
						+ "   WHERE delai.declaration.id = lr.id"
						+ "   AND delai.annulationDate IS NULL"
						+ "   AND delai.delaiAccordeAu IS NOT NULL"
						+ "   GROUP BY delai.declaration.id"
						+ "   HAVING MAX(delai.delaiAccordeAu) < :dateLimite"
						+ " )";
				final Query query = session.createQuery(sql);

				if (categorie != null) {
					query.setParameter("categorie", categorie.name());
				}
				query.setParameter("dateLimite", dateLimite.index());
				return query.list();
			}
		});
		return ids;
	}
}
