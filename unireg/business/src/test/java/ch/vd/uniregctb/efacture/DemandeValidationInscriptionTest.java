package ch.vd.uniregctb.efacture;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static ch.vd.uniregctb.efacture.TypeRefusEFacture.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DemandeValidationInscriptionTest extends WithoutSpringTest {

	DemandeValidationInscription demande;

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

		//noinspection NullableProblems
		demande = nouvelleDemande().setEmail(null).build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = nouvelleDemande().setEmail("").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = nouvelleDemande().setEmail("toto").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = nouvelleDemande().setEmail("toto@").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = nouvelleDemande().setEmail("toto@tutu").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);

		demande = nouvelleDemande().setEmail("toto@@gmail.com").build();
		assertPerformBasicValidationFailWith(EMAIL_INVALIDE);
	}

	private DemandeValidationInscriptionBuilderForUnitTests nouvelleDemande() {
		return new DemandeValidationInscriptionBuilderForUnitTests();
	}

	private void assertPerformBasicValidationOK() {
		assertNull(demande.performBasicValidation());
	}

	private void assertPerformBasicValidationFailWith(TypeRefusEFacture typeRefusEFacture) {
		assertEquals(typeRefusEFacture, demande.performBasicValidation());
	}

}
