package ch.vd.uniregctb.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.TiersNotFoundException;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.manager.AutorisationManager;

public class AddForPrincipalValidator extends AddForRevenuFortuneValidator {

	private final HibernateTemplate hibernateTemplate;
	private final AutorisationManager autorisationManager;

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

		// validation du mode d'imposition (seulement pour les contribuables PP, puisque cet attribut n'a aucun sens pour les autres...)
		if (ctb instanceof ContribuableImpositionPersonnesPhysiques) {
			if (view.getModeImposition() == null) {
				errors.rejectValue("modeImposition", "error.mode.imposition.incorrect");
			}
			else {
				final StringBuilder messageErreurModeImposition = new StringBuilder();
				if (!autorisationManager.isModeImpositionAllowed(ctb, view.getModeImposition(), view.getTypeAutoriteFiscale(), view.getMotifRattachement(), view.getDateDebut(),
				                                                 AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID(), messageErreurModeImposition)) {
					errors.rejectValue("modeImposition", messageErreurModeImposition.toString());
				}
			}
		}

		// on établi la liste des périodes des fors fiscaux existants
		final List<DateRange> fors = new ArrayList<>();
		for (ForFiscal f : ctb.getForsFiscauxPrincipauxActifsSorted()) {
			if (f.getDateFin() == null && view.getDateDebut() != null && f.getDateDebut().isBefore(view.getDateDebut())) {
				// simule la fermeture du for courant à la veille du nouveau for
				fors.add(new DateRangeHelper.Range(f.getDateDebut(), view.getDateDebut().getOneDayBefore()));
			}
			else {
				fors.add(new DateRangeHelper.Range(f));
			}
		}
		if (DateRangeHelper.intersect(new DateRangeHelper.Range(view.getDateDebut(), view.getDateFin()), fors)) {
			// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
			if (!errors.hasFieldErrors("dateDebut")) {
				errors.rejectValue("dateDebut", "error.date.chevauchement");
			}
			if (!errors.hasFieldErrors("dateFin")) {
				errors.rejectValue("dateFin", "error.date.chevauchement");
			}
		}
	}
}
