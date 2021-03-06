package ch.vd.unireg.fors;

import org.springframework.validation.Errors;

import ch.vd.unireg.tiers.validator.MotifsForHelper;
import ch.vd.unireg.type.MotifFor;

public abstract class ForValidatorHelper {

	public static void validateMotifDebut(MotifsForHelper.TypeFor typeFor, MotifFor motifDebut, Errors errors) {
		if (!MotifsForHelper.getMotifsOuverture(typeFor).contains(motifDebut)) {
			errors.rejectValue("motifDebut", "error.motif.ouverture.invalide");
		}
	}

	public static void validateMotifFin(MotifsForHelper.TypeFor typeFor, MotifFor motifFin, Errors errors) {
		if (!MotifsForHelper.getMotifsFermeture(typeFor).contains(motifFin)) {
			errors.rejectValue("motifFin", "error.motif.fermeture.invalide");
		}
	}
}
