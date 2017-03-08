package ch.vd.unireg.interfaces.infra;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
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
		catch (RuntimeException | Error e) {
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivite", () -> String.format("noColAdm=%d", noColAdm));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", items, null);
		}
	}

	@Override
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(final List<TypeCollectivite> typesCollectivite) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CollectiviteAdministrative> list = target.getCollectivitesAdministratives(typesCollectivite);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCollectivitesAdministratives", items, () -> String.format("typesCollectivite=%s", ServiceTracing.toString(typesCollectivite)));
		}
	}

	@Override
	public Integer getNoOfsCommuneByEgid(final int egid, final RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getNoOfsCommuneByEgid(egid, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getNoOfsCommuneByEgid", () -> String.format("egid=%d, date=%s", egid, ServiceTracing.toString(date)));
		}
	}

	@Override
	public Commune getCommuneByLocalite(final Localite localite) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getCommuneByLocalite(localite);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneByLocalite", () -> String.format("localite=%s", localite != null ? localite.getNoOrdre() : null));
		}
	}

	@Nullable
	@Override
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.findCommuneByNomOfficiel(nomOfficiel, includeFaitieres, includeFractions, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findCommuneByNomOfficiel", () -> String.format("nom=%s, includeFaitieres=%s, includeFractions=%s, date=%s", nomOfficiel, includeFaitieres, includeFractions, date));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneHistoByNumeroOfs", items, () -> String.format("noOfsCommune=%d", noOfsCommune));
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
		catch (RuntimeException | Error e) {
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeCommunes", items, () -> String.format("canton=%s", canton != null ? canton.getSigleOFS() : null));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeFractionsCommunes", items, null);
		}
	}

	@Override
	public List<Localite> getLocalitesByONRP(final int onrp) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLocalitesByONRP(onrp);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocaliteByONRP", () -> String.format("onrp=%d", onrp));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalites", items, null);
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
		catch (RuntimeException | Error e) {
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", items, null);
		}
	}

	@Override
	public List<Pays> getPaysHisto(final int numeroOFS) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPaysHisto(numeroOFS);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPaysHisto", () -> String.format("numeroOFS=%d", numeroOFS));
		}
	}

	@Override
	public Pays getPays(final int numeroOFS, @Nullable final RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPays(numeroOFS, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", () -> String.format("numeroOFS=%d, date=%s", numeroOFS, ServiceTracing.toString(date)));
		}
	}

	@Override
	public Pays getPays(@NotNull final String codePays, @Nullable final RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getPays(codePays, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPays", () -> String.format("codePays=%s, date=%s", codePays, ServiceTracing.toString(date)));
		}
	}

	@Override
	public Rue getRueByNumero(final int numero, final RegDate date) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getRueByNumero(numero, date);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRueByNumero", () -> String.format("numero=%d, date=%s", numero, ServiceTracing.toString(date)));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRues", items, () -> String.format("localite=%s", localite != null ? localite.getNoOrdre() : null));
		}
	}

	@Override
	public List<Rue> getRuesHisto(final int numero) throws ServiceInfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Rue> list = target.getRuesHisto(numero);
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRuesHisto", items, () -> String.format("numero=%d", numero));
		}
	}

	@Override
	public InstitutionFinanciere getInstitutionFinanciere(final int id) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getInstitutionFinanciere(id);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getInstitutionFinanciere", () -> String.format("id=%d", id));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getInstitutionsFinancieres", items, () -> String.format("noClearing=%s", noClearing));
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
	public List<Localite> getLocalitesByNPA(final int npa, final RegDate dateReference) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		int items = 0;
		try {
			final List<Localite> liste = target.getLocalitesByNPA(npa, dateReference);
			items = liste == null ? 0 : liste.size();
			return liste;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalitesByNPA", items, () -> String.format("npa=%d, dateReference=%s", npa, ServiceTracing.toString(dateReference)));
		}
	}

	@Override
	public String getUrlVers(final ApplicationFiscale application, final Long tiersId, final Integer oid) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUrlVers(application, tiersId, oid);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUrlVers", () -> String.format("application=%s, tiersId=%d, oid=%d", application.name(), tiersId, oid));
		}
	}

	@Override
	public String getUrlVisualisationDocument(Long tiersId, @Nullable Integer pf, Integer oid, String cleDocument) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUrlVisualisationDocument(tiersId, pf, oid, cleDocument);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUrlVisualisationDocument", () -> String.format("tiersId=%d, pf=%d, oid=%d, cleDocument='%s'", tiersId, pf, oid, cleDocument));
		}
	}

	@Override
	public Logiciel getLogiciel(final Long idLogiciel) throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLogiciel(idLogiciel);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLogiciel", () -> String.format("id=%d", idLogiciel));
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
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesLogiciels", items, null);
		}
	}

	@Override
	public District getDistrict(final int code) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getDistrict(code);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getDistrict", () -> String.format("id=%d", code));
		}
	}

	@Override
	public Region getRegion(final int code) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getRegion(code);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRegion", () -> String.format("id=%d", code));
		}
	}

	@Override
	public List<TypeRegimeFiscal> getTousLesRegimesFiscaux() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTousLesRegimesFiscaux();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesRegimesFiscaux", null);
		}
	}

	@Override
	public List<GenreImpotMandataire> getTousLesGenresImpotMandataires() {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getTousLesGenresImpotMandataires();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesGenresImpotMandataires", null);
		}
	}

	@Override
	public void ping() throws ServiceInfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			target.ping();
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "ping", null);
		}
	}
}