package ch.vd.uniregctb.migration.pm.engine.helpers;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.MigrationResultProduction;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.utils.DatesParticulieres;

/**
 * Bean Spring pour les contrôles sur les dates
 */
public class DateHelper {

	private DatesParticulieres datesParticulieres;

	public DateHelper(DatesParticulieres datesParticulieres) {
		this.datesParticulieres = datesParticulieres;
	}

	/**
	 * @param date date testée
	 * @return <code>true</code> si la date est non nulle et postérieure à la date du jour
	 */
	public boolean isFutureDate(@Nullable RegDate date) {
		return NullDateBehavior.EARLIEST.compare(RegDate.get(), date) < 0;
	}

	/**
	 * @param date date testée
	 * @return <code>true</code> si la date est non nulle et postérieure ou égale à la date du jour
	 */
	public boolean isFutureDateOrToday(@Nullable RegDate date) {
		return NullDateBehavior.EARLIEST.compare(RegDate.get(), date) <= 0;
	}

	/**
	 * @param date date testée
	 * @param description description à placer dans le log (avant " est antérieure au...")
	 * @param logCategory catégorie du log à utiliser
	 * @param mr collecteur de messages de log
	 */
	public void checkDateLouche(@Nullable RegDate date,
	                               Supplier<String> description,
	                               LogCategory logCategory,
	                               MigrationResultProduction mr) {

		// on loggue la date si elle est considérée comme anormale...
		final RegDate seuilDateNormale = datesParticulieres.getSeuilDateNormale();
		if (date != null && NullDateBehavior.LATEST.compare(date, seuilDateNormale) < 0) {
			mr.addMessage(logCategory, LogLevel.WARN,
			              String.format("%s est antérieure au %s (%s).",
			                            description.get(),
			                            StringRenderers.DATE_RENDERER.toString(seuilDateNormale),
			                            StringRenderers.DATE_RENDERER.toString(date)));
		}
	}
}
