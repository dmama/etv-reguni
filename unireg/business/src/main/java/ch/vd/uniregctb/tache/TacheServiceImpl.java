package ch.vd.uniregctb.tache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.jetbrains.annotations.Nullable;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.FiscalDateHelper;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImposition;
import ch.vd.uniregctb.metier.assujettissement.PeriodeImpositionService;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tache.sync.AddDI;
import ch.vd.uniregctb.tache.sync.AnnuleTache;
import ch.vd.uniregctb.tache.sync.Context;
import ch.vd.uniregctb.tache.sync.DeleteDI;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tache.sync.UpdateDI;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheControleDossier;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheDAO.TacheStats;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheNouveauDossier;
import ch.vd.uniregctb.tiers.TacheTransmissionDossier;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * Service permettant la génération de tâches à la suite d'événements fiscaux
 */
public class TacheServiceImpl implements TacheService {

	private static final Logger LOGGER = Logger.getLogger(TacheServiceImpl.class);

	private TacheDAO tacheDAO;
	private DeclarationImpotOrdinaireDAO diDAO;
	private DeclarationImpotService diService;
	private ParametreAppService parametres;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;
	private PlatformTransactionManager transactionManager;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;
	private Map<Integer, TacheStats> tacheStatsPerOid = new HashMap<Integer, TacheStats>();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	@Override
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	public void updateStats() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final long start = System.nanoTime();

		// on est appelé dans un thread Quartz -> pas de transaction ouverte par défaut
		final Map<Integer, TacheStats> stats = template.execute(new TransactionCallback<Map<Integer, TacheStats>>() {
			@Override
			public Map<Integer, TacheStats> doInTransaction(TransactionStatus status) {
				return tacheDAO.getTacheStats();
			}
		});

		final long end = System.nanoTime();

		final boolean somethingChanged = (tacheStatsPerOid == null || !tacheStatsPerOid.equals(stats));

		if (LOGGER.isDebugEnabled() && somethingChanged) { // on évite de logger si rien n'a changé depuis le dernier appel
			final long ms = (end - start) / 1000000;

			StringBuilder s = new StringBuilder();
			s.append("Statistiques des tâches en instances par OID (récupérées en ").append(ms).append(" ms)");

			if (stats.isEmpty()) {
				s.append(" : aucune tâche trouvée");
			}
			else {
				s.append(" :\n");

				// trie la liste par OID
				List<Map.Entry<Integer, TacheStats>> list = new ArrayList<Map.Entry<Integer, TacheStats>>(stats.entrySet());
				Collections.sort(list, new Comparator<Map.Entry<Integer, TacheStats>>() {
					@Override
					public int compare(Map.Entry<Integer, TacheStats> o1, Map.Entry<Integer, TacheStats> o2) {
						return o1.getKey().compareTo(o2.getKey());
					}
				});

				for (Map.Entry<Integer, TacheStats> e : list) {
					final TacheStats ts = e.getValue();
					s.append("  - ").append(e.getKey()).append(" : tâches=").append(ts.tachesEnInstance).append(" dossiers=").append(ts.dossiersEnInstance).append('\n');
				}
			}
			LOGGER.debug(s.toString());
		}

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		tacheStatsPerOid = stats;
	}

	/**
	 * Genere une tache à partir de la fermeture d'un for principal
	 *
	 * @param contribuable le contribuable sur lequel un for principal a été fermé
	 * @param forPrincipal le for fiscal principal qui vient d'être fermé
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forPrincipal) {

		final RegDate dateFermeture = forPrincipal.getDateFin();
		final MotifFor motifFermeture = forPrincipal.getMotifFermeture();

		if (motifFermeture == null) { // les for HC et HS peuvent ne pas avoir de motif de fermeture
			return;
		}

		final ModeImposition modeImposition = forPrincipal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
			return;
		}

		final List<ForFiscal> forsFiscaux = contribuable.getForsFiscauxValidAt(dateFermeture);
		final boolean dernierForFerme = forsFiscaux.size() < 2;

		switch (motifFermeture) {
		case DEPART_HS:
			if (TypeAutoriteFiscale.PAYS_HS == forPrincipal.getTypeAutoriteFiscale()) {
				/*
				 * le for principal est déjà hors-Suisse ! Cela arrive sur certain contribuables (voir par exemple le ctb n°52108102) où le
				 * départ HS a été enregistré comme déménagement VD. A ce moment-là, si le for principal HS est fermé avec le motif départ
				 * HS, il faut ignorer cet événement qui ne correspond à rien puisque le contribuable est déjà HS.
				 */
				return;
			}
			if (dernierForFerme) {
				genereTacheControleDossier(contribuable);
			}
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case DEPART_HC:
			/*
			 * Si ce départ a lieu lors de la période courante, une tâche de contrôle du dossier est engendrée,
			 * pour que l’utilisateur puisse vérifier si les fors secondaires éventuels ont bien été enregistrés.
			 */
			if (dateFermeture.year() == RegDate.get().year()) {
				genereTacheControleDossier(contribuable);
			}
			// [UNIREG-1262] La génération de tâches d'annulation de DI doit se faire aussi sur l'année du départ
			// [UNIREG-2031] La génération de tâches d'annulation de DI n'est valable quepour un départ avant le 31.12 de la période fiscale courante.
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case VEUVAGE_DECES:
			final CollectiviteAdministrative collectiviteAssignee = tiersService.getOfficeImpotAt(contribuable, null);
			if (collectiviteAssignee != null) { // [UNIREG-3223] les sourciers purs ne possèdent pas de dossier, on peut donc les ignorer
				generateTacheTransmissionDossier(contribuable, collectiviteAssignee);
			}
			// [UNIREG-1112] Annule toutes les déclarations d'impôt à partir de l'année de décès (car elles n'ont pas lieu d'être)
			// [UNIREG-2104] Génère la tache d'envoi de DI assigné à l'ACI
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor

			break;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			if (!contribuable.getForsParTypeAt(dateFermeture, false).secondaires.isEmpty()) {
				// [UNIREG-1105] Une tâche de contrôle de dossier (pour répartir les fors secondaires) doit être ouverte sur le couple en cas de séparation
				genereTacheControleDossier(contribuable);
			}
			// [UNIREG-1112] Annule toutes les déclarations d'impôt à partir de l'année de séparation (car elles n'ont pas lieu d'être)
			// [UNIREG-1111] Génère une tâche d'émission de DI
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;
		}
	}

	private void generateTacheTransmissionDossier(Contribuable contribuable, CollectiviteAdministrative collectiviteAssignee) {
		Assert.notNull(collectiviteAssignee);
		final TacheTransmissionDossier tache = new TacheTransmissionDossier(TypeEtatTache.EN_INSTANCE, null, contribuable, collectiviteAssignee);
		tacheDAO.save(tache);
	}

	/**
	 * Génère une tâche à partir de la fermeture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été fermé.
	 * @param forSecondaire le for secondaire qui vient d'être fermé.
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forSecondaire) {

		final RegDate dateFermeture = forSecondaire.getDateFin();
		final ForsParTypeAt forsAt = contribuable.getForsParTypeAt(dateFermeture, false);

		if (forsAt.principal == null || TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forsAt.principal.getTypeAutoriteFiscale()) {
			return;
		}

		final ModeImposition modeImposition = forsAt.principal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
			return;
		}

		// S'il s'agit du dernier for secondaire existant, on génère une tâche de contrôle de dossier
		if (forsAt.secondaires.size() == 1) {
			Assert.isEqual(forSecondaire, forsAt.secondaires.get(0));
			genereTacheControleDossier(contribuable);
		}

		// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
	}

	/**
	 * Génère une tache de contrôle de dossier sur un contribuable, en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable le contribuable sur lequel un tâche de contrôle de dossier doit être générée.
	 */
	private void genereTacheControleDossier(Contribuable contribuable) {
		genereTacheControleDossier(contribuable, null);
	}


	/**
	 * Génère une tache de contrôle de dossier sur un contribuable et la lie à une collectivitéAdministrative en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable le contribuable sur lequel un tâche de contrôle de dossier doit être générée.
	 * @param collectivite la collectivité administrative assignée aux tâches nouvellement créées.
	 */
	private void genereTacheControleDossier(Contribuable contribuable, @Nullable CollectiviteAdministrative collectivite) {

		if (!tacheDAO.existsTacheEnInstanceOuEnCours(contribuable.getNumero(), TypeTache.TacheControleDossier)) {
			//UNIREG-1024 "la tâche de contrôle du dossier doit être engendrée pour l'ancien office d'impôt"
			if (collectivite == null) {
				collectivite = getOfficeImpot(contribuable);
			}
			final TacheControleDossier tache = new TacheControleDossier(TypeEtatTache.EN_INSTANCE, null, contribuable, collectivite);
			tacheDAO.save(tache);
		}
	}

	/**
	 * Genere une tache à partir de l'ouverture d'un for principal
	 *
	 * @param contribuable         le contribuable sur lequel un un for principal a été ouvert
	 * @param forFiscal            le for fiscal principal qui vient d'être ouvert
	 * @param ancienModeImposition nécessaire en cas d'ouverture de for pour motif "CHGT_MODE_IMPOSITION"
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition) {

		final MotifFor motifOuverture = forFiscal.getMotifOuverture();
		if (motifOuverture == null) { // les for principaux HC ou HS peuvent ne pas avoir de motif
			return; // rien à faire
		}

		final ModeImposition modeImposition = forFiscal.getModeImposition();
		if (modeImposition == ModeImposition.SOURCE) {
			// les sourciers ne recoivent pas de DIs (et donc pas de dossier non plus)
			return; // pas de tâche sur les sourciers purs [UNIREG-1888]
		}

		// [UNIREG-2378] Les fors principaux HC ou HS ne donnent pas lieu à des générations de tâches
		// d'ouverture de dossier ou d'envoi de DI
		if (forFiscal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			return;
		}

		switch (motifOuverture) {
		case ARRIVEE_HC:
			try {
				final List<Assujettissement> assujettissements = assujettissementService.determine(contribuable, forFiscal.getDateDebut().year());
				if (assujettissements != null) {
					final int size = assujettissements.size();
					if (size > 1) {
						final Assujettissement dernier = assujettissements.get(size - 1);
						final Assujettissement avantdernier = assujettissements.get(size - 2);
						if (dernier.getMotifFractDebut() == MotifFor.ARRIVEE_HC && avantdernier.getMotifFractFin() == MotifFor.DEPART_HS) {
							// si on est en présence d'une arrivée de hors-Canton précédée d'un départ hors-Suisse, on génère une tâche de contrôle de dossier
							genereTacheControleDossier(contribuable);
						}
					}
				}
			}
			catch (AssujettissementException e) {
				// on ignore joyeusement cette erreur, au pire il manquera une tâche de contrôle de dossier
				LOGGER.warn("Impossible de creer la tâche de contrôle de dossier: " + e.getMessage());
			}

		case MAJORITE:
		case PERMIS_C_SUISSE:
		case ARRIVEE_HS:
		case MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION:
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			// [SIFISC-3357] On ne génére plus de tache nouveau dossier pour les ctb VD
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case CHGT_MODE_IMPOSITION:
			// [SIFISC-3357] On ne génére plus de tache nouveau dossier pour les ctb VD
 			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case VEUVAGE_DECES:
			// [SIFISC-3357] On ne génére plus de tache nouveau dossier pour les ctb VD
			// [UNIREG-1112] il faut générer les tâches d'envoi de DIs sur le tiers survivant
			// [UNIREG-1265] Plus de création de tâche de génération de DI pour les décès
			// [UNIREG-1198] assignation de la tache au service succession mis en place
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case DEMENAGEMENT_VD:
			// si le demenagement arrive dans une periode fiscale échue, une tâche de contrôle
			// du dossier est engendrée pour l’ancien office d’impôt gérant
			// (déterminé par l’ancien for principal) s’il a changé.
			ForFiscalPrincipal ffp = contribuable.getForFiscalPrincipalAt(forFiscal.getDateDebut().getOneDayBefore());
			boolean changementOfficeImpot = false;
			if (ffp != null && !ffp.getNumeroOfsAutoriteFiscale().equals(forFiscal.getNumeroOfsAutoriteFiscale())) {
				changementOfficeImpot = true;
			}

			if (FiscalDateHelper.isEnPeriodeEchue(forFiscal.getDateDebut()) && changementOfficeImpot) {
				OfficeImpot office;
				office = serviceInfra.getOfficeImpotDeCommune(ffp.getNumeroOfsAutoriteFiscale());
				//UNIREG-1886 si l'office nest pas trouvé (cas hors canton, hors suisse) on ne génère pas de tâche
				if (office != null) {
					CollectiviteAdministrative collectivite = tiersService.getCollectiviteAdministrative(office.getNoColAdm());
					genereTacheControleDossier(contribuable, collectivite);
				}

			}
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable) {
		// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
	}

	private void generateTacheNouveauDossier(Contribuable contribuable) {
		final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(contribuable, null);
		Assert.notNull(oid);
		final TacheNouveauDossier tacheNouveauDossier = new TacheNouveauDossier(TypeEtatTache.EN_INSTANCE, null, contribuable, oid);
		tacheDAO.save(tacheNouveauDossier);
	}

	@Override
	public void synchronizeTachesDIs(final Collection<Long> ctbIds) {

		final Map<Long, List<SynchronizeAction>> entityActions = new HashMap<Long, List<SynchronizeAction>>();

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.execute(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
					@Override
					public Object doInHibernate(Session session) throws HibernateException, SQLException {

						// détermine tous les actions à effectuer sur les contribuables
						final Map<Long, List<SynchronizeAction>> actions = determineAllSynchronizeActionsForDIs(ctbIds);
						final Map<Long, List<SynchronizeAction>> tacheActions = new HashMap<Long, List<SynchronizeAction>>(actions.size());
						splitActions(actions, tacheActions, entityActions);

						// on exécute toutes les actions sur les tâches dans la transaction courante, car - sauf bug -
						// elles ne peuvent pas provoquer d'erreurs de validation.
						if (!tacheActions.isEmpty()) {
							executeTacheActions(tacheActions);
						}
						return null;
					}
				});
				return null;
			}
		});

		// finalement, on exécute toutes les actions sur les entités dans une ou plusieurs transactions additionnelles (SIFISC-3141)
		if (!entityActions.isEmpty()) {
			executeEntityActions(entityActions);
		}
	}

	/**
	 * Exécuter toutes les actions de type 'tache' spécifiées. Cette méthode ne gère <b>pas</b> elle-même les transactions et doit donc être appelée dans un context transactionnel.
	 *
	 * @param tacheActions la liste des actions de type 'tache' à effectuer
	 */
	private void executeTacheActions(Map<Long, List<SynchronizeAction>> tacheActions) {
		for (Map.Entry<Long, List<SynchronizeAction>> entry : tacheActions.entrySet()) {
			executeActions(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Exécute toutes les actions de type 'entity' spécifiées. Cette méthode gère elle-même les transactions, de manière à pouvoir reprendre le traitement en cas d'erreur de validation après modification
	 * des entités. Elle ne doit pas être appelée dans un context transactionnel.
	 *
	 * @param entityActions la liste des actions de type 'entité' à effectuer.
	 */
	private void executeEntityActions(Map<Long, List<SynchronizeAction>> entityActions) {

		// on exécute toutes les actions en lots de 100. Les actions sont groupées par numéro de contribuable, de telle manière que
		// toutes les actions d'un contribuable soient exécutées dans une même transaction.
		final BatchTransactionTemplate<Map.Entry<Long, List<SynchronizeAction>>, BatchResults> batchTemplate =
				new BatchTransactionTemplate<Map.Entry<Long, List<SynchronizeAction>>, BatchResults>(entityActions.entrySet(), 100, BatchTransactionTemplate.Behavior.REPRISE_AUTOMATIQUE,
						transactionManager, null, hibernateTemplate);
		batchTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		batchTemplate.execute(new BatchTransactionTemplate.BatchCallback<Map.Entry<Long, List<SynchronizeAction>>, BatchResults>() {
			@Override
			public boolean doInTransaction(List<Map.Entry<Long, List<SynchronizeAction>>> batch, BatchResults rapport) throws Exception {
				for (Map.Entry<Long, List<SynchronizeAction>> entry : batch) {
					executeActions(entry.getKey(), entry.getValue());
				}
				return false;
			}

			@Override
			public void afterTransactionRollback(Exception e, boolean willRetry) {
				if (!willRetry) {
					LOGGER.error(e, e);
				}
			}
		});
	}

	private void executeActions(Long ctbId, List<SynchronizeAction> actions) {

		final CollectiviteAdministrative officeSuccessions = tiersService.getOrCreateCollectiviteAdministrative(ServiceInfrastructureService.noACISuccessions, true);
		if (officeSuccessions == null) {
			throw new IllegalArgumentException("Impossible de trouver l'office des successions !");
		}

		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers instanceof Contribuable) {
			// On effectue toutes les actions nécessaires
			final Contribuable contribuable = (Contribuable) tiers;
			final CollectiviteAdministrative collectivite = getOfficeImpot(contribuable);

			final Context context = new Context(contribuable, collectivite, tacheDAO, diService, officeSuccessions, diDAO);

			for (SynchronizeAction action : actions) {
				action.execute(context);
			}
		}
	}

	private static void splitActions(Map<Long, List<SynchronizeAction>> actions, Map<Long, List<SynchronizeAction>> tacheActions, Map<Long, List<SynchronizeAction>> entityActions) {
		for (Map.Entry<Long, List<SynchronizeAction>> entry : actions.entrySet()) {
			final List<SynchronizeAction> values = entry.getValue();
			final List<SynchronizeAction> taches = new ArrayList<SynchronizeAction>(values.size());
			final List<SynchronizeAction> entites = new ArrayList<SynchronizeAction>(values.size());
			for (SynchronizeAction action : values) {
				if (action.willChangeEntity()) {
					entites.add(action);
				}
				else {
					taches.add(action);
				}
			}
			if (!taches.isEmpty()) {
				tacheActions.put(entry.getKey(), taches);
			}
			if (!entites.isEmpty()) {
				entityActions.put(entry.getKey(), entites);
			}
		}
	}

	private Map<Long, List<SynchronizeAction>> determineAllSynchronizeActionsForDIs(Collection<Long> ctbIds) {
		final Map<Long, List<SynchronizeAction>> map = new HashMap<Long, List<SynchronizeAction>>();
		for (Long id : ctbIds) {
			final Tiers tiers = tiersService.getTiers(id);
			if (tiers instanceof Contribuable) {
				List<SynchronizeAction> actions;
				try {
					actions = determineSynchronizeActionsForDIs((Contribuable) tiers);
				}
				catch (AssujettissementException e) {
					Audit.warn("Impossible de calculer les périodes d'imposition théoriques du contribuable n°" + id
							+ " lors de la mise-à-jour des tâches d'envoi et d'annulation des déclarations d'impôt:"
							+ " aucune action n'est effectuée.");
					LOGGER.warn(e, e);
					actions = null;
				}
				if (actions != null && !actions.isEmpty()) {
					map.put(id, actions);
				}
			}
		}
		return map;
	}

	@Override
	public List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) throws AssujettissementException {

		// On récupère les données brutes
		final List<PeriodeImposition> periodes = getPeriodesImpositionHisto(contribuable);
		final List<DeclarationImpotOrdinaire> declarations = getDeclarationsActives(contribuable);
		final List<TacheEnvoiDeclarationImpot> tachesEnvoi = getTachesEnvoiEnInstance(contribuable);
		final List<TacheAnnulationDeclarationImpot> tachesAnnulation = getTachesAnnulationEnInstance(contribuable);

		final List<AddDI> addActions = new ArrayList<AddDI>();
		final List<UpdateDI> updateActions = new ArrayList<UpdateDI>();
		final List<DeleteDI> deleteActions = new ArrayList<DeleteDI>();
		final List<AnnuleTache> annuleActions = new ArrayList<AnnuleTache>();

		final int anneeCourante = RegDate.get().year();

		//
		// On détermine les périodes d'imposition qui n'ont pas de déclaration d'impôt valide correspondante
		//

		for (PeriodeImposition periode : periodes) {
			final List<DeclarationImpotOrdinaire> dis = getIntersectingRangeAt(declarations, periode);
			if (dis == null) {
				// il n'y a pas de déclaration pour la période
				if (isDeclarationMandatory(periode)) {
					// on ajoute une DI si elle est obligatoire
					// [UNIREG-2735] Le mécanisme ne doit pas créer de tâche d'émission de DI pour l'année en cours
					if (peutCreerTacheEnvoiDI(periode, anneeCourante)) {
						addActions.add(new AddDI(periode));
					}
				}
			}
			else {
				Assert.isFalse(dis.isEmpty());
				DeclarationImpotOrdinaire toUpdate = null;
				PeriodeImposition toAdd = null;

				for (DeclarationImpotOrdinaire di : dis) {
					if (DateRangeHelper.equals(di, periode)) {
						// la durée de la déclaration et de la période d'imposition correspondent parfaitement
						if (di.getTypeContribuable() == periode.getTypeContribuable()) {
							// les types correspondent, rien à faire
						}
						else {
							// les types ne correspondent pas
							if (peutMettreAJourDeclarationExistante(di, periode, anneeCourante)) {
								// le type de contribuable peut être mis-à-jour
								if (toUpdate != null) {
									deleteActions.add(new DeleteDI(toUpdate));
								}
								toUpdate = di;
								toAdd = null;
							}
							else {
								// le type de contribuable ne peut pas être mis-à-jour : la déclaration doit être annulée
								deleteActions.add(new DeleteDI(di));
								if (toUpdate == null) {
									// on prévoit de recréer la déclaration
									toAdd = periode;
								}
							}
						}
					}
					else {
						// la durée de la déclaration et de la période d'imposition ne correspondent pas
						if (toUpdate != null) {
							// il y a déjà une déclaration compatible pouvant être mise-à-jour, inutile de chercher plus loin
							deleteActions.add(new DeleteDI(di));
						}
						else {
							if (peutMettreAJourDeclarationExistante(di, periode, anneeCourante)) {
								// si les types sont compatibles, on adapte la déclaration
								toUpdate = di;
								toAdd = null;
							}
							else if (!isDiLibreSurPeriodeCourante(di, anneeCourante)) {
								// si les types sont incompatibles, on annule et on prévoit de recréer la déclaration
								deleteActions.add(new DeleteDI(di));
								toAdd = periode;
							}
						}
					}
				}
				if (toUpdate != null) {
					updateActions.add(new UpdateDI(periode, toUpdate));
				}
				else if (toAdd != null) {
					// [UNIREG-2735] Le mécanisme ne doit pas créer de tâche d'émission de DI pour l'année en cours
					if (peutCreerTacheEnvoiDI(periode, anneeCourante)) {
						addActions.add(new AddDI(toAdd));
					}
				}
			}
		}

		// on retranche les actions d'ajout de DI pour lesquelles il existe déjà une tâche d'envoi de DI
		if (!addActions.isEmpty()) {
			for (int i = addActions.size() - 1; i >= 0; i--) {
				final PeriodeImposition periode = addActions.get(i).periodeImposition;
				final TacheEnvoiDeclarationImpot envoi = getMatchingRangeAt(tachesEnvoi, periode);
				if (envoi != null && envoi.getTypeContribuable() == periode.getTypeContribuable()) {
					addActions.remove(i);
				}
			}
		}

		//
		// On détermine toutes les déclarations qui ne sont pas valides vis-à-vis des périodes d'imposition
		//

		for (DeclarationImpotOrdinaire declaration : declarations) {
			final List<PeriodeImposition> ps = getIntersectingRangeAt(periodes, declaration);
			if (ps == null) {
				if (!isDeclarationToBeUpdated(updateActions, declaration)) { // [UNIREG-3028]
					// il n'y a pas de période correspondante
					deleteActions.add(new DeleteDI(declaration));
				}
			}
			else {
				Assert.isFalse(ps.isEmpty());
				// s'il y a une intersection entre la déclaration et une période d'imposition, le cas a déjà été traité à partir des périodes d'imposition -> rien d'autre à faire
			}
		}

		// on retranche les déclarations pour lesquelles il existe déjà une tâche d'annulation
		if (!deleteActions.isEmpty()) {
			for (int i = deleteActions.size() - 1; i >= 0; i--) {
				final Long diId = deleteActions.get(i).diId;
				for (TacheAnnulationDeclarationImpot annulation : tachesAnnulation) {
					if (annulation.getDeclarationImpotOrdinaire().getId().equals(diId)) {
						deleteActions.remove(i);
						break;      // pas la peine de l'enlever plusieurs fois...
					}
				}
			}
		}

		//
		//  On détermine la liste des tâches qui ne sont plus valides vis-à-vis des périodes d'imposition et des déclarations existantes
		//

		for (TacheEnvoiDeclarationImpot envoi : tachesEnvoi) {
			if (!isTacheEnvoiValide(envoi, periodes, declarations, updateActions)) {
				annuleActions.add(new AnnuleTache(envoi));
			}
		}

		for (TacheAnnulationDeclarationImpot annulation : tachesAnnulation) {
			if (!isTacheAnnulationValide(annulation, periodes, updateActions, anneeCourante)) {
				annuleActions.add(new AnnuleTache(annulation));
			}
		}

		final int size = addActions.size() + updateActions.size() + deleteActions.size() + annuleActions.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		else {
			final List<SynchronizeAction> actions = new ArrayList<SynchronizeAction>(size);
			actions.addAll(addActions);
			actions.addAll(updateActions);
			actions.addAll(deleteActions);
			actions.addAll(annuleActions);
			return actions;
		}
	}

	/**
	 * @param periode une période d'imposition
	 * @return <b>vrai</b> si une déclaration d'impôt doit obligatoirement être envoyée pour la période d'imposition spécifiée; <b>faux</b> dans tous les autres cas.
	 */
	private static boolean isDeclarationMandatory(PeriodeImposition periode) {
		return !periode.isOptionnelle() && !periode.isRemplaceeParNote() && !periode.isDiplomateSuisseSansImmeuble();
	}

	/**
	 * Détermine si la tâche d'envoi d'une déclaration d'impôt est (toujours) valide en se mettant dans la position où les actions prévues ont été effectuées.
	 *
	 * @param envoi         une tâche d'envoi
	 * @param periodes      les périodes d'imposition théorique du contribuable
	 * @param declarations  les déclarations existantes
	 * @param updateActions les actions prévues de mise-à-jour des déclarations
	 * @return <b>vrai</b> si la tâche est valide; <b>faux</b> si elle est invalide et doit être annulée.
	 */
	private static boolean isTacheEnvoiValide(TacheEnvoiDeclarationImpot envoi, List<PeriodeImposition> periodes, List<DeclarationImpotOrdinaire> declarations, List<UpdateDI> updateActions) {

		final PeriodeImposition periode = getMatchingRangeAt(periodes, envoi);
		if (periode == null || !isDeclarationMandatory(periode)) {
			// pas de période correspondante -> la tâche n'est plus valable
			// [SIFISC-1653] déclaration d'impôt pas obligatoire -> la tâche n'est plus valable
			return false;
		}

		if (envoi.getTypeContribuable() != periode.getTypeContribuable()) {
			// il y a une période correspondante, mais le type ne correspond pas -> la tâche n'est plus valable
			return false;
		}


		final DeclarationImpotOrdinaire declaration = getMatchingRangeAt(declarations, periode);
		if (declaration == null) {
			// il n'y a pas de déclaration, la tâche est donc valide
			return true;
		}

		if (isDeclarationToBeUpdated(updateActions, declaration)) { // [SIFISC-1288]
			// la déclaration existante va être mise-à-jour, la tâche est donc invalide
			return false;
		}

		if (envoi.getTypeContribuable() == declaration.getTypeContribuable()) {
			// le type de contribuable de la tâche d'envoi et de la déclaration correspondent, la tâche d'envoi est donc invalide
			return false;
		}

		// la tâche est valide
		return true;
	}

	/**
	 * Détermine si la tâche d'annulation d'une déclaration d'impôt est (toujours) valide en se mettant dans la position où les actions prévues ont été effectuées.
	 *
	 * @param annulation    une tâche d'annulation
	 * @param periodes      les périodes d'imposition théorique du contribuable
	 * @param updateActions les actions prévues de mise-à-jour des déclarations
	 * @param anneeCourante l'année courante
	 * @return <b>vrai</b> si la tâche est valide; <b>faux</b> si elle est invalide et doit être annulée.
	 */
	private static boolean isTacheAnnulationValide(TacheAnnulationDeclarationImpot annulation, List<PeriodeImposition> periodes, List<UpdateDI> updateActions, int anneeCourante) {

		final DeclarationImpotOrdinaire declaration = annulation.getDeclarationImpotOrdinaire();
		if (declaration.isAnnule()) {
			// la déclaration est déjà annulée
			return false;
		}

		if (isDeclarationToBeUpdated(updateActions, declaration)) { // [UNIREG-3028]
			// la déclaration va être mise-à-jour, la tâche d'annulation est donc invalide
			return false;
		}

		final PeriodeImposition periode = getMatchingRangeAt(periodes, declaration);
		if (periode == null) {
			// il n'y a pas de période d'imposition correspondante, la tâche d'annulation est donc valide
			return true;
		}

		//noinspection RedundantIfStatement
		if (periode.getTypeContribuable() == declaration.getTypeContribuable() || peutMettreAJourDeclarationExistante(declaration, periode, anneeCourante)) { // [UNIREG-3028]
			// le type de contribuable de la période et de la déclaration correspondent, la tâche d'annulation est donc invalide.
			return false;
		}

		return true;
	}

	/**
	 * @param updateActions la liste des actions de mise-à-jour
	 * @param declaration   une déclaration d'impôt
	 * @return <b>vrai</b> si la déclaration spécifiée est référencée dans la liste d'actions de mise-à-jour; <b>faux</b> si ce n'est pas le cas.
	 */
	private static boolean isDeclarationToBeUpdated(List<UpdateDI> updateActions, DeclarationImpotOrdinaire declaration) {
		boolean declarationUpdated = false;
		if (!updateActions.isEmpty()) {
			for (UpdateDI updateAction : updateActions) {
				if (updateAction.diId.equals(declaration.getId())) {
					declarationUpdated = true;
					break;
				}
			}
		}
		return declarationUpdated;
	}

	/**
	 * On peut mettre à jour une déclaration existante si elle est sur une période passée ou - sur la période courante - si la nouvelle fin de période n'est pas la fin de l'année (= déplacement de fin
	 * d'assujettissement). <b>Note:</b> le test sur le type de document n'est plus nécessaire (UNIREG-3281).
	 *
	 * @param diExistante   la DI potentiellement à mettre à jour
	 * @param periode       la période d'imposition avec laquelle la DI serait mise à jour
	 * @param anneeCourante année de la période dite "courante"
	 * @return <code>true</code> si la mise à jour est autorisée, <code>false</code> sinon.
	 */
	@SuppressWarnings({"UnusedParameters"})
	private static boolean peutMettreAJourDeclarationExistante(DeclarationImpotOrdinaire diExistante, PeriodeImposition periode, int anneeCourante) {
		return isPeriodePasseeOuCouranteIncomplete(periode, anneeCourante);
	}

	private static boolean isDiLibreSurPeriodeCourante(DeclarationImpotOrdinaire di, int anneeCourante) {
		return di.isLibre() && di.getPeriode().getAnnee() == anneeCourante;
	}

	/**
	 * On peut créer une tache d'envoi de DI pour toute période d'imposition dans une année passée.<br/>
	 * Sur la période courante, il faut que la période d'imposition se termine avant la fin de l'année (= fin d'assujettissement)
	 * @param periode période d'imposition pour laquelle on voudrait peut-être créer une tâche d'envoi de DI
	 * @param anneeCourante année de la période dite "courante"
	 * @return <code>true</code> si la création de la tâche est autorisée, <code>false</code> sinon
	 */
	private static boolean peutCreerTacheEnvoiDI(PeriodeImposition periode, int anneeCourante) {
		return isPeriodePasseeOuCouranteIncomplete(periode, anneeCourante);
	}

	/**
	 * Une période d'imposition est dite passée si elle fait référence à une année qui n'est pas l'année courante.<br/>
	 * Une période d'imposition est dite incomplète si elle se termine avant la fin de l'année civile
	 * @param periode période d'imposition à tester
	 * @param anneeCourante année considérée comme l'année courante
	 * @return <code>true</code> si la période est passée ou courante incomplète, <code>false</code> sinon (courante complète ou, pourquoi pas, future)
	 */
	private static boolean isPeriodePasseeOuCouranteIncomplete(PeriodeImposition periode, int anneeCourante) {
		final RegDate avantDernierJourAnnee = RegDate.get(anneeCourante, 12, 30);
		return periode.getDateDebut().year() < anneeCourante || RegDateHelper.isBeforeOrEqual(periode.getDateFin(), avantDernierJourAnnee, NullDateBehavior.LATEST);
	}

	/**
	 * Retourne l'office d'impôt courant du contribuable.
	 * <p>
	 * [UNIREG-3285] Si le contribuable ne possède logiquement pas d'office d'impôt assigné (cas du sourcier pur), on retourne l'OID du dernier for fiscal (principal ou secondaire) vaudois
	 * non-source annulé. Si finalement on a toujours rien, on retourne l'OID du dernier for fiscal vaudois indépendemment de son mode d'imposition ou de son type (principal, secondaire, autre...).
	 *
	 * @param contribuable un contribuable
	 * @return l'office d'impôt du contribuable.
	 */
	protected CollectiviteAdministrative getOfficeImpot(Contribuable contribuable) {
		CollectiviteAdministrative collectivite = tiersService.getOfficeImpotAt(contribuable, null);
		if (collectivite == null) {

			// [UNIREG-3285] On analyse les fors fiscaux du contribuable à la recherche d'un for qui puisse être utilisé pour déterminer un OID convenable
			ForFiscal dernierForFiscalVaudois = null;
			ForFiscal dernierForFiscalVaudoisNonSourceAnnule = null;

			final List<ForFiscal> fors = contribuable.getForsFiscauxSorted();
			if (fors != null) {
				for (int i = fors.size() - 1; i >= 0; --i) {
					final ForFiscal f = fors.get(i);
					if (f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						if (dernierForFiscalVaudois == null) {
							dernierForFiscalVaudois = f;
						}
						if (f.isAnnule()) {
							if (f instanceof ForFiscalSecondaire || (f.isPrincipal() && ((ForFiscalPrincipal) f).getModeImposition() != ModeImposition.SOURCE)) {
								dernierForFiscalVaudoisNonSourceAnnule = f;
								break;
							}
						}
					}
				}
			}

			final ForFiscal forConvenable = (dernierForFiscalVaudoisNonSourceAnnule != null ? dernierForFiscalVaudoisNonSourceAnnule : dernierForFiscalVaudois);
			if (forConvenable == null) {
				throw new IllegalArgumentException("Impossible de trouver un for fiscal convenable pour la détermination de l'OID du contribuable n°" + contribuable.getNumero());
			}

			final Integer oid = tiersService.getOfficeImpotId(forConvenable.getNumeroOfsAutoriteFiscale());
			if (oid == null) {
				throw new IllegalArgumentException("Impossible de déterminer l'OID pour la commune avec le numéro Ofs n°" + forConvenable.getNumeroOfsAutoriteFiscale());
			}
			collectivite = tiersService.getCollectiviteAdministrative(oid);
		}

		Assert.notNull(collectivite);
		return collectivite;
	}

	private List<PeriodeImposition> getPeriodesImpositionHisto(Contribuable contribuable) throws AssujettissementException {

		final RegDate dateCourante = RegDate.get();
		final int anneeCourante = dateCourante.year();
		final int anneeDebut = getPremierePeriodeFiscale();

		final List<PeriodeImposition> periodes = new ArrayList<PeriodeImposition>();
		for (int annee = anneeDebut; annee <= anneeCourante; ++annee) {
			final List<PeriodeImposition> list = periodeImpositionService.determine(contribuable, annee);
			if (list != null) {
				periodes.addAll(list);
			}
		}
		return periodes;
	}

	@SuppressWarnings({"unchecked"})
	private List<DeclarationImpotOrdinaire> getDeclarationsActives(Contribuable contribuable) {
		final Set<Declaration> declarations = contribuable.getDeclarations();
		if (declarations == null || declarations.isEmpty()) {
			return Collections.emptyList();
		}
		final List<DeclarationImpotOrdinaire> list = new ArrayList<DeclarationImpotOrdinaire>(declarations.size());
		for (Declaration d : declarations) {
			if (d.isAnnule()) {
				continue;
			}
			list.add((DeclarationImpotOrdinaire) d);
		}
		Collections.sort(list, new DateRangeComparator<DeclarationImpotOrdinaire>());
		return list;
	}

	private List<TacheAnnulationDeclarationImpot> getTachesAnnulationEnInstance(Contribuable contribuable) {
		final List<TacheAnnulationDeclarationImpot> tachesAnnulation;
		{
			final TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(contribuable);
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			criterion.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);

			final List<Tache> list = tacheDAO.find(criterion, true);
			if (list.isEmpty()) {
				tachesAnnulation = Collections.emptyList();
			}
			else {
				tachesAnnulation = new ArrayList<TacheAnnulationDeclarationImpot>(list.size());
				for (Tache t : list) {
					tachesAnnulation.add((TacheAnnulationDeclarationImpot) t);
				}
			}
		}
		return tachesAnnulation;
	}

	private List<TacheEnvoiDeclarationImpot> getTachesEnvoiEnInstance(Contribuable contribuable) {
		final List<TacheEnvoiDeclarationImpot> tachesEnvoi;
		{
			final TacheCriteria criterion = new TacheCriteria();
			criterion.setContribuable(contribuable);
			criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
			criterion.setTypeTache(TypeTache.TacheEnvoiDeclarationImpot);

			final List<Tache> list = tacheDAO.find(criterion, true);
			if (list.isEmpty()) {
				tachesEnvoi = Collections.emptyList();
			}
			else {
				tachesEnvoi = new ArrayList<TacheEnvoiDeclarationImpot>(list.size());
				for (Tache t : list) {
					tachesEnvoi.add((TacheEnvoiDeclarationImpot) t);
				}
			}
		}
		return tachesEnvoi;
	}

	private static <T extends DateRange> T getMatchingRangeAt(List<T> dis, DateRange range) {
		if (dis == null) {
			return null;
		}
		for (T t : dis) {
			if (DateRangeHelper.equals(t, range)) {
				return t;
			}
		}
		return null;
	}

	private <T extends DateRange> List<T> getIntersectingRangeAt(List<T> dis, DateRange range) {
		if (dis == null) {
			return null;
		}
		List<T> result = null;
		for (T t : dis) {
			if (DateRangeHelper.intersect(t, range)) {
				if (result == null) {
					result = new ArrayList<T>();
				}
				result.add(t);
			}
		}
		return result;
	}

	/**
	 * Genere une tache à partir de la overture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été ouvert
	 * @param forFiscal    le for fiscal secondaire qui vient d'être ouvert
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {

		ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(null);

		// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
		if (forPrincipal != null && forPrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && forPrincipal.getModeImposition() != ModeImposition.SOURCE) {
			final MotifRattachement motifRattachement = forFiscal.getMotifRattachement();
			if (motifRattachement == MotifRattachement.ACTIVITE_INDEPENDANTE || motifRattachement == MotifRattachement.IMMEUBLE_PRIVE) {
				generateTacheNouveauDossier(contribuable);
			}
		}

		// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getTachesEnInstanceCount(Integer oid) {
		final TacheStats stats = tacheStatsPerOid.get(oid);
		return stats == null ? 0: stats.tachesEnInstance;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getDossiersEnInstanceCount(Integer oid) {
		final TacheStats stats = tacheStatsPerOid.get(oid);
		return stats == null ? 0: stats.dossiersEnInstance;
	}

	private int getPremierePeriodeFiscale() {
		return parametres.getPremierePeriodeFiscale();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAnnulationContribuable(Contribuable contribuable) {

		TacheCriteria criteria = new TacheCriteria();
		criteria.setContribuable(contribuable);

		final List<Tache> taches = tacheDAO.find(criteria);
		for (Tache t : taches) {
			if (TypeEtatTache.TRAITE == t.getEtat()) {
				// inutile d'annuler les tâches traitées
				continue;
			}
			if (t instanceof TacheAnnulationDeclarationImpot) {
				// rien à faire, il reste nécessaire d'annuler les déclarations d'impôt
			}
			else {
				// dans tous les autres cas, on annule la tâche qui est devenue caduque
				t.setAnnule(true);
			}
		}
	}

	@Override
	public ListeTachesEnInstanceParOID produireListeTachesEnInstanceParOID(RegDate dateTraitement, StatusManager status) throws Exception {
		final ProduireListeTachesEnInstanceParOIDProcessor processor = new ProduireListeTachesEnInstanceParOIDProcessor(hibernateTemplate, serviceInfra, transactionManager);
		return processor.run(dateTraitement, status);
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfra = serviceInfrastructureService;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeImpositionService(PeriodeImpositionService periodeImpositionService) {
		this.periodeImpositionService = periodeImpositionService;
	}
}
