package ch.vd.unireg.wsclient.host;

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
import ch.vd.unireg.interfaces.infra.InfrastructureException;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClientException;

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
	public CollectiviteAdministrative getCollectivite(final int noColAdm) throws InfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivite(noColAdm);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivite", () -> String.format("noColAdm=%d", noColAdm));
		}
	}

	@Override
	public Rue getRueByNumero(final Integer numero) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getRueByNumero(numero);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRueByNumero", () -> String.format("numero=%d", numero));
		}
	}

	@Override
	public CollectiviteAdministrative getCollectivite(final String sigle) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivite(sigle);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivite", () -> String.format("sigle=%s", sigle));
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministratives(final String canton) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministratives(canton);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", () -> String.format("canton=%s", canton));
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministratives(final TypeCollectivite[] types) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministratives(types);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", () -> String.format("types=%s", Arrays.toString(types)));
		}
	}

	@Override
	public ListeCollectiviteAdministrative getCollectivitesAdministrativesPourTypeCommunication(final TypeCommunicationPourTier typeCommunication) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCollectivitesAdministrativesPourTypeCommunication(typeCommunication);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministrativesPourTypeCommunication", () -> String.format("typeCommunication=%s", typeCommunication));
		}
	}

	@Override
	public CollectiviteAdministrative getOidDeCommune(final int noOfsCommune) throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOidDeCommune(noOfsCommune);
		}
		catch (InfrastructureException e) {
			t = e;
			throw e;
		}
		catch (ServiceInfrastructureClientException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOidDeCommune", () -> String.format("ofsCommune=%d", noOfsCommune));
		}
	}

	@Override
	public ListeTypesCollectivite getTypesCollectivites() throws ServiceInfrastructureClientException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTypesCollectivites();
		}
		catch (InfrastructureException e) {
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
		catch (InfrastructureException e) {
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
		catch (InfrastructureException e) {
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
