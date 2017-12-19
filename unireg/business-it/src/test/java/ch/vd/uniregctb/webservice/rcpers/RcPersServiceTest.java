package ch.vd.uniregctb.webservice.rcpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.evd0001.v5.Event;
import ch.vd.evd0001.v5.ListOfPersons;
import ch.vd.evd0001.v5.MaritalData;
import ch.vd.evd0001.v5.Parent;
import ch.vd.evd0001.v5.Person;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.IndividuRCPers;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.wsclient.WebClientPool;
import ch.vd.unireg.wsclient.rcpers.RcPersClientImpl;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.utils.UniregProperties;
import ch.vd.uniregctb.utils.UniregPropertiesImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RcPersServiceTest {

	private UniregProperties uniregProperties;

	public RcPersServiceTest() {
		try {
			final UniregPropertiesImpl impl = new UniregPropertiesImpl();
			impl.setFilename("../base/unireg-ut.properties");
			impl.afterPropertiesSet();
			uniregProperties = impl;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test(timeout = 5000)
	public void testGetPeople() throws Exception {
		final RcPersClientImpl client = buildClient();

		final ListOfPersons list = client.getPersons(Collections.singletonList(476228L), null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final List<ListOfPersons.ListOfResults.Result> result = list.getListOfResults().getResult();
		final Person person = result.get(0).getPerson();
		assertNotNull(person);
		// vue de la confédération
		assertNotNull(person.getUpiPerson());
		assertEquals("Maia", person.getUpiPerson().getOfficialName());
		assertEquals("António", person.getUpiPerson().getFirstNames());

		// vue des communes vaudoises
		final String officialName = person.getIdentity().getOfficialName();
		assertEquals("MAIA", officialName.toUpperCase()); // on ignore la casse parce que cette valeur change souvent lors des reprises de données
		assertEquals("Antonio", person.getIdentity().getCallName());
	}

	@Test(timeout = 5000)
	public void testGetPeopleWithRelation() throws Exception {

		final RcPersClientImpl client = buildClient();
		final RegDate dateNaissance = RegDate.get(1976, 4, 15);

		final ListOfPersons list = client.getPersons(Collections.singletonList(347101L), null, true);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final List<ListOfPersons.ListOfResults.Result> result = list.getListOfResults().getResult();
		final Person person = result.get(0).getPerson();
		assertNotNull(person);
		assertEquals(dateNaissance, EchHelper.partialDateFromEch44(person.getIdentity().getDateOfBirth()));

		// les parents (deux fois les mêmes, une fois avec un numéro d'identification, une fois sans - car l'individu est parti puis revenu, et RCPers n'a pas réussi à établir avec
		// certitude que les parents étaient restés les mêmes)
		final List<Parent> parents = person.getParentHistory();
		assertNotNull(parents);
		assertEquals(4, parents.size());

		final List<Parent> parentSorted = new ArrayList<>(parents);
		Collections.sort(parentSorted, new Comparator<Parent>() {
			@Override
			public int compare(Parent o1, Parent o2) {
				// papa (1 = M) avant maman (2 = F)
				int comparison = o1.getIdentification().getIdentification().getSex().compareTo(o2.getIdentification().getIdentification().getSex());
				if (comparison == 0) {
					// 2 fois papa, ou deux fois maman... -> comparaison sur la date
					final RegDate refDate1 = o1.getParentFrom() == null ? XmlUtils.xmlcal2regdate(o1.getReportingDate()) : XmlUtils.xmlcal2regdate(o1.getParentFrom());
					final RegDate refDate2 = o2.getParentFrom() == null ? XmlUtils.xmlcal2regdate(o2.getReportingDate()) : XmlUtils.xmlcal2regdate(o2.getParentFrom());
					comparison = NullDateBehavior.EARLIEST.compare(refDate1, refDate2);
				}
				return comparison;
			}
		});
		{
			final Parent parent = parentSorted.get(0);
			assertEquals("347148", parent.getIdentification().getIdentification().getLocalPersonId().getPersonId());
			assertNull(parent.getParentFrom());
			assertEquals(RegDate.get(2015, 12, 31), XmlUtils.xmlcal2regdate(parent.getParentTill()));
		}
		{
			final Parent parent = parentSorted.get(1);
			assertNull(parent.getIdentification().getIdentification().getLocalPersonId());
			assertNull(parent.getParentFrom());
			assertNull(parent.getParentTill());
			assertEquals(RegDate.get(2016, 1, 1), XmlUtils.xmlcal2regdate(parent.getReportingDate()));
		}
		{
			final Parent parent = parentSorted.get(2);
			assertEquals("347149", parent.getIdentification().getIdentification().getLocalPersonId().getPersonId());
			assertNull(parent.getParentFrom());
			assertEquals(RegDate.get(2015, 12, 31), XmlUtils.xmlcal2regdate(parent.getParentTill()));
		}
		{
			final Parent parent = parentSorted.get(3);
			assertNull(parent.getIdentification().getIdentification().getLocalPersonId());
			assertNull(parent.getParentFrom());
			assertNull(parent.getParentTill());
			assertEquals(RegDate.get(2016, 1, 1), XmlUtils.xmlcal2regdate(parent.getReportingDate()));
		}

		// le conjoint (non-habitant)
		final List<MaritalData> msHistory = person.getMaritalStatusHistory();
		assertNotNull(msHistory);
		assertEquals(2, msHistory.size());
		{
			final MaritalData md = msHistory.get(0);
			assertEquals("1", md.getMaritalStatus());
			assertNull(md.getPartner());
		}
		{
			final MaritalData md = msHistory.get(1);
			assertEquals("2", md.getMaritalStatus());
			assertNotNull(md.getPartner());
		}
	}

	@Test(timeout = 5000)
	public void testGetEvent() throws Exception {
		final RcPersClientImpl client = buildClient();

		final Event event = client.getEvent(1812347788L);
		assertNotNull(event);

		final Person p = event.getPersonAfterEvent();
		assertNotNull("La personne associée à l'événement n°1812347788 est nulle !", p);
		assertEquals(828322L, IndividuRCPers.getNoIndividu(p));
	}

	@Test(timeout = 5000)
	public void testGetPersonByEvent() throws Exception {
		final RcPersClientImpl client = buildClient();
		final ListOfPersons list = client.getPersonByEvent(1812347788L, null, false);
		assertNotNull(list);
		assertEquals(1, list.getNumberOfResults().intValue());

		final Person p = list.getListOfResults().getResult().get(0).getPerson();
		assertNotNull("La personne associée à l'événement n°1812347788 est nulle !", p);
		assertEquals(828322L, IndividuRCPers.getNoIndividu(p));
	}

	private RcPersClientImpl buildClient() throws Exception {

		final String rcpUrl = uniregProperties.getProperty("testprop.webservice.rcpers.url");

		final WebClientPool wcPool = new WebClientPool();
		wcPool.setBaseUrl(rcpUrl);
		wcPool.setUsername("gvd0unireg");
		wcPool.setPassword("Welc0me_");

		final RcPersClientImpl client = new RcPersClientImpl();
		client.setWcPool(wcPool);
		client.setPeoplePath("persons/ct.vd.rcpers");
		client.setPeopleByEventIdPath("persons/byevent");
		client.setEventPath("event");
		return client;
	}
}
