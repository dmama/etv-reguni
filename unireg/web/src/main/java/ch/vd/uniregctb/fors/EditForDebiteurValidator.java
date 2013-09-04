package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.tiers.ForDebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.NatureTiers;
import ch.vd.uniregctb.tiers.validator.MotifsForHelper;
import ch.vd.uniregctb.type.GenreImpot;

public class EditForDebiteurValidator extends EditForAvecMotifsValidator {

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
		view.reinitReadOnlyData(fdpi); // on ré-initialise les données en lecture-seule parce qu'elles ne font pas partie du formulaire (et ne doivent pas l'être pour des raisons de sécurité)

		super.validate(target, errors);

		if (fdpi.getDateFin() == view.getDateFin() && fdpi.getMotifFermeture() == view.getMotifFin()) {
			errors.reject("global.error.aucun.changement");
		}
		else {
			// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification)
			final List<DateRange> fors = new ArrayList<>();
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

		// validation du motif de fin
		if (view.getMotifFin() != null) {
			final NatureTiers natureTiers = fdpi.getTiers().getNatureTiers();
			final MotifsForHelper.TypeFor typeFor = new MotifsForHelper.TypeFor(natureTiers, GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE, null);
			ForValidatorHelper.validateMotifFin(typeFor, view.getMotifFin(), errors);
		}
	}
}
