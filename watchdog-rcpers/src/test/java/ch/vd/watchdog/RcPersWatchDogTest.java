package ch.vd.watchdog;

import java.util.Arrays;
import java.util.List;

import ch.ech.ech0044.v2.NamedPersonId;
import org.junit.Before;
import org.junit.Test;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0006.v1.Event;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Teste que les différents déploiements de RcPers dans les différents environnements sont accessibles.
 */
public abstract class RcPersWatchDogTest {

	protected RcPersClient client;

	@Before
	public void setUp() throws Exception {
		client = buildClient();
	}

	protected abstract RcPersClientImpl buildClient() throws Exception;

	@Test(timeout = 10000)
	public void testGetOnePerson() throws Exception {
		final RcPersClientImpl client = buildClient();

		final ListOfPersons list = client.getPersons(Arrays.asList(476228L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final List<ListOfPersons.ListOfResults.Result> result = list.getListOfResults().getResult();
		final Person person = result.get(0).getPerson();
		assertNotNull("La personne n°476228 est introuvable !", person);

		// vue des communes vaudoises
		final String officialName = person.getIdentity().getPersonIdentification().getOfficialName();
		assertEquals("MAIA", officialName.toUpperCase()); // la casse du nom change très souvent lors des reprises de données
		assertEquals("Antonio", person.getIdentity().getCallName());
	}

	@Test(timeout = 10000)
	public void testGetUPIPerson() throws Exception {
		final RcPersClientImpl client = buildClient();

		final ListOfPersons list = client.getPersons(Arrays.asList(476228L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final List<ListOfPersons.ListOfResults.Result> result = list.getListOfResults().getResult();
		final Person person = result.get(0).getPerson();
		assertNotNull("La personne n°476228 est introuvable !", person);

		// vue de la confédération
		assertNotNull(person.getUpiPerson());
		assertEquals("Maia", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getOfficialName());
		assertEquals("António", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getFirstNames());
	}

	@Test(timeout = 10000)
	public void testGetRelations() throws Exception {

		final RcPersClientImpl client = buildClient();

		final ListOfRelations list = client.getRelations(Arrays.asList(476228L), null, true);
		assertNotNull(list);

		final List<ListOfRelations.ListOfResults.Result> allRelations = list.getListOfResults().getResult();
		assertNotNull(allRelations);
		assertEquals(1, allRelations.size()); // on n'a demandé qu'une seule personne

		final Relations relations = allRelations.get(0).getRelation();
		assertNotNull("Les relations de la personne n°476228 sont introuvables !", relations);
		assertEquals("476228", relations.getLocalPersonId().getPersonId()); // c'est bien les relations de la personne demandée

		final List<Relationship> historique = relations.getRelationshipHistory();
		assertNotNull(historique);
		assertEquals(2, historique.size());

		// le femme
		final Relationship histo0 = historique.get(0);
		assertNotNull(histo0);
		assertEquals("1", histo0.getTypeOfRelationship());
		assertEquals("476229", histo0.getLocalPersonId().getPersonId());

		// la fille
		final Relationship histo1 = historique.get(1);
		assertNotNull(histo1);
		assertEquals("102", histo1.getTypeOfRelationship());
		assertEquals("476232", histo1.getLocalPersonId().getPersonId());
	}

	@Test(timeout = 10000)
	public void testGetEvent() throws Exception {
		final RcPersClientImpl client = buildClient();

		final Event event = client.getEvent(1787458431L);
		assertNotNull(event);

		final Person p = event.getPersonAfterEvent().getPerson();
		assertNotNull("La personne associée à l'événement n°1787458431 est nulle !", p);
		assertEquals(192690L, getNoIndividu(p));
	}

	private static long getNoIndividu(Person person) {
		return getNoIndividu(person.getIdentity().getPersonIdentification().getLocalPersonId());
	}

	private static long getNoIndividu(NamedPersonId personId) {
		return Long.parseLong(personId.getPersonId());
	}
}
