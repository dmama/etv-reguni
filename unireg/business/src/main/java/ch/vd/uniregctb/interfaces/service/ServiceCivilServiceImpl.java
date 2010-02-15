package ch.vd.uniregctb.interfaces.service;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.uniregctb.common.JvmVersionHelper;
import ch.vd.uniregctb.interfaces.model.*;
import ch.vd.uniregctb.interfaces.model.wrapper.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, EnumAttributeIndividu... parties) {
		try {
			final List<Individu> list = new ArrayList<Individu>(nosIndividus.size());

			final int annee = (date == null ? 2400 : date.year());
			final Collection<ch.vd.registre.civil.model.Individu> individus = serviceCivil.getIndividus(nosIndividus, annee, parties);

			for (ch.vd.registre.civil.model.Individu ind : individus) {
				Individu individu = IndividuWrapper.get(ind);
				if (individu != null) {
					list.add(individu);
				}
			}

			return list;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " pour la date " + date, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer les individus n° " + ArrayUtils.toString(nosIndividus.toArray()) + " pour la date " + date, e);
		}
	}

	public Collection<Nationalite> getNationalites(long noIndividu, int annee) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getNationalites(" + noIndividu + ", " + annee + ")");
		}

		Collection<?> nats;
		try {
			nats = serviceCivil.getNationalites(noIndividu, annee);
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer les nationalités de l'individu n°" + noIndividu + " pour l'année "
					+ annee, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer les nationalités de l'individu n°" + noIndividu + " pour l'année "
					+ annee, e);
		}

		List<Nationalite> nationalites = new ArrayList<Nationalite>();
		for (Object o : nats) {
			ch.vd.registre.civil.model.Nationalite n = (ch.vd.registre.civil.model.Nationalite) o;
			nationalites.add(NationaliteWrapper.get(n));
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("End of getNationalites(" + noIndividu + ", " + annee + ")");
		}
		return nationalites;
	}

	public Origine getOrigine(long noIndividu, int annee) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getOrigine(" + noIndividu + ", " + annee + ")");
		}
		try {
			Origine origine = OrigineWrapper.get(serviceCivil.getOrigine(noIndividu, annee));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("End of getOrigine(" + noIndividu + ", " + annee + ")");
			}
			return origine;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer l'origine de l'individu n°" + noIndividu + " pour l'année " + annee, e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer l'origine de l'individu n°" + noIndividu + " pour l'année " + annee, e);
		}
	}

	public Collection<Permis> getPermis(long noIndividu, int annee) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getPermis(" + noIndividu + ", " + annee + ")");
		}

		Collection<?> perms;
		try {
			perms = serviceCivil.getPermis(noIndividu, annee);
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer les permis de l'individu n°" + noIndividu + " pour l'année " + annee,
					e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer les permis de l'individu n°" + noIndividu + " pour l'année " + annee,
					e);
		}

		List<Permis> permis = new ArrayList<Permis>();
		for (Object o : perms) {
			ch.vd.registre.civil.model.Permis p = (ch.vd.registre.civil.model.Permis) o;
			permis.add(PermisWrapper.get(p));
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("End of getPermis(" + noIndividu + ", " + annee + ")");
		}
		return permis;
	}

	public Tutelle getTutelle(long noIndividu, int annee) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Calling getTutelle(" + noIndividu + ", " + annee + ")");
		}
		try {
			Tutelle tut = TutelleWrapper.get(serviceCivil.getTutelle(noIndividu, annee));
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("End of getTutelle(" + noIndividu + ", " + annee + ")");
			}
			return tut;
		}
		catch (RemoteException e) {
			throw new ServiceCivilException("Impossible de récupérer la tutelle de l'individu n°" + noIndividu + " pour l'année " + annee,
					e);
		}
		catch (RegistreException e) {
			throw new ServiceCivilException("Impossible de récupérer la tutelle de l'individu n°" + noIndividu + " pour l'année " + annee,
					e);
		}
	}

	public void setUp(ServiceCivilService target) {
		throw new NotImplementedException();
	}

	public void tearDown() {
		throw new NotImplementedException();
	}

	public boolean isWarmable() {
		return false;
	}

	public void warmCache(List<Individu> individus, RegDate date, EnumAttributeIndividu... parties) {
		// rien à faire ici
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
