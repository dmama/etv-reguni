package ch.vd.uniregctb.interfaces.service.host;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.IndividuDumper;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuImpl;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;

public class ServiceCivilServiceHostInterfaces extends ServiceCivilServiceBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceHostInterfaces.class);

	private ServiceCivil serviceCivil;

	private final ThreadLocal<MutableBoolean> dumpIndividu = new ThreadLocal<MutableBoolean>() {
		@Override
		protected MutableBoolean initialValue() {
			return new MutableBoolean(false);
		}
	};

	public ServiceCivilServiceHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	/**
	 * @param serviceCivil
	 *            the serviceCivil to set
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivil serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@Override
	public Individu getIndividu(long noIndividu, RegDate date, AttributeIndividu... parties) {
		try {
			final int annee = date == null ? 2400 : date.year();
			Individu ind = IndividuImpl.get(serviceCivil.getIndividu(noIndividu, annee, AttributeIndividu.toEAI(parties)), date, parties);
			if (ind != null) {
				assertCoherence(noIndividu, ind.getNoTechnique());
			}

			if (LOGGER.isTraceEnabled() || dumpIndividu.get().booleanValue()) {
				final String message = String.format("getIndividu(noIndividu=%d, date=%s, parties=%s) => %s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties),
						IndividuDumper.dump(ind, false, false, false));
				// force le log en mode trace, même si le LOGGER n'est pas en mode trace
				LOGGER.callAppenders(new LoggingEvent(Logger.class.getName(), LOGGER, Level.TRACE, message, null));
			}

			return ind;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer l'individu n°" + noIndividu + " à la date " + date, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer l'individu n°" + noIndividu + " à la date " + date, e);
		}
	}

	@SuppressWarnings({"unchecked"})
	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {
		try {
			// l'appel à l'EJB a besoin d'une collection sérialisable
			if (!(nosIndividus instanceof Serializable)) {
				nosIndividus = new ArrayList<Long>(nosIndividus);
			}
			final int annee = date == null ? 2400 : date.year();
			final Collection<ch.vd.registre.civil.model.Individu> individus = serviceCivil.getIndividus(nosIndividus, annee, AttributeIndividu.toEAI(parties));

			final List<Individu> list = new ArrayList<Individu>(individus.size());
			for (ch.vd.registre.civil.model.Individu ind : individus) {
				final Individu individu = IndividuImpl.get(ind, date, parties);
				if (individu != null) {
					list.add(individu);
				}
			}

			if (LOGGER.isTraceEnabled() || dumpIndividu.get().booleanValue()) {
				final String message = String.format("getIndividus(nosIndividus=%s, date=%s, parties=%s) => %s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date),
						ServiceTracing.toString(parties), IndividuDumper.dump(list, false, false));
				// force le log en mode trace, même si le LOGGER n'est pas en mode trace
				LOGGER.callAppenders(new LoggingEvent(Logger.class.getName(), LOGGER, Level.TRACE, message, null));
			}

			return list;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " à la date " + date, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " à la date " + date, e);
		}
	}

	@Override
	public boolean isWarmable() {
		return false;
	}

	@Override
	public void setIndividuLogger(boolean value) {
		dumpIndividu.get().setValue(value);
	}
}
