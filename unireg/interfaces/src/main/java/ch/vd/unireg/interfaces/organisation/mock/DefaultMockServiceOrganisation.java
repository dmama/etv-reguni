package ch.vd.unireg.interfaces.organisation.mock;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;

public class DefaultMockServiceOrganisation extends MockServiceOrganisation {
	@Override
	protected void init() {

	}

	@Override
	public void ping() throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}
}
