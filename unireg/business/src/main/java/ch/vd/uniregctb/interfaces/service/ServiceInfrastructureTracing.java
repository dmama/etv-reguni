package ch.vd.uniregctb.interfaces.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.infrastructure.model.EnumTypeCollectivite;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.ApplicationFiscale;
import ch.vd.uniregctb.interfaces.model.Canton;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.interfaces.model.Localite;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.model.OfficeImpot;
import ch.vd.uniregctb.interfaces.model.Pays;
import ch.vd.uniregctb.interfaces.model.Rue;
import ch.vd.uniregctb.interfaces.model.TypeEtatPM;
import ch.vd.uniregctb.interfaces.model.TypeRegimeFiscal;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceInfrastructureTracing implements ServiceInfrastructureRaw, InitializingBean, DisposableBean {

	private ServiceInfrastructureRaw target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(ServicePersonneMoraleService.SERVICE_NAME);

	public void setTarget(ServiceInfrastructureRaw target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public List<Canton> getAllCantons() throws ServiceInfrastructureException {
		List<Canton> result;
		long time = tracing.start();
		try {
			result = target.getAllCantons();
		}
		finally {
			tracing.end(time, "getAllCantons", null);
		}

		return result;
	}

	public CollectiviteAdministrative getCollectivite(final int noColAdm) throws ServiceInfrastructureException {
		CollectiviteAdministrative result;
		long time = tracing.start();
		try {
			result = target.getCollectivite(noColAdm);
		}
		finally {
			tracing.end(time, "getCollectivite", new Object() {
				@Override
				public String toString() {
					return String.format("noColAdm=%d", noColAdm);
				}
			});
		}

		return result;
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws ServiceInfrastructureException {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesAdministratives();
		}
		finally {
			tracing.end(time, "getCollectivitesAdministratives", null);
		}

		return result;
	}

	public List<CollectiviteAdministrative> getCollectivitesAdministratives(final List<EnumTypeCollectivite> typesCollectivite)
			throws ServiceInfrastructureException {
		List<CollectiviteAdministrative> result;
		long time = tracing.start();
		try {
			result = target.getCollectivitesAdministratives(typesCollectivite);
		}
		finally {
			tracing.end(time, "getCollectivitesAdministratives", new Object() {
				@Override
				public String toString() {
					return String.format("typesCollectivite=%s", ServiceTracing.toString(typesCollectivite));
				}
			});
		}

		return result;
	}

	@Override
	public Integer getNoOfsCommuneByEgid(final int egid, final RegDate date, final int hintNoOfsCommune) throws ServiceInfrastructureException {
		Integer result;
		long time = tracing.start();
		try {
			result = target.getNoOfsCommuneByEgid(egid, date, hintNoOfsCommune);
		}
		finally {
			tracing.end(time, "getNoOfsCommuneByEgid", new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, date=%s, hintNoOfsCommune=%d", egid, ServiceTracing.toString(date), hintNoOfsCommune);
				}
			});
		}

		return result;
	}

	public Commune getCommuneByLocalite(final Localite localite) throws ServiceInfrastructureException {
		Commune result;
		long time = tracing.start();
		try {
			result = target.getCommuneByLocalite(localite);
		}
		finally {
			tracing.end(time, "getCommuneByLocalite", new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}

		return result;
	}

	@Override
	public List<Commune> getCommuneHistoByNumeroOfs(final int noOfsCommune) throws ServiceInfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getCommuneHistoByNumeroOfs(noOfsCommune);
		}
		finally {
			tracing.end(time, "getCommuneHistoByNumeroOfs", new Object() {
				@Override
				public String toString() {
					return String.format("noOfsCommune=%d", noOfsCommune);
				}
			});
		}

		return result;
	}

	public List<Commune> getCommunes() throws ServiceInfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getCommunes();
		}
		finally {
			tracing.end(time, "getCommunes", null);
		}

		return result;
	}

	public List<Commune> getListeCommunes(final Canton canton) throws ServiceInfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeCommunes(canton);
		}
		finally {
			tracing.end(time, "getListeCommunes", new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
		}

		return result;
	}

	public List<Commune> getListeFractionsCommunes() throws ServiceInfrastructureException {
		List<Commune> result;
		long time = tracing.start();
		try {
			result = target.getListeFractionsCommunes();
		}
		finally {
			tracing.end(time, "getListeFractionsCommunes", null);
		}

		return result;
	}

	public Localite getLocaliteByONRP(final int onrp) throws ServiceInfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByONRP(onrp);
		}
		finally {
			tracing.end(time, "getLocaliteByONRP", new Object() {
				@Override
				public String toString() {
					return String.format("onrp=%d", onrp);
				}
			});
		}

		return result;
	}

	public List<Localite> getLocalites() throws ServiceInfrastructureException {
		List<Localite> result;
		long time = tracing.start();
		try {
			result = target.getLocalites();
		}
		finally {
			tracing.end(time, "getLocalites", null);
		}

		return result;
	}

	public OfficeImpot getOfficeImpot(final int noColAdm) throws ServiceInfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpot(noColAdm);
		}
		finally {
			tracing.end(time, "getOfficeImpot", new Object() {
				@Override
				public String toString() {
					return String.format("noColAdm=%d", noColAdm);
				}
			});
		}

		return result;
	}

	public OfficeImpot getOfficeImpotDeCommune(final int noCommune) throws ServiceInfrastructureException {
		OfficeImpot result;
		long time = tracing.start();
		try {
			result = target.getOfficeImpotDeCommune(noCommune);
		}
		finally {
			tracing.end(time, "getOfficeImpotDeCommune", new Object() {
				@Override
				public String toString() {
					return String.format("noCommune=%d", noCommune);
				}
			});
		}

		return result;
	}

	public List<OfficeImpot> getOfficesImpot() throws ServiceInfrastructureException {
		List<OfficeImpot> result;
		long time = tracing.start();
		try {
			result = target.getOfficesImpot();
		}
		finally {
			tracing.end(time, "getOfficesImpot", null);
		}

		return result;
	}

	public List<Pays> getPays() throws ServiceInfrastructureException {
		List<Pays> result;
		long time = tracing.start();
		try {
			result = target.getPays();
		}
		finally {
			tracing.end(time, "getPays", null);
		}

		return result;
	}

	public Rue getRueByNumero(final int numero) throws ServiceInfrastructureException {
		Rue result;
		long time = tracing.start();
		try {
			result = target.getRueByNumero(numero);
		}
		finally {
			tracing.end(time, "getRueByNumero", new Object() {
				@Override
				public String toString() {
					return String.format("numero=%d", numero);
				}
			});
		}

		return result;
	}

	public List<Rue> getRues(final Localite localite) throws ServiceInfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(localite);
		}
		finally {
			tracing.end(time, "getRues", new Object() {
				@Override
				public String toString() {
					return String.format("localite=%s", localite != null ? localite.getNoOrdre() : null);
				}
			});
		}

		return result;
	}

	public List<Rue> getRues(final Canton canton) throws ServiceInfrastructureException {
		List<Rue> result;
		long time = tracing.start();
		try {
			result = target.getRues(canton);
		}
		finally {
			tracing.end(time, "getRues", new Object() {
				@Override
				public String toString() {
					return String.format("canton=%s", canton != null ? canton.getSigleOFS() : null);
				}
			});
		}

		return result;
	}

	public InstitutionFinanciere getInstitutionFinanciere(final int id) throws ServiceInfrastructureException {
		InstitutionFinanciere result;
		long time = tracing.start();
		try {
			result = target.getInstitutionFinanciere(id);
		}
		finally {
			tracing.end(time, "getInstitutionFinanciere", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", id);
				}
			});
		}

		return result;
	}

	public List<InstitutionFinanciere> getInstitutionsFinancieres(final String noClearing) throws ServiceInfrastructureException {
		List<InstitutionFinanciere> result;
		long time = tracing.start();
		try {
			result = target.getInstitutionsFinancieres(noClearing);
		}
		finally {
			tracing.end(time, "getInstitutionsFinancieres", new Object() {
				@Override
				public String toString() {
					return String.format("noClearing=%s", noClearing);
				}
			});
		}

		return result;
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(ServicePersonneMoraleService.SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(ServicePersonneMoraleService.SERVICE_NAME);
		}
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}

	public Localite getLocaliteByNPA(final int npa) throws ServiceInfrastructureException {
		Localite result;
		long time = tracing.start();
		try {
			result = target.getLocaliteByNPA(npa);
		}
		finally {
			tracing.end(time, "getLocaliteByNPA", new Object() {
				@Override
				public String toString() {
					return String.format("npa=%d", npa);
				}
			});
		}

		return result;
	}

	public List<TypeRegimeFiscal> getTypesRegimesFiscaux() throws ServiceInfrastructureException {
		List<TypeRegimeFiscal> result;
		long time = tracing.start();
		try {
			result = target.getTypesRegimesFiscaux();
		}
		finally {
			tracing.end(time, "getTypesRegimesFiscaux", null);
		}

		return result;
	}

	public TypeRegimeFiscal getTypeRegimeFiscal(final String code) throws ServiceInfrastructureException {
		TypeRegimeFiscal result;
		long time = tracing.start();
		try {
			result = target.getTypeRegimeFiscal(code);
		}
		finally {
			tracing.end(time, "getTypeRegimeFiscal", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}

		return result;
	}

	public List<TypeEtatPM> getTypesEtatsPM() throws ServiceInfrastructureException {
		List<TypeEtatPM> result;
		long time = tracing.start();
		try {
			result = target.getTypesEtatsPM();
		}
		finally {
			tracing.end(time, "getTypesEtatsPM", null);
		}

		return result;
	}

	public TypeEtatPM getTypeEtatPM(final String code) throws ServiceInfrastructureException {
		TypeEtatPM result;
		long time = tracing.start();
		try {
			result = target.getTypeEtatPM(code);
		}
		finally {
			tracing.end(time, "getTypeEtatPM", new Object() {
				@Override
				public String toString() {
					return String.format("code=%s", code);
				}
			});
		}

		return result;
	}

	public String getUrlVers(final ApplicationFiscale application, final Long tiersId) {
		String result;
		final long time = tracing.start();
		try {
			result = target.getUrlVers(application, tiersId);
		}
		finally {
			tracing.end(time, "getUrlVers", new Object() {
				@Override
				public String toString() {
					return String.format("application=%s, tiersId=%d", application.name(), tiersId);
				}
			});
		}

		return result;
	}

	public Logiciel getLogiciel(final Long idLogiciel) throws ServiceInfrastructureException {
		Logiciel result;
		long time = tracing.start();
		try {
			result = target.getLogiciel(idLogiciel);
		}
		finally {
			tracing.end(time, "getLogiciel", new Object() {
				@Override
				public String toString() {
					return String.format("id=%d", idLogiciel);
				}
			});
		}

		return result;
	}

	public List<Logiciel> getTousLesLogiciels() throws ServiceInfrastructureException {
		List<Logiciel> result;
		long time = tracing.start();
		try {
			result = target.getTousLesLogiciels();
		}
		finally {
			tracing.end(time, "getTousLesLogiciels", null);
		}

		return result;
	}
}

