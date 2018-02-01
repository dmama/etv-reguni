package ch.vd.unireg.validation.fors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class ForFiscalValidatorTest extends AbstractValidatorTest<ForFiscal> {

	@Override
	protected String getValidatorBeanName() {
		return "concreteTestForFiscalValidator";
	}

	@Test
	public void testCommunePrincipale() throws Exception {

		final MockCommune communesPrincipales[] = { MockCommune.LAbbaye, MockCommune.LeChenit, MockCommune.LeLieu };
		for (Commune commune : communesPrincipales) {
			final ForFiscal ff = new ForFiscalSecondaire(RegDate.get(), MotifFor.ACHAT_IMMOBILIER, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			final ValidationResults vr = validate(ff);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas", ff, commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	public void testDateFinValiditeCommune() throws Exception {
		final Commune commune = MockCommune.Malapalud;
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String expectedMsg = String.format("Le for fiscal %s a une période de validité qui dépasse la période de validité de la commune %s (%d) depuis le 01.01.2009",
			                                         ffp, commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, RegDate.get(2008, 12, 31), MotifFor.FUSION_COMMUNES, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	public void testDateFinValiditeCommuneDansFutur() throws Exception {
		final Commune commune = MockCommune.Mirage;
		Assert.assertTrue(commune.getDateFinValidite().isAfterOrEqual(RegDate.get()));
		{
			// le for est encore ouvert à droite : en théorie, puisque la commune a une date de fin, cela devrait donner une erreur, mais en fait non, car cette date est dans le futur
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, RegDate.get(2010, 12, 31), MotifFor.DEPART_HS, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	public void testDateDebutValiditeCommune() throws Exception {
		final Commune commune = MockCommune.ValDeTravers;
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String expectedMsg = String.format("Le for fiscal %s a une période de validité qui dépasse la période de validité de la commune %s (%d) entre le 01.07.2008 et le 31.12.2008",
			                                         ffp, commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2009, 1, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	public void testCommuneVaudoiseOuHorsCanton() throws Exception {
		{
			final Commune commune = MockCommune.Lausanne;
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s montre une incohérence entre son type d'autorité fiscale %s et la commune vaudoise %s (%d) depuis le 01.07.2008", ffp, ffp.getTypeAutoriteFiscale(), commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final Commune commune = MockCommune.Neuchatel;
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ARRIVEE_HS, null, null, commune.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s montre une incohérence entre son type d'autorité fiscale %s et la commune non-vaudoise %s (%d) depuis le 01.07.2008", ffp, ffp.getTypeAutoriteFiscale(), commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	public void testPaysHS() throws Exception {
		{
			final Commune commune = MockCommune.Lausanne;
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ACHAT_IMMOBILIER, null, null, commune.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s est sur un pays (%d) inconnu dans l'infrastructure à sa date d'entrée en vigueur", ffp, ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockPays.Suisse.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s devrait être sur un canton (VD ou autre) suisse", ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockPays.Gibraltar.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s est sur un pays (%s, %d) qui n'est pas un état souverain, mais un territoire", ffp, MockPays.Gibraltar.getNomCourt(), MockPays.Gibraltar.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2008, 7, 1), MotifFor.ACHAT_IMMOBILIER, null, null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	public void testDateDebutDansLeFutur() throws Exception {
		final RegDate aujourdhui = RegDate.get();
		final RegDate demain = aujourdhui.addDays(1);
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(aujourdhui, MotifFor.ARRIVEE_HS, null, null, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(demain, MotifFor.ARRIVEE_HS, null, null, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s possède une date de début dans le futur", ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	public void testDateFinDansLeFutur() throws Exception {
		final RegDate debut = date(2010, 1, 1);
		final RegDate aujourdhui = RegDate.get();
		final RegDate demain = aujourdhui.addDays(1);
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(debut, MotifFor.ARRIVEE_HS, aujourdhui, MotifFor.DEPART_HS, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(debut, MotifFor.ARRIVEE_HS, demain, MotifFor.DEPART_HS, MockCommune.Cossonay.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s possède une date de fin dans le futur", ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	public void testCommuneAvecChangementCantonFutur() throws Exception {
		final RegDate debut = date(2010, 1, 1);
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(debut, null, null, null, MockCommune.MoutierBE.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	public void testCommuneAvecChangementCantonPasse() throws Exception {
		final RegDate debut = date(2010, 1, 1);
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(debut, null, null, null, MockCommune.TransfugeZH.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}
}
