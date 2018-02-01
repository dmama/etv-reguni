package ch.vd.unireg.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;

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

		// préparation de la vue
		final EditForPrincipalView view = (EditForPrincipalView) target;
		final ForFiscalPrincipal ffp = hibernateTemplate.get(ForFiscalPrincipal.class, view.getId());
		if (ffp == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}
		view.initReadOnlyData(ffp); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)

		super.validate(target, errors);

		// [SIFISC-7381] si aucun changement n'a été saisi, on réaffiche le formulaire
		if (ffp.getMotifFermeture() == view.getMotifFin() && ffp.getDateFin() == view.getDateFin() && ffp.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale())) {
			errors.reject("global.error.aucun.changement");
			return;
		}

		// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification)
		final List<DateRange> fors = new ArrayList<>();
		for (ForFiscal f : ffp.getTiers().getForsFiscauxPrincipauxActifsSorted()) {
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
