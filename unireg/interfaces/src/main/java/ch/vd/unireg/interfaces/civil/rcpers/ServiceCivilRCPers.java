package ch.vd.unireg.interfaces.civil.rcpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0006.v1.Event;
import ch.vd.evd0006.v1.EventIdentification;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.ServiceCivilInterceptor;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.NationaliteRCPers;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.common.BatchIterator;
import ch.vd.uniregctb.common.StandardBatchIterator;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class ServiceCivilRCPers implements ServiceCivilRaw {

//	private static final Logger LOGGER = Logger.getLogger(ServiceCivilRCPers.class);

	private RcPersClient client;
	private ServiceInfrastructureRaw infraService;
	private ServiceCivilInterceptor interceptor;

	private static final int NB_PARAMS_MAX_PAR_GET = 100;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(RcPersClient client) {
		this.client = client;
	}

	public void setInfraService(ServiceInfrastructureRaw infraService) {
		this.infraService = infraService;
	}

	public void setInterceptor(ServiceCivilInterceptor interceptor) {
		this.interceptor = interceptor;
	}

	@Override
	public Individu getIndividu(long noIndividu, @Nullable RegDate date, AttributeIndividu... parties) {

		final ListOfPersons list = getPersonsSafely(Arrays.asList(noIndividu), date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu = " + noIndividu);
		}

		// il faut demander les relations entre individus dans un appel séparé
		final List<Relationship> relations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			final ListOfRelations rel = getRelationsSafely(Arrays.asList(noIndividu), date, true);
			if (rel != null && rel.getListOfResults().getResult() != null && !rel.getListOfResults().getResult().isEmpty()) {
				if (rel.getListOfResults().getResult().size() > 1) {
					throw new ServiceCivilException("Plusieurs relations d'individu trouvés avec le même numéro d'individu = " + noIndividu);
				}
				final Relations r = rel.getListOfResults().getResult().get(0).getRelation();
				relations = (r == null ? null : r.getRelationshipHistory());
			}
			else {
				relations = null;
			}
		}
		else {
			relations = null;
		}

		final Person person = list.getListOfResults().getResult().get(0).getPerson();
		final Individu individu = IndividuRCPers.get(person, relations, true, infraService);
		if (individu != null) {
			long actual = individu.getNoTechnique();
			if (noIndividu != actual) {
				throw new IllegalArgumentException(String.format(
						"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noIndividu, actual));
			}
		}

		if (interceptor != null) {
			interceptor.afterGetIndividu(individu, noIndividu, date, parties);
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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, @Nullable RegDate date, AttributeIndividu... parties) {

		final ListOfPersons list = getPersonsSafely(nosIndividus, date, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return Collections.emptyList();
		}

		// il faut demander les relations entre individus dans un appel séparé
		final Map<Long, List<Relationship>> allRelations;
		if (parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS)) {
			final ListOfRelations rel = getRelationsSafely(nosIndividus, date, true);
			if (rel != null && rel.getListOfResults().getResult() != null) {
				allRelations = new HashMap<Long, List<Relationship>>();
				for (ListOfRelations.ListOfResults.Result relRes : rel.getListOfResults().getResult()) {
					final Relations relations = relRes.getRelation();
					if (relations != null) {
						final List<Relationship> relationship = relations.getRelationshipHistory();
						if (relationship != null) {
							allRelations.put(IndividuRCPers.getNoIndividu(relations.getLocalPersonId()), relationship);
						}
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
				final List<Relationship> relations = allRelations == null ? null : allRelations.get(IndividuRCPers.getNoIndividu(person));
				final Individu individu = IndividuRCPers.get(person, relations, true, infraService);
				individus.add(individu);
			}
		}

		if (interceptor != null) {
			interceptor.afterGetIndividus(individus, nosIndividus, date, parties);
		}

		return individus;
	}

	private ListOfPersons getPersonsSafely(Collection<Long> ids, RegDate date, boolean withHistory) {
		try {
			ListOfPersons list = null;
			final BatchIterator<Long> batches = new StandardBatchIterator<Long>(ids, NB_PARAMS_MAX_PAR_GET);
			while (batches.hasNext()) {
				final ListOfPersons localList = client.getPersons(batches.next(), date, withHistory);
				if (list == null) {
					list = localList;
				}
				else {
					list.setNumberOfResults(list.getNumberOfResults().add(localList.getNumberOfResults()));
					list.getListOfResults().getResult().addAll(localList.getListOfResults().getResult());
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new ServiceCivilException(e);
		}
	}

	private ListOfRelations getRelationsSafely(Collection<Long> nosIndividus, RegDate date, boolean withHistory) {
		try {
			ListOfRelations list = null;
			final BatchIterator<Long> batches = new StandardBatchIterator<Long>(nosIndividus, NB_PARAMS_MAX_PAR_GET);
			while (batches.hasNext()) {
				final ListOfRelations localList = client.getRelations(batches.next(), date, withHistory);
				if (list == null) {
					list = localList;
				}
				else {
					list.setNumberOfResults(list.getNumberOfResults().add(localList.getNumberOfResults()));
					list.getListOfResults().getResult().addAll(localList.getListOfResults().getResult());
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new ServiceCivilException(e);
		}
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		final Event ref = client.getEvent(eventId);
		if (ref != null) {
			final Event.PersonAfterEvent personAfterEvent = ref.getPersonAfterEvent();
			final Individu individu = IndividuRCPers.get(personAfterEvent.getPerson(), personAfterEvent.getRelations(), false, infraService);
			final EventIdentification idtf = ref.getIdentification();
			final Long refMessageId = idtf.getReferenceMessageId();
			final RegDate dateEvt = XmlUtils.xmlcal2regdate(idtf.getDate());
			final TypeEvenementCivilEch type = TypeEvenementCivilEch.fromEchCode(idtf.getType());
			final ActionEvenementCivilEch action = ActionEvenementCivilEch.fromEchCode(idtf.getAction());
			return new IndividuApresEvenement(individu, dateEvt, type, action, refMessageId);
		}
		return null;
	}

	@Override
	public Nationalite getNationaliteAt(long noIndividu, @Nullable RegDate date) {

		final ListOfPersons list = getPersonsSafely(Arrays.asList(noIndividu), date, false);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu = " + noIndividu);
		}

		Nationalite nationalite = null;

		final Person person = list.getListOfResults().getResult().get(0).getPerson();
		if (person != null) {
			nationalite = NationaliteRCPers.get(person, infraService);
		}

		return nationalite;
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
