package ch.vd.unireg.separation.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.ValidatorHelper;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.separation.view.SeparationRecapView;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.utils.ValidatorUtils;

public class SeparationRecapValidator implements Validator {

	private MetierService metierService;
	private TiersService tiersService;
	private ValidatorHelper validatorHelper;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setValidatorHelper(ValidatorHelper validatorHelper) {
		this.validatorHelper = validatorHelper;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SeparationRecapView.class.equals(clazz) ;
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof SeparationRecapView);
		SeparationRecapView separationRecapView = (SeparationRecapView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		final RegDate dateSeparation = separationRecapView.getDateSeparation();
		if (!errors.hasFieldErrors("dateSeparation")) {
			if (dateSeparation == null) {
				if (EtatCivil.DIVORCE == separationRecapView.getEtatCivil()) {
					errors.rejectValue("dateSeparation", "error.date.divorce.vide");
				}
				else {
					errors.rejectValue("dateSeparation", "error.date.separation.vide");
				}
			}
			else {
				if (RegDate.get().isBefore(dateSeparation)) {
					if (EtatCivil.DIVORCE == separationRecapView.getEtatCivil()) {
						errors.rejectValue("dateSeparation", "error.date.divorce.future");
					}
					else {
						errors.rejectValue("dateSeparation", "error.date.separation.future");
					}
				}
			}
		}

		if (dateSeparation != null) {
			//Validation de la séparation
			final MenageCommun menage = (MenageCommun) tiersService.getTiers(separationRecapView.getIdMenage());
			final ValidationResults results = new ValidationResults();

			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(menage, dateSeparation);
			final PersonnePhysique principal = couple.getPrincipal();
			validatorHelper.validateSexeConnu(principal, results);

			final PersonnePhysique conjoint = couple.getConjoint();
			validatorHelper.validateSexeConnu(conjoint, results);

			results.merge(metierService.validateSeparation(menage, dateSeparation));

			final List<String> validationErrors = results.getErrors();
			ValidatorUtils.rejectErrors(validationErrors, errors);
		}
	}
}