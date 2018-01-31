package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.equalator;

import ch.ech.ech0097.v2.NamedOrganisationId;

import ch.vd.uniregctb.common.Equalator;

public class NamedOrganisationIdEqualator implements Equalator<NamedOrganisationId> {

	@Override
	public boolean test(NamedOrganisationId id1, NamedOrganisationId id2) {
		if (id1 == id2) return true;
		if (id2 == null || id1.getClass() != id2.getClass()) return false;

		if (!id1.getOrganisationIdCategory().equals(id2.getOrganisationIdCategory())) return false;
		return id1.getOrganisationId().equals(id2.getOrganisationId());
	}
}
