package ch.vd.uniregctb.interfaces.service.rcpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.ForceLogger;
import ch.vd.uniregctb.interfaces.IndividuDumper;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;
import ch.vd.uniregctb.interfaces.service.ServiceCivilException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilServiceBase;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;

public class ServiceCivilRCPers extends ServiceCivilServiceBase {

	private static final Logger LOGGER = Logger.getLogger(ServiceCivilRCPers.class);

	private RcPersClient client;

	private final ThreadLocal<MutableBoolean> dumpIndividu = new ThreadLocal<MutableBoolean>() {
		@Override
		protected MutableBoolean initialValue() {
			return new MutableBoolean(false);
		}
	};

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(RcPersClient client) {
		this.client = client;
	}

	@Override
	public Individu getIndividu(long noIndividu, RegDate date, AttributeIndividu... parties) {

		final ListOfPersons list = getPersonsSafely(Arrays.asList(noIndividu), date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu.");
		}

		// il faut demander les relations entre individus dans un appel séparé
		final Relations relations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			final ListOfRelations rel = getRelationsSafely(Arrays.asList(noIndividu), date, true);
			if (rel != null && rel.getListOfResults().getResult() != null && !rel.getListOfResults().getResult().isEmpty()) {
				if (rel.getListOfResults().getResult().size() > 1) {
					throw new ServiceCivilException("Plusieurs relations d'individu trouvés avec le même numéro d'individu.");
				}
				relations = rel.getListOfResults().getResult().get(0).getRelation();
			}
			else {
				relations = null;
			}
		}
		else {
			relations = null;
		}

		final Person person = list.getListOfResults().getResult().get(0).getPerson();
		final Individu individu = IndividuRCPers.get(person, relations, infraService);
		if (individu != null) {
			assertCoherence(noIndividu, individu.getNoTechnique());
		}

		if (LOGGER.isTraceEnabled() || dumpIndividu.get().booleanValue()) {
			final String message = String.format("getIndividu(noIndividu=%d, date=%s, parties=%s) => %s", noIndividu, ServiceTracing.toString(date), ServiceTracing.toString(parties),
					IndividuDumper.dump(individu, false, false, false));
			// force le log en mode trace, même si le LOGGER n'est pas en mode trace
			new ForceLogger(LOGGER).trace(message);
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

		final ListOfPersons list = getPersonsSafely(nosIndividus, date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return Collections.emptyList();
		}

		// il faut demander les relations entre individus dans un appel séparé
		final Map<Long, Relations> allRelations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			final ListOfRelations rel = getRelationsSafely(nosIndividus, date, true);
			if (rel != null && rel.getListOfResults().getResult() != null) {
				allRelations = new HashMap<Long, Relations>();
				for (ListOfRelations.ListOfResults.Result relRes : rel.getListOfResults().getResult()) {
					final Relations relations = relRes.getRelation();
					if (relations != null) {
						allRelations.put(IndividuRCPers.getNoIndividu(relations.getLocalPersonId()), relations);
					}
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

		for (ListOfPersons.ListOfResults.Result personRes : list.getListOfResults().getResult()) {
			final Person person = personRes.getPerson();
			if (person != null) {
				final Relations relations = allRelations == null ? null : allRelations.get(IndividuRCPers.getNoIndividu(person));
				final Individu individu = IndividuRCPers.get(person, relations, infraService);
				individus.add(individu);
			}
		}

		if (LOGGER.isTraceEnabled() || dumpIndividu.get().booleanValue()) {
			final String message = String.format("getIndividus(nosIndividus=%s, date=%s, parties=%s) => %s", ServiceTracing.toString(nosIndividus), ServiceTracing.toString(date),
					ServiceTracing.toString(parties), IndividuDumper.dump(individus, false, false));
			// force le log en mode trace, même si le LOGGER n'est pas en mode trace
			new ForceLogger(LOGGER).trace(message);
		}

		return individus;
	}

	private ListOfPersons getPersonsSafely(Collection<Long> ids, RegDate date, boolean withHistory) {
		try {
			return client.getPersons(ids, date, withHistory);
		}
		catch (Exception e) {
			throw new ServiceCivilException(e);
		}
	}

	private ListOfRelations getRelationsSafely(Collection<Long> nosIndividus, RegDate date, boolean withHistory) {
		try {
			return client.getRelations(nosIndividus, date, withHistory);
		}
		catch (Exception e) {
			throw new ServiceCivilException(e);
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
