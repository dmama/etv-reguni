package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.capitastra.grundstueck.Bergwerk;
import ch.vd.capitastra.grundstueck.GewoehnlichesMiteigentum;
import ch.vd.capitastra.grundstueck.GrundstueckFlaeche;
import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.SDR;
import ch.vd.capitastra.grundstueck.StammGrundstueck;
import ch.vd.capitastra.grundstueck.StockwerksEinheit;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.UniregJUnit4Runner;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitDistinctEtPermanentRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.MineRF;
import ch.vd.uniregctb.registrefoncier.PartCoproprieteRF;
import ch.vd.uniregctb.registrefoncier.ProprieteParEtageRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF;

import static ch.vd.uniregctb.common.AbstractSpringTest.assertEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(UniregJUnit4Runner.class)
public class ImmeubleRFHelperTest {

	/**
	 * Ce test vérifie que deux immeubles identiques sont bien considérés égaux.
	 */
	@Test
	public void testCurrentDataEquals() throws Exception {
		
		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);
		estimation.setDateDebut(RegDate.get(2000, 1, 1));

		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setSurface(1329);

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);
		immeuble.addSurfaceTotale(surfaceTotale);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(1329);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		assertTrue(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles identiques mais dont l'un est radié sont bien considérés inégaux.
	 */
	@Test
	public void testCurrentDataEqualsImmeubleRadie() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);
		estimation.setDateDebut(RegDate.get(2000, 1, 1));

		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setSurface(1329);

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);
		immeuble.addSurfaceTotale(surfaceTotale);
		immeuble.setDateRadiation(RegDate.get(2017, 1, 1)); // <---- immeuble radié

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(1329);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		// l'immeuble est radié : pas égal
		assertFalse(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles avec des situations différentes sont bien considérés inégaux.
	 */
	@Test
	public void testCurrentDataEqualsSituationsDifferentes() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);
		estimation.setDateDebut(RegDate.get(2000, 1, 1));

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(5586);   // <-- différent
		grundstueckNummer.setStammNr(22);   // <-- différent

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);

		assertFalse(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles avec des estimations différentes sont bien considérés inégaux.
	 */
	@Test
	public void testCurrentDataEqualsEstimationsDifferentes() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);
		estimation.setDateDebut(RegDate.get(2000, 1, 1));

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);                    // <-- différent
		amtlicheBewertung.setProtokollNr("2016");                       // <-- différent
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));   // <-- différent
		amtlicheBewertung.setProtokollGueltig(true);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);

		assertFalse(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles avec des surfaces totales différentes sont bien considérés inégaux.
	 */
	@Test
	public void testCurrentDataEqualsSurfacesTotalesDifferentes() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000L);
		estimation.setReference("2015");
		estimation.setDateInscription(RegDate.get(2015, 7, 1));
		estimation.setEnRevision(false);
		estimation.setDateDebut(RegDate.get(2000, 1, 1));

		final SurfaceTotaleRF surfaceTotale = new SurfaceTotaleRF();
		surfaceTotale.setSurface(1329);

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.addEstimation(estimation);
		immeuble.addSurfaceTotale(surfaceTotale);

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(120000L);
		amtlicheBewertung.setProtokollNr("2015");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2015, 7, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);    // <--- différent

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		assertFalse(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles sans estimation sont bien considérés égaux.
	 */
	@Test
	public void testCurrentDataEqualsSansEstimation() throws Exception {

		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(2233);

		final SituationRF situation = new SituationRF();
		situation.setCommune(commune);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final BienFondRF immeuble = new BienFondRF();
		immeuble.setIdRF("382929efa218");
		immeuble.setCfa(true);
		immeuble.setEgrid("CH282891891");
		immeuble.addSituation(situation);
		immeuble.setEstimations(new HashSet<>());
		immeuble.setSurfacesTotales(new HashSet<>());

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);

		assertTrue(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	@Test
	public void testNewImmeubleRFMine() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2016");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);

		final Bergwerk grundstueck = new Bergwerk();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(MineRF.class, immeuble.getClass());

		final MineRF mine = (MineRF) immeuble;
		assertEquals("382929efa218", mine.getIdRF());
		assertEquals("CH282891891", mine.getEgrid());

		final Set<SituationRF> situations = mine.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(37823), situation.getIndex2());
		assertEquals(Integer.valueOf(82), situation.getIndex3());

		final Set<EstimationRF> estimations = mine.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("2016", estimation.getReference());
		assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());

		final Set<SurfaceTotaleRF> surfacesTotales = mine.getSurfacesTotales();
		assertEquals(1, surfacesTotales.size());
		final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
		assertEquals(322, surfaceTotale.getSurface());
	}

	@Test
	public void testNewImmeubleRFCopropriete() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2016");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GewoehnlichesMiteigentum grundstueck = new GewoehnlichesMiteigentum();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setStammGrundstueck(new StammGrundstueck(new Quote(1L, 3L, null, null), null, null));

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(PartCoproprieteRF.class, immeuble.getClass());

		final PartCoproprieteRF copro = (PartCoproprieteRF) immeuble;
		assertEquals("382929efa218", copro.getIdRF());
		assertEquals("CH282891891", copro.getEgrid());
		assertEquals(new Fraction(1, 3), copro.getQuotePart());

		final Set<SituationRF> situations = copro.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(37823), situation.getIndex2());
		assertEquals(Integer.valueOf(82), situation.getIndex3());

		final Set<EstimationRF> estimations = copro.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("2016", estimation.getReference());
		assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());
	}

	@Test
	public void testNewImmeubleRFBienFond() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2016");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(BienFondRF.class, immeuble.getClass());

		final BienFondRF bienFond = (BienFondRF) immeuble;
		assertEquals("382929efa218", bienFond.getIdRF());
		assertTrue(bienFond.isCfa());
		assertEquals("CH282891891", bienFond.getEgrid());

		final Set<SituationRF> situations = bienFond.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(37823), situation.getIndex2());
		assertEquals(Integer.valueOf(82), situation.getIndex3());

		final Set<EstimationRF> estimations = bienFond.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("2016", estimation.getReference());
		assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());

		final Set<SurfaceTotaleRF> surfacesTotales = bienFond.getSurfacesTotales();
		assertEquals(1, surfacesTotales.size());
		final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
		assertEquals(322, surfaceTotale.getSurface());
	}

	@Test
	public void testNewImmeubleRFDDP() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2016");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);

		final SDR grundstueck = new SDR();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(DroitDistinctEtPermanentRF.class, immeuble.getClass());

		final DroitDistinctEtPermanentRF ddp = (DroitDistinctEtPermanentRF) immeuble;
		assertEquals("382929efa218", ddp.getIdRF());
		assertEquals("CH282891891", ddp.getEgrid());

		final Set<SituationRF> situations = ddp.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(37823), situation.getIndex2());
		assertEquals(Integer.valueOf(82), situation.getIndex3());

		final Set<EstimationRF> estimations = ddp.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("2016", estimation.getReference());
		assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());

		final Set<SurfaceTotaleRF> surfacesTotales = ddp.getSurfacesTotales();
		assertEquals(1, surfacesTotales.size());
		final SurfaceTotaleRF surfaceTotale = surfacesTotales.iterator().next();
		assertEquals(322, surfaceTotale.getSurface());
	}

	@Test
	public void testNewImmeubleRFPPE() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("2016");
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final StockwerksEinheit grundstueck = new StockwerksEinheit();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setStammGrundstueck(new StammGrundstueck(new Quote(1L, 3L, null, null), null, null));

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(ProprieteParEtageRF.class, immeuble.getClass());

		final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeuble;
		assertEquals("382929efa218", ppe.getIdRF());
		assertEquals("CH282891891", ppe.getEgrid());
		assertEquals(new Fraction(1, 3), ppe.getQuotePart());

		final Set<SituationRF> situations = ppe.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(2233, situation.getCommune().getNoRf());
		assertEquals(109, situation.getNoParcelle());
		assertEquals(Integer.valueOf(17), situation.getIndex1());
		assertEquals(Integer.valueOf(37823), situation.getIndex2());
		assertEquals(Integer.valueOf(82), situation.getIndex3());

		final Set<EstimationRF> estimations = ppe.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("2016", estimation.getReference());
		assertEquals(Integer.valueOf(2016), estimation.getAnneeReference());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());
	}

	/**
	 * Cas de l'immeuble rfId=_8af80e62567f816f01571d91f3e56a38 qui ne possède pas d'estimation fiscale.
	 */
	@Test
	public void testNewImmeubleSansEstimationFiscale() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(13);
		grundstueckNummer.setStammNr(917);
		grundstueckNummer.setIndexNr1(106);

		final StockwerksEinheit grundstueck = new StockwerksEinheit();
		grundstueck.setGrundstueckID("_8af80e62567f816f01571d91f3e56a38");
		grundstueck.setEGrid("CH776584246539");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setStammGrundstueck(new StammGrundstueck(new Quote(8L, 1000L, null, null), null, null));

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(ProprieteParEtageRF.class, immeuble.getClass());

		final ProprieteParEtageRF ppe = (ProprieteParEtageRF) immeuble;
		assertEquals("_8af80e62567f816f01571d91f3e56a38", ppe.getIdRF());
		assertEquals("CH776584246539", ppe.getEgrid());
		assertEquals(new Fraction(8, 1000), ppe.getQuotePart());

		final Set<SituationRF> situations = ppe.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(13, situation.getCommune().getNoRf());
		assertEquals(917, situation.getNoParcelle());
		assertEquals(Integer.valueOf(106), situation.getIndex1());
		assertNull(situation.getIndex2());
		assertNull(situation.getIndex3());

		final Set<EstimationRF> estimations = ppe.getEstimations();
		assertEmpty(estimations);
	}

	/**
	 * [SIFISC-23478] Vérifie que l'année de référence est bien calculée à nulle si la référence n'est pas exploitable.
	 */
	@Test
	public void testNewImmeubleAvecEstimationRevisionInutilisable() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(2233);
		grundstueckNummer.setStammNr(109);
		grundstueckNummer.setIndexNr1(17);
		grundstueckNummer.setIndexNr2(37823);
		grundstueckNummer.setIndexNr3(82);

		final AmtlicheBewertung amtlicheBewertung = new AmtlicheBewertung();
		amtlicheBewertung.setAmtlicherWert(500000L);
		amtlicheBewertung.setProtokollNr("70000");  // <-- référence non-exploitable
		amtlicheBewertung.setProtokollDatum(RegDate.get(2016, 1, 1));
		amtlicheBewertung.setProtokollGueltig(true);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(322);

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("382929efa218");
		grundstueck.setLigUnterartEnum("cfa");
		grundstueck.setEGrid("CH282891891");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setAmtlicheBewertung(amtlicheBewertung);
		grundstueck.setGrundstueckFlaeche(flaeche);

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(BienFondRF.class, immeuble.getClass());

		final BienFondRF bienFond = (BienFondRF) immeuble;
		assertEquals("382929efa218", bienFond.getIdRF());
		assertTrue(bienFond.isCfa());
		assertEquals("CH282891891", bienFond.getEgrid());

		final Set<EstimationRF> estimations = bienFond.getEstimations();
		assertEquals(1, estimations.size());
		final EstimationRF estimation = estimations.iterator().next();
		assertEquals(Long.valueOf(500000L), estimation.getMontant());
		assertEquals("70000", estimation.getReference());
		assertNull(estimation.getAnneeReference()); // <--- l'année de référence est nulle
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateInscription());
		assertEquals(RegDate.get(2016, 1, 1), estimation.getDateDebutMetier());
		assertNull(estimation.getDateFinMetier());
		assertFalse(estimation.isEnRevision());
	}

	/**
	 * Cas de l'immeuble rfId=_8af806fc3971fea40139902846c13c38 qui possède une surface totale mais sans indication de surface...
	 */
	@Test
	public void testNewImmeubleAvecSurfaceTotaleNulle() throws Exception {

		final GrundstueckNummer grundstueckNummer = new GrundstueckNummer();
		grundstueckNummer.setBfsNr(62);
		grundstueckNummer.setStammNr(100008);

		final GrundstueckFlaeche flaeche = new GrundstueckFlaeche();
		flaeche.setFlaeche(null);   // <--- là !

		final Liegenschaft grundstueck = new Liegenschaft();
		grundstueck.setGrundstueckID("_8af806fc3971fea40139902846c13c38");
		grundstueck.setGrundstueckNummer(grundstueckNummer);
		grundstueck.setGrundstueckFlaeche(flaeche);
		grundstueck.setLigUnterartEnum("cfa");

		final ImmeubleRF immeuble = ImmeubleRFHelper.newImmeubleRF(grundstueck, ImmeubleRFHelperTest::simplisticCommuneProvider);
		assertEquals(BienFondRF.class, immeuble.getClass());

		final BienFondRF bf = (BienFondRF) immeuble;
		assertEquals("_8af806fc3971fea40139902846c13c38", bf.getIdRF());
		assertNull(bf.getEgrid());

		final Set<SituationRF> situations = bf.getSituations();
		assertEquals(1, situations.size());
		final SituationRF situation = situations.iterator().next();
		assertEquals(62, situation.getCommune().getNoRf());
		assertEquals(100008, situation.getNoParcelle());
		assertNull(situation.getIndex1());
		assertNull(situation.getIndex2());
		assertNull(situation.getIndex3());

		assertEmpty(bf.getEstimations());
		assertEmpty(bf.getSurfacesAuSol());
	}

	private static CommuneRF simplisticCommuneProvider(Integer noRf) {
		final CommuneRF commune = new CommuneRF();
		commune.setNoRf(noRf);
		return commune;
	}
}