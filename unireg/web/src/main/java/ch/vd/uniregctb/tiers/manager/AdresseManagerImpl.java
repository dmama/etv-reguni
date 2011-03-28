package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseAutreTiers;
import ch.vd.uniregctb.adresse.AdresseCivile;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.adresse.TypeAdresseRepresentant;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.tiers.view.AdresseDisponibleView;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.tiers.view.TiersVisuView;
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
	 * @param id
	 * @param numero
	 * @return
	 */
	@Transactional(readOnly = true)
	public AdresseView getAdresseView(Long id) {
		AdresseView adresseView = new AdresseView();
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(id);

		if (adresseTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.adresse.inexistante" , null,  WebContextUtils.getDefaultLocale()));
		}

		adresseView = enrichiAdresseView(adresseTiers);
		final Tiers tiers = tiersService.getTiers(adresseTiers.getTiers().getNumero());
		List<AdresseDisponibleView> lAdresse = getAdressesDisponible(tiers);

		adresseView.setNature(tiers.getNatureTiers());
		adresseView.setAdresseDisponibles(lAdresse);
		adresseView.setNumCTB(adresseTiers.getTiers().getNumero());
		return adresseView;
	}

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
	 * Cree une nouvelle vue AdresseView pour une nouvelle adresse
	 *
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = true)
	public AdresseView create(Long numeroCtb) {
		AdresseView adresseView = new AdresseView();
		adresseView.setNumCTB(numeroCtb);
		adresseView.setTypeLocalite(TYPE_LOCALITE_SUISSE);
		final Tiers tiers = tiersService.getTiers(numeroCtb);
		List<AdresseDisponibleView> lAdresse = getAdressesDisponible(tiers);
		adresseView.setAdresseDisponibles(lAdresse);
		return adresseView;
	}

	/**
	 * Sauvegarde de l'adresse en base de donnees
	 *
	 * @param adresseView la vue web de l'adresse à sauver
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AdresseView adresseView) {
		if (adresseView.getId() == null) {
			AdresseTiers adresseTiers = createAdresseTiers(adresseView);
			if (adresseTiers != null) {
				Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());
				adresseService.addAdresse(tiers, adresseTiers);
			}
		}
		else {
			updateAdresse(adresseView);
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
	}

	/**
	 * Alimente AdresseView de core en fonction d'adresse Tiers
	 *
	 * @param adresseTiers
	 * @return
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

			}
			else {

				AdresseEtrangere adresseEtrangere = (AdresseEtrangere) adresse;

				adresseView.setTypeLocalite("pays");
				Pays pays = getPays(adresseEtrangere);
				adresseView.setPaysNpa(pays.getNomMinuscule());
				adresseView.setPaysOFS(pays.getNoOFS());
				adresseView.setLocaliteNpa(adresseEtrangere.getNumeroPostalLocalite());
				adresseView.setComplementLocalite(adresseEtrangere.getComplementLocalite());
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
	 *
	 * @param idAdresse
	 * @throws AdressesResolutionException
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void annulerAdresse(Long idAdresse) {
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(idAdresse);
		if (adresseTiers != null) {
			getAdresseService().annulerAdresse(adresseTiers);
		}
	}

	public void fermerAdresse(AdresseView bean){
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(bean.getId());
		if (adresseTiers != null) {
			getAdresseService().fermerAdresse(adresseTiers,bean.getRegDateFin());
		}
	}

	/**
	 * Recupere la Localite
	 *
	 * @param adresse
	 * @return
	 */
	private Localite getLocalite(AdresseSuisse adresse) {
		final Integer noLocalite = getNumeroOrdreLocalite(adresse);
		final Localite localite;
		localite = getServiceInfrastructureService().getLocaliteByONRP(noLocalite);
		return localite;
	}

	/**
	 * Recupere le numéro ordre poste
	 *
	 * @param adresse
	 * @return
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
	 *
	 * @param adresseEtrangere
	 * @return
	 */
	public Pays getPays(AdresseEtrangere adresseEtrangere) {

		Assert.notNull(adresseEtrangere);
		return getServiceInfrastructureService().getPays(adresseEtrangere.getNumeroOfsPays());

	}

	/**
	 * Recupere la liste des adresses disponibles
	 *
	 * @param tiers
	 * @return
	 */
	private List<AdresseDisponibleView> getAdressesDisponible(Tiers tiers) {

		List<AdresseDisponibleView> adresses = new ArrayList<AdresseDisponibleView>();

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
	 *
	 * @param adressesDisponibleView
	 * @param adressesIndividu
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
	 *
	 * @param addIndividu
	 * @return
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
				addDispoView.setPaysNpa(pays.getNomMinuscule());
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
				addDispoView.setPaysNpa(pays.getNomMinuscule());
			}
		}

		addDispoView.setRue(adresse.getRue());
		return addDispoView;
	}

	/**
	 * Recupere le nom courrier
	 *
	 * @param tiers
	 * @return
	 */
	private String getNomCourrier(Tiers tiers) {

		List<String> nomCourrier;
		try {
			nomCourrier = getAdresseService().getNomCourrier(tiers, null, false);
		}
		catch (AdresseException e) {
			nomCourrier = null;
		}

		StringBuffer result = new StringBuffer();

		if (nomCourrier != null) {
			for (String nom : nomCourrier) {
				result.append(nom).append(' ');
			}
		}

		return result.toString().trim();
	}

	/**
	 * Charge les informations dans TiersView
	 *
	 * @param numero
	 * @return un objet TiersView
	 * @throws AdresseException
	 * @throws InfrastructureException
	 */
	@Transactional(readOnly = true)
	public TiersEditView getView(Long numero) throws AdresseException, InfrastructureException{

		if ( numero == null) {
			return null;
		}
		TiersEditView tiersEditView = new TiersEditView();
		final Tiers tiers = tiersService.getTiers(numero);
		if (tiers == null) {
			throw new RuntimeException( this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		tiersEditView.setTiers(tiers);
		setTiersGeneralView(tiersEditView, tiers);
		setAdressesFiscalesModifiables(tiersEditView, tiers);		

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
		allowedOnglet.put(TiersVisuView.MODIF_ADRESSE, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_B, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_C, Boolean.FALSE);
		allowedOnglet.put(TiersEditView.ADR_P, Boolean.FALSE);
		return allowedOnglet;
	}
}
