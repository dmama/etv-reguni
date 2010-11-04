package ch.vd.uniregctb.general.manager;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdresseEnvoiDetaillee;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.general.view.RoleView;
import ch.vd.uniregctb.general.view.TiersGeneralView;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForGestion;
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

		try {
			final AdresseEnvoiDetaillee adresseEnvoi = adresseService.getAdresseEnvoi(tiers, null, TypeAdresseFiscale.COURRIER, false);
			tiersGeneralView.setAdresseEnvoi(adresseEnvoi);
		} catch (Exception e) {
			tiersGeneralView.setAdresseEnvoi(null);
			tiersGeneralView.setAdresseEnvoiException(e);
		}
		tiersGeneralView.setNatureTiers(tiers.getNatureTiers());
		tiersGeneralView.setAnnule(tiers.isAnnule());

		setCommuneGestion(tiers, tiersGeneralView);
		return tiersGeneralView;
	}

	private void setCommuneGestion(Tiers tiers, TiersGeneralView view) {
		final ForGestion forGestion = tiersService.getDernierForGestionConnu(tiers, null);
		if (forGestion != null) {
			final int ofsCommune = forGestion.getNoOfsCommune();
			try {
				final Commune commune = infraService.getCommuneByNumeroOfsEtendu(ofsCommune, forGestion.getDateFin());
				view.setNomCommuneGestion(commune.getNomMinuscule());
			}
			catch (InfrastructureException e) {
				LOGGER.error("Erreur lors de la récupération de la commune de gestion", e);
				view.setNomCommuneGestion(null);
			}
		}
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
		if (etendu) {
			tiersGeneralView.setCategorie(dpi.getCategorieImpotSource());
			tiersGeneralView.setPeriodicite(dpi.getPeriodiciteAt(RegDate.get()).getPeriodiciteDecompte());
			tiersGeneralView.setPeriode(dpi.getPeriodiciteAt(RegDate.get()).getPeriodeDecompte());
			tiersGeneralView.setModeCommunication(dpi.getModeCommunication());
			tiersGeneralView.setPersonneContact(dpi.getPersonneContact());
			tiersGeneralView.setNumeroTelephone(dpi.getNumeroTelephonePrive());
		}
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
