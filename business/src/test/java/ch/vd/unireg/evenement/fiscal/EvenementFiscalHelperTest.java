package ch.vd.uniregctb.evenement.fiscal;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

public class EvenementFiscalHelperTest extends WithoutSpringTest {

	@Test
	public void testExtractBusinessUser() throws Exception {
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("EvtCivil-24657432567"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("EvtOrganisation-158414515"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("JMS-Turlututu"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("JMS-EvtDeclaration"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("Récupération-démarrage"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("JMS-RetourDégrèvement"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("[system]"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("[cron]"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("[Batch WS]"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("FaitQuelqueChoseJob"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("EnvoiDIJob"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("AutoSynchro"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("AutoSynchroParentes"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("ReqDes-15441"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("ReqDes-UT-7481548"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("Toto-reqdes"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("ReqDesEvent"));
		Assert.assertEquals("bfjkz37v", EvenementFiscalHelper.getBusinessUser("bfjkz37v"));
		Assert.assertEquals("bfjkz37v", EvenementFiscalHelper.getBusinessUser("bfjkz37v-SuperGra"));
		Assert.assertEquals("fhkjfgaf", EvenementFiscalHelper.getBusinessUser("fhkjfgaf-recalculParentes"));
		Assert.assertEquals("hk3zus", EvenementFiscalHelper.getBusinessUser("hk3zus-recalculTaches"));
		Assert.assertEquals("4237gdf", EvenementFiscalHelper.getBusinessUser("4237gdf-auto-mvt"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("JMS-Turlututu-recalculParentes"));
		Assert.assertEquals("unireg", EvenementFiscalHelper.getBusinessUser("FaitQuelqueChoseOuPasJob-recalculTaches"));
	}
}
