package ch.vd.unireg.efacture.manager;

import java.math.BigInteger;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.editique.EditiqueException;
import ch.vd.unireg.efacture.DestinataireAvecHistoView;
import ch.vd.unireg.efacture.DummyEFactureService;
import ch.vd.unireg.efacture.EFactureResponseService;
import ch.vd.unireg.efacture.EFactureService;
import ch.vd.unireg.efacture.EvenementEfactureException;
import ch.vd.unireg.type.TypeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EFactureManagerTest extends WithoutSpringTest {

	private EfactureManagerImpl eFactureManager = new EfactureManagerImpl();

	@Override
	public void onSetUp() throws Exception {
		final MessageSource messageSource = new MessageSource() {
			@Override
			public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
				return "LIBELLE_ETAT_DESTINATAIRE";
			}

			@Override
			public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
				return "LIBELLE_ETAT_DESTINATAIRE";
			}

			@Override
			public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
				return "LIBELLE_ETAT_DESTINATAIRE";
			}
		};
		eFactureManager = new EfactureManagerImpl();
		eFactureManager.setMessageSource(messageSource);
		eFactureManager.seteFactureService(new DummyEFactureService());
		eFactureManager.seteFactureResponseService(new EFactureResponseServiceMock());
		AuthenticationHelper.pushPrincipal("USER_ID");
	}

	@Override
	public void onTearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
	}

	private static final long NO_CTB = 12345678;
	private static final RegDate DATE_DEMANDE = RegDate.get(2012,3,4);

	@Test
	public void testEnvoyerDocumentAvecNotificationEFacture () throws Exception {
		EFactureService eFactureService = new DummyEFactureService() {
			@Override
			public String imprimerDocumentEfacture(Long ctbId, TypeDocument typeDocument, RegDate dateDemande, BigInteger noAdherent, RegDate dateDemandePrecedente, BigInteger noAdherentPrecedent) throws EditiqueException {
				assertEquals(NO_CTB, ctbId.longValue());
				assertEquals(TypeDocument.E_FACTURE_ATTENTE_CONTACT, typeDocument);
				assertEquals(DATE_DEMANDE, dateDemande);
				return "ARCHIVAGE_ID";
			}

			@Override
			public String notifieMiseEnAttenteInscription(String idDemande, TypeAttenteDemande typeAttenteEFacture, String description, String idArchivage, boolean retourAttendu) throws
					EvenementEfactureException {
				assertEquals("ID_DEMANDE", idDemande);
				assertEquals(TypeAttenteDemande.EN_ATTENTE_CONTACT, typeAttenteEFacture);
				assertContains("USER_ID", description);
				assertEquals("ARCHIVAGE_ID", idArchivage);
				assertEquals(true, retourAttendu);
				return "BUSINESS_ID";
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		assertEquals("BUSINESS_ID", eFactureManager.envoyerDocumentAvecNotificationEFacture(NO_CTB, TypeDocument.E_FACTURE_ATTENTE_CONTACT, "ID_DEMANDE", DATE_DEMANDE, BigInteger.ONE, null, null));
	}


	@Test
	public void testGetDestinataireAvecSonHistorique() {
		EFactureService eFactureService = new DummyEFactureService() {
			boolean firstCall = true;
			@Override
			public DestinataireAvecHisto getDestinataireAvecSonHistorique(long ctbId) {
				assertEquals(NO_CTB, ctbId);
				if (firstCall) {
					firstCall = false;
					return null;
				} else {
					PayerSituationHistoryEntry pshe = new PayerSituationHistoryEntry();
					pshe.setStatus(PayerStatus.INSCRIT);
					return new DestinataireAvecHisto(
							new PayerWithHistory(
									new PayerId("BUSINESS_ID", "BILLER_ID"),
									PayerStatus.INSCRIT,
									new PayerWithHistory.HistoryOfSituations(Collections.singletonList(pshe)),
									new PayerWithHistory.HistoryOfRequests()),
							NO_CTB);
				}
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		DestinataireAvecHistoView view = eFactureManager.getDestinataireAvecSonHistorique(NO_CTB);
		assertNull(view);
		view = eFactureManager.getDestinataireAvecSonHistorique(NO_CTB);
		assertNotNull(view);
	}

	@Test
	public void testSuspendreContribuable() throws Exception {
		final EFactureService eFactureService = new DummyEFactureService() {
			@Override
			public String suspendreContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(NO_CTB, ctbId);
				assertEquals(true, retourAttendu);
				assertContains("USER_ID", description);
				return "BUSINESS_ID";
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		assertEquals("BUSINESS_ID", eFactureManager.suspendreContribuable(NO_CTB, null));
	}

	@Test
	public void testActiverContribuable() throws Exception {
		final EFactureService eFactureService = new DummyEFactureService() {
			@Override
			public String activerContribuable(long ctbId, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(NO_CTB, ctbId);
				assertEquals(true, retourAttendu);
				assertContains("USER_ID", description);
				return "BUSINESS_ID";
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		assertEquals("BUSINESS_ID", eFactureManager.activerContribuable(NO_CTB, null));
	}

	@Test
	public void testAccepterDemande() throws Exception {
		EFactureService eFactureService = new DummyEFactureService() {
			@Override
			public String accepterDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals("ID_DEMANDE",idDemande);
				assertEquals(true,retourAttendu);
				assertContains("USER_ID", description);
				return "BUSINESS_ID";
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		assertEquals("BUSINESS_ID", eFactureManager.accepterDemande("ID_DEMANDE"));
	}

	@Test
	public void testRefuserDemande() throws Exception {
		EFactureService eFactureService = new DummyEFactureService() {
			@Override
			public String refuserDemande(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals("ID_DEMANDE",idDemande);
				assertEquals(true,retourAttendu);
				assertContains("USER_ID", description);
				return "BUSINESS_ID";
			}
		};
		eFactureManager.seteFactureService(eFactureService);
		assertEquals("BUSINESS_ID", eFactureManager.refuserDemande("ID_DEMANDE"));
	}

	@Test
	public void testIsReponseRecueDeEfacture() {
		EFactureResponseService eFactureResponseService = new EFactureResponseService() {
			@Override
			public void onNewResponse(String businessId) {}

			boolean firstCall = true;
			@Override
			public boolean waitForResponse(String businessId, Duration timeout) {
				assertEquals("BUSINESS_ID", businessId);
				if (firstCall) {
					firstCall = false;
					return true;
				}
				return false;
			}
		};
		eFactureManager.seteFactureResponseService(eFactureResponseService);
		assertTrue(eFactureManager.isReponseRecueDeEfacture("BUSINESS_ID"));
		assertFalse(eFactureManager.isReponseRecueDeEfacture("BUSINESS_ID"));
	}

	@Test
	public void testGetMessageQuittancement() {
		EFactureResponseService eFactureResponseService = new EFactureResponseService() {
			@Override
			public void onNewResponse(String businessId) {}

			boolean firstCall = true;
			@Override
			public boolean waitForResponse(String businessId, Duration timeout) {
				assertEquals("BUSINESS_ID", businessId);
				if (firstCall) {
					firstCall = false;
					return false;
				}
				return true;
			}
		};
		eFactureManager.seteFactureResponseService(eFactureResponseService);
		String msg = eFactureManager.getMessageQuittancement(ResultatQuittancement.enCours("BUSINESS_ID"), NO_CTB);
		assertContains("en cours", msg);
		assertContains(Long.toString(NO_CTB), msg);
		msg = eFactureManager.getMessageQuittancement(ResultatQuittancement.enCours("BUSINESS_ID"), NO_CTB);
		assertContains("maintenant inscrit", msg);
		assertContains(Long.toString(NO_CTB), msg);
	}

	private static class EFactureResponseServiceMock implements EFactureResponseService {
		@Override
		public void onNewResponse(String businessId) {}

		@Override
		public boolean waitForResponse(String businessId, Duration timeout) {
			return false;
		}
	}
}
