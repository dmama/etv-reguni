package ch.vd.uniregctb.interfaces.service;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.wrapper.IndividuWrapper;

public class ServiceCivilServiceImpl extends ServiceCivilServiceBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceImpl.class);

	private ServiceCivil serviceCivil;

	public ServiceCivilServiceImpl() {
		// l'EJB de HostInterface a besoin d'une version 1.5
		JvmVersionHelper.checkJava_1_5();
	}

	/**
	 * @param serviceCivil
	 *            the serviceCivil to set
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivil serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public Individu getIndividu(long noIndividu, int annee, EnumAttributeIndividu... parties) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getIndividu(" + noIndividu + ", " + annee + ")");
		}
		try {
			Individu ind = IndividuWrapper.get(serviceCivil.getIndividu(noIndividu, annee, parties));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("End of getIndividu(" + noIndividu + ", " + annee + ")");
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

	@SuppressWarnings({"unchecked"})
	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, EnumAttributeIndividu... parties) {
		try {
			// l'appel à l'EJB a besoin d'une collection sérialisable
			if (!(nosIndividus instanceof Serializable)) {
				nosIndividus = new ArrayList<Long>(nosIndividus);
			}
			final Collection<ch.vd.registre.civil.model.Individu> individus = serviceCivil.getIndividus(nosIndividus, annee, parties);

			final List<Individu> list = new ArrayList<Individu>(individus.size());
			for (ch.vd.registre.civil.model.Individu ind : individus) {
				final Individu individu = IndividuWrapper.get(ind);
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

	public boolean isWarmable() {
		return false;
	}

	/**
	 * Vérifie que l'id de l'individu retourné corresponds bien à celui demandé.
	 *
	 * @param expected la valeur attendue
	 * @param actual   la valeur constatée
	 */
	private void assertCoherence(long expected, long actual) {
		if (expected != actual) {
			throw new IllegalArgumentException(String.format(
					"Incohérence des données retournées détectées: tiers demandé = %d, tiers retourné = %d.", expected, actual));
		}
	}
}
