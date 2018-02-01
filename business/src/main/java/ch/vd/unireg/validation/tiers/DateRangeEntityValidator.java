package ch.vd.unireg.validation.tiers;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.common.Annulable;
import ch.vd.unireg.validation.EntityValidatorImpl;

public abstract class DateRangeEntityValidator<T extends DateRange> extends EntityValidatorImpl<T> {

	@Override
	public ValidationResults validate(T entity) {
		final ValidationResults results = new ValidationResults();

		// une entité annulée est toujours valide...
		if (entity instanceof Annulable && ((Annulable) entity).isAnnule()) {
			return results;
		}

		final RegDate dateDebut = entity.getDateDebut();
		final RegDate dateFin = entity.getDateFin();

		// La date de début doit être renseignée
		if (dateDebut == null && !isDateDebutNullAllowed()) {
			results.addError(String.format("%s %s possède une date de début nulle", getEntityCategoryName(), getEntityDisplayString(entity)));
		}
		else if (dateDebut != null && !isDateDebutFutureAllowed() && dateDebut.isAfter(getFutureBeginDate())) {
			// la date de début d'une entité ne doit en aucun cas être dans le futur !
			results.addError(String.format("%s %s possède une date de début dans le futur", getEntityCategoryName(), getEntityDisplayString(entity)));
		}
		if (dateFin == null && !isDateFinNullAllowed()) {
			results.addError(String.format("%s %s possède une date de fin nulle", getEntityCategoryName(), getEntityDisplayString(entity)));
		}
		else if (dateFin != null && !isDateFinFutureAllowed() && dateFin.isAfter(getFutureBeginDate())) {
			// la date de fin non plus, ne doit jamais être dans le futur !
			results.addError(String.format("%s %s possède une date de fin dans le futur", getEntityCategoryName(), getEntityDisplayString(entity)));
		}

		// Date de début doit être avant la date de fin
		// Si "date de début" = "date de fin", c'est un cas OK (durée d'un jour)
		if (dateDebut != null && dateFin != null && dateDebut.isAfter(dateFin)) {
			results.addError(String.format("%s %s possède une date de début qui est après la date de fin: début = %s, fin = %s",
			                               getEntityCategoryName(), getEntityDisplayString(entity), RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin)));
		}

		return results;
	}

	/**
	 * Interface utilisée pour faire en sorte que la validation d'une décision soit faite
	 * en supposant une date du jour spécifique (pour les tests)
	 */
	private interface ReferenceDateAccessor {
		RegDate getReferenceDate();
	}

	private static final ReferenceDateAccessor CURRENT_DATE_ACCESSOR = RegDate::get;

	private static final ThreadLocal<ReferenceDateAccessor> futureBeginDateAccessors = ThreadLocal.withInitial(() -> CURRENT_DATE_ACCESSOR);

	/**
	 * Méthode utilisable dans les tests et qui fait en sorte que la date de "début du futur" soit la date donnée
	 * @param date date qui devra dorénavant être considérée comme la date du jour pour ce qui concerne la validation des décisions
	 */
	public static void setFutureBeginDate(@Nullable final RegDate date) {
		if (date == null) {
			futureBeginDateAccessors.remove();
		}
		else {
			futureBeginDateAccessors.set(() -> date);
		}
	}

	private static ReferenceDateAccessor getFutureBeginDateAccessor() {
		return futureBeginDateAccessors.get();
	}

	protected static RegDate getFutureBeginDate() {
		final ReferenceDateAccessor accessor = getFutureBeginDateAccessor();
		return accessor.getReferenceDate();
	}

	protected static String rangeToString(@NotNull DateRange range) {
		return String.format("%s - %s",
		                     StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(range.getDateDebut()), "?"),
		                     StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(range.getDateFin()), "?"));
	}

	/**
	 * @return <code>true</code> si la date de début est autorisée dans le futur (faux par défaut)
	 */
	protected boolean isDateDebutFutureAllowed() {
		return false;
	}

	/**
	 * @return <code>true</code> si la date de début peut être omise (faux par défaut)
	 */
	protected boolean isDateDebutNullAllowed() {
		return false;
	}

	/**
	 * [SIFISC-10141] Certains fors fiscaux (sur les DPI) peuvent avoir des dates de fermeture dans le futur
	 * @return <code>false</code> par défaut, surchargeable
	 */
	protected boolean isDateFinFutureAllowed() {
		return false;
	}

	/**
	 * @return <code>true</code> si la date de fin peut être omise (vrai par défaut)
	 */
	protected boolean isDateFinNullAllowed() {
		return true;
	}

	/**
	 * @return une chaîne comme "Le for fiscal", ou "La décision ACI", ou...
	 */
	protected abstract String getEntityCategoryName();

}
