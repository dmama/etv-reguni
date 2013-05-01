package ch.vd.uniregctb.interfaces.service;

import java.util.Collection;

import org.apache.log4j.Logger;

import ch.vd.unireg.interfaces.civil.ServiceCivilInterceptor;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.ForceLogger;
import ch.vd.uniregctb.common.ThreadSwitch;
import ch.vd.uniregctb.interfaces.IndividuDumper;
import ch.vd.uniregctb.stats.ServiceTracing;

public class ServiceCivilLoggerImpl implements ServiceCivilLogger, ServiceCivilInterceptor {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilLoggerImpl.class);

	private final ThreadSwitch dumpIndividu = new ThreadSwitch(false);

	@Override
	public void afterGetIndividu(Individu individu, long noIndividu, AttributeIndividu... parties) {
		if (LOGGER.isTraceEnabled() || dumpIndividu.isEnabled()) {
			final String message = String.format("getIndividu(noIndividu=%d, parties=%s) => %s", noIndividu, ServiceTracing.toString(parties),
					IndividuDumper.dump(individu, false, false, false));
			// force le log en mode trace, même si le LOGGER n'est pas en mode trace
			new ForceLogger(LOGGER).trace(message);
		}
	}

	@Override
	public void afterGetIndividus(Collection<Individu> individus, Collection<Long> nosIndividus, AttributeIndividu... parties) {
		if (LOGGER.isTraceEnabled() || dumpIndividu.isEnabled()) {
			final String message = String.format("getIndividus(nosIndividus=%s, parties=%s) => %s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(parties),
					IndividuDumper.dump(individus, false, false));
			// force le log en mode trace, même si le LOGGER n'est pas en mode trace
			new ForceLogger(LOGGER).trace(message);
		}
	}

	@Override
	public void setIndividuLogging(boolean value) {
		dumpIndividu.setEnabled(value);
	}
}
