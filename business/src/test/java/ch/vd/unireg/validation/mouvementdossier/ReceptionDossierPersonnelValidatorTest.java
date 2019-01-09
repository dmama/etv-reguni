package ch.vd.unireg.validation.mouvementdossier;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.mouvement.ReceptionDossierPersonnel;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class ReceptionDossierPersonnelValidatorTest extends AbstractValidatorTest<ReceptionDossierPersonnel> {

	@Override
	protected String getValidatorBeanName() {
		return "receptionDossierPersonnelValidator";
	}

	@Test
	public void testReceptionDossierAnnuleeSansVisaCollaborateur() throws Exception {
		final ReceptionDossierPersonnel receptionDossierPersonnel = new ReceptionDossierPersonnel();
		receptionDossierPersonnel.setAnnule(true);
		receptionDossierPersonnel.setVisaRecepteur(null);

		final ValidationResults vr = validate(receptionDossierPersonnel);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}

	@Test
	public void testReceptionDossierNonAnnuleeSansVisaCollaborateur() throws Exception {
		final ReceptionDossierPersonnel receptionDossierPersonnel = new ReceptionDossierPersonnel();
		receptionDossierPersonnel.setAnnule(false);
		receptionDossierPersonnel.setVisaRecepteur(null);

		final ValidationResults vr = validate(receptionDossierPersonnel);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(1, vr.errorsCount());

		final String error = vr.getErrors().get(0);
		Assert.assertNotNull(error);
		Assert.assertEquals("Le VisaRecepteur de l'envoi dossier vers collaborateur ne doit pas être vide.", error);
	}


	@Test
	public void testReceptionDossierNonAnnuleeAvecVisaCollaborateurCorrect() throws Exception {
		final ReceptionDossierPersonnel receptionDossierPersonnel = new ReceptionDossierPersonnel();
		receptionDossierPersonnel.setAnnule(false);
		receptionDossierPersonnel.setVisaRecepteur("test");

		final ValidationResults vr = validate(receptionDossierPersonnel);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}
}
