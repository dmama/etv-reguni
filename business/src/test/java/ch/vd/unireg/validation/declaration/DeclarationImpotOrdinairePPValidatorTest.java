package ch.vd.unireg.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
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

	@Test
	@Transactional(rollbackFor = Throwable.class)
	// Les dates de début et de fin doivent être renseignées
	public void testValidateDateDebutFinNotNull() {

		final DeclarationImpotOrdinairePP di = newDeclarationInstance();
		di.setNumero(1);
		final int annee = DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE;
		final PeriodeFiscale pf = new PeriodeFiscale();
		pf.setAnnee(annee);
		di.setPeriode(pf);

		final ModeleDocument modele = new ModeleDocument();
		di.setModeleDocument(modele);
		di.setCodeSegment(2);

		di.setDateDebut(null);
		di.setDateFin(null);
		final ValidationResults validationResults = validate(di);
		assertTrue(validationResults.hasErrors());
		assertEquals("La DI DeclarationImpotOrdinairePP (? - ?) possède une date de début nulle", validationResults.getErrorsList().get(0).getMessage());
		assertEquals("La DI DeclarationImpotOrdinairePP (? - ?) possède une date de fin nulle", validationResults.getErrorsList().get(1).getMessage());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	// La période de validité doit être renseignée
	public void testValidatePeriodeNotNull() {

		final DeclarationImpotOrdinairePP di = newDeclarationInstance();
		di.setNumero(1);
		final int annee = DeclarationImpotOrdinairePP.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE;
		di.setDateDebut(RegDate.get(annee, 1, 1));
		di.setDateFin(RegDate.get(annee, 12, 31));

		final ModeleDocument modele = new ModeleDocument();
		di.setModeleDocument(modele);
		di.setCodeSegment(2);

		di.setPeriode(null);
		final ValidationResults validationResults = validate(di);
		assertTrue(validationResults.hasErrors());
		assertEquals("La période ne peut pas être nulle.", validationResults.getErrorsList().get(0).getMessage());
	}
}