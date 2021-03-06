package ch.vd.unireg.validation.declaration;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.declaration.QuestionnaireSNC;

public class QuestionnaireSNCValidator extends DeclarationValidator<QuestionnaireSNC> {

	@Override
	protected String getEntityCategoryName() {
		return "Le questionnaire SNC";
	}

	@Override
	protected Class<QuestionnaireSNC> getValidatedClass() {
		return QuestionnaireSNC.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull QuestionnaireSNC q) {
		final ValidationResults vr = super.validate(q);
		if (!q.isAnnule()) {
			if (q.getPeriode() == null) {
				vr.addError("La période ne peut pas être nulle.");
			}

			if (q.getNumero() == null) {
				vr.addError("Le numéro de séquence du questionnaire ne peut pas être nul.");
			}
		}
		return vr;
	}
}
