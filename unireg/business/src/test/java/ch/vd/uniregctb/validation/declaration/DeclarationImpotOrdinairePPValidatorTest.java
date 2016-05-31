package ch.vd.uniregctb.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeclarationImpotOrdinairePPValidatorTest extends DeclarationImpotOrdinaireValidatorTest<DeclarationImpotOrdinairePP> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotOrdinairePPValidator";
	}

	@Override
	protected DeclarationImpotOrdinairePP newDeclarationInstance() {
		return new DeclarationImpotOrdinairePP();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCodeSegmentation() {
		final DeclarationImpotOrdinairePP di = newDeclarationInstance();
		di.setNumero(1);
		di.setAnnule(false);
		di.setCodeSegment(null);
		di.setModeleDocument(new ModeleDocument());

		final PersonnePhysique pp = addNonHabitant("Albert", "Capitastamus", null, Sexe.MASCULIN);
		di.setTiers(pp);

		{
			final int annee = DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE - 1;
			di.setDateDebut(RegDate.get(annee, 1, 1));
			di.setDateFin(RegDate.get(annee, 12, 31));

			final PeriodeFiscale pf = new PeriodeFiscale();
			di.setPeriode(pf);
			pf.setAnnee(annee);
			assertFalse(validate(di).hasErrors());
		}
		{
			final int annee = DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE;
			di.setDateDebut(RegDate.get(annee, 1, 1));
			di.setDateFin(RegDate.get(annee, 12, 31));

			final PeriodeFiscale pf = new PeriodeFiscale();
			di.setPeriode(pf);
			pf.setAnnee(annee);

			assertTrue(validate(di).hasErrors());
			di.setCodeSegment(2);
			assertFalse(validate(di).hasErrors());
		}
	}
}