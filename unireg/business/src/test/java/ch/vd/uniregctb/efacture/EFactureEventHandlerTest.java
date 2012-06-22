package ch.vd.uniregctb.efacture;


import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeDocument;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;


public class EFactureEventHandlerTest extends WithoutSpringTest {

	private static final long CTB_ID = 123456L;
	private static final String NO_AVS = "1230456078900";
	private static final String EMAIL = "noel.flantier@dgse.fr";
	private static final String DEMANDE_ID = "DEMANDE_123";
	private static final RegDate DEMANDE_DATE = date(2012, 6, 21);
	private static final String ARCHIVAGE_ID = "ARCHIVE_123ABC";


	private EFactureEventHandlerImpl handler;
	private DemandeValidationInscription inscription;
	private EFactureService service;
	private EFactureMessageSender sender;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = new EFactureEventHandlerImpl();
		service = createMock(EFactureService.class);
		handler.seteFactureService(service);
		sender = createMock(EFactureMessageSender.class);
		handler.setSender(sender);
		inscription = createMock(DemandeValidationInscription.class);
		expect(inscription.getCtbId()).andReturn(CTB_ID).anyTimes();
		expect(inscription.getAction()).andReturn(DemandeValidationInscription.Action.INSCRIPTION).anyTimes();
		expect(inscription.getNoAvs()).andReturn(NO_AVS).anyTimes();
		expect(inscription.getDateDemande()).andReturn(DEMANDE_DATE).anyTimes();
		expect(inscription.getEmail()).andReturn(EMAIL).anyTimes();
		expect(inscription.getIdDemande()).andReturn(DEMANDE_ID).anyTimes();
	}

	@Override
	public void onTearDown() throws Exception {
		super.onTearDown();
	}

	@Test
	public void testDemandeInscription_OK() throws Exception {
		expect(inscription.performBasicValidation()).andReturn(null).once();
		expect(service.getDemandeInscriptionEnCoursDeTraitement(CTB_ID)).andReturn(null).once();
		expect(service.identifieContribuablePourInscription(CTB_ID, NO_AVS)).andReturn(null).once();
		expect(service.valideEtatContribuablePourInscription(CTB_ID)).andReturn(true).once();
		expect(service.imprimerDocumentEfacture(CTB_ID, TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, DEMANDE_DATE)).andReturn(ARCHIVAGE_ID).once();
		sender.envoieMiseEnAttenteDemandeInscription(DEMANDE_ID,TypeAttenteEFacture.EN_ATTENTE_SIGNATURE, ARCHIVAGE_ID, false);
		service.updateEmailContribuable(CTB_ID, EMAIL);
		replay(inscription, sender, service);
		handler.handle(inscription);
		verify(inscription, sender, service);
	}

	@Test
	public void testDemandeInscription_OK_MaisEtatIncoherent() throws Exception {
		expect(inscription.performBasicValidation()).andReturn(null).once();
		expect(service.getDemandeInscriptionEnCoursDeTraitement(CTB_ID)).andReturn(null).once();
		expect(service.identifieContribuablePourInscription(CTB_ID, NO_AVS)).andReturn(null).once();
		expect(service.valideEtatContribuablePourInscription(CTB_ID)).andReturn(false).once();
		expect(service.imprimerDocumentEfacture(CTB_ID, TypeDocument.E_FACTURE_ATTENTE_CONTACT, DEMANDE_DATE)).andReturn(ARCHIVAGE_ID).once();
		sender.envoieMiseEnAttenteDemandeInscription(DEMANDE_ID,TypeAttenteEFacture.EN_ATTENTE_CONTACT, ARCHIVAGE_ID, false);
		service.updateEmailContribuable(CTB_ID, EMAIL);
		replay(inscription, sender, service);
		handler.handle(inscription);
		verify(inscription, sender, service);
	}

	@Test
	public void testDemandeInscription_EmailInvalide() throws Exception {
		testDemandeInscription_EchecValidationBasique(TypeRefusEFacture.EMAIL_INVALIDE);
	}

	@Test
	public void testDemandeInscription_NoAvsInvalide() throws Exception {
		testDemandeInscription_EchecValidationBasique(TypeRefusEFacture.NUMERO_AVS_INVALIDE);
	}

	@Test
	public void testDemandeInscription_DateAbsente() throws Exception {
		testDemandeInscription_EchecValidationBasique(TypeRefusEFacture.DATE_DEMANDE_ABSENTE);
	}

	private void testDemandeInscription_EchecValidationBasique(TypeRefusEFacture typeRefus) throws Exception  {
		expect(inscription.performBasicValidation()).andReturn(typeRefus).once();
		sender.envoieRefusDemandeInscription(DEMANDE_ID, typeRefus);
		replay(inscription, sender, service);
		handler.handle(inscription);
		verify(inscription, sender, service);
	}

	@Test
	public void testDemandeInscription_UneAutreDemandeEstDejaEnCoursDeTraitment() throws Exception {
		expect(inscription.performBasicValidation()).andReturn(null).once();
		expect(service.getDemandeInscriptionEnCoursDeTraitement(CTB_ID)).andReturn(createMock(DemandeValidationInscriptionDejaSoumise.class)).once();
		sender.envoieRefusDemandeInscription(DEMANDE_ID, TypeRefusEFacture.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT);
		replay(inscription, sender, service);
		handler.handle(inscription);
		verify(inscription, sender, service);
	}

	@Test
	public void testDemandeInscription_EchecIdentification_ContribualbleInconnu() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusEFacture.NUMERO_CTB_INCOHERENT);
	}

	@Test
	public void testDemandeInscription_EchecIdentification_AvsEtCtbNeCorrespondentPas() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT);
	}

	@Test
	public void testDemandeInscription_EchecIdentification_AdresseCourrierInexistante() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusEFacture.ADRESSE_COURRIER_INEXISTANTE);
	}

	private void testDemandeInscription_EchecIdentification(TypeRefusEFacture typeRefus) throws Exception {
		expect(inscription.performBasicValidation()).andReturn(null).once();
		expect(service.getDemandeInscriptionEnCoursDeTraitement(CTB_ID)).andReturn(null).once();
		expect(service.identifieContribuablePourInscription(CTB_ID, NO_AVS)).andReturn(typeRefus).once();
		sender.envoieRefusDemandeInscription(DEMANDE_ID, typeRefus);
		replay(inscription, sender, service);
		handler.handle(inscription);
		verify(inscription, sender, service);
	}



}
