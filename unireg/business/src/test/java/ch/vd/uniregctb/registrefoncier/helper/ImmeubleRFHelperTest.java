package ch.vd.uniregctb.registrefoncier.helper;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.capitastra.grundstueck.Liegenschaft;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.EstimationRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImmeubleRFHelperTest {

	/**
	 * Ce test vérifie que deux immeubles identiques sont bien considérés égaux.
	 */
	@Test
	public void testCurrentDataEquals() throws Exception {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
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

		assertTrue(ImmeubleRFHelper.currentDataEquals(immeuble, grundstueck));
	}

	/**
	 * Ce test vérifie que deux immeubles avec des situations différentes sont bien considérés inégaux.
	 */
	@Test
	public void testCurrentDataEqualsSituationsDifferentes() throws Exception {

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
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

		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(2233);
		situation.setNoParcelle(109);
		situation.setIndex1(17);

		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(120000);
		estimation.setReference("2015");
		estimation.setDateEstimation(RegDate.get(2015, 7, 1));
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
}