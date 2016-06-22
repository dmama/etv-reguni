package ch.vd.uniregctb.wsclient.host;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.securite.model.rest.ListeOperateurs;
import ch.vd.securite.model.rest.Operateur;
import ch.vd.securite.model.rest.ProfilOperateur;
import ch.vd.unireg.wsclient.host.interfaces.SecuriteException;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceSecuriteClientException;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceSecuriteClientTracing implements ServiceSecuriteClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "ServiceSecuriteClient";

	private ServiceSecuriteClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceSecuriteClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesUtilisateur(final String visa) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesUtilisateur(visa);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s", visa);
				}
			});
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesUtilisateurCommunicationTier(final String visa) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesUtilisateurCommunicationTier(visa);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesUtilisateurCommunicationTier", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s", visa);
				}
			});
		}
	}

	@Override
	public ProfilOperateur getProfileUtilisateur(final String visa, final int codeCollectivite) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getProfileUtilisateur(visa,codeCollectivite);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getProfileUtilisateur", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s, codeCollectivite=%d", visa,codeCollectivite);
				}
			});
		}
	}

	@Override
	public ListeOperateurs getOperateurs(final TypeCollectivite[] types) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateurs(types);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateurs", new Object() {
				@Override
				public String toString() {
					return String.format("typeCollectivite=%s",types.toString());
				}
			});
		}
	}

	@Override
	public Operateur getOperateur(final long numeroInd) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateur(numeroInd);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("numero individu=%d",numeroInd);
				}
			});
		}
	}

	@Override
	public Operateur getOperateurTous(final long numeroInd) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateurTous(numeroInd);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateurTous", new Object() {
				@Override
				public String toString() {
					return String.format("numero individu=%d",numeroInd);
				}
			});
		}
	}

	@Override
	public Operateur getOperateur(final String visa) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateur(visa);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateur", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s",visa);
				}
			});
		}
	}

	@Override
	public Operateur getOperateurTous(final String visa) throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOperateurTous(visa);
		}

		catch (ServiceSecuriteClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOperateurTous", new Object() {
				@Override
				public String toString() {
					return String.format("visa=%s",visa);
				}
			});
		}
	}

	@Override
	public String ping() throws SecuriteException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.ping();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}

	@Override
	public void destroy() throws Exception {

	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}
}
