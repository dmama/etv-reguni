package ch.vd.uniregctb.migration.pm.engine.helpers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.migration.pm.regpm.NumeroIDE;
import ch.vd.uniregctb.migration.pm.utils.EntityWrapper;
import ch.vd.uniregctb.tiers.LocalizedDateRange;
import ch.vd.uniregctb.tiers.MontantMonetaire;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.DayMonthHelper;

/**
 * Quelques implémentation de {@link ch.vd.uniregctb.common.StringRenderer} bien pratiques
 */
public abstract class StringRenderers {

	/**
	 * Entité qui permet de dumper une valeur de date dans un format lisible (et commun...)
	 */
	public static final StringRenderer<RegDate> DATE_RENDERER = date -> StringUtils.defaultIfBlank(RegDateHelper.dateToDisplayString(date), "?");

	/**
	 * Entité qui permet de dumper des valeurs de ranges dans un format lisible
	 */
	public static final StringRenderer<DateRange> DATE_RANGE_RENDERER =
			range -> String.format("[%s -> %s]", DATE_RENDERER.toString(range.getDateDebut()), DATE_RENDERER.toString(range.getDateFin()));

	/**
	 * Entité qui permet de dumper des valeurs de {@link DayMonth}
	 */
	public static final StringRenderer<DayMonth> DAYMONTH_RENDERER = dm -> StringUtils.defaultIfBlank(DayMonthHelper.toDisplayString(dm), "?");

	/**
	 * Entité qui permet de dumper des valeurs de {@link LocalizedDateRange}
	 */
	public static final StringRenderer<LocalizedDateRange> LOCALISATION_DATEE_RENDERER =
			ld -> String.format("%s %s sur %s/%d",
			                    (ld instanceof EntityWrapper ? ((EntityWrapper<?>) ld).getWrappedEntity() : ld).getClass().getSimpleName(),
			                    DATE_RANGE_RENDERER.toString(ld),
			                    ld.getTypeAutoriteFiscale(),
			                    ld.getNumeroOfsAutoriteFiscale());

	/**
	 * Entité qui permet de dumper des valeurs de {@link MontantMonetaire}
	 */
	public static final StringRenderer<MontantMonetaire> MONTANT_MONETAIRE_RENDERER = mm -> String.format("%d %s", mm.getMontant(), mm.getMonnaie());

	/**
	 * Dump d'un {@link NumeroIDE} sous sa forme canonique LLLCCCCCCCCC
	 */
	public static final StringRenderer<NumeroIDE> NUMERO_IDE_CANONICAL_RENDERER = ide -> String.format("%s%09d", ide.getCategorie(), ide.getNumero());

	/**
	 * Dump d'un {@link NumeroIDE} sous sa forme affichable LLL-CCC.CCC.CCC
	 */
	public static final StringRenderer<NumeroIDE> NUMERO_IDE_RENDERER = ide -> FormatNumeroHelper.formatNumIDE(NUMERO_IDE_CANONICAL_RENDERER.toString(ide));
}
