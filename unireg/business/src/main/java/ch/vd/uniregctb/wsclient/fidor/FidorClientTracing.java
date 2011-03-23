package ch.vd.uniregctb.wsclient.fidor;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.CommuneFiscale;
import ch.vd.fidor.ws.v2.FidorBusinessException_Exception;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.ParameterMap;
import ch.vd.fidor.ws.v2.Pays;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.webservice.fidor.FidorClient;

public class FidorClientTracing implements FidorClient, InitializingBean, DisposableBean {

	public final String SERVICE_NAME = "FidorClient";

	private FidorClient target;

	private StatsService statsService;

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);

	public void setTarget(FidorClient target) {
		this.target = target;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public Logiciel getLogicielDetail(final long id) {
		final long start = tracing.start();
		try {
			return target.getLogicielDetail(id);
		}
		finally {
			tracing.end(start, "getLogicielDetail", new Object() {
				@Override
				public String toString() {
					return String.format("id=%s", id);
				}
			});
		}
	}

	@Override
	public CommuneFiscale getCommuneParNoOFS(final int noOfs, final XMLGregorianCalendar cal) throws FidorBusinessException_Exception {
		final long start = tracing.start();
		try {
			return target.getCommuneParNoOFS(noOfs, cal);
		}
		finally {
			tracing.end(start, "getCommuneFromNoOfs", new Object() {
				@Override
				public String toString() {
					return String.format("noOfs=%d, date=%s", noOfs, XmlUtils.xmlcal2regdate(cal));
				}
			});
		}
	}

	@Override
	public CommuneFiscale getCommuneParNoTechnique(final int noTechnique) throws FidorBusinessException_Exception {
		final long start = tracing.start();
		try {
			return target.getCommuneParNoTechnique(noTechnique);
		}
		finally {
			tracing.end(start, "getCommuneFromNoTechnique", new Object() {
				@Override
				public String toString() {
					return String.format("noTechnique=%d", noTechnique);
				}
			});
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesHistoParNoOFS(final int noOfs) throws FidorBusinessException_Exception {
		final long start = tracing.start();
		try {
			return target.getCommunesHistoParNoOFS(noOfs);
		}
		finally {
			tracing.end(start, "getCommunesHistoFromNoOfs", new Object() {
				@Override
				public String toString() {
					return String.format("noOfs=%d", noOfs);
				}
			});
		}
	}

	@Override
	public List<CommuneFiscale> getCommunesHistoParNoTechnique(final int noTechnique) throws FidorBusinessException_Exception {
		final long start = tracing.start();
		try {
			return target.getCommunesHistoParNoTechnique(noTechnique);
		}
		finally {
			tracing.end(start, "getCommunesHistoParNoTechnique", new Object() {
				@Override
				public String toString() {
					return String.format("noTechnique=%d", noTechnique);
				}
			});
		}
	}

	@Override
	public List<CommuneFiscale> getCommunes(final XMLGregorianCalendar cal) throws FidorBusinessException_Exception {
		final long start = tracing.start();
		try {
			return target.getCommunes(cal);
		}
		finally {
			tracing.end(start, "getCommunes", new Object() {
				@Override
				public String toString() {
					return String.format("date=%s", XmlUtils.xmlcal2regdate(cal));
				}
			});
		}
	}

	@Override
	public CommuneFiscale getCommuneParBatiment(final int egid, final int noOfs, final XMLGregorianCalendar cal) {
		final long start = tracing.start();
		try {
			return target.getCommuneParBatiment(egid, noOfs, cal);
		}
		finally {
			tracing.end(start, "getCommuneFromBatiment", new Object() {
				@Override
				public String toString() {
					return String.format("egid=%d, noOfs=%d, date=%s", egid, noOfs, XmlUtils.xmlcal2regdate(cal));
				}
			});
		}
	}

	public Pays getPaysDetail(final long l) {
		final long start = tracing.start();
		try {
			return target.getPaysDetail(l);
		}
		finally {
			tracing.end(start, "getPaysDetail", new Object() {
				@Override
				public String toString() {
					return String.format("getPaysDetail=%s", l);
				}
			});
		}
	}

	public Collection<Logiciel> getTousLesLogiciels() {
		final long start = tracing.start();
		try {
			return target.getTousLesLogiciels();
		}
		finally {
			tracing.end(start, "getTousLesLogiciels", new Object() {
				@Override
				public String toString() {
					return String.format("getTousLesLogiciels");
				}
			});
		}
	}

	public Collection<Pays> getTousLesPays() {
		final long start = tracing.start();
		try {
			return target.getTousLesPays();
		}
		finally {
			tracing.end(start, "getTousLesPays", new Object() {
				@Override
				public String toString() {
					return String.format("getTousLesPays");
				}
			});
		}
	}

	public String getUrl(final String s, final Acces acces, final String s1, final ParameterMap parameterMap) {
		final long start = tracing.start();
		try {
			return target.getUrl(s, acces, s1,parameterMap);
		}
		finally {
			tracing.end(start, "getUrl", new Object() {
				@Override
				public String toString() {
					return String.format("getUrl app=%s, acces=%s, target=%s",s,acces.name(),s1);
				}
			});
		}
	}

	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {
			statsService.registerService(SERVICE_NAME, tracing);
		}
	}

	public void destroy() throws Exception {
		if (statsService != null) {
			statsService.unregisterService(SERVICE_NAME);
		}
	}
}
