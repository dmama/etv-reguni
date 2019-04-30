package ch.vd.unireg.tache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.Behavior;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.QuestionnaireSNC;
import ch.vd.unireg.declaration.QuestionnaireSNCDAO;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.declaration.snc.QuestionnaireSNCService;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteService;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.metier.assujettissement.Assujettissement;
import ch.vd.unireg.metier.assujettissement.AssujettissementException;
import ch.vd.unireg.metier.assujettissement.AssujettissementService;
import ch.vd.unireg.metier.assujettissement.MotifAssujettissement;
import ch.vd.unireg.metier.assujettissement.PeriodeImposition;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesMorales;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionPersonnesPhysiques;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.parametrage.ParametreAppService;
import ch.vd.unireg.tache.sync.AddDI;
import ch.vd.unireg.tache.sync.AddDIPM;
import ch.vd.unireg.tache.sync.AddDIPP;
import ch.vd.unireg.tache.sync.AddQSNC;
import ch.vd.unireg.tache.sync.AnnuleTache;
import ch.vd.unireg.tache.sync.Context;
import ch.vd.unireg.tache.sync.DeleteDI;
import ch.vd.unireg.tache.sync.DeleteDIPM;
import ch.vd.unireg.tache.sync.DeleteDIPP;
import ch.vd.unireg.tache.sync.DeleteQSNC;
import ch.vd.unireg.tache.sync.SynchronizeAction;
import ch.vd.unireg.tache.sync.TacheSynchronizeAction;
import ch.vd.unireg.tache.sync.UpdateDI;
import ch.vd.unireg.tache.sync.UpdateDIPM;
import ch.vd.unireg.tache.sync.UpdateDIPP;
import ch.vd.unireg.tache.sync.UpdateQSNC;
import ch.vd.unireg.tache.sync.UpdateTacheEnvoiDI;
import ch.vd.unireg.tache.sync.UpdateTacheEnvoiDIPM;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesMorales;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.ForsParTypeAt;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheAnnulationQuestionnaireSNC;
import ch.vd.unireg.tiers.TacheControleDossier;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheDAO.TacheStats;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.TacheNouveauDossier;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.GroupeTypesDocumentBatchLocal;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

/**
 * Service permettant la génération de tâches à la suite d'événements fiscaux
 */
public class TacheServiceImpl implements TacheService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TacheServiceImpl.class);

	private TacheDAO tacheDAO;
	private DeclarationImpotOrdinaireDAO diDAO;
	private QuestionnaireSNCDAO qsncDAO;
	private PeriodeFiscaleDAO pfDAO;
	private DeclarationImpotService diService;
	private QuestionnaireSNCService qsncService;
	private ParametreAppService parametres;
	private HibernateTemplate hibernateTemplate;
	private ServiceInfrastructureService serviceInfra;
	private TiersService tiersService;
	private EtiquetteService etiquetteService;
	private PlatformTransactionManager transactionManager;
	private AssujettissementService assujettissementService;
	private PeriodeImpositionService periodeImpositionService;
	private AdresseService adresseService;
	private AuditManager audit;

	private Map<Integer, TacheStats> tacheStatsPerOid = new HashMap<>();
	private Map<TypeTache, List<String>> commentairesDistincts = new EnumMap<>(TypeTache.class);
	private Set<Integer> oidsAvecTaches = new HashSet<>();

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public void setQuestionnaireSNCDAO(QuestionnaireSNCDAO qsncDAO) {
		this.qsncDAO = qsncDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO pfDAO) {
		this.pfDAO = pfDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDiService(DeclarationImpotService diService) {
		this.diService = diService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setParametres(ParametreAppService parametres) {
		this.parametres = parametres;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setQsncService(QuestionnaireSNCService qsncService) {
		this.qsncService = qsncService;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	@Override
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	public void updateStats() {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		final long start = System.nanoTime();

		// on est appelé dans un thread Quartz -> pas de transaction ouverte par défaut
		final Map<Integer, TacheStats> stats = template.execute(status -> tacheDAO.getTacheStats());

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
				List<Map.Entry<Integer, TacheStats>> list = new ArrayList<>(stats.entrySet());
				list.sort(Comparator.comparing(Map.Entry::getKey));

				for (Map.Entry<Integer, TacheStats> e : list) {
					final TacheStats ts = e.getValue();
					s.append("  - ").append(e.getKey()).append(" : tâches=").append(ts.tachesEnInstance).append(" dossiers=").append(ts.dossiersEnInstance).append('\n');
				}
			}
			LOGGER.debug(s.toString());
		}

		// pas de besoin de synchronisation parce que l'assignement est atomique en java
		tacheStatsPerOid = stats;

		//
		// on va aussi aller chercher les valeurs des commentaires disponibles par type de tache
		//
		commentairesDistincts = template.execute(status -> tacheDAO.getCommentairesDistincts());

		//
		// et aussi la liste des collectivités administratives auxquelles des tâches (quel que soit leur état) sont attachées
		//
		oidsAvecTaches = template.execute(status -> tacheDAO.getCollectivitesAvecTaches());
	}

	@NotNull
	@Override
	public List<String> getCommentairesDistincts(TypeTache typeTache) {
		// on tape dans le cache, en le supposant rafraîchi de temps en temps
		return commentairesDistincts.getOrDefault(typeTache, Collections.emptyList());
	}

	@NotNull
	@Override
	public Set<Integer> getCollectivitesAdministrativesAvecTaches() {
		// on tape dans le cache, en le supposant rafraîchi de temps en temps
		return Collections.unmodifiableSet(oidsAvecTaches);
	}

	@Override
    public void annuleTachesObsoletes(final Collection<Long> ctbIds) {

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.execute(status -> hibernateTemplate.executeWithNewSession(session -> {
			ctbIds.stream()
					.map(tiersService::getTiers)
					.filter(Objects::nonNull)
					.forEach(tiers -> {
						if (tiers.isDesactive(null)) {
							// Si le contribuable est désactivé (ce qui inclus il fait qu'il puisse être annulé)
							// alors on annule toutes ses tâches non traitées (autre que annulation de DI).
							annuleTachesNonTraitees(tiers.getNumero(), TypeTache.TacheNouveauDossier, TypeTache.TacheControleDossier, TypeTache.TacheEnvoiDeclarationImpotPP, TypeTache.TacheTransmissionDossier);
						}
						else {
							// [SIFISC-2690] Annule les tâches d'ouverture de dossier pour les contribuables
							// qui n'ont plus de for de gestion actifs
							ForGestion forGestionActif = tiersService.getForGestionActif(tiers, null);
							if (forGestionActif == null) {
								annuleTachesNonTraitees(tiers.getNumero(), TypeTache.TacheNouveauDossier);
							}
						}
					});
			return null;
		}));
	}

    /**
     * Annule toute les tâches non traitées d'un type donné pour un contribuable
     *
     * @param contribuableId l'id du contribuable
     * @param types les types de tâches à annuler; ne pas renseigner pour annuler tous les types de tâches
     */
    private void annuleTachesNonTraitees(Long contribuableId, TypeTache... types) {
        final List<Tache> taches = tacheDAO.find(contribuableId);
        final boolean annuleTout = types.length == 0;
        List<TypeTache> listTypes = Arrays.asList(types);
        for (Tache tache : taches) {
            if (tache.getEtat() != TypeEtatTache.TRAITE && tache.getAnnulationDate() == null && (annuleTout || listTypes.contains(tache.getTypeTache()))) {
                tache.setAnnule(true);
            }
        }
    }

	/**
	 * [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
	 * @param ffp un for principal
	 * @return <code>true</code> si ce for implique une exclusion de la mécanique des tâches et des calculs d'OID (pour le moment, les fors principaux PP "Source" ont cet effet)
	 */
	private static boolean isExcludingFromTaskManagement(@NotNull ForFiscalPrincipal ffp) {
 		return ffp instanceof ForFiscalPrincipalPP && ((ForFiscalPrincipalPP) ffp).getModeImposition() == ModeImposition.SOURCE;
	}

    /**
	 * Genere une tache à partir de la fermeture d'un for principal
	 *
	 * @param contribuable le contribuable sur lequel un for principal a été fermé
	 * @param forPrincipal le for fiscal principal qui vient d'être fermé
	 */
	@Override
	public void genereTacheDepuisFermetureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forPrincipal) {

		final RegDate dateFermeture = forPrincipal.getDateFin();
		final MotifFor motifFermeture = forPrincipal.getMotifFermeture();

		if (motifFermeture == null) { // les for HC et HS peuvent ne pas avoir de motif de fermeture
			return;
		}

		if (isExcludingFromTaskManagement(forPrincipal)) {
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
				genereTacheControleDossier(contribuable, "Départ hors-Suisse");
			}
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case DEPART_HC:
			/*
			 * Si ce départ a lieu lors de la période courante, une tâche de contrôle du dossier est engendrée,
			 * pour que l’utilisateur puisse vérifier si les fors secondaires éventuels ont bien été enregistrés.
			 * [SIFISC-11939] cela doit être fait également en cas de départ dans la PF précédente
			 */
			if (dateFermeture.year() == RegDate.get().year() || dateFermeture.year() == RegDate.get().year() - 1) {
				genereTacheControleDossier(contribuable, "Départ hors-canton");
			}
			// [UNIREG-1262] La génération de tâches d'annulation de DI doit se faire aussi sur l'année du départ
			// [UNIREG-2031] La génération de tâches d'annulation de DI n'est valable quepour un départ avant le 31.12 de la période fiscale courante.
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			break;

		case VEUVAGE_DECES:
			// [UNIREG-1112] Annule toutes les déclarations d'impôt à partir de l'année de décès (car elles n'ont pas lieu d'être)
			// [UNIREG-2104] Génère la tache d'envoi de DI assigné à l'ACI
			// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
			// [SIFISC-14863] les tâches de transmission de dossier ne sont maintenant plus générées
			break;
		case SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT:
			if (!contribuable.getForsParTypeAt(dateFermeture, false).secondaires.isEmpty()) {
				// [UNIREG-1105] Une tâche de contrôle de dossier (pour répartir les fors secondaires) doit être ouverte sur le couple en cas de séparation
				genereTacheControleDossier(contribuable, "Clôture de ménage commun avec for(s) secondaire(s)");
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

	/**
	 * Génère une tâche à partir de la fermeture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été fermé.
	 * @param forSecondaire le for secondaire qui vient d'être fermé.
	 */
	@Override
	public void genereTacheDepuisFermetureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forSecondaire) {

		final RegDate dateFermeture = forSecondaire.getDateFin();
		final ForsParTypeAt forsAt = contribuable.getForsParTypeAt(dateFermeture, false);

		if (forsAt.principal == null || TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD == forsAt.principal.getTypeAutoriteFiscale()) {
			return;
		}

		if (isExcludingFromTaskManagement(forsAt.principal)) {
			return;
		}

		// S'il s'agit du dernier for secondaire existant, on génère une tâche de contrôle de dossier
		if (forsAt.secondaires.size() == 1) {
			if (!Objects.equals(forSecondaire, forsAt.secondaires.get(0))) {
				throw new IllegalArgumentException();
			}
			genereTacheControleDossier(contribuable, "Fermeture du dernier for secondaire");
		}

		// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
	}

	/**
	 * Génère une tache de contrôle de dossier sur un contribuable, en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable le contribuable sur lequel un tâche de contrôle de dossier doit être générée.
	 * @param commentaire commentaire (= motif) à associer à la tâche de contrôle de dossier
	 */
	public void genereTacheControleDossier(Contribuable contribuable, String commentaire) {
		genereTacheControleDossier(contribuable, null, commentaire);
	}


	/**
	 * Génère une tache de contrôle de dossier sur un contribuable et la lie à une collectivitéAdministrative en prenant bien soin de vérifier qu'il n'y en a pas déjà une non-traitée
	 *
	 * @param contribuable le contribuable sur lequel un tâche de contrôle de dossier doit être générée.
	 * @param collectivite la collectivité administrative assignée aux tâches nouvellement créées.
	 */
	private void genereTacheControleDossier(Contribuable contribuable, @Nullable CollectiviteAdministrative collectivite, String commentaire) {
		if (!tacheDAO.existsTacheControleDossierEnInstanceOuEnCours(contribuable.getNumero(), commentaire)) {
			//UNIREG-1024 "la tâche de contrôle du dossier doit être engendrée pour l'ancien office d'impôt"
			if (collectivite == null) {
				collectivite = getOfficeImpot(contribuable);
			}
			final TacheControleDossier tache = new TacheControleDossier(TypeEtatTache.EN_INSTANCE, null, contribuable, collectivite);
			tache.setCommentaire(commentaire);
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
	public void genereTacheDepuisOuvertureForPrincipal(Contribuable contribuable, ForFiscalPrincipal forFiscal, ModeImposition ancienModeImposition) {

		final MotifFor motifOuverture = forFiscal.getMotifOuverture();
		if (motifOuverture == null) { // les for principaux HC ou HS peuvent ne pas avoir de motif
			return; // rien à faire
		}

		if (isExcludingFromTaskManagement(forFiscal)) {
			return;
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
						if (dernier.getMotifFractDebut() == MotifAssujettissement.ARRIVEE_HC && avantdernier.getMotifFractFin() == MotifAssujettissement.DEPART_HS) {
							// si on est en présence d'une arrivée de hors-Canton précédée d'un départ hors-Suisse, on génère une tâche de contrôle de dossier
							genereTacheControleDossier(contribuable, "Arrivée hors-canton précédée d'un départ hors-Suisse");
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
			// [SIFISC-14441] on évitera une NPE si on s'assure que les numéros OFS que l'on compare sont bien des communes (on sait déjà que le nouveau for est vaudois, mais quid de l'ancien?)
			// [SIFISC-29770] Ne plus générer de tâche de contrôle de dossier pour les PP lors d'un déménagement sur une PF échue avec changement d'OID
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void genereTachesDepuisAnnulationDeFor(Contribuable contribuable) {
		// [UNIREG-2322] appelé de manière automatique par le TacheSynchronizerInterceptor
	}

	private void generateTacheNouveauDossier(Contribuable contribuable) {
		final CollectiviteAdministrative oid = tiersService.getOfficeImpotAt(contribuable, null);
		if (oid == null) {
			throw new IllegalArgumentException();
		}
		final TacheNouveauDossier tacheNouveauDossier = new TacheNouveauDossier(TypeEtatTache.EN_INSTANCE, null, contribuable, oid);
		tacheDAO.save(tacheNouveauDossier);
	}

	@Override
	public TacheSyncResults synchronizeTachesDeclarations(final Collection<Long> ctbIds) {

		final TacheSyncResults results = new TacheSyncResults(false);
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		final Map<Long, List<SynchronizeAction>> entityActions = template.execute(status -> hibernateTemplate.executeWithNewSession(session -> {
			// détermine toutes les actions à effectuer sur les contribuables
			final Map<Long, List<SynchronizeAction>> actions = determineAllSynchronizeActions(ctbIds);
			final Map<Long, List<SynchronizeAction>> tacheActions = new HashMap<>(actions.size());
			final Map<Long, List<SynchronizeAction>> resultingActions = new HashMap<>(actions.size());
			splitActions(actions, tacheActions, resultingActions);

			// on exécute toutes les actions sur les tâches dans la transaction courante, car - sauf bug -
			// elles ne peuvent pas provoquer d'erreurs de validation.
			if (!tacheActions.isEmpty()) {
				results.addAll(executeTacheActions(tacheActions));
			}
			return resultingActions;
		}));

		// finalement, on exécute toutes les actions sur les entités dans une ou plusieurs transactions additionnelles (SIFISC-3141)
		if (!entityActions.isEmpty()) {
			results.addAll(executeEntityActions(entityActions));
		}
		return results;
	}

	/**
	 * Exécuter toutes les actions de type 'tache' spécifiées. Cette méthode ne gère <b>pas</b> elle-même les transactions et doit donc être appelée dans un context transactionnel.
	 *
	 * @param tacheActions la liste des actions de type 'tache' à effectuer
	 */
	private TacheSyncResults executeTacheActions(Map<Long, List<SynchronizeAction>> tacheActions) {
		final TacheSyncResults results = new TacheSyncResults(false);
		for (Map.Entry<Long, List<SynchronizeAction>> entry : tacheActions.entrySet()) {
			executeActions(entry.getKey(), entry.getValue(), results);
		}
		return results;
	}

	/**
	 * Exécute toutes les actions de type 'entity' spécifiées. Cette méthode gère elle-même les transactions, de manière à pouvoir reprendre le traitement en cas d'erreur de validation après modification
	 * des entités. Elle ne doit pas être appelée dans un context transactionnel.
	 *
	 * @param entityActions la liste des actions de type 'entité' à effectuer.
	 */
	private TacheSyncResults executeEntityActions(final Map<Long, List<SynchronizeAction>> entityActions) {

		// on exécute toutes les actions en lots de 100. Les actions sont groupées par numéro de contribuable, de telle manière que
		// toutes les actions d'un contribuable soient exécutées dans une même transaction.
		final TacheSyncResults results = new TacheSyncResults(false);

		final BatchTransactionTemplateWithResults<Long, TacheSyncResults> batchTemplate =
				new BatchTransactionTemplateWithResults<>(entityActions.keySet(), 100, Behavior.REPRISE_AUTOMATIQUE, transactionManager, null);
		batchTemplate.execute(results, new BatchWithResultsCallback<Long, TacheSyncResults>() {
			@Override
			public boolean doInTransaction(List<Long> batch, TacheSyncResults rapport) throws Exception {
				for (Long id : batch) {
					executeActions(id, entityActions.get(id), rapport);
				}
				return true;
			}

			@Override
			public TacheSyncResults createSubRapport() {
				return new TacheSyncResults(false);
			}
		}, null);

		return results;
	}

	private void executeActions(Long ctbId, List<SynchronizeAction> actions, TacheSyncResults results) {

		final Tiers tiers = tiersService.getTiers(ctbId);
		if (tiers instanceof Contribuable) {

			// la collectivité à utiliser après un décès est extraite de la première (en fait, il ne devrait y en avoir qu'une seule)
			// étiquette non-annulée qui indique une action en cas de décès et associée à une collectivité administrative, justement...
			// (si on n'en trouve pas, c'est bizarre, car on dirait bien qu'il manque quelque chose en base, mais ce n'est réellement
			// un souci que dans le cas d'un contribuable assimilé "personne physique")
			final CollectiviteAdministrative officeApresDeces = etiquetteService.getAllEtiquettes(true).stream()
					.filter(e -> !e.isAnnule() && e.isActive() && e.getActionSurDeces() != null)
					.map(Etiquette::getCollectiviteAdministrative)
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(null);
			if (officeApresDeces == null && tiers instanceof ContribuableImpositionPersonnesPhysiques) {
				throw new IllegalArgumentException("Impossible de trouver la collectivité administrative à utiliser après un décès !");
			}

			// On effectue toutes les actions nécessaires
			final Contribuable contribuable = (Contribuable) tiers;
			final CollectiviteAdministrative collectivite = getOfficeImpot(contribuable);

			final Context context = new Context(contribuable, collectivite, tacheDAO, diService, officeApresDeces, tiersService, diDAO, qsncDAO, pfDAO, parametres);
			for (SynchronizeAction action : actions) {
				action.execute(context);
				results.addAction(ctbId, action);
			}
		}
	}

	private static void splitActions(Map<Long, List<SynchronizeAction>> actions, Map<Long, List<SynchronizeAction>> tacheActions, Map<Long, List<SynchronizeAction>> entityActions) {
		for (Map.Entry<Long, List<SynchronizeAction>> entry : actions.entrySet()) {
			final SplittedActions splittedActions = SplittedActions.splitFrom(entry.getValue());
			if (!splittedActions.taskActions.isEmpty()) {
				tacheActions.put(entry.getKey(), splittedActions.taskActions);
			}
			if (!splittedActions.entityActions.isEmpty()) {
				entityActions.put(entry.getKey(), splittedActions.entityActions);
			}
		}
	}

	private static final class SplittedActions {
		public final List<SynchronizeAction> taskActions;
		public final List<SynchronizeAction> entityActions;

		private SplittedActions(List<SynchronizeAction> taskActions, List<SynchronizeAction> entityActions) {
			this.taskActions = taskActions;
			this.entityActions = entityActions;
		}

		public static SplittedActions splitFrom(List<SynchronizeAction> actions) {
			final List<SynchronizeAction> taches = new ArrayList<>(actions.size());
			final List<SynchronizeAction> entites = new ArrayList<>(actions.size());
			for (SynchronizeAction action : actions) {
				if (action.willChangeEntity()) {
					entites.add(action);
				}
				else {
					taches.add(action);
				}
			}
			return new SplittedActions(taches, entites);
		}
	}

	private Map<Long, List<SynchronizeAction>> determineAllSynchronizeActions(Collection<Long> ctbIds) {
		final Map<Long, List<SynchronizeAction>> map = new HashMap<>(ctbIds.size());
		for (Long id : ctbIds) {
			final Tiers tiers = tiersService.getTiers(id);
			final List<SynchronizeAction> actions = new LinkedList<>();

			// d'abord les tâches autour des déclarations d'impôt
			if (tiers instanceof Contribuable) {
				try {
					actions.addAll(determineSynchronizeActionsForDIs((Contribuable) tiers));
				}
				catch (AssujettissementException e) {
					audit.warn("Impossible de calculer les périodes d'imposition théoriques du contribuable n°" + id
							           + " lors de la mise-à-jour des tâches d'envoi et d'annulation des déclarations d'impôt:"
							           + " aucune action n'est effectuée.");
					LOGGER.warn(e.getMessage(), e);
				}
			}

			// ... puis éventuellement les tâches autour des questionnaires SNC
			if (tiers instanceof Entreprise) {
				actions.addAll(determineSynchronizeActionsForQSNCs((Entreprise) tiers));
			}

			// si quelque action a été collectée, on la garde pour plus tard
			if (!actions.isEmpty()) {
				map.put(id, actions);
			}
		}
		return map;
	}

	@NotNull
	private List<SynchronizeAction> determineSynchronizeActionsForQSNCs(Entreprise entreprise) {

		// le critère de présence d'un questionnaire SNC est l'existence d'un for vaudois ouvert sur une année civile
		final List<DateRange> periodesSNC = getPeriodesQuestionnaireSNC(entreprise);
		final List<QuestionnaireSNC> questionnairesExistants = filterQuestionnairesSNC(getQuestionnairesSNCExistants(entreprise));
		final List<TacheEnvoiQuestionnaireSNC> tachesEnvoi = getTachesEnvoiQuestionnairesSNCEnInstance(entreprise);
		final List<TacheAnnulationQuestionnaireSNC> tachesAnnulation = getTachesAnnulationQuestionnairesSNCEnInstance(entreprise);

		// maintenant il faut comparer les deux

		// si tout est vide... facile, il n'y a rien à faire
		if (periodesSNC.isEmpty() && questionnairesExistants.isEmpty() && tachesEnvoi.isEmpty() && tachesAnnulation.isEmpty()) {
			return Collections.emptyList();
		}

		// ok, l'une au moins des collections n'est pas vide... ça devient intéressant.

		final List<AddQSNC> adds = new ArrayList<>(periodesSNC.size());
		final List<DeleteQSNC> deletes = new ArrayList<>(questionnairesExistants.size());
		final List<UpdateQSNC> updates = new ArrayList<>(questionnairesExistants.size());
		final List<AnnuleTache> cancels = new ArrayList<>();

		final RegDate today = RegDate.get();

		// on va boucler sur les périodes théoriques pour comparer à l'existant
		for (DateRange periode : periodesSNC) {
			final List<QuestionnaireSNC> questionnaires = getIntersectingRangeAt(questionnairesExistants, periode);
			if (questionnaires == null) {
				// il n'y a pas de questionnaire pour la période

				// Le mécanisme ne doit pas créer de tâche d'émission de questionnaire pour l'année en cours (sauf si la
				// période théorique est interrompue avant la fin de l'année)
				final int anneePeriode = periode.getDateDebut().year();
				if (anneePeriode < today.year() || (anneePeriode == today.year() && DayMonth.get(periode.getDateFin()) != DayMonth.get(12, 31))) {
					adds.add(new AddQSNC(periode));
				}
			}
			else {
				if (questionnaires.isEmpty()) {
					throw new IllegalArgumentException();
				}
				QuestionnaireSNC toUpdate = null;

				for (QuestionnaireSNC q : questionnaires) {
					if (!DateRangeHelper.equals(q, periode)) {
						// la durée du questionnaire et de la période d'imposition ne correspondent pas
						if (toUpdate != null) {
							// il y a déjà un questionnaire compatible pouvant être mis-à-jour, inutile de chercher plus loin
							deletes.add(new DeleteQSNC(q));
						}
						else {
							toUpdate = q;
						}
					}
				}
				if (toUpdate != null) {
					updates.add(new UpdateQSNC(toUpdate, periode));
				}
			}
		}

		//
		// on retranche les actions d'ajout pour lesquelles il existe déjà une tâche d'envoi
		//

		if (!adds.isEmpty()) {
			for (int i = adds.size() - 1; i >= 0; i--) {
				final DateRange periode = adds.get(i).range;
				final TacheEnvoiQuestionnaireSNC envoi = getMatchingRangeAt(tachesEnvoi, periode);
				if (envoi != null) {
					adds.remove(i);
				}
			}
		}

		//
		// On détermine tous les questionnaires qui ne sont pas valides vis-à-vis des ranges théoriques
		//

		for (QuestionnaireSNC questionnaire : questionnairesExistants) {
			final List<DateRange> ps = getIntersectingRangeAt(periodesSNC, questionnaire);
			if (ps == null) {
				if (!isQuestionnaireToBeUpdated(updates, questionnaire)) { // [UNIREG-3028]
					// il n'y a pas de période correspondante
					deletes.add(new DeleteQSNC(questionnaire));
				}
			}
			else {
				if (ps.isEmpty()) {
					throw new IllegalArgumentException();
				}
				// s'il y a une intersection entre la déclaration et une période d'imposition, le cas a déjà été traité à partir des périodes d'imposition -> rien d'autre à faire
			}
		}

		// on retranche les demandes d'annulation pour lesquelles la tâche d'annulation existe déjà
		if (!deletes.isEmpty()) {
			for (int i = deletes.size() - 1; i >= 0; i--) {
				final Long questionnaireId = deletes.get(i).questionnaireId;
				for (TacheAnnulationQuestionnaireSNC annulation : tachesAnnulation) {
					if (annulation.getDeclaration().getId().equals(questionnaireId)) {
						deletes.remove(i);
						break;      // pas la peine de l'enlever plusieurs fois...
					}
				}
			}
		}

		//
		//  On détermine la liste des tâches qui ne sont plus valides vis-à-vis des périodes d'imposition et des déclarations existantes
		//

		for (TacheEnvoiQuestionnaireSNC envoi : tachesEnvoi) {
			if (!isTacheEnvoiQuestionnaireSNCValide(envoi, periodesSNC, questionnairesExistants, updates)) {
				cancels.add(new AnnuleTache(envoi));
			}
		}

		for (TacheAnnulationQuestionnaireSNC annulation : tachesAnnulation) {
			if (!isTacheAnnulationQuestionnaireSNCValide(annulation, periodesSNC, updates)) {
				cancels.add(new AnnuleTache(annulation));
			}
		}

		final int size = adds.size() + updates.size() + deletes.size() + cancels.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		else {
			final List<SynchronizeAction> actions = new ArrayList<>(size);
			actions.addAll(adds);
			actions.addAll(updates);
			actions.addAll(deletes);
			actions.addAll(cancels);
			return actions;
		}
	}

	private List<QuestionnaireSNC> filterQuestionnairesSNC(List<QuestionnaireSNC> all) {
		final int premierePeriode = parametres.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		final List<QuestionnaireSNC> filtree = new ArrayList<>(all.size());
		for (QuestionnaireSNC q : all) {
			if (q.getPeriode().getAnnee() >= premierePeriode) {
				filtree.add(q);
			}
		}
		return filtree;
	}

	/**
	 * @param entreprise une entreprise
	 * @return la liste des questionnaires SNC existants, non-annulés, triés
	 */
	private static List<QuestionnaireSNC> getQuestionnairesSNCExistants(Entreprise entreprise) {
		return entreprise.getDeclarationsTriees(QuestionnaireSNC.class, false);
	}

	/**
	 * @param entreprise une entreprise
	 * @return la liste des périodes temporelles pour lesquelles Unireg devrait posséder un questionnaire SNC
	 */
	@NotNull
	private List<DateRange> getPeriodesQuestionnaireSNC(Entreprise entreprise) {
		final Set<Integer> periodesTheoriques = qsncService.getPeriodesFiscalesTheoriquementCouvertes(entreprise, true);
		final List<DateRange> ranges = new ArrayList<>(periodesTheoriques.size());
		for (int pf : periodesTheoriques) {
			ranges.add(new DateRangeHelper.Range(RegDate.get(pf, 1, 1), RegDate.get(pf, 12, 31)));
		}
		return ranges;
	}

	/**
	 * Les différentes mutations possibles détectées lors de la comparaison entre une DI existante
	 * et une période d'imposition calculée
	 */
	private enum Mutation {
		/**
		 * Aucune différence constatée, rien à faire, tout est en ordre
		 */
		AUCUNE,

		/**
		 * Différence compatible constatée -> il suffit de mettre à jour la DI
		 */
		COMPATIBLE,

		/**
		 * Différence trop importante constatée -> il faut impérativement annuler la DI existante et la remplacer
		 */
		MAJEURE
	}

	/**
	 * Le domaine de contribuable :
	 * <br/>
	 * <ul>
	 *     <li>imposition des personnes physiques&nbsp;</li>
	 *     <li>imposition des personnes morales.</li>
	 * </ul>
	 */
	private enum DomaineContribuable {

		PERSONNES_PHYSIQUES {
			@Override
			public AddDIPP newAddDI(PeriodeImposition pi) {
				return new AddDIPP((PeriodeImpositionPersonnesPhysiques) pi);
			}

			@Override
			public DeleteDIPP newDeleteDI(DeclarationImpotOrdinaire di) {
				return new DeleteDIPP((DeclarationImpotOrdinairePP) di);
			}

			@Override
			public UpdateDI<?, ?> newUpdateDI(PeriodeImposition pi, DeclarationImpotOrdinaire di) {
				return new UpdateDIPP((PeriodeImpositionPersonnesPhysiques) pi, (DeclarationImpotOrdinairePP) di);
			}

			@Override
			public UpdateTacheEnvoiDI newUpdateTacheEnvoi(TacheEnvoiDeclarationImpot tache, AddDI<?> addAction) {
				return null;
			}

			@Override
			public Mutation compare(DeclarationImpotOrdinaire di, PeriodeImposition pi, RegDate dateReference) {
				if (di.getTypeContribuable() == pi.getTypeContribuable() && DateRangeHelper.equals(di, pi)) {
					return Mutation.AUCUNE;
				}
				else if (peutMettreAJourDeclarationExistante(di, pi, dateReference)) {
					return Mutation.COMPATIBLE;
				}
				else {
					return Mutation.MAJEURE;
				}
			}

			@Override
			public List<PeriodeImposition> filtrerPeriodes(List<PeriodeImposition> periodes, ParametreAppService paramService) {
				// il n'y a pas, pour les personnes physiques, de différence entre les périodes calculées et celles
				// sur lesquelles l'algorithme de calcul automatique est applicable
				return periodes;
			}

			@Override
			public List<DeclarationImpotOrdinaire> filtrerDeclarations(List<DeclarationImpotOrdinaire> declarations, ParametreAppService paramService) {
				// il n'y a pas, pour les personnes physiques, de différence entre les périodes calculées et celles
				// sur lesquelles l'algorithme de calcul automatique est applicable
				return declarations;
			}

			@Override
			public <T extends SynchronizeAction> List<T> filtrerActions(List<T> actions, ParametreAppService paramService) {
				// il n'y a pas, pour les personnes physiques, de différence entre les périodes calculées et celles
				// sur lesquelles l'algorithme de calcul automatique est applicable
				return actions;
			}
		},

		PERSONNES_MORALES {
			@Override
			public AddDIPM newAddDI(PeriodeImposition pi) {
				return new AddDIPM((PeriodeImpositionPersonnesMorales) pi);
			}

			@Override
			public DeleteDIPM newDeleteDI(DeclarationImpotOrdinaire di) {
				return new DeleteDIPM((DeclarationImpotOrdinairePM) di);
			}

			@Override
			public UpdateDI<?, ?> newUpdateDI(PeriodeImposition pi, DeclarationImpotOrdinaire di) {
				return new UpdateDIPM((PeriodeImpositionPersonnesMorales) pi, (DeclarationImpotOrdinairePM) di);
			}

			@Override
			public UpdateTacheEnvoiDI newUpdateTacheEnvoi(TacheEnvoiDeclarationImpot tache, AddDI<?> addAction) {
				return UpdateTacheEnvoiDIPM.createIfNecessary((TacheEnvoiDeclarationImpotPM) tache, (AddDIPM) addAction);
			}

			@Override
			public Mutation compare(DeclarationImpotOrdinaire di, PeriodeImposition pi, RegDate dateReference) {
				// [SIFISC-20682] on ne touche pas au type de contribuable des DI sur lesquelles il est vide (en fait, si c'est la seule différence, on l'oublie, tout simplement)
				if (DateRangeHelper.equals(di, pi) && (di.getTypeContribuable() == null || di.getTypeContribuable() == pi.getTypeContribuable())) {
					// [SIFISC-19894] sur une DI migrée de SIMPA, il n'y a pas de type de déclaration... la comparaison ne fait alors pas de sens
					// [SIFISC-19894] sauf qu'en fait, le type de document a été rattrapé par script (afin de pouvoir faire des duplicata) et il faut comparer les types par groupe
					final boolean areTypeDocumentEquivalent = areEquivalent(di.getTypeDeclaration(), pi.getTypeDocumentDeclaration());
					if (DateRangeHelper.equals(((DeclarationImpotOrdinairePM) di).getExerciceCommercial(), ((PeriodeImpositionPersonnesMorales) pi).getExerciceCommercial()) && areTypeDocumentEquivalent) {
						return Mutation.AUCUNE;
					}
					else if (!areTypeDocumentEquivalent) {
						return Mutation.MAJEURE;
					}
					else {
						return Mutation.COMPATIBLE;
					}
				}
				else if (peutMettreAJourDeclarationExistante(di, pi, dateReference)) {
					return Mutation.COMPATIBLE;
				}
				else {
					return Mutation.MAJEURE;
				}
			}

			@Override
			public List<PeriodeImposition> filtrerPeriodes(List<PeriodeImposition> periodes, ParametreAppService paramService) {
				final List<PeriodeImposition> apresFiltre = new ArrayList<>(periodes.size());
				final int premierePeriode = paramService.getPremierePeriodeFiscalePersonnesMorales();
				for (PeriodeImposition pi : periodes) {
					if (pi.getPeriodeFiscale() >= premierePeriode) {
						apresFiltre.add(pi);
					}
				}
				return apresFiltre;
			}

			@Override
			public List<DeclarationImpotOrdinaire> filtrerDeclarations(List<DeclarationImpotOrdinaire> declarations, ParametreAppService paramService) {
				final List<DeclarationImpotOrdinaire> apresFiltre = new ArrayList<>(declarations.size());
				final int premierePeriode = paramService.getPremierePeriodeFiscalePersonnesMorales();
				for (DeclarationImpotOrdinaire di : declarations) {
					if (di.getPeriode().getAnnee() >= premierePeriode) {
						apresFiltre.add(di);
					}
				}
				return apresFiltre;
			}

			/**
			 * Les actions sur les tâches doivent n'avoir lieu qu'à partir de la période fiscale où on commence à envoyer les DI
			 * @param actions une liste d'actions brutes
			 * @param paramService le services des paramètres applicatifs
			 * @param <T> le type d'action
			 * @return une nouvelle liste filtrée
			 */
			@Override
			public <T extends SynchronizeAction> List<T> filtrerActions(List<T> actions, ParametreAppService paramService) {
				final List<T> apresFiltre = new ArrayList<>(actions.size());
				final int premierePeriode = paramService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
				for (T action : actions) {
					if (action.willChangeEntity() || !(action instanceof TacheSynchronizeAction) || ((TacheSynchronizeAction) action).getPeriodeFiscale() >= premierePeriode)  {
						apresFiltre.add(action);
					}
				}
				return apresFiltre;
			}
		};

		@Nullable
		static DomaineContribuable of(Contribuable contribuable) {
			if (contribuable instanceof ContribuableImpositionPersonnesPhysiques) {
				return PERSONNES_PHYSIQUES;
			}
			if (contribuable instanceof ContribuableImpositionPersonnesMorales) {
				return PERSONNES_MORALES;
			}

			// non traité par la mécanique de recalcul des tâches
			return null;
		}

		/**
		 * Création d'une nouvelle action de création de tâche d'envoi de DI d'après la période d'imposition donnée
		 * @param pi la période d'imposition
		 * @return une nouvelle action de création de tâche d'envoi de DI
		 * @throws ClassCastException si la période d'imposition n'est pas du type attendu par la catégorie
		 */
		public abstract AddDI<?> newAddDI(PeriodeImposition pi);

		/**
		 * Création d'une nouvelle action de création de tâche d'annulation de la DI donnée
		 * @param di la déclaration à annuler
		 * @return une nouvelle action de création de tâche d'annulation de DI
		 * @throws ClassCastException si la déclaration d'impôt n'est pas du type attendu par la catégorie
		 */
		public abstract DeleteDI<?> newDeleteDI(DeclarationImpotOrdinaire di);

		/**
		 * Création d'une nouvelle action de mise à jour d'une déclaration d'impôt à partir
		 * des données contenue dans la période d'imposition
		 * @param pi la période d'imposition
		 * @param di la déclaration d'impôt
		 * @return une nouvelle action de modification de DI
		 */
		public abstract UpdateDI<?, ?> newUpdateDI(PeriodeImposition pi, DeclarationImpotOrdinaire di);

		/**
		 * Création d'une nouvelle action de mise à jour de tâche d'émission de déclaration d'impôt
		 * @param tache tâche à mettre à jour si nécessaire
		 * @param addAction action qui contient les données utiles pour la mise à jour
		 * @return une nouvelle action de mise à jour de tâche d'émission de DI
		 */
		public abstract UpdateTacheEnvoiDI newUpdateTacheEnvoi(TacheEnvoiDeclarationImpot tache, AddDI<?> addAction);

		/**
		 * Détection du type d'adéquation entre une DI (= existante) et une PI (= théorique) sans prendre en compte les dates.
		 * @param di la déclaration d'impôt existante
		 * @param pi la période d'imposition calculée
		 * @param dateReference date de référence pour la comparaison
		 * @return le type de différence constatée entre l'existant et le théorique
		 */
		public abstract Mutation compare(DeclarationImpotOrdinaire di, PeriodeImposition pi, RegDate dateReference);

		/**
		 * @param periodes une liste des périodes d'impositions brutes
		 * @param paramService le service des paramètres applicatifs
		 * @return une liste des périodes d'impositions utiles pour le calcul des tâches d'envoi/annulation de DI
		 */
		public abstract List<PeriodeImposition> filtrerPeriodes(List<PeriodeImposition> periodes, ParametreAppService paramService);

		/**
		 * @param declarations une liste des déclarations brutes
		 * @param paramService le service des paramètres applicatifs
		 * @return une liste des déclarations utiles pour le calcul des tâches d'envoi/annulation de DI
		 */
		public abstract List<DeclarationImpotOrdinaire> filtrerDeclarations(List<DeclarationImpotOrdinaire> declarations, ParametreAppService paramService);

		/**
		 * @param actions une liste d'actions brutes
		 * @param paramService le services des paramètres applicatifs
		 * @return une liste d'actions à appliquer effectivement
		 */
		public abstract <T extends SynchronizeAction> List<T> filtrerActions(List<T> actions, ParametreAppService paramService);
	}

	/**
	 * @param type1 un type de document
	 * @param type2 un autre type de document
	 * @return <code>true</code> si les deux types sont identiques ou dans le même {@link ch.vd.unireg.type.GroupeTypesDocumentBatchLocal}
	 */
	private static boolean areEquivalent(TypeDocument type1, TypeDocument type2) {
		if (type1 == type2) {
			return true;
		}
		final GroupeTypesDocumentBatchLocal groupe1 = GroupeTypesDocumentBatchLocal.of(type1);
		final GroupeTypesDocumentBatchLocal groupe2 = GroupeTypesDocumentBatchLocal.of(type2);
		return groupe1 != null && groupe2 != null && groupe1 == groupe2;
	}

	@NotNull
	@Override
	public List<SynchronizeAction> determineSynchronizeActionsForDIs(Contribuable contribuable) throws AssujettissementException {

		// On récupère les données brutes
		final DomaineContribuable domaine = DomaineContribuable.of(contribuable);
		if (domaine == null) {
			// type de contribuable non traité par la mécanique de recalcul des tâches
			return Collections.emptyList();
		}

		final List<PeriodeImposition> periodes = domaine.filtrerPeriodes(getPeriodesImpositionHisto(contribuable), parametres);
		final List<DeclarationImpotOrdinaire> declarations = domaine.filtrerDeclarations(getDeclarationsActives(contribuable), parametres);
		final List<TacheEnvoiDeclarationImpot> tachesEnvoi = getTachesEnvoiDIsEnInstance(contribuable);
		final List<TacheAnnulationDeclarationImpot> tachesAnnulation = getTachesAnnulationDIsEnInstance(contribuable);

		final List<AddDI> addActions = new ArrayList<>();
		final List<UpdateDI> updateActions = new ArrayList<>();
		final List<DeleteDI> deleteActions = new ArrayList<>();
		final List<AnnuleTache> annuleActions = new ArrayList<>();
		final List<UpdateTacheEnvoiDI> updateTacheActions = new ArrayList<>();

		final RegDate today = RegDate.get();

		//
		// On détermine les périodes d'imposition qui n'ont pas de déclaration d'impôt valide correspondante
		//

		for (final PeriodeImposition periode : periodes) {
			final List<DeclarationImpotOrdinaire> dis = getIntersectingRangeAtWithAdditionnalFilter(declarations, periode, di -> periode.getDateFin().year() == di.getPeriode().getAnnee());
			if (dis == null) {
				// il n'y a pas de déclaration pour la période
				if (periode.isDeclarationMandatory()) {
					// on ajoute une DI si elle est obligatoire
					// [UNIREG-2735] Le mécanisme ne doit pas créer de tâche d'émission de DI pour l'année en cours
					if (peutCreerTacheEnvoiDI(periode, today)) {
						addActions.add(domaine.newAddDI(periode));
					}
				}
			}
			else {
				if (dis.isEmpty()) {
					throw new IllegalArgumentException();
				}
				DeclarationImpotOrdinaire toUpdate = null;
				PeriodeImposition toAdd = null;

				for (DeclarationImpotOrdinaire di : dis) {
					if (DateRangeHelper.equals(di, periode)) {
						// la durée de la déclaration et de la période d'imposition correspondent parfaitement
						final Mutation mutation = domaine.compare(di, periode, today);
						if (mutation == Mutation.COMPATIBLE) {
							// mise à jour possible
							if (toUpdate != null) {
								deleteActions.add(domaine.newDeleteDI(toUpdate));
							}
							toUpdate = di;
							toAdd = null;
						}
						else if (mutation == Mutation.MAJEURE) {
							// on doit passer par une annulation / ré-émission
							deleteActions.add(domaine.newDeleteDI(di));
							if (toUpdate == null) {
								// on prévoit de recréer la déclaration
								toAdd = periode;
							}
						}
					}
					else {
						// la durée de la déclaration et de la période d'imposition ne correspondent pas
						if (toUpdate != null) {
							// il y a déjà une déclaration compatible pouvant être mise-à-jour, inutile de chercher plus loin
							deleteActions.add(domaine.newDeleteDI(di));
						}
						else {
							final Mutation mutation = domaine.compare(di, periode, today);
							if (mutation == Mutation.AUCUNE || mutation == Mutation.COMPATIBLE) {
								// si les types sont compatibles, on adapte la déclaration
								toUpdate = di;
								toAdd = null;
							}
							else if (!isDiLibreSurPeriodeCourante(di, today)) {
								// si les types sont incompatibles, on annule et on prévoit de recréer la déclaration
								deleteActions.add(domaine.newDeleteDI(di));
								toAdd = periode;
							}
						}
					}
				}
				if (toUpdate != null) {
					updateActions.add(domaine.newUpdateDI(periode, toUpdate));
				}
				else if (toAdd != null) {
					// [UNIREG-2735] Le mécanisme ne doit pas créer de tâche d'émission de DI pour l'année en cours
					if (peutCreerTacheEnvoiDI(periode, today)) {
						addActions.add(domaine.newAddDI(toAdd));
					}
				}
			}
		}

		// on retranche les actions d'ajout de DI pour lesquelles il existe déjà une tâche d'envoi de DI
		if (!addActions.isEmpty()) {
			for (int i = addActions.size() - 1; i >= 0; i--) {
				final PeriodeImposition periode = addActions.get(i).periodeImposition;
				final TacheEnvoiDeclarationImpot envoi = getMatchingRangeAt(tachesEnvoi, periode);
				if (envoi != null && envoi.getTypeContribuable() == periode.getTypeContribuable() && envoi.getTypeDocument() == periode.getTypeDocumentDeclaration()) {

					// il faut peut-être mettre la tâche existante à jour par rapport aux nouvelles données du contribuable, non ?
					// par exemple : catégorie d'entreprise...
					final UpdateTacheEnvoiDI updateTache = domaine.newUpdateTacheEnvoi(envoi, addActions.get(i));
					if (updateTache != null) {
						updateTacheActions.add(updateTache);
					}

					addActions.remove(i);
				}
			}
		}

		//
		// On détermine toutes les déclarations qui ne sont pas valides vis-à-vis des périodes d'imposition
		//

		for (final DeclarationImpotOrdinaire declaration : declarations) {
			final List<PeriodeImposition> ps = getIntersectingRangeAtWithAdditionnalFilter(periodes, declaration, pi -> pi.getDateFin().year() == declaration.getPeriode().getAnnee());
			if (ps == null) {
				if (!isDeclarationToBeUpdated(updateActions, declaration)) { // [UNIREG-3028]
					// il n'y a pas de période correspondante
					deleteActions.add(domaine.newDeleteDI(declaration));
				}
			}
			else {
				if (ps.isEmpty()) {
					throw new IllegalArgumentException();
				}
				// s'il y a une intersection entre la déclaration et une période d'imposition, le cas a déjà été traité à partir des périodes d'imposition -> rien d'autre à faire
			}
		}

		// on retranche les déclarations pour lesquelles il existe déjà une tâche d'annulation
		if (!deleteActions.isEmpty()) {
			for (int i = deleteActions.size() - 1; i >= 0; i--) {
				final Long diId = deleteActions.get(i).diId;
				for (TacheAnnulationDeclarationImpot annulation : tachesAnnulation) {
					if (annulation.getDeclaration().getId().equals(diId)) {
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
			if (!isTacheEnvoiDIValide(envoi, periodes, declarations, updateActions)) {
				annuleActions.add(new AnnuleTache(envoi));
			}
		}

		for (TacheAnnulationDeclarationImpot annulation : tachesAnnulation) {
			if (!isTacheAnnulationDIValide(annulation, periodes, updateActions, today)) {
				annuleActions.add(new AnnuleTache(annulation));
			}
		}

		final int size = addActions.size() + updateActions.size() + deleteActions.size() + annuleActions.size() + updateTacheActions.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		else {
			final List<SynchronizeAction> actions = new ArrayList<>(size);
			actions.addAll(domaine.filtrerActions(addActions, parametres));
			actions.addAll(domaine.filtrerActions(updateActions, parametres));
			actions.addAll(domaine.filtrerActions(deleteActions, parametres));
			actions.addAll(domaine.filtrerActions(annuleActions, parametres));
			actions.addAll(domaine.filtrerActions(updateTacheActions, parametres));
			return actions;
		}
	}

	/**
	 * Détermine si la tâche d'envoi d'un questionnaire SNC est (toujours) valide en se mettant dans la position où les actions prévues ont été effectuées.
	 *
	 * @param envoi         une tâche d'envoi
	 * @param periodes      les périodes théoriques du contribuable
	 * @param questionnaires les déclarations existantes
	 * @param updates les actions prévues de mise-à-jour des questionnaires
	 * @return <b>vrai</b> si la tâche est valide; <b>faux</b> si elle est invalide et doit être annulée.
	 */
	private static boolean isTacheEnvoiQuestionnaireSNCValide(TacheEnvoiQuestionnaireSNC envoi, List<DateRange> periodes, List<QuestionnaireSNC> questionnaires, List<UpdateQSNC> updates) {

		final DateRange periode = getMatchingRangeAt(periodes, envoi);
		if (periode == null) {
			// pas de période correspondante -> la tâche n'est plus valable
			return false;
		}

		final QuestionnaireSNC questionnaire = getMatchingRangeAt(questionnaires, periode);
		if (questionnaire == null) {
			// il n'y a pas de questionnaire, la tâche est donc valide
			return true;
		}

		// la tâche est invalide
		return false;
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
	private static boolean isTacheEnvoiDIValide(TacheEnvoiDeclarationImpot envoi, List<PeriodeImposition> periodes, List<DeclarationImpotOrdinaire> declarations, List<UpdateDI> updateActions) {

		final PeriodeImposition periode = getMatchingRangeAt(periodes, envoi);
		if (periode == null || !periode.isDeclarationMandatory()) {
			// pas de période correspondante -> la tâche n'est plus valable
			// [SIFISC-1653] déclaration d'impôt pas obligatoire -> la tâche n'est plus valable
			return false;
		}

		if (envoi.getTypeContribuable() != periode.getTypeContribuable()) {
			// il y a une période correspondante, mais le type ne correspond pas -> la tâche n'est plus valable
			return false;
		}

		if (envoi.getTypeDocument() != periode.getTypeDocumentDeclaration()) {
			// il y a une période correspondante pour le bon type de contribuable, mais le type de document n'est plus le même -> la tâche n'est plus valable
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
	 * @param updates les actions prévues de mise-à-jour des déclarations
	 * @return <b>vrai</b> si la tâche est valide; <b>faux</b> si elle est invalide et doit être annulée.
	 */
	private static boolean isTacheAnnulationQuestionnaireSNCValide(TacheAnnulationQuestionnaireSNC annulation, List<DateRange> periodes, List<UpdateQSNC> updates) {

		final QuestionnaireSNC questionnaire = annulation.getDeclaration();
		if (questionnaire.isAnnule()) {
			// la déclaration est déjà annulée -> la tâche ne sert à rien
			return false;
		}

		if (isQuestionnaireToBeUpdated(updates, questionnaire)) { // [UNIREG-3028]
			// la déclaration va être mise-à-jour, la tâche d'annulation est donc invalide
			return false;
		}

		// si on trouve une période théorique correspondante à ce questionnaire, la tâche d'annulation n'est plus valide, et l'est dans le cas contraire
		final DateRange periode = getMatchingRangeAt(periodes, questionnaire);
		return periode == null;
	}

	/**
	 * Détermine si la tâche d'annulation d'une déclaration d'impôt est (toujours) valide en se mettant dans la position où les actions prévues ont été effectuées.
	 *
	 * @param annulation    une tâche d'annulation
	 * @param periodes      les périodes d'imposition théorique du contribuable
	 * @param updateActions les actions prévues de mise-à-jour des déclarations
	 * @param dateReference date de référence
	 * @return <b>vrai</b> si la tâche est valide; <b>faux</b> si elle est invalide et doit être annulée.
	 */
	private static boolean isTacheAnnulationDIValide(TacheAnnulationDeclarationImpot annulation, List<PeriodeImposition> periodes, List<UpdateDI> updateActions, RegDate dateReference) {

		final DeclarationImpotOrdinaire declaration = annulation.getDeclaration();
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
		if (periode.getTypeContribuable() == declaration.getTypeContribuable() || peutMettreAJourDeclarationExistante(declaration, periode, dateReference)) { // [UNIREG-3028]
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
	 * @param updateActions la liste des actions de mise-à-jour
	 * @param questionnaire   un questionnaire SNC
	 * @return <b>vrai</b> si le questionnaire spécifié est référencé dans la liste d'actions de mise-à-jour; <b>faux</b> si ce n'est pas le cas.
	 */
	private static boolean isQuestionnaireToBeUpdated(List<UpdateQSNC> updateActions, QuestionnaireSNC questionnaire) {
		boolean declarationUpdated = false;
		if (!updateActions.isEmpty()) {
			for (UpdateQSNC updateAction : updateActions) {
				if (updateAction.questionnaireId == questionnaire.getId()) {
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
	 * @param dateReference date de référence
	 * @return <code>true</code> si la mise à jour est autorisée, <code>false</code> sinon.
	 */
	@SuppressWarnings({"UnusedParameters"})
	private static boolean peutMettreAJourDeclarationExistante(DeclarationImpotOrdinaire diExistante, PeriodeImposition periode, RegDate dateReference) {
		return isPeriodePasseeOuCouranteIncomplete(periode, dateReference);
	}

	private static boolean isDiLibreSurPeriodeCourante(DeclarationImpotOrdinaire di, RegDate dateReference) {
		return di.isLibre() && di.isValidAt(dateReference);
	}

	/**
	 * On peut créer une tache d'envoi de DI pour toute période d'imposition dans une année passée.<br/>
	 * Sur la période courante, il faut que la période d'imposition se termine avant la date de référence.
	 * @param periode période d'imposition pour laquelle on voudrait peut-être créer une tâche d'envoi de DI
	 * @param dateReference date de référence
	 * @return <code>true</code> si la création de la tâche est autorisée, <code>false</code> sinon
	 */
	private static boolean peutCreerTacheEnvoiDI(PeriodeImposition periode, RegDate dateReference) {
		return isPeriodePasseeOuCouranteIncomplete(periode, dateReference);
	}

	/**
	 * Une période d'imposition est dite passée si elle fait référence à une année qui n'est pas l'année courante.<br/>
	 * Une période d'imposition est dite incomplète si elle se termine avant la fin de l'année civile
	 * @param periode période d'imposition à tester
	 * @param dateReference date de référence
	 * @return <code>true</code> si la période est passée ou courante incomplète, <code>false</code> sinon (courante complète ou, pourquoi pas, future)
	 */
	private static boolean isPeriodePasseeOuCouranteIncomplete(PeriodeImposition periode, RegDate dateReference) {
		return periode.getDateFin().isBefore(dateReference) || periode.isFermetureAnticipee();
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
		CollectiviteAdministrative collectivite;
		if (contribuable instanceof ContribuableImpositionPersonnesMorales) {
			collectivite = tiersService.getCollectiviteAdministrative(ServiceInfrastructureService.noOIPM, true);
		}
		else {
			collectivite = tiersService.getOfficeImpotAt(contribuable, null);
			if (collectivite == null) {

				// [UNIREG-3285] On analyse les fors fiscaux du contribuable à la recherche d'un for qui puisse être utilisé pour déterminer un OID convenable
				ForFiscal dernierForFiscalVaudois = null;
				ForFiscal dernierForFiscalVaudoisNonSourceAnnule = null;

				final List<ForFiscal> fors = contribuable.getForsFiscauxSorted();
				for (int i = fors.size() - 1; i >= 0; --i) {
					final ForFiscal f = fors.get(i);
					if (f.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
						if (dernierForFiscalVaudois == null) {
							dernierForFiscalVaudois = f;
						}
						if (f.isAnnule()) {
							if (f instanceof ForFiscalSecondaire || (f instanceof ForFiscalPrincipalPP && ((ForFiscalPrincipalPP) f).getModeImposition() != ModeImposition.SOURCE)) {
								dernierForFiscalVaudoisNonSourceAnnule = f;
								break;
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
				collectivite = tiersService.getOfficeImpot(oid);
			}
		}

		if (collectivite == null) {
			throw new IllegalArgumentException();
		}
		return collectivite;
	}

	private List<PeriodeImposition> getPeriodesImpositionHisto(Contribuable contribuable) throws AssujettissementException {
		final List<PeriodeImposition> pis = periodeImpositionService.determine(contribuable);
		return pis == null ? Collections.emptyList() : pis;
	}

	@SuppressWarnings({"unchecked"})
	private List<DeclarationImpotOrdinaire> getDeclarationsActives(Contribuable contribuable) {
		return contribuable.getDeclarationsTriees(DeclarationImpotOrdinaire.class, false);
	}

	private List<TacheAnnulationDeclarationImpot> getTachesAnnulationDIsEnInstance(Contribuable contribuable) {
		return getTachesEnInstance(contribuable, TypeTache.TacheAnnulationDeclarationImpot);
	}

	private List<TacheAnnulationQuestionnaireSNC> getTachesAnnulationQuestionnairesSNCEnInstance(Entreprise entreprise) {
		return getTachesEnInstance(entreprise, TypeTache.TacheAnnulationQuestionnaireSNC);
	}

	private List<TacheEnvoiDeclarationImpot> getTachesEnvoiDIsEnInstance(Contribuable contribuable) {
		if (contribuable instanceof ContribuableImpositionPersonnesPhysiques) {
			return getTachesEnInstance(contribuable, TypeTache.TacheEnvoiDeclarationImpotPP);
		}
		if (contribuable instanceof ContribuableImpositionPersonnesMorales) {
			return getTachesEnInstance(contribuable, TypeTache.TacheEnvoiDeclarationImpotPM);
		}
		return Collections.emptyList();
	}

	private List<TacheEnvoiQuestionnaireSNC> getTachesEnvoiQuestionnairesSNCEnInstance(Entreprise entreprise) {
		return getTachesEnInstance(entreprise, TypeTache.TacheEnvoiQuestionnaireSNC);
	}

	private <T extends Tache> List<T> getTachesEnInstance(Contribuable contribuable, TypeTache typeTache) {
		final TacheCriteria criterion = new TacheCriteria();
		criterion.setContribuable(contribuable);
		criterion.setEtatTache(TypeEtatTache.EN_INSTANCE);
		criterion.setTypeTache(typeTache);
		criterion.setInclureTachesAnnulees(false);

		final List<T> tachesEnvoi;
		final List<Tache> list = tacheDAO.find(criterion, true);
		if (list.isEmpty()) {
			tachesEnvoi = Collections.emptyList();
		}
		else {
			tachesEnvoi = new ArrayList<>(list.size());
			for (Tache t : list) {
				tachesEnvoi.add((T) t);
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

	private static <T extends DateRange> List<T> getIntersectingRangeAt(List<T> dis, DateRange range) {
		return getIntersectingRangeAtWithAdditionnalFilter(dis, range, x -> true);
	}

	private static <T extends DateRange> List<T> getIntersectingRangeAtWithAdditionnalFilter(List<T> src, DateRange range, Predicate<? super T> filter) {
		if (src == null || src.isEmpty()) {
			return null;
		}
		final List<T> filtered = src.stream()
				.filter(s -> DateRangeHelper.intersect(s, range))
				.filter(filter)
				.collect(Collectors.toList());
		return filtered.isEmpty() ? null : filtered;
	}

	/**
	 * Genere une tache à partir de la overture d'un for secondaire
	 *
	 * @param contribuable le contribuable sur lequel un for secondaire a été ouvert
	 * @param forFiscal    le for fiscal secondaire qui vient d'être ouvert
	 */
	@Override
	public void genereTacheDepuisOuvertureForSecondaire(Contribuable contribuable, ForFiscalSecondaire forFiscal) {

		ForFiscalPrincipal forPrincipal = contribuable.getForFiscalPrincipalAt(null);

		// [UNIREG-1888] Aucune tâche générée pour les contribuables dont le mode d'imposition est "SOURCE"
		if (forPrincipal != null && forPrincipal.getTypeAutoriteFiscale() != TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD && !isExcludingFromTaskManagement(forPrincipal)) {
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
		final ProduireListeTachesEnInstanceParOIDProcessor processor = new ProduireListeTachesEnInstanceParOIDProcessor(hibernateTemplate, tiersService, adresseService);
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

	public void setEtiquetteService(EtiquetteService etiquetteService) {
		this.etiquetteService = etiquetteService;
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
