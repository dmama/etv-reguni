package ch.vd.uniregctb.registrefoncier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServitudeCombinationIteratorTest {

	@Test
	public void testEmptyIterator() throws Exception {
		ServitudeCombinationIterator iter = new ServitudeCombinationIterator(Collections.emptyIterator());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecUnSeulImmeuble() throws Exception {

		final AyantDroitRF felipe = newBeneficiairePP("217178178", "Felipe", "Schaudi");
		final AyantDroitRF jeanne = newBeneficiairePP("922908211", "Jeanne-Gabrielle", "De la Boétie");

		final ServitudeRF servitude0 = newServitude("2348923892389",
		                                                      Collections.singletonList(felipe),
		                                                      Collections.singletonList("8888888"));
		final ServitudeRF servitude1 = newServitude("4873838",
		                                                      Collections.singletonList(jeanne),
		                                                      Collections.singletonList("33333333"));
		final List<ServitudeRF> servitudes = Arrays.asList(servitude0, servitude1);

		final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", jeanne);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursImmeubles() throws Exception {

		final AyantDroitRF felipe = newBeneficiairePP("217178178", "Felipe", "Schaudi");
		final AyantDroitRF jeanne = newBeneficiairePP("922908211", "Jeanne-Gabrielle", "De la Boétie");

		final ServitudeRF servitude0 = newServitude("2348923892389",
		                                                      Collections.singletonList(felipe),
		                                                      Arrays.asList("8888888", "7777777", "00000111"));
		final ServitudeRF servitude1 = newServitude("4873838",
		                                                      Collections.singletonList(jeanne),
		                                                      Arrays.asList("33333333", "99999"));
		final List<ServitudeRF> servitudes = Arrays.asList(servitude0, servitude1);

		final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", jeanne);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", jeanne);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursBeneficiaires() throws Exception {

		final AyantDroitRF felipe = newBeneficiairePP("217178178", "Felipe", "Schaudi");
		final AyantDroitRF jeanne = newBeneficiairePP("922908211", "Jeanne-Gabrielle", "De la Boétie");
		final AyantDroitRF marcel = newBeneficiairePP("387282928", "Marcel", "Fluuu");
		final AyantDroitRF robert = newBeneficiairePP("271800299", "Robert", "O'Connor");
		final AyantDroitRF filippo = newBeneficiairePP("553622627", "Filippo", "Arcivivi");

		final List<AyantDroitRF> communaute1 = Arrays.asList(felipe, jeanne);
		final List<AyantDroitRF> communaute2 = Arrays.asList(marcel, robert, filippo);

		final ServitudeRF servitude0 = newServitude("2348923892389",
		                                                      communaute1,
		                                                      Collections.singletonList("8888888"));
		final ServitudeRF servitude1 = newServitude("4873838",
		                                                      communaute2,
		                                                      Collections.singletonList("33333333"));
		final List<ServitudeRF> servitudes = Arrays.asList(servitude0, servitude1);

		final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", jeanne);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", robert);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", marcel);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", filippo);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursImmeublesEtBeneficiaires() throws Exception {

		final AyantDroitRF felipe = newBeneficiairePP("217178178", "Felipe", "Schaudi");
		final AyantDroitRF jeanne = newBeneficiairePP("922908211", "Jeanne-Gabrielle", "De la Boétie");
		final AyantDroitRF marcel = newBeneficiairePP("387282928", "Marcel", "Fluuu");
		final AyantDroitRF robert = newBeneficiairePP("271800299", "Robert", "O'Connor");
		final AyantDroitRF filippo = newBeneficiairePP("553622627", "Filippo", "Arcivivi");

		final List<AyantDroitRF> communaute1 = Arrays.asList(felipe, jeanne);
		final List<AyantDroitRF> communaute2 = Arrays.asList(marcel, robert, filippo);

		final ServitudeRF servitude0 = newServitude("2348923892389",
		                                                      communaute1,
		                                                      Arrays.asList("8888888", "7777777", "00000111"));
		final ServitudeRF servitude1 = newServitude("4873838",
		                                                      communaute2,
		                                                      Arrays.asList("33333333", "99999"));
		final List<ServitudeRF> servitudes = Arrays.asList(servitude0, servitude1);

		final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", jeanne);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", jeanne);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", jeanne);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", robert);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", robert);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", marcel);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", marcel);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", filippo);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", filippo);
		assertFalse(iter.hasNext());
	}

	/**
	 * [SIFISC-23744] Ce test s'assure que l'itérateur ne crashe pas lorsqu'il rencontre une servitude sans bénéficiaire et que la servitude est simplement ignorée.
	 */
	@Test
	public void testServitudeSansBeneficiaire() throws Exception {

		// une seule servitude sans bénéficiaire
		{
			final ServitudeRF servitude = newServitude("2348923892389",
			                                                     Collections.emptyList(),
			                                                     Collections.singletonList("8888888"));

			final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(Collections.singletonList(servitude).iterator());
			assertFalse(iter.hasNext()); // il y a bien une servitude, mais elle est vide et ignorée

			final List<ServitudeRF> emptyServitudes = iter.getEmptyServitudes();
			assertEquals(1, emptyServitudes.size());
			assertEquals("2348923892389", emptyServitudes.get(0).getMasterIdRF());
		}

		// une servitude sans bénéficiaire suivi d'une servitude avec bénéficiaire
		{
			final AyantDroitRF marcel = newBeneficiairePP("387282928", "Marcel", "Fluuu");

			// pas de bénéficiaire
			final ServitudeRF servitude0 = newServitude("2348923892389",
			                                                      Collections.emptyList(),
			                                                      Arrays.asList("8888888", "7777777", "00000111"));
			// un bénéficiaire
			final ServitudeRF servitude1 = newServitude("4873838",
			                                                      Collections.singletonList(marcel),
			                                                      Arrays.asList("33333333", "99999"));
			final List<ServitudeRF> servitudes = Arrays.asList(servitude0, servitude1);

			final ServitudeCombinationIterator iter = new ServitudeCombinationIterator(servitudes.iterator());
			assertTrue(iter.hasNext());
			assertServitude(iter.next(), "4873838", "33333333", marcel);
			assertTrue(iter.hasNext());
			assertServitude(iter.next(), "4873838", "99999", marcel);
			assertFalse(iter.hasNext());


			final List<ServitudeRF> emptyServitudes = iter.getEmptyServitudes();
			assertEquals(1, emptyServitudes.size());
			assertEquals("2348923892389", emptyServitudes.get(0).getMasterIdRF());
		}
	}

	/**
	 * Asserte que la servitude ne possède qu'un seul immeuble avec les valeurs spécifiées.
	 */
	private static void assertServitude(ServitudeRF servitude, String servitudeIdRef, String immeublesIdRef, AyantDroitRF beneficiaire) {

		assertEquals(servitudeIdRef, servitude.getMasterIdRF());

		final Set<ImmeubleRF> immeubles = servitude.getImmeubles();
		assertEquals(1, immeubles.size());
		final ImmeubleRF immeuble = immeubles.iterator().next();
		assertNotNull(immeuble);
		assertEquals(immeublesIdRef, immeuble.getIdRF());

		final Set<AyantDroitRF> ayantDroits = servitude.getAyantDroits();
		assertEquals(1, ayantDroits.size());
		final AyantDroitRF ayantDroit = ayantDroits.iterator().next();
		assertNotNull(ayantDroit);
		assertBeneficiaire(beneficiaire, ayantDroit);
	}

	private static void assertBeneficiaire(AyantDroitRF expected, AyantDroitRF actual) {
		if (expected instanceof PersonnePhysiqueRF) {
			final PersonnePhysiqueRF expectedPP =(PersonnePhysiqueRF) expected;
			assertBeneficiairePP(expectedPP.getPrenom(), expectedPP.getNom(), actual);
		}
		else if (expected instanceof PersonneMoraleRF) {
			final PersonneMoraleRF expectedPM = (PersonneMoraleRF) expected;
			assertBeneficiairePM(expectedPM.getRaisonSociale(), expectedPM.getNumeroRC(), actual);
		}
		else {
			throw new IllegalArgumentException("Type de bénéficiaire inconnu = [" + expected.getClass().getSimpleName()+					                                   "]");
		}
	}

	private static void assertBeneficiairePM(String raisonSociale, String noRC, AyantDroitRF person) {
		assertNotNull(person);
		assertTrue(person instanceof PersonneMoraleRF);
		final PersonneMoraleRF pm = (PersonneMoraleRF) person;
		assertNotNull(pm);
		assertEquals(raisonSociale, pm.getRaisonSociale());
		assertEquals(noRC, pm.getNumeroRC());
	}

	private static void assertBeneficiairePP(String prenom, String nom, AyantDroitRF person) {
		assertNotNull(person);
		assertTrue(person instanceof PersonnePhysiqueRF);
		final PersonnePhysiqueRF pp = (PersonnePhysiqueRF) person;
		assertNotNull(pp);
		assertEquals(prenom, pp.getPrenom());
		assertEquals(nom, pp.getNom());
	}

	@NotNull
	private static ServitudeRF newServitude(String servitudeIdRef, List<AyantDroitRF> beneficiaires, List<String> immeublesIdRefs) {

		final ServitudeRF servitude = new UsufruitRF();
		servitude.setMasterIdRF(servitudeIdRef);
		servitude.setImmeubles(new HashSet<>());
		servitude.setAyantDroits(new HashSet<>());
		immeublesIdRefs.forEach(idRF -> servitude.addImmeuble(newImmeuble(idRF)));
		beneficiaires.forEach(servitude::addAyantDroit);
		return servitude;
	}

	@NotNull
	private static ImmeubleRF newImmeuble(String grundstueckIDREF) {
		final ImmeubleRF grundstueck = new BienFondRF();
		grundstueck.setIdRF(grundstueckIDREF);
		return grundstueck;
	}

	private static AyantDroitRF newBeneficiairePP(String idRF, String prenom, String nom) {
		final PersonnePhysiqueRF person = new PersonnePhysiqueRF();
		person.setIdRF(idRF);
		person.setPrenom(prenom);
		person.setNom(nom);
		return person;
	}
}