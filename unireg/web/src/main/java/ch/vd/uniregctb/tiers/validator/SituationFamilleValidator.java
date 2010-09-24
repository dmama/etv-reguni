package ch.vd.uniregctb.tiers.validator;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.view.SituationFamilleView;
import ch.vd.uniregctb.utils.ValidateHelper;

public class SituationFamilleValidator implements Validator {

	private TiersDAO tiersDAO;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return SituationFamilleView.class.equals(clazz) ;
	}

	@Transactional(readOnly = true)
	public void validate(Object obj, Errors errors) {
		SituationFamilleView situationFamilleView = (SituationFamilleView) obj;

		if (situationFamilleView.getDateDebut() == null) {
			errors.rejectValue("dateDebut", "error.date.debut.vide");
		}

		if (situationFamilleView.getNombreEnfants() == null) {
			errors.rejectValue("nombreEnfants", "error.nombre.enfants.vide");
		}

		Contribuable ctb = (Contribuable) tiersDAO.get(situationFamilleView.getNumeroCtb());
		SituationFamille situationFamille = ctb.getSituationFamilleActive();
		if ((situationFamille != null) && (situationFamilleView.getDateDebut() != null)) {
			if (situationFamilleView.getDateDebut().before(RegDate.asJavaDate(situationFamille.getDateDebut()))) {
				errors.rejectValue("dateDebut", "error.date.debut.anterieure");
			}
		}

		if (situationFamilleView.getNombreEnfants() != null) {
			if (!ValidateHelper.isPositiveInteger(situationFamilleView.getNombreEnfants().toString())) {
				errors.rejectValue("nombreEnfants", "error.nombre.enfants.invalide");
			}
		}
	}

}
