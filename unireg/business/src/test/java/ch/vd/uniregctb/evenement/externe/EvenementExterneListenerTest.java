package ch.vd.uniregctb.evenement.externe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.context.Lifecycle;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.evenement.externe.jms.EvenementExterneResultatImpl;
import ch.vd.uniregctb.evenement.externe.jms.MessageListener;
import ch.vd.uniregctb.evenement.externe.mock.MockEvenementExterneFacade;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EvenementExterneListenerTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/lr/LrServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12500001L;
	private final static RegDate DATE_DEBUT_PERIODE = RegDate.get(2008, 1, 1);
	private final static RegDate DATE_FIN_PERIODE = RegDate.get(2008, 1, 31);
	private final static RegDate DATE_QUITTANCEMENT = RegDate.get();

	private ListenerWrapper listener;

	private EvenementExterneServiceImpl evenementExterneService;

	private DatabaseService databaseService;

	private MockEvenementExterneFacade facade;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		databaseService = getBean(DatabaseService.class, "databaseService");

		listener = new ListenerWrapper();
		evenementExterneService = new EvenementExterneServiceImpl();
		listener.setEvenementExterneDAO(evenementExterneDAO);
		listener.setTiersDAO(tiersDAO);
		listener.setDatabaseService(databaseService);
		facade = new MockEvenementExterneFacade();
		facade.setMessageListener(new MessageListener());
		facade.setListenerContainer(EasyMock.createNiceMock(Lifecycle.class));
		evenementExterneService.setEvenementExterneFacade(facade);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void sendEventWithNull() throws Exception {
		@SuppressWarnings("unused")
		EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(null);
	}

	@Test
	public void sendEventEmpty() throws Exception {

		EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(createMessageQuittancementEmpty());
		listener.sentEvent(event);
	}

	@Test
	public void sendEventImpotSource() throws Exception {
		final EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(createMessageQuittancement(TypeQuittance.QUITTANCEMENT));
		listener.sentEvent(event);
	}

	@Test
	public void sendEventQuittancement() throws Exception {

		final EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(createMessageQuittancement(TypeQuittance.QUITTANCEMENT));
		listener.sentEvent(event);

		assertEquals(1, evenementExterneDAO.getAll().size());

		final DeclarationImpotSource declaration = getDeclarationImpotSource(getDefaultTiers(), DATE_DEBUT_PERIODE);
		assertNotNull(declaration);
		final EtatDeclaration etatDeclaration = declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		assertNotNull(etatDeclaration);
		assertEquals(DATE_QUITTANCEMENT, etatDeclaration.getDateObtention());
		assertEquals(Boolean.FALSE, etatDeclaration.isAnnule());
	}

	@Test
	public void sendEventAnnulationEtatRetourNonExiste() throws Exception{
		EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(createMessageQuittancement(TypeQuittance.ANNULATION));
		listener.sentEvent(event);

		assertEquals(1 ,evenementExterneDAO.getAll().size());

		DeclarationImpotSource declaration = getDeclarationImpotSource(getDefaultTiers(), DATE_DEBUT_PERIODE);
		assertNotNull(declaration);
		EtatDeclaration etatDeclaration = declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		assertNull(etatDeclaration);
	}

	@Test
	@NotTransactional
	public void sendEventAnnulation() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// envoi d'un événement de quittancement
				EvenementExterneResultat message = createMessageQuittancement(TypeQuittance.QUITTANCEMENT);
				EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(message);
				listener.sentEvent(event);

				// envoi d'un événement qui annule l'événement de quittancement précédent
				message = createMessageQuittancement(TypeQuittance.ANNULATION);
				event = new EvenementExterneReceivedEvent(message);
				listener.sentEvent(event);

				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final List<EvenementExterne> list = evenementExterneDAO.getAll();
				assertEquals(2, list.size());
				assertEquals(EtatEvenementExterne.TRAITE, list.get(0).getEtat());
				assertEquals(EtatEvenementExterne.TRAITE, list.get(1).getEtat());

				final DeclarationImpotSource declaration = getDeclarationImpotSource(getDefaultTiers(), DATE_DEBUT_PERIODE);
				assertNotNull(declaration);

				final Set<EtatDeclaration> etats = declaration.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final EtatDeclaration dernierEtat = etats.iterator().next();
				assertNotNull(dernierEtat);
				assertEquals(DATE_QUITTANCEMENT, dernierEtat.getDateObtention());
				assertTrue(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	@Test
	public void sendEventAnnulationWithError() throws Exception {
		assertEquals(0, evenementExterneDAO.getAll().size()); // précondition

		// envoi d'un événement d'annulation d'un événement précédent mais qui n'existe pas -> erreur
		EvenementExterneReceivedEvent event = new EvenementExterneReceivedEvent(createMessageQuittancement(TypeQuittance.ANNULATION));
		listener.sentEvent(event);

		final List<EvenementExterne> all = evenementExterneDAO.getAll();
		assertEquals(1, all.size());

		final EvenementExterne event0 = all.get(0);
		assertNotNull(event0);
		assertEquals(EtatEvenementExterne.NON_TRAITE, event0.getEtat());
	}

	private EvenementExterneResultat createMessageQuittancementEmpty() {
		EvenementExterneResultat resultat = new EvenementExterneResultatImpl();
		return resultat;
	}

	private EvenementExterneResultat createMessageQuittancement(TypeQuittance.Enum quitancement) {
		EvenementExterneResultatImpl resultat = new EvenementExterneResultatImpl();
		resultat.setEvenement(createEvenementQuittancement(quitancement));
		resultat.setCorrelationId("TEST-" + System.currentTimeMillis());
		return resultat;
	}

	private EvenementImpotSourceQuittanceType createEvenementQuittancement(TypeQuittance.Enum quitancement) {
		return evenementExterneService.createEvenementQuittancement(quitancement, NUMERO_CONTRIBUABLE, DATE_DEBUT_PERIODE,
				DATE_FIN_PERIODE, DATE_QUITTANCEMENT);
	}

	private Tiers getDefaultTiers() {
		return this.tiersDAO.get(NUMERO_CONTRIBUABLE);
	}

	private DeclarationImpotSource getDeclarationImpotSource(Tiers tiers, RegDate dateDebut) {
		for (Declaration declaration : tiers.getDeclarations()) {
			if (declaration instanceof DeclarationImpotSource && !declaration.isAnnule()) {
				if (declaration.getDateDebut().compareTo(dateDebut) == 0) {
					return (DeclarationImpotSource) declaration;
				}
			}
		}
		return null;
	}

	private class ListenerWrapper extends EvenementExterneListenerImpl {
		public void sentEvent(EvenementExterneReceivedEvent event) {
			this.doEvent(event);
		}
	}
}
