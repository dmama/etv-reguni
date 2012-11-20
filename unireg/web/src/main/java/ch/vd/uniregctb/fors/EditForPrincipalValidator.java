package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;

public class EditForPrincipalValidator extends EditForRevenuFortuneValidator {

	private final HibernateTemplate hibernateTemplate;

	public EditForPrincipalValidator(HibernateTemplate hibernateTemplate) {
		super(hibernateTemplate);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForPrincipalView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);    //To change body of overridden methods use File | Settings | File Templates.

		final EditForPrincipalView view = (EditForPrincipalView) target;

		final Contribuable ctb = hibernateTemplate.get(Contribuable.class, view.getTiersId());
		if (ctb != null) {
			// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification)
			final List<DateRange> fors = new ArrayList<DateRange>();
			for (ForFiscal f : ctb.getForsFiscauxPrincipauxActifsSorted()) {
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
