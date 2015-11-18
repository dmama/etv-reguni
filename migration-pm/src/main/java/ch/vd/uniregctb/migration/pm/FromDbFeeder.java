package ch.vd.uniregctb.migration.pm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.GentilIterator;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAppartenanceGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmAssocieSC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmDossierFiscal;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEnvironnementTaxation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtatEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmFusion;
import ch.vd.uniregctb.migration.pm.regpm.RegpmGroupeProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.RegpmImmeuble;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLiquidateur;
import ch.vd.uniregctb.migration.pm.regpm.RegpmLiquidation;
import ch.vd.uniregctb.migration.pm.regpm.RegpmMandat;
import ch.vd.uniregctb.migration.pm.regpm.RegpmPrononceFaillite;
import ch.vd.uniregctb.migration.pm.regpm.RegpmRattachementProprietaire;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;
import ch.vd.uniregctb.migration.pm.utils.DataLoadHelper;

/**
 * Feeder qui va chercher les données des PM dans la base de données du mainframe
 */
public class FromDbFeeder implements Feeder, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(FromDbFeeder.class);

	private PlatformTransactionManager regpmTransactionManager;
	private SessionFactory sessionFactory;
	private Set<Long> idsEntreprisesDejaMigrees;
	private String nomFichierIdentifiantsAExtraire;
	private MigrationMode mode;
	private volatile boolean shutdownInProgress = false;

	/**
	 * Interface de remplissage du cache
	 */
	private interface GrapheRecorder {

		/**
		 * @param entreprise entreprise à enregistrer
		 * @return <code>true</code> si l'entreprise a été enregistrée, <code>false</code> si elle l'était déjà
		 */
		boolean register(RegpmEntreprise entreprise);

		/**
		 * @param etablissement établissement à enregistrer
		 * @return <code>true</code> si l'établissement a été enregistré, <code>false</code> s'il l'était déjà
		 */
		boolean register(RegpmEtablissement etablissement);

		/**
		 * @param individu individu à enregistrer
		 * @return <code>true</code> si l'individu a été enregistré, <code>false</code> s'il l'était déjà
		 */
		boolean register(RegpmIndividu individu);

		/**
		 * @param immeuble immeuble à enregistrer
		 * @return <code>true</code> si l'immeuble a été enregistré, <code>false</code> s'il l'était déjà
		 */
		boolean register(RegpmImmeuble immeuble);
	}

	/**
	 * Implémentation de graphe
	 */
	private static class GrapheImpl implements Serializable, Graphe, GrapheRecorder {

		private final Map<Long, RegpmEntreprise> entreprises = new TreeMap<>();
		private final Map<Long, RegpmEtablissement> etablissements = new TreeMap<>();
		private final Map<Long, RegpmIndividu> individus = new TreeMap<>();
		private final Map<Long, RegpmImmeuble> immeubles = new TreeMap<>();

		@Override
		public String toString() {
			final List<String> array = new ArrayList<>(4);
			if (!entreprises.isEmpty()) {
				array.add(String.format("%d entreprise(s) (%s)", entreprises.size(), Arrays.toString(entreprises.keySet().toArray(new Long[entreprises.size()]))));
			}
			if (!etablissements.isEmpty()) {
				array.add(String.format("%d établissement(s) (%s)", etablissements.size(), Arrays.toString(etablissements.keySet().toArray(new Long[etablissements.size()]))));
			}
			if (!individus.isEmpty()) {
				array.add(String.format("%d individu(s) (%s)", individus.size(), Arrays.toString(individus.keySet().toArray(new Long[individus.size()]))));
			}
			if (!immeubles.isEmpty()) {
				array.add(String.format("%d immeuble(s) (%s)", immeubles.size(), Arrays.toString(immeubles.keySet().toArray(new Long[immeubles.size()]))));
			}

			if (array.isEmpty()) {
				return "rien (???)";
			}
			else {
				return array.stream().collect(Collectors.joining(", "));
			}
		}

		@Override
		public Map<Long, RegpmEntreprise> getEntreprises() {
			return Collections.unmodifiableMap(entreprises);
		}

		@Override
		public Map<Long, RegpmEtablissement> getEtablissements() {
			return Collections.unmodifiableMap(etablissements);
		}

		@Override
		public Map<Long, RegpmIndividu> getIndividus() {
			return Collections.unmodifiableMap(individus);
		}

		public Map<Long, RegpmImmeuble> getImmeubles() {
			return Collections.unmodifiableMap(immeubles);
		}

		/**
		 * @param entreprise entreprise à enregistrer
		 * @return <code>true</code> si l'entreprise a été enregistrée, <code>false</code> si elle l'était déjà
		 */
		@Override
		public boolean register(RegpmEntreprise entreprise) {
			return register(entreprise, entreprises);
		}

		/**
		 * @param etablissement établissement à enregistrer
		 * @return <code>true</code> si l'établissement a été enregistré, <code>false</code> s'il l'était déjà
		 */
		@Override
		public boolean register(RegpmEtablissement etablissement) {
			return register(etablissement, etablissements);
		}

		/**
		 * @param individu individu à enregistrer
		 * @return <code>true</code> si l'individu a été enregistré, <code>false</code> s'il l'était déjà
		 */
		@Override
		public boolean register(RegpmIndividu individu) {
			return register(individu, individus);
		}

		/**
		 * @param immeuble immeuble à enregistrer
		 * @return <code>true</code> si l'immeuble a été enregistré, <code>false</code> s'il l'était déjà
		 */
		@Override
		public boolean register(RegpmImmeuble immeuble) {
			return register(immeuble, immeubles);
		}

		/**
		 * @param entity une entité dont l'identifiant est un long
		 * @param entities la map des entités du même type déjà enregistrées
		 * @return <code>true</code> le l'entité à été enregistrée, <code>false</code> si elle l'était déjà
		 */
		private static <T extends WithLongId> boolean register(T entity, Map<Long, T> entities) {
			if (entity == null) {
				return false;
			}

			final Long id = entity.getId();
			if (entities.containsKey(id)) {
				return false;
			}

			// la prochaine fois, on saura que c'est fait...
			entities.put(id, entity);
			return true;
		}
	}

	private static void forceLoad(RegpmImmeuble immeuble, GrapheRecorder graphe) {
		if (!graphe.register(immeuble)) {
			return;
		}

		// le lien vers la parcelle
		forceLoad(immeuble.getParcelle(), graphe);
	}

	private static void forceLoadProprietaire(GrapheRecorder graphe, Supplier<Collection<RegpmRattachementProprietaire>> rattachementsProprietaires, Supplier<Collection<RegpmAppartenanceGroupeProprietaire>> appartenancesGroupeProprietaire) {
		// rattachements propriétaires directs
		final Collection<RegpmRattachementProprietaire> rattachements = rattachementsProprietaires != null ? rattachementsProprietaires.get() : Collections.emptyList();
		for (RegpmRattachementProprietaire rattachement : rattachements) {
			forceLoad(rattachement.getImmeuble(), graphe);
		}

		// rattachements propriétaires au travers de groupes de propriétaires
		final Collection<RegpmAppartenanceGroupeProprietaire> appartenancesGroupe = appartenancesGroupeProprietaire != null ? appartenancesGroupeProprietaire.get() : Collections.emptyList();
		for (RegpmAppartenanceGroupeProprietaire appartenance : appartenancesGroupe) {
			final RegpmGroupeProprietaire groupe = appartenance.getGroupeProprietaire();
			if (groupe != null) {
				for (RegpmRattachementProprietaire rattachement : groupe.getRattachementsProprietaires()) {
					forceLoad(rattachement.getImmeuble(), graphe);
				}
			}
		}
	}

	private static void forceLoad(RegpmEtablissement etablissement, GrapheRecorder graphe) {
		if (!graphe.register(etablissement)) {
			return;
		}

		// lazy init
		etablissement.getDomicilesEtablissements().size();
		etablissement.getInscriptionsRC().size();
		etablissement.getRadiationsRC().size();
		etablissement.getMandants().size();
		etablissement.getNotes().size();

		// chargement de l'entreprise/individu lié(e)
		forceLoad(etablissement.getEntreprise(), graphe);
		forceLoad(etablissement.getIndividu(), graphe);

		// chargement des succursales
		for (RegpmEtablissement succ : etablissement.getSuccursales()) {
			forceLoad(succ, graphe);
		}

		// immeubles
		forceLoadProprietaire(graphe, etablissement::getRattachementsProprietaires, etablissement::getAppartenancesGroupeProprietaire);
	}

	private static void forceLoad(RegpmIndividu individu, GrapheRecorder graphe) {
		if (!graphe.register(individu)) {
			return;
		}

		// lazy init
		individu.getCaracteristiques().size();
		individu.getAdresses().size();
		individu.getMandants().size();
	}

	private static void forceLoad(RegpmEntreprise pm, GrapheRecorder graphe) {
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
		pm.getMandants().size();
		pm.getNotes().size();
		pm.getCriteresSegmentation().size();

		// lazy init + éventuels liens vers d'autres entités
		{
			final Set<RegpmDossierFiscal> dfs = pm.getDossiersFiscaux();
			for (RegpmDossierFiscal df : dfs) {

				// lazy init
				df.getDemandesDelai().size();

				// lazy init en profondeur
				for (RegpmEnvironnementTaxation et : df.getEnvironnementsTaxation()) {
					et.getDecisionsTaxation().size();
				}
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

		// immeubles
		forceLoadProprietaire(graphe, pm::getRattachementsProprietaires, pm::getAppartenancesGroupeProprietaire);

		// TODO documents dégrèvement... (si nécessaire dans cette phase de migration)
	}

	public void setRegpmTransactionManager(PlatformTransactionManager regpmTransactionManager) {
		this.regpmTransactionManager = regpmTransactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void setIdsEntreprisesDejaMigrees(Set<Long> idsEntreprisesDejaMigrees) {
		this.idsEntreprisesDejaMigrees = idsEntreprisesDejaMigrees;
	}

	public void setNomFichierIdentifiantsAExtraire(String nomFichierIdentifiantsAExtraire) {
		this.nomFichierIdentifiantsAExtraire = StringUtils.trimToNull(nomFichierIdentifiantsAExtraire);
	}

	public void setMode(MigrationMode mode) {
		this.mode = mode;
	}

	@Override
	public void destroy() throws Exception {
		this.shutdownInProgress = true;
	}

	@Override
	public void feed(Worker worker) throws Exception {
		// première étape, allons chercher les identifiants des entreprises à migrer...
		LOGGER.info("Récupération des entreprises de RegPM à migrer...");
		final List<Long> ids = getIds();
		LOGGER.info("Récupération des identifiants des " + ids.size() + " entreprises de RegPM terminée.");

		// container des identifiants des entreprises déjà traitées (dans un graphe précédent ou même carrément déjà migrées)
		final Set<Long> idsDejaTrouvees = new HashSet<>(ids.size());
		if (mode != MigrationMode.DUMP) {
			// en mode DUMP, on ne fait pas attention à ce qu'il y a déja en base de destination...
			idsDejaTrouvees.addAll(idsEntreprisesDejaMigrees);
		}

		// boucle sur les identifiants d'entreprise trouvés et envoi vers le worker
		final GentilIterator<Long> idIterator = new GentilIterator<>(ids);
		while (idIterator.hasNext()) {

			// en cas de demande d'arrêt du programme, le thread de feed est interrompu
			if (shutdownInProgress || Thread.currentThread().isInterrupted()) {
				break;
			}

			final Long id = idIterator.next();

			// on ne recrée pas de graphe autour d'une entreprise qui a déjà été prise
			// dans un graphe précédent (ou déjà migrée dans un run précédent)
			if (!idsDejaTrouvees.contains(id)) {

				// chargement du graphe depuis la base de données
				final Graphe graphe = loadGraphe(id);

				// récupération des IDs des entreprises concernées
				idsDejaTrouvees.addAll(graphe.getEntreprises().keySet());

				// traitement du graphe (migration, dump...)
				worker.onGraphe(graphe);
			}

			// un peu de log pour mesurer l'avancement...
			if (idIterator.isAtNewPercent()) {
				LOGGER.info(String.format("Avancement de l'extraction des entreprises depuis la base : %d %%", idIterator.getPercent()));
			}
		}
	}

	private List<Long> getIds() throws Exception {
		return nomFichierIdentifiantsAExtraire == null ? getAllIds() : getSpecificIdsFromFile(nomFichierIdentifiantsAExtraire);
	}

	private List<Long> getSpecificIdsFromFile(String filename) throws IOException {
		final List<Long> brut = DataLoadHelper.loadIdentifiantsPM(filename, () -> "les identifiants des PM à migrer");
		return new ArrayList<>(new LinkedHashSet<>(brut));      // on conserve l'ordre mais on enlève les éventuels doublons
	}

	private List<Long> getAllIds() {
		final TransactionTemplate template = new TransactionTemplate(regpmTransactionManager);
		template.setReadOnly(true);
		return template.execute(status -> {
			final Session session = sessionFactory.getCurrentSession();
			//noinspection JpaQlInspection
			final Query query = session.createQuery("select e.id from RegpmEntreprise e");
			//noinspection unchecked
			return (List<Long>) query.list();
		});
	}

	protected Graphe loadGraphe(final long idEntreprise) {
		final long start = System.nanoTime();
		final GrapheImpl graphe = new GrapheImpl();     // vide au départ...
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
