package ch.vd.uniregctb.wsclient.fidor;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.evd0007.v1.Country;
import ch.vd.evd0007.v1.ExtendedCanton;
import ch.vd.evd0012.v1.CommuneFiscale;
import ch.vd.evd0012.v1.DistrictFiscal;
import ch.vd.evd0012.v1.Logiciel;
import ch.vd.evd0012.v1.RegionFiscale;
import ch.vd.fidor.xml.impotspecial.v1.ImpotSpecial;
import ch.vd.fidor.xml.post.v1.PostalLocality;
import ch.vd.fidor.xml.post.v1.Street;
import ch.vd.fidor.xml.regimefiscal.v1.RegimeFiscal;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.stats.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservice.fidor.v5.FidorClient;

public class FidorClientTracing implements FidorClient, InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "FidorClient";

	private FidorClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(FidorClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void ping() {
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

	@Override
	public CommuneFiscale getCommuneParNoOFS(final int ofsId, final RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final CommuneFiscale commune = target.getCommuneParNoOFS(ofsId, date);
			if (commune != null) {
				items = 1;
			}
			return commune;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneParNoOFS", items, new Object() {
				@Override
				public String toString() {
					return String.format("ofsId=%d, date=%s", ofsId, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesParNoOFS(final int ofsId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CommuneFiscale> communes = target.getCommunesParNoOFS(ofsId);
			if (communes != null) {
				items = communes.size();
			}
			return communes;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunesParNoOFS", items, new Object() {
				@Override
				public String toString() {
					return String.format("ofsId=%d", ofsId);
				}
			});
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesParCanton(final int ofsId, final RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CommuneFiscale> communes = target.getCommunesParCanton(ofsId, date);
			if (communes != null) {
				items = communes.size();
			}
			return communes;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommunesParCanton", items, new Object() {
				@Override
				public String toString() {
					return String.format("ofsId=%d, date=%s", ofsId, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public List<ExtendedCanton> getTousLesCantons() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<ExtendedCanton> cantons = target.getTousLesCantons();
			if (cantons != null) {
				items = cantons.size();
			}
			return cantons;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesCantons", items, null);
		}
	}

	@Override
	public List<CommuneFiscale> getToutesLesCommunes() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CommuneFiscale> communes = target.getToutesLesCommunes();
			if (communes != null) {
				items = communes.size();
			}
			return communes;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getToutesLesCommunes", items, null);
		}
	}

	@NotNull
	@Override
	public List<CommuneFiscale> findCommuneByNomOfficiel(@NotNull String nomOfficiel, @Nullable RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<CommuneFiscale> communes = target.findCommuneByNomOfficiel(nomOfficiel, date);
			items = communes.size();
			return communes;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "findCommuneByNomOfficiel", items, new Object() {
				@Override
				public String toString() {
					return String.format("nom=%s, date=%s", nomOfficiel, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public CommuneFiscale getCommuneParBatiment(final int egid, final RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final CommuneFiscale commune = target.getCommuneParBatiment(egid, date);
			if (commune != null) {
				items = 1;
			}
			return commune;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getCommuneParBatiment", items, new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, date=%s", egid, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Country getPaysDetail(final long ofsId, final RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Country pays = target.getPaysDetail(ofsId, date);
			if (pays != null) {
				items = 1;
			}
			return pays;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPaysDetail", items, new Object() {
				@Override
				public String toString() {
					return String.format("ofsId=%d, date=%s", ofsId, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public Country getPaysDetail(final String iso2Id, final RegDate date) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Country pays = target.getPaysDetail(iso2Id, date);
			if (pays != null) {
				items = 1;
			}
			return pays;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPaysDetail", items, new Object() {
				@Override
				public String toString() {
					return String.format("iso2Id=%s, date=%s", iso2Id, ServiceTracing.toString(date));
				}
			});
		}
	}

	@Override
	public List<Country> getPaysHisto(final long ofsId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Country> pays = target.getPaysHisto(ofsId);
			if (pays != null) {
				items = pays.size();
			}
			return pays;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getPaysHisto", items, new Object() {
				@Override
				public String toString() {
					return String.format("ofsId=%d", ofsId);
				}
			});
		}
	}

	@Override
	public List<Country> getTousLesPays() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Country> pays = target.getTousLesPays();
			if (pays != null) {
				items = pays.size();
			}
			return pays;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getTousLesPays", items, null);
		}
	}

	@Override
	public Logiciel getLogicielDetail(final long logicielId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final Logiciel l = target.getLogicielDetail(logicielId);
			if (l != null) {
				items = 1;
			}
			return l;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLogicielDetail", items, new Object() {
				@Override
				public String toString() {
					return String.format("logicielId=%d", logicielId);
				}
			});
		}
	}

	@Override
	public List<Logiciel> getTousLesLogiciels() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Logiciel> ls = target.getTousLesLogiciels();
			if (ls != null) {
				items = ls.size();
			}
			return ls;
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
	public DistrictFiscal getDistrict(final int code) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final DistrictFiscal district = target.getDistrict(code);
			if (district != null) {
				items = 1;
			}
			return district;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getDistrict", items, new Object() {
				@Override
				public String toString() {
					return String.format("code=%d", code);
				}
			});
		}
	}

	@Override
	public RegionFiscale getRegion(final int code) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final RegionFiscale region = target.getRegion(code);
			if (region != null) {
				items = 1;
			}
			return region;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRegion", items, new Object() {
				@Override
				public String toString() {
					return String.format("code=%d", code);
				}
			});
		}
	}

	@Override
	public String getUrl(final String app, final String acces, final String targetType, final Map<String, String> map) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final String url = target.getUrl(app, acces, targetType, map);
			if (url != null) {
				items = 1;
			}
			return url;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getUrl", items, new Object() {
				@Override
				public String toString() {
					return String.format("app=%s, access=%s, targetType=%s, map=%s", app, acces, targetType, mapToString(map));
				}
			});
		}
	}

	private static String mapToString(Map<String, String> map) {
		if (map == null || map.isEmpty()) {
			return "null";
		}
		final StringBuilder b = new StringBuilder("{");
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (b.length() > 1) {
				b.append(", ");
			}
			b.append('\'').append(entry.getKey()).append("':");
			if (entry.getValue() == null) {
				b.append(entry.getValue());
			}
			else {
				b.append('\'').append(entry.getValue()).append('\'');
			}
		}
		b.append("}");
		return b.toString();
	}

	@Override
	public List<PostalLocality> getLocalitesPostales(final RegDate dateReference, final Integer npa, final Integer noOrdrePostal, final String nom, final Integer cantonOfsId) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<PostalLocality> pls = target.getLocalitesPostales(dateReference, npa, noOrdrePostal, nom, cantonOfsId);
			if (pls != null) {
				items = pls.size();
			}
			return pls;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalitesPostales", items, new Object() {
				@Override
				public String toString() {
					return String.format("dateReference=%s, npa=%d, noOrdrePostal=%d, nom=%s, cantonOfsId=%d",
					                     ServiceTracing.toString(dateReference), npa, noOrdrePostal, nom, cantonOfsId);
				}
			});
		}
	}

	@Override
	public List<PostalLocality> getLocalitesPostalesHisto(final int noOrdrePostal) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<PostalLocality> pls = target.getLocalitesPostalesHisto(noOrdrePostal);
			if (pls != null) {
				items = pls.size();
			}
			return pls;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalitesPostalesHisto", items, new Object() {
				@Override
				public String toString() {
					return String.format("noOrdrePostal=%d", noOrdrePostal);
				}
			});
		}
	}

	@Override
	public PostalLocality getLocalitePostale(final RegDate dateReference, final int noOrdrePostal) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final PostalLocality pl = target.getLocalitePostale(dateReference, noOrdrePostal);
			if (pl != null) {
				items = 1;
			}
			return pl;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getLocalitePostale", items, new Object() {
				@Override
				public String toString() {
					return String.format("dateReference=%s, noOrdrePostal=%d", ServiceTracing.toString(dateReference), noOrdrePostal);
				}
			});
		}
	}

	@Override
	public List<Street> getRuesParNumeroOrdrePosteEtDate(final int noOrdrePostal, final RegDate dateReference) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Street> rues = target.getRuesParNumeroOrdrePosteEtDate(noOrdrePostal, dateReference);
			if (rues != null) {
				items = rues.size();
			}
			return rues;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRuesParNumeroOrdrePostalEtDate", items, new Object() {
				@Override
				public String toString() {
					return String.format("noOrdrePostal=%d, dateReference=%s", noOrdrePostal, ServiceTracing.toString(dateReference));
				}
			});
		}
	}

	@Override
	public List<Street> getRuesParEstrid(final int estrid, final RegDate dateReference) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<Street> rues = target.getRuesParEstrid(estrid, dateReference);
			if (rues != null) {
				items = rues.size();
			}
			return rues;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRuesParEstrid", items, new Object() {
				@Override
				public String toString() {
					return String.format("estrid=%d, dateReference=%s", estrid, ServiceTracing.toString(dateReference));
				}
			});
		}
	}

	@Override
	public RegimeFiscal getRegimeFiscalParCode(final String code) {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final RegimeFiscal rf = target.getRegimeFiscalParCode(code);
			if (rf != null) {
				items = 1;
			}
			return rf;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRegimeFiscalParCode", items, new Object() {
				@Override
				public String toString() {
					return String.format("code='%s'", code);
				}
			});
		}
	}

	@Override
	public List<RegimeFiscal> getRegimesFiscaux() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<RegimeFiscal> regimes = target.getRegimesFiscaux();
			if (regimes != null) {
				items = regimes.size();
			}
			return regimes;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getRegimesFiscaux", items, null);
		}
	}

	@Override
	public List<ImpotSpecial> getImpotsSpeciaux() {
		Throwable t = null;
		int items = 0;
		final long time = tracing.start();
		try {
			final List<ImpotSpecial> types = target.getImpotsSpeciaux();
			if (types != null) {
				items = types.size();
			}
			return types;
		}
		catch (RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			tracing.end(time, t, "getImpotsSpeciaux", items, null);
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
