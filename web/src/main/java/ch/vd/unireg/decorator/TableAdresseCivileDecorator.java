package ch.vd.unireg.decorator;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.tiers.view.AdresseView;

public class TableAdresseCivileDecorator extends TableEntityDecorator {

	@Override
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
