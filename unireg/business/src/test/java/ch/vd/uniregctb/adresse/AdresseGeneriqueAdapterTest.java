package ch.vd.uniregctb.adresse;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdresseGeneriqueAdapterTest {

	@Test
	public void testConstructors() {

		final RegDate debut = RegDate.get(2000, 1, 1);
		final RegDate fin = RegDate.get(2005, 1, 1);

		AdresseGenerique adresse = new MockAdresseGeneric(debut, fin, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));

		// Constructeur n°1
		AdresseGeneriqueAdapter adapter1 = new AdresseGeneriqueAdapter(adresse, new AdresseGenerique.Source(AdresseGenerique.SourceType.FISCALE, null), true);
		assertEquals(debut, adapter1.getDateDebut());
		assertEquals(fin, adapter1.getDateFin());
		assertEquals(AdresseGenerique.SourceType.FISCALE, adapter1.getSource().getType());
		assertTrue(adapter1.isDefault());

		// Constructeur n°2
		AdresseGeneriqueAdapter adapter2 = new AdresseGeneriqueAdapter(adresse, RegDate.get(2002, 1, 1), RegDate.get(2003, 1,
				1), false);
		assertEquals(RegDate.get(2002, 1, 1), adapter2.getDateDebut());
		assertEquals(RegDate.get(2003, 1, 1), adapter2.getDateFin());
		assertEquals(AdresseGenerique.SourceType.CIVILE, adapter2.getSource().getType());
		assertFalse(adapter2.isDefault());

		// Constructeur n°3
		AdresseGeneriqueAdapter adapter3 = new AdresseGeneriqueAdapter(adresse, RegDate.get(2002, 1, 1), RegDate.get(2003, 1,
				1), new AdresseGenerique.Source(AdresseGenerique.SourceType.FISCALE, null), null);
		assertEquals(RegDate.get(2002, 1, 1), adapter3.getDateDebut());
		assertEquals(RegDate.get(2003, 1, 1), adapter3.getDateFin());
		assertEquals(AdresseGenerique.SourceType.FISCALE, adapter3.getSource().getType());
		assertFalse(adapter3.isDefault());
	}

	@Test
	public void testConstructorParams() {

		final RegDate _1995_01_01 = RegDate.get(1995, 1, 1);
		final RegDate _2000_01_01 = RegDate.get(2000, 1, 1);
		final RegDate _2005_01_01 = RegDate.get(2005, 1, 1);
		final RegDate _2010_01_01 = RegDate.get(2010, 1, 1);

		AdresseGenerique adresse_00_05 = new MockAdresseGeneric(_2000_01_01, _2005_01_01, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));
		AdresseGenerique adresse_05_00 = new MockAdresseGeneric(_2005_01_01, _2000_01_01, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));
		AdresseGenerique adresse_null_05 = new MockAdresseGeneric(null, _2005_01_01, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));
		AdresseGenerique adresse_00_null = new MockAdresseGeneric(_2000_01_01, null, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));

		// ok
		new AdresseGeneriqueAdapter(adresse_00_05, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), true);
		new AdresseGeneriqueAdapter(adresse_05_00, null, _2010_01_01, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), true);
		new AdresseGeneriqueAdapter(adresse_null_05, _2000_01_01, null, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), true);
		new AdresseGeneriqueAdapter(adresse_00_null, null, _2010_01_01, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), true);

		// ko
		assertAdresseKo(adresse_05_00, null, null);
		assertAdresseKo(adresse_00_05, _2010_01_01, null);
		assertAdresseKo(adresse_00_05, null, _1995_01_01);
	}

	private void assertAdresseKo(AdresseGenerique adresse, RegDate debut, RegDate fin) {
		try {
			new AdresseGeneriqueAdapter(adresse, debut, fin, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), true);
			fail();
		}
		catch (IllegalArgumentException ignored) {
			// ok
		}
	}

	@Test
	public void testOptimize() {

		AdresseGenerique adresse = new MockAdresseGeneric(RegDate.get(2000, 1, 1), RegDate.get(2005, 1, 1), new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));
		AdresseGeneriqueAdapter embedded = new AdresseGeneriqueAdapter(adresse, RegDate.get(1998, 1, 1), RegDate.get(2015,
				12, 31), new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null), false);

		// Toutes les valeurs surchargées
		AdresseGeneriqueAdapter adapter1 = new AdresseGeneriqueAdapter(embedded, RegDate.get(2002, 1, 1), RegDate.get(2003,
				1, 1), new AdresseGenerique.Source(AdresseGenerique.SourceType.FISCALE, null), true);
		assertEquals(RegDate.get(2002, 1, 1), adapter1.getDateDebut());
		assertEquals(RegDate.get(2003, 1, 1), adapter1.getDateFin());
		assertEquals(AdresseGenerique.SourceType.FISCALE, adapter1.getSource().getType());
		assertTrue(adapter1.isDefault());
		assertSame(adresse, adapter1.getTarget());

		// Aucune valeurs surchargée
		AdresseGeneriqueAdapter adapter2 = new AdresseGeneriqueAdapter(embedded, null, null);
		assertEquals(RegDate.get(1998, 1, 1), adapter2.getDateDebut());
		assertEquals(RegDate.get(2015, 12, 31), adapter2.getDateFin());
		assertEquals(AdresseGenerique.SourceType.CIVILE, adapter2.getSource().getType());
		assertFalse(adapter2.isDefault());
		assertSame(adresse, adapter2.getTarget());
	}

	/**
	 * Stupide test pour que Cobertura ne râle pas sur des getters non testés (qui peut bien vouloir tester des getters ?)
	 */
	@Test
	public void testGetters() {
		AdresseGenerique adresse = new MockAdresseGeneric(null, null, new AdresseGenerique.Source(AdresseGenerique.SourceType.CIVILE, null));
		AdresseGeneriqueAdapter adapter = new AdresseGeneriqueAdapter(adresse, null, null);

		assertNull(adapter.getNumeroAppartement());
		assertNull(adapter.getNumeroRue());
		assertEquals(0, adapter.getNumeroOrdrePostal());
		assertNull(adapter.getNumeroPostalComplementaire());
	}
}
