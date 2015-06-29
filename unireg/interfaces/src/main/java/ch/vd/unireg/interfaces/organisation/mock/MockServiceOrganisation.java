package ch.vd.unireg.interfaces.organisation.mock;

import java.util.HashMap;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
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

	@Override
	public Organisation getOrganisation(long cantonalId) throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Organisation getOrganisation(long cantonalId, RegDate date) throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Organisation getOrganisationHistory(long cantonalId) throws ServiceOrganisationException {
		return organisationMap.get(cantonalId);
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		throw new UnsupportedOperationException();
	}

}
