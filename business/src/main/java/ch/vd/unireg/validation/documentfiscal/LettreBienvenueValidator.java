package ch.vd.uniregctb.validation.documentfiscal;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.documentfiscal.LettreBienvenue;

/**
 * Validateur des lettres de bienvenue
 */
public class LettreBienvenueValidator extends AutreDocumentFiscalAvecSuiviValidator<LettreBienvenue> {

	@Override
	protected Class<LettreBienvenue> getValidatedClass() {
		return LettreBienvenue.class;
	}

	@Override
	public ValidationResults validate(LettreBienvenue entity) {
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
