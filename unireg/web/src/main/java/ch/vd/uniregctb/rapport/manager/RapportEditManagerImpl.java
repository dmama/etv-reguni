package ch.vd.uniregctb.rapport.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.Tutelle;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;


/**
 * Claase offrant les services au controller RapportEditController
 *
 * @author xcifde
 *
 */
public class RapportEditManagerImpl extends TiersManager implements RapportEditManager {

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroTiers
	 * @param numeroTiersLie
	 * @return une RapportView
	 * @throws AdressesResolutionException
	 */
	@Transactional(readOnly = true)
	public RapportView get(Long numeroTiers, Long numeroTiersLie) throws AdressesResolutionException{
		//création d'un rapport autre que travail
		Tiers tiers = tiersService.getTiers(numeroTiers);

		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		RapportView rapportView =  new RapportView();
		//vérification des droits
		rapportView.setAllowed(checkDroitEdit(tiers));
		if(rapportView.isAllowed()){
			//Tiers
			TiersGeneralView tiersView = tiersGeneralManager.getTiers(tiers, true);
			rapportView.setTiers(tiersView);

			//Tiers lié
			Tiers tiersLie = tiersService.getTiers(numeroTiersLie);
			if (tiersLie == null) {
				throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
			}

			TiersGeneralView tiersLieView = tiersGeneralManager.getTiers(tiersLie, true);
			rapportView.setTiersLie(tiersLieView);
		}
		rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);

		return rapportView;
	}

	@Transactional(readOnly = true)
	public RapportView get(Long idRapport, SensRapportEntreTiers editingFrom) throws AdresseException {

		final RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.rapport.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		RapportView rapportView = new RapportView();
		rapportView.setSensRapportEntreTiers(editingFrom);
		rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));

		final Long numeroTiersCourant;
		final Long numeroTiersLie;
		switch (editingFrom) {
		case OBJET:
			numeroTiersCourant = rapportEntreTiers.getObjetId();
			numeroTiersLie = rapportEntreTiers.getSujetId();
			break;
		case SUJET:
			numeroTiersCourant = rapportEntreTiers.getSujetId();
			numeroTiersLie = rapportEntreTiers.getObjetId();
			break;
		default:
			throw new IllegalArgumentException("Sens de rapport-entre-tiers inconnu =[" + editingFrom + "]");
		}

		// on récupère les tiers eux-mêmes, et quelques infos supplémentaires
		final Tiers tiersCourant = tiersService.getTiers(numeroTiersCourant); // le tiers par lequel on est arrivé sur le rapport
		Assert.notNull(tiersCourant);
		final Tiers tiersLie = tiersService.getTiers(numeroTiersLie); // l'autre tiers du rapport (pas celui par lequel on est arrivé sur le rapport)
		Assert.notNull(tiersLie);

		final List<String> nomTiersCourant = adresseService.getNomCourrier(tiersCourant, null, false);
		final List<String> nomTiersLie = adresseService.getNomCourrier(tiersLie, null, false);
		final String toolTipMessage = getRapportEntreTiersTooltips(rapportEntreTiers);

		rapportView.setNumero(numeroTiersLie);
		rapportView.setNomCourrier(nomTiersLie);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		rapportView.setAllowed(true);
		rapportView.setToolTipMessage(toolTipMessage);

		//vérification droit édition du rapport pour fermeture
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			if (!SecurityProvider.isGranted(Role.RT)) {
				rapportView.setAllowed(false);
			}
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
			rapportView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}
		else if (rapportEntreTiers.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
			rapportView.setAllowed(false);
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
			final Boolean b = repres.getExtensionExecutionForcee();
			rapportView.setExtensionExecutionForcee(b != null && b);
			rapportView.setAllowed(checkDroitEdit(tiersCourant)); // [UNIREG-2814]
		}
		else {//rapport de non travail
			rapportView.setAllowed(checkDroitEdit(tiersLie));
		}
		return rapportView;
	}

	/**
	 * Persiste le rapport entre tiers
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RapportView rapportView) {

		if (rapportView.getId() == null) {

			final TypeRapportEntreTiers type = rapportView.getTypeRapportEntreTiers().toCore();
			final Tiers sujet; // sujet : pupille, curatelle ou personne sous conseil légal
			final Tiers objet; // objet : tuteur, curateur ou conseil légal

			// récupère les données
			final SensRapportEntreTiers sens = rapportView.getSensRapportEntreTiers();
			if (sens == SensRapportEntreTiers.OBJET) {
				sujet = tiersService.getTiers(rapportView.getTiers().getNumero());
				objet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
			}
			else {
				Assert.isEqual(SensRapportEntreTiers.SUJET, sens);
				sujet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
				objet = tiersService.getTiers(rapportView.getTiers().getNumero());
			}
			Assert.notNull(type);

			// instancie le bon rapport
			RapportEntreTiers rapport = type.newInstance();
			rapport.setDateDebut(rapportView.getRegDateDebut());

			// [UNIREG-755] tenir compte de l'extension de l'exécution à la création du rapport
			if (rapport instanceof RepresentationConventionnelle) {
				final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapport;
				validateRepresentationConventionnelle(rapportView, sujet);
				repres.setExtensionExecutionForcee(rapportView.getExtensionExecutionForcee());
			}

			if (rapport instanceof RepresentationLegale) {
				final RepresentationLegale representation = (RepresentationLegale) rapport;
				final Long autoriteId = rapportView.getAutoriteTutelaireId();
				if (autoriteId != null) {
					CollectiviteAdministrative autorite = findAutorite(autoriteId);
					representation.setAutoriteTutelaire(autorite);
				}

			}

			// établit le rapport entre les deux tiers
			tiersService.addRapport(rapport, sujet, objet);
		}
		else {
			RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(rapportView.getId());
			rapportEntreTiers.setDateFin(rapportView.getRegDateFin());
			if (rapportEntreTiers instanceof RapportPrestationImposable) {
				RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
				rapportPrestationImposable.setTauxActivite(rapportView.getTauxActivite());
				rapportPrestationImposable.setTypeActivite(rapportView.getTypeActivite());
			}
			else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
				final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
				final Tiers sujet = tiersDAO.get(repres.getSujetId());
				validateRepresentationConventionnelle(rapportView, sujet);
				repres.setExtensionExecutionForcee(rapportView.getExtensionExecutionForcee());
			}
		}

	}

	private CollectiviteAdministrative findAutorite(Long autoriteTutelaireId) {
		if(autoriteTutelaireId !=null){
			return tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(autoriteTutelaireId.intValue());
		}
		return null;
	}

	private void validateRepresentationConventionnelle(RapportView rapportView, Tiers sujet) {
		if (rapportView.getExtensionExecutionForcee() && sujet instanceof PersonnePhysique && ((PersonnePhysique)sujet).isHabitantVD()) {
			// [UNIREG-1341]
			throw new ValidationException(sujet, "L'extension de l'exécution forcée est uniquement autorisée pour les tiers domiciliés à l'étranger");
		}
	}

	/**
	 * Annule le rapport de prestation
	 *
	 * @param idRapport
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRapport(Long idRapport) {
		RapportEntreTiers rapport = rapportEntreTiersDAO.get(idRapport);
		rapport.setAnnule(true);
	}


	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		if (tiers != null){
			tiersEditView.setTiers(tiers);
			TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
			tiersEditView.setTiersGeneral(tiersGeneralView);
			tiersEditView.setDossiersApparentes(getRapports(tiers));
			if (tiers instanceof Contribuable) {
				Contribuable contribuable = (Contribuable) tiers;
				tiersEditView.setDebiteurs(getDebiteurs(contribuable));
			}

			//gestion des droits d'édition
			boolean allowed = false;
			Map<String, Boolean> allowedOnglet = initAllowedOnglet();
			allowed = setDroitEdition(tiers, allowedOnglet);

			tiersEditView.setAllowedOnglet(allowedOnglet);
			tiersEditView.setAllowed(allowed);

			if(!allowed){
				tiersEditView.setTiers(null);
			}
		}
		else {
			tiersEditView.setAllowed(true);
		}

		return tiersEditView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional(readOnly = true)
	public TiersEditView getRapportsPrestationView(Long numero, WebParamPagination webParamPagination, boolean rapportsPrestationHisto) throws AdresseException, InfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		tiersEditView.setTiers(tiers);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersEditView.setTiersGeneral(tiersGeneralView);
		if (tiers instanceof DebiteurPrestationImposable ) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			tiersEditView.setRapportsPrestation(getRapportsPrestation(dpi, webParamPagination, rapportsPrestationHisto));
			setContribuablesAssocies(tiersEditView, dpi);
			if (dpi.getContribuableId() == null) {
				tiersEditView.setAddContactISAllowed(true);
			}
			else {
				tiersEditView.setAddContactISAllowed(false);
			}
		}
		//gestion des droits d'édition
		boolean allowed = false;
		Map<String, Boolean> allowedOnglet = initAllowedOnglet();
		allowed = setDroitEdition(tiers, allowedOnglet);

		tiersEditView.setAllowedOnglet(allowedOnglet);
		tiersEditView.setAllowed(allowed);

		if(!allowed){
			tiersEditView.setTiers(null);
		}

		return tiersEditView;
	}


	/**
	 * initialise les droits d'édition des onglets du tiers
	 * @return la map de droit d'édition des onglets
	 */
	private Map<String, Boolean> initAllowedOnglet(){
		Map<String, Boolean> allowedOnglet = new HashMap<String, Boolean>();
		allowedOnglet.put(TiersVisuView.MODIF_DOSSIER, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.DOSSIER_NO_TRAVAIL, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.DOSSIER_TRAVAIL, Boolean.FALSE);
		allowedOnglet.put(TiersVisuView.MODIF_RAPPORT, Boolean.FALSE);

		return allowedOnglet;
	}
}
