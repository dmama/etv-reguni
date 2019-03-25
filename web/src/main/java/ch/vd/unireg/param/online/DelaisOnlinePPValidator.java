package ch.vd.unireg.param.online;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.param.view.DelaisAccordablesOnlinePPView;
import ch.vd.unireg.type.DayMonth;

public class DelaisOnlinePPValidator implements Validator {

	private PeriodeFiscaleDAO periodeFiscaleDAO;

	public void setPeriodeFiscaleDAO(PeriodeFiscaleDAO periodeFiscaleDAO) {
		this.periodeFiscaleDAO = periodeFiscaleDAO;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return clazz == DelaisOnlinePPView.class;
	}

	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	@Override
	public void validate(Object target, Errors errors) {
		final DelaisOnlinePPView view = (DelaisOnlinePPView) target;

		final PeriodeFiscale pf = periodeFiscaleDAO.get(view.getPeriodeFiscaleId());
		if (pf == null) {
			throw new ObjectNotFoundException("Impossible de retrouver la période fiscale id : " + view.getPeriodeFiscaleId());
		}

		final List<DelaisAccordablesOnlinePPView> periodes = view.getPeriodes();
		if (periodes != null) {

			RegDate previous = null;
			for (int i = 0; i < periodes.size(); i++) {
				final DelaisAccordablesOnlinePPView periode = periodes.get(i);
				final RegDate dateDebut = periode.getDateDebut();

				// on s'assure que toutes les périodes possèdes des dates de début non nulles
				if (dateDebut == null) {
					if (!errors.hasFieldErrors("periodes[" + i + "].dateDebut")) {  // [FISCPROJ-1077] inutile d'agonir l'utilisateur avec plusieurs erreurs
						errors.rejectValue("periodes[" + i + "].dateDebut", "error.date.debut.vide");
					}
					continue;
				}

				// on s'assure que les périodes sont triées par ordre chronologique croissant
				if (previous != null) {
					if (dateDebut.isBeforeOrEqual(previous)) {
						errors.rejectValue("periodes[" + i + "].dateDebut", "error.date.debut.anterieure.egale");
					}
				}
				previous = dateDebut;

				// on s'assure que toutes les dates de début sont situées dans l'année + 1 de la période fiscale
				if (dateDebut.year() != pf.getAnnee() + 1) {
					errors.rejectValue("periodes[" + i + "].dateDebut", "error.date.debut.delai.hors.pf", new String[]{String.valueOf(pf.getAnnee() + 1), String.valueOf(pf.getAnnee())}, null);
				}
			}

			// on s'assure que les délais 2 sont vides si les délais 1 sont vides aussi
			// on s'assure que les délais 2 sont après les délais 1 s'ils sont renseignés
			for (int i = 0; i < periodes.size(); i++) {
				final DelaisAccordablesOnlinePPView periode = periodes.get(i);
				final DayMonth delai1DemandeUnitaire = periode.getDelai1DemandeUnitaire();
				final DayMonth delai2DemandeUnitaire = periode.getDelai2DemandeUnitaire();
				if (delai1DemandeUnitaire == null && delai2DemandeUnitaire != null) {
					errors.rejectValue("periodes[" + i + "].delai2DemandeUnitaire", "error.delai2.renseigne.mais.pas.delai1");
				}
				else if (delai1DemandeUnitaire != null && delai2DemandeUnitaire != null && delai2DemandeUnitaire.compareTo(delai1DemandeUnitaire) <= 0) {
					errors.rejectValue("periodes[" + i + "].delai2DemandeUnitaire", "error.delai2.egale.anterieur.delai1");
				}

				final DayMonth delai1DemandeGroupee = periode.getDelai1DemandeGroupee();
				final DayMonth delai2DemandeGroupee = periode.getDelai2DemandeGroupee();
				if (delai1DemandeGroupee == null && delai2DemandeGroupee != null) {
					errors.rejectValue("periodes[" + i + "].delai2DemandeGroupee", "error.delai2.renseigne.mais.pas.delai1");
				}
				else if (delai1DemandeGroupee != null && delai2DemandeGroupee != null && delai2DemandeGroupee.compareTo(delai1DemandeGroupee) <= 0) {
					errors.rejectValue("periodes[" + i + "].delai2DemandeGroupee", "error.delai2.egale.anterieur.delai1");
				}
			}
		}
	}
}
