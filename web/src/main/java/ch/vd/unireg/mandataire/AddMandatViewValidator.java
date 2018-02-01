package ch.vd.uniregctb.mandataire;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.tiers.view.DateRangeViewValidator;
import ch.vd.uniregctb.type.TypeMandat;

public class AddMandatViewValidator implements Validator {

	private static final DateRangeViewValidator RANGE_VALIDATOR = new DateRangeViewValidator(false, true, true, true);

	private final IbanValidator ibanValidator;
	private final Set<String> codesGenreImpotMandataireAutorises;

	public AddMandatViewValidator(IbanValidator ibanValidator, Set<String> codesGenreImpotMandataireAutorises) {
		this.ibanValidator = ibanValidator;
		this.codesGenreImpotMandataireAutorises = codesGenreImpotMandataireAutorises;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddMandatView.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		final AddMandatView view = (AddMandatView) target;

		// validation des dates
		RANGE_VALIDATOR.validate(view, errors);

		// type de mandat : obligatoire
		if (view.getTypeMandat() == null) {
			if (!errors.hasFieldErrors("typeMandat")) {
				errors.rejectValue("typeMandat", "error.type.mandat.vide");
			}
		}

		// spécificités du mandat TIERS : IBAN
		else if (view.getTypeMandat() == TypeMandat.TIERS) {
			final String iban = view.getIban();
			if (StringUtils.isBlank(iban)) {
				errors.rejectValue("iban", "error.iban.mandat.tiers.vide");
			}
			else {
				final String erreurIban = ibanValidator.getIbanValidationError(iban);
				if (StringUtils.isNotBlank(erreurIban)) {
					errors.rejectValue("iban", "error.iban.detail", new Object[]{erreurIban}, null);
				}
				else {
					final String normalized = FormatNumeroHelper.removeSpaceAndDash(iban).toUpperCase();
					if (!normalized.startsWith(ServiceInfrastructureRaw.SIGLE_SUISSE)) {
						errors.rejectValue("iban", "error.iban.etranger");
					}
				}
			}

			if (view.getIdTiersMandataire() == null) {
				errors.reject("error.mandat.tiers.sans.tiers.referent");
			}
		}

		// autres types de mandat : GENERAL et SPECIAL
		else {

			// il faut un genre d'impôt
			if (view.getTypeMandat() == TypeMandat.SPECIAL) {

				if (StringUtils.isBlank(view.getCodeGenreImpot())) {
					errors.rejectValue("codeGenreImpot", "error.genre.impot.mandat.special.vide");
				}
				else if (!codesGenreImpotMandataireAutorises.contains(view.getCodeGenreImpot())) {
					errors.rejectValue("codeGenreImpot", "error.genre.impot.mandat.special.invalide");
				}

			}
		}

		// avec ou sans tiers référent ?
		if (view.getIdTiersMandataire() == null) {

			// validation des données d'adresse
			if (StringUtils.isBlank(view.getRaisonSociale())) {
				errors.rejectValue("raisonSociale", "error.raison.sociale.mandat.adresse.vide");
			}

			// c'est une adresse en Suisse... il doit donc y avoir un numéro d'ordre poste
			if (StringUtils.isBlank(view.getAdresse().getNumeroOrdrePoste())) {
				errors.rejectValue("adresse.localiteSuisse", "error.format.localite_suisse");
			}

		}
	}
}
