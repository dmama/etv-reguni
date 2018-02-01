package ch.vd.unireg.acces;

import ch.vd.unireg.acces.copie.validator.SelectUtilisateursValidator;
import ch.vd.unireg.acces.copie.view.ConfirmCopieView;
import ch.vd.unireg.acces.copie.view.ConfirmedDataView;
import ch.vd.unireg.acces.copie.view.SelectUtilisateursView;
import ch.vd.unireg.acces.parDossier.validator.DroitAccesEditValidator;
import ch.vd.unireg.acces.parDossier.view.DossierEditRestrictionView;
import ch.vd.unireg.acces.parDossier.view.DroitAccesView;
import ch.vd.unireg.acces.parUtilisateur.validator.SelectUtilisateurValidator;
import ch.vd.unireg.acces.parUtilisateur.view.RecapPersonneUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.SelectUtilisateurView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurEditRestrictionView;
import ch.vd.unireg.acces.parUtilisateur.view.UtilisateurListPersonneView;
import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.tiers.validator.TiersCriteriaValidator;
import ch.vd.unireg.tiers.view.TiersCriteriaView;

public class DroitAccesValidator extends DelegatingValidator {

	public DroitAccesValidator() {
		// par-dossier
		addSubValidator(TiersCriteriaView.class, new TiersCriteriaValidator());
		addSubValidator(DossierEditRestrictionView.class, new DummyValidator<>(DossierEditRestrictionView.class));
		addSubValidator(DroitAccesView.class, new DroitAccesEditValidator());

		// par-utilisateur
		addSubValidator(SelectUtilisateurView.class, new SelectUtilisateurValidator());
		addSubValidator(UtilisateurEditRestrictionView.class, new DummyValidator<>(UtilisateurEditRestrictionView.class));
		addSubValidator(UtilisateurListPersonneView.class, new TiersCriteriaValidator());
		addSubValidator(RecapPersonneUtilisateurView.class, new DummyValidator<>(RecapPersonneUtilisateurView.class));

		// copie-transfert
		addSubValidator(SelectUtilisateursView.class, new SelectUtilisateursValidator());
		addSubValidator(ConfirmCopieView.class, new DummyValidator<>(ConfirmCopieView.class));
		addSubValidator(ConfirmedDataView.class, new DummyValidator<>(ConfirmedDataView.class));
	}
}
