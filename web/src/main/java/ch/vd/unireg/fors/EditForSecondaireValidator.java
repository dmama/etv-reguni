package ch.vd.unireg.fors;

import org.springframework.validation.Errors;

import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.ForFiscalSecondaire;

public class EditForSecondaireValidator extends EditForRevenuFortuneValidator {

	private final HibernateTemplate hibernateTemplate;

	public EditForSecondaireValidator(HibernateTemplate hibernateTemplate) {
		super(hibernateTemplate);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return EditForSecondaireView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {

		// préparation de la vue
		final EditForSecondaireView view = (EditForSecondaireView) target;
		final ForFiscalSecondaire ffs = hibernateTemplate.get(ForFiscalSecondaire.class, view.getId());
		if (ffs == null) {
			throw new ObjectNotFoundException("Impossible de trouver le for fiscal avec l'id=" + view.getId());
		}
		view.initReadOnlyData(ffs); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)

		super.validate(target, errors);

		// [SIFISC-7381] si aucun changement n'a été saisi, on réaffiche le formulaire
		if (ffs.getDateDebut() == view.getDateDebut() && ffs.getMotifOuverture() == view.getMotifDebut() &&
				ffs.getDateFin() == view.getDateFin() && ffs.getMotifFermeture() == view.getMotifFin() &&
				ffs.getNumeroOfsAutoriteFiscale().equals(view.getNoAutoriteFiscale())) {
			errors.reject("global.error.aucun.changement");
		}
	}
}
