package ch.vd.uniregctb.wsclient.host;

import java.util.Arrays;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeCollectiviteAdministrative;
import ch.vd.infrastructure.model.rest.ListeTypesCollectivite;
import ch.vd.infrastructure.model.rest.Rue;
import ch.vd.infrastructure.model.rest.TypeCollectivite;
import ch.vd.infrastructure.model.rest.TypeCommunicationPourTier;
import ch.vd.infrastructure.registre.common.model.rest.InstitutionFinanciere;
import ch.vd.infrastructure.registre.common.model.rest.ListeInstitutionsFinancieres;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClientException;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

public class ServiceInfrastructureClientTracing implements ServiceInfrastructureClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "HostInterfacesInfra";

	private ServiceInfrastructureClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(ServiceInfrastructureClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public String ping() {
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
	public CollectiviteAdministrative getCollectivite(final int noColAdm) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivite(noColAdm);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("noColAdm=%d", noColAdm);
				}
			});
		}
	}

	@Override
	public Rue getRueByNumero(final Integer numero) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getRueByNumero(numero);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRueByNumero", new Object() {
				@Override
				public String toString() {
					return String.format("numero=%d", numero);
				}
			});
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(final String var1) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivite(var1);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("Sigle=%s", var1);
				}
			});
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministratives(final String var1) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministratives(var1);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", new Object() {
				@Override
				public String toString() {
					return String.format("Sigle canton=%s", var1);
				}
			});
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministratives(final TypeCollectivite[] types) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministratives(types);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", new Object() {
				@Override
				public String toString() {
					return String.format("Types=%s", Arrays.toString(types));
				}
			});
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministrativesPourTypeCommunication(final TypeCommunicationPourTier var1) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministrativesPourTypeCommunication(var1);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministrativesPourTypeCommunication", new Object() {
				@Override
				public String toString() {
					return String.format("TypeCommunications=%s", var1.toString());
				}
			});
		}
	}

	@Override
	public CollectiviteAdministrative getOidDeCommune(final int var1) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOidDeCommune(var1);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOidDeCommune", new Object() {
				@Override
				public String toString() {
					return String.format("numero ommune=%d", var1);
				}
			});
		}
	}

	@Override
	public ListeTypesCollectivite getTypesCollectivites() throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTypesCollectivites();
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}

		finally {
			tracing.end(time, t, "getTypesCollectivites",null);
		}
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(int id) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getInstitutionFinanciere(id);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}

		finally {
			tracing.end(time, t, "getInstitutionFinanciere",null);
		}
	}

	@Override
	public ListeInstitutionsFinancieres getInstitutionsFinancieres(String noClearing) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getInstitutionsFinancieres(noClearing);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}

		finally {
			tracing.end(time, t, "getInstitutionsFinancieres",null);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}
}
