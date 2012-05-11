package ch.vd.unireg.interfaces.civil.host;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.IndividuImpl;
import ch.vd.uniregctb.common.JvmVersionHelper;

public class ServiceCivilServiceHostInterfaces implements ServiceCivilRaw {

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
				long actual = ind.getNoTechnique();
				if (noIndividu != actual) {
					throw new IllegalArgumentException(String.format(
							"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noIndividu, actual));
				}
			}

			if (LOGGER.isTraceEnabled() || dumpIndividu.get().booleanValue()) {
// FIXME (msi) utiliser le service civil interceptor
//				final String message = String.format("getIndividu(noIndividu=%d, date=%s, parties=%s) => %s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties),
//						IndividuDumper.dump(ind, false, false, false));
//				// force le log en mode trace, même si le LOGGER n'est pas en mode trace
//				new ForceLogger(LOGGER).trace(message);
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
// FIXME (msi) utiliser le service civil interceptor
//				final String message = String.format("getIndividus(nosIndividus=%s, date=%s, parties=%s) => %s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date),
//						ServiceTracing.toString(parties), IndividuDumper.dump(list, false, false));
//				// force le log en mode trace, même si le LOGGER n'est pas en mode trace
//				new ForceLogger(LOGGER).trace(message);
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
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		throw new NotImplementedException("La méthode getIndividuFromEvent ne doit pas être appelée sur Host-Interface");
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
