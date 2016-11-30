package ch.vd.uniregctb.role;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class RolePopulationPPExtractorTest extends WithoutSpringTest {

	private RolePopulationPPExtractor extractor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.extractor = new RolePopulationPPExtractor((ctb, date) -> false);
	}

	@Test
	public void testVaudoisSedentaireSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pp));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testHorsCantonSedentaireSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testHorsSuisseSedentaireSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testVaudoisSedentaireAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pp));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testHorsCantonSedentaireAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testHorsSuisseSedentaireAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testVaudoisSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pp));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testHorsCantonSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2007) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testHorsSuisseSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testVaudoisDecedeAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 31), MotifFor.VEUVAGE_DECES, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pp));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			if (annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testHorsCantonDecedeAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 31), MotifFor.VEUVAGE_DECES, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testHorsSuisseDecedeAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.DEPART_HS, date(2008, 5, 31), MotifFor.VEUVAGE_DECES, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDepartHSDepuisVDSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2010) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDepartHSDepuisHCSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
		}
	}

	@Test
	public void testDepartHSDepuisVDAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else if (annee >= 2010 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDepartHSDepuisHCAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDepartHCDepuisVDSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HC, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HC, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDepartHCDepuisVDAvecForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else if (annee >= 2010 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testDemenagementVaudoisSansForSecondaire() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2010, 6, 13), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Bussigny.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else if (annee >= 2010) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Bussigny.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testMariage() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testSeparation() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 5, 18), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null, null, MockCommune.Echallens.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testSedentaireHCAvecPlusieursForsSecondaires() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), null, null, null, MockCommune.Bern.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2002, 6, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 8, 16), MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 5, 22), MotifFor.ACHAT_IMMOBILIER, date(2012, 9, 19), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2002 && annee <= 2006) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Leysin.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else if (annee >= 2007 && annee <= 2011) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Morges.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}

	@Test
	public void testSedentaireHSAvecPlusieursForsSecondaires() throws Exception {
		final PersonnePhysique pp = new PersonnePhysique();
		pp.addForFiscal(new ForFiscalPrincipalPP(date(2000, 1, 1), null, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2002, 6, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 8, 16), MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));
		pp.addForFiscal(new ForFiscalSecondaire(date(2005, 5, 22), MotifFor.ACHAT_IMMOBILIER, date(2012, 9, 19), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2002 && annee <= 2006) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Leysin.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else if (annee >= 2007 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Morges.getNoOFS(), extractor.getCommunePourRoles(annee, pp));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pp));
			}
		}
	}
}
