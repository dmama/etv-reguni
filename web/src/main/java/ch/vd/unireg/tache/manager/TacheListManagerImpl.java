package ch.vd.unireg.tache.manager;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FiscalDateHelper;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.editique.EditiqueCompositionService;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.editique.EditiqueResultatDocument;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.security.SecurityDebugConfig;
import ch.vd.unireg.tache.TacheService;
import ch.vd.unireg.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.unireg.tache.view.NouveauDossierCriteriaView;
import ch.vd.unireg.tache.view.NouveauDossierListView;
import ch.vd.unireg.tache.view.TacheCriteriaView;
import ch.vd.unireg.tache.view.TacheCriteriaViewBase;
import ch.vd.unireg.tache.view.TacheListView;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EntrepriseNotFoundException;
import ch.vd.unireg.tiers.ForGestion;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheAnnulationDeclaration;
import ch.vd.unireg.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.tiers.TacheDAO;
import ch.vd.unireg.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.unireg.tiers.TacheEnvoiDocument;
import ch.vd.unireg.tiers.TacheEnvoiQuestionnaireSNC;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.manager.AutorisationCache;
import ch.vd.unireg.tiers.manager.Autorisations;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeTache;

/**
 * Manager de recherche de taches
 *
 * @author xcifde
 */
public class TacheListManagerImpl implements TacheListManager {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TacheListManagerImpl.class);

	private static final int DELAI_RETOUR_DI = 60;

	private TacheDAO tacheDAO;
	private TiersService tiersService;
	private TacheService tacheService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private AdresseService adresseService;
	private ServiceSecuriteService serviceSecurite;
	private EditiqueCompositionService editiqueService;
	private ExecutorService threadPool;
	private PlatformTransactionManager transactionManager;
	private AutorisationCache autorisationCache;

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setTacheService(TacheService tacheService) {
		this.tacheService = tacheService;
	}

	public void setServiceInfrastructureService(ServiceInfrastructureService serviceInfrastructureService) {
		this.serviceInfrastructureService = serviceInfrastructureService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceSecurite(ServiceSecuriteService serviceSecurite) {
		this.serviceSecurite = serviceSecurite;
	}

	public void setEditiqueService(EditiqueCompositionService editiqueService) {
		this.editiqueService = editiqueService;
	}

	public void setThreadPool(ExecutorService threadPool) {
		this.threadPool = threadPool;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setAutorisationCache(AutorisationCache autorisationCache) {
		this.autorisationCache = autorisationCache;
	}

	private String getNomCollectiviteAdministrativeAssociee(Tache tache) throws InfrastructureException {
		final CollectiviteAdministrative caAssignee = tache.getCollectiviteAdministrativeAssignee();
		if (caAssignee != null) {
			final ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative ca = serviceInfrastructureService.getCollectivite(caAssignee.getNumeroCollectiviteAdministrative());
			return ca.getNomCourt();
		}
		else {
			return null;
		}
	}

	/**
	 * Recherche de declarations d'impot suivant certains criteres
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws InfrastructureException, AdressesResolutionException {

		final String currentPrincipal = AuthenticationHelper.getCurrentPrincipal();
		final Integer currentOID = AuthenticationHelper.getCurrentOID();
		if (currentOID == null) {
			throw new IllegalArgumentException("L'OID courant de l'utilisateur [" + currentPrincipal + "] n'est pas défini.");
		}

		// on recherche les tâches
		final TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteria);
		final List<Tache> taches = tacheDAO.find(coreCriteria, paramPagination);

		// on les transforme dans leurs vues respectives en utilisant plusieurs threads
		return CollectionsUtils.parallelMap(taches, t -> newTacheView(t.getId(), currentPrincipal, currentOID), threadPool);
	}

	@NotNull
	private TacheListView newTacheView(long tacheId, String currentPrincipal, int currentOID) {
		AuthenticationHelper.pushPrincipal(currentPrincipal, currentOID);
		try {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setReadOnly(true);
			return template.execute(status -> {
				final Tache tache = tacheDAO.get(tacheId);
				final Contribuable contribuable = tache.getContribuable();
				final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
				final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());

				final TacheListView tacheView = new TacheListView();
				tacheView.setId(tache.getId());
				tacheView.setNumero(contribuable.getNumero());
				tacheView.setNumeroForGestion(numeroOfsAutoriteFiscale);
				tacheView.setDateEnregistrement(tache.getLogCreationDate());

				final String nomCa = getNomCollectiviteAdministrativeAssociee(tache);
				if (nomCa != null) {
					tacheView.setOfficeImpot(nomCa);
				}

				try {
					final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null, false);
					tacheView.setNomCourrier(nomPrenom);
				}
				catch (IndividuNotFoundException e) {
					// [UNIREG-1545] on cas d'incoherence des données, on évite de crasher (dans la mesure du possible)
					LOGGER.warn("Impossible d'afficher toutes les données de la tâche n°" + tache.getId(), e);
					tacheView.setNomCourrier(Collections.singletonList("<erreur: individu introuvable>"));
				}
				catch (EntrepriseNotFoundException e) {
					LOGGER.warn("Impossible d'afficher toutes les données de la tâche n°" + tache.getId(), e);
					tacheView.setNomCourrier(Collections.singletonList("<erreur: entreprise introuvable>"));
				}
				catch (Exception e) {
					LOGGER.warn("Impossible d'afficher toutes les données de la tâche n°" + tache.getId(), e);
					tacheView.setNomCourrier(Collections.singletonList("<erreur: " + e.getMessage() + '>'));
				}

				tacheView.setTypeTache(tache.getClass().getSimpleName());
				tacheView.setEtatTache(tache.getEtat());

				if (tache instanceof TacheEnvoiDocument) {
					final TacheEnvoiDocument ted = (TacheEnvoiDocument) tache;
					tacheView.setTypeDocument(ted.getTypeDocument());
					if (tache instanceof TacheEnvoiDeclarationImpot) {
						final TacheEnvoiDeclarationImpot tedi = (TacheEnvoiDeclarationImpot) tache;
						final int annee = tedi.getDateFin().year();
						tacheView.setAnnee(annee);
						tacheView.setDateDebutImposition(tedi.getDateDebut());
						tacheView.setDateFinImposition(tedi.getDateFin());
						tacheView.setLongueurPeriodeImposition(FiscalDateHelper.getLongueurEnJoursOuNullSiPasPossible(tedi));
						tacheView.setTypeContribuable(tedi.getTypeContribuable());
						tacheView.setDelaiRetourEnJours(DELAI_RETOUR_DI);       // TODO pour les PM aussi ???
					}
					else if (tache instanceof TacheEnvoiQuestionnaireSNC) {
						final TacheEnvoiQuestionnaireSNC teqsnc = (TacheEnvoiQuestionnaireSNC) tache;
						final int annee = teqsnc.getDateFin().year();
						tacheView.setAnnee(annee);
						tacheView.setDateDebutImposition(teqsnc.getDateDebut());
						tacheView.setDateFinImposition(teqsnc.getDateFin());
					}
				}
				else if (tache instanceof TacheAnnulationDeclaration) {
					final TacheAnnulationDeclaration<?> tad = (TacheAnnulationDeclaration<?>) tache;
					final Declaration declaration = tad.getDeclaration();
					final int annee = declaration.getPeriode().getAnnee();
					tacheView.setAnnee(annee);
					tacheView.setDateDebutImposition(declaration.getDateDebut());
					tacheView.setDateFinImposition(declaration.getDateFin());
					tacheView.setLongueurPeriodeImposition(FiscalDateHelper.getLongueurEnJoursOuNullSiPasPossible(declaration));
					tacheView.setTypeDocument(declaration.getModeleDocument() != null ? declaration.getModeleDocument().getTypeDocument() : null);
					tacheView.setIdDI(declaration.getId());

					if (tache instanceof TacheAnnulationDeclarationImpot) {
						final TacheAnnulationDeclarationImpot tadi = (TacheAnnulationDeclarationImpot) tache;
						final DeclarationImpotOrdinaire di = tadi.getDeclaration();
						tacheView.setTypeContribuable(di.getTypeContribuable());
					}
				}

				tacheView.setAnnule(tache.isAnnule());
				tacheView.setCommentaire(StringUtils.trimToNull(tache.getCommentaire()));
				try {
					tacheView.setAuthDossier(autorisationCache.getAutorisations(contribuable.getNumero(), currentPrincipal, currentOID));
				}
				catch (IndividuNotFoundException | EntrepriseNotFoundException e) {
					// erreur normalement déjà signalée... aucun droit de modification en tout état de cause...
					tacheView.setAuthDossier(new Autorisations());
				}
				return tacheView;
			});
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private TacheCriteria buildCoreCriteria(TacheCriteriaViewBase tacheCriteria) throws InfrastructureException {
		TacheCriteria coreCriteria = tacheCriteria.asCoreCriteria();
		if (tacheCriteria.getTypeTache() == null) {
			coreCriteria.setTypeTache(TypeTache.TacheNouveauDossier);
			coreCriteria.setInvertTypeTache(true);
		}
		else {
			coreCriteria.setInvertTypeTache(false);
		}

		if (coreCriteria.getOid() == null) { //l'utilisateur a choisit TOUS =>  Filtre sur les OID de l'utilisateur
			if (!SecurityDebugConfig.isSecurityDebug()) {

				// [SIFISC-22880] si l'utilisateur est loggué dans la collectivité 22, "Tous" signifie "Tous", non-limité aux
				// collectivités dans lesquelles l'utilisateur à des droits
				final Integer loggedInOid = AuthenticationHelper.getCurrentOID();
				if (loggedInOid == null || loggedInOid != ServiceInfrastructureService.noACI) {
					List<ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative> collectivites;
					try {
						collectivites = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
						if (collectivites == null) {
							collectivites = Collections.emptyList();
						}
					}
					catch (ServiceSecuriteException e) {
						// si le visa n'existe pas, l'ejb lève une sécurité exception au lieu de retourner une collection vide...
						collectivites = Collections.emptyList();
					}

					final List<Integer> oids = new ArrayList<>(collectivites.size());
					for (ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative c : collectivites) {
						oids.add(c.getNoColAdm());
					}
					coreCriteria.setOidUser(oids.toArray(new Integer[0]));
				}
			}
		}

		return coreCriteria;
	}

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 */
	@Override
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws InfrastructureException, AdresseException {

		List<NouveauDossierListView> dossiersView = new ArrayList<>();

		dossierCriteria.setTypeTache(TypeTache.TacheNouveauDossier);
		TacheCriteria coreCriteria = buildCoreCriteria(dossierCriteria);
		List<Tache> taches = tacheDAO.find(coreCriteria);

		for (Tache tache : taches) {
			final Contribuable contribuable = tache.getContribuable();
			final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
			final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());
			//	final Integer officeImpotID = contribuable.getOfficeImpotId();
			//	if (officeImpotID == null || mapCollectivites.containsKey(officeImpotID)) {
			NouveauDossierListView dossierView = new NouveauDossierListView();
			dossierView.setId(tache.getId());
			dossierView.setNumero(contribuable.getNumero());
			dossierView.setNumeroForGestion(numeroOfsAutoriteFiscale);
			dossierView.setAnnule(tache.isAnnule());

			final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null, false);
			dossierView.setNomCourrier(nomPrenom);
			dossierView.setEtatTache(tache.getEtat());
			dossiersView.add(dossierView);
			//}
		}
		Collections.sort(dossiersView);
		return dossiersView;
	}

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 */
	@Override
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws InfrastructureException, AdresseException {

		final List<NouveauDossierListView> nouveauxDossiersView = new ArrayList<>();
		dossierCriteria.setTypeTache(TypeTache.TacheNouveauDossier);
		final TacheCriteria coreCriteria = buildCoreCriteria(dossierCriteria);
		final List<Tache> taches = tacheDAO.find(coreCriteria, paramPagination);

		for (Tache tache : taches) {
			final Contribuable contribuable = tache.getContribuable();
			final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
			final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());

			final NouveauDossierListView nouveauDossierView = new NouveauDossierListView();
			nouveauDossierView.setId(tache.getId());
			nouveauDossierView.setNumero(contribuable.getNumero());
			nouveauDossierView.setNumeroForGestion(numeroOfsAutoriteFiscale);

			final String nomCa = getNomCollectiviteAdministrativeAssociee(tache);
			if (nomCa != null) {
				nouveauDossierView.setOfficeImpot(nomCa);
			}

			final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null, false);
			nouveauDossierView.setNomCourrier(nomPrenom);
			nouveauDossierView.setEtatTache(tache.getEtat());
			nouveauDossierView.setAnnule(tache.isAnnule());
			nouveauxDossiersView.add(nouveauDossierView);
		}

		return nouveauxDossiersView;
	}

	/**
	 * Imprime les nouveaux dossiers
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDossier(ImpressionNouveauxDossiersView view) throws EditiqueException {

		final Long[] tabIdsDossiers = view.getTabIdsDossiers();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Tab Ids dossiers:" + Arrays.toString(tabIdsDossiers));
		}

		if (tabIdsDossiers != null && tabIdsDossiers.length > 0) {
			final List<Contribuable> contribuables = new ArrayList<>(tabIdsDossiers.length);
			final List<Tache> taches = new ArrayList<>(tabIdsDossiers.length);
			for (int i = 0; i < tabIdsDossiers.length; i++) {
				final Tache tache = tacheDAO.get(tabIdsDossiers[i]);
				taches.add(tache);
				contribuables.add(tache.getContribuable());
			}

			try {
				final EditiqueResultat impression = editiqueService.imprimeNouveauxDossiers(contribuables);
				if (impression instanceof EditiqueResultatDocument) {
					// on peut marquer les tâches comme traitées, puisque le document est bien revenu...
					for (Tache tache : taches) {
						tache.setEtat(TypeEtatTache.TRAITE);
					}
				}
				return impression;
			}
			catch (JMSException e) {
				throw new EditiqueException(e);
			}
		}

		return null;
	}

	/**
	 * Retourne le nombre de tache correspondant aux criteres
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(TacheCriteriaView tacheCriteriaView) throws InfrastructureException {
		final TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteriaView);
		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws InfrastructureException {
		final TacheCriteria coreCriteria = buildCoreCriteria(nouveauDossierCriteriaView);
		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Passe la tâche à l'état TRAITE
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void traiteTache(Long id) {
		final Tache tache = tacheDAO.get(id);
		tache.setEtat(TypeEtatTache.TRAITE);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getCommentairesDistincts(TypeTache typeTache) {
		return tacheService.getCommentairesDistincts(typeTache);
	}

	@Override
	public @Nullable Contribuable getContribuableFromTache(long tacheId) {
		final Tache tache = tacheDAO.get(tacheId);
		if (tache == null) {
			return null;
		}
		return tache.getContribuable();
	}
}
