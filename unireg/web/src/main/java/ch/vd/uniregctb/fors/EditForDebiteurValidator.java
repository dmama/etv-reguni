package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersDAO;

public class EditForDebiteurValidator extends EditForValidator {

	private TiersDAO tiersDAO;

	public EditForDebiteurValidator(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForDebiteurView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final EditForDebiteurView view = (EditForDebiteurView) target;

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(view.getTiersId());
		if (dpi != null) {
			// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification s'il s'agit d'une modification)
			final List<DateRange> fors = new ArrayList<DateRange>();
			for (ForFiscal f : dpi.getForsFiscauxNonAnnules(true)) {
				if (view.getId() != f.getId()) {
					fors.add(new DateRangeHelper.Range(f));
				}
			}
			if (DateRangeHelper.intersect(new DateRangeHelper.Range(view.getDateDebut(), view.getDateFin()), fors)) {
				errors.rejectValue("dateDebut", "error.date.chevauchement");
				errors.rejectValue("dateFin", "error.date.chevauchement");
			}
		}
	}
}
