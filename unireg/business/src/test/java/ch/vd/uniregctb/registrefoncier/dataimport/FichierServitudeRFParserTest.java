package ch.vd.uniregctb.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.rechteregister.BelastetesGrundstueck;
import ch.vd.capitastra.rechteregister.BerechtigtePerson;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.GeburtsDatum;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.NatuerlichePersonGb;
import ch.vd.uniregctb.common.UniregJUnit4Runner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(UniregJUnit4Runner.class)
public class FichierServitudeRFParserTest {

	private FichierServitudeRFParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new FichierServitudeRFParser();
		parser.setXmlHelperRF(new XmlHelperRFImpl());
	}

	@Test
	public void testParseServitudes() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_servitudes_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les servitudes ont bien été parsées
		final List<Dienstbarkeit> servitudes = callback.getServitudes();
		assertEquals(2, servitudes.size());

		final Dienstbarkeit servitude0 = servitudes.get(0);
		{
			assertNotNull(servitude0);
			assertEquals("_1f109152380ffd8901380ffec2506c02", servitude0.getStandardRechtID());

			final List<String> immeubles = servitude0.getBeteiligtesGrundstueckIDREF();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe0d893e41", immeubles.get(0));

			assertEquals("Droit d'habitation", servitude0.getStichwort().getTextFr());
		}

		final Dienstbarkeit servitude1 = servitudes.get(1);
		{
			assertNotNull(servitude1);
			assertEquals("_1f109152380ffd8901380ffefad54360", servitude1.getStandardRechtID());

			final List<String> immeubles = servitude1.getBeteiligtesGrundstueckIDREF();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe090827e1", immeubles.get(0));

			assertEquals("Usufruit", servitude1.getStichwort().getTextFr());
		}
	}

	@Test
	public void testParseBeneficiaireDeServitudes() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/registrefoncier/export_beneficiaires_servitudes_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les bénéficiaires de servitude ont bien été parsés
		final List<LastRechtGruppe> beneficiaires = callback.getBeneficiaires();
		assertEquals(2, beneficiaires.size());

		final LastRechtGruppe beneficiaire0 = beneficiaires.get(0);
		{
			assertNotNull(beneficiaire0);
			assertEquals("_1f109152380ffd8901380fff10ca631e", beneficiaire0.getStandardRechtIDREF());

			final List<BelastetesGrundstueck> immeubles = beneficiaire0.getBelastetesGrundstueck();
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe15bd72c0", immeubles.get(0).getBelastetesGrundstueckIDREF());

			final List<BerechtigtePerson> personnes = beneficiaire0.getBerechtigtePerson();
			assertEquals(1, personnes.size());
			assertPersonnePhysique("Pierre", "Meylan", 1934, 11, 4, "_1f109152381091220138109237ca2768", personnes.get(0));
		}

		final LastRechtGruppe beneficiaire1 = beneficiaires.get(1);
		{
			assertNotNull(beneficiaire1);
			assertEquals("_1f109152380ffd8901380fff1f476d0d", beneficiaire1.getStandardRechtIDREF());

			final List<BelastetesGrundstueck> immeubles = beneficiaire1.getBelastetesGrundstueck();
			assertEquals(1, immeubles.size());
			assertEquals("_1f109152380ffd8901380ffe025f139d", immeubles.get(0).getBelastetesGrundstueckIDREF());

			final List<BerechtigtePerson> personnes = beneficiaire1.getBerechtigtePerson();
			assertEquals(1, personnes.size());
			assertPersonnePhysique("Michèle", "Bonard", 1940, 12, 7, "_1f109152381059670138105a72ae5581", personnes.get(0));
		}
	}

	private static class TestCallback implements FichierServitudeRFParser.Callback {

		private final List<Dienstbarkeit> servitudes = new ArrayList<>();

		private final List<LastRechtGruppe> beneficiaires = new ArrayList<>();
		@Override
		public void onServitude(@NotNull Dienstbarkeit servitude) {
			servitudes.add(servitude);
		}

		@Override
		public void onBeneficiaire(@NotNull LastRechtGruppe beneficiaire) {
			beneficiaires.add(beneficiaire);
		}

		@Override
		public void done() {
		}

		public List<Dienstbarkeit> getServitudes() {
			return servitudes;
		}

		public List<LastRechtGruppe> getBeneficiaires() {
			return beneficiaires;
		}

	}

	private static void assertPersonnePhysique(String prenom, String nom, int year, int month, int day, String idRef, BerechtigtePerson personne) {
		final NatuerlichePersonGb pp = personne.getNatuerlichePersonGb();
		assertNotNull(pp);
		assertEquals(nom, pp.getName());
		assertEquals(prenom, pp.getVorname());
		assertDateNaissance(year, month, day, pp.getGeburtsdatum());
		assertEquals(idRef, pp.getPersonstammIDREF());
	}

	private static void assertDateNaissance(int year, int month, int day, GeburtsDatum dateNaissance) {
		assertNotNull(dateNaissance);
		assertEquals(Long.valueOf(year), dateNaissance.getJahr());
		assertEquals(Long.valueOf(month), dateNaissance.getMonat());
		assertEquals(Long.valueOf(day), dateNaissance.getTag());
	}
}