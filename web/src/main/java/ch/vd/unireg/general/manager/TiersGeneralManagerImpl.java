package ch.vd.unireg.general.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.general.view.RoleView;
import ch.vd.unireg.general.view.TiersGeneralView;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;

/**
 * Methodes relatives au cartouche contribuable
 * @author xcifde
 *
 */
public class TiersGeneralManagerImpl implements TiersGeneralManager{

	private static final Logger LOGGER = LoggerFactory.getLogger(TiersGeneralManagerImpl.class);

	private TiersService tiersService;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * Alimente TiersGeneralView en fonction du tiers
	 *
	 * @param tiers
	 * @param full
	 * @return TiersGeneralView
	 */
	@Override
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
	@Override
	@Transactional(readOnly = true)
	public TiersGeneralView getDebiteur(DebiteurPrestationImposable dpi, boolean etendu) {
		return getTiers(dpi, true);
	}


	/**
	 * Alimente le carouche d'une personne physique
	 *
	 * @param pp
	 * @param etendu
	 * @return
	 */
	@Override
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
