package ch.vd.unireg.interfaces.entreprise;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.AnnonceIDEQuery;
import ch.vd.unireg.interfaces.entreprise.data.BaseAnnonceIDE;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileEvent;
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

public class EntrepriseConnectorEndPoint implements EntrepriseConnector, DetailedLoadMonitorable {

	private EntrepriseConnector target;

	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(EntrepriseConnector target) {
		this.target = target;
	}

	@Override
	public EntrepriseCivile getEntrepriseHistory(long noEntreprise) throws EntrepriseConnectorException {
		loadMeter.start(new MethodCallDescriptor("getEntrepriseHistory", "noEntreprise", noEntreprise));
		try {
			return target.getEntrepriseHistory(noEntreprise);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Long getNoEntrepriseFromNoEtablissement(Long noEtablissementCivil) throws EntrepriseConnectorException {
		loadMeter.start(new MethodCallDescriptor("getNoEntrepriseFromNoEtablissement", "noEtablissementCivil", noEtablissementCivil));
		try {
			return target.getNoEntrepriseFromNoEtablissement(noEtablissementCivil);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Map<Long, EntrepriseCivileEvent> getEntrepriseEvent(long noEvenement) throws EntrepriseConnectorException {
		loadMeter.start(new MethodCallDescriptor("getEntrepriseEvent", "noEvenement", noEvenement));
		try {
			return target.getEntrepriseEvent(noEvenement);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public Identifiers getEntrepriseByNoIde(String noide) throws EntrepriseConnectorException {
		loadMeter.start(new MethodCallDescriptor("getEntrepriseByNoIde", "noide", noide));
		try {
			return target.getEntrepriseByNoIde(noide);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	public BaseAnnonceIDE.Statut validerAnnonceIDE(BaseAnnonceIDE modele) throws EntrepriseConnectorException {
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
	public Page<AnnonceIDE> findAnnoncesIDE(@NotNull AnnonceIDEQuery query, @Nullable Sort.Order order, int pageNumber, int resultsPerPage) throws EntrepriseConnectorException {
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
	public void ping() throws InfrastructureException {
		loadMeter.start(new MethodCallDescriptor("ping"));
		try {
			target.ping();
		}
		finally {
			loadMeter.end();
		}
	}
}
