package ch.vd.unireg.tache.validator;

import ch.vd.unireg.common.DelegatingValidator;
import ch.vd.unireg.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.unireg.tache.view.NouveauDossierCriteriaView;
import ch.vd.unireg.tache.view.TacheCriteriaView;

public class TachesValidator extends DelegatingValidator {

	public TachesValidator() {
		addSubValidator(TacheCriteriaView.class, new TacheListValidator());
		addSubValidator(NouveauDossierCriteriaView.class, new DummyValidator<>(NouveauDossierCriteriaView.class));
		addSubValidator(ImpressionNouveauxDossiersView.class, new DummyValidator<>(ImpressionNouveauxDossiersView.class));
	}
}
