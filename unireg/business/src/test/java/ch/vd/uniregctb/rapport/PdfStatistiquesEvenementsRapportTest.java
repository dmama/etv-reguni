package ch.vd.uniregctb.rapport;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class PdfStatistiquesEvenementsRapportTest extends WithoutSpringTest {

	@Test
	public void testEvtCivilEchMsgTypeKeyMessageCollapse() throws Exception {
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (?)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (?, ?)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (?, ?, ?)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (12)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (12, 45, 23)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (12.12.2012)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
		Assert.assertEquals("fdsgusdfgsz (?)", new PdfStatistiquesEvenementsRapport.EvtCivilEchMsgTypeKey("fdsgusdfgsz (12, 45.25, 23)", TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON).msg);
	}

}
