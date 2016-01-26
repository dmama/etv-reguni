package ch.vd.uniregctb.decorator;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.tiers.view.AllegementFiscalView;

public class TableAllegementDecorator extends TableEntityDecorator {

	@Override
	public String addRowClass() {
		final String base = super.addRowClass();
		final StringBuilder b = new StringBuilder(StringUtils.trimToEmpty(base));
		final Object rowObject = getCurrentRowObject();
		if (rowObject instanceof AllegementFiscalView) {
			final AllegementFiscalView view = (AllegementFiscalView) rowObject;
			if (view.isAnnule() || RegDateHelper.isBefore(view.getDateFin(), RegDate.get(), NullDateBehavior.LATEST)) {
				b.append(" histo-only");
			}
		}
		return StringUtils.trimToEmpty(b.toString());
	}
}
