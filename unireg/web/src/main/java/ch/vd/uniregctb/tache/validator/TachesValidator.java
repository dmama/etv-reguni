package ch.vd.uniregctb.tache.validator;

import ch.vd.uniregctb.common.DelegatingValidator;
import ch.vd.uniregctb.tache.view.ImpressionNouveauxDossiersView;
import ch.vd.uniregctb.tache.view.NouveauDossierCriteriaView;
import ch.vd.uniregctb.tache.view.TacheCriteriaView;

public class TachesValidator extends DelegatingValidator {

	public TachesValidator() {
		addSubValidator(TacheCriteriaView.class, new TacheListValidator());
		addSubValidator(NouveauDossierCriteriaView.class, new DummyValidator<>(NouveauDossierCriteriaView.class));
		addSubValidator(ImpressionNouveauxDossiersView.class, new DummyValidator<>(ImpressionNouveauxDossiersView.class));
	}
}
