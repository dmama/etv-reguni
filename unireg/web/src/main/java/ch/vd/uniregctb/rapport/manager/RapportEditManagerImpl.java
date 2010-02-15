package ch.vd.uniregctb.rapport.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebParamPagination;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
import ch.vd.uniregctb.type.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;


/**
 * Claase offrant les services au controller RapportEditController
 *
 * @author xcifde
 *
 */
public class RapportEditManagerImpl extends TiersManager implements RapportEditManager {

	private RapportEntreTiersDAO rapportEntreTiersDAO;

	@Override
	public RapportEntreTiersDAO getRapportEntreTiersDAO() {
		return rapportEntreTiersDAO;
	}

	@Override
	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroTiers
	 * @param numeroTiersLie
	 * @return une RapportView
	 * @throws AdressesResolutionException
	 */
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
			TiersGeneralView tiersView = tiersGeneralManager.get(tiers);
			rapportView.setTiers(tiersView);

			//Tiers lié
			Tiers tiersLie = tiersService.getTiers(numeroTiersLie);
			if (tiersLie == null) {
				throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
			}

			TiersGeneralView tiersLieView = tiersGeneralManager.get(tiersLie);
			rapportView.setTiersLie(tiersLieView);
		}
		rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);

		return rapportView;
	}


	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 * @throws AdressesResolutionException
	 */
	public RapportView get (Long idRapport, SensRapportEntreTiers sensRapportEntreTiers) throws AdressesResolutionException {
		//édition d'un rapport
		RapportView rapportView =  new RapportView();

		RapportEntreTiers rapportEntreTiers	= rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.rapport.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		rapportView.setSensRapportEntreTiers(sensRapportEntreTiers);
		rapportView.setTypeRapportEntreTiers(TypeRapportEntreTiersWeb.fromCore(rapportEntreTiers.getType()));
		Long numero = null;
		if (sensRapportEntreTiers.equals(SensRapportEntreTiers.OBJET)) {
			numero = rapportEntreTiers.getSujet().getNumero();
		}
		if (sensRapportEntreTiers.equals(SensRapportEntreTiers.SUJET)) {
			numero = rapportEntreTiers.getObjet().getNumero();
		}
		rapportView.setNumero(numero);
		final Tiers tiers = getTiersService().getTiers(numero);
		//récupération nomCourrier1 et nomCourrier2
		List<String> nomCourrier = getAdresseService().getNomCourrier(tiers, null);
		rapportView.setNomCourrier(nomCourrier);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		//vérification droit édition du rapport pour fermeture
		rapportView.setAllowed(true);
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			if(!SecurityProvider.isGranted(Role.RT)){
				rapportView.setAllowed(false);
			}
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
			rapportView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}
		else if(rapportEntreTiers.getType().equals(TypeRapportEntreTiers.APPARTENANCE_MENAGE)){
			rapportView.setAllowed(false);
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
			final Boolean b = repres.getExtensionExecutionForcee();
			rapportView.setExtensionExecutionForcee(b == null ? false : b.booleanValue());
			rapportView.setAllowed(checkDroitEdit(tiers));
		}
		else {//rapport de non travail
			rapportView.setAllowed(checkDroitEdit(tiers));
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
			if (sens.equals(SensRapportEntreTiers.OBJET)) {
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
				repres.setExtensionExecutionForcee(rapportView.isExtensionExecutionForcee());
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
				repres.setExtensionExecutionForcee(rapportView.isExtensionExecutionForcee());
			}
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
		rapport.setAnnule(new Boolean(true));
	}


	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView getView(Long numero) throws AdressesResolutionException, InfrastructureException{
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
			TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
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
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @param webParamPagination
	 * @return un objet TiersView
	 * @throws AdressesResolutionException
	 * @throws InfrastructureException
	 */
	public TiersEditView getRapportsPrestationView(Long numero, WebParamPagination webParamPagination) throws AdressesResolutionException, InfrastructureException{
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
			TiersGeneralView tiersGeneralView = tiersGeneralManager.get(tiers);
			tiersEditView.setTiersGeneral(tiersGeneralView);
			if (tiers instanceof DebiteurPrestationImposable ) {
				DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
				tiersEditView.setRapportsPrestation(getRapportsPrestation(dpi, webParamPagination));
				setContribuablesAssocies(tiersEditView, dpi);
				if (dpi.getContribuable() == null) {
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
		}
		else {
			tiersEditView.setAllowed(true);
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
