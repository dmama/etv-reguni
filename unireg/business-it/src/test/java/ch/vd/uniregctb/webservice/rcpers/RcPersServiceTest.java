package ch.vd.uniregctb.webservice.rcpers;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.ListOfRelations;
import ch.vd.evd0001.v3.Person;
import ch.vd.evd0001.v3.Relations;
import ch.vd.evd0001.v3.Relationship;
import ch.vd.evd0006.v1.Event;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RcPersServiceTest {

	@Test
	public void testGetPeople() throws Exception {
		final RcPersClientImpl client = buildClient();

		final ListOfPersons list = client.getPersons(Arrays.asList(333528L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final Person person = list.getListOfResults().getPerson().get(0);
		assertNotNull(person);
		// vue de la confédération
		assertNotNull(person.getUpiPerson());
		assertEquals("Cuendet", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getOfficialName());
		assertEquals("Jean-Eric", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getFirstNames());

		// vue des communes vaudoises
		assertEquals("Cuendet", person.getIdentity().getPersonIdentification().getOfficialName());
		assertEquals("Jean-Eric", person.getIdentity().getCallName());
	}

	@Test
	public void testGetRelations() throws Exception {

		final RcPersClientImpl client = buildClient();

		final ListOfRelations list = client.getRelations(Arrays.asList(333528L), null, true);
		assertNotNull(list);

		final List<Relations> allRelations = list.getListOfResults().getRelation();
		assertNotNull(allRelations);
		assertEquals(1, allRelations.size()); // on n'a demandé qu'une seule personne

		final Relations relations = allRelations.get(0);
		assertNotNull(relations);
		assertEquals("333528", relations.getPersonId().getPersonId()); // c'est bien les relations de la personne demandée

		final List<Relationship> historique = relations.getRelationshipHistory();
		assertNotNull(historique);
		assertEquals(3, historique.size());

		// le femme
		final Relationship histo0 = historique.get(0);
		assertNotNull(histo0);
		assertEquals("1", histo0.getTypeOfRelationship());
		assertEquals("333529", histo0.getLocalPersonId().getPersonId());

		// le fils
		final Relationship histo1 = historique.get(1);
		assertNotNull(histo1);
		assertEquals("102", histo1.getTypeOfRelationship());
		assertEquals("333527", histo1.getLocalPersonId().getPersonId());

		// la fille
		final Relationship histo2 = historique.get(2);
		assertNotNull(histo2);
		assertEquals("101", histo2.getTypeOfRelationship());
		assertEquals("946039", histo2.getLocalPersonId().getPersonId());
	}

	@Test
	public void testGetEvent() throws Exception {
		final RcPersClientImpl client = buildClient();

		final Event event = client.getEvent(1328541610257001L);
		assertNotNull(event);

		final Person p = event.getPersonAfterEvent();
		assertEquals(1047898L, IndividuRCPers.getNoIndividu(p));
	}

	private static RcPersClientImpl buildClient() throws Exception {
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl("http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v3");
		client.setPeoplePath("persons/ct.vd.rcpers");
		client.setEventPath("event");
		client.setRelationsPath("relations/ct.vd.rcpers");
		client.setUsername("sirec05");
		client.setPassword("welc0me");
		client.afterPropertiesSet();
		return client;
	}
}
