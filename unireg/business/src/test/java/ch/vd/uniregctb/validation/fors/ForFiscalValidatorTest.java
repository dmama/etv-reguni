package ch.vd.uniregctb.validation.fors;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.interfaces.model.Commune;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
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
	public void testCommunePrincipale() throws Exception {

		final MockCommune communesPrincipales[] = { MockCommune.LAbbaye, MockCommune.LeChenit, MockCommune.LeLieu };
		for (Commune commune : communesPrincipales) {
			final ForFiscal ff = new ForFiscalSecondaire(RegDate.get(), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.IMMEUBLE_PRIVE);
			final ValidationResults vr = validate(ff);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());

			final String expectedMsg = String.format("Le for fiscal %s ne peut pas être ouvert sur une commune faîtière de fractions de commune (ici %s / OFS %d), une fraction est attendue dans ce cas", ff, commune.getNomMinuscule(), commune.getNoOFSEtendu());
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
	}

	@Test
	public void testDateFinValiditeCommune() throws Exception {
		final Commune commune = MockCommune.Malapalud;
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());

			final String expectedMsg = String.format("La période de validité du for fiscal %s dépasse la période de validité de la commune à laquelle il est assigné [%s]",
										ffp, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite())));
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), RegDate.get(2008, 12, 31), commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
		}
	}

	@Test
	public void testDateDebutValiditeCommune() throws Exception {
		final Commune commune = MockCommune.ValDeTravers;
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2008, 7, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(1, vr.errorsCount());

			final String expectedMsg = String.format("La période de validité du for fiscal %s dépasse la période de validité de la commune à laquelle il est assigné [%s]",
										ffp, DateRangeHelper.toDisplayString(new DateRangeHelper.Range(commune.getDateDebutValidite(), commune.getDateFinValidite())));
			Assert.assertEquals(expectedMsg, vr.getErrors().get(0));
		}
		{
			final ForFiscalPrincipal ffp = new ForFiscalPrincipal(RegDate.get(2009, 1, 1), null, commune.getNoOFSEtendu(), TypeAutoriteFiscale.COMMUNE_HC, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			final ValidationResults vr = validate(ffp);
			Assert.assertNotNull(vr);
			Assert.assertEquals(0, vr.errorsCount());
		}
	}
}
