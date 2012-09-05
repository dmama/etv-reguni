package ch.vd.uniregctb.di;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.uniregctb.di.manager.DeclarationImpotEditManager;
import ch.vd.uniregctb.di.view.ImprimerNouvelleDeclarationImpotView;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;

public class DeclarationImpotControllerValidator implements Validator {

	private TiersDAO tiersDAO;
	private DeclarationImpotEditManager manager;

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	public void setManager(DeclarationImpotEditManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return ImprimerNouvelleDeclarationImpotView.class.equals(clazz);
	}

	@Override
	@Transactional(readOnly = true, rollbackFor = Throwable.class)
	public void validate(Object target, Errors errors) {
		final ImprimerNouvelleDeclarationImpotView view = (ImprimerNouvelleDeclarationImpotView) target;

		// Vérifie que les paramètres reçus sont valides

		final Tiers tiers = tiersDAO.get(view.getTiersId());
		if (tiers == null) {
			errors.reject("error.tiers.inexistant");
			return;
		}

		if (!(tiers instanceof Contribuable)) {
			errors.reject("error.tiers.doit.etre.contribuable");
			return;
		}

		final Contribuable ctb = (Contribuable) tiers;

		if (view.getDateDebutPeriodeImposition() == null) {
			errors.rejectValue("dateDebutPeriodeImposition", "error.date.debut.vide");
		}
		else if (view.getDateFinPeriodeImposition() == null) {
			errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.vide");
		}
		else if (view.getDateDebutPeriodeImposition().year() == RegDate.get().year()) {
			// si la période est ouverte les dates sont libres... dans la limite des valeurs raisonnables
			if (view.getDateDebutPeriodeImposition().isAfter(view.getDateFinPeriodeImposition())) {
				errors.rejectValue("dateFinPeriodeImposition", "error.date.fin.avant.debut");
			}
			else if (view.getDateDebutPeriodeImposition().year() != view.getDateFinPeriodeImposition().year()) {
				errors.rejectValue("dateFinPeriodeImposition", "error.declaration.cheval.plusieurs.annees");
			}
		}
		else {
			try {
				manager.checkRangeDi(ctb, new DateRangeHelper.Range(view.getDateDebutPeriodeImposition(), view.getDateFinPeriodeImposition()));
			}
			catch (ValidationException e) {
				errors.reject(e.getMessage());
			}
		}
	}
}
