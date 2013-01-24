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
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.utils.UniregProperties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RcPersServiceTest {

	private UniregProperties uniregProperties;

	public RcPersServiceTest() {
		try {
			uniregProperties = new UniregProperties();
			uniregProperties.setFilename("file:../base/unireg-ut.properties");
			uniregProperties.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test(timeout = 5000)
	public void testGetPeople() throws Exception {
		final RcPersClientImpl client = buildClient();

		final ListOfPersons list = client.getPersons(Arrays.asList(476228L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final List<ListOfPersons.ListOfResults.Result> result = list.getListOfResults().getResult();
		final Person person = result.get(0).getPerson();
		assertNotNull(person);
		// vue de la confédération
		assertNotNull(person.getUpiPerson());
		assertEquals("Maia", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getOfficialName());
		assertEquals("António", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getFirstNames());

		// vue des communes vaudoises
		final String officialName = person.getIdentity().getPersonIdentification().getOfficialName();
		assertEquals("MAIA", officialName.toUpperCase()); // on ignore la casse parce que cette valeur change souvent lors des reprises de données
		assertEquals("Antonio", person.getIdentity().getCallName());
	}

	@Test(timeout = 5000)
	public void testGetRelations() throws Exception {

		final RcPersClientImpl client = buildClient();

		final ListOfRelations list = client.getRelations(Arrays.asList(476228L), null, true);
		assertNotNull(list);

		final List<ListOfRelations.ListOfResults.Result> allRelations = list.getListOfResults().getResult();
		assertNotNull(allRelations);
		assertEquals(1, allRelations.size()); // on n'a demandé qu'une seule personne

		final Relations relations = allRelations.get(0).getRelation();
		assertNotNull(relations);
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

	@Test(timeout = 5000)
	public void testGetEvent() throws Exception {
		final RcPersClientImpl client = buildClient();

		final Event event = client.getEvent(1812347788L);
		assertNotNull(event);

		final Person p = event.getPersonAfterEvent().getPerson();
		assertNotNull("La personne associée à l'événement n°1812347788 est nulle !", p);
		assertEquals(828322L, IndividuRCPers.getNoIndividu(p));
	}

	private RcPersClientImpl buildClient() throws Exception {

		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.rcpers.url");

		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl(rcpUrl);
		client.setPeoplePath("persons/ct.vd.rcpers");
		client.setEventPath("event");
		client.setRelationsPath("relations/ct.vd.rcpers");
		client.setUsername("gvd0unireg");
		client.setPassword("Welc0me_");
		client.afterPropertiesSet();
		return client;
	}
}
