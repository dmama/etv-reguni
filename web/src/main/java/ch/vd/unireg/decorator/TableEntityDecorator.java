package ch.vd.unireg.decorator;

import org.apache.commons.lang3.StringUtils;
import org.displaytag.decorator.TableDecorator;

import ch.vd.unireg.common.Annulable;

public class TableEntityDecorator extends TableDecorator {

	@Override
	public String addRowClass() {

		final String superClassAddRowClass = super.addRowClass();
		final StringBuilder b = new StringBuilder(superClassAddRowClass != null ? superClassAddRowClass : StringUtils.EMPTY);

		final Object rowObject = getCurrentRowObject();
		if (rowObject instanceof Annulable) {
			final Annulable annulable = (Annulable) rowObject;
			if (annulable.isAnnule()) {
				b.append(" strike");
			}
		}

		return b.length() > 0 ? b.toString().trim() : null;
	}
}
