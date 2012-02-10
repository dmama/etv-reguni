package ch.vd.uniregctb.interfaces.service.rcpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;

public class ServiceCivilRCPers extends ServiceCivilServiceBase {

//	private static final Logger LOGGER = Logger.getLogger(ServiceCivilRCPers.class);

	private RcPersClient client;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(RcPersClient client) {
		this.client = client;
	}

	@Override
	public Individu getIndividu(long noIndividu, RegDate date, AttributeIndividu... parties) {

		final ListOfPersons list = client.getPersons(Arrays.asList(noIndividu), date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu.");
		}

		// il faut demander les relations entre individus dans un appel séparé
		final Relations relations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			ListOfRelations rel = client.getRelations(Arrays.asList(noIndividu), date, true);
			if (rel != null && rel.getListOfResults().getRelation() != null && !rel.getListOfResults().getRelation().isEmpty()) {
				if (rel.getListOfResults().getRelation().size() > 1) {
					throw new ServiceCivilException("Plusieurs relations d'individu trouvés avec le même numéro d'individu.");
				}
				relations = rel.getListOfResults().getRelation().get(0);
			}
			else {
				relations = null;
			}
		}
		else {
			relations = null;
		}

		final Person person = list.getListOfResults().getPerson().get(0);
		final Individu individu = IndividuRCPers.get(person, relations, infraService);
		if (individu != null) {
			assertCoherence(noIndividu, individu.getNoTechnique());
		}

		return individu;
	}

	private boolean containsAny(AttributeIndividu[] container, AttributeIndividu... values) {
		for (AttributeIndividu i : container) {
			for (AttributeIndividu j : values) {
				if (i == j) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<Individu> getIndividus(Collection<Long> nosIndividus, RegDate date, AttributeIndividu... parties) {

		final ListOfPersons list = client.getPersons(nosIndividus, date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		// il faut demander les relations entre individus dans un appel séparé
		final Map<Long, Relations> allRelations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			final ListOfRelations rel = client.getRelations(nosIndividus, date, true);
			if (rel != null && rel.getListOfResults().getRelation() != null) {
				allRelations = new HashMap<Long, Relations>();
				for (Relations relations : rel.getListOfResults().getRelation()) {
					allRelations.put(IndividuRCPers.getNoIndividu(relations.getPersonId()), relations);
				}
			}
			else {
				allRelations = null;
			}
		}
		else {
			allRelations = null;
		}

		final List<Individu> individus = new ArrayList<Individu>(nosIndividus.size());

		final List<Person> people = list.getListOfResults().getPerson();
		for (Person person : people) {
			final Relations relations = allRelations == null ? null : allRelations.get(IndividuRCPers.getNoIndividu(person));
			final Individu individu = IndividuRCPers.get(person, relations, infraService);
			individus.add(individu);
		}

		return individus;
	}

	@Override
	public boolean isWarmable() {
		return false;
	}
}
