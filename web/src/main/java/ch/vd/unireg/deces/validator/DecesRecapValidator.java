package ch.vd.unireg.deces.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.deces.view.DecesRecapView;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;

import static ch.vd.registre.base.date.RegDate.get;
import static ch.vd.unireg.utils.ValidatorUtils.rejectErrors;

public class DecesRecapValidator implements Validator {

	private MetierService metierService;
	private TiersService tiersService;

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DecesRecapView.class.equals(clazz) ;
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		if (!(obj instanceof DecesRecapView)) {
			throw new IllegalArgumentException();
		}
		DecesRecapView decesRecapView = (DecesRecapView) obj;

		boolean veuvageMarieSeul = decesRecapView.isMarieSeul() && decesRecapView.isVeuf();

		final RegDate dateDeces = decesRecapView.getDateDeces();
		if (dateDeces == null) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateDeces")) {
				if (veuvageMarieSeul) {
					errors.rejectValue("dateDeces", "error.date.veuvage.vide");
				}
				else {
					errors.rejectValue("dateDeces", "error.date.deces.vide");
				}
			}
		}
		else {
			boolean dateDecesFuture = get().isBefore(dateDeces);

			//Validation du deces
			PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(decesRecapView.getTiersId());
			ValidationResults results;

			if (veuvageMarieSeul) {
				if (dateDecesFuture) {
					errors.rejectValue("dateDeces", "error.date.veuvage.future");
				}
				results = metierService.validateVeuvage(pp, RegDateHelper.get(decesRecapView.getDateDeces()));
			}
			else {
				if (dateDecesFuture) {
					errors.rejectValue("dateDeces", "error.date.deces.future");
				}
				results = metierService.validateDeces(pp, RegDateHelper.get(decesRecapView.getDateDeces()));
			}
			List<String> erreurs = results.getErrors();
			rejectErrors(erreurs, errors);
		}
	}

	public MetierService getMetierService() {
		return metierService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	public TiersService getTiersService() {
		return tiersService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}


}
