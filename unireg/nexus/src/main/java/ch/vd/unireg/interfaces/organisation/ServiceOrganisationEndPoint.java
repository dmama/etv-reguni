package ch.vd.unireg.interfaces.organisation;

import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.load.DetailedLoadMonitorable;
import ch.vd.uniregctb.load.LoadDetail;
import ch.vd.uniregctb.load.MethodCallDescriptor;

public class ServiceOrganisationEndPoint implements ServiceOrganisationRaw, DetailedLoadMonitorable {

	private ServiceOrganisationRaw target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(ServiceOrganisationRaw target) {
		this.target = target;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("getOrganisationHistory", "noOrganisation", noOrganisation));
		try {
			return target.getOrganisationHistory(noOrganisation);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("getOrganisationPourSite", "noSite", noSite));
		try {
			return target.getOrganisationPourSite(noSite);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Map<Long, Organisation> getPseudoOrganisationHistory(long noEvenement) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("getPseudoOrganisationHistory", "noEvenement", noEvenement));
		try {
			return target.getPseudoOrganisationHistory(noEvenement);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("getOrganisationByNoIde", "noide", noide));
		try {
			return target.getOrganisationByNoIde(noide);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		loadMeter.start(new MethodCallDescriptor("ping"));
		try {
			target.ping();
		}
		finally {
			loadMeter.end();
		}
	}
}
