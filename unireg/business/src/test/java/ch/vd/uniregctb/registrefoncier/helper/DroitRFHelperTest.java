package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.rf.GenrePropriete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DroitRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(DroitRFHelper.dataEquals((Set<DroitRF>) null, null));
		assertTrue(DroitRFHelper.dataEquals(Collections.emptySet(), null));
		assertTrue(DroitRFHelper.dataEquals(null, Collections.emptyList()));
		assertTrue(DroitRFHelper.dataEquals(Collections.emptySet(), Collections.emptyList()));

		assertFalse(DroitRFHelper.dataEquals(null, Collections.singletonList(new PersonEigentumAnteil())));
		assertFalse(DroitRFHelper.dataEquals(Collections.singleton(new DroitProprietePersonnePhysiqueRF()), null));
	}

	@Test
	public void testDataEqualsListDifferentSizes() throws Exception {
		assertFalse(DroitRFHelper.dataEquals(Collections.singleton(new DroitProprietePersonnePhysiqueRF()),
		                                     Arrays.asList(new PersonEigentumAnteil(), new PersonEigentumAnteil())));
	}

	@Test
	public void testDataEqualsList() throws Exception {

		final DroitProprietePersonnePhysiqueRF droitPP1 = new DroitProprietePersonnePhysiqueRF();
		{
			final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF("34838282030");

			final ImmeubleRF immeuble = new BienFondRF();
			immeuble.setIdRF("ae93920bc34");

			droitPP1.setMasterIdRF("9a9c9e94923");
			droitPP1.setAyantDroit(pp);
			droitPP1.setImmeuble(immeuble);
			droitPP1.setCommunaute(null);
			droitPP1.setDateDebut(RegDate.get(2010, 6, 1));
			droitPP1.setMotifDebut("Achat");
			droitPP1.setDateFin(null);
			droitPP1.setMotifFin(null);
			droitPP1.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
			droitPP1.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 120, 3));
			droitPP1.setPart(new Fraction(1, 2));
			droitPP1.setRegime(GenrePropriete.COPROPRIETE);
		}

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		{
			final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF("574737237");

			final ImmeubleRF immeuble = new BienFondRF();
			immeuble.setIdRF("5848cd8483");

			droitPP2.setMasterIdRF("4848d8e83");
			droitPP2.setAyantDroit(pp);
			droitPP2.setImmeuble(immeuble);
			droitPP2.setCommunaute(null);
			droitPP2.setDateDebut(RegDate.get(2013, 9, 2));
			droitPP2.setMotifDebut("Héritage");
			droitPP2.setDateFin(null);
			droitPP2.setMotifFin(null);
			droitPP2.setDateDebutOfficielle(RegDate.get(2013, 8, 22));
			droitPP2.setNumeroAffaire(new IdentifiantAffaireRF(6, 2013, 33, 1));
			droitPP2.setPart(new Fraction(1, 1));
			droitPP2.setRegime(GenrePropriete.INDIVIDUELLE);
		}

		final PersonEigentumAnteil eigentumAnteil1 = new PersonEigentumAnteil();
		{
			final Rechtsgrund recht = new Rechtsgrund();
			recht.setBelegDatum(RegDate.get(2010, 4, 23));
			recht.setAmtNummer(6);
			recht.setBelegJahr(2010);
			recht.setBelegNummer(120);
			recht.setBelegNummerIndex(3);
			recht.setRechtsgrundCode(new CapiCode(null, "Achat"));

			final NatuerlichePersonGb natuerliche = new NatuerlichePersonGb();
			natuerliche.setPersonstammIDREF("34838282030");
			natuerliche.getRechtsgruende().add(recht);

			eigentumAnteil1.setMasterID("9a9c9e94923");
			eigentumAnteil1.setNatuerlichePersonGb(natuerliche);
			eigentumAnteil1.setBelastetesGrundstueckIDREF("ae93920bc34");
			eigentumAnteil1.setQuote(new Quote(1L, 2L, null, null));
			eigentumAnteil1.setPersonEigentumsForm(PersonEigentumsform.MITEIGENTUM);
		}

		final PersonEigentumAnteil eigentumAnteil2 = new PersonEigentumAnteil();
		{
			final Rechtsgrund recht = new Rechtsgrund();
			recht.setBelegDatum(RegDate.get(2013, 8, 22));
			recht.setAmtNummer(6);
			recht.setBelegJahr(2013);
			recht.setBelegNummer(33);
			recht.setBelegNummerIndex(1);
			recht.setRechtsgrundCode(new CapiCode(null, "Héritage"));

			final NatuerlichePersonGb natuerliche = new NatuerlichePersonGb();
			natuerliche.setPersonstammIDREF("574737237");
			natuerliche.getRechtsgruende().add(recht);

			eigentumAnteil2.setMasterID("4848d8e83");
			eigentumAnteil2.setNatuerlichePersonGb(natuerliche);
			eigentumAnteil2.setBelastetesGrundstueckIDREF("5848cd8483");
			eigentumAnteil2.setQuote(new Quote(1L, 1L, null, null));
			eigentumAnteil2.setPersonEigentumsForm(PersonEigentumsform.ALLEINEIGENTUM);
		}

		assertTrue(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP1, droitPP2)), Arrays.asList(eigentumAnteil1, eigentumAnteil2)));
		assertTrue(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP2, droitPP1)), Arrays.asList(eigentumAnteil2, eigentumAnteil1)));
		assertFalse(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP1)), Arrays.asList(eigentumAnteil2)));
	}

	@Test
	public void testDataEqualsDroitProprietePP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("34838282030");

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprietePersonnePhysiqueRF droitPP = new DroitProprietePersonnePhysiqueRF();
		droitPP.setMasterIdRF("9a9c9e94923");
		droitPP.setAyantDroit(pp);
		droitPP.setImmeuble(immeuble);
		droitPP.setCommunaute(null);
		droitPP.setDateDebut(RegDate.get(2010, 6, 1));
		droitPP.setMotifDebut("Achat");
		droitPP.setDateFin(null);
		droitPP.setMotifFin(null);
		droitPP.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPP.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 120, 3));
		droitPP.setPart(new Fraction(1, 2));
		droitPP.setRegime(GenrePropriete.COPROPRIETE);

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(RegDate.get(2010, 4, 23));
		recht.setAmtNummer(6);
		recht.setBelegJahr(2010);
		recht.setBelegNummer(120);
		recht.setBelegNummerIndex(3);
		recht.setRechtsgrundCode(new CapiCode(null, "Achat"));

		final NatuerlichePersonGb natuerliche = new NatuerlichePersonGb();
		natuerliche.setPersonstammIDREF("34838282030");
		natuerliche.getRechtsgruende().add(recht);

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setNatuerlichePersonGb(natuerliche);
		eigentumAnteil.setBelastetesGrundstueckIDREF("ae93920bc34");
		eigentumAnteil.setQuote(new Quote(1L, 2L, null, null));
		eigentumAnteil.setPersonEigentumsForm(PersonEigentumsform.MITEIGENTUM);

		assertTrue(DroitRFHelper.dataEquals(droitPP, eigentumAnteil));
	}

	@Test
	public void testDataEqualsDroitProprietePM() throws Exception {

		final PersonneMoraleRF pm = new PersonneMoraleRF();
		pm.setIdRF("34838282030");

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprietePersonneMoraleRF droitPM = new DroitProprietePersonneMoraleRF();
		droitPM.setMasterIdRF("9a9c9e94923");
		droitPM.setAyantDroit(pm);
		droitPM.setImmeuble(immeuble);
		droitPM.setCommunaute(null);
		droitPM.setDateDebut(RegDate.get(2010, 6, 1));
		droitPM.setMotifDebut("Achat");
		droitPM.setDateFin(null);
		droitPM.setMotifFin(null);
		droitPM.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitPM.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 120, 3));
		droitPM.setPart(new Fraction(1, 2));
		droitPM.setRegime(GenrePropriete.INDIVIDUELLE);

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(RegDate.get(2010, 4, 23));
		recht.setAmtNummer(6);
		recht.setBelegJahr(2010);
		recht.setBelegNummer(120);
		recht.setBelegNummerIndex(3);
		recht.setRechtsgrundCode(new CapiCode(null, "Achat"));

		final JuristischePersonGb juristiche = new JuristischePersonGb();
		juristiche.setPersonstammIDREF("34838282030");
		juristiche.getRechtsgruende().add(recht);

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setJuristischePersonGb(juristiche);
		eigentumAnteil.setBelastetesGrundstueckIDREF("ae93920bc34");
		eigentumAnteil.setQuote(new Quote(1L, 2L, null, null));
		eigentumAnteil.setPersonEigentumsForm(PersonEigentumsform.ALLEINEIGENTUM);

		assertTrue(DroitRFHelper.dataEquals(droitPM, eigentumAnteil));
	}

	@Test
	public void testDataEqualsDroitCommunaute() throws Exception {

		final CommunauteRF communaute = new CommunauteRF();
		communaute.setIdRF("34838282030");

		final ImmeubleRF immeuble = new BienFondRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprieteCommunauteRF droitComm = new DroitProprieteCommunauteRF();
		droitComm.setMasterIdRF("9a9c9e94923");
		droitComm.setAyantDroit(communaute);
		droitComm.setImmeuble(immeuble);
		droitComm.setDateDebut(RegDate.get(2010, 6, 1));
		droitComm.setMotifDebut("Achat");
		droitComm.setDateFin(null);
		droitComm.setMotifFin(null);
		droitComm.setDateDebutOfficielle(RegDate.get(2010, 4, 23));
		droitComm.setNumeroAffaire(new IdentifiantAffaireRF(6, 2010, 120, 3));
		droitComm.setPart(new Fraction(1, 2));
		droitComm.setRegime(GenrePropriete.COMMUNE);

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(RegDate.get(2010, 4, 23));
		recht.setAmtNummer(6);
		recht.setBelegJahr(2010);
		recht.setBelegNummer(120);
		recht.setBelegNummerIndex(3);
		recht.setRechtsgrundCode(new CapiCode(null, "Achat"));

		final Gemeinschaft gemeinschaft = new Gemeinschaft();
		gemeinschaft.setGemeinschatID("34838282030");
		gemeinschaft.getRechtsgruende().add(recht);

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setGemeinschaft(gemeinschaft);
		eigentumAnteil.setBelastetesGrundstueckIDREF("ae93920bc34");
		eigentumAnteil.setQuote(new Quote(1L, 2L, null, null));
		eigentumAnteil.setPersonEigentumsForm(PersonEigentumsform.GESAMTEIGENTUM);

		assertTrue(DroitRFHelper.dataEquals(droitComm, eigentumAnteil));
	}

	/**
	 * Vérifie qu'un droit sur une personne physique n'est pas égal au droit sur une personne morale.
	 */
	@Test
	public void testDataTypeNotEqualsDroitPP() throws Exception {

		final DroitProprietePersonnePhysiqueRF droitPP = new DroitProprietePersonnePhysiqueRF();
		droitPP.setMasterIdRF("9a9c9e94923");

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setJuristischePersonGb(new JuristischePersonGb());

		assertFalse(DroitRFHelper.dataEquals(droitPP, eigentumAnteil));
	}

	/**
	 * Vérifie qu'un droit sur une personne morale n'est pas égal au droit sur une personne physique.
	 */
	@Test
	public void testDataTypeNotEqualsDroitPM() throws Exception {

		final DroitProprietePersonneMoraleRF droitPM = new DroitProprietePersonneMoraleRF();
		droitPM.setMasterIdRF("9a9c9e94923");

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setNatuerlichePersonGb(new NatuerlichePersonGb());

		assertFalse(DroitRFHelper.dataEquals(droitPM, eigentumAnteil));
	}

	/**
	 * Vérifie qu'un droit sur une communauté n'est pas égal au droit sur une personne physique.
	 */
	@Test
	public void testDataTypeNotEqualsDroitCommunaute() throws Exception {

		final DroitProprieteCommunauteRF droitComm = new DroitProprieteCommunauteRF();
		droitComm.setMasterIdRF("9a9c9e94923");

		final PersonEigentumAnteil eigentumAnteil = new PersonEigentumAnteil();
		eigentumAnteil.setMasterID("9a9c9e94923");
		eigentumAnteil.setNatuerlichePersonGb(new NatuerlichePersonGb());

		assertFalse(DroitRFHelper.dataEquals(droitComm, eigentumAnteil));
	}

	@Test
	public void testGetDroitDeReference() throws Exception {

		final Rechtsgrund droit1 = new Rechtsgrund(1, null, null, RegDate.get(2010, 1, 22), null, null, null, null, null);
		final Rechtsgrund droit2 = new Rechtsgrund(2, null, null, RegDate.get(2014, 4, 12), null, null, null, null, null);

		assertSame(droit1, DroitRFHelper.getDroitDeReference(Arrays.asList(droit1, droit2)));
		assertSame(droit1, DroitRFHelper.getDroitDeReference(Arrays.asList(droit2, droit1)));
	}

	@Test
	public void testGetDroitDeReferenceListVide() throws Exception {
		assertNull(DroitRFHelper.getDroitDeReference(Collections.emptyList()));
	}

	@Test
	public void testGetDroitDeReferenceAnneeNulle() throws Exception {

		final Rechtsgrund droit1 = new Rechtsgrund(1, null, null, null, null, null, null, null, null);
		final Rechtsgrund droit2 = new Rechtsgrund(2, null, null, RegDate.get(2014, 4, 12), null, null, null, null, null);

		assertSame(droit1, DroitRFHelper.getDroitDeReference(Arrays.asList(droit1, droit2)));
		assertSame(droit1, DroitRFHelper.getDroitDeReference(Arrays.asList(droit2, droit1)));
	}
}
