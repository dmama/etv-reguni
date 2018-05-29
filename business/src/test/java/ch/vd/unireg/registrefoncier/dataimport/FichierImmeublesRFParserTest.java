package ch.vd.unireg.registrefoncier.dataimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.GebaeudeArt;
import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GewoehnlichesMiteigentum;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckZuGebaeude;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.capitastra.grundstueck.SDR;
import ch.vd.capitastra.grundstueck.StockwerksEinheit;
import ch.vd.registre.base.date.RegDate;

import static ch.vd.unireg.common.WithoutSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FichierImmeublesRFParserTest {

	private FichierImmeublesRFParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new FichierImmeublesRFParser();
		parser.setXmlHelperRF(new XmlHelperRFImpl());
	}

	@Test
	public void testParseImmeubles() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_immeubles_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les immeubles ont bien été parsés
		final List<Grundstueck> immeubles = callback.getImmeubles();
		assertEquals(4, immeubles.size());

		// un immeuble de type bien-fonds
		final Liegenschaft im0 = (Liegenschaft) immeubles.get(0);
		assertEquals("_1f109152381026b501381028a73d1852", im0.getGrundstueckID());
		assertEquals("CH938391457759", im0.getEGrid());
		assertEquals("Oron", im0.getGrundstueckNummer().getGemeindenamen());    // nom de la commune
		assertEquals(5089, im0.getGrundstueckNummer().getStammNr());            // numéro de la parcelle
		assertNull(im0.getGrundstueckNummer().getIndexNr1());
		assertNull(im0.getGrundstueckNummer().getIndexNr2());
		assertNull(im0.getGrundstueckNummer().getIndexNr3());
		assertEquals(Long.valueOf(260000L), im0.getAmtlicheBewertung().getAmtlicherWert());
		assertNull(im0.getAmtlicheBewertung().getProtokollDatum());
		assertTrue(im0.getAmtlicheBewertung().isProtokollGueltig());
		assertEquals("RG93", im0.getAmtlicheBewertung().getProtokollNr());
		assertNull(im0.getLigUnterartEnum());

		// Un immeuble de type de droit distinct et permanent
		final SDR im1 = (SDR) immeubles.get(1);
		assertEquals("_8af806cc3971feb60139e36d062130f3", im1.getGrundstueckID());
		assertEquals("CH729253834531", im1.getEGrid());
		assertEquals("Oron", im1.getGrundstueckNummer().getGemeindenamen());    // nom de la commune
		assertEquals(692, im1.getGrundstueckNummer().getStammNr());            // numéro de la parcelle
		assertNull(im1.getGrundstueckNummer().getIndexNr1());
		assertNull(im1.getGrundstueckNummer().getIndexNr2());
		assertNull(im1.getGrundstueckNummer().getIndexNr3());
		assertEquals(Long.valueOf(2120000L), im1.getAmtlicheBewertung().getAmtlicherWert());
		assertEquals(RegDate.get(2016, 9, 13), im1.getAmtlicheBewertung().getProtokollDatum());
		assertTrue(im1.getAmtlicheBewertung().isProtokollGueltig());
		assertEquals("2016", im1.getAmtlicheBewertung().getProtokollNr());

		// Un immeuble de type propriété par étage PPE
		final StockwerksEinheit im2 = (StockwerksEinheit) immeubles.get(2);
		assertEquals("_8af806fc45d223e60149c23f475365d5", im2.getGrundstueckID());
		assertEquals("CH336583651349", im2.getEGrid());
		assertEquals("Boulens", im2.getGrundstueckNummer().getGemeindenamen());    // nom de la commune
		assertEquals(19, im2.getGrundstueckNummer().getStammNr());                  // numéro de la parcelle
		assertEquals(Integer.valueOf(4), im2.getGrundstueckNummer().getIndexNr1());
		assertNull(im2.getGrundstueckNummer().getIndexNr2());
		assertNull(im2.getGrundstueckNummer().getIndexNr3());
		assertEquals(Long.valueOf(495000L), im2.getAmtlicheBewertung().getAmtlicherWert());
		assertEquals(RegDate.get(2016, 9, 13), im2.getAmtlicheBewertung().getProtokollDatum());
		assertTrue(im2.getAmtlicheBewertung().isProtokollGueltig());
		assertEquals("2016", im2.getAmtlicheBewertung().getProtokollNr());
		assertEquals(Long.valueOf(293L), im2.getStammGrundstueck().getQuote().getAnteilZaehler());
		assertEquals(Long.valueOf(1000L), im2.getStammGrundstueck().getQuote().getAnteilNenner());

		// Un immeuble de type copropriété
		final GewoehnlichesMiteigentum im3 = (GewoehnlichesMiteigentum) immeubles.get(3);
		assertEquals("_8af806cc5043853201508e1e8a3a1a71", im3.getGrundstueckID());
		assertEquals("CH516579658411", im3.getEGrid());
		assertEquals("Corcelles-près-Payerne", im3.getGrundstueckNummer().getGemeindenamen());    // nom de la commune
		assertEquals(3601, im3.getGrundstueckNummer().getStammNr());                  // numéro de la parcelle
		assertEquals(Integer.valueOf(7), im3.getGrundstueckNummer().getIndexNr1());
		assertEquals(Integer.valueOf(13), im3.getGrundstueckNummer().getIndexNr2());
		assertNull(im3.getGrundstueckNummer().getIndexNr3());
		assertEquals(Long.valueOf(550L), im3.getAmtlicheBewertung().getAmtlicherWert());
		assertEquals(RegDate.get(2015, 10, 22), im3.getAmtlicheBewertung().getProtokollDatum());
		assertTrue(im3.getAmtlicheBewertung().isProtokollGueltig());
		assertEquals("2015", im3.getAmtlicheBewertung().getProtokollNr());
		assertEquals(Long.valueOf(1L), im3.getStammGrundstueck().getQuote().getAnteilZaehler());
		assertEquals(Long.valueOf(18L), im3.getStammGrundstueck().getQuote().getAnteilNenner());
	}

	@Test
	public void testParseDroits() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_droits_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les droits ont bien été parsés
		final List<EigentumAnteil> droits = callback.getDroits();
		assertEquals(4, droits.size());

		final PersonEigentumAnteil droit0 = (PersonEigentumAnteil) droits.get(0);
		{
			assertEquals(Long.valueOf(1), droit0.getQuote().getAnteilZaehler());
			assertEquals(Long.valueOf(2), droit0.getQuote().getAnteilNenner());
			assertEquals("_1f109152380ffd8901380ffe08d626e9", droit0.getBelastetesGrundstueckIDREF());

			final NatuerlichePersonGb personnePhysique = droit0.getNatuerlichePersonGb();
			assertNotNull(personnePhysique);
			assertEquals("_1f109152380ffd8901380ffdb60b39f4", personnePhysique.getPersonstammIDREF());

			final List<Rechtsgrund> raisonsAcquisition = personnePhysique.getRechtsgruende();
			assertNotNull(raisonsAcquisition);
			assertEquals(1, raisonsAcquisition.size());

			final Rechtsgrund raison0 = raisonsAcquisition.get(0);
			assertEquals(RegDate.get(2007, 10, 19), raison0.getBelegDatum());
			assertEquals("Achat", raison0.getRechtsgrundCode().getTextFr());
			assertEquals(8, raison0.getAmtNummer());
			assertEquals(Integer.valueOf(2007), raison0.getBelegJahr());
			assertEquals(Integer.valueOf(497), raison0.getBelegNummer());
			assertEquals(Integer.valueOf(0), raison0.getBelegNummerIndex());

			assertEquals(PersonEigentumsform.MITEIGENTUM, droit0.getPersonEigentumsForm());
		}

		final PersonEigentumAnteil droit1 = (PersonEigentumAnteil) droits.get(1);
		{
			assertEquals(Long.valueOf(1), droit1.getQuote().getAnteilZaehler());
			assertEquals(Long.valueOf(6), droit1.getQuote().getAnteilNenner());
			assertEquals("_1f1091523810039001381005406907e5", droit1.getBelastetesGrundstueckIDREF());

			final Gemeinschaft communaute = droit1.getGemeinschaft();
			assertNotNull(communaute);
			assertEquals("_1f10915238100390013810060a314f95", communaute.getGemeinschatID());
			assertEmpty(communaute.getRechtsgruende());

			assertEquals(PersonEigentumsform.MITEIGENTUM, droit1.getPersonEigentumsForm());
		}

		final PersonEigentumAnteil droit2 = (PersonEigentumAnteil) droits.get(2);
		{
			assertEquals(Long.valueOf(1), droit2.getQuote().getAnteilZaehler());
			assertEquals(Long.valueOf(1), droit2.getQuote().getAnteilNenner());
			assertEquals("_1f109152381009be0138100bd8d01389", droit2.getBelastetesGrundstueckIDREF());

			JuristischePersonGb personneMorale = droit2.getJuristischePersonGb();
			assertNotNull(personneMorale);
			assertEquals("_1f10915238109122013810913723057f", personneMorale.getPersonstammIDREF());
			assertEmpty(personneMorale.getRechtsgruende());

			assertEquals(PersonEigentumsform.ALLEINEIGENTUM, droit2.getPersonEigentumsForm());
		}

		final GrundstueckEigentumAnteil droit3 = (GrundstueckEigentumAnteil) droits.get(3);
		{
			assertEquals(Long.valueOf(1), droit3.getQuote().getAnteilZaehler());
			assertEquals(Long.valueOf(14), droit3.getQuote().getAnteilNenner());
			assertEquals("_8af806fa40347c3301409bf788af25a5", droit3.getBelastetesGrundstueckIDREF());
			assertEquals("_8af806fa40347c3301409c009e4e25db", droit3.getBerechtigtesGrundstueckIDREF());

			final List<Rechtsgrund> raisonsAcquisition = droit3.getRechtsgruende();
			assertEquals(1, raisonsAcquisition.size());

			final Rechtsgrund raison0 = raisonsAcquisition.get(0);
			assertEquals(RegDate.get(2013, 8, 8), raison0.getBelegDatum());
			assertEquals("Constitution de parts de copropriété", raison0.getRechtsgrundCode().getTextFr());
			assertEquals(6, raison0.getAmtNummer());
			assertEquals(Integer.valueOf(2013), raison0.getBelegJahr());
			assertEquals(Integer.valueOf(6302), raison0.getBelegNummer());
			assertEquals(Integer.valueOf(0), raison0.getBelegNummerIndex());
		}
	}

	@Test
	public void testParseProprietaires() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_proprietaires_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les propriétaires ont bien été parsés
		final List<Personstamm> proprietaires = callback.getProprietaires();
		assertEquals(2, proprietaires.size());

		final NatuerlichePersonstamm prop0 = (NatuerlichePersonstamm) proprietaires.get(0);
		{
			assertEquals("_1f109152380ffd8901380ffda31b11e2", prop0.getPersonstammID());
			assertEquals("Debonneville", prop0.getName());
			assertEquals("Jean-Luc", prop0.getVorname());
			assertNull(prop0.getNrACI());
			assertNull(prop0.getNrIROLE());
			assertEquals(Long.valueOf(209544), prop0.getNoRF());

			final GeburtsDatum dateNaissance = prop0.getGeburtsdatum();
			assertNotNull(dateNaissance);
			assertEquals(Integer.valueOf(21), dateNaissance.getTag());
			assertEquals(Integer.valueOf(5), dateNaissance.getMonat());
			assertEquals(Integer.valueOf(1961), dateNaissance.getJahr());
		}

		final JuristischePersonstamm prop1 = (JuristischePersonstamm) proprietaires.get(1);
		{
			assertEquals("_1f1091523810039001381003a44d07a1", prop1.getPersonstammID());
			assertEquals("Société du pâturage des Seytorées", prop1.getName());
			assertNull(prop1.getNrACI());
			assertEquals(Long.valueOf(113800), prop1.getNoRF());
		}
	}

	@Test
	public void testParseBatiments() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_batiments_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les batiments ont bien été parsées
		final List<Gebaeude> batiments = callback.getBatiments();
		assertEquals(2, batiments.size());

		final Gebaeude bat0 = batiments.get(0);
		{
			final List<GrundstueckZuGebaeude> immeubles = bat0.getGrundstueckZuGebaeude();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());

			final GrundstueckZuGebaeude imm0 = immeubles.get(0);
			assertEquals("_1f109152380ffd8901380ffe19367d20", imm0.getGrundstueckIDREF());
			assertEquals(Integer.valueOf(157), imm0.getAbschnittFlaeche());

			assertNull(bat0.getFlaeche());

			final List<GebaeudeArt> descriptions = bat0.getGebaeudeArten();
			assertNotNull(descriptions);
			assertEquals(1, descriptions.size());
			assertEquals("Habitation avec affectation mixte", descriptions.get(0).getGebaeudeArtCode().getTextFr());
		}

		final Gebaeude bat1 = batiments.get(1);
		{
			final List<GrundstueckZuGebaeude> immeubles = bat1.getGrundstueckZuGebaeude();
			assertNotNull(immeubles);
			assertEquals(1, immeubles.size());

			final GrundstueckZuGebaeude imm0 = immeubles.get(0);
			assertEquals("_1f109152380ffd8901380ffe07fb2421", imm0.getGrundstueckIDREF());
			assertEquals(Integer.valueOf(107), imm0.getAbschnittFlaeche());

			assertEquals(Integer.valueOf(107), bat1.getFlaeche());

			final List<GebaeudeArt> descriptions = bat1.getGebaeudeArten();
			assertNotNull(descriptions);
			assertEquals(1, descriptions.size());
			assertEquals("Habitation", descriptions.get(0).getGebaeudeArtCode().getTextFr());
		}
	}

	@Test
	public void testParseSurfaces() throws Exception {

		final File file = ResourceUtils.getFile("classpath:ch/vd/unireg/registrefoncier/export_surfaces_rf_hebdo.xml");
		assertNotNull(file);

		// on parse le fichier
		final TestCallback callback = new TestCallback();
		try (InputStream is = new FileInputStream(file)) {
			parser.processFile(is, callback);
		}

		// on s'assure que les surfaces ont bien été parsées
		final List<Bodenbedeckung> surfaces = callback.getSurfaces();
		assertEquals(3, surfaces.size());

		final Bodenbedeckung surface0 = surfaces.get(0);
		{
			assertEquals("_1f10915238109122013810973b5b5d7e", surface0.getGrundstueckIDREF());
			assertEquals(Integer.valueOf(1766), surface0.getFlaeche());
			assertEquals("Place-jardin", surface0.getArt().getTextFr());
		}

		final Bodenbedeckung surface1 = surfaces.get(1);
		{
			assertEquals("_1f10915238102ecd01381030e605091e", surface1.getGrundstueckIDREF());
			assertEquals(Integer.valueOf(3457), surface1.getFlaeche());
			assertEquals("Pré-champ", surface1.getArt().getTextFr());
		}

		final Bodenbedeckung surface2 = surfaces.get(2);
		{
			assertEquals("_1f10915238107c1501381081420d5645", surface2.getGrundstueckIDREF());
			assertEquals(Integer.valueOf(3510), surface2.getFlaeche());
			assertEquals("Forêt", surface2.getArt().getTextFr());
		}
	}

	private static class TestCallback implements FichierImmeublesRFParser.Callback {

		private final List<Grundstueck> immeubles = new ArrayList<>();
		private final List<EigentumAnteil> droits = new ArrayList<>();
		private final List<Personstamm> proprietaires = new ArrayList<>();
		private final List<Gebaeude> batiments = new ArrayList<>();
		private final List<Bodenbedeckung> surfaces = new ArrayList<>();

		@Override
		public void onImmeuble(@NotNull Grundstueck immeuble) {
			immeubles.add(immeuble);
		}

		@Override
		public void onDroit(EigentumAnteil droit) {
			droits.add(droit);
		}

		@Override
		public void onProprietaire(@NotNull Personstamm personne) {
			proprietaires.add(personne);
		}

		@Override
		public void onBatiment(@NotNull Gebaeude batiment) {
			batiments.add(batiment);
		}

		@Override
		public void onSurface(@NotNull Bodenbedeckung surface) {
			surfaces.add(surface);
		}

		public List<Grundstueck> getImmeubles() {
			return immeubles;
		}

		public List<EigentumAnteil> getDroits() {
			return droits;
		}

		public List<Personstamm> getProprietaires() {
			return proprietaires;
		}

		public List<Gebaeude> getBatiments() {
			return batiments;
		}

		public List<Bodenbedeckung> getSurfaces() {
			return surfaces;
		}

		@Override
		public void done() {
		}
	}
}
