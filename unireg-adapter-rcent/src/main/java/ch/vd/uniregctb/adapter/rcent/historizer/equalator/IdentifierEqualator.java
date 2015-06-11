package ch.vd.uniregctb.adapter.rcent.historizer.equalator;

import ch.vd.evd0022.v1.Identifier;

public class IdentifierEqualator implements Equalator<Identifier> {

	@Override
	public boolean test(Identifier id1, Identifier id2) {
		if (id1 == id2) return true;
		if (id2 == null || id1.getClass() != id2.getClass()) return false;

		if (!id1.getIdentifierCategory().equals(id2.getIdentifierCategory())) return false;
		return id1.getIdentifierValue().equals(id2.getIdentifierValue());
	}
}
