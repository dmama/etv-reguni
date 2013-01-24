package ch.vd.uniregctb.tache.manager;

import javax.jms.JMSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.editique.EditiqueCompositionService;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueResultatDocument;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.SecurityDebugConfig;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheCriteriaViewBase;
import ch.vd.uniregctb.tache.view.TacheListView;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.IndividuNotFoundException;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeEtatTache;
import ch.vd.uniregctb.type.TypeTache;

/**
 * Manager de recherche de taches
 *
 * @author xcifde
 *
 */
public class TacheListManagerImpl implements TacheListManager {

	protected static final Logger LOGGER = Logger.getLogger(TacheListManagerImpl.class);

	private static final int DELAI_RETOUR_DI = 60;

	private TacheDAO tacheDAO;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private AdresseService adresseService;
	private ServiceSecuriteService serviceSecurite;
	private EditiqueCompositionService editiqueService;

	public void setTacheDAO(TacheDAO tacheDAO) {
		this.tacheDAO = tacheDAO;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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

	private String getNomCollectiviteAdministrativeAssociee(Tache tache) throws ServiceInfrastructureException {
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
	 *
	 * @param tacheCriteria
	 * @param paramPagination
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdressesResolutionException {
		final List<TacheListView> tachesView = new ArrayList<TacheListView>();

		final TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteria);
		final List<Tache> taches = tacheDAO.find(coreCriteria, paramPagination);

		for (Tache tache : taches) {
			final Contribuable contribuable = tache.getContribuable();
			final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
			final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());

			final TacheListView tacheView = new TacheListView();
			tacheView.setId(tache.getId());
			tacheView.setNumero(contribuable.getNumero());
			tacheView.setNumeroForGestion(numeroOfsAutoriteFiscale);

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
				tacheView.setNomCourrier(Arrays.asList("<erreur: individu introuvable>"));
			}
			catch (Exception e) {
				LOGGER.warn("Impossible d'afficher toutes les données de la tâche n°" + tache.getId(), e);
				tacheView.setNomCourrier(Arrays.asList("<erreur: " + e.getMessage() + '>'));
			}

			tacheView.setTypeTache(tache.getClass().getSimpleName());
			tacheView.setEtatTache(tache.getEtat());

			if (tache instanceof TacheEnvoiDeclarationImpot) {
				final TacheEnvoiDeclarationImpot tedi = (TacheEnvoiDeclarationImpot) tache;
				final int annee = tedi.getDateDebut().year();
				tacheView.setAnnee(annee);
				tacheView.setDateDebutImposition(tedi.getDateDebut());
				tacheView.setDateFinImposition(tedi.getDateFin());
				tacheView.setTypeContribuable(tedi.getTypeContribuable());
				tacheView.setTypeDocument(tedi.getTypeDocument());
				tacheView.setDelaiRetourEnJours(DELAI_RETOUR_DI);
			}
			else if (tache instanceof TacheAnnulationDeclarationImpot) {
				final TacheAnnulationDeclarationImpot tadi = (TacheAnnulationDeclarationImpot) tache;
				final int annee = tadi.getDeclarationImpotOrdinaire().getDateDebut().year();
				tacheView.setAnnee(annee);
				tacheView.setDateDebutImposition(tadi.getDeclarationImpotOrdinaire().getDateDebut());
				tacheView.setDateFinImposition(tadi.getDeclarationImpotOrdinaire().getDateFin());
				tacheView.setTypeContribuable(tadi.getDeclarationImpotOrdinaire().getTypeContribuable());
				tacheView.setTypeDocument(tadi.getDeclarationImpotOrdinaire().getModeleDocument().getTypeDocument());
				tacheView.setIdDI(tadi.getDeclarationImpotOrdinaire().getId());
			}

			tacheView.setAnnulee(tache.isAnnule());
			tachesView.add(tacheView);
		}

		return tachesView;
	}

	private TacheCriteria buildCoreCriteria(TacheCriteriaViewBase tacheCriteria) throws ServiceInfrastructureException {
		TacheCriteria coreCriteria = tacheCriteria.asCoreCriteria();
		if ((tacheCriteria.getTypeTache() == null) || (tacheCriteria.getTypeTache().equals("TOUS"))) {
			coreCriteria.setTypeTache(TypeTache.TacheNouveauDossier);
			coreCriteria.setInvertTypeTache(true);
		} else {
			coreCriteria.setInvertTypeTache(false);
		}

		if (coreCriteria.getOid() == null) { //l'utilisateur a choisit TOUS =>  Filtre sur les OID de l'utilisateur

			if (!SecurityDebugConfig.isIamDebug()) {

				List<ch.vd.infrastructure.model.CollectiviteAdministrative> collectivites = null;
				try {
					collectivites = serviceSecurite.getCollectivitesUtilisateur(AuthenticationHelper.getCurrentPrincipal());
				}
				catch (ServiceSecuriteException e) {
					// si le visa n'existe pas, l'ejb lève une sécurité exception au lieu de retourner une collection vide...
					collectivites = Collections.emptyList();
				}

				List<Integer> oids = new ArrayList<Integer>(collectivites.size());

				for (ch.vd.infrastructure.model.CollectiviteAdministrative c:collectivites) {
					if (c.isACI()) {
						// les personnes de l'ACI voient toutes les tâches sans restriction
						oids = null;
						break;
					}
					else {
						oids.add(c.getNoColAdm());
					}
				}
				if (oids != null) {
					coreCriteria.setOidUser(oids.toArray(new Integer[oids.size()]));
				}
			}
		}
		else if (coreCriteria.getOid() == serviceInfrastructureService.getACI().getNoColAdm()) {//ACI == tous)
			coreCriteria.setOid(null);
		}

		return coreCriteria;
	}

	/**
	 * Recherche des nouveaux dossiers suivant certains critères
	 *
	 * @param dossierCriteria
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws ServiceInfrastructureException, AdresseException {

		List<NouveauDossierListView> dossiersView = new ArrayList<NouveauDossierListView>();

		dossierCriteria.setTypeTache(TypeTache.TacheNouveauDossier.toString());
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
	 *
	 * @param dossierCriteria
	 * @param paramPagination
	 * @return
	 * @throws ServiceInfrastructureException
	 * @throws AdressesResolutionException
	 */
	@Override
	@Transactional(readOnly = true)
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws ServiceInfrastructureException, AdresseException {

		final List<NouveauDossierListView> nouveauxDossiersView = new ArrayList<NouveauDossierListView>();
		dossierCriteria.setTypeTache(TypeTache.TacheNouveauDossier.toString());
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
	 *
	 * @param nouveauDossierCriteriaView
	 * @throws ServiceInfrastructureException
	 * @throws EditiqueException
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocalDossier(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws EditiqueException {

		final Long[] tabIdsDossiers = nouveauDossierCriteriaView.getTabIdsDossiers();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Tab Ids dossiers:" + Arrays.toString(tabIdsDossiers));
		}

		if (tabIdsDossiers != null && tabIdsDossiers.length > 0) {
			final List<Contribuable> contribuables = new ArrayList<Contribuable>(tabIdsDossiers.length);
			final List<Tache> taches = new ArrayList<Tache>(tabIdsDossiers.length);
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
	 *
	 * @param criterion
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(TacheCriteriaView tacheCriteriaView) throws ServiceInfrastructureException {
		TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteriaView);

		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws ServiceInfrastructureException
	 */
	@Override
	@Transactional(readOnly = true)
	public int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws ServiceInfrastructureException {
		TacheCriteria coreCriteria = buildCoreCriteria(nouveauDossierCriteriaView);

		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Passe la tâche à l'état TRAITE
	 *
	 * @param id
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void traiteTache(Long id) {
		Tache tache = tacheDAO.get(id);
		tache.setEtat(TypeEtatTache.TRAITE);
	}

}
