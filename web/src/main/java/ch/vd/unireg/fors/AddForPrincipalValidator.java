package ch.vd.unireg.fors;

import java.util.ArrayList;
import java.util.List;

import org.springframework.validation.Errors;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.TiersNotFoundException;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.manager.AutorisationManager;
import ch.vd.unireg.tiers.manager.AutorisationManagerImpl;
import ch.vd.unireg.tiers.manager.RetourModeImpositionAllowed;

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
				final String messageErreurModeImposition;
				RetourModeImpositionAllowed allowed = autorisationManager.isModeImpositionAllowed(ctb, view.getModeImposition(), view.getTypeAutoriteFiscale(), view.getMotifRattachement(), view.getDateDebut(),
				                                                                                  AuthenticationHelper.getCurrentPrincipal(), AuthenticationHelper.getCurrentOID());
				switch (allowed) {
				case INTERDIT:
					messageErreurModeImposition = "error.mode.imposition.interdit";
					break;
				case DROITS_INCOHERENTS:
					messageErreurModeImposition = "error.for.principal.droits.incoherents";
					break;
				case REGLES_INCOHERENTES:
					messageErreurModeImposition = "error.mode.imposition.regles.incoherentes";
					break;
				case OK:
					messageErreurModeImposition = "";
					break;
				default:
					throw new IllegalArgumentException("Type de retour sur le mode d'imposition inconnu = [ "+ allowed +" ]");
				}

				if (allowed != RetourModeImpositionAllowed.OK) {
					errors.rejectValue("modeImposition", messageErreurModeImposition);
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
