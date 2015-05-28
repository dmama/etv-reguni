package ch.vd.uniregctb.migration.pm.historizer.equalator;

import ch.vd.evd0022.v1.Identifier;

public class IdentifierEqualator implements Equalator<Identifier> {

	@Override
	public boolean test(Identifier identifier, Identifier identifier2) {
		return identifier.getIdentifierCategory().equals(identifier2.getIdentifierCategory());
	}
}
