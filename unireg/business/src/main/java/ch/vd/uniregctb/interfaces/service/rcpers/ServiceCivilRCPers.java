package ch.vd.uniregctb.interfaces.service.rcpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.evd0001.v2.ListOfPersons;
import ch.vd.evd0001.v2.Person;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;

public class ServiceCivilRCPers extends ServiceCivilServiceBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilRCPers.class);

	private RcPersClientImpl client;

	public void setClient(RcPersClientImpl client) {
		this.client = client;
	}

	@Override
	public Individu getIndividu(long noIndividu, int annee, AttributeIndividu... parties) {

		final ListOfPersons list = client.getPeople(Arrays.asList(noIndividu), RegDate.get(annee, 12, 31));
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu.");
		}

		final Person person = list.getListOfResults().getPerson().get(0);
		final Individu individu = IndividuRCPers.get(person, infraService);
		if (individu != null) {
			assertCoherence(noIndividu, individu.getNoTechnique());
		}

		return individu;
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, int annee, AttributeIndividu... parties) {

		final ListOfPersons list = client.getPeople(nosIndividus, RegDate.get(annee, 12, 31));
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu.");
		}

		final List<Individu> individus = new ArrayList<Individu>(nosIndividus.size());

		final List<Person> people = list.getListOfResults().getPerson();
		for (Person person : people) {
			final Individu individu = IndividuRCPers.get(person, infraService);
			individus.add(individu);

		}

		return individus;
	}

	@Override
	public boolean isWarmable() {
		return false;
	}
}
