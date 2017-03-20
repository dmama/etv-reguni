package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.DienstbarkeitDiscrete;
import ch.vd.capitastra.rechteregister.DienstbarkeitExtended;
import ch.vd.capitastra.rechteregister.JuristischePersonGb;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;

import static ch.vd.uniregctb.common.AbstractSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DienstbarkeitDiscreteIteratorTest {

	@Test
	public void testEmptyIterator() throws Exception {
		DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(Collections.emptyIterator());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecUnSeulImmeuble() throws Exception {

		final BerechtigtePerson felipe = newBeneficiairePP("Felipe", "Schaudi");
		final BerechtigtePerson jeanne = newBeneficiairePP("Jeanne-Gabrielle", "De la Boétie");

		final DienstbarkeitExtended servitude0 = newServitude("2348923892389",
		                                                      Collections.singletonList(felipe),
		                                                      Collections.singletonList("8888888"));
		final DienstbarkeitExtended servitude1 = newServitude("4873838",
		                                                      Collections.singletonList(jeanne),
		                                                      Collections.singletonList("33333333"));
		final List<DienstbarkeitExtended> servitudes = Arrays.asList(servitude0, servitude1);

		final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe, null);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", jeanne, null);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursImmeubles() throws Exception {

		final BerechtigtePerson felipe = newBeneficiairePP("Felipe", "Schaudi");
		final BerechtigtePerson jeanne = newBeneficiairePP("Jeanne-Gabrielle", "De la Boétie");

		final DienstbarkeitExtended servitude0 = newServitude("2348923892389",
		                                                      Collections.singletonList(felipe),
		                                                      Arrays.asList("8888888", "7777777", "00000111"));
		final DienstbarkeitExtended servitude1 = newServitude("4873838",
		                                                      Collections.singletonList(jeanne),
		                                                      Arrays.asList("33333333", "99999"));
		final List<DienstbarkeitExtended> servitudes = Arrays.asList(servitude0, servitude1);

		final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe, null);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", felipe, null);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", felipe, null);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", jeanne, null);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", jeanne, null);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursBeneficiaires() throws Exception {

		final BerechtigtePerson felipe = newBeneficiairePP("Felipe", "Schaudi");
		final BerechtigtePerson jeanne = newBeneficiairePP("Jeanne-Gabrielle", "De la Boétie");
		final BerechtigtePerson marcel = newBeneficiairePP("Marcel", "Fluuu");
		final BerechtigtePerson robert = newBeneficiairePP("Robert", "O'Connor");
		final BerechtigtePerson filippo = newBeneficiairePP("Filippo", "Arcivivi");

		final List<BerechtigtePerson> communaute1 = Arrays.asList(felipe, jeanne);
		final List<BerechtigtePerson> communaute2 = Arrays.asList(marcel, robert, filippo);

		final DienstbarkeitExtended servitude0 = newServitude("2348923892389",
		                                                      communaute1,
		                                                      Collections.singletonList("8888888"));
		final DienstbarkeitExtended servitude1 = newServitude("4873838",
		                                                      communaute2,
		                                                      Collections.singletonList("33333333"));
		final List<DienstbarkeitExtended> servitudes = Arrays.asList(servitude0, servitude1);

		final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", jeanne, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", marcel, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", robert, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", filippo, communaute2);
		assertFalse(iter.hasNext());
	}

	@Test
	public void testServitudesAvecPlusieursImmeublesEtBeneficiaires() throws Exception {

		final BerechtigtePerson felipe = newBeneficiairePP("Felipe", "Schaudi");
		final BerechtigtePerson jeanne = newBeneficiairePP("Jeanne-Gabrielle", "De la Boétie");
		final BerechtigtePerson marcel = newBeneficiairePP("Marcel", "Fluuu");
		final BerechtigtePerson robert = newBeneficiairePP("Robert", "O'Connor");
		final BerechtigtePerson filippo = newBeneficiairePP("Filippo", "Arcivivi");

		final List<BerechtigtePerson> communaute1 = Arrays.asList(felipe, jeanne);
		final List<BerechtigtePerson> communaute2 = Arrays.asList(marcel, robert, filippo);

		final DienstbarkeitExtended servitude0 = newServitude("2348923892389",
		                                                      communaute1,
		                                                      Arrays.asList("8888888", "7777777", "00000111"));
		final DienstbarkeitExtended servitude1 = newServitude("4873838",
		                                                      communaute2,
		                                                      Arrays.asList("33333333", "99999"));
		final List<DienstbarkeitExtended> servitudes = Arrays.asList(servitude0, servitude1);

		final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(servitudes.iterator());
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", felipe, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", felipe, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", felipe, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "8888888", jeanne, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "7777777", jeanne, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "2348923892389", "00000111", jeanne, communaute1);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", marcel, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", marcel, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", robert, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", robert, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "33333333", filippo, communaute2);
		assertTrue(iter.hasNext());
		assertServitude(iter.next(), "4873838", "99999", filippo, communaute2);
		assertFalse(iter.hasNext());
	}

	/**
	 * [SIFISC-23744] Ce test s'assure que l'itérateur ne crashe pas lorsqu'il rencontre une servitude sans bénéficiaire et que la servitude est simplement ignorée.
	 */
	@Test
	public void testServitudeSansBeneficiaire() throws Exception {

		// une seule servitude sans bénéficiaire
		{
			final DienstbarkeitExtended servitude = newServitude("2348923892389",
			                                                     Collections.emptyList(),
			                                                     Collections.singletonList("8888888"));

			final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(Collections.singletonList(servitude).iterator());
			assertFalse(iter.hasNext()); // il y a bien une servitude, mais elle est vide et ignorée
		}

		// une servitude sans bénéficiaire suivi d'une servitude avec bénéficiaire
		{
			final BerechtigtePerson marcel = newBeneficiairePP("Marcel", "Fluuu");

			// pas de bénéficiaire
			final DienstbarkeitExtended servitude0 = newServitude("2348923892389",
			                                                      Collections.emptyList(),
			                                                      Arrays.asList("8888888", "7777777", "00000111"));
			// un bénéficiaire
			final DienstbarkeitExtended servitude1 = newServitude("4873838",
			                                                      Collections.singletonList(marcel),
			                                                      Arrays.asList("33333333", "99999"));
			final List<DienstbarkeitExtended> servitudes = Arrays.asList(servitude0, servitude1);

			final DienstbarkeitDiscreteIterator iter = new DienstbarkeitDiscreteIterator(servitudes.iterator());
			assertTrue(iter.hasNext());
			assertServitude(iter.next(), "4873838", "33333333", marcel, null);
			assertTrue(iter.hasNext());
			assertServitude(iter.next(), "4873838", "99999", marcel, null);
			assertFalse(iter.hasNext());
		}
	}

	/**
	 * Asserte que la servitude ne possède qu'un seul immeuble avec les valeurs spécifiées.
	 */
	private static void assertServitude(DienstbarkeitDiscrete servitude, String servitudeIdRef, String immeublesIdRef, BerechtigtePerson beneficiaire, @Nullable List<BerechtigtePerson> communaute) {

		final Dienstbarkeit dienstbarkeit0 = servitude.getDienstbarkeit();
		assertEquals(servitudeIdRef, dienstbarkeit0.getStandardRechtID());

		final BelastetesGrundstueck grundstueck = servitude.getBelastetesGrundstueck();
		assertNotNull(grundstueck);
		assertEquals(immeublesIdRef, grundstueck.getBelastetesGrundstueckIDREF());
		assertBeneficiaire(beneficiaire, servitude.getBerechtigtePerson());

		if (communaute == null || communaute.isEmpty()) {
			assertEmpty(servitude.getGemeinschaft());
		}
		else {
			final List<BerechtigtePerson> gemeinschaft = servitude.getGemeinschaft();
			assertNotNull(gemeinschaft);
			assertEquals(communaute.size(), gemeinschaft.size());
			for (int i = 0; i < communaute.size(); i++) {
				BerechtigtePerson expected = communaute.get(i);
				BerechtigtePerson actual = gemeinschaft.get(i);
				assertBeneficiaire(expected, actual);
			}
		}
	}

	private static void assertBeneficiaire(BerechtigtePerson expected, BerechtigtePerson actual) {
		if (expected.getJuristischePersonGb() != null) {
			assertBeneficiairePM(expected.getJuristischePersonGb().getName(), expected.getJuristischePersonGb().getSitz(), actual);
		}
		else {
			assertBeneficiairePP(expected.getNatuerlichePersonGb().getVorname(), expected.getNatuerlichePersonGb().getName(), actual);
		}
	}

	private static void assertBeneficiairePM(String nom, String lieu, BerechtigtePerson person) {
		assertNotNull(person);
		assertNull(person.getNatuerlichePersonGb());
		final JuristischePersonGb pp = person.getJuristischePersonGb();
		assertNotNull(pp);
		assertEquals(nom, pp.getName());
		assertEquals(lieu, pp.getSitz());
	}

	private static void assertBeneficiairePP(String prenom, String nom, BerechtigtePerson person) {
		assertNotNull(person);
		assertNull(person.getJuristischePersonGb());
		final NatuerlichePersonGb pp = person.getNatuerlichePersonGb();
		assertNotNull(pp);
		assertEquals(prenom, pp.getVorname());
		assertEquals(nom, pp.getName());
	}

	@NotNull
	private static DienstbarkeitExtended newServitude(String servitudeIdRef, List<BerechtigtePerson> beneficiaires, List<String> immeublesIdRefs) {

		final Dienstbarkeit dienstbarkeit = new Dienstbarkeit();
		dienstbarkeit.setStandardRechtID(servitudeIdRef);

		final LastRechtGruppe gruppe = new LastRechtGruppe();
		gruppe.setStandardRechtIDREF(servitudeIdRef);

		for (BerechtigtePerson beneficiaire : beneficiaires) {
			gruppe.getBerechtigtePerson().add(beneficiaire);
		}

		for (String grundstueckIDRef : immeublesIdRefs) {
			dienstbarkeit.getBeteiligtesGrundstueckIDREF().add(grundstueckIDRef);
			gruppe.getBelastetesGrundstueck().add(newBelastetesGrundstueck(grundstueckIDRef));
		}

		return new DienstbarkeitExtended(dienstbarkeit, gruppe);
	}

	@NotNull
	private static BelastetesGrundstueck newBelastetesGrundstueck(String grundstueckIDREF) {
		final BelastetesGrundstueck grundstueck = new BelastetesGrundstueck();
		grundstueck.setBelastetesGrundstueckIDREF(grundstueckIDREF);
		return grundstueck;
	}

	@NotNull
	private static BerechtigtePerson newBeneficiairePP(String prenom, String nom) {
		final BerechtigtePerson person = new BerechtigtePerson();
		person.setNatuerlichePersonGb(newNatuerlichePerson(prenom, nom));
		return person;
	}

	@NotNull
	private static NatuerlichePersonGb newNatuerlichePerson(String prenom, String nom) {
		final NatuerlichePersonGb pp = new NatuerlichePersonGb();
		pp.setVorname(prenom);
		pp.setName(nom);
		return pp;
	}
}