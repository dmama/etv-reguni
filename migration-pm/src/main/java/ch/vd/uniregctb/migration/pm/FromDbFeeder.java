package ch.vd.uniregctb.migration.pm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.migration.pm.regpm.RegpmAssocieSC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLiquidateur;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLiquidation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;

/**
 * Feeder qui va chercher les données des PM dans la base de données du mainframe
 */
public class FromDbFeeder implements Feeder {

	private static final Logger LOGGER = LoggerFactory.getLogger(FromDbFeeder.class);

	private PlatformTransactionManager regpmTransactionManager;
	private SessionFactory sessionFactory;

	private static void forceLoad(RegpmEtablissement etablissement, Graphe graphe) {
		if (!graphe.register(etablissement)) {
			return;
		}

		// lazy init
		etablissement.getDomicilesEtablissements().size();

		// chargement de l'entreprise/individu lié(e)
		forceLoad(etablissement.getEntreprise(), graphe);
		forceLoad(etablissement.getIndividu(), graphe);

		// chargement des succursales
		for (RegpmEtablissement succ : etablissement.getSuccursales()) {
			forceLoad(succ, graphe);
		}
	}

	private static void forceLoad(RegpmIndividu individu, Graphe graphe) {
		if (!graphe.register(individu)) {
			return;
		}

		// lazy init
		individu.getCaracteristiques().size();
		individu.getAdresses().size();
	}

	private static void forceLoad(RegpmEntreprise pm, Graphe graphe) {
		if (!graphe.register(pm)) {
			return;
		}

		// lazy init
		pm.getRaisonsSociales().size();
		pm.getInscriptionsRC().size();
		pm.getRadiationsRC().size();
		pm.getFormesJuridiques().size();
		pm.getSieges().size();
		pm.getRegimesFiscauxCH().size();
		pm.getRegimesFiscauxVD().size();
		pm.getExercicesCommerciaux().size();
		pm.getAdresses().size();
		pm.getAssujettissements().size();
		pm.getForsPrincipaux().size();
		pm.getForsSecondaires().size();
		pm.getAllegementsFiscaux().size();
		pm.getQuestionnairesSNC().size();
		pm.getCapitaux().size();

		// lazy init + éventuels liens vers d'autres entités
		{
			final Set<RegpmDossierFiscal> dfs = pm.getDossiersFiscaux();
			for (RegpmDossierFiscal df : dfs) {

				// lazy init
				df.getDemandesDelai().size();
			}
		}
		{
			final Set<RegpmEtablissement> etablissements = pm.getEtablissements();
			for (RegpmEtablissement etablissement : etablissements) {
				forceLoad(etablissement, graphe);
			}
		}
		{
			final Set<RegpmAssocieSC> associes = pm.getAssociesSC();
			for (RegpmAssocieSC associe : associes) {
				if (associe != null) {
					forceLoad(associe.getEntreprise(), graphe);
					forceLoad(associe.getEtablissement(), graphe);
					forceLoad(associe.getIndividu(), graphe);
				}
			}
		}
		{
			final Set<RegpmFusion> avant = pm.getFusionsAvant();
			for (RegpmFusion fusion : avant) {
				if (fusion != null) {
					forceLoad(fusion.getEntrepriseApres(), graphe);
					forceLoad(fusion.getEntrepriseAvant(), graphe);
				}
			}
		}
		{
			final Set<RegpmFusion> apres = pm.getFusionsApres();
			for (RegpmFusion fusion : apres) {
				if (fusion != null) {
					forceLoad(fusion.getEntrepriseApres(), graphe);
					forceLoad(fusion.getEntrepriseAvant(), graphe);
				}
			}
		}
		{
			final Set<RegpmEtatEntreprise> etats = pm.getEtatsEntreprise();
			for (RegpmEtatEntreprise etat : etats) {
				if (etat != null) {
					final SortedSet<RegpmPrononceFaillite> prononces = etat.getPrononcesFaillite();
					for (RegpmPrononceFaillite p : prononces) {
						final Set<RegpmLiquidateur> liqs = p.getLiquidateurs();
						for (RegpmLiquidateur liq : liqs) {
							forceLoad(liq.getEntreprise(), graphe);
							forceLoad(liq.getEtablissement(), graphe);
							forceLoad(liq.getIndividu(), graphe);
						}
					}

					final SortedSet<RegpmLiquidation> liquidations = etat.getLiquidations();
					for (RegpmLiquidation liquidation : liquidations) {
						final Set<RegpmLiquidateur> liqs = liquidation.getLiquidateurs();
						for (RegpmLiquidateur liq : liqs) {
							forceLoad(liq.getEntreprise(), graphe);
							forceLoad(liq.getEtablissement(), graphe);
							forceLoad(liq.getIndividu(), graphe);
						}
					}
				}
			}
		}
		{
			final Set<RegpmMandat> mandataires = pm.getMandataires();
			for (RegpmMandat mandataire : mandataires) {
				if (mandataire != null) {
					forceLoad(mandataire.getMandataireEntreprise(), graphe);
					forceLoad(mandataire.getMandataireEtablissement(), graphe);
					forceLoad(mandataire.getMandataireIndividu(), graphe);
				}
			}
		}

		// TODO immeubles, documents dégrèvement, rattachements propriétaires... (si nécessaire dans cette phase de migration)
	}

	public void setRegpmTransactionManager(PlatformTransactionManager regpmTransactionManager) {
		this.regpmTransactionManager = regpmTransactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void feed(Worker worker) throws Exception {
		// première étape, allons chercher les identifiants des entreprises à migrer...
		LOGGER.info("Récupération des entreprises de RegPM à migrer...");
		final List<Long> ids = getIds();
		LOGGER.info("Récupération des identifiants des " + ids.size() + " entreprises de RegPM terminée.");

		// container des identifiants des entreprises déjà présentes dans un graphe
		final Set<Long> idsDejaTrouvees = new HashSet<>(ids.size());

		// boucle sur les identifiants d'entreprise trouvés et envoi vers le worker
		for (long id : ids) {
//		for (long id : Arrays.asList(27, 848, 61)) {
			if (!idsDejaTrouvees.contains(id)) {
				final Graphe graphe = loadGraphe(id);
				idsDejaTrouvees.addAll(graphe.getEntreprises().keySet());
				worker.onGraphe(graphe);
			}
		}
	}

	private List<Long> getIds() {
		final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
		template.setReadOnly(true);
		return template.execute(new TransactionCallback<List<Long>>() {
			@Override
			public List<Long> doInTransaction(TransactionStatus status) {
				final Session session = sessionFactory.getCurrentSession();
				//noinspection JpaQlInspection
				final Query query = session.createQuery("select e.id from RegpmEntreprise e");
				//noinspection unchecked
				return query.list();
			}
		});
	}

	private Graphe loadGraphe(final long idEntreprise) {
		final long start = System.nanoTime();
		final Graphe graphe = new Graphe();     // vide au départ...
		try {
			final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
			template.setReadOnly(true);
			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final Session session = sessionFactory.getCurrentSession();
					final RegpmEntreprise pm = (RegpmEntreprise) session.get(RegpmEntreprise.class, idEntreprise);
					forceLoad(pm, graphe);
				}
			});
			return graphe;
		}
		finally {
			final long end = System.nanoTime();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Chargement de l'entreprise " + idEntreprise + " (" + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms) : " + graphe);
			}
		}
	}
}
