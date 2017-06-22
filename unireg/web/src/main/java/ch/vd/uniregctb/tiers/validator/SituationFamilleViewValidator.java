package ch.vd.uniregctb.tiers.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.utils.ValidatorUtils;

public class SituationFamilleViewValidator implements Validator {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SituationFamilleView.class.equals(clazz) ;
	}

	@Override
	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		final SituationFamilleView situationFamilleView = (SituationFamilleView) obj;

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("dateDebut")) {
			if (situationFamilleView.getDateDebut() == null) {
				errors.rejectValue("dateDebut", "error.date.debut.vide");
			}
		}

		// [SIFISC-18086] blindage en cas de mauvais format de saisie, pour éviter le double message d'erreur
		if (!errors.hasFieldErrors("nombreEnfants")) {
			if (situationFamilleView.getNombreEnfants() == null) {
				errors.rejectValue("nombreEnfants", "error.nombre.enfants.vide");
			}
		}

		final ContribuableImpositionPersonnesPhysiques ctb = (ContribuableImpositionPersonnesPhysiques) tiersDAO.get(situationFamilleView.getNumeroCtb());
		final SituationFamille situationFamille = ctb.getSituationFamilleActive();
		if ((situationFamille != null) && (situationFamilleView.getDateDebut() != null)) {
			if (situationFamilleView.getDateDebut().isBefore(situationFamille.getDateDebut())) {
				errors.rejectValue("dateDebut", "error.date.debut.anterieure");
			}
		}

		if (situationFamilleView.getNombreEnfants() != null) {
			if (!ValidatorUtils.isPositiveInteger(situationFamilleView.getNombreEnfants().toString())) {
				errors.rejectValue("nombreEnfants", "error.nombre.enfants.invalide");
			}
		}

		// [SIFISC-21605] l'état civil est obligatoire, non ?
		if (situationFamilleView.getEtatCivil() == null) {
			errors.rejectValue("etatCivil", "error.etat.civil.obligatoire");
		}
	}

}
