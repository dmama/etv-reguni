package ch.vd.unireg.data;

import org.junit.Test;

import ch.vd.unireg.xml.event.data.v1.Relationship;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertNotNull;

public class DataEventTest extends BusinessTest {

	/**
	 * Permet de s'assurer que chaque rapport entre tiers a un équivalent déclaré dans la xsd des dataEvent
	 */
	@Test
	public void testCoherenceRelationTypeWithEvent() {
		for (TypeRapportEntreTiers typeRapportEntreTier : TypeRapportEntreTiers.values()) {
			final Relationship relation = Relationship.valueOf(typeRapportEntreTier.name());
			assertNotNull(relation);
		}
	}

	@Test
	public void testRelationShipMapping() {
		for (TypeRapportEntreTiers typeRapportEntreTiers : TypeRapportEntreTiers.values()) {
			assertNotNull(ConcentratingDataEventJmsSender.getRelationshipMapping(typeRapportEntreTiers));
		}
	}
}
