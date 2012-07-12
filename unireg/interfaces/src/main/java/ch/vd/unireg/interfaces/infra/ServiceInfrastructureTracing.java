package ch.vd.unireg.interfaces.infra;

import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Pays;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeEtatPM;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceInfrastructureTracing implements ServiceInfrastructureRaw, InitializingBean, DisposableBean {

	private ServiceInfrastructureRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(ServiceInfrastructureRaw.SERVICE_NAME);

	public void setTarget(ServiceInfrastructureRaw target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Canton> list = target.getAllCantons();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getAllCantons", items, null);
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
		catch (RuntimeException e) {
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CollectiviteAdministrative> list = target.getCollectivitesAdministratives();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", items, null);
		}
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(final List<EnumTypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CollectiviteAdministrative> list = target.getCollectivitesAdministratives(typesCollectivite);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", items, new Object() {
				@Override
				public String toString() {
					return String.format("typesCollectivite=%s", ServiceTracing.toString(typesCollectivite));
				}
			});
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(final int egid, final RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNoOfsCommuneByEgid(egid, date);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNoOfsCommuneByEgid", new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, date=%s", egid, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Commune getCommuneByLocalite(final Localite localite) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCommuneByLocalite(localite);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneByLocalite", new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(final int noOfsCommune) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getCommuneHistoByNumeroOfs(noOfsCommune);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneHistoByNumeroOfs", items, new Object() {
				@Override
				public String toString() {
					return String.format("noOfsCommune=%d", noOfsCommune);
				}
			});
		}
	}

	@Override
	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getCommunes();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunes", items, null);
		}
	}

	@Override
	public List<Commune> getListeCommunes(final Canton canton) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getListeCommunes(canton);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeCommunes", items, new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
		}
	}

	@Override
	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getListeFractionsCommunes();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeFractionsCommunes", items, null);
		}
	}

	@Override
	public Localite getLocaliteByONRP(final int onrp) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLocaliteByONRP(onrp);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocaliteByONRP", new Object() {
				@Override
				public String toString() {
					return String.format("onrp=%d", onrp);
				}
			});
		}
	}

	@Override
	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Localite> list = target.getLocalites();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalites", items, null);
		}
	}

	@Override
	public OfficeImpot getOfficeImpotDeCommune(final int noCommune) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getOfficeImpotDeCommune(noCommune);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOfficeImpotDeCommune", new Object() {
				@Override
				public String toString() {
					return String.format("noCommune=%d", noCommune);
				}
			});
		}
	}

	@Override
	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<OfficeImpot> list = target.getOfficesImpot();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getOfficesImpot", items, null);
		}
	}

	@Override
	public List<Pays> getPays() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Pays> list = target.getPays();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", items, null);
		}
	}

	@Override
	public Pays getPays(final int numeroOFS) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPays(numeroOFS);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", new Object() {
				@Override
				public String toString() {
					return String.format("numeroOFS=%d", numeroOFS);
				}
			});
		}
	}

	@Override
	public Pays getPays(final String codePays) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPays(codePays);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", new Object() {
				@Override
				public String toString() {
					return String.format("codePays=%s", codePays);
				}
			});
		}
	}

	@Override
	public Rue getRueByNumero(final int numero) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getRueByNumero(numero);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
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
	public List<Rue> getRues(final Localite localite) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Rue> list = target.getRues(localite);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRues", items, new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}
	}

	@Override
	public List<Rue> getRues(final Canton canton) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Rue> list = target.getRues(canton);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRues", items, new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
		}
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(final int id) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getInstitutionFinanciere(id);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getInstitutionFinanciere", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}
	}

	@Override
	public List<InstitutionFinanciere> getInstitutionsFinancieres(final String noClearing) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<InstitutionFinanciere> list = target.getInstitutionsFinancieres(noClearing);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getInstitutionsFinancieres", items, new Object() {
				@Override
				public String toString() {
					return String.format("noClearing=%s", noClearing);
				}
			});
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(ServiceInfrastructureRaw.SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(ServiceInfrastructureRaw.SERVICE_NAME);
		}
	}

	@Override
	public Localite getLocaliteByNPA(final int npa) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLocaliteByNPA(npa);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocaliteByNPA", new Object() {
				@Override
				public String toString() {
					return String.format("npa=%d", npa);
				}
			});
		}
	}

	@Override
	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<TypeRegimeFiscal> list = target.getTypesRegimesFiscaux();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTypesRegimesFiscaux", items, null);
		}
	}

	@Override
	public TypeRegimeFiscal getTypeRegimeFiscal(final String code) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTypeRegimeFiscal(code);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTypeRegimeFiscal", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}
	}

	@Override
	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<TypeEtatPM> list = target.getTypesEtatsPM();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTypesEtatsPM", items, null);
		}
	}

	@Override
	public TypeEtatPM getTypeEtatPM(final String code) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTypeEtatPM(code);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTypeEtatPM", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}
	}

	@Override
	public String getUrlVers(final ApplicationFiscale application, final Long tiersId, final Integer oid) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUrlVers(application, tiersId, oid);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUrlVers", new Object() {
				@Override
				public String toString() {
					return String.format("application=%s, tiersId=%d, oid=%d", application.name(), tiersId, oid);
				}
			});
		}
	}

	@Override
	public Logiciel getLogiciel(final Long idLogiciel) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLogiciel(idLogiciel);
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLogiciel", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", idLogiciel);
				}
			});
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Logiciel> list = target.getTousLesLogiciels();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (ServiceInfrastructureException e) {
			t = e;
			throw e;
		}
		catch (RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesLogiciels", items, null);
		}
	}
}