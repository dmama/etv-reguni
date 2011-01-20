package ch.vd.uniregctb.general.manager;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.general.view.RoleView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;

/**
 * Methodes relatives au cartouche contribuable
 * @author xcifde
 *
 */
public class TiersGeneralManagerImpl implements TiersGeneralManager{

	private static final Logger LOGGER = Logger.getLogger(TiersGeneralManagerImpl.class);

	private AdresseService adresseService;

	private TiersService tiersService;

	private ServiceCivilService serviceCivilService;

	private ServiceInfrastructureService infraService;

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setServiceCivilService(ServiceCivilService serviceCivilService) {
		this.serviceCivilService = serviceCivilService;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	/**
	 * Alimente TiersGeneralView en fonction du tiers
	 *
	 * @param tiers
	 * @param full
	 * @return TiersGeneralView
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView getTiers(Tiers tiers, boolean full) {
		final TiersGeneralView tiersGeneralView = new TiersGeneralView();
		setRole(tiersGeneralView, tiers);
		tiersGeneralView.setNumero(tiers.getNumero());

		tiersGeneralView.setNatureTiers(tiers.getNatureTiers());
		tiersGeneralView.setAnnule(tiers.isAnnule());

		return tiersGeneralView;
	}

	/**
	 * Alimente un cartouche DPI étendu
	 *
	 * @param dpi
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView getDebiteur(DebiteurPrestationImposable dpi, boolean etendu) {
		TiersGeneralView tiersGeneralView = getTiers(dpi, true);
		return tiersGeneralView;
	}


	/**
	 * Alimente le carouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	@Transactional(readOnly = true)
	public TiersGeneralView getPersonnePhysique(PersonnePhysique pp, boolean etendu) {
		TiersGeneralView tiersGeneralView = getTiers(pp, true);
		if (etendu) {
			 setCaracteristiquesPP(tiersGeneralView, pp);
		}
		return tiersGeneralView;
	}

	/**
	 * Mise à jour des caractéristiques PP
	 *
	 * @param pp
	 * @param tiersGeneralView
	 */
	private void setCaracteristiquesPP( TiersGeneralView tiersGeneralView, PersonnePhysique pp) {
		tiersGeneralView.setDateNaissance(tiersService.getDateNaissance(pp));
		tiersGeneralView.setAncienNumeroAVS(tiersService.getAncienNumeroAssureSocial(pp));
		tiersGeneralView.setNumeroAssureSocial(tiersService.getNumeroAssureSocial(pp));
	}


	/**
	 * Mise à jour du rôle
	 *
	 * @param tiers
	 */
	private void setRole(TiersGeneralView tiersGeneralView, Tiers tiers) {
		RoleView role = new RoleView();
		role.setLigne1(tiers.getRoleLigne1());
		role.setLigne2(tiersService.getRoleAssujettissement(tiers, RegDate.get()));
		tiersGeneralView.setRole(role);
	}

}
