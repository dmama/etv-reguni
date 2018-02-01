package ch.vd.unireg.interfaces.organisation;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.organisation.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.ServiceOrganisationEvent;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

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
	public Map<Long, ServiceOrganisationEvent> getOrganisationEvent(long noEvenement) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("getPseudoOrganisationHistory", "noEvenement", noEvenement));
		try {
			return target.getOrganisationEvent(noEvenement);
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
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("validerAnnonceIDE", "modele", modele));
		try {
			return target.validerAnnonceIDE(modele);
		}
		finally {
			loadMeter.end();
		}
	}

	@NotNull
	@Override
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws ServiceOrganisationException {
		loadMeter.start(new MethodCallDescriptor("findAnnoncesIDE", "query", query, "order", order, "pageNumber", pageNumber));
		try {
			return target.findAnnoncesIDE(query, order, pageNumber, resultsPerPage);
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
