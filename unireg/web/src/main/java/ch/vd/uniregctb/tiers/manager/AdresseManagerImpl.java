package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.interfaces.civil.data.Adresse;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.TypeAdresseRepresentant;
import ch.vd.uniregctb.common.ActionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.security.AccessDeniedException;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityHelper;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.view.AdresseDisponibleView;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.EtatSuccessoralView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Service offrant les methodes permettant de gerer le controller TiersAdresseController
 *
 * @author xcifde
 *
 */
public class AdresseManagerImpl extends TiersManager implements AdresseManager {

	protected final Logger LOGGER = Logger.getLogger(AdresseManagerImpl.class);

	private final static String TYPE_LOCALITE_SUISSE = "suisse";

	private final static String TYPE_LOCALITE_PAYS = "pays";

	/**
	 * Alimente la vue AdresseView pour une adresse existante
	 *
	 * @param id l'id de l'adresse tiers existante
	 * @return la vue de l'adresse
	 */
	@Override
	@Transactional(readOnly = true)
	public AdresseView getAdresseView(Long id) {

		final AdresseTiers adresseTiers = getAdresseTiersDAO().get(id);
		if (adresseTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.adresse.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		final AdresseView view = enrichiAdresseView(adresseTiers);
		final Tiers tiers = tiersService.getTiers(adresseTiers.getTiers().getNumero());
		final List<AdresseDisponibleView> lAdresse = getAdressesDisponible(tiers);
		final EtatSuccessoralView etatSuccessoral = EtatSuccessoralView.determine(tiers, tiersService);

		view.setNature(tiers.getNatureTiers());
		view.setAdresseDisponibles(lAdresse);
		view.setNumCTB(adresseTiers.getTiers().getNumero());
		view.setEtatSuccessoral(etatSuccessoral);
		view.setMettreAJourDecedes(etatSuccessoral != null);
		return view;
	}

	@Override
	public AdresseView getAdresseView(TiersEditView tiers, Long numero) {
		List<AdresseView> adresses = tiers.getHistoriqueAdresses();
		for (AdresseView adresseView : adresses) {
			if(numero.equals(adresseView.getId())){
				return adresseView;
			}
		}
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * Cree une vue pour une nouvelle adresse
	 *
	 * @param numeroCtb le numéro de contribuable sur lequel la nouvelle adresse sera créée
	 * @return la vue de l'adresse à créer
	 */
	@Override
	@Transactional(readOnly = true)
	public AdresseView create(Long numeroCtb) {

		final Tiers tiers = tiersService.getTiers(numeroCtb);
		if (tiers == null) {
			throw new TiersNotFoundException(numeroCtb);
		}
		
		final List<AdresseDisponibleView> lAdresse = getAdressesDisponible(tiers);
		final EtatSuccessoralView etatSuccessoral = EtatSuccessoralView.determine(tiers, tiersService);

		final AdresseView view = new AdresseView();
		view.setNumCTB(numeroCtb);
		view.setTypeLocalite(TYPE_LOCALITE_SUISSE);
		view.setAdresseDisponibles(lAdresse);
		view.setEtatSuccessoral(etatSuccessoral);
		view.setMettreAJourDecedes(etatSuccessoral != null);

		return view;
	}

	/**
	 * Sauvegarde de l'adresse en base de donnees
	 *
	 * @param adresseView la vue web de l'adresse à sauver
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void save(AdresseView adresseView) throws AccessDeniedException {
		final Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());
		if (adresseView.getId() == null) {
			addNewAdresse(adresseView, tiers);
		}
		else {
			updateAdresse(adresseView);
		}

		// [SIFISC-156] Mis-à-jour des adresses successorales si demandé
		if (adresseView.isMettreAJourDecedes() && tiers instanceof MenageCommun) {
			updateAdressesSuccessorales(adresseView, (MenageCommun) tiers);
		}
	}

	private void addNewAdresse(AdresseView adresseView, Tiers tiers) {
		final AdresseTiers adresseTiers = createAdresseTiers(adresseView);
		if (adresseTiers != null) {
			adresseService.addAdresse(tiers, adresseTiers);
		}
	}

	private void updateAdressesSuccessorales(AdresseView adresseView, MenageCommun tiers) throws AccessDeniedException {
		if (!SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD)) {
			throw new AccessDeniedException("Vous ne possédez pas les droits IfoSec pour modifier l'adresse d'un décédé.");
		}

		// On vérifie la cohérence de l'état successoral entre le formulaire et la base
		final EtatSuccessoralView etat = adresseView.getEtatSuccessoral();
		final EtatSuccessoralView etatDb = EtatSuccessoralView.determine(tiers, tiersService);
		if (etat == null || !etat.equals(etatDb)) {
			throw new ActionException("L'état successoral n'est pas cohérent. Un autre utilisateur a peut-être effectué des modifications entre-temps. Veuillez recommencer l'opération.");
		}

		// On peut maintenant mettre-à-jour les adresses des décédés
		if (etat.getNumeroPrincipalDecede() != null) {
			PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(etat.getNumeroPrincipalDecede());
			addNewAdresse(adresseView, principal);
		}
		if (etat.getNumeroConjointDecede() != null) {
			PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.get(etat.getNumeroConjointDecede());
			addNewAdresse(adresseView, conjoint);
		}
	}

	/**
	 * Met à jour en base de données une adresse
	 *
	 * @param adresseView la vue web de l'adresse à sauver
	 */
	private void updateAdresse(AdresseView adresseView) {
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(adresseView.getId());

		if (TYPE_LOCALITE_SUISSE.equals(adresseView.getTypeLocalite())) {
			updateAdresseSuisse(adresseView, (AdresseSuisse) adresseTiers);
		}

		if (TYPE_LOCALITE_PAYS.equals(adresseView.getTypeLocalite())) {
			updateAdresseEtrangere(adresseView, (AdresseEtrangere) adresseTiers);
		}

	}

	/**
	 * Sauvegarde d'une reprise d'adresse
	 *
	 * @param adresseView la vue web de l'adresse à sauver
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void saveReprise(AdresseView adresseView) {

		Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());

		Integer index = Integer.valueOf(adresseView.getIndex());
		AdresseDisponibleView addDisponibleView = adresseView.getAdresseDisponibles().get(index);

		if (adresseView.getId() == null) {
			// Ajout d'une adresse
			addAdresseReprise(adresseView, addDisponibleView, tiers);
		}
		else {
			// Mise-à-jour d'une adresse
			updateAdresseReprise(adresseView, addDisponibleView, tiers);
		}

		getTiersDAO().save(tiers);
	}

	private void updateAdresseReprise(AdresseView adresseView, AdresseDisponibleView addDisponibleView, Tiers tiers) {

		AdresseTiers existante = getAdresseTiersDAO().get(adresseView.getId());

		if ("reprise".equals(adresseView.getMode())) {

			if (existante instanceof AdresseAutreTiers) {
				AdresseAutreTiers a = (AdresseAutreTiers) existante;
				a.setType(TypeAdresseTiers.REPRESENTATION);
			}
			else {
				/* reprise d'une adresse d'un autre type -> on annule l'adresse précédente et on en crée une nouvelle */
				existante.setAnnule(true);
				addAdresseReprise(adresseView, addDisponibleView, tiers);
			}
		}
		else if ("repriseCivil".equals(adresseView.getMode())) {

			if (existante instanceof AdresseCivile) {
				AdresseCivile a = (AdresseCivile) existante;
				a.setType(addDisponibleView.getTypeAdresse());
			}
			else {
				/* reprise d'une adresse d'un autre type -> on annule l'adresse précédente et on en crée une nouvelle */
				existante.setAnnule(true);
				addAdresseReprise(adresseView, addDisponibleView, tiers);
			}
		}
	}

	private void addAdresseReprise(AdresseView adresseView, AdresseDisponibleView addDisponibleView, Tiers tiers) {

		if ("reprise".equals(adresseView.getMode())) {

			AdresseAutreTiers addAutreTiers = new AdresseAutreTiers();
			addAutreTiers.setDateDebut(adresseView.getRegDateDebut());
			addAutreTiers.setUsage(adresseView.getUsage());
			addAutreTiers.setType(TypeAdresseTiers.REPRESENTATION);

			// Recuperer Tiers Representant

			Tiers representant = tiersService.getTiers(addDisponibleView.getRepresentantId());
			addAutreTiers.setAutreTiersId(representant.getId());
			adresseService.addAdresse(tiers, addAutreTiers);

		}
		else if ("repriseCivil".equals(adresseView.getMode())) {

			AdresseCivile addCivil = new AdresseCivile();
			addCivil.setDateDebut(adresseView.getRegDateDebut());
			addCivil.setUsage(adresseView.getUsage());
			addCivil.setType(addDisponibleView.getTypeAdresse());

			adresseService.addAdresse(tiers, addCivil);
		}
	}

	/**
	 * Crée une adresse tiers à partir de la vue web d'une adresse.
	 *
	 * @param adresseView la vue web d'une adresse
	 * @return une adresse tiers; ou <b>null</b> si la vue spécifiée n'est pas suffisemment renseignée.
	 */
	private AdresseTiers createAdresseTiers(AdresseView adresseView) {

		AdresseTiers adresseTiers = null;

		if (TYPE_LOCALITE_SUISSE.equals(adresseView.getTypeLocalite())) {
			AdresseSuisse adresse = new AdresseSuisse();
			updateAdresseSuisse(adresseView, adresse);
			adresseTiers = adresse;
		}

		if (TYPE_LOCALITE_PAYS.equals(adresseView.getTypeLocalite())) {
			AdresseEtrangere adresseEtrangere = new AdresseEtrangere();
			updateAdresseEtrangere(adresseView, adresseEtrangere);
			adresseTiers = adresseEtrangere;
		}

		return adresseTiers;
	}

	/**
	 * Met-à-jour une adresse étrangère en fonction d'une vue web d'une adresse.
	 *
	 * @param source la vue web d'une adresse
	 * @param target une adresse fiscale étrangère
	 */
	private void updateAdresseEtrangere(AdresseView source, AdresseEtrangere target) {
		if (target.getId() == null) {
			target.setDateDebut(source.getRegDateDebut());
		}
		target.setUsage(source.getUsage());
		target.setNumeroMaison(source.getNumeroMaison());
		target.setNumeroOfsPays(source.getPaysOFS());
		target.setComplement(source.getComplements());
		target.setPermanente(source.isPermanente());
		target.setNumeroAppartement(source.getNumeroAppartement());
		target.setNumeroCasePostale(source.getNumeroCasePostale());
		target.setRue(source.getRue());
		target.setTexteCasePostale(source.getTexteCasePostale());
		target.setNumeroPostalLocalite(source.getLocaliteNpa());
		target.setComplement(source.getComplements());
		target.setComplementLocalite(source.getComplementLocalite());
	}

	/**
	 * Met-à-jour une adresse suisse en fonction d'une vue web d'une adresse.
	 *
	 * @param source la vue web d'une adresse
	 * @param target une adresse fiscale suisse
	 */
	private void updateAdresseSuisse(AdresseView source, AdresseSuisse target) {

		if (target.getId() == null) {
			target.setDateDebut(source.getRegDateDebut());
		}
		target.setUsage(source.getUsage());
		target.setNumeroMaison(source.getNumeroMaison());
		target.setNumeroRue(source.getNumeroRue());
		Integer numeroOrdrePoste = null;
		if (!StringUtils.isEmpty(source.getNumeroOrdrePoste())) {
			numeroOrdrePoste = new Integer(source.getNumeroOrdrePoste());
		}
		target.setNumeroOrdrePoste(numeroOrdrePoste);
		target.setComplement(source.getComplements());
		target.setPermanente(source.isPermanente());
		target.setNumeroAppartement(source.getNumeroAppartement());
		target.setNumeroCasePostale(source.getNumeroCasePostale());
		target.setRue(source.getRue());
		target.setTexteCasePostale(source.getTexteCasePostale());
		target.setNpaCasePostale(source.getNpaCasePostale());
	}

	/**
	 * Alimente AdresseView de core en fonction d'adresse Tiers
	 *
	 */
	private AdresseView enrichiAdresseView(AdresseTiers adresse) {

		AdresseView adresseView = new AdresseView();
		adresseView.setNumCTB(adresse.getTiers().getNumero());
		adresseView.setId(adresse.getId());
		adresseView.setAnnule(adresse.isAnnule());
		if (adresse instanceof AdresseSupplementaire) {

			// TODO (FDE) supprimer tout ce code copié-collé et utiliser un AdresseSupplementaireAdapter (voir aussi createAdresseView sur TiersEditManagerImpl)
			AdresseSupplementaire adresseSupp = (AdresseSupplementaire) adresse;

			adresseView.setDateDebut(adresseSupp.getDateDebut());
			adresseView.setUsage(adresseSupp.getUsage());
			adresseView.setNumeroMaison(adresseSupp.getNumeroMaison());
			adresseView.setComplements(adresseSupp.getComplement());
			adresseView.setNumeroAppartement(adresseSupp.getNumeroAppartement());
			adresseView.setNumeroCasePostale(adresseSupp.getNumeroCasePostale());
			adresseView.setRue(adresseSupp.getRue());
			adresseView.setTexteCasePostale(adresseSupp.getTexteCasePostale());
			adresseView.setComplements(adresseSupp.getComplement());
			adresseView.setPermanente(adresseSupp.isPermanente());

			if (adresse instanceof AdresseSuisse) {

				AdresseSuisse adresseSuisse = (AdresseSuisse) adresse;

				adresseView.setTypeLocalite("suisse");
				Localite localite = getLocalite(adresseSuisse);
				if (localite != null) {
					adresseView.setLocaliteSuisse(localite.getNomAbregeMinuscule());
					adresseView.setNumeroOrdrePoste(localite.getNoOrdre().toString());
					adresseView.setNumCommune(localite.getNoCommune().toString());
				}
				adresseView.setNpaCasePostale(adresseSuisse.getNpaCasePostale());
			}
			else if (adresse instanceof AdresseEtrangere) {

				AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;

				adresseView.setTypeLocalite("pays");
				Pays pays = getPays(adresseEtrangere);
				adresseView.setPaysNpa(pays.getNomCourt());
				adresseView.setPaysOFS(pays.getNoOFS());
				adresseView.setLocaliteNpa(adresseEtrangere.getNumeroPostalLocalite());
				adresseView.setComplementLocalite(adresseEtrangere.getComplementLocalite());
			}
			else {
				throw new IllegalArgumentException("Type d'adresse inconnu = [" + adresse.getClass().getSimpleName() + ']');
			}
		}

		if (adresse instanceof AdresseCivile) {

			AdresseCivile adressecivil = (AdresseCivile) adresse;

			adresseView.setDateDebut(adressecivil.getDateDebut());
			adresseView.setUsage(adressecivil.getUsage());

		}

		if (adresse instanceof AdresseAutreTiers) {

			AdresseAutreTiers adresseAutreTiers = (AdresseAutreTiers) adresse;

			adresseView.setDateDebut(adresseAutreTiers.getDateDebut());
			adresseView.setUsage(adresseAutreTiers.getUsage());

		}

		return adresseView;
	}

	/**
	 * Annule une adresse
	 */
	@Override
	@Transactional(rollbackFor = Throwable.class)
	public void annulerAdresse(Long idAdresse) {
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(idAdresse);
		if (adresseTiers != null) {
			getAdresseService().annulerAdresse(adresseTiers);
		}
	}

	@Override
	public void fermerAdresse(AdresseView bean){
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(bean.getId());
		if (adresseTiers != null) {
			getAdresseService().fermerAdresse(adresseTiers,bean.getRegDateFin());
		}
	}

	/**
	 * Recupere la Localite
	 */
	private Localite getLocalite(AdresseSuisse adresse) {
		final Integer noLocalite = getNumeroOrdreLocalite(adresse);
		final Localite localite;
		localite = getServiceInfrastructureService().getLocaliteByONRP(noLocalite);
		return localite;
	}

	/**
	 * Recupere le numéro ordre poste
	 */
	private Integer getNumeroOrdreLocalite(AdresseSuisse adresse) {
		final Integer noLocalite;
		// On passe par le rue, si elle est spécifiée
		final Integer numeroRue = adresse.getNumeroRue();
		if (numeroRue != null) {
			final Rue rue;
			rue = getServiceInfrastructureService().getRueByNumero(numeroRue);
			noLocalite = rue.getNoLocalite();
		}
		else {
			noLocalite = adresse.getNumeroOrdrePoste();
		}
		return noLocalite;
	}

	/**
	 * Recupere le pays
	 */
	public Pays getPays(AdresseEtrangere adresseEtrangere) {

		Assert.notNull(adresseEtrangere);
		return getServiceInfrastructureService().getPays(adresseEtrangere.getNumeroOfsPays());

	}

	/**
	 * Recupere la liste des adresses disponibles
	 */
	private List<AdresseDisponibleView> getAdressesDisponible(Tiers tiers) {

		List<AdresseDisponibleView> adresses = new ArrayList<>();

		if ( tiers instanceof PersonnePhysique) {
			PersonnePhysique pp = (PersonnePhysique) tiers;
			if (pp.isHabitantVD()) {
				final AdressesCiviles adressesIndividu;
				try {
					adressesIndividu = getAdresseService().getAdressesCiviles(pp, null, false);
					if (adressesIndividu != null) {
						fillAdressesDisponibleViewFromAddIndividu(adresses, adressesIndividu);
					}
				}
				catch (AdresseException e) {
					// que faire ici ?
				}
			}
		}

		if (!(tiers instanceof MenageCommun)) { // [UNIREG-2645] un ménage commun ne peut pas être sous tutelle/curatelle/conseil légal

			final List<TypeAdresseRepresentant> typesRepresentation = Arrays.asList(TypeAdresseRepresentant.CONSEIL_LEGAL, TypeAdresseRepresentant.TUTELLE, TypeAdresseRepresentant.CURATELLE);
			for (TypeAdresseRepresentant type : typesRepresentation) {
				AdresseGenerique adresse;
				try {
					adresse = adresseService.getAdresseRepresentant(tiers, type, null, false);
				}
				catch (AdresseException e) {
					adresse = null;
				}

				if (adresse != null) {
					adresses.add(createAdresseDisponibleViewFromAddGenerique(adresse, tiers, type));
				}
			}
		}

		return adresses;
	}

	/**
	 * Remplit la collection des adressesView avec l'adresse fiscale du type spécifié.
	 */
	private void fillAdressesDisponibleViewFromAddIndividu(List<AdresseDisponibleView> adressesDisponibleView,
			final AdressesCiviles adressesIndividu) {

		if (adressesIndividu != null) {

			if (adressesIndividu.courrier != null) {

				AdresseDisponibleView addDispoView = createAdresseDisponibleViewFromAdresseCivil(adressesIndividu.courrier);
				addDispoView.setTypeAdresse(TypeAdresseCivil.COURRIER);
				adressesDisponibleView.add(addDispoView);
			}
			if (adressesIndividu.principale != null) {

				AdresseDisponibleView addDispoView = createAdresseDisponibleViewFromAdresseCivil(adressesIndividu.principale);
				addDispoView.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
				adressesDisponibleView.add(addDispoView);
			}
		}
	}

	/**
	 * Remplit AdresseDisponibleView en fonction de Adresse
	 */
	private AdresseDisponibleView createAdresseDisponibleViewFromAdresseCivil(Adresse addIndividu) {
		AdresseDisponibleView addDispoView = new AdresseDisponibleView();

		addDispoView.setSource(AdresseGenerique.SourceType.CIVILE);
		addDispoView.setLocalite(addIndividu.getLocalite());
		addDispoView.setNumeroCasePostale(addIndividu.getNumeroOrdrePostal());

		Integer noOfsPays = addIndividu.getNoOfsPays();
		if (noOfsPays != null) {
			Pays pays = getServiceInfrastructureService().getPays(noOfsPays);
			if (pays != null) {
				addDispoView.setPaysNpa(pays.getNomCourt());
			}
		}

		addDispoView.setRue(addIndividu.getRue());

		return addDispoView;
	}

	/**
	 * Crée et retourne la vue qui représente une adresse de reprise disponible.
	 *
	 * @param adresse l'adresse générique sur laquelle la vue sera construite
	 * @param tiers le tiers possédant l'adresse spécifiée
	 * @param type le type de 
	 * @return une nouvelle instance de type 'AdresseDisponibleView'
	 */
	private AdresseDisponibleView createAdresseDisponibleViewFromAddGenerique(AdresseGenerique adresse, Tiers tiers, TypeAdresseRepresentant type) {
		AdresseDisponibleView addDispoView = new AdresseDisponibleView();

		Assert.isFalse(tiers instanceof MenageCommun);
		final RapportEntreTiers rapport = TiersHelper.getRapportSujetOfType(tiers, type.getTypeRapport(), null);
		final Tiers conseiller = tiersDAO.get(rapport.getObjetId());

		addDispoView.setSource(type.getTypeSource());
		addDispoView.setRepresentantLegal(getNomCourrier(conseiller));
		addDispoView.setLocalite(adresse.getLocalite());
		addDispoView.setNumeroCasePostale(adresse.getNumeroOrdrePostal());
		addDispoView.setRepresentantId(conseiller.getNumero());

		Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays != null) {
			Pays pays = getServiceInfrastructureService().getPays(noOfsPays);
			if (pays != null) {
				addDispoView.setPaysNpa(pays.getNomCourt());
			}
		}

		addDispoView.setRue(adresse.getRue());
		return addDispoView;
	}

	/**
	 * Recupere le nom courrier
	 */
	private String getNomCourrier(Tiers tiers) {

		List<String> nomCourrier;
		try {
			nomCourrier = getAdresseService().getNomCourrier(tiers, null, false);
		}
		catch (AdresseException e) {
			nomCourrier = null;
		}

		final StringBuilder result = new StringBuilder();
		if (nomCourrier != null) {
			for (String nom : nomCourrier) {
				result.append(nom).append(' ');
			}
		}

		return result.toString().trim();
	}

	/**
	 * Charge les informations dans TiersView
	 */
	@Override
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, ServiceInfrastructureException{

		if ( numero == null) {
			return null;
		}
		TiersEditView tiersEditView = new TiersEditView();
		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new TiersNotFoundException(numero);
		}

		tiersEditView.setTiers(tiers);
		setTiersGeneralView(tiersEditView, tiers);
		setAdressesFiscalesModifiables(tiersEditView, tiers);		

		return tiersEditView;
	}
}
