package ch.vd.uniregctb.tiers.validator;

import ch.vd.uniregctb.adresse.*;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.tiers.view.AdresseView;
import ch.vd.uniregctb.tiers.view.TiersEditView;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class TiersAdresseValidator implements Validator {

	protected final Logger LOGGER = Logger.getLogger(TiersAdresseValidator.class);

	private AdresseService adresseService;
	private TiersService tiersService;
	private ServiceInfrastructureService serviceInfra;

	public AdresseService getAdresseService() {
		return adresseService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setServiceInfra(ServiceInfrastructureService serviceInfra) {
		this.serviceInfra = serviceInfra;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * @see org.springframework.validation.Validator#supports(java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return Tiers.class.equals(clazz) || DebiteurPrestationImposable.class.equals(clazz) || TiersEditView.class.equals(clazz)
				|| AdresseView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
	 */
	public void validate(Object obj, Errors errors) {

		if (obj == null) {
			return;
		}

		final AdresseView adresseView = (AdresseView) obj;

		if (!("reprise".equals(adresseView.getMode())) && !("repriseCivil".equals(adresseView.getMode()))) {

			if ("suisse".equals(adresseView.getTypeLocalite())) {
				// On doit avoir un NO_ORDRE
				if (StringUtils.isBlank(adresseView.getNumeroOrdrePoste())) {
					errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
				}
				// et il doit etre valide
				else {
					try {
						Integer noOrdre = Integer.parseInt(adresseView.getNumeroOrdrePoste());
						Localite loc = serviceInfra.getLocaliteByONRP(noOrdre);
						if (loc == null) {
							errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
						}
					}
					catch (Exception e) {
						errors.rejectValue("localiteSuisse", "error.invalid.localite_suisse");
					}
				}
			}

			if ("pays".equals(adresseView.getTypeLocalite())) {

				if (adresseView.getPaysOFS() == null) {
					errors.rejectValue("paysNpa", "error.required.pays");
				}

				if (StringUtils.isBlank(adresseView.getLocaliteNpa())) {
					errors.rejectValue("localiteNpa", "error.required.localite_npa");
				}
			}

		}


		final RegDate dateDebut = adresseView.getRegDateDebut();
		final TypeAdresseTiers usage = adresseView.getUsage();
		final Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces.equals(Niveau.LECTURE)) {
			errors.reject("error.tiers.interdit");
		}

		// Vérification de la date de début
		if (dateDebut == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}
		else if (adresseView.getId() == null) {
			if (RegDate.get().isBefore(dateDebut)) {
				errors.rejectValue("dateDebut", "error.date.debut.future");
			}
			AdressesFiscales adressesFiscales = null;
			try {
				adressesFiscales = getAdresseService().getAdressesFiscales(tiers, null, false);
			}
			catch (AdresseException e) {
				LOGGER.debug(e, e);
			}
			if (adressesFiscales != null) {
				final AdresseGenerique adresse = adressesFiscales.ofType(usage);
				//Vérifie que la date de début de la nouvelle adresse n'est pas *avant* la date début de la dernière adresse existante
				if (adresse != null && adresse.getDateDebut() != null && dateDebut.isBefore(adresse.getDateDebut())) {
					errors.rejectValue("dateDebut", "error.date.debut.anterieure");
				}
			}
		}

		//gestion des droits de création d'une adresse
		boolean isAllowed = false;
		if(tiers.getNatureTiers().equals(Tiers.NATURE_HABITANT) || tiers.getNatureTiers().equals(Tiers.NATURE_NONHABITANT) ||
				tiers.getNatureTiers().equals(Tiers.NATURE_MENAGECOMMUN)){
			//PP
			switch (usage) {
				case COURRIER :
					// [UNIREG-1292] - mise à jour selon la matrice des droits Unireg
					if (tiers instanceof PersonnePhysique) {
						final PersonnePhysique pp = (PersonnePhysique) tiers;
						if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD) && tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un décédé uniquement si ont possède le rôle correspondant
							isAllowed = true;
						}
						else if (SecurityProvider.isGranted(Role.ADR_PP_C) && !tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un non décédé uniquement si ont possède le rôle correspondant
							isAllowed = true;
						}
					}
					else if (tiers instanceof MenageCommun) {
						final MenageCommun mc = (MenageCommun) tiers;
						boolean auMoinsUnDecede = false;
						for (PersonnePhysique pp : mc.getPersonnesPhysiques()) {
							if (tiersService.isDecede(pp)) {
								auMoinsUnDecede = true;
							}
						}
						// pour les ménages commun le droit sur les décédés est le plus contraignant
						if (auMoinsUnDecede) {
							if (SecurityProvider.isGranted(Role.ADR_PP_C_DCD)) {
								isAllowed = true;
							}
						}
						else {
							if(SecurityProvider.isGranted(Role.ADR_PP_C)) {
								isAllowed = true;
							}
						}
					}
					break;
				case DOMICILE :
					if(SecurityProvider.isGranted(Role.ADR_PP_D))
						isAllowed = true;
					break;
				case POURSUITE :
					if(SecurityProvider.isGranted(Role.ADR_P))
						isAllowed = true;
					break;
				case REPRESENTATION :
					if(SecurityProvider.isGranted(Role.ADR_PP_B))
						isAllowed = true;
					break;
			}
		}
		else if(tiers.getNatureTiers().equals(Tiers.NATURE_DPI)){
			if(SecurityProvider.isGranted(Role.CREATE_DPI))
				isAllowed = true;
		}
		else {
			//PM
			switch (usage) {
				case COURRIER :
					if(SecurityProvider.isGranted(Role.ADR_PM_C))
						isAllowed = true;
					break;
				case DOMICILE :
					if(SecurityProvider.isGranted(Role.ADR_PM_D))
						isAllowed = true;
					break;
				case POURSUITE :
					if(SecurityProvider.isGranted(Role.ADR_P))
						isAllowed = true;
					break;
				case REPRESENTATION :
					if(SecurityProvider.isGranted(Role.ADR_PM_B))
						isAllowed = true;
					break;
			}
		}

		if(!isAllowed){
			if (adresseView.getId() == null) {//création
				errors.rejectValue("usage", "error.usage.interdit");
			}
			else {//édition (il ne devrait plus y avoir d'édition d'adresse à terme)
				errors.reject("error.tiers.interdit");
			}
		}
	}
}
