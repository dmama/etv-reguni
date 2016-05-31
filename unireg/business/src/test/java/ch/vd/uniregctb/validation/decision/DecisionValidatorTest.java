package ch.vd.uniregctb.validation.decision;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class DecisionValidatorTest extends AbstractValidatorTest<DecisionAci> {

	@Override
	protected String getValidatorBeanName() {
		return "decisionValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommunePrincipale() throws Exception {

		final MockCommune communesPrincipales[] = { MockCommune.LAbbaye, MockCommune.LeChenit, MockCommune.LeLieu };
		for (Commune commune : communesPrincipales) {
			final DecisionAci d = new DecisionAci(null,date(2012,2,12),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s ne peut pas être sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas", d, commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateFinValiditeCommune() throws Exception {
		final Commune commune = MockCommune.Malapalud;
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String debutValiditeCommune = commune.getDateDebutValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateDebutValidite());
			final String finValiditeCommune = commune.getDateFinValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateFinValidite());
			final String expectedMsg = String.format("La décision ACI %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
			                                         d, commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),date(2008,12,31), commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateFinValiditeCommuneDansFutur() throws Exception {
		final Commune commune = MockCommune.Mirage;
		Assert.assertTrue(commune.getDateFinValidite().isAfterOrEqual(RegDate.get()));
		{
			// le for est encore ouvert à droite : en théorie, puisque la commune a une date de fin, cela devrait donner une erreur, mais en fait non, car cette date est dans le futur
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),date(2010,12,31), commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDebutValiditeCommune() throws Exception {
		final Commune commune = MockCommune.ValDeTravers;
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_HC,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String debutValiditeCommune = commune.getDateDebutValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateDebutValidite());
			final String finValiditeCommune = commune.getDateFinValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateFinValidite());
			final String expectedMsg = String.format("La décision ACI %s a une période de validité qui dépasse la période de validité de sa commune %s (%d) (%s - %s)",
			                                         d, commune.getNomOfficiel(), commune.getNoOFS(), debutValiditeCommune, finValiditeCommune);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2009,1,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_HC,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommuneVaudoiseOuHorsCanton() throws Exception {
		{
			final Commune commune = MockCommune.Lausanne;
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_HC,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s montre une incohérence entre le type d'autorité fiscale %s et la commune vaudoise %s (%d)", d, d.getTypeAutoriteFiscale(), commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final Commune commune = MockCommune.Neuchatel;
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s montre une incohérence entre le type d'autorité fiscale %s et la commune non-vaudoise %s (%d)", d, d.getTypeAutoriteFiscale(), commune.getNomOfficiel(), commune.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPaysHS() throws Exception {
		{
			final Commune commune = MockCommune.Lausanne;
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null, commune.getNoOFS(),TypeAutoriteFiscale.PAYS_HS,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s est sur un pays (%d) inconnu dans l'infrastructure à sa date d'entrée en vigueur", d, d.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null,  MockPays.Suisse.getNoOFS(),TypeAutoriteFiscale.PAYS_HS,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s devrait être sur un canton (VD ou autre) suisse", d);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null,  MockPays.Gibraltar.getNoOFS(),TypeAutoriteFiscale.PAYS_HS,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s est sur un pays (%s, %d) qui n'est pas un état souverain, mais un territoire", d, MockPays.Gibraltar.getNomCourt(), MockPays.Gibraltar.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final DecisionAci d = new DecisionAci(null,date(2008,7,1),null,  MockPays.France.getNoOFS(),TypeAutoriteFiscale.PAYS_HS,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDebutDansLeFutur() throws Exception {
		final RegDate aujourdhui = RegDate.get();
		final RegDate demain = aujourdhui.addDays(1);
		{
			final DecisionAci d = new DecisionAci(null,aujourdhui,null,  MockCommune.Cossonay.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final DecisionAci d = new DecisionAci(null,demain,null,  MockCommune.Cossonay.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s possède une date de début dans le futur", d);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateFinDansLeFutur() throws Exception {
		final RegDate debut = date(2010, 1, 1);
		final RegDate aujourdhui = RegDate.get();
		final RegDate demain = aujourdhui.addDays(1);
		{
			final DecisionAci d = new DecisionAci(null,debut,aujourdhui,  MockCommune.Cossonay.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final DecisionAci d = new DecisionAci(null,debut,demain,  MockCommune.Cossonay.getNoOFS(),TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,null);
			final ValidationResults vr = validate(d);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La décision ACI %s possède une date de fin dans le futur", d);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}
}
