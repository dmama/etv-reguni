package ch.vd.uniregctb.deces.validator;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.deces.view.DecesRecapView;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.utils.ValidateHelper;

public class DecesRecapValidator implements Validator {

	private MetierService metierService;
	private TiersService tiersService;

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return DecesRecapView.class.equals(clazz) ;
	}

	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {

		Assert.isTrue(obj instanceof DecesRecapView);
		DecesRecapView decesRecapView = (DecesRecapView) obj;

		boolean veuvageMarieSeul = decesRecapView.isMarieSeul() && decesRecapView.isVeuf();

		final RegDate dateDeces = RegDate.get(decesRecapView.getDateDeces());
		if (dateDeces == null) {
			if (veuvageMarieSeul) {
				ValidationUtils.rejectIfEmpty(errors, "dateDeces", "error.date.veuvage.vide");
			}
			else {
				ValidationUtils.rejectIfEmpty(errors, "dateDeces", "error.date.deces.vide");
			}
		}
		else {
			boolean dateDecesFuture = RegDate.get().isBefore(dateDeces);

			//Validation du deces
			PersonnePhysique pp = (PersonnePhysique) tiersService.getTiers(decesRecapView.getPersonne().getNumero());
			ValidationResults results;

			if (veuvageMarieSeul) {
				if (dateDecesFuture) {
					errors.rejectValue("dateDeces", "error.date.veuvage.future");
				}
				results = metierService.validateVeuvage(pp, RegDate.get(decesRecapView.getDateDeces()));
			}
			else {
				if (dateDecesFuture) {
					errors.rejectValue("dateDeces", "error.date.deces.future");
				}
				results = metierService.validateDeces(pp, RegDate.get(decesRecapView.getDateDeces()));
			}
			List<String> erreurs = results.getErrors();
			ValidateHelper.rejectErrors(erreurs, errors);
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
