package ch.vd.uniregctb.tiers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RapportEntreTiersDAOTest {

	@Test
	public void testGetRapportEntreTiersComparatorAutoriteTutelaireId() {
		final List<RapportEntreTiers> rapportEntreTiersList = new ArrayList<RapportEntreTiers>(5);

		final Comparator<RapportEntreTiers> comparateurAsc = RapportEntreTiersDAOImpl.getRapportEntreTiersComparator(132456L, "autoriteTutelaireId");

		// Alimentation de la liste de RapportEntreTiers
		RapportEntreTiers rapportEntreTiers_00 = new AppartenanceMenage();
		rapportEntreTiersList.add(rapportEntreTiers_00);
		RapportEntreTiers rapportEntreTiers_01 = new Tutelle();
		((Tutelle)rapportEntreTiers_01).setAutoriteTutelaireId(333L);
		rapportEntreTiersList.add(rapportEntreTiers_01);
		RapportEntreTiers rapportEntreTiers_02 = new ActiviteEconomique();
		rapportEntreTiersList.add(rapportEntreTiers_02);
		RapportEntreTiers rapportEntreTiers_03 = new Curatelle();
		((Curatelle) rapportEntreTiers_03).setAutoriteTutelaireId(111L);
		rapportEntreTiersList.add(rapportEntreTiers_03);
		RapportEntreTiers rapportEntreTiers_04 = new AppartenanceMenage();
		rapportEntreTiersList.add(rapportEntreTiers_04);
		RapportEntreTiers rapportEntreTiers_05 = new Curatelle();
		((Curatelle) rapportEntreTiers_05).setAutoriteTutelaireId(222L);
		rapportEntreTiersList.add(rapportEntreTiers_05);

		// Tri
		rapportEntreTiersList.sort(comparateurAsc);

		// Test sur l'ordre du tri
		assertEquals(rapportEntreTiersList.get(0), (RapportEntreTiers) rapportEntreTiers_03);
		assertEquals(rapportEntreTiersList.get(1), rapportEntreTiers_05);
		assertEquals(rapportEntreTiersList.get(2), rapportEntreTiers_01);
		assertEquals(rapportEntreTiersList.get(3), rapportEntreTiers_00);
		assertEquals(rapportEntreTiersList.get(4), rapportEntreTiers_02);
		assertEquals(rapportEntreTiersList.get(5), rapportEntreTiers_04);

	}


}
