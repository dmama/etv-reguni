package ch.vd.uniregctb.interfaces.service;

import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.common.DonneesOrganisationException;

public class ServiceOrganisationImpl implements ServiceOrganisationService {

	//private static final Logger LOGGER = LoggerFactory.getLogger(ServiceOrganisationImpl.class);

	private ServiceOrganisationRaw target;

	public ServiceOrganisationImpl() {
	}

	public ServiceOrganisationImpl(ServiceOrganisationRaw target) {
		this.target = target;
	}

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws DonneesOrganisationException {
		Organisation org = target.getOrganisationHistory(noOrganisation);
		if (org == null) {
			throw new DonneesOrganisationException(String.format("L'organisation %s est introuvable.", noOrganisation));
		}
		return org;
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		Long noOrganisation = target.getOrganisationPourSite(noSite);
		if (noOrganisation == null) {
			throw new DonneesOrganisationException(String.format("Pas d'organisation correspondant au site %s.", noSite));
		}
		return noOrganisation;

	}
}
