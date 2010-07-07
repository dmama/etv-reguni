package ch.vd.uniregctb.tiers;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ForFiscalTest extends WithoutSpringTest {

	@Test
	public void testIsValid() {

		final ForFiscal forFiscal = new ForFiscalPrincipal();
		forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
		forFiscal.setDateFin(RegDate.get(2009, 12, 31));

		forFiscal.setAnnule(false);
		assertTrue(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));

		forFiscal.setAnnule(true);
		assertFalse(forFiscal.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(forFiscal.isValidAt(RegDate.get(2060, 1, 1)));
	}
	
	/**
	 * Test de non régression concernant le cas JIRA UNIREG-585.<br>
	 * </br>
	 * Pour un rattachement personnel de type domicile, dans un autre canton ou à l'étranger, 
	 * les seuls modes d'imposition possibles sont normalement uniquement "ordinaire", "source" ou "mixte 137 al1". 
	 * Voir spéc. "enregistrer un nouveau tiers" 3.1.9.
	 * 
	 */
	@Test
	public void testJiraUNIREG585(){
		ForFiscalPrincipal ffp = new ForFiscalPrincipal();
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.PAYS_HS);
		ffp.setMotifRattachement(MotifRattachement.DOMICILE);
		assertTrue(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.INDIGENT);
		assertTrue(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.MIXTE_137_2);
		assertTrue(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.DEPENSE);
		assertTrue(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.ORDINAIRE);
		assertFalse(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.MIXTE_137_1);
		assertFalse(hasForFiscalPrincipalErrors(ffp));
		ffp.setModeImposition(ModeImposition.SOURCE);
		assertFalse(hasForFiscalPrincipalErrors(ffp));
	}

	private boolean hasForFiscalPrincipalErrors(ForFiscalPrincipal ffp) {
		List<String> listErr = ffp.validate(true).getErrors();
		for (String err : listErr) {
			for (String code : ForFiscalPrincipal.VALIDATION_ERROR_CODES) {
				if (err.indexOf(code) >= 0) {
					return true;
				}
			}
		}
		return false;
	}
}
