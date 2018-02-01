package ch.vd.uniregctb.tiers.view;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;

public class CommuneViewTest {
	@Test
	public void testGetNomEtValidite() throws Exception {
		assertEquals("Bled", new CommuneView(1, "Bled", null, null).getNomEtValidite());
		assertEquals("Bled (valide depuis le 01.01.1990)", new CommuneView(1, "Bled", RegDate.get(1990, 1, 1), null).getNomEtValidite());
		assertEquals("Bled (valide depuis le 01.01.1990 jusqu'au 31.12.2000)", new CommuneView(1, "Bled", RegDate.get(1990, 1, 1), RegDate.get(2000, 12, 31)).getNomEtValidite());
		assertEquals("Bled (valide jusqu'au 31.12.2000)", new CommuneView(1, "Bled", null, RegDate.get(2000, 12, 31)).getNomEtValidite());
	}
}