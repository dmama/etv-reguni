package ch.vd.unireg.fors;

import org.springframework.validation.Errors;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.ForFiscalAutreElementImposable;

public class EditForAutreElementImposableValidator extends EditForRevenuFortuneValidator {

	private final HibernateTemplate hibernateTemplate;

	public EditForAutreElementImposableValidator(HibernateTemplate hibernateTemplate) {
		super(hibernateTemplate);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForAutreElementImposableView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		// préparation de la vue
		final EditForAutreElementImposableView view = (EditForAutreElementImposableView) target;
		final ForFiscalAutreElementImposable ffaei = hibernateTemplate.get(ForFiscalAutreElementImposable.class, view.getId());
		if (ffaei == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}
		view.initReadOnlyData(ffaei); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)

		super.validate(target, errors);

		// [SIFISC-7381] si aucun changement n'a été saisi, on réaffiche le formulaire
		if (ffaei.getMotifFermeture() == view.getMotifFin() && ffaei.getDateFin() == view.getDateFin() && ffaei.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale())) {
			errors.reject("global.error.aucun.changement");
		}
	}
}
