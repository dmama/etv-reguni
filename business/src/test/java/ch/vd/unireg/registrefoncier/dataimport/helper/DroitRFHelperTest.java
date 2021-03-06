package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumsform;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class DroitRFHelperTest {

	@Test
	public void testDataEqualsListNullity() throws Exception {

		assertTrue(DroitRFHelper.dataEquals((Set<DroitProprieteRF>) null, null));
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

			final ImmeubleRF immeuble = new BienFondsRF();
			immeuble.setIdRF("ae93920bc34");

			droitPP1.setMasterIdRF("9a9c9e94923");
			droitPP1.setVersionIdRF("1");
			droitPP1.setAyantDroit(pp);
			droitPP1.setImmeuble(immeuble);
			droitPP1.setCommunaute(null);
			droitPP1.setDateDebut(RegDate.get(2010, 6, 1));
			droitPP1.setDateFin(null);
			droitPP1.setDateDebutMetier(RegDate.get(2010, 4, 23));
			droitPP1.setMotifDebut("Achat");
			droitPP1.setMotifFin(null);
			droitPP1.setPart(new Fraction(1, 2));
			droitPP1.setRegime(GenrePropriete.COPROPRIETE);
			droitPP1.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2010, 120, 3)));
		}

		final DroitProprietePersonnePhysiqueRF droitPP2 = new DroitProprietePersonnePhysiqueRF();
		{
			final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF("574737237");

			final ImmeubleRF immeuble = new BienFondsRF();
			immeuble.setIdRF("5848cd8483");

			droitPP2.setMasterIdRF("4848d8e83");
			droitPP2.setVersionIdRF("1");
			droitPP2.setAyantDroit(pp);
			droitPP2.setImmeuble(immeuble);
			droitPP2.setCommunaute(null);
			droitPP2.setDateDebut(RegDate.get(2013, 9, 2));
			droitPP2.setDateFin(null);
			droitPP2.setDateDebutMetier(RegDate.get(2013, 8, 22));
			droitPP2.setMotifDebut("Héritage");
			droitPP2.setMotifFin(null);
			droitPP2.setPart(new Fraction(1, 1));
			droitPP2.setRegime(GenrePropriete.INDIVIDUELLE);
			droitPP2.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2013, 8, 22), "Héritage", new IdentifiantAffaireRF(6, 2013, 33, 1)));
		}

		final DroitProprieteImmeubleRF droitImm3 = new DroitProprieteImmeubleRF();
		{
			final ImmeubleBeneficiaireRF beneficiaire1 = new ImmeubleBeneficiaireRF();
			beneficiaire1.setIdRF("5848cd8483");

			final ImmeubleRF immeuble2 = new BienFondsRF();
			immeuble2.setIdRF("ae93920bc34");

			droitImm3.setMasterIdRF("029382719");
			droitImm3.setVersionIdRF("1");
			droitImm3.setAyantDroit(beneficiaire1);
			droitImm3.setImmeuble(immeuble2);
			droitImm3.setDateDebut(RegDate.get(2015, 3, 27));
			droitImm3.setDateFin(null);
			droitImm3.setDateDebutMetier(RegDate.get(2015, 2, 10));
			droitImm3.setMotifDebut("Constitution de parts de copropriété");
			droitImm3.setMotifFin(null);
			droitImm3.setPart(new Fraction(1, 14));
			droitImm3.setRegime(GenrePropriete.COPROPRIETE);
			droitImm3.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2015,2,10), "Constitution de parts de copropriété", new IdentifiantAffaireRF(6, 2015, 3400, 1)));
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
			eigentumAnteil1.setVersionID("1");
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
			eigentumAnteil2.setVersionID("1");
			eigentumAnteil2.setNatuerlichePersonGb(natuerliche);
			eigentumAnteil2.setBelastetesGrundstueckIDREF("5848cd8483");
			eigentumAnteil2.setQuote(new Quote(1L, 1L, null, null));
			eigentumAnteil2.setPersonEigentumsForm(PersonEigentumsform.ALLEINEIGENTUM);
		}

		final GrundstueckEigentumAnteil eigentumAnteil3 = new GrundstueckEigentumAnteil();
		{
			final Rechtsgrund recht = new Rechtsgrund();
			recht.setBelegDatum(RegDate.get(2015,2,10));
			recht.setAmtNummer(6);
			recht.setBelegJahr(2015);
			recht.setBelegNummer(3400);
			recht.setBelegNummerIndex(1);
			recht.setRechtsgrundCode(new CapiCode(null, "Constitution de parts de copropriété"));

			eigentumAnteil3.setMasterID("029382719");
			eigentumAnteil3.setVersionID("1");
			eigentumAnteil3.setBerechtigtesGrundstueckIDREF("5848cd8483");
			eigentumAnteil3.setBelastetesGrundstueckIDREF("ae93920bc34");
			eigentumAnteil3.setQuote(new Quote(1L, 14L, null, null));
			eigentumAnteil3.setGrundstueckEigentumsForm(GrundstueckEigentumsform.MITEIGENTUM);
			eigentumAnteil3.getRechtsgruende().add(recht);
		}

		assertTrue(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP1, droitPP2)), Arrays.asList(eigentumAnteil1, eigentumAnteil2)));
		assertTrue(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP2, droitPP1)), Arrays.asList(eigentumAnteil2, eigentumAnteil1)));
		assertTrue(DroitRFHelper.dataEquals(new HashSet<>(Arrays.asList(droitPP1, droitImm3)), Arrays.asList(eigentumAnteil1, eigentumAnteil3)));
		assertFalse(DroitRFHelper.dataEquals(new HashSet<>(Collections.singletonList(droitPP1)), Collections.singletonList(eigentumAnteil2)));
		assertFalse(DroitRFHelper.dataEquals(new HashSet<>(Collections.singletonList(droitImm3)), Collections.singletonList(eigentumAnteil2)));
	}

	@Test
	public void testDataEqualsDroitProprietePP() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setIdRF("34838282030");

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprietePersonnePhysiqueRF droitPP = new DroitProprietePersonnePhysiqueRF();
		droitPP.setMasterIdRF("9a9c9e94923");
		droitPP.setVersionIdRF("1");
		droitPP.setAyantDroit(pp);
		droitPP.setImmeuble(immeuble);
		droitPP.setCommunaute(null);
		droitPP.setDateDebut(RegDate.get(2010, 6, 1));
		droitPP.setDateFin(null);
		droitPP.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPP.setMotifDebut("Achat");
		droitPP.setMotifFin(null);
		droitPP.setPart(new Fraction(1, 2));
		droitPP.setRegime(GenrePropriete.COPROPRIETE);
		droitPP.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2010, 120, 3)));

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
		eigentumAnteil.setVersionID("1");
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

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprietePersonneMoraleRF droitPM = new DroitProprietePersonneMoraleRF();
		droitPM.setMasterIdRF("9a9c9e94923");
		droitPM.setVersionIdRF("1");
		droitPM.setAyantDroit(pm);
		droitPM.setImmeuble(immeuble);
		droitPM.setCommunaute(null);
		droitPM.setDateDebut(RegDate.get(2010, 6, 1));
		droitPM.setDateFin(null);
		droitPM.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitPM.setMotifDebut("Achat");
		droitPM.setMotifFin(null);
		droitPM.setPart(new Fraction(1, 2));
		droitPM.setRegime(GenrePropriete.INDIVIDUELLE);
		droitPM.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2010, 120, 3)));

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
		eigentumAnteil.setVersionID("1");
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

		final ImmeubleRF immeuble = new BienFondsRF();
		immeuble.setIdRF("ae93920bc34");

		final DroitProprieteCommunauteRF droitComm = new DroitProprieteCommunauteRF();
		droitComm.setMasterIdRF("9a9c9e94923");
		droitComm.setVersionIdRF("1");
		droitComm.setAyantDroit(communaute);
		droitComm.setImmeuble(immeuble);
		droitComm.setDateDebut(RegDate.get(2010, 6, 1));
		droitComm.setDateFin(null);
		droitComm.setDateDebutMetier(RegDate.get(2010, 4, 23));
		droitComm.setMotifDebut("Achat");
		droitComm.setMotifFin(null);
		droitComm.setPart(new Fraction(1, 2));
		droitComm.setRegime(GenrePropriete.COMMUNE);
		droitComm.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2010, 4, 23), "Achat", new IdentifiantAffaireRF(6, 2010, 120, 3)));

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
		eigentumAnteil.setVersionID("1");
		eigentumAnteil.setGemeinschaft(gemeinschaft);
		eigentumAnteil.setBelastetesGrundstueckIDREF("ae93920bc34");
		eigentumAnteil.setQuote(new Quote(1L, 2L, null, null));
		eigentumAnteil.setPersonEigentumsForm(PersonEigentumsform.GESAMTEIGENTUM);

		assertTrue(DroitRFHelper.dataEquals(droitComm, eigentumAnteil));
	}

	@Test
	public void testDataEqualsDroitProprieteImmeuble() throws Exception {

		final ImmeubleBeneficiaireRF beneficiaire1 = new ImmeubleBeneficiaireRF();
		beneficiaire1.setIdRF("5848cd8483");

		final ImmeubleRF immeuble2 = new BienFondsRF();
		immeuble2.setIdRF("ae93920bc34");

		final DroitProprieteImmeubleRF droitImm = new DroitProprieteImmeubleRF();
		droitImm.setMasterIdRF("029382719");
		droitImm.setVersionIdRF("1");
		droitImm.setAyantDroit(beneficiaire1);
		droitImm.setImmeuble(immeuble2);
		droitImm.setDateDebut(RegDate.get(2015, 3, 27));
		droitImm.setDateFin(null);
		droitImm.setMotifDebut(null);
		droitImm.setDateDebutMetier(RegDate.get(2015, 2, 10));
		droitImm.setMotifDebut("Constitution de parts de copropriété");
		droitImm.setMotifFin(null);
		droitImm.setPart(new Fraction(1, 14));
		droitImm.setRegime(GenrePropriete.COPROPRIETE);
		droitImm.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2015,2,10), "Constitution de parts de copropriété", new IdentifiantAffaireRF(6, 2015, 3400, 1)));

		final Rechtsgrund recht = new Rechtsgrund();
		recht.setBelegDatum(RegDate.get(2015, 2, 10));
		recht.setAmtNummer(6);
		recht.setBelegJahr(2015);
		recht.setBelegNummer(3400);
		recht.setBelegNummerIndex(1);
		recht.setRechtsgrundCode(new CapiCode(null, "Constitution de parts de copropriété"));

		final GrundstueckEigentumAnteil eigentumAnteil = new GrundstueckEigentumAnteil();
		eigentumAnteil.setMasterID("029382719");
		eigentumAnteil.setVersionID("1");
		eigentumAnteil.setBerechtigtesGrundstueckIDREF("5848cd8483");
		eigentumAnteil.setBelastetesGrundstueckIDREF("ae93920bc34");
		eigentumAnteil.setQuote(new Quote(1L, 14L, null, null));
		eigentumAnteil.setGrundstueckEigentumsForm(GrundstueckEigentumsform.MITEIGENTUM);
		eigentumAnteil.getRechtsgruende().add(recht);

		assertTrue(DroitRFHelper.dataEquals(droitImm, eigentumAnteil));
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

	/**
	 * [SIFISC-22288] Ce test vérifie que les numéros d'affaire sont bien extraits, en fonction des divers cas possibles.
	 */
	@Test
	public void testGetAffaire() throws Exception {

		// cas de la nullité (je ne vise personne)
		assertNull(DroitRFHelper.getAffaire(null));

		// cas des données structurées
		final Rechtsgrund recht1 = new Rechtsgrund();
		recht1.setAmtNummer(5);
		recht1.setBelegJahr(2005);
		recht1.setBelegNummer(223);
		recht1.setBelegNummerIndex(3);

		final IdentifiantAffaireRF affaire1 = DroitRFHelper.getAffaire(recht1);
		assertNotNull(affaire1);
		assertEquals(5, affaire1.getNumeroOffice());
		assertEquals("2005/223/3", affaire1.getNumeroAffaire());

		// cas des données en texte libre
		final Rechtsgrund recht2 = new Rechtsgrund();
		recht2.setAmtNummer(5);
		recht2.setBelegAlt("2005/223/3");

		final IdentifiantAffaireRF affaire2 = DroitRFHelper.getAffaire(recht2);
		assertNotNull(affaire2);
		assertEquals(5, affaire2.getNumeroOffice());
		assertEquals("2005/223/3", affaire2.getNumeroAffaire());
	}
}
