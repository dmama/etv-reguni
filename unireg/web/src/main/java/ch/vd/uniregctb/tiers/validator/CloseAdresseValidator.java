package ch.vd.uniregctb.tiers.validator;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
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

public class CloseAdresseValidator implements Validator {

	protected final Logger LOGGER = Logger.getLogger(CloseAdresseValidator.class);


	private TiersService tiersService;


	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	/**
	 * @see org.springframework.validation.Validator#supports(Class)
	 */
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return Tiers.class.equals(clazz) || DebiteurPrestationImposable.class.equals(clazz) || TiersEditView.class.equals(clazz)
				|| AdresseView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(Object, org.springframework.validation.Errors)
	 */
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		if (obj == null) {
			return;
		}

		final AdresseView adresseView = (AdresseView) obj;
		final RegDate dateFin = adresseView.getRegDateFin();
		final TypeAdresseTiers usage = adresseView.getUsage();
		final Tiers tiers = tiersService.getTiers(adresseView.getNumCTB());

		final Niveau acces = SecurityProvider.getDroitAcces(tiers);
		if (acces == null || acces.equals(Niveau.LECTURE)) {
			errors.reject("error.tiers.interdit");
		}

		// Vérification de la date de Fin
		if (dateFin == null) {
			errors.rejectValue("dateFin", "error.date.fin.vide");
		}
		else if (dateFin.isBefore(adresseView.getRegDateDebut())) {

				errors.rejectValue("dateFin", "error.date.fin.avant.debut");

		}
		else if (dateFin.isAfter(RegDate.get())) {

				errors.rejectValue("dateFin", "error.date.fin.dans.futur");

		}

		//gestion des droits de fermeture d'une adresse
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
						for (PersonnePhysique pp : tiersService.getPersonnesPhysiques(mc)) {
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
			if (usage == TypeAdresseTiers.POURSUITE) {
				isAllowed = SecurityProvider.isGranted(Role.ADR_P);
			}
			else {
				isAllowed = SecurityProvider.isGranted(Role.CREATE_DPI);
			}
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