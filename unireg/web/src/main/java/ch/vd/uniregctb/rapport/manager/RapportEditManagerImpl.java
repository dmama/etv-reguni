package ch.vd.uniregctb.rapport.manager;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.common.pagination.WebParamPagination;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rapport.SensRapportEntreTiers;
import ch.vd.uniregctb.rapport.TypeRapportEntreTiersWeb;
import ch.vd.uniregctb.rapport.view.RapportView;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Heritage;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.RepresentationConventionnelle;
import ch.vd.uniregctb.tiers.RepresentationLegale;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersWebHelper;
import ch.vd.uniregctb.tiers.manager.TiersManager;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.utils.WebContextUtils;


/**
 * Claase offrant les services au controller RapportController
 *
 * @author xcifde
 *
 */
public class RapportEditManagerImpl extends TiersManager implements RapportEditManager {

	/**
	 * Alimente la vue RapportView
	 */
	@Override
	@Transactional(readOnly = true)
	public RapportView get(Long numeroTiers, Long numeroTiersLie) throws AdressesResolutionException{
		//création d'un rapport autre que travail
		Tiers tiers = tiersService.getTiers(numeroTiers);

		if (tiers == null) {
			throw new TiersNotFoundException(numeroTiers);
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
				throw new TiersNotFoundException(numeroTiersLie);
			}

			TiersGeneralView tiersLieView = tiersGeneralManager.getTiers(tiersLie, true);
			rapportView.setTiersLie(tiersLieView);
		}
		rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);

		return rapportView;
	}

	@Override
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
			throw new IllegalArgumentException("Sens de rapport-entre-tiers inconnu =[" + editingFrom + ']');
		}

		// on récupère les tiers eux-mêmes, et quelques infos supplémentaires
		final Tiers tiersLie = tiersService.getTiers(numeroTiersLie); // l'autre tiers du rapport (pas celui par lequel on est arrivé sur le rapport)
		Assert.notNull(tiersLie);

		final List<String> nomTiersLie = adresseService.getNomCourrier(tiersLie, null, false);
		final String toolTipMessage = TiersWebHelper.getRapportEntreTiersTooltips(rapportEntreTiers, adresseService, tiersService);

		rapportView.setNumeroCourant(numeroTiersCourant);
		rapportView.setNumero(numeroTiersLie);
		rapportView.setNomCourrier(nomTiersLie);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		rapportView.setToolTipMessage(toolTipMessage);

		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
			final Boolean b = repres.getExtensionExecutionForcee();
			rapportView.setExtensionExecutionForcee(b != null && b);
			final boolean isHorsSuisse = isHorsSuisse(rapportEntreTiers.getSujetId(), rapportEntreTiers);
			rapportView.setExtensionExecutionForceeAllowed(isHorsSuisse); // [UNIREG-2655]
		}
		else if (rapportEntreTiers instanceof Heritage) {
			rapportView.setPrincipalCommunaute(((Heritage) rapportEntreTiers).getPrincipalCommunaute());
		}

		//vérification droit édition du rapport pour fermeture
		rapportView.setAllowed(isEditionAllowed(idRapport, editingFrom));

		return rapportView;
	}

	@Override
	public boolean isEditionAllowed(long idRapport, @NotNull SensRapportEntreTiers sens) {

		final RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.rapport.inexistant", null, WebContextUtils.getDefaultLocale()));
		}

		final Long numeroTiersCourant;
		final Long numeroTiersLie;
		switch (sens) {
		case OBJET:
			numeroTiersCourant = rapportEntreTiers.getObjetId();
			numeroTiersLie = rapportEntreTiers.getSujetId();
			break;
		case SUJET:
			numeroTiersCourant = rapportEntreTiers.getSujetId();
			numeroTiersLie = rapportEntreTiers.getObjetId();
			break;
		default:
			throw new IllegalArgumentException("Sens de rapport-entre-tiers inconnu =[" + sens + ']');
		}

		boolean allowed = true;

		//vérification droit édition du rapport pour fermeture
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			if (!SecurityHelper.isGranted(securityProvider, Role.RT)) {
				allowed = false;
			}
		}
		else if (rapportEntreTiers.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
			allowed = false;
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final Tiers tiersCourant = tiersService.getTiers(numeroTiersCourant); // le tiers par lequel on est arrivé sur le rapport
			Assert.notNull(tiersCourant);
			allowed = checkDroitEdit(tiersCourant); // [UNIREG-2814]
		}
		else {//rapport de non travail
			final Tiers tiersLie = tiersService.getTiers(numeroTiersLie); // l'autre tiers du rapport (pas celui par lequel on est arrivé sur le rapport)
			Assert.notNull(tiersLie);
			allowed = checkDroitEdit(tiersLie);
		}

		return allowed;
	}

	/**
	 * Persiste le rapport entre tiers
	 */
	@Override
	public void add(@NotNull RapportView rapportView) {

		if (rapportView.getId() != null) {
			throw new IllegalArgumentException("Le rapport est déjà persisté.");
		}

		final TypeRapportEntreTiers type = rapportView.getTypeRapportEntreTiers().toCore();
		final Tiers sujet; // sujet : pupille, curatelle, personne sous conseil légal ou substitué pour l'assujettissement
		final Tiers objet; // objet : tuteur, curateur, conseil légal ou substituant pour l'assujettissement

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
		rapport.setDateDebut(rapportView.getDateDebut());

		// [UNIREG-755] tenir compte de l'extension de l'exécution à la création du rapport
		if (rapport instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapport;
			repres.setExtensionExecutionForcee(rapportView.getExtensionExecutionForcee());
			validateExecutionForcee(repres, sujet);
		}

		if (rapport instanceof RepresentationLegale) {
			final RepresentationLegale representation = (RepresentationLegale) rapport;
			final Long autoriteId = rapportView.getAutoriteTutelaireId();
			if (autoriteId != null) {
				CollectiviteAdministrative autorite = findAutorite(autoriteId);
				representation.setAutoriteTutelaire(autorite);
			}

		}

		if (rapport instanceof Heritage) {
			final Heritage heritage =(Heritage) rapport;
			heritage.setPrincipalCommunaute(rapportView.getPrincipalCommunaute());
		}

		// établit le rapport entre les deux tiers
		tiersService.addRapport(rapport, sujet, objet);
	}

	@Override
	public void update(@NotNull RapportView rapportView) {

		if (rapportView.getId() == null) {
			throw new IllegalArgumentException("Le rapport n'est pas déjà persisté.");
		}

		// mise-à-jour du rapport
		RapportEntreTiers rapportEntreTiers = rapportEntreTiersDAO.get(rapportView.getId());
		rapportEntreTiers.setDateFin(rapportView.getDateFin());
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
			repres.setExtensionExecutionForcee(rapportView.getExtensionExecutionForcee());
			final Tiers sujet = tiersDAO.get(repres.getSujetId());
			validateExecutionForcee(repres, sujet);
		}
	}

	private CollectiviteAdministrative findAutorite(Long autoriteTutelaireId) {
		if(autoriteTutelaireId !=null){
			return tiersDAO.getCollectiviteAdministrativesByNumeroTechnique(autoriteTutelaireId.intValue());
		}
		return null;
	}

	private void validateExecutionForcee(RepresentationConventionnelle rapport, Tiers sujet) {
		if (rapport.getExtensionExecutionForcee() && !isHorsSuisse(sujet.getId(), rapport)) {
			// [UNIREG-1341/UNIREG-2655]
			throw new ValidationException(sujet, "L'extension de l'exécution forcée est uniquement autorisée pour les tiers avec un for fiscal principal hors-Suisse");
		}
	}

	/**
	 * Annule le rapport de prestation
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerRapport(Long idRapport) {
		RapportEntreTiers rapport = rapportEntreTiersDAO.get(idRapport);
		rapport.setAnnule(true);
	}


	/**
	 * Charge les informations dans TiersView
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		tiersEditView.setTiers(tiers);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersEditView.setTiersGeneral(tiersGeneralView);
		if (!(tiers instanceof CollectiviteAdministrative)) { // [SIFISC-2561] il n'est pas permis d'éditer les rapports des collectivités, inutiles donc les charger
			tiersEditView.setDossiersApparentes(getRapports(tiers));
		}
		if (tiers instanceof Entreprise || tiers instanceof PersonnePhysique) {
			tiersEditView.setRapportsEtablissements(getRapportsEtablissements(tiers));
		}
		if (tiers instanceof Contribuable) {
			Contribuable contribuable = (Contribuable) tiers;
			tiersEditView.setDebiteurs(getDebiteurs(contribuable));
		}

		return tiersEditView;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getRapportsPrestationView(Long numero, WebParamPagination webParamPagination, boolean rapportsPrestationHisto) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if ( numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		tiersEditView.setTiers(tiers);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersEditView.setTiersGeneral(tiersGeneralView);
		if (tiers instanceof DebiteurPrestationImposable ) {
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			tiersEditView.setRapportsPrestation(getRapportsPrestation(dpi, webParamPagination, rapportsPrestationHisto));
			setContribuablesAssocies(tiersEditView, dpi, false);
			if (dpi.getContribuableId() == null) {
				tiersEditView.setAddContactISAllowed(true);
			}
			else {
				tiersEditView.setAddContactISAllowed(false);
			}
		}

		return tiersEditView;
	}
}
