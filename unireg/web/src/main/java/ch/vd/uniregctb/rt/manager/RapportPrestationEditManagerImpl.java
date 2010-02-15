package ch.vd.uniregctb.rt.manager;

import java.util.List;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdressesResolutionException;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.general.manager.TiersGeneralManager;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.rt.view.DebiteurListView;
import ch.vd.uniregctb.rt.view.RapportPrestationView;
import ch.vd.uniregctb.rt.view.SourcierListView;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportEntreTiersDAO;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.SensRapportEntreTiers;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Classe qui gere le controller RapportEditController
 *
 * @author xcifde
 *
 */
public class RapportPrestationEditManagerImpl implements RapportPrestationEditManager, MessageSourceAware{

	private TiersService tiersService;

	private AdresseService adresseService;

	private TiersGeneralManager tiersGeneralManager;

	private RapportEntreTiersDAO rapportEntreTiersDAO;

	private MessageSource messageSource;

	public RapportEntreTiersDAO getRapportEntreTiersDAO() {
		return rapportEntreTiersDAO;
	}

	public void setRapportEntreTiersDAO(RapportEntreTiersDAO rapportEntreTiersDAO) {
		this.rapportEntreTiersDAO = rapportEntreTiersDAO;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public TiersGeneralManager getTiersGeneralManager() {
		return tiersGeneralManager;
	}

	public void setTiersGeneralManager(TiersGeneralManager tiersGeneralManager) {
		this.tiersGeneralManager = tiersGeneralManager;
	}

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 * @throws AdressesResolutionException
	 */
	public RapportPrestationView get (Long numeroSrc, Long numeroDpi, String provenance) {
		RapportPrestationView rapportView =  new RapportPrestationView();

		rapportView.setProvenance(provenance);
		rapportView.setTypeActivite(TypeActivite.PRINCIPALE);

		PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(numeroSrc);
		if (sourcier == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.sourcier.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView sourcierView = tiersGeneralManager.get(sourcier, true);
		rapportView.setSourcier(sourcierView);

		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi);
		if (dpi == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.debiteur.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		rapportView.setDebiteur(dpiView);

		return rapportView;
	}

	/**
	 * Alimente la vue RapportView
	 *
	 * @param numeroSrc
	 * @param numeroDpi
	 * @return
	 */
	public RapportPrestationView get (Long idRapport, SensRapportEntreTiers sensRapportEntreTiers) throws AdressesResolutionException {
		RapportPrestationView rapportView =  new RapportPrestationView();

		RapportEntreTiers rapportEntreTiers	= rapportEntreTiersDAO.get(idRapport);
		if (rapportEntreTiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.rapport.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}

		rapportView.setSensRapportEntreTiers(sensRapportEntreTiers);
		rapportView.setTypeRapportEntreTiers(rapportEntreTiers.getType());
		Long numero = null;
		if (sensRapportEntreTiers.equals(SensRapportEntreTiers.OBJET)) {
			numero = rapportEntreTiers.getSujet().getNumero();
		}
		if (sensRapportEntreTiers.equals(SensRapportEntreTiers.SUJET)) {
			numero = rapportEntreTiers.getObjet().getNumero();
		}
		setNomCourrier(rapportView, numero);
		rapportView.setId(rapportEntreTiers.getId());
		rapportView.setDateDebut(rapportEntreTiers.getDateDebut());
		rapportView.setDateFin(rapportEntreTiers.getDateFin());
		rapportView.setNatureRapportEntreTiers(rapportEntreTiers.getClass().getSimpleName());
		if (rapportEntreTiers instanceof RapportPrestationImposable) {
			RapportPrestationImposable rapportPrestationImposable = (RapportPrestationImposable) rapportEntreTiers;
			rapportView.setTauxActivite(rapportPrestationImposable.getTauxActivite());
			rapportView.setTypeActivite(rapportPrestationImposable.getTypeActivite());
			rapportView.setNatureRapportEntreTiers(rapportPrestationImposable.getClass().getSimpleName());
		}

		return rapportView;
	}

	/**
	 * Mise à jour de numero, nomCourrier1 et nomCourrier2 en fonction du numero
	 * @param rapportView
	 * @param numero
	 * @throws AdressesResolutionException
	 */
	private void setNomCourrier(RapportPrestationView rapportView, Long numero) throws AdressesResolutionException{
		rapportView.setNumero(numero);

		final Tiers tiers = getTiersService().getTiers(numero);
		if (tiers == null) {
			throw new ObjectNotFoundException(this.getMessageSource().getMessage("error.tiers.inexistant" , null,  WebContextUtils.getDefaultLocale()));
		}
		List<String> nomCourrier = getAdresseService().getNomCourrier(tiers, null);
		rapportView.setNomCourrier(nomCourrier);

	}


	/**
	 * Persiste le rapport de travail
	 * @param rapportView
	 */
	@Transactional(rollbackFor = Throwable.class)
	public void save(RapportPrestationView rapportView) {

		PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(rapportView.getSourcier().getNumero());
		DebiteurPrestationImposable debiteur = (DebiteurPrestationImposable) tiersService.getTiers(rapportView.getDebiteur().getNumero());
		getTiersService().addRapportPrestationImposable(sourcier, debiteur, rapportView.getRegDateDebut(), null, rapportView.getTypeActivite(), rapportView.getTauxActivite());

	}

	/**
	 * Charge l'écran de recherche débiteurs pour un sourcier
	 *
	 * @param numeroSrc
	 * @return
	 */
	public DebiteurListView getDebiteurList(Long numeroSrc) {
		PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(numeroSrc) ;
		DebiteurListView bean = new DebiteurListView();
		TiersGeneralView sourcierView = tiersGeneralManager.get(pp, true);
		bean.setSourcier(sourcierView);
		bean.setTypeRechercheDuNom(DebiteurListView.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiers(DebiteurListView.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE);
		bean.setNumeroSourcier(numeroSrc);
		return bean;
	}

	/**
	 * Charge l'écran de recherche sourciers pour un debiteur
	 *
	 * @param numeroDpi
	 * @return
	 */
	public SourcierListView getSourcierList(Long numeroDpi) {
		DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi) ;
		SourcierListView bean = new SourcierListView();
		TiersGeneralView dpiView = tiersGeneralManager.get(dpi, true);
		bean.setDebiteur(dpiView);
		bean.setTypeRechercheDuNom(SourcierListView.TypeRecherche.EST_EXACTEMENT);
		bean.setTypeTiers(SourcierListView.TypeTiers.PERSONNE_PHYSIQUE);
		bean.setNumeroDebiteur(numeroDpi);
		return bean;
	}


	/**
	 * @return the messageSource
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}

	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

}
