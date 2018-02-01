package ch.vd.unireg.role;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipalPM;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

public class RolePopulationPMExtractorTest extends WithoutSpringTest {

	private RolePopulationPMExtractor extractor;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.extractor = new RolePopulationPMExtractor();
	}

	private static ForFiscalPrincipalPM newForFiscalPrincipal(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement, GenreImpot genreImpot) {
		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM(dateOuverture,
		                                                          motifOuverture,
		                                                          dateFermeture,
		                                                          motifFermeture,
		                                                          commune.getNoOFS(),
		                                                          commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC,
		                                                          motifRattachement);
		ffp.setGenreImpot(genreImpot);
		return ffp;
	}

	private static ForFiscalPrincipalPM newForFiscalPrincipal(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement) {
		return newForFiscalPrincipal(dateOuverture, motifOuverture, dateFermeture, motifFermeture, commune, motifRattachement, GenreImpot.BENEFICE_CAPITAL);
	}

	private static ForFiscalPrincipalPM newForFiscalPrincipal(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockPays pays, MotifRattachement motifRattachement, GenreImpot genreImpot) {
		final ForFiscalPrincipalPM ffp = new ForFiscalPrincipalPM(dateOuverture,
		                                                          motifOuverture,
		                                                          dateFermeture,
		                                                          motifFermeture,
		                                                          pays.getNoOFS(),
		                                                          TypeAutoriteFiscale.PAYS_HS,
		                                                          motifRattachement);
		ffp.setGenreImpot(genreImpot);
		return ffp;
	}

	private static ForFiscalPrincipalPM newForFiscalPrincipal(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockPays pays, MotifRattachement motifRattachement) {
		return newForFiscalPrincipal(dateOuverture, motifOuverture, dateFermeture, motifFermeture, pays, motifRattachement, GenreImpot.BENEFICE_CAPITAL);
	}

	private static ForFiscalSecondaire newForFiscalSecondaire(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockCommune commune, MotifRattachement motifRattachement, GenreImpot genreImpot) {
		final ForFiscalSecondaire ffs = new ForFiscalSecondaire(dateOuverture,
		                                                        motifOuverture,
		                                                        dateFermeture,
		                                                        motifFermeture,
		                                                        commune.getNoOFS(),
		                                                        commune.isVaudoise() ? TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD : TypeAutoriteFiscale.COMMUNE_HC,
		                                                        motifRattachement);
		ffs.setGenreImpot(genreImpot);
		return ffs;
	}

	private static ForFiscalSecondaire newForFiscalSecondaire(RegDate dateOuverture, MotifFor motifOuverture, RegDate dateFermeture, MotifFor motifFermeture, MockCommune mockCommune, MotifRattachement motifRattachement) {
		return newForFiscalSecondaire(dateOuverture, motifOuverture, dateFermeture, motifFermeture, mockCommune, motifRattachement, GenreImpot.BENEFICE_CAPITAL);
	}

	@Test
	public void testVaudoisSedentaireSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens, MotifRattachement.DOMICILE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pm));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testHorsCantonSedentaireSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testHorsSuisseSedentaireSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testVaudoisSedentaireAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pm));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testHorsCantonSedentaireAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testHorsSuisseSedentaireAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testVaudoisSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pm));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testHorsCantonSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, null, null, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2007) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testHorsSuisseSedentaireAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testVaudoisRadieAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 31), MotifFor.FIN_EXPLOITATION, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		Assert.assertNull(extractor.getCommunePourRoles(1999, pm));
		for (int annee = 2000 ; annee <= RegDate.get().year() ; ++ annee) {
			if (annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testHorsCantonRadieAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2008, 5, 31), MotifFor.FIN_EXPLOITATION, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testHorsSuisseRadieAvecForSecondaireFermeEnMilieuAnnee() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.DEPART_HS, date(2008, 5, 31), MotifFor.FIN_EXPLOITATION, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2008, 5, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2008) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDepartHSDepuisVDSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2010) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDepartHSDepuisHCSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
		}
	}

	@Test
	public void testDepartHSDepuisVDAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else if (annee >= 2010 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDepartHSDepuisHCAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2005 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDepartHCDepuisVDSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HC, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HC, null, null, MockCommune.Bern, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDepartHCDepuisVDAvecForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEPART_HS, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEPART_HS, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 7, 23), MotifFor.ACHAT_IMMOBILIER, date(2012, 7, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Prilly, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else if (annee >= 2010 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Prilly.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testDemenagementVaudoisSansForSecondaire() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), MotifFor.ARRIVEE_HS, date(2010, 6, 12), MotifFor.DEMENAGEMENT_VD, MockCommune.Echallens, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalPrincipal(date(2010, 6, 13), MotifFor.DEMENAGEMENT_VD, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2000 && annee <= 2009) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Echallens.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else if (annee >= 2010) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Bussigny.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testSedentaireHCAvecPlusieursForsSecondaires() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), null, null, null, MockCommune.Bern, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2002, 6, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 8, 16), MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin, MotifRattachement.IMMEUBLE_PRIVE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 5, 22), MotifFor.ACHAT_IMMOBILIER, date(2012, 9, 19), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2002 && annee <= 2006) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Leysin.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else if (annee >= 2007 && annee <= 2011) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Morges.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	@Test
	public void testSedentaireHSAvecPlusieursForsSecondaires() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), null, null, null, MockPays.France, MotifRattachement.DOMICILE));
		pm.addForFiscal(newForFiscalSecondaire(date(2002, 6, 12), MotifFor.ACHAT_IMMOBILIER, date(2007, 8, 16), MotifFor.VENTE_IMMOBILIER, MockCommune.Leysin, MotifRattachement.IMMEUBLE_PRIVE));
		pm.addForFiscal(newForFiscalSecondaire(date(2005, 5, 22), MotifFor.ACHAT_IMMOBILIER, date(2012, 9, 19), MotifFor.VENTE_IMMOBILIER, MockCommune.Morges, MotifRattachement.IMMEUBLE_PRIVE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			if (annee >= 2002 && annee <= 2006) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Leysin.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else if (annee >= 2007 && annee <= 2012) {
				Assert.assertEquals(String.valueOf(annee), (Integer) MockCommune.Morges.getNoOFS(), extractor.getCommunePourRoles(annee, pm));
			}
			else {
				Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
			}
		}
	}

	/**
	 * SC / SNC
	 */
	@Test
	public void testSocieteVaudoiseDePersonnes() throws Exception {
		final Entreprise pm = new Entreprise();
		pm.addForFiscal(newForFiscalPrincipal(date(2000, 1, 1), null, null, null, MockCommune.Bussigny, MotifRattachement.DOMICILE, GenreImpot.REVENU_FORTUNE));

		for (int annee = 1998 ; annee < RegDate.get().year() ; ++ annee) {
			Assert.assertNull(String.valueOf(annee), extractor.getCommunePourRoles(annee, pm));
		}
	}
}
