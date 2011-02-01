package ch.vd.uniregctb.wsclient.fidor;

import java.util.Collection;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.fidor.ws.v2.Acces;
import ch.vd.fidor.ws.v2.Logiciel;
import ch.vd.fidor.ws.v2.ParameterMap;
import ch.vd.fidor.ws.v2.Pays;
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


	public Logiciel getLogicielDetail(final long l) {
		final long start = tracing.start();
		try {
			return target.getLogicielDetail(l);
		}
		finally {
			tracing.end(start, "getLogicielDetail", new Object() {
				@Override
				public String toString() {
					return String.format("getLogicielDetail=%s", l);
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
