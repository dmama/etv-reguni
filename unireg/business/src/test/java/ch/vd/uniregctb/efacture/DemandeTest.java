package ch.vd.uniregctb.efacture;

import org.junit.Test;

import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.common.WithoutSpringTest;

import static ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DemandeTest extends WithoutSpringTest {

	Demande demande;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test
	public void testPerformBasicValidation (){

		demande = nouvelleDemande().build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setNoAvs("756.581.724.90.33").build();
		assertPerformBasicValidationOK();

		//noinspection NullableProblems
		demande = nouvelleDemande().setNoAvs(null).build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("XXXXXXXXXXXXX").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("75658172490").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("75658172490330").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("07565817249033").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		demande = nouvelleDemande().setNoAvs("7565817249034").build();
		assertPerformBasicValidationFailWith(NUMERO_AVS_INVALIDE);

		// [SIFISC-7123] la validité de l'adresse email n'est plus verifiée par unireg
		//noinspection NullableProblems
		demande = nouvelleDemande().setEmail(null).build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setEmail("").build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setEmail("toto").build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setEmail("toto@").build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setEmail("toto@tutu").build();
		assertPerformBasicValidationOK();

		demande = nouvelleDemande().setEmail("toto@@gmail.com").build();
		assertPerformBasicValidationOK();
	}

	private DemandeBuilderForUnitTests nouvelleDemande() {
		return new DemandeBuilderForUnitTests();
	}

	private void assertPerformBasicValidationOK() {
		assertNull(demande.performBasicValidation());
	}

	private void assertPerformBasicValidationFailWith(TypeRefusDemande typeRefusEFacture) {
		assertEquals(typeRefusEFacture, demande.performBasicValidation());
	}

}
