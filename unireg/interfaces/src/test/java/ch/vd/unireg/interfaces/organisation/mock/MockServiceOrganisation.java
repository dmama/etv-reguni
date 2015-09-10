package ch.vd.unireg.interfaces.organisation.mock;

import java.util.HashMap;
import java.util.Map;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;

public abstract class MockServiceOrganisation implements ServiceOrganisationRaw {

	/**
	 * Map des organisations par numéro
	 */
	private final Map<Long, Organisation> organisationMap = new HashMap<>();

	/**
	 * Cette méthode initialise le mock en fonction des données voulues.
	 */
	protected abstract void init();

	public MockServiceOrganisation() {
		this.init();
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		return organisationMap.get(noOrganisation);
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

	protected void addOrganisation(Organisation organisation) {
		organisationMap.put(organisation.getNo(), organisation);
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

}
