package ch.vd.uniregctb.efacture;


import org.apache.commons.lang.mutable.MutableBoolean;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.Demande;
import ch.vd.unireg.interfaces.efacture.data.DemandeAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.editique.EditiqueException;
import ch.vd.uniregctb.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class EFactureEventHandlerTest extends WithoutSpringTest {

	private static final long CTB_ID = 123456L;
	private static final String NO_AVS = "7565817249033";
	private static final String EMAIL = "noel.flantier@dgse.fr";
	private static final String DEMANDE_ID = "DEMANDE_123";
	private static final RegDate DEMANDE_DATE = date(2012, 6, 21);
	private static final String ARCHIVAGE_ID = "ARCHIVE_123ABC";


	private EFactureEventHandlerImpl handler;
	private Demande inscription;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		handler = new EFactureEventHandlerImpl();
		inscription = new DemandeBuilderForUnitTests()
							.id(DEMANDE_ID)
							.dateDemande(DEMANDE_DATE)
							.noAvs(NO_AVS)
							.email(EMAIL)
							.businessPayerId(Long.toString(CTB_ID)).build();
	}

	@Test
	public void testDemandeInscription_OK() throws Exception {
		final MutableBoolean demandeOK = new MutableBoolean(false);
		EFactureService service = new EFactureServiceMock() {
			@Override
			public DemandeAvecHisto getDemandeEnAttente(long ctbId) {
				assertEquals(CTB_ID,ctbId);
				return null;
			}

			@Override
			public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String noAvs) throws AdresseException {
				assertEquals(CTB_ID,ctbId);
				assertEquals(NO_AVS,noAvs);
				return null;
			}

			@Override
			public boolean valideEtatFiscalContribuablePourInscription(long ctbId) {
				assertEquals(CTB_ID,ctbId);
				return true;
			}

			@Override
			public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException {
				assertEquals(CTB_ID, ctbId.longValue());
				assertEquals(TypeDocument.E_FACTURE_ATTENTE_SIGNATURE, typeDocument);
				assertEquals(DEMANDE_DATE, dateDemande);
				return ARCHIVAGE_ID;
			}

			@Override
			public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws
					EvenementEfactureException {
				assertEquals(DEMANDE_ID, idDemande);
				assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE, typeAttenteEFacture);
				assertEquals(TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getDescription(), description);
				assertEquals(ARCHIVAGE_ID, idArchivage);
				assertEquals(false, retourAttendu);
				demandeOK.setValue(true);
				return null;
			}

			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				fail("La demande ne doit pas être refusée");
				return null;
			}
		};
		handler.seteFactureService(service);
		handler.handle(inscription);
		assertTrue(demandeOK.booleanValue());
	}

	@Test
	public void testDemandeInscription_OK_MaisEtatIncoherent() throws Exception {
		final MutableBoolean demandeOK = new MutableBoolean(false);
		EFactureService service = new EFactureServiceMock() {
			@Override
			public DemandeAvecHisto getDemandeEnAttente(long ctbId) {
				assertEquals(CTB_ID,ctbId);
				return null;
			}

			@Override
			public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String noAvs) throws AdresseException {
				assertEquals(CTB_ID,ctbId);
				assertEquals(NO_AVS,noAvs);
				return null;
			}

			@Override
			public boolean valideEtatFiscalContribuablePourInscription(long ctbId) {
				assertEquals(CTB_ID,ctbId);
				return false;
			}

			@Override
			public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande) throws EditiqueException {
				assertEquals(CTB_ID, ctbId.longValue());
				assertEquals(TypeDocument.E_FACTURE_ATTENTE_CONTACT, typeDocument);
				assertEquals(DEMANDE_DATE, dateDemande);
				return ARCHIVAGE_ID;
			}

			@Override
			public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws
					EvenementEfactureException {
				assertEquals(DEMANDE_ID, idDemande);
				assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT, typeAttenteEFacture);
				assertEquals(String.format("%s Assujettissement incohérent avec la e-facture.", TypeAttenteDemande.EN_ATTENTE_CONTACT.getDescription()), description);
				assertEquals(ARCHIVAGE_ID, idArchivage);
				assertEquals(false, retourAttendu);
				demandeOK.setValue(true);
				return null;
			}

			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				fail("La demande ne doit pas être refusée");
				return null;
			}
		};
		handler.seteFactureService(service);
		handler.handle(inscription);
		assertTrue(demandeOK.booleanValue());
	}

	@Test
	public void testDemandeInscription_NoAvsInvalide() throws Exception {
		inscription = new DemandeBuilderForUnitTests()
				.id(DEMANDE_ID)
				.dateDemande(DEMANDE_DATE)
				.noAvs("12330404040")
				.email(EMAIL)
				.businessPayerId(Long.toString(CTB_ID)).build();
		testDemandeInscription_EchecValidationBasique(TypeRefusDemande.NUMERO_AVS_INVALIDE);
	}

	@Test
	public void testDemandeInscription_DateAbsente() throws Exception {
		//noinspection NullableProblems
		inscription = new DemandeBuilderForUnitTests()
				.id(DEMANDE_ID)
				.noAvs(NO_AVS)
				.dateDemande(null)
				.email(EMAIL)
				.businessPayerId(Long.toString(CTB_ID)).build();
		testDemandeInscription_EchecValidationBasique(TypeRefusDemande.DATE_DEMANDE_ABSENTE);
	}

	private void testDemandeInscription_EchecValidationBasique(final TypeRefusDemande typeRefus) throws Exception  {
		final MutableBoolean demandeRefusee = new MutableBoolean(false);
		EFactureService service = new EFactureServiceMock() {
			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(DEMANDE_ID, idDemande);
				assertEquals(false, retourAttendu);
				assertEquals(typeRefus.getDescription(), description);
				demandeRefusee.setValue(true);
				return null;
			}
		};
		handler.seteFactureService(service);
		handler.handle(inscription);
		assertTrue(demandeRefusee.booleanValue());
	}

	@Test
	public void testDemandeInscription_UneAutreDemandeEstDejaEnCoursDeTraitment() throws Exception {
		final MutableBoolean demandeRefusee = new MutableBoolean(false);
		EFactureService service = new EFactureServiceMock() {
			@Override
			public DemandeAvecHisto getDemandeEnAttente(long ctbId) {
				assertEquals(CTB_ID, ctbId);
				return new DemandeAvecHistoBuilderForUnitTests().build();
			}

			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(DEMANDE_ID, idDemande);
				assertEquals(false, retourAttendu);
				assertEquals(TypeRefusDemande.AUTRE_DEMANDE_EN_COURS_DE_TRAITEMENT.getDescription(), description);
				demandeRefusee.setValue(true);
				return null;
			}
		};
		handler.seteFactureService(service);
		handler.handle(inscription);
		assertTrue(demandeRefusee.booleanValue());
	}

	@Test
	public void testDemandeInscription_EchecIdentification_ContribualbleInconnu() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusDemande.NUMERO_CTB_INCOHERENT);
	}

	@Test
	public void testDemandeInscription_EchecIdentification_AvsEtCtbNeCorrespondentPas() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT);
	}

	@Test
	public void testDemandeInscription_EchecIdentification_AdresseCourrierInexistante() throws Exception {
		testDemandeInscription_EchecIdentification(TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE);
	}

	private void testDemandeInscription_EchecIdentification(final TypeRefusDemande typeRefus) throws Exception {
		final MutableBoolean demandeRefusee = new MutableBoolean(false);
		EFactureService service = new EFactureServiceMock() {
			@Override
			public TypeRefusDemande identifieContribuablePourInscription(long ctbId, String noAvs) throws AdresseException {
				assertEquals(CTB_ID, ctbId);
				assertEquals(NO_AVS, noAvs);
				return typeRefus;
			}

			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(DEMANDE_ID, idDemande);
				assertEquals(false, retourAttendu);
				assertEquals(typeRefus.getDescription(), description);
				demandeRefusee.setValue(true);
				return null;
			}

		};
		handler.seteFactureService(service);
		handler.handle(inscription);
		assertTrue(demandeRefusee.booleanValue());
	}

}
