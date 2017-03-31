package ch.vd.uniregctb.decorator;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;

/**
 * Décorateur utilisable pour les entités
 * <ul>
 *     <li>implémentant {@link Annulable}</li>
 *     <li>implémentant {@link DateRange}</li>
 * </ul>
 * Si l'entité est annulée, la ligne (tr) portera les classes "strike" et "histo-only";<br/>
 * Si l'entité n'est pas annulée, mais a une date de fin dans le passé, la ligne (tr) portera la classe "histo-only", ce qui permettra de n'afficher, en contrôlant la présence
 * (ou l'absence) de cette classe, que les entités actuellement actives ou encore à venir
 */
public class TableAnnulableDateRangeDecorator extends TableEntityDecorator {

	@Override
	public String addRowClass() {
		final StringBuilder b = new StringBuilder(StringUtils.trimToEmpty(super.addRowClass()));
		final Object rowObject = getCurrentRowObject();
		final boolean canceled = rowObject instanceof Annulable && ((Annulable) rowObject).isAnnule();
		final boolean past = rowObject instanceof DateRange && RegDateHelper.isBefore(((DateRange) rowObject).getDateFin(), RegDate.get(), NullDateBehavior.LATEST);
		if (canceled || past) {
			b.append(" histo-only");
		}
		return StringUtils.trimToEmpty(b.toString());
	}
}
