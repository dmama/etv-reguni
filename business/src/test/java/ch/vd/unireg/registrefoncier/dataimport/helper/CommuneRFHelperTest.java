package ch.vd.unireg.registrefoncier.dataimport.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.unireg.registrefoncier.CommuneRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CommuneRFHelperTest {

	/**
	 * Vérifie que les comparaison de commune fonctionne sur les numéros RF
	 */
	@Test
	public void testDataEqualsCommunesAvecNumerosRF() {

		// cas simples
		final CommuneRF c1 = new CommuneRF(1, "commune 1", 1111);
		final CommuneRF c1bis = new CommuneRF(1, null, 0);          // <-- commune partiellement renseignée
		final CommuneRF c2 = new CommuneRF(2, "commune 2", 2222);
		assertTrue(CommuneRFHelper.dataEquals(c1, c1));
		assertTrue(CommuneRFHelper.dataEquals(c1, c1bis));
		assertFalse(CommuneRFHelper.dataEquals(c1, c2));

		// les numéros RF à zéro ne doivent pas être pris en compte
		final CommuneRF c3 = new CommuneRF(0, "commune 3", 3333);   // <-- numéro RF non-renseigné
		final CommuneRF c4 = new CommuneRF(0, "commune 4", 4444);   // <-- numéro RF non-renseigné
		assertFalse(CommuneRFHelper.dataEquals(c3, c4));
	}

	/**
	 * Vérifie que les comparaison de commune fonctionne sur les numéros Ofs
	 */
	@Test
	public void testDataEqualsCommunesAvecNumerosOfs() {

		// cas simples
		final CommuneRF c1 = new CommuneRF(0, "commune 1", 1111);
		final CommuneRF c1bis = new CommuneRF(0, null, 1111);          // <-- commune partiellement renseignée
		final CommuneRF c2 = new CommuneRF(0, "commune 2", 2222);
		assertTrue(CommuneRFHelper.dataEquals(c1, c1));
		assertTrue(CommuneRFHelper.dataEquals(c1, c1bis));
		assertFalse(CommuneRFHelper.dataEquals(c1, c2));

		// les numéros Ofs à zéro ne doivent pas être pris en compte (cas métier normalement impossible)
		final CommuneRF c3 = new CommuneRF(3, "commune 3", 0);   // <-- numéro Ofs non-renseigné
		final CommuneRF c4 = new CommuneRF(4, "commune 4", 0);   // <-- numéro Ofs non-renseigné
		assertFalse(CommuneRFHelper.dataEquals(c3, c4));
	}

	/**
	 * Vérifie que les comparaison de commune fonctionne sur les numéros Rf et Ofs quand ils sont mélangés
	 */
	@Test
	public void testDataEqualsCommunesAvecNumerosRfEtOfsMelanges() {

		// cas simples
		final CommuneRF c1 = new CommuneRF(1, "commune 1", 1111);
		final CommuneRF c1bis = new CommuneRF(0, null, 1111);          // <-- commune partiellement renseignée
		final CommuneRF c2 = new CommuneRF(2, "commune 2", 2222);
		final CommuneRF c2bis = new CommuneRF(2, null, 0);             // <-- commune partiellement renseignée
		assertTrue(CommuneRFHelper.dataEquals(c1, c1bis));
		assertTrue(CommuneRFHelper.dataEquals(c2, c2bis));
		assertFalse(CommuneRFHelper.dataEquals(c1, c2));
		assertFalse(CommuneRFHelper.dataEquals(c1bis, c2bis));
	}

	/**
	 * Vérifie que la création d'une nouvelle commune à partir d'un numéro RF fonctionne bien
	 */
	@Test
	public void testNewCommuneRFAvecNumeroRF() {

		final GrundstueckNummer communeImport = new GrundstueckNummer();
		communeImport.setBfsNr(1);
		communeImport.setGemeindenamen("commune 1");

		final CommuneRF commune = CommuneRFHelper.newCommuneRF(communeImport, (nom) -> 1111);
		assertNotNull(commune);
		assertEquals(1, commune.getNoRf());
		assertEquals(1111, commune.getNoOfs());
		assertEquals("commune 1", commune.getNomRf());
	}

	/**
	 * Vérifie que la création d'une nouvelle commune à partir d'un numéro Ofs fonctionne bien
	 */
	@Test
	public void testNewCommuneRFAvecNumeroOfs() {

		final GrundstueckNummer communeImport = new GrundstueckNummer();
		communeImport.setBfsNr(1111);
		communeImport.setGemeindenamen("commune 1");

		final CommuneRF commune = CommuneRFHelper.newCommuneRF(communeImport, (nom) -> 1111);
		assertNotNull(commune);
		assertEquals(0, commune.getNoRf()); // <-- 0, parce que le numéro RF n'est plus attribué sur les nouvelles communes après le basculement aux numéros Ofs
		assertEquals(1111, commune.getNoOfs());
		assertEquals("commune 1", commune.getNomRf());
	}
}