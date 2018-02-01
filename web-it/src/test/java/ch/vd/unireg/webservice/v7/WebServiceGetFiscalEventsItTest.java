package ch.vd.unireg.webservice.v7;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.xml.XmlUtils;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvent;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.xml.event.fiscal.v3.CategorieTiers;
import ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureFor;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;

public class WebServiceGetFiscalEventsItTest extends AbstractWebServiceItTest {

	private static final String DB_UNIT_DATA_FILE = "WebServiceGetFiscalEventsItTest.xml";

	private static boolean alreadySetUp = false;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		if (!alreadySetUp) {
			loadDatabase(DB_UNIT_DATA_FILE);
			alreadySetUp = true;
		}
	}

	private static Pair<String, Map<String, ?>> buildUriAndParams(int partyNo) {
		final Map<String, Object> map = new HashMap<>();
		map.put("partyNo", partyNo);
		return Pair.<String, Map<String, ?>>of("/fiscalEvents/{partyNo}?user=zaizzt/22", map);
	}

	@Test
	public void testWithEvents() throws Exception {
		final int noTiers = 86116202;

		{
			final Pair<String, Map<String, ?>> params = buildUriAndParams(noTiers);
			final ResponseEntity<FiscalEvents> resp = get(FiscalEvents.class, MediaType.APPLICATION_XML, params.getLeft(), params.getRight());
			Assert.assertNotNull(resp);
			Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

			final FiscalEvents container = resp.getBody();
			Assert.assertNotNull(container);
			Assert.assertNotNull(container.getEvents());
			Assert.assertEquals(2, container.getEvents().size());
			{
				final FiscalEvent evt = container.getEvents().get(0);
				Assert.assertNotNull(evt);
				Assert.assertEquals("tiers-basic", evt.getUser());
				Assert.assertEquals(RegDate.get(2008, 1, 1), XmlUtils.cal2regdate(evt.getTreatmentDate()));
				Assert.assertEquals("Ouverture d'un for principal pour motif 'Mariage / enregistrement partenariat / réconciliation'", evt.getDescription());

				final EvenementFiscal xml = evt.getEvent();
				Assert.assertNotNull(xml);
				Assert.assertEquals(OuvertureFor.class, xml.getClass());
				Assert.assertEquals(1, xml.getDate().getDay());
				Assert.assertEquals(2, xml.getDate().getMonth());
				Assert.assertEquals(1987, xml.getDate().getYear());
				Assert.assertEquals(CategorieTiers.PP, xml.getCategorieTiers());
				Assert.assertEquals(noTiers, xml.getNumeroTiers());

				final OuvertureFor ouverture = (OuvertureFor) xml;
				Assert.assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, ouverture.getMotifOuverture());
			}
			{
				final FiscalEvent evt = container.getEvents().get(1);
				Assert.assertNotNull(evt);
				Assert.assertEquals("tiers-basic", evt.getUser());
				Assert.assertEquals(RegDate.get(2008, 1, 1), XmlUtils.cal2regdate(evt.getTreatmentDate()));
				Assert.assertEquals("Ouverture d'un for secondaire pour motif 'Achat immobilier'", evt.getDescription());

				final EvenementFiscal xml = evt.getEvent();
				Assert.assertNotNull(xml);
				Assert.assertEquals(OuvertureFor.class, xml.getClass());
				Assert.assertEquals(1, xml.getDate().getDay());
				Assert.assertEquals(3, xml.getDate().getMonth());
				Assert.assertEquals(2006, xml.getDate().getYear());
				Assert.assertEquals(CategorieTiers.PP, xml.getCategorieTiers());
				Assert.assertEquals(noTiers, xml.getNumeroTiers());

				final OuvertureFor ouverture = (OuvertureFor) xml;
				Assert.assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, ouverture.getMotifOuverture());
			}
		}
	}
}
