package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AddForDebiteurValidator extends AddForValidator {

	private HibernateTemplate hibernateTemplate;

	public AddForDebiteurValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate) {
		super(infraService);
		this.hibernateTemplate = hibernateTemplate;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForDebiteurView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForDebiteurView view =(AddForDebiteurView) target;

		if (view.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {
			errors.rejectValue("typeAutoriteFiscale", "error.type.autorite.incorrect");
		}

		if (view.getDateDebut() != null) {
			final DebiteurPrestationImposable dpi = hibernateTemplate.get(DebiteurPrestationImposable.class, view.getTiersId());
			if (dpi != null) {
				// on établi la liste des périodes des fors fiscaux existants
				final List<DateRange> fors = new ArrayList<DateRange>();
				for (ForFiscal f : dpi.getForsFiscauxNonAnnules(true)) {
					if (f.getDateFin() == null && (f.getDateDebut() == null || f.getDateDebut().isBefore(view.getDateDebut()))) {
						// simule la fermeture du for courant à la veille du nouveau for
						fors.add(new DateRangeHelper.Range(f.getDateDebut(), view.getDateDebut().getOneDayBefore()));
					}
					else {
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
}
