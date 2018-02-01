package ch.vd.unireg.webservices.v7;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.unireg.xml.party.v5.PartyType;
import ch.vd.unireg.tiers.TiersCriteria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PartySearchTypeTest extends EnumTest {

	@Test
	public void testCoherenceAvecPartyType() throws Exception {
		// ces constantes sont spécifiques à la recherche
		final EnumSet<PartySearchType> specificSearchTypes = EnumSet.of(PartySearchType.NON_RESIDENT_NATURAL_PERSON, PartySearchType.RESIDENT_NATURAL_PERSON);

		final Set<PartyType> partyTypes = EnumSet.allOf(PartyType.class);
		final Set<PartySearchType> commonSearchTypes = EnumSet.complementOf(specificSearchTypes);

		assertEquals(partyTypes.size(), commonSearchTypes.size());
		for (PartyType pt : partyTypes) {
			final PartySearchType pst = PartySearchType.valueOf(pt.name());
			assertNotNull(pt.name(), pst);
			assertTrue(pt.name(), commonSearchTypes.contains(pst));
		}
	}

	@Test
	public void testToCore() throws Exception {
		for (PartySearchType pst : PartySearchType.values()) {
			assertNotNull(pst.name(), EnumHelper.toCore(pst));
		}

		assertEquals(TiersCriteria.TypeTiers.AUTRE_COMMUNAUTE, EnumHelper.toCore(PartySearchType.OTHER_COMMUNITY));
		assertEquals(TiersCriteria.TypeTiers.COLLECTIVITE_ADMINISTRATIVE, EnumHelper.toCore(PartySearchType.ADMINISTRATIVE_AUTHORITY));
		assertEquals(TiersCriteria.TypeTiers.DEBITEUR_PRESTATION_IMPOSABLE, EnumHelper.toCore(PartySearchType.DEBTOR));
		assertEquals(TiersCriteria.TypeTiers.ENTREPRISE, EnumHelper.toCore(PartySearchType.CORPORATION));
		assertEquals(TiersCriteria.TypeTiers.HABITANT, EnumHelper.toCore(PartySearchType.RESIDENT_NATURAL_PERSON));
		assertEquals(TiersCriteria.TypeTiers.MENAGE_COMMUN, EnumHelper.toCore(PartySearchType.HOUSEHOLD));
		assertEquals(TiersCriteria.TypeTiers.NON_HABITANT, EnumHelper.toCore(PartySearchType.NON_RESIDENT_NATURAL_PERSON));
		assertEquals(TiersCriteria.TypeTiers.PERSONNE_PHYSIQUE, EnumHelper.toCore(PartySearchType.NATURAL_PERSON));
		assertEquals(TiersCriteria.TypeTiers.ETABLISSEMENT, EnumHelper.toCore(PartySearchType.ESTABLISHMENT));
	}
}
