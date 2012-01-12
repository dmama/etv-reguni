package ch.vd.uniregctb.interfaces.service.host;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuImpl;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;

public class ServiceCivilServiceHostInterfaces extends ServiceCivilServiceBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceHostInterfaces.class);

	private ServiceCivil serviceCivil;

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
	public Individu getIndividu(long noIndividu, int annee, AttributeIndividu... parties) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getIndividu(" + noIndividu + ", " + annee + ')');
		}
		try {
			Individu ind = IndividuImpl.get(serviceCivil.getIndividu(noIndividu, annee, AttributeIndividu.toEAI(parties)));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("End of getIndividu(" + noIndividu + ", " + annee + ')');
			}
			if (ind != null) {
				assertCoherence(noIndividu, ind.getNoTechnique());
			}
			return ind;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer l'individu n°" + noIndividu + " pour l'année " + annee, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer l'individu n°" + noIndividu + " pour l'année " + annee, e);
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, AttributeIndividu... parties) {
		try {
			// l'appel à l'EJB a besoin d'une collection sérialisable
			if (!(nosIndividus instanceof Serializable)) {
				nosIndividus = new ArrayList<Long>(nosIndividus);
			}
			final Collection<ch.vd.registre.civil.model.Individu> individus = serviceCivil.getIndividus(nosIndividus, annee, AttributeIndividu.toEAI(parties));

			final List<Individu> list = new ArrayList<Individu>(individus.size());
			for (ch.vd.registre.civil.model.Individu ind : individus) {
				final Individu individu = IndividuImpl.get(ind);
				if (individu != null) {
					list.add(individu);
				}
			}

			return list;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " pour l'année " + annee, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " pour l'année " + annee, e);
		}
	}

	@Override
	public boolean isWarmable() {
		return false;
	}
}
