package ch.vd.unireg.rapport.manager;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.adresse.AdresseException;
import ch.vd.unireg.adresse.AdressesResolutionException;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.common.pagination.WebParamPagination;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesEtSNCException;
import ch.vd.unireg.declaration.snc.liens.associes.LienAssociesSNCService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalService;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.rapport.SensRapportEntreTiers;
import ch.vd.unireg.rapport.TypeRapportEntreTiersWeb;
import ch.vd.unireg.rapport.view.RapportView;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Heritage;
import ch.vd.unireg.tiers.LienAssociesEtSNC;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.RepresentationConventionnelle;
import ch.vd.unireg.tiers.RepresentationLegale;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersWebHelper;
import ch.vd.unireg.tiers.manager.TiersManager;
import ch.vd.unireg.tiers.view.TiersEditView;
import ch.vd.unireg.type.TypeRapportEntreTiers;
import ch.vd.unireg.utils.WebContextUtils;


/**
 * Claase offrant les services au controller RapportController
 *
 * @author xcifde
 */
public class RapportEditManagerImpl extends TiersManager implements RapportEditManager {

	private EvenementFiscalService evenementFiscalService;
	private LienAssociesSNCService lienAssociesSNCService;

	/**
	 * Alimente la vue RapportView
	 */
	@Override
	@Transactional(readOnly = true)
	public RapportView get(Long numeroTiers, Long numeroTiersLie) throws AdressesResolutionException {
		//création d'un rapport autre que travail
		Tiers tiers = tiersService.getTiers(numeroTiers);

		if (tiers == null) {
			throw new TiersNotFoundException(numeroTiers);
		}

		RapportView rapportView = new RapportView();
		//vérification des droits
		rapportView.setAllowed(checkDroitEdit(tiers));
		if (rapportView.isAllowed()) {
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

		if (tiers instanceof Entreprise && ((Entreprise) tiers).isSNC()) {
			rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.SUJET);
		}
		else {
			rapportView.setSensRapportEntreTiers(SensRapportEntreTiers.OBJET);
		}

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
		if (tiersLie == null) {
			throw new IllegalArgumentException();
		}

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
			if (!SecurityHelper.isAnyGranted(securityProvider, Role.RT, Role.CREATE_MODIF_DPI)) {
				allowed = false;
			}
		}
		else if (rapportEntreTiers.getType() == TypeRapportEntreTiers.APPARTENANCE_MENAGE) {
			allowed = false;
		}
		else if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final Tiers tiersCourant = tiersService.getTiers(numeroTiersCourant); // le tiers par lequel on est arrivé sur le rapport
			if (tiersCourant == null) {
				throw new IllegalArgumentException();
			}
			allowed = checkDroitEdit(tiersCourant); // [UNIREG-2814]
		}
		else if (rapportEntreTiers instanceof LienAssociesEtSNC) {
			if (!SecurityHelper.isGranted(securityProvider, Role.GEST_SNC)) {
				allowed = false;
			}
		}
		else {//rapport de non travail
			final Tiers tiersLie = tiersService.getTiers(numeroTiersLie); // l'autre tiers du rapport (pas celui par lequel on est arrivé sur le rapport)
			if (tiersLie == null) {
				throw new IllegalArgumentException();
			}
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
		Tiers sujet; // sujet : pupille, curatelle, personne sous conseil légal ou substitué pour l'assujettissement
		Tiers objet; // objet : tuteur, curateur, conseil légal ou substituant pour l'assujettissement

		// récupère les données
		final SensRapportEntreTiers sens = rapportView.getSensRapportEntreTiers();
		if (sens == SensRapportEntreTiers.OBJET) {
			sujet = tiersService.getTiers(rapportView.getTiers().getNumero());
			objet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
		}
		else {
			if (SensRapportEntreTiers.SUJET != sens) {
				throw new IllegalArgumentException();
			}
			sujet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
			objet = tiersService.getTiers(rapportView.getTiers().getNumero());
		}
		if (type == null) {
			throw new IllegalArgumentException();
		}

		// instancie le bon rapport
		RapportEntreTiers rapport = type.newInstance();
		rapport.setDateDebut(rapportView.getDateDebut());

		if (rapport instanceof LienAssociesEtSNC) {

			if (sens == SensRapportEntreTiers.SUJET) {
				sujet = tiersService.getTiers(rapportView.getTiers().getNumero());
				objet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
			}
			else {
				objet = tiersService.getTiers(rapportView.getTiers().getNumero());
				sujet = tiersService.getTiers(rapportView.getTiersLie().getNumero());
			}

			try {
				lienAssociesSNCService.isAllowed((Contribuable) sujet, (Contribuable) objet, rapportView.getDateDebut());
			}
			catch (LienAssociesEtSNCException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}

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
			final Heritage heritage = (Heritage) rapport;

			final List<Heritage> principaux = objet.getRapportsObjet().stream()
					.filter(AnnulableHelper::nonAnnule)
					.filter(Heritage.class::isInstance)
					.map(Heritage.class::cast)
					.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())
					.collect(Collectors.toList());
			final boolean principalDefini = DateRangeHelper.intersect(heritage, principaux);

			// [SIFISC-24999] on défigne automatiquement le tiers comme principal s'il n'y a pas déjà un principal défini,
			// autrement le validateur ne laissera pas passer l'ajout de l'héritage (et l'utilisateur pourra toujours
			// changer le principal plus tard)
			heritage.setPrincipalCommunaute(!principalDefini);

			// on envoie les événements fiscaux de modification des communautés RF impactées
			final Set<CommunauteRF> communautes = Heritage.findCommunautesRF((PersonnePhysique) objet);
			communautes.forEach(communaute -> evenementFiscalService.publierModificationHeritageCommunaute(heritage.getDateDebut(), communaute));
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

		if (rapportEntreTiers instanceof RepresentationConventionnelle) {
			final RepresentationConventionnelle repres = (RepresentationConventionnelle) rapportEntreTiers;
			repres.setExtensionExecutionForcee(rapportView.getExtensionExecutionForcee());
			final Tiers sujet = tiersDAO.get(repres.getSujetId());
			validateExecutionForcee(repres, sujet);
		}
		else if (rapportEntreTiers instanceof Heritage) {
			// on envoie les événements fiscaux de modification des communautés RF impactées
			final Tiers objet = tiersDAO.get(rapportEntreTiers.getObjetId());
			final Set<CommunauteRF> communautes = Heritage.findCommunautesRF((PersonnePhysique) objet);
			communautes.forEach(communaute -> evenementFiscalService.publierModificationHeritageCommunaute(rapportEntreTiers.getDateDebut(), communaute));
		}
	}

	private CollectiviteAdministrative findAutorite(Long autoriteTutelaireId) {
		if (autoriteTutelaireId != null) {
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

		if (rapport instanceof Heritage) {
			// on envoie les événements fiscaux de modification des communautés RF impactées
			final PersonnePhysique defunt = (PersonnePhysique) tiersDAO.get(rapport.getObjetId());
			final Set<CommunauteRF> communautes = Heritage.findCommunautesRF(defunt);
			communautes.forEach(communaute -> evenementFiscalService.publierModificationHeritageCommunaute(rapport.getDateDebut(), communaute));
		}
	}

	@Override
	public void annulerRapportPrestation(long rapportId) {

		final RapportEntreTiers rapport = rapportEntreTiersDAO.get(rapportId);
		if (rapport == null) {
			throw new ObjectNotFoundException("Le rapport avec l'id = " + rapportId + "n'existe pas");
		}
		if (!(rapport instanceof RapportPrestationImposable)) {
			throw new IllegalArgumentException("Le rapport avec l'id = " + rapportId + "n'est pas un rapport de prestations imposables");
		}

		rapport.setAnnule(true);
	}

	@Override
	public long getDebiteurId(long rapportId) {

		final RapportEntreTiers rapport = rapportEntreTiersDAO.get(rapportId);
		if (rapport == null) {
			throw new ObjectNotFoundException("Le rapport avec l'id = " + rapportId + "n'existe pas");
		}
		if (!(rapport instanceof RapportPrestationImposable)) {
			throw new IllegalArgumentException("Le rapport avec l'id = " + rapportId + "n'est pas un rapport de prestations imposables");
		}
		return rapport.getObjetId();
	}

	/**
	 * Charge les informations dans TiersView
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException {
		TiersEditView tiersEditView = new TiersEditView();
		if (numero == null) {
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
		if (numero == null) {
			return null;
		}

		final Tiers tiers = getTiersDAO().get(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		tiersEditView.setTiers(tiers);
		TiersGeneralView tiersGeneralView = tiersGeneralManager.getTiers(tiers, true);
		tiersEditView.setTiersGeneral(tiersGeneralView);
		if (tiers instanceof DebiteurPrestationImposable) {
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

	/**
	 * Enregistre l'héritier spécifié comme héritier principal de la communauté d'héritier du défunt spécifié.
	 *
	 * @param defuntId   l'id du défunt
	 * @param heritierId l'id de l'héritier qui doit devenir principal
	 * @param dateDebut  la date de début de validité
	 */
	@Override
	public void setPrincipal(long defuntId, long heritierId, @NotNull RegDate dateDebut) {

		final Tiers defunt = tiersDAO.get(defuntId);
		if (defunt == null) {
			throw new TiersNotFoundException(defuntId);
		}
		final Tiers newPrincipal = tiersDAO.get(heritierId);
		if (newPrincipal == null) {
			throw new TiersNotFoundException(heritierId);
		}

		// on recherche le lien d'héritage 'principal' courant
		final Heritage currentHeritagePrincipal = defunt.getRapportsObjet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance)
				.map(Heritage.class::cast)
				.filter(h -> h.getPrincipalCommunaute() != null && h.getPrincipalCommunaute())
				.max(Comparator.comparing(Heritage::getDateDebut, RegDate::compareTo))
				.orElseThrow(() -> new IllegalArgumentException("Il devrait toujours y avoir un lien d'héritage principal"));
		if (dateDebut.isBefore(currentHeritagePrincipal.getDateDebut())) {
			throw new IllegalArgumentException("Il n'est pas possible d'intercaler un principal entre deux périodes existantes");
		}

		final Tiers oldPrincipal = tiersDAO.get(currentHeritagePrincipal.getSujetId());
		if (oldPrincipal == null) {
			throw new TiersNotFoundException(currentHeritagePrincipal.getSujetId());
		}

		// on recherche le lien d'héritage courant de l'héritier à désigner comme principal
		final Heritage currentHeritageHeritier = defunt.getRapportsObjet().stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(Heritage.class::isInstance)
				.map(Heritage.class::cast)
				.filter(h -> h.getSujetId().equals(heritierId))
				.max(Comparator.comparing(Heritage::getDateDebut, RegDate::compareTo))
				.orElseThrow(() -> new IllegalArgumentException("Il devrait toujours y avoir un lien d'héritage pour l'héritier spécifié."));
		if (dateDebut.isBefore(currentHeritageHeritier.getDateDebut())) {
			throw new IllegalArgumentException("Il n'est pas possible de choisir un principal avant la date de la dernière période");
		}

		// on enlève le flag principal sur le rapport d'héritage qui l'a actuellement
		if (dateDebut == currentHeritagePrincipal.getDateDebut()) {
			// la date de début correspond à la date de début du rapport d'héritage 'principal' existant :
			// on annule le rapport existant et on le recrée sans le flag 'principal'
			final Heritage clone = currentHeritagePrincipal.duplicate();
			clone.setPrincipalCommunaute(false);
			tiersService.addRapport(clone, oldPrincipal, defunt);
			currentHeritagePrincipal.setAnnule(true);
		}
		else {
			// la date de début est postérieur à la date de début du rapport d'héritage existant :
			// on ferme le rapport existant et on en crée une copie sans le flag 'principal' qui débute à la date qui va bien.
			final Heritage clone = currentHeritagePrincipal.duplicate();
			clone.setDateDebut(dateDebut);
			clone.setPrincipalCommunaute(false);
			tiersService.addRapport(clone, oldPrincipal, defunt);
			currentHeritagePrincipal.setDateFin(dateDebut.getOneDayBefore());
		}

		// on ajoute le flag principal sur le rapport d'héritage de l'héritier désigné
		if (dateDebut == currentHeritageHeritier.getDateDebut()) {
			// la date de début correspond à la date de début du rapport d'héritage existant de l'héritier :
			// on annule le rapport existant et on le recrée avec le flag 'principal'
			final Heritage clone = currentHeritageHeritier.duplicate();
			clone.setPrincipalCommunaute(true);
			tiersService.addRapport(clone, newPrincipal, defunt);
			currentHeritageHeritier.setAnnule(true);
		}
		else {
			// la date de début est postérieur à la date de début du rapport d'héritage existant de l'héritier :
			// on ferme le rapport existant et on en crée une copie avec le flag 'principal' qui débute à la date qui va bien.
			final Heritage clone = currentHeritageHeritier.duplicate();
			clone.setDateDebut(dateDebut);
			clone.setPrincipalCommunaute(true);
			tiersService.addRapport(clone, newPrincipal, defunt);
			currentHeritageHeritier.setDateFin(dateDebut.getOneDayBefore());
		}

		// on envoie les événements fiscaux de modification des communautés RF impactées
		final Set<CommunauteRF> communautes = Heritage.findCommunautesRF((PersonnePhysique) defunt);
		communautes.forEach(communaute -> evenementFiscalService.publierModificationPrincipalCommunaute(dateDebut, communaute));
	}

	public void setEvenementFiscalService(EvenementFiscalService evenementFiscalService) {
		this.evenementFiscalService = evenementFiscalService;
	}

	public void setLienAssociesSNCService(LienAssociesSNCService lienAssociesSNCService) {
		this.lienAssociesSNCService = lienAssociesSNCService;
	}
}
