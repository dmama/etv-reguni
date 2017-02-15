package ch.vd.uniregctb.validation.registrefoncier;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

public class ImmeubleRFValidator extends EntityValidatorImpl<ImmeubleRF> {
	@Override
	protected Class<ImmeubleRF> getValidatedClass() {
		return ImmeubleRF.class;
	}

	@Override
	public ValidationResults validate(ImmeubleRF immeuble) {

		final ValidationResults results = new ValidationResults();

		// un immeuble annulée est toujours valide...
		if (immeuble.isAnnule()) {
			return results;
		}

		if (immeuble.getDateRadiation() != null) {
			// l'immeuble est radié, tous les droits, surfaces, etc... devraient être fermées.
			validateCollectionFermee(results, immeuble.getEstimations(), "L'immeuble radié avec l'idRF=[" + immeuble.getIdRF() + "] possède une estimation fiscale active");
			validateCollectionFermee(results, immeuble.getImplantations(), "L'immeuble radié avec l'idRF=[" + immeuble.getIdRF() + "] possède un implantation active");
			validateCollectionFermee(results, immeuble.getSituations(), "L'immeuble radié avec l'idRF=[" + immeuble.getIdRF() + "] possède une situation active");
			validateCollectionFermee(results, immeuble.getSurfacesAuSol(), "L'immeuble radié avec l'idRF=[" + immeuble.getIdRF() + "] possède une surface au sol active");
			validateCollectionFermee(results, immeuble.getSurfacesTotales(), "L'immeuble radié avec l'idRF=[" + immeuble.getIdRF() + "] possède une surface totale active");
		}

		return results;
	}

	private static <T extends DateRange> void validateCollectionFermee(@NotNull ValidationResults results, @Nullable Set<T> set, String message) {
		if (set != null) {
			set.stream()
					.filter(e -> !(e instanceof Annulable) || ((Annulable) e).isNotAnnule())
					.filter(e -> e.getDateFin() == null)
					.findAny()
					.ifPresent(e -> results.addError(message));
		}
	}
}
