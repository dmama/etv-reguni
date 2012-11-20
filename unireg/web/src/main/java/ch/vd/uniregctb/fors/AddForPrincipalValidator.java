package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;

public class AddForPrincipalValidator extends AddForRevenuFortuneValidator {

	private HibernateTemplate hibernateTemplate;
	private AutorisationManager autorisationManager;

	public AddForPrincipalValidator(ServiceInfrastructureService infraService, HibernateTemplate hibernateTemplate, AutorisationManager autorisationManager) {
		super(infraService, hibernateTemplate);
		this.hibernateTemplate = hibernateTemplate;
		this.autorisationManager = autorisationManager;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return AddForPrincipalView.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);

		final AddForPrincipalView view = (AddForPrincipalView) target;

		final Contribuable ctb = hibernateTemplate.get(Contribuable.class, view.getTiersId());
		if (ctb == null) {
			throw new TiersNotFoundException(view.getTiersId());
		}

		// validation du mode d'imposition
		if (view.getModeImposition() == null) {
			errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
		}
		else {
			if (!autorisationManager.isModeImpositionAllowed(ctb, view.getModeImposition(), view.getTypeAutoriteFiscale(), view.getMotifRattachement(), view.getDateDebut(),
			                                                 AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID())) {
				errors.rejectValue("modeImposition", "error.mode.imposition.interdit");
			}
		}

		// on établi la liste des périodes des fors fiscaux existants
		final List<DateRange> fors = new ArrayList<DateRange>();
		for (ForFiscal f : ctb.getForsFiscauxPrincipauxActifsSorted()) {
			if (f.getDateFin() == null && f.getDateDebut().isBefore(view.getDateDebut())) {
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
