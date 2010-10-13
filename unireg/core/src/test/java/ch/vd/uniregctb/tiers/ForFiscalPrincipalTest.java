package ch.vd.uniregctb.tiers;

import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForFiscalPrincipalTest extends WithoutSpringTest {

	@Test
	public void testValidateForAnnule() {

		final ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();

		// For invalide (mode d'imposition incorrect) mais annulé => pas d'erreur
		{
			forFiscal.setModeImposition(null);
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For valide et annulé => pas d'erreur
		{
			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setDateDebut(RegDate.get(2000, 1, 1));
			forFiscal.setAnnule(true);
			assertFalse(forFiscal.validate().hasErrors());
		}
	}

	@Test
	public void testValidateForDiplomateSuisse() {

		final ForFiscalPrincipal forFiscal = new ForFiscalPrincipal();
		forFiscal.setMotifRattachement(MotifRattachement.DIPLOMATE_SUISSE);
		forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		forFiscal.setModeImposition(ModeImposition.ORDINAIRE);
		forFiscal.setDateDebut(RegDate.get(2000, 1, 1));

		// For diplomate et motifs début/fin activité diplomatique => valide
		{
			// for ouvert
			forFiscal.setMotifOuverture(MotifFor.DEBUT_ACTIVITE_DIPLOMATIQUE);
			assertFalse(forFiscal.validate().hasErrors());

			// for fermé
			forFiscal.setDateFin(RegDate.get(2005,12,31));
			forFiscal.setMotifFermeture(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);
			assertFalse(forFiscal.validate().hasErrors());
		}

		// For non-diplomate et motifs début/fin activité diplomatique => invalide
		{
			// for ouvert
			forFiscal.setDateFin(null);
			forFiscal.setMotifFermeture(null);

			forFiscal.setMotifRattachement(MotifRattachement.DOMICILE);
			ValidationResults results = forFiscal.validate();
			assertTrue(results.hasErrors());

			List<String> errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(1, errors.size());
			assertEquals("Le motif de début d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(0));

			// for fermé
			forFiscal.setDateFin(RegDate.get(2005,12,31));
			forFiscal.setMotifFermeture(MotifFor.FIN_ACTIVITE_DIPLOMATIQUE);

			results = forFiscal.validate();
			assertTrue(results.hasErrors());

			errors = results.getErrors();
			assertNotNull(errors);
			assertEquals(2, errors.size());
			assertEquals("Le motif de début d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(0));
			assertEquals("Le motif de fin d'activité diplomatique est uniquement applicable aux diplomates suisses basés à l'étranger", errors.get(1));
		}
	}

}