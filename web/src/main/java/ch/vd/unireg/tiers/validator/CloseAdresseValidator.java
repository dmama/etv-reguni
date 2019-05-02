package ch.vd.unireg.tiers.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.security.SecurityHelper;
import ch.vd.unireg.security.SecurityProviderInterface;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.NatureTiers;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.tiers.view.CloseAdresseView;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.TypeAdresseTiers;

public class CloseAdresseValidator implements Validator {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloseAdresseValidator.class);

	private TiersService tiersService;
	private SecurityProviderInterface securityProvider;

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}

	/**
	 * @see org.springframework.validation.Validator#supports(Class)
	 */
	@Override
	public boolean supports(Class<?> clazz) {
		return CloseAdresseView.class.equals(clazz);
	}

	/**
	 * @see org.springframework.validation.Validator#validate(Object, org.springframework.validation.Errors)
	 */
	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		if (obj == null) {
			return;
		}

		final CloseAdresseView adresseView = (CloseAdresseView) obj;
		final RegDate dateFin = adresseView.getDateFin();
		final TypeAdresseTiers usage = adresseView.getUsage();
		final Tiers tiers = tiersService.getTiers(adresseView.getIdTiers());

		final Niveau acces = SecurityHelper.getDroitAcces(securityProvider, tiers);
		if (acces == null || acces == Niveau.LECTURE) {
			errors.reject("error.tiers.interdit");
		}

		// Vérification de la date de Fin
		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateFin")) {
			if (dateFin == null) {
				errors.rejectValue("dateFin", "error.date.fin.vide");
			}
			else if (dateFin.isBefore(adresseView.getDateDebut())) {
				errors.rejectValue("dateFin", "error.date.fin.avant.debut");
			}
			else if (dateFin.isAfter(RegDate.get())) {
				errors.rejectValue("dateFin", "error.date.fin.dans.futur");
			}
		}

		//gestion des droits de fermeture d'une adresse
		boolean isAllowed = false;
		if(tiers.getNatureTiers() == NatureTiers.Habitant || tiers.getNatureTiers() == NatureTiers.NonHabitant ||
				tiers.getNatureTiers() == NatureTiers.MenageCommun){
			//PP
			switch (usage) {
				case COURRIER :
					// [UNIREG-1292] - mise à jour selon la matrice des droits Unireg
					if (tiers instanceof PersonnePhysique) {
						final PersonnePhysique pp = (PersonnePhysique) tiers;
						if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD) && tiersService.isDecede(pp)) {
							// on peut modifier l'adresse courrier d'un décédé uniquement si ont possède le rôle correspondant
							isAllowed = true;
						}
						else if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C) && !tiersService.isDecede(pp)) {
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
							if (SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C_DCD)) {
								isAllowed = true;
							}
						}
						else {
							if(SecurityHelper.isGranted(securityProvider, Role.ADR_PP_C)) {
								isAllowed = true;
							}
						}
					}
					break;
				case DOMICILE :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_PP_D);
					break;
				case POURSUITE :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_P);
					break;
				case REPRESENTATION :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_PP_B);
					break;
			}
		}
		else if(tiers.getNatureTiers() == NatureTiers.DebiteurPrestationImposable){
			if (usage == TypeAdresseTiers.POURSUITE) {
				isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_P);
			}
			else {
				isAllowed = SecurityHelper.isGranted(securityProvider, Role.CREATE_MODIF_DPI);
			}
		}
		else {
			//PM
			switch (usage) {
				case COURRIER :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_PM_C);
					break;
				case DOMICILE :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_PM_D);
					break;
				case POURSUITE :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_P);
					break;
				case REPRESENTATION :
					isAllowed = SecurityHelper.isGranted(securityProvider, Role.ADR_PM_B);
					break;
			}
		}

		if (!isAllowed) {
			errors.rejectValue("usage", "error.usage.interdit");
		}
	}
}