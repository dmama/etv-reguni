package ch.vd.unireg.interfaces.securite;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.load.DetailedLoadMeter;
import ch.vd.unireg.load.MethodCallDescriptor;
import ch.vd.unireg.security.Operateur;
import ch.vd.unireg.security.ProfileOperateur;
import ch.vd.unireg.stats.DetailedLoadMonitorable;
import ch.vd.unireg.stats.LoadDetail;

public class SecuriteConnectorEndPoint implements SecuriteConnector, DetailedLoadMonitorable {

	private SecuriteConnector target;
	private final DetailedLoadMeter<MethodCallDescriptor> loadMeter = new DetailedLoadMeter<>();

	public void setTarget(SecuriteConnector target) {
		this.target = target;
	}

	@Override
	@Nullable
	public Operateur getOperateur(@NotNull String visa) throws SecuriteConnectorException {
		loadMeter.start(new MethodCallDescriptor("getOperateur", "visa", visa));
		try {
			return target.getOperateur(visa);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	@Nullable
	public ProfileOperateur getProfileUtilisateur(String visaOperateur, int codeCollectivite) throws SecuriteConnectorException {
		loadMeter.start(new MethodCallDescriptor("getProfileUtilisateur", "visaOperateur", visaOperateur, "codeCollectivite", codeCollectivite));
		try {
			return target.getProfileUtilisateur(visaOperateur, codeCollectivite);
		}
		finally {
			loadMeter.end();
		}
	}

	@NotNull
	@Override
	public List<String> getUtilisateurs(int noCollAdmin) throws SecuriteConnectorException {
		loadMeter.start(new MethodCallDescriptor("getUtilisateurs", "noCollAdmin", noCollAdmin));
		try {
			return target.getUtilisateurs(noCollAdmin);
		}
		finally {
			loadMeter.end();
		}
	}

	@Override
	@NotNull
	public Set<Integer> getCollectivitesOperateur(@NotNull String visaOperateur) throws SecuriteConnectorException {
		loadMeter.start(new MethodCallDescriptor("getCollectivitesOperateur", "visaOperateur", visaOperateur));
		try {
			return target.getCollectivitesOperateur(visaOperateur);
		}
		finally {
			loadMeter.end();
		}
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

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}
}
