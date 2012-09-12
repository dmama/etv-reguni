package ch.vd.uniregctb.efacture.manager;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;
import org.springframework.context.MessageSource;

import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.efacture.data.DestinataireAvecHisto;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.efacture.DestinataireAvecHistoView;
import ch.vd.uniregctb.efacture.EFactureResponseService;
import ch.vd.uniregctb.efacture.EFactureService;
import ch.vd.uniregctb.type.TypeDocument;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.contains;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EFactureManagerTest extends WithoutSpringTest {

	private EfactureManagerImpl eFactureManager = new EfactureManagerImpl();
	private EFactureService eFactureService;
	private EFactureResponseService eFactureResponseService;
	private MessageSource messageSource;
	@Override
	public void onSetUp() throws Exception {
		eFactureService = createMock(EFactureService.class);
		eFactureResponseService = createMock(EFactureResponseService.class);
		messageSource = createMock(MessageSource.class);
		expect(messageSource.getMessage(anyObject(String.class),anyObject(Object[].class),anyObject(Locale.class))).andStubReturn("LIBELLE_ETAT_DESTINATAIRE");
		eFactureManager = new EfactureManagerImpl();
		eFactureManager.seteFactureResponseService(eFactureResponseService);
		eFactureManager.seteFactureService(eFactureService);
		eFactureManager.setMessageSource(messageSource);
		AuthenticationHelper.pushPrincipal("USER_ID");
	}

	@Override
	public void onTearDown() throws Exception {
		AuthenticationHelper.popPrincipal();
		verify(eFactureService, eFactureResponseService, messageSource);
	}

	private static final long NO_CTB = 12345678;
	private static final RegDate DATE_DEMANDE = RegDate.get(2012,3,4);

	@Test
	public void testEnvoyerDocumentAvecNotificationEFacture () throws Exception {
		final TypeDocument typeDocumentAEnvoyer = TypeDocument.E_FACTURE_ATTENTE_CONTACT;
		expect(eFactureService.imprimerDocumentEfacture(NO_CTB, typeDocumentAEnvoyer, DATE_DEMANDE)).andReturn("ARCHIVAGE_ID");
		expect(eFactureService.notifieMiseEnAttenteInscription(
				eq("ID_DEMANDE"),
				eq(TypeAttenteDemande.EN_ATTENTE_CONTACT),
				contains("USER_ID"),
				eq("ARCHIVAGE_ID"), eq(true))).andReturn("BUSINESS_ID");
		replayAll();
		assertEquals("BUSINESS_ID", eFactureManager.envoyerDocumentAvecNotificationEFacture(NO_CTB, typeDocumentAEnvoyer, "ID_DEMANDE", DATE_DEMANDE));
	}

	private void replayAll() {
		replay(eFactureResponseService, eFactureService, messageSource);
	}

	@Test
	public void testGetDestinataireAvecSonHistorique() {
		PayerSituationHistoryEntry pshe = new PayerSituationHistoryEntry();
		pshe.setStatus(PayerStatus.INSCRIT);
		expect(eFactureService.getDestinataireAvecSonHistorique(NO_CTB))
				.andReturn(null)
				.andReturn(
						new DestinataireAvecHisto(
								new PayerWithHistory(
										new PayerId("BUSINESS_ID", "BILLER_ID"),
										PayerStatus.INSCRIT,
										new PayerWithHistory.HistoryOfSituations(Arrays.asList(pshe)),
										new PayerWithHistory.HistoryOfRequests()),
								NO_CTB));
		replayAll();
		DestinataireAvecHistoView view = eFactureManager.getDestinataireAvecSonHistorique(NO_CTB);
		assertNull(view);
		view = eFactureManager.getDestinataireAvecSonHistorique(NO_CTB);
		assertNotNull(view);
	}

	@Test
	public void testSuspendreContribuable() throws Exception {
		expect(eFactureService.suspendreContribuable(eq(NO_CTB), eq(true), contains("USER_ID"))).andReturn("BUSINESS_ID");
		replayAll();
		assertEquals("BUSINESS_ID", eFactureManager.suspendreContribuable(NO_CTB));
	}

	@Test
	public void testActiverContribuable() throws Exception {
		expect(eFactureService.activerContribuable(eq(NO_CTB), eq(true), contains("USER_ID"))).andReturn("BUSINESS_ID");
		replayAll();
		assertEquals("BUSINESS_ID", eFactureManager.activerContribuable(NO_CTB));
	}

	@Test
	public void testAccepterDemande() throws Exception {
		expect(eFactureService.accepterDemande(eq("ID_DEMANDE"), eq(true), contains("USER_ID"))).andReturn("BUSINESS_ID");
		replayAll();
		assertEquals("BUSINESS_ID", eFactureManager.accepterDemande("ID_DEMANDE"));
	}

	@Test
	public void testRefuserDemande() throws Exception {
		expect(eFactureService.refuserDemande(eq("ID_DEMANDE"), eq(true), contains("USER_ID"))).andReturn("BUSINESS_ID");
		replayAll();
		assertEquals("BUSINESS_ID", eFactureManager.refuserDemande("ID_DEMANDE"));
	}

	@Test
	public void testIsReponseRecueDeEfacture() {
		expect(eFactureResponseService.waitForResponse(eq("BUSINESS_ID"), anyLong())).andReturn(true).andReturn(false);
		replayAll();
		assertTrue(eFactureManager.isReponseRecueDeEfacture("BUSINESS_ID"));
		assertFalse(eFactureManager.isReponseRecueDeEfacture("BUSINESS_ID"));
	}

	@Test
	public void testGetMessageQuittancement() {
		expect(eFactureResponseService.waitForResponse(eq("BUSINESS_ID"), anyLong())).andReturn(false).andReturn(true);
		replayAll();
		String msg = eFactureManager.getMessageQuittancement(ResultatQuittancement.enCours("BUSINESS_ID"), NO_CTB);
		assertContains("en cours", msg);
		assertContains(Long.toString(NO_CTB), msg);
		msg = eFactureManager.getMessageQuittancement(ResultatQuittancement.enCours("BUSINESS_ID"), NO_CTB);
		assertContains("maintenant inscrit", msg);
		assertContains(Long.toString(NO_CTB), msg);
	}
}
