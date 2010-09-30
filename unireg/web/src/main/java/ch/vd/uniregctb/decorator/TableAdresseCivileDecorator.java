package ch.vd.uniregctb.decorator;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.tiers.view.AdresseView;

public class TableAdresseCivileDecorator extends TableEntityDecorator {

	public String addRowClass() {

		final String superClassAddRowClass = super.addRowClass();
		final StringBuilder b = new StringBuilder(superClassAddRowClass != null ? superClassAddRowClass : StringUtils.EMPTY);

		final Object rowObject = getCurrentRowObject();
		if (rowObject instanceof AdresseView) {
			final AdresseView adresseView = (AdresseView) rowObject;
			if (!adresseView.isSurVaud()) {
				b.append(" horscanton");
			}
		}

		return b.length() > 0 ? b.toString().trim() : null;
	}
}
