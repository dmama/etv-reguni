package ch.vd.uniregctb.tiers.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.uniregctb.adresse.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.adresse.AdresseGenerique.Source;
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
		final Tiers tiers = getTiersService().getTiers(adresseTiers.getTiers().getNumero());
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
		final Tiers tiers = getTiersService().getTiers(numeroCtb);
		List<AdresseDisponibleView> lAdresse = getAdressesDisponible(tiers);
		adresseView.setAdresseDisponibles(lAdresse);
		return adresseView;
	}

	/**
	 * Sauvegarde de l'adresse en base de donnees
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(AdresseView adresseView) {
		if (adresseView.getId() == null) {
			AdresseTiers adresseTiers = enrichiAdresseTiers(adresseView);

			Tiers tiers = getTiersService().getTiers(adresseView.getNumCTB());
			if (adresseTiers != null) {
				getAdresseService().addAdresse(tiers, adresseTiers);
			}
		}
		else {
			updateAdresse(adresseView);
		}
	}

	/**
	 * Met à jour en base de données une adresse
	 *
	 * @param adresseView
	 */
	private void updateAdresse(AdresseView adresseView) {
		AdresseTiers adresseTiers = getAdresseTiersDAO().get(adresseView.getId());

		if (TYPE_LOCALITE_SUISSE.equals(adresseView.getTypeLocalite())) {
			adresseTiers = fillAdresseSuisse(adresseView, (AdresseSuisse) adresseTiers);
		}

		if (TYPE_LOCALITE_PAYS.equals(adresseView.getTypeLocalite())) {
			adresseTiers = fillAdresseEtrangere(adresseView, (AdresseEtrangere) adresseTiers);
		}

	}

	/**
	 * Sauvegarde d'une reprise d'adresse
	 *
	 * @param adresseView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void saveReprise(AdresseView adresseView) {

		Tiers tiers = getTiersService().getTiers(adresseView.getNumCTB());

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

			Tiers representant = getTiersService().getTiers(addDisponibleView.getNumeroTiers());
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
	 * Alimente AdresseTiers de core en fonction de la vue
	 *
	 * @param adresseView
	 * @return
	 */
	private AdresseTiers enrichiAdresseTiers(AdresseView adresseView) {

		AdresseTiers adresseTiers = null;

		if (TYPE_LOCALITE_SUISSE.equals(adresseView.getTypeLocalite())) {

			AdresseSuisse adresse = new AdresseSuisse();
			adresseTiers = fillAdresseSuisse(adresseView, adresse);

		}

		if (TYPE_LOCALITE_PAYS.equals(adresseView.getTypeLocalite())) {

			AdresseEtrangere adresseEtrangere = new AdresseEtrangere();
			adresseTiers = fillAdresseEtrangere(adresseView, adresseEtrangere);
		}

		return adresseTiers;
	}

	/**
	 * Alimente un objet AdresseEtrangere en fonction de AdresseView
	 *
	 * @param adresseView
	 * @param adresseEtrangere
	 * @return
	 */
	private AdresseTiers fillAdresseEtrangere(AdresseView adresseView, AdresseEtrangere adresseEtrangere) {
		AdresseTiers adresseTiers;

		if (adresseEtrangere.getId() == null) {
			adresseEtrangere.setDateDebut(adresseView.getRegDateDebut());
		}

		adresseEtrangere.setUsage(adresseView.getUsage());
		adresseEtrangere.setNumeroMaison(adresseView.getNumeroMaison());
		adresseEtrangere.setNumeroOfsPays(adresseView.getPaysOFS());
		adresseEtrangere.setComplement(adresseView.getComplements());
		adresseEtrangere.setPermanente(adresseView.isPermanente());
		adresseEtrangere.setNumeroAppartement(adresseView.getNumeroAppartement());
		adresseEtrangere.setNumeroCasePostale(adresseView.getNumeroCasePostale());
		adresseEtrangere.setRue(adresseView.getRue());
		adresseEtrangere.setTexteCasePostale(adresseView.getTexteCasePostale());
		adresseEtrangere.setNumeroPostalLocalite(adresseView.getLocaliteNpa());
		adresseEtrangere.setComplement(adresseView.getComplements());
		adresseEtrangere.setComplementLocalite(adresseView.getComplementLocalite());
		adresseTiers = adresseEtrangere;
		return adresseTiers;
	}

	/**
	 * Alimente un objet AdresseSuisse en fonction de AdresseView
	 *
	 * @param adresseView
	 * @param adresse
	 * @return
	 */
	private AdresseTiers fillAdresseSuisse(AdresseView adresseView, AdresseSuisse adresse) {
		AdresseTiers adresseTiers;

		if (adresse.getId() == null) {
			adresse.setDateDebut(adresseView.getRegDateDebut());
		}
		adresse.setUsage(adresseView.getUsage());
		adresse.setNumeroMaison(adresseView.getNumeroMaison());
		adresse.setNumeroRue(adresseView.getNumeroRue());
		Integer numeroOrdrePoste = null;
		if (!StringUtils.isEmpty(adresseView.getNumeroOrdrePoste())) {
			numeroOrdrePoste = new Integer(adresseView.getNumeroOrdrePoste());
		}
		adresse.setNumeroOrdrePoste(numeroOrdrePoste);
		adresse.setComplement(adresseView.getComplements());
		adresse.setPermanente(adresseView.isPermanente());
		adresse.setNumeroAppartement(adresseView.getNumeroAppartement());
		adresse.setNumeroCasePostale(adresseView.getNumeroCasePostale());
		adresse.setRue(adresseView.getRue());
		adresse.setTexteCasePostale(adresseView.getTexteCasePostale());
		adresseTiers = adresse;
		return adresseTiers;
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
		try {
			localite = getServiceInfrastructureService().getLocaliteByONRP(noLocalite);
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur en essayant de récupérer la localité avec le numéro = " + noLocalite, e);
		}
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
			try {
				rue = getServiceInfrastructureService().getRueByNumero(numeroRue);
			}
			catch (InfrastructureException e) {
				throw new RuntimeException("Erreur en essayant de récupérer la rue avec le numéro OFS = " + numeroRue, e);
			}
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
		try {
			return getServiceInfrastructureService().getPays(adresseEtrangere.getNumeroOfsPays());
		}
		catch (InfrastructureException e) {
			throw new RuntimeException("Erreur en essayant de récupérer le pays", e);
		}

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
				addDispoView.setTypeAdresse(EnumTypeAdresse.COURRIER);
				adressesDisponibleView.add(addDispoView);
			}
			if (adressesIndividu.principale != null) {

				AdresseDisponibleView addDispoView = createAdresseDisponibleViewFromAdresseCivil(adressesIndividu.principale);
				addDispoView.setTypeAdresse(EnumTypeAdresse.PRINCIPALE);
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

		addDispoView.setSource(Source.CIVILE);
		addDispoView.setLocalite(addIndividu.getLocalite());
		addDispoView.setNumeroCasePostale(addIndividu.getNumeroOrdrePostal());

		Integer noOfsPays = addIndividu.getNoOfsPays();
		if (noOfsPays != null) {
			try {
				Pays pays = getServiceInfrastructureService().getPays(noOfsPays);
				if (pays != null) {
					addDispoView.setPaysNpa(pays.getNomMinuscule());
				}
			}
			catch (InfrastructureException e) {
				throw new RuntimeException(e);
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

		if (type == TypeAdresseRepresentant.CONSEIL_LEGAL || type == TypeAdresseRepresentant.TUTELLE || type == TypeAdresseRepresentant.CURATELLE) {
			Assert.isFalse(tiers instanceof MenageCommun);
			final RapportEntreTiers rapport = TiersHelper.getRapportSujetOfType(tiers, type.getTypeRapport(), null);
			final Tiers conseiller = tiersDAO.get(rapport.getObjetId());
			
			addDispoView.setSource(type.getTypeSource());
			addDispoView.setRepresentantLegal(getNomCourrier(conseiller));
		}

		addDispoView.setLocalite(adresse.getLocalite());
		addDispoView.setNumeroCasePostale(adresse.getNumeroOrdrePostal());
		addDispoView.setNumeroTiers(tiers.getNumero());

		Integer noOfsPays = adresse.getNoOfsPays();
		if (noOfsPays != null) {
			try {
				Pays pays = getServiceInfrastructureService().getPays(noOfsPays);
				if (pays != null) {
					addDispoView.setPaysNpa(pays.getNomMinuscule());
				}
			}
			catch (InfrastructureException e) {
				throw new RuntimeException(e);
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
		final Tiers tiers = getTiersService().getTiers(numero);
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
