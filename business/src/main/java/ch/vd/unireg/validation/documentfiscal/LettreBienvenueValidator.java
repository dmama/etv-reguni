package ch.vd.unireg.validation.documentfiscal;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.documentfiscal.LettreBienvenue;

/**
 * Validateur des lettres de bienvenue
 */
public class LettreBienvenueValidator extends AutreDocumentFiscalAvecSuiviValidator<LettreBienvenue> {

	@Override
	protected Class<LettreBienvenue> getValidatedClass() {
		return LettreBienvenue.class;
	}

	@Override
	@NotNull
	public ValidationResults validate(@NotNull LettreBienvenue entity) {
		final ValidationResults vr = super.validate(entity);
		if (!entity.isAnnule()) {

			// le type est obligatoire sur les lettres de bienvenue
			if (entity.getType() == null) {
				vr.addError("Le type de lettre de bienvenue est obligatoire.");
			}

		}
		return vr;
	}
}
