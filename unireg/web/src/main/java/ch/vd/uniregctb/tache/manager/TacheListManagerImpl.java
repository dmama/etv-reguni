package ch.vd.uniregctb.tache.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.ParamPagination;
import ch.vd.uniregctb.declaration.DeclarationException;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.editique.EditiqueResultat;
import ch.vd.uniregctb.editique.EditiqueService;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteException;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.security.SecurityDebugConfig;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.NouveauDossierListView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;
import ch.vd.uniregctb.tache.view.TacheCriteriaViewBase;
import ch.vd.uniregctb.tache.view.TacheListView;
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

	private final Integer DELAI_RETOUR_DI = new Integer(60);

	private TacheDAO tacheDAO;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfrastructureService;
	private AdresseService adresseService;
	private ServiceSecuriteService serviceSecurite;
	private EditiqueService editiqueService;

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



	public EditiqueService getEditiqueService() {
		return editiqueService;
	}

	public void setEditiqueService(EditiqueService editiqueService) {
		this.editiqueService = editiqueService;
	}

	/**
	 * Recherche de declarations d'impot suivant certains criteres
	 *
	 * @param tacheCriteria
	 * @param paramPagination
	 * @return
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<TacheListView> find(TacheCriteriaView tacheCriteria, ParamPagination paramPagination) throws InfrastructureException,
			AdressesResolutionException {
		List<TacheListView> tachesView = new ArrayList<TacheListView>();

		TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteria);
		List<Tache> taches = tacheDAO.find(coreCriteria, paramPagination);

		for (Tache tache : taches) {
			final Contribuable contribuable = tache.getContribuable();
			final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
			final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());

			Integer oid = null;
			ch.vd.uniregctb.tiers.CollectiviteAdministrative collectiviteTache = tache.getCollectiviteAdministrativeAssignee();
			if (collectiviteTache!=null) {
				oid = collectiviteTache.getNumeroCollectiviteAdministrative();
			}
			else {
				oid = contribuable.getOfficeImpotId();
			}
			final CollectiviteAdministrative officeImpot = (oid == null ? null : serviceInfrastructureService.getCollectivite(oid));

			final TacheListView tacheView = new TacheListView();
			tacheView.setId(tache.getId());
			tacheView.setNumero(contribuable.getNumero());
			tacheView.setNumeroForGestion(numeroOfsAutoriteFiscale);
			if (officeImpot != null) {
				tacheView.setOfficeImpot(officeImpot.getNomCourt());
			}

			try {
				final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null);
				tacheView.setNomCourrier(nomPrenom);
			}
			catch (IndividuNotFoundException e) {
				// [UNIREG-1545] on cas d'incoherence des données, on évite de crasher (dans la mesure du possible)
				LOGGER.warn("Impossible d'afficher toutes les données de la tâche n°" + tache.getId(), e);
				tacheView.setNomCourrier(Arrays.asList("<erreur: individu introuvable>"));
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

	private TacheCriteria buildCoreCriteria(TacheCriteriaViewBase tacheCriteria) throws InfrastructureException {
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
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria) throws InfrastructureException,
			AdressesResolutionException {

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

			final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null);
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
	 * @throws InfrastructureException
	 * @throws AdressesResolutionException
	 */
	public List<NouveauDossierListView> find(NouveauDossierCriteriaView dossierCriteria, ParamPagination paramPagination) throws InfrastructureException,
			AdressesResolutionException {

		List<NouveauDossierListView> nouveauxDossiersView = new ArrayList<NouveauDossierListView>();
		dossierCriteria.setTypeTache(TypeTache.TacheNouveauDossier.toString());
		TacheCriteria coreCriteria = buildCoreCriteria(dossierCriteria);
		List<Tache> taches = tacheDAO.find(coreCriteria, paramPagination);

		for (Tache tache : taches) {
			final Contribuable contribuable = tache.getContribuable();
			final ForGestion forGestionActif = tiersService.getDernierForGestionConnu(contribuable, null);
			final Integer numeroOfsAutoriteFiscale = (forGestionActif == null ? null : forGestionActif.getNoOfsCommune());
			final Integer oid = contribuable.getOfficeImpotId();
			final CollectiviteAdministrative officeImpot = (oid == null ? null : serviceInfrastructureService.getCollectivite(oid));

			final NouveauDossierListView nouveauDossierView = new NouveauDossierListView();
			nouveauDossierView.setId(tache.getId());
			nouveauDossierView.setNumero(contribuable.getNumero());
			nouveauDossierView.setNumeroForGestion(numeroOfsAutoriteFiscale);
			if (officeImpot != null) {
				nouveauDossierView.setOfficeImpot(officeImpot.getNomCourt());
			}

			final List<String> nomPrenom = adresseService.getNomCourrier(contribuable, null);
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
	 * @throws InfrastructureException
	 * @throws EditiqueException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public String envoieImpressionLocalDossier(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws EditiqueException, InfrastructureException {
		LOGGER.debug("Tab Ids dossiers:" + nouveauDossierCriteriaView.getTabIdsDossiers());
		List<Contribuable> contribuables = new ArrayList<Contribuable>();
		if (nouveauDossierCriteriaView.getTabIdsDossiers() != null) {
			for (int i = 0; i < nouveauDossierCriteriaView.getTabIdsDossiers().length; i++) {
				Tache tache = tacheDAO.get(new Long(nouveauDossierCriteriaView.getTabIdsDossiers()[i]));
				tache.setEtat(TypeEtatTache.TRAITE);
				contribuables.add(tache.getContribuable());
			}
		}
		String docId = editiqueService.imprimeNouveauxDossiers(contribuables);
		return docId;
	}

	/**
	 * Imprime un nouveau dossier
	 * Partie reception
	 * @param lrEditView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public byte[] recoitImpressionLocalDossier(String docID) throws DeclarationException {
		EditiqueResultat editiqueResultat;
		try {
			editiqueResultat = editiqueService.getDocument(docID, true);
		}
		catch (JMSException e) {
			throw new DeclarationException(e);
		}
		if (editiqueResultat == null) {
			return null;
		}
		return editiqueResultat.getDocument();
	}

	/**
	 * Retourne le nombre de tache correspondant aux criteres
	 *
	 * @param criterion
	 * @return
	 * @throws InfrastructureException
	 */
	public int count(TacheCriteriaView tacheCriteriaView) throws InfrastructureException {
		TacheCriteria coreCriteria = buildCoreCriteria(tacheCriteriaView);

		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Retourne le nombre de nouveaux dossiers correspondant aux criteres
	 *
	 * @param nouveauDossierCriteriaView
	 * @return
	 * @throws InfrastructureException
	 */
	public int count(NouveauDossierCriteriaView nouveauDossierCriteriaView) throws InfrastructureException {
		TacheCriteria coreCriteria = buildCoreCriteria(nouveauDossierCriteriaView);

		return tacheDAO.count(coreCriteria);
	}

	/**
	 * Passe la tâche à l'état TRAITE
	 *
	 * @param id
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void traiteTache(Long id) {
		Tache tache = tacheDAO.get(id);
		tache.setEtat(TypeEtatTache.TRAITE);
	}

}
