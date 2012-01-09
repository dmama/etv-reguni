package ch.vd.uniregctb.webservice.rcpers;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import ch.vd.evd0001.v3.ListOfPersons;
import ch.vd.evd0001.v3.Person;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RcPersServiceTest {

	@Test
	public void testGetPeople() throws Exception {
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl("http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v3");
		client.setPeoplePath("persons/ct.vd.rcpers");
		client.setUsername("sirec01");
		client.setPassword("welc0me");
		client.afterPropertiesSet();

		final ListOfPersons list = client.getPersons(Arrays.asList(333528L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final Person person = list.getListOfResults().getPerson().get(0);
		assertNotNull(person);
		// vue de la confédération
		assertEquals("Cuendet", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getOfficialName());
		assertEquals("Jean-Eric", person.getUpiPerson().getValuesStoredUnderAhvvn().getPerson().getFirstNames());

		// vue des communes vaudoises
		assertEquals("Cuendet", person.getIdentity().getPersonIdentification().getOfficialName());
		assertEquals("Jean-Eric", person.getIdentity().getCallName());
	}

	@Ignore("Il faudrait trouver un numéro d'événement qui existe...")
	@Test
	public void testGetPersonForEvent() throws Exception {
		final RcPersClientImpl client = new RcPersClientImpl();
		client.setBaseUrl("http://rp-ws-va.etat-de-vaud.ch/registres/int-rcpers/west/ws/v3");
		client.setEventPath("event");
		client.setUsername("sirec01");
		client.setPassword("welc0me");
		client.afterPropertiesSet();

		final Person p = client.getPersonForEvent(1L);
		assertNotNull(p);
	}
}
