package ch.vd.uniregctb.migration.pm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.metier.bouclement.BouclementService;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
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
import ch.vd.uniregctb.migration.pm.regpm.RegpmMotifEnvoi;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;
import ch.vd.uniregctb.tiers.Bouclement;

public class Migrator implements SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);

	private PlatformTransactionManager regpmTransactionManager;
	private SessionFactory sessionFactory;
	private BouclementService bouclementService;

	private Thread thread;

	public void setRegpmTransactionManager(PlatformTransactionManager regpmTransactionManager) {
		this.regpmTransactionManager = regpmTransactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setBouclementService(BouclementService bouclementService) {
		this.bouclementService = bouclementService;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		thread = new Thread("Migrator") {
			@Override
			public void run() {
				migrate();
			}
		};
		thread.start();
	}

	@Override
	public void stop() {
		thread.interrupt();
		try {
			thread.join();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return thread != null && thread.isAlive();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	private void migrate() {
		LOGGER.info("Démarrage de la migration...");
		try {

			// première étape, allons chercher les identifiants des entreprises à migrer...
			LOGGER.info("Récupération des entreprises de RegPM à migrer...");
			final List<Long> ids = getIds();
			LOGGER.info("Récupération des identifiants des " + ids.size() + " entreprises de RegPM terminée.");

//			for (long id : ids) {
//				migrate(id);
//			}
			migrate(61);
		}
		catch (Exception e) {
			LOGGER.error("Exception levée dans le thread principal de migration", e);
		}
		finally {
			LOGGER.info("Fin de la migration.");
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

	private static final class IdsDejaCharges {
		final Set<Long> entreprises = new TreeSet<>();
		final Set<Long> etablissements = new TreeSet<>();
		final Set<Long> individus = new TreeSet<>();

		@Override
		public String toString() {
			final List<String> array = new ArrayList<>(3);
			if (!entreprises.isEmpty()) {
				array.add(String.format("%d entreprise(s) (%s)", entreprises.size(), Arrays.toString(entreprises.toArray(new Long[entreprises.size()]))));
			}
			if (!etablissements.isEmpty()) {
				array.add(String.format("%d établissement(s) (%s)", etablissements.size(), Arrays.toString(etablissements.toArray(new Long[etablissements.size()]))));
			}
			if (!individus.isEmpty()) {
				array.add(String.format("%d individu(s) (%s)", individus.size(), Arrays.toString(individus.toArray(new Long[individus.size()]))));
			}

			if (array.isEmpty()) {
				return "rien (???)";
			}
			else {
				final StringBuilder b = new StringBuilder(array.get(0));
				for (int i = 1 ; i < array.size() ; ++ i) {
					b.append(", ").append(array.get(i));
				}
				return b.toString();
			}
		}
	}

	private RegpmEntreprise loadEntreprise(final long id) {
		final long start = System.nanoTime();
		final IdsDejaCharges idsCharges = new IdsDejaCharges();     // vide au départ...
		try {
			final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
			template.setReadOnly(true);
			return template.execute(new TransactionCallback<RegpmEntreprise>() {
				@Override
				public RegpmEntreprise doInTransaction(TransactionStatus status) {
					final Session session = sessionFactory.getCurrentSession();
					final RegpmEntreprise pm = (RegpmEntreprise) session.get(RegpmEntreprise.class, id);
					forceLoad(pm, idsCharges);
					return pm;
				}
			});
		}
		finally {
			final long end = System.nanoTime();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Chargement de l'entreprise " + id + " (" + TimeUnit.NANOSECONDS.toMillis(end - start) + " ms) : " + idsCharges);
			}
		}
	}

	/**
	 * @param entity une entité dont l'identifiant est un long
	 * @param idsDejaCharges le set des ids du même type déjà chargés
	 * @return si la récursion doit être stoppée (parce qu'il n'y a pas d'entité ou parce que son identifiant est déjà dans le set - après l'appel à cette méthode l'identifiant est de toute façon dans le set)
	 */
	private static boolean shouldStopRecursion(WithLongId entity, Set<Long> idsDejaCharges) {
		if (entity == null) {
			return true;
		}

		final Long id = entity.getId();
		if (idsDejaCharges.contains(id)) {
			return true;
		}

		// la prochaine fois, on saura que c'est fait...
		idsDejaCharges.add(id);
		return false;
	}

	private static void forceLoad(RegpmEtablissement etablissement, IdsDejaCharges idsDejaCharges) {
		if (shouldStopRecursion(etablissement, idsDejaCharges.etablissements)) {
			return;
		}

		// lazy init
		etablissement.getDomicilesEtablissements().size();

		// chargement de l'entreprise liée
		forceLoad(etablissement.getEntreprise(), idsDejaCharges);

		// chargement des succursales
		for (RegpmEtablissement succ : etablissement.getSuccursales()) {
			forceLoad(succ, idsDejaCharges);
		}
	}

	private static void forceLoad(RegpmIndividu individu, IdsDejaCharges idsDejaCharges) {
		if (shouldStopRecursion(individu, idsDejaCharges.individus)) {
			return;
		}

		// lazy init
		individu.getCaracteristiques().size();
		individu.getAdresses().size();
	}

	private static void forceLoad(RegpmEntreprise pm, IdsDejaCharges idsDejaCharges) {
		if (shouldStopRecursion(pm, idsDejaCharges.entreprises)) {
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
				forceLoad(etablissement, idsDejaCharges);
			}
		}
		{
			final Set<RegpmAssocieSC> associes = pm.getAssociesSC();
			for (RegpmAssocieSC associe : associes) {
				if (associe != null) {
					forceLoad(associe.getEntreprise(), idsDejaCharges);
					forceLoad(associe.getEtablissement(), idsDejaCharges);
					forceLoad(associe.getIndividu(), idsDejaCharges);
				}
			}
		}
		{
			final Set<RegpmFusion> avant = pm.getFusionsAvant();
			for (RegpmFusion fusion : avant) {
				if (fusion != null) {
					forceLoad(fusion.getEntrepriseApres(), idsDejaCharges);
					forceLoad(fusion.getEntrepriseAvant(), idsDejaCharges);
				}
			}
		}
		{
			final Set<RegpmFusion> apres = pm.getFusionsApres();
			for (RegpmFusion fusion : apres) {
				if (fusion != null) {
					forceLoad(fusion.getEntrepriseApres(), idsDejaCharges);
					forceLoad(fusion.getEntrepriseAvant(), idsDejaCharges);
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
							forceLoad(liq.getEntreprise(), idsDejaCharges);
							forceLoad(liq.getEtablissement(), idsDejaCharges);
							forceLoad(liq.getIndividu(), idsDejaCharges);
						}
					}

					final SortedSet<RegpmLiquidation> liquidations = etat.getLiquidations();
					for (RegpmLiquidation liquidation : liquidations) {
						final Set<RegpmLiquidateur> liqs = liquidation.getLiquidateurs();
						for (RegpmLiquidateur liq : liqs) {
							forceLoad(liq.getEntreprise(), idsDejaCharges);
							forceLoad(liq.getEtablissement(), idsDejaCharges);
							forceLoad(liq.getIndividu(), idsDejaCharges);
						}
					}
				}
			}
		}
		{
			final Set<RegpmMandat> mandataires = pm.getMandataires();
			for (RegpmMandat mandataire : mandataires) {
				if (mandataire != null) {
					forceLoad(mandataire.getMandataireEntreprise(), idsDejaCharges);
					forceLoad(mandataire.getMandataireEtablissement(), idsDejaCharges);
					forceLoad(mandataire.getMandataireIndividu(), idsDejaCharges);
				}
			}
		}

		// TODO immeubles, documents dégrèvement, rattachements propriétaires... (si nécessaire dans cette phase de migration)
	}

	private void migrate(long id) {
		final RegpmEntreprise pm = loadEntreprise(id);

//		traiterBouclements(pm);

		// TODO va quand-même falloir bosser un peu, maintenant !!!
		int i = 0;
	}

	private List<Bouclement> traiterBouclements(RegpmEntreprise pm) {
		// il faut d'abord extraire les dates de bouclements passées
		final List<RegDate> datesConnues = new ArrayList<>();
		datesConnues.add(pm.getDateBouclementFutur());
		for (RegpmDossierFiscal df : pm.getDossiersFiscaux()) {
			// TODO faut-il prendre aussi les autres motifs ?
			if (df.getMotifEnvoi() == RegpmMotifEnvoi.FIN_EXER) {
				datesConnues.add(df.getAssujettissement().getDateFin());
			}
		}

		// TODO attention, il n'y a pas toujours une date de bouclement futur

		final List<Bouclement> bouclements = bouclementService.extractBouclementsDepuisDates(datesConnues, 12);

		// on fait une petite vérification quand aux dates des dossiers fiscaux passés
		final List<ExerciceCommercial> exercices = bouclementService.getExercicesCommerciaux(bouclements, new DateRangeHelper.Range(pm.getDateInscriptionRC(), pm.getDateBouclementFutur()));

		return bouclements;
	}
}
