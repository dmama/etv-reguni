package ch.vd.uniregctb.evenement.externe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.Lifecycle;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.evenement.externe.jms.MessageListener;
import ch.vd.uniregctb.evenement.externe.mock.MockEvenementExterneFacade;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EvenementExterneServiceTest extends BusinessTest {

	private static final long ID_LR = 21L;
	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/evenement/externe/EvenementExterneServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12500001L;
	private final static RegDate DATE_DEBUT_PERIODE = RegDate.get(2008, 1, 1);
	private final static RegDate DATE_FIN_PERIODE = RegDate.get(2008, 1, 31);
	private final static RegDate DATE_QUITTANCEMENT = RegDate.get();

	private EvenementExterneDAO evenementExterneDAO;
	private EvenementExterneServiceImpl evenementExterneService;
	private MockEvenementExterneFacade facade;

	@Override
	protected void doLoadDatabase() throws Exception {
		super.doLoadDatabase();

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");

		evenementExterneService = new EvenementExterneServiceImpl();
		facade = new MockEvenementExterneFacade();
		facade.setMessageListener(new MessageListener());
		facade.setListenerContainer(EasyMock.createNiceMock(Lifecycle.class));
		evenementExterneService.setEvenementExterneFacade(facade);
	}

	@Test
	@ExpectedException(IllegalArgumentException.class)
	public void nullListener() throws Exception {
		facade.setMessageListener(null);
	}

	@Test
	@ExpectedException(EvenementExterneException.class)
	public void sendMessageEmpty() throws Exception {
		final EvenementImpotSourceQuittanceType evenement = evenementExterneService.getEvenementExterneFacade().creerEvenementImpotSource();
		evenementExterneService.sendEvenementExterne(evenement);
	}

	@Test
	public void sendWithoutPropagation() throws Exception {
		assertEquals(0, evenementExterneDAO.getAll().size());
		final EvenementImpotSourceQuittanceType evenement = createEvenementQuittancement(TypeQuittance.QUITTANCEMENT);
		evenementExterneService.sendEvenementExterne(evenement);
		assertEquals(0, evenementExterneDAO.getAll().size()); // pas d'application context, pas de listener d'event, pas d'event créés en base
	}

	@Test
	@NotTransactional
	public void sendEvenementQuittancement() throws Exception {
		assertEquals(0, evenementExterneDAO.getAll().size()); // précondition

		// envoi de l'événement externe avec insertion de l'événement dans la base
		evenementExterneService.setApplicationContext(getApplicationContext());
		final EvenementImpotSourceQuittanceType evenement = createEvenementQuittancement(TypeQuittance.QUITTANCEMENT);
		evenementExterneService.sendEvenementExterne(evenement);

		// vérification de l'événement inséré dans la base
		final List<EvenementExterne> all = evenementExterneDAO.getAll();
		assertEquals(1, all.size());
		assertEvenement(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(),
				null, all.get(0));

		// vérification que la déclaration a bien été flaggée comme retournée
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) {

				final DeclarationImpotSource lr = getLR(ID_LR);
				assertNotNull(lr);
				assertFalse(lr.isAnnule());

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final EtatDeclaration dernierEtat = etats.iterator().next();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertFalse(dernierEtat.isAnnule());
				return null;
			}
		});
	}


	@Test
	@NotTransactional
	public void sendEvenementAnnulationQuittancement() throws Exception {

		sendEvenementQuittancement();

		assertEquals(1, evenementExterneDAO.getAll().size()); // précondition

		// envoi de l'événement externe avec insertion de l'événement dans la base
		evenementExterneService.setApplicationContext(getApplicationContext());
		final EvenementImpotSourceQuittanceType evenement = createEvenementQuittancement(TypeQuittance.ANNULATION);
		evenementExterneService.sendEvenementExterne(evenement);

		// vérification de l'événement inséré dans la base
		final List<EvenementExterne> all = evenementExterneDAO.getAll();
		assertEquals(2, all.size());
		assertEvenement(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(),
				null, all.get(0));
		assertEvenement(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(),
				null, all.get(1));

		// vérification que l'état retourné de la déclaration a bien été annulé
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) {

				final DeclarationImpotSource lr = getLR(ID_LR);
				assertNotNull(lr);
				assertFalse(lr.isAnnule());

				final Set<EtatDeclaration> etats = lr.getEtats();
				assertNotNull(etats);
				assertEquals(1, etats.size());

				final EtatDeclaration dernierEtat = etats.iterator().next();
				assertNotNull(dernierEtat);
				assertEquals(TypeEtatDeclaration.RETOURNEE, dernierEtat.getEtat());
				assertTrue(dernierEtat.isAnnule());
				return null;
			}
		});
	}

	@Test
	@NotTransactional
	public void sendEvenementQuittancementTwice() throws Exception {
		assertEquals(0, evenementExterneDAO.getAll().size()); // précondition

		// envoi de l'événement externe avec insertion de l'événement dans la base
		facade.setNumberOfSend(2);
		evenementExterneService.setApplicationContext(getApplicationContext());
		final EvenementImpotSourceQuittanceType evenement = createEvenementQuittancement(TypeQuittance.QUITTANCEMENT);
		evenementExterneService.sendEvenementExterne(evenement);

		// vérification que un seul événement a été inséré dans la base
		final List<EvenementExterne> all = evenementExterneDAO.getAll();
		assertEquals(1, all.size());
		assertEvenement(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(),
				null, all.get(0));
		Assert.assertEquals(1, evenementExterneDAO.getAll().size());
	}

	private EvenementImpotSourceQuittanceType createEvenementQuittancement(TypeQuittance.Enum quitancement) {
		return evenementExterneService.createEvenementQuittancement(quitancement, NUMERO_CONTRIBUABLE, DATE_DEBUT_PERIODE,
				DATE_FIN_PERIODE, DATE_QUITTANCEMENT);
	}

	private DeclarationImpotSource getLR(long id) {
		final DeclarationImpotSource lr = (DeclarationImpotSource) evenementExterneDAO.getHibernateTemplate().get(
				DeclarationImpotSource.class, id);
		return lr;
	}

	private static void assertEvenement(final Long numeroTiers, final EtatEvenementExterne etat, final Date dateEvenement,
			final Date dateTraitement, String errorMessage, final EvenementExterne event0) {
		assertNotNull(event0);
		assertNotNull(event0.getCorrelationId());
		assertEquals(dateTraitement, event0.getDateTraitement());
		assertEquals(errorMessage, event0.getErrorMessage());
		assertEquals(etat, event0.getEtat());
		assertEquals(numeroTiers, event0.getNumeroTiers());
		assertEquals(dateEvenement, event0.getDateEvenement());
	}
}

