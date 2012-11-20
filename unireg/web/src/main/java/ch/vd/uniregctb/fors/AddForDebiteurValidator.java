package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class AddForDebiteurValidator extends AddForValidator {

	private TiersDAO tiersDAO;

	protected AddForDebiteurValidator(ServiceInfrastructureService infraService, TiersDAO tiersDAO) {
		super(infraService);
		this.tiersDAO = tiersDAO;
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

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersDAO.get(view.getTiersId());
		if (dpi != null) {
			// on établi la liste des périodes des fors fiscaux existants (sans prendre en compte le for en cours de modification s'il s'agit d'une modification)
			final List<DateRange> fors = new ArrayList<DateRange>();
			for (ForFiscal f : dpi.getForsFiscauxNonAnnules(true)) {
				if (view.getId() == null || !view.getId().equals(f.getId())) {
					if (view.getId() == null && f.getDateFin() == null && f.getDateDebut().isBefore(view.getDateDebut())) {
						// simule la fermeture du for courant à la veille du nouveau for
						fors.add(new DateRangeHelper.Range(f.getDateDebut(), view.getDateDebut().getOneDayBefore()));
					}
					else {
						fors.add(new DateRangeHelper.Range(f));
					}
				}
			}
			if (DateRangeHelper.intersect(new DateRangeHelper.Range(view.getDateDebut(), view.getDateFin()), fors)) {
				errors.rejectValue("dateDebut", "error.date.chevauchement");
				errors.rejectValue("dateFin", "error.date.chevauchement");
			}
		}
	}
}
