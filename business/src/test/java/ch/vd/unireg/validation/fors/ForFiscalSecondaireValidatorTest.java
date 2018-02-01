package ch.vd.unireg.validation.fors;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.validation.AbstractValidatorTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ForFiscalSecondaireValidatorTest extends AbstractValidatorTest<ForFiscalSecondaire> {

	@Override
	protected String getValidatorBeanName() {
		return "forFiscalSecondaireValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateForAnnule() {

		final ForFiscalSecondaire forFiscal = new ForFiscalSecondaire();

		// For invalide (type d'autorité fiscale incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(validate(forFiscal).hasErrors());
		}
	}

	@Test
	public void testPresenceDateFermetureSiMotifFermeturePresent() throws Exception {

		final ForFiscalSecondaire ffp = new ForFiscalSecondaire();
		ffp.setMotifRattachement(MotifRattachement.IMMEUBLE_PRIVE);
		ffp.setGenreImpot(GenreImpot.REVENU_FORTUNE);
		ffp.setDateDebut(RegDate.get(2000, 1, 1));
		ffp.setMotifOuverture(MotifFor.ARRIVEE_HS);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		ffp.setNumeroOfsAutoriteFiscale(MockCommune.Aigle.getNoOFS());
		ffp.setMotifFermeture(MotifFor.DEPART_HS);
		{
			ffp.setDateFin(null);
			final ValidationResults vr = validate(ffp);
			Assert.assertTrue(vr.hasErrors());
			final List<String> errors = vr.getErrors();
			assertEquals(1, errors.size());
			assertEquals("Une date de fermeture doit être indiquée si un motif de fermeture l'est.", errors.get(0));
		}
		{
			ffp.setDateFin(date(2005, 5, 23));
			final ValidationResults vr = validate(ffp);
			Assert.assertFalse(vr.hasErrors());
		}
	}
}