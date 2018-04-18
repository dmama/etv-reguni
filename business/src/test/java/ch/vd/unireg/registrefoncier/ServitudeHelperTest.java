package ch.vd.unireg.registrefoncier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static ch.vd.unireg.common.AbstractSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServitudeHelperTest {

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

		final List<ServitudeRF> list0 = ServitudeHelper.combinate(servitude0, null, null);
		assertEquals(1, list0.size());
		assertServitude(list0.get(0), "2348923892389", "8888888", felipe);

		final List<ServitudeRF> list1 = ServitudeHelper.combinate(servitude1, null, null);
		assertEquals(1, list1.size());
		assertServitude(list1.get(0), "4873838", "33333333", jeanne);
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

		final List<ServitudeRF> list0 = ServitudeHelper.combinate(servitude0, null, null);
		assertEquals(3, list0.size());
		assertServitude(list0.get(0), "2348923892389", "00000111", felipe);
		assertServitude(list0.get(1), "2348923892389", "7777777", felipe);
		assertServitude(list0.get(2), "2348923892389", "8888888", felipe);

		final List<ServitudeRF> list1 = ServitudeHelper.combinate(servitude1, null, null);
		assertEquals(2, list1.size());
		assertServitude(list1.get(0), "4873838", "33333333", jeanne);
		assertServitude(list1.get(1), "4873838", "99999", jeanne);
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

		final List<ServitudeRF> list0 = ServitudeHelper.combinate(servitude0, null, null);
		assertEquals(2, list0.size());
		assertServitude(list0.get(0), "2348923892389", "8888888", felipe);
		assertServitude(list0.get(1), "2348923892389", "8888888", jeanne);

		final List<ServitudeRF> list1 = ServitudeHelper.combinate(servitude1, null, null);
		assertEquals(3, list1.size());
		assertServitude(list1.get(0), "4873838", "33333333", robert);
		assertServitude(list1.get(1), "4873838", "33333333", marcel);
		assertServitude(list1.get(2), "4873838", "33333333", filippo);
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

		final List<ServitudeRF> list0 = ServitudeHelper.combinate(servitude0, null, null);
		assertEquals(6, list0.size());
		assertServitude(list0.get(0), "2348923892389", "00000111", felipe);
		assertServitude(list0.get(1), "2348923892389", "00000111", jeanne);
		assertServitude(list0.get(2), "2348923892389", "7777777", felipe);
		assertServitude(list0.get(3), "2348923892389", "7777777", jeanne);
		assertServitude(list0.get(4), "2348923892389", "8888888", felipe);
		assertServitude(list0.get(5), "2348923892389", "8888888", jeanne);

		final List<ServitudeRF> list1 = ServitudeHelper.combinate(servitude1, null, null);
		assertEquals(6, list1.size());
		assertServitude(list1.get(0), "4873838", "33333333", robert);
		assertServitude(list1.get(1), "4873838", "33333333", marcel);
		assertServitude(list1.get(2), "4873838", "33333333", filippo);
		assertServitude(list1.get(3), "4873838", "99999", robert);
		assertServitude(list1.get(4), "4873838", "99999", marcel);
		assertServitude(list1.get(5), "4873838", "99999", filippo);
	}

	/**
	 * [SIFISC-23744] Ce test s'assure que l'itérateur ne crashe pas lorsqu'il rencontre une servitude sans bénéficiaire et que la servitude est simplement ignorée.
	 */
	@Test
	public void testServitudeSansBeneficiaire() throws Exception {

		// une seule servitude sans bénéficiaire
		final ServitudeRF servitude = newServitude("2348923892389",
		                                                     Collections.emptyList(),
		                                                     Collections.singletonList("8888888"));
		assertEmpty(ServitudeHelper.combinate(servitude, null, null)); // il y a bien une servitude, mais elle est vide et ignorée
	}

	/**
	 * Asserte que la servitude ne possède qu'un seul immeuble avec les valeurs spécifiées.
	 */
	private static void assertServitude(ServitudeRF servitude, String servitudeIdRef, String immeublesIdRef, AyantDroitRF beneficiaire) {

		assertEquals(servitudeIdRef, servitude.getMasterIdRF());

		final Set<ChargeServitudeRF> charges = servitude.getCharges();
		assertEquals(1, charges.size());
		final ImmeubleRF immeuble = charges.iterator().next().getImmeuble();
		assertNotNull(immeuble);
		assertEquals(immeublesIdRef, immeuble.getIdRF());

		final Set<BeneficeServitudeRF> benefices = servitude.getBenefices();
		assertEquals(1, benefices.size());
		final AyantDroitRF ayantDroit = benefices.iterator().next().getAyantDroit();
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
			throw new IllegalArgumentException("Type de bénéficiaire inconnu = [" + expected.getClass().getSimpleName() + "]");
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
		servitude.setCharges(new HashSet<>());
		servitude.setBenefices(new HashSet<>());
		immeublesIdRefs.forEach(idRF -> {
			final ImmeubleRF immeuble = newImmeuble(idRF);
			servitude.addCharge(new ChargeServitudeRF(null, null, servitude, immeuble));
		});
		beneficiaires.forEach(bene -> servitude.addBenefice(new BeneficeServitudeRF(null, null, servitude, bene)));
		return servitude;
	}

	@NotNull
	private static ImmeubleRF newImmeuble(String grundstueckIDREF) {
		final ImmeubleRF grundstueck = new BienFondsRF();
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