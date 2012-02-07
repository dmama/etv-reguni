package ch.vd.uniregctb.validation.declaration;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.ordinaire.DeclarationImpotService;
import ch.vd.uniregctb.validation.AbstractValidatorTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class DeclarationImpotOrdinaireValidatorTest extends AbstractValidatorTest<DeclarationImpotOrdinaire> {

	@Override
	protected String getValidatorBeanName() {
		return "declarationImpotOrdinaireValidator";
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateDeclarationAnnulee() {

		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();

		// Déclaration invalide (numéro de séquence nul) mais annulée => pas d'erreur
		{
			di.setNumero(null);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}

		// Déclaration valide et annulée => pas d'erreur
		{
			di.setPeriode(new PeriodeFiscale());
			di.setModeleDocument(new ModeleDocument());
			di.setNumero(1);
			di.setAnnule(true);
			assertFalse(validate(di).hasErrors());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testValidateCodeSegmentation() {
		final DeclarationImpotOrdinaire di = new DeclarationImpotOrdinaire();
		di.setNumero(1);
		di.setAnnule(false);

		int annee = DeclarationImpotOrdinaire.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE - 1;
		di.setModeleDocument(new ModeleDocument());
		di.setDateDebut(RegDate.get(annee, 1, 1));
		di.setDateFin(RegDate.get(annee, 12, 31));
		di.setCodeSegment(null);
		PeriodeFiscale pf = new PeriodeFiscale();
		di.setPeriode(pf);
		pf.setAnnee(annee);
		assertFalse(validate(di).hasErrors());
		annee = DeclarationImpotOrdinaire.PREMIERE_ANNEE_RETOUR_ELECTRONIQUE;
		di.setDateDebut(RegDate.get(annee, 1, 1));
		di.setDateFin(RegDate.get(annee, 12, 31));
		pf.setAnnee(annee);
		assertTrue(validate(di).hasErrors());
		di.setCodeSegment(2);
		assertFalse(validate(di).hasErrors());
	}

}