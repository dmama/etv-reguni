package ch.vd.unireg.interfaces.civil.rcpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.evd0001.v5.Event;
import ch.vd.evd0001.v5.EventIdentification;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.evd0001.v5.Person;
import ch.vd.evd0004.v3.Error;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BatchIterator;
import ch.vd.unireg.common.StandardBatchIterator;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.interfaces.civil.IndividuConnector;
import ch.vd.unireg.interfaces.civil.IndividuConnectorException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.TypeEvenementCivilEch;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;

/**
 * Implémentation du connecteur qui utilise le WS v5 de RCPers.
 */
public class IndividuConnectorRCPers implements IndividuConnector {

//	private static final Logger LOGGER = LoggerFactory.getLogger(IndividuConnectorRCPers.class);

	private RcPersClient client;
	private ServiceInfrastructureRaw infraService;

	private static final int NB_PARAMS_MAX_PAR_GET = 100;
	private static final Integer NOT_FOUND_PERSON = 4; // voir le fichier http://subversion.etat-de-vaud.ch/SVN_ACI/registre/rcpers/trunk/06-Deploiement/ManuelsTechniques/TEC-ServicesEchangesDonnees-3-0.doc

	@SuppressWarnings({"UnusedDeclaration"})
	public void setClient(RcPersClient client) {
		this.client = client;
	}

	public void setInfraService(ServiceInfrastructureRaw infraService) {
		this.infraService = infraService;
	}

	@Override
	public Individu getIndividu(long noIndividu, AttributeIndividu... parties) throws IndividuConnectorException {

		// on récupère la personne
		final ListOfPersons list = getPersonsSafely(Collections.singletonList(noIndividu), null, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new IndividuConnectorException("Plusieurs individus trouvés avec le même numéro d'individu = " + noIndividu);
		}

		final Person person = extractPerson(list.getListOfResults().getResult().get(0));
		if (person == null) {
			return null;
		}

		// on peut maintenant construire l'individu
		final Individu individu = IndividuRCPers.get(person, true, infraService);
		if (individu != null) {
			long actual = individu.getNoTechnique();
			if (noIndividu != actual) {
				throw new IllegalArgumentException(String.format(
						"Incohérence des données retournées détectées: individu demandé = %d, individu retourné = %d.", noIndividu, actual));
			}
		}

		return individu;
	}

	@Override
	public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws IndividuConnectorException {

		// on récupère la personne
		final ListOfPersons list = getPersonFromEventSafely(evtId, true);
		if (list == null || list.getNumberOfResults().intValue() == 0) {
			return null;
		}

		if (list.getNumberOfResults().intValue() > 1) {
			throw new IndividuConnectorException("Plusieurs individus trouvés d'après le même numéro d'événement = " + evtId);
		}

		final Person person = extractPerson(list.getListOfResults().getResult().get(0));
		if (person == null) {
			return null;
		}

		// il faut demander les relations entre individus dans un appel séparé
		final long noIndividu = IndividuRCPers.getNoIndividu(person);

		// on peut maintenant construire l'individu
		final Individu individu = IndividuRCPers.get(person, true, infraService);
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
	 * @throws IndividuConnectorException si RcPers à retourné un code d'erreur qui correspond à une erreur chez eux.
	 */
	@Nullable
	private static Person extractPerson(ListOfPersons.ListOfResults.Result result) throws IndividuConnectorException {

		// [SIFISC-6685] on détecte les cas où on ne reçoit rien parce qu'il y a un bug dans RcPers
		final Error notReturnedPersonError = result.getNotReturnedPersonReason();
		if (notReturnedPersonError != null) {
			final String noIndividu = result.getLocalPersonId().getPersonId();
			final String category = result.getLocalPersonId().getPersonIdCategory();
			final Integer code = notReturnedPersonError.getCode();
			if (code.equals(NOT_FOUND_PERSON)) {
				return null; // le seul cas où la personne n'est pas retournée parce qu'elle n'existe simplement pas
			}
			else {
				throw new IndividuConnectorException("RcPers a retourné le code d'erreur " + code + " (" + notReturnedPersonError.getMessage() + ") sur l'appel à l'individu = " + category + "/" + noIndividu);
			}
		}

		return result.getPerson();
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
	public List<Individu> getIndividus(Collection<Long> nosIndividus, AttributeIndividu... parties) throws IndividuConnectorException {

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

		// on peut maintenant construire les individus
		final List<Individu> individus = new ArrayList<>(nosIndividus.size());
		for (Person person : persons) {
			final Individu individu = IndividuRCPers.get(person, true, infraService);
			individus.add(individu);
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
			throw new IndividuConnectorException(e);
		}
	}

	private ListOfPersons getPersonFromEventSafely(long evtId, boolean withHistory) {
		try {
			return client.getPersonByEvent(evtId, null, withHistory);
		}
		catch (Exception e) {
			throw new IndividuConnectorException(e);
		}
	}

	@Override
	public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
		try {
			final Event ref = client.getEvent(eventId);
			if (ref != null) {
				final Person personAfterEvent = ref.getPersonAfterEvent();
				final Individu individu = IndividuRCPers.get(personAfterEvent, false, infraService);
				final EventIdentification idtf = ref.getIdentification();
				final Long refMessageId = idtf.getReferenceMessageId();
				final RegDate dateEvt = XmlUtils.xmlcal2regdate(idtf.getEventDate());
				final TypeEvenementCivilEch type = TypeEvenementCivilEch.fromEchCode(idtf.getEventType());
				final ActionEvenementCivilEch action = ActionEvenementCivilEch.fromEchCode(idtf.getAction());
				return new IndividuApresEvenement(individu, dateEvt, type, action, refMessageId);
			}
			return null;
		}
		catch (Exception e) {
			throw new IndividuConnectorException(e);
		}
	}

	@Override
	public void ping() throws IndividuConnectorException {
		final Individu individu = getIndividu(611836); // Francis Perroset
		if (individu == null) {
			throw new IndividuConnectorException("L'individu n°611836 est introuvable");
		}
		if (individu.getNoTechnique() != 611836) {
			throw new IndividuConnectorException("Demandé l'individu n°611836, reçu l'individu n°" + individu.getNoTechnique());
		}
	}

	@Override
	public boolean isWarmable() {
		return false;
	}
}
