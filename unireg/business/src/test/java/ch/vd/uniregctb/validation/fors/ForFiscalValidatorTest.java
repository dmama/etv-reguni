package ch.vd.uniregctb.validation.fors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

public class ForFiscalValidatorTest extends AbstractValidatorTest<ForFiscal> {

	@Override
	protected String getValidatorBeanName() {
		return "concreteTestForFiscalValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCommunePrincipale() throws Exception {

		final MockCommune communesPrincipales[] = { MockCommune.LAbbaye, MockCommune.LeChenit, MockCommune.LeLieu };
		for (Commune commune : communesPrincipales) {
			final ForFiscal ff = new ForFiscalSecondaire(RegDate.get(), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			final ValidationResults vr = validate(ff);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for fiscal %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas", ff, commune.getNomMinuscule(), commune.getNoOFSEtendu());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateFinValiditeCommune() throws Exception {
		final Commune commune = MockCommune.Malapalud;
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String debutValiditeCommune = commune.getDateDebutValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateDebutValidite());
			final String finValiditeCommune = commune.getDateFinValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateFinValidite());
			final String expectedMsg = String.format("La période de validité du for fiscal %s dépasse la période de validité de la commune %s (%d) à laquelle il est assigné (%s - %s)",
										ffp, commune.getNomMinuscule(), commune.getNoOFSEtendu(), debutValiditeCommune, finValiditeCommune);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), RegDate.get(2008, 12, 31), commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
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
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(0, vr.errorsCount());
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), RegDate.get(2010, 12, 31), commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
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
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.warningsCount());
			Assert.assertEquals(1, vr.errorsCount());

			final String debutValiditeCommune = commune.getDateDebutValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateDebutValidite());
			final String finValiditeCommune = commune.getDateFinValidite() == null ? "?" : RegDateHelper.dateToDisplayString(commune.getDateFinValidite());
			final String expectedMsg = String.format("La période de validité du for fiscal %s dépasse la période de validité de la commune %s (%d) à laquelle il est assigné (%s - %s)",
										ffp, commune.getNomMinuscule(), commune.getNoOFSEtendu(), debutValiditeCommune, finValiditeCommune);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2009, 1, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
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
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Incohérence entre le type d'autorité fiscale %s et la commune vaudoise %s (%d) sur le for %s", ffp.getTypeAutoriteFiscale(), commune.getNomMinuscule(), commune.getNoOFSEtendu(), ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final Commune commune = MockCommune.Neuchatel;
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Incohérence entre le type d'autorité fiscale %s et la commune non-vaudoise %s (%d) sur le for %s", ffp.getTypeAutoriteFiscale(), commune.getNomMinuscule(), commune.getNoOFSEtendu(), ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testPaysHS() throws Exception {
		{
			final Commune commune = MockCommune.Lausanne;
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le pays du for fiscal %s (%d) est inconnu dans l'infrastructure", ffp, ffp.getNumeroOfsAutoriteFiscale());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, MockPays.Suisse.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le for %s devrait être vaudois ou hors-canton", ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, MockPays.Gibraltar.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("Le pays du for fiscal %s (%s, %d) n'est pas un état souverain, mais un territoire", ffp, MockPays.Gibraltar.getNomMinuscule(), MockPays.Gibraltar.getNoOFS());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, MockPays.France.getNoOFS(), TypeAutoriteFiscale.PAYS_HS, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateDebutDansLeFutur() throws Exception {
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(), null, MockCommune.Cossonay.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get().addDays(1), null, MockCommune.Cossonay.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());
			Assert.assertEquals(0, vr.warningsCount());

			final String expectedMsg = String.format("La date de début du for %s est dans le futur", ffp);
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}
}
