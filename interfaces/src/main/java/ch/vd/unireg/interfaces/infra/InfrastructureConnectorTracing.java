package ch.vd.unireg.interfaces.infra;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.StringRenderer;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.data.Canton;
import ch.vd.unireg.interfaces.infra.data.CollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.District;
import ch.vd.unireg.interfaces.infra.data.GenreImpotMandataire;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Logiciel;
import ch.vd.unireg.interfaces.infra.data.OfficeImpot;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.unireg.interfaces.infra.data.Region;
import ch.vd.unireg.interfaces.infra.data.Rue;
import ch.vd.unireg.interfaces.infra.data.TypeCollectivite;
import ch.vd.unireg.interfaces.infra.data.TypeRegimeFiscal;
import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Implémentation qui permet de comptabiliser le temps passé dans les appels du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class InfrastructureConnectorTracing implements InfrastructureConnector, InitializingBean, DisposableBean {

	private InfrastructureConnector target;
	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(InfrastructureConnector.SERVICE_NAME);

	public void setTarget(InfrastructureConnector target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public List<Canton> getAllCantons() throws InfrastructureException {
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
	public CollectiviteAdministrative getCollectivite(final int noColAdm) throws InfrastructureException {
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives() throws InfrastructureException {
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
	public List<CollectiviteAdministrative> getCollectivitesAdministratives(final List<TypeCollectivite> typesCollectivite) throws InfrastructureException {
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
	public Integer getNoOfsCommuneByEgid(final int egid, final RegDate date) throws InfrastructureException {
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
	public Commune getCommuneByLocalite(final Localite localite) throws InfrastructureException {
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
	public Commune findCommuneByNomOfficiel(@NotNull String nomOfficiel, boolean includeFaitieres, boolean includeFractions, @Nullable RegDate date) throws InfrastructureException {
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
	public List<Commune> getCommuneHistoByNumeroOfs(final int noOfsCommune) throws InfrastructureException {
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
	public List<Commune> getCommunes() throws InfrastructureException {
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
	public List<Commune> getListeCommunes(final Canton canton) throws InfrastructureException {
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
	public List<Commune> getCommunesVD() throws InfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getCommunesVD();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunesVD", items, null);
		}
	}

	@Override
	public List<Commune> getListeCommunesFaitieres() throws InfrastructureException {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Commune> list = target.getListeCommunesFaitieres();
			items = list == null ? 0 : list.size();
			return list;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getListeCommunesFaitieres", items, null);
		}
	}

	@Override
	public List<Localite> getLocalitesByONRP(final int onrp) throws InfrastructureException {
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
	public Localite getLocaliteByONRP(int onrp, RegDate dateReference) throws InfrastructureException {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getLocaliteByONRP(onrp, dateReference);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocaliteByONRP", () -> "onrp=" + onrp + ", dateReference=" + dateReference);
		}
	}

	@Override
	public List<Localite> getLocalites() throws InfrastructureException {
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
	public List<OfficeImpot> getOfficesImpot() throws InfrastructureException {
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
	public List<Pays> getPays() throws InfrastructureException {
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
	public List<Pays> getPaysHisto(final int numeroOFS) throws InfrastructureException {
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
	public Pays getPays(final int numeroOFS, @Nullable final RegDate date) throws InfrastructureException {
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
	public Pays getPays(@NotNull final String codePays, @Nullable final RegDate date) throws InfrastructureException {
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
	public Rue getRueByNumero(final int numero, final RegDate date) throws InfrastructureException {
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
	public List<Rue> getRues(final Localite localite) throws InfrastructureException {
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
	public List<Rue> getRuesHisto(final int numero) throws InfrastructureException {
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
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(InfrastructureConnector.SERVICE_NAME, tracing);
		}
	}

	@Override
	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(InfrastructureConnector.SERVICE_NAME);
		}
	}

	@Override
	public List<Localite> getLocalitesByNPA(final int npa, final RegDate dateReference) throws InfrastructureException {
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

	private static String buildMapString(Map<String, String> map) {
		return CollectionsUtils.toString(map,
		                                 StringRenderer.DEFAULT,
		                                 str -> str != null ? String.format("'%s'", str) : StringUtils.EMPTY,
		                                 ", ",
		                                 "[",
		                                 "]",
		                                 "null");
	}

	@Override
	public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.getUrl(application, parametres);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUrl", () -> String.format("application=%s, parametres=%s", application.name(), buildMapString(parametres)));
		}
	}

	@Override
	public Logiciel getLogiciel(final Long idLogiciel) throws InfrastructureException {
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
	public List<Logiciel> getTousLesLogiciels() throws InfrastructureException {
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
	public List<CollectiviteAdministrative> findCollectivitesAdministratives(@NotNull Collection<Integer> codeCollectivites, boolean inactif) {
		Throwable t = null;
		final long time = tracing.start();
		try {
			return target.findCollectivitesAdministratives(codeCollectivites, inactif);
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findCollectivitesAdministratives :", () -> String.format("codeCollectivites=%s , inactif=%s", codeCollectivites.stream()
					.map(String::valueOf)
					.collect(Collectors.joining(", ")), Boolean.toString(inactif)));
		}
	}

	@Override
	public void ping() throws InfrastructureException {
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