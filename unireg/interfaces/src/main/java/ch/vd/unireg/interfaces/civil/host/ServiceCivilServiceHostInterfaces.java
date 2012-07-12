package ch.vd.unireg.interfaces.civil.host;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.registre.civil.service.ServiceCivil;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.ServiceCivilInterceptor;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.IndividuImpl;
import ch.vd.uniregctb.common.JvmVersionHelper;

public class ServiceCivilServiceHostInterfaces implements ServiceCivilRaw {

//	private static final Logger LOGGER = Logger.getLogger(ServiceCivilServiceHostInterfaces.class);

	private ServiceCivil serviceCivil;
	private ServiceCivilInterceptor interceptor;

	public ServiceCivilServiceHostInterfaces() {
		JvmVersionHelper.checkJvmWrtHostInterfaces();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivil(ServiceCivil serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setInterceptor(ServiceCivilInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {
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

			if (interceptor != null) {
				interceptor.afterGetIndividu(ind, noIndividu, date, parties);
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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) {
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

			if (interceptor != null) {
				interceptor.afterGetIndividus(list, nosIndividus, date, parties);
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
	public void ping() throws ServiceCivilException {
		final Individu individu = getIndividu(611836, null); // Francis Perroset
		if (individu == null) {
			throw new ServiceCivilException("L'individu n°611836 est introuvable");
		}
		if (individu.getNoTechnique() != 611836) {
			throw new ServiceCivilException("Demandé l'individu n°611836, reçu l'individu n°" + individu.getNoTechnique());
		}
	}

	@Override
	public boolean isWarmable() {
		return false;
	}
}
