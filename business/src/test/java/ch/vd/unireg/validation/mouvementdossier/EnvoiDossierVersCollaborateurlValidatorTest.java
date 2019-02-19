package ch.vd.unireg.validation.mouvementdossier;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.mouvement.EnvoiDossierVersCollaborateur;
import ch.vd.unireg.validation.AbstractValidatorTest;

public class EnvoiDossierVersCollaborateurlValidatorTest extends AbstractValidatorTest<EnvoiDossierVersCollaborateur> {

	@Override
	protected String getValidatorBeanName() {
		return "envoiDossierVersCollaborateurValidator";
	}

	@Test
	public void testEnvoiDossierVersCollaborateurAnnuleeVisaDestinataire() throws Exception {
		final EnvoiDossierVersCollaborateur envoiDossierVersCollaborateur = new EnvoiDossierVersCollaborateur();
		envoiDossierVersCollaborateur.setAnnule(true);
		envoiDossierVersCollaborateur.setVisaDestinataire(null);

		final ValidationResults vr = validate(envoiDossierVersCollaborateur);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}

	@Test
	public void testEnvoiDossierVersCollaborateurNonAnnuleeSansVisaDestinataire() throws Exception {
		final EnvoiDossierVersCollaborateur envoiDossierVersCollaborateur = new EnvoiDossierVersCollaborateur();
		envoiDossierVersCollaborateur.setAnnule(false);
		envoiDossierVersCollaborateur.setVisaDestinataire(null);

		final ValidationResults vr = validate(envoiDossierVersCollaborateur);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(1, vr.errorsCount());

		final String error = vr.getErrors().get(0);
		Assert.assertNotNull(error);
		Assert.assertEquals("Le visaDestinataire de l'envoi dossier vers collaborateur ne doit pas être vide.", error);
	}


	@Test
	public void testEnvoiDossierVersCollaborateurNonAnnuleeAvecVisaCollaborateurCorrect() throws Exception {
		final EnvoiDossierVersCollaborateur envoiDossierVersCollaborateur = new EnvoiDossierVersCollaborateur();
		envoiDossierVersCollaborateur.setAnnule(false);
		envoiDossierVersCollaborateur.setVisaDestinataire("test");

		final ValidationResults vr = validate(envoiDossierVersCollaborateur);
		Assert.assertNotNull(vr);
		Assert.assertEquals(0, vr.warningsCount());
		Assert.assertEquals(0, vr.errorsCount());
	}
}
