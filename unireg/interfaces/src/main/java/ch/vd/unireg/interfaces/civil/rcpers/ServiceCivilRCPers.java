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
import ch.vd.evd0001.v3.NotReturnedPerson;
import ch.vd.evd0001.v3.NotReturnedRelation;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0004.v2.Error;
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
	private static final Integer NOT_FOUND_PERSON = 4; // voir le fichier http://subversion.etat-de-vaud.ch/SVN_ACI/registre/rcpers/trunk/06-Deploiement/ManuelsTechniques/TEC-ServicesEchangesDonnees-3-0.doc

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
	public Individu getIndividu(long noIndividu, AttributeIndividu... parties) throws ServiceCivilException {

		// on récupère la personne
		final ListOfPersons list = getPersonsSafely(Arrays.asList(noIndividu), null, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés avec le même numéro d'individu = " + noIndividu);
		}

		final Person person = extractPerson(list.getListOfResults().getResult().get(0));
		if (person == null) {
			return null;
		}

		// il faut demander les relations entre individus dans un appel séparé
		final boolean withRelations = isWithRelations(parties);
		final List<Relationship> relations = withRelations ? getRelationsPourIndividu(noIndividu) : null;

		// on peut maintenant construire l'individu
		final Individu individu = IndividuRCPers.get(person, relations, true, withRelations, infraService);
		if (individu != null) {
			long actual = individu.getNoTechnique();
			if (noIndividu != actual) {
				throw new IllegalArgumentException(String.format(
						"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noIndividu, actual));
			}
		}

		if (interceptor != null) {
			interceptor.afterGetIndividu(individu, noIndividu, parties);
		}

		return individu;
	}

	private static boolean isWithRelations(AttributeIndividu... parties) {
		return parties != null && containsAny(parties, AttributeIndividu.PARENTS, AttributeIndividu.ENFANTS, AttributeIndividu.CONJOINTS);
	}

	@Nullable
	private List<Relationship> getRelationsPourIndividu(long noIndividu) {
		final List<Relationship> relations;
		final ListOfRelations rel = getRelationsSafely(Arrays.asList(noIndividu), null, true);
		if (rel != null && rel.getListOfResults().getResult() != null && !rel.getListOfResults().getResult().isEmpty()) {
			if (rel.getListOfResults().getResult().size() > 1) {
				throw new ServiceCivilException("Plusieurs relations d'individu trouvés avec le même numéro d'individu = " + noIndividu);
			}
			relations = extractRelations(noIndividu, rel.getListOfResults().getResult().get(0));
		}
		else {
			relations = null;
		}
		return relations;
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {

		// on récupère la personne
		final ListOfPersons list = getPersonFromEventSafely(evtId, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new ServiceCivilException("Plusieurs individus trouvés d'après le même numéro d'événement = " + evtId);
		}

		final Person person = extractPerson(list.getListOfResults().getResult().get(0));
		if (person == null) {
			return null;
		}

		// il faut demander les relations entre individus dans un appel séparé
		final long noIndividu = IndividuRCPers.getNoIndividu(person);
		final boolean withRelations = isWithRelations(parties);
		final List<Relationship> relations = withRelations ? getRelationsPourIndividu(noIndividu) : null;

		// on peut maintenant construire l'individu
		final Individu individu = IndividuRCPers.get(person, relations, true, withRelations, infraService);
		if (individu != null) {
			long actual = individu.getNoTechnique();
			if (noIndividu != actual) {
				throw new IllegalArgumentException(String.format(
						"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noIndividu, actual));
			}
		}

		return individu;
	}

	/**
	 * Interprète le résultat retourné par RcPers et en extrait la personne valide.
	 *
	 * @param result le résultat retourné par RcPers
	 * @return une personne; ou <b>null</b> si la personne n'existe pas.
	 * @throws ServiceCivilException si RcPers à retourné un code d'erreur qui correspond à une erreur chez eux.
	 */
	@Nullable
	private static Person extractPerson(ListOfPersons.ListOfResults.Result result) throws ServiceCivilException {

		// [SIFISC-6685] on détecte les cas où on ne reçoit rien parce qu'il y a un bug dans RcPers
		final NotReturnedPerson notReturnedPerson = result.getNotReturnedPerson();
		if (notReturnedPerson != null) {
			final String noIndividu = notReturnedPerson.getLocalPersonId().getPersonId();
			final String category = notReturnedPerson.getLocalPersonId().getPersonIdCategory();
			final List<Error> reasons = notReturnedPerson.getReason();
			if (reasons.size() != 1) {
				throw new IllegalArgumentException("Plusieurs raisons codes d'erreur retournés par RcPers sur l'appel à l'individu = " + category + "/" + noIndividu);
			}
			final Integer code = reasons.get(0).getCode();
			if (code.equals(NOT_FOUND_PERSON)) {
				return null; // le seul cas où la personne n'est pas retournée parce qu'elle n'existe simplement pas
			}
			else {
				throw new ServiceCivilException("RcPers a retourné le code d'erreur " + code + " (" + reasons.get(0).getMessage() + ") sur l'appel à l'individu = " + category + "/" + noIndividu);
			}
		}

		return result.getPerson();
	}

	/**
	 * Interprète le résultat retourné par RcPers et en extrait les relations valides de la personne.
	 *
	 * @param result le résultat retourné par RcPers
	 * @return les relations d'une personne; ou <b>null</b> si la personne ne possède pas de relations.
	 * @throws ServiceCivilException si RcPers à retourné un code d'erreur qui correspond à une erreur chez eux.
	 */
	@Nullable
	private static List<Relationship> extractRelations(long noIndividu, ListOfRelations.ListOfResults.Result result) throws ServiceCivilException {

		// [SIFISC-6685] on détecte les cas où on ne reçoit rien parce qu'il y a un bug dans RcPers
		final NotReturnedRelation notReturnedRelation = result.getNotReturnedRelation();
		if (notReturnedRelation != null) {
			final List<Error> reasons = notReturnedRelation.getReason();
			if (reasons.size() != 1) {
				throw new IllegalArgumentException("Plusieurs raisons codes d'erreur retournés par RcPers sur l'appel aux relations de l'individu = " + noIndividu);
			}
			final Integer code = reasons.get(0).getCode();
			if (code.equals(NOT_FOUND_PERSON)) {
				return null; // le seul cas où les relations de la personne nesont pas retournée parce qu'elles n'existent simplement pas
			}
			else {
				throw new ServiceCivilException("RcPers a retourné le code d'erreur " + code + " (" + reasons.get(0).getMessage() + ") sur l'appel aux relations de l'individu = " + noIndividu);
			}
		}

		final Relations r = result.getRelation();
		return (r == null ? null : r.getRelationshipHistory());
	}

	private static boolean containsAny(AttributeIndividu[] container, AttributeIndividu... values) {
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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws ServiceCivilException {

		// on récupère les personnes
		final ListOfPersons list = getPersonsSafely(nosIndividus, null, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return Collections.emptyList();
		}

		final List<Person> persons = new ArrayList<>(list.getNumberOfResults().intValue());
		for (ListOfPersons.ListOfResults.Result personRes : list.getListOfResults().getResult()) {
			final Person person = extractPerson(personRes);
			if (person != null) {
				persons.add(person);
			}
		}

		// il faut demander les relations entre individus dans un appel séparé
		final boolean withRelations = isWithRelations(parties);
		final Map<Long, List<Relationship>> allRelations;
		if (withRelations) {
			final ListOfRelations rel = getRelationsSafely(nosIndividus, null, true);
			if (rel != null && rel.getListOfResults().getResult() != null) {
				allRelations = new HashMap<>();
				for (ListOfRelations.ListOfResults.Result relRes : rel.getListOfResults().getResult()) {
					final List<Relationship> relations = extractRelations(0, relRes);
					if (relations != null) {
						allRelations.put(IndividuRCPers.getNoIndividu(relRes.getRelation().getLocalPersonId()), relations);
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

		// on peut maintenant construire les individus
		final List<Individu> individus = new ArrayList<>(nosIndividus.size());
		for (Person person : persons) {
			final List<Relationship> relations = allRelations == null ? null : allRelations.get(IndividuRCPers.getNoIndividu(person));
			final Individu individu = IndividuRCPers.get(person, relations, true, withRelations, infraService);
			individus.add(individu);
		}

		if (interceptor != null) {
			interceptor.afterGetIndividus(individus, nosIndividus, parties);
		}

		return individus;
	}

	private ListOfPersons getPersonsSafely(Collection<Long> ids, @Nullable RegDate date, boolean withHistory) {
		if (date != null && withHistory) {
			throw new IllegalArgumentException("Il n'est pas possible de spécifier à la fois une date de validité et de demander l'historique complet d'un individu");
		}
		try {
			ListOfPersons list = null;
			final BatchIterator<Long> batches = new StandardBatchIterator<>(ids, NB_PARAMS_MAX_PAR_GET);
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

	private ListOfPersons getPersonFromEventSafely(long evtId, boolean withHistory) {
		try {
			return client.getPersonByEvent(evtId, null, withHistory);
		}
		catch (Exception e) {
			throw new ServiceCivilException(e);
		}
	}

	private ListOfRelations getRelationsSafely(Collection<Long> nosIndividus, @Nullable RegDate date, boolean withHistory) {
		if (date != null && withHistory) {
			throw new IllegalArgumentException("Il n'est pas possible de spécifier à la fois une date de validité et de demander l'historique complet des relations d'un individu");
		}
		try {
			ListOfRelations list = null;
			final BatchIterator<Long> batches = new StandardBatchIterator<>(nosIndividus, NB_PARAMS_MAX_PAR_GET);
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
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		final Event ref = client.getEvent(eventId);
		if (ref != null) {
			final Event.PersonAfterEvent personAfterEvent = ref.getPersonAfterEvent();
			final Individu individu = IndividuRCPers.get(personAfterEvent.getPerson(), personAfterEvent.getRelations(), false, true, infraService);
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
		final Individu individu = getIndividu(611836); // Francis Perroset
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
