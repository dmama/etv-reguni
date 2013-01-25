package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;

public class EditForDebiteurValidator extends EditForValidator {

	private HibernateTemplate hibernateTemplate;

	public EditForDebiteurValidator(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForDebiteurView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		// préparation de la vue
		final EditForDebiteurView view = (EditForDebiteurView) target;
		final ForDebiteurPrestationImposable fdpi = hibernateTemplate.get(ForDebiteurPrestationImposable.class, view.getId());
		if (fdpi == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}
		view.initReadOnlyData(fdpi); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)

		super.validate(target, errors);

		if (fdpi.getDateFin() == view.getDateFin()) {
			errors.reject("global.error.aucun.changement");
		}
		else {
			// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification)
			final List<DateRange> fors = new ArrayList<DateRange>();
			for (ForFiscal f : fdpi.getTiers().getForsFiscauxNonAnnules(true)) {
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
