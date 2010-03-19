package ch.vd.uniregctb.evenement.externe;

import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceDocument;
import ch.vd.fiscalite.registre.evenementImpotSourceV1.EvenementImpotSourceQuittanceType.TypeQuittance;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotSource;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class EvenementExterneListenerTest extends BusinessTest {

	private final static String DB_UNIT_DATA_FILE = "classpath:ch/vd/uniregctb/lr/LrServiceTest.xml";
	private final static Long NUMERO_CONTRIBUABLE = 12500001L;
	private final static RegDate DATE_DEBUT_PERIODE = RegDate.get(2008, 1, 1);
	private final static RegDate DATE_FIN_PERIODE = RegDate.get(2008, 1, 31);
	private final static RegDate DATE_QUITTANCEMENT = RegDate.get();

	private EvenementExterneListenerImpl listener;

	private EvenementExterneDAO evenementExterneDAO;
	private TiersDAO tiersDAO;
	private EvenementExterneService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		evenementExterneDAO = getBean(EvenementExterneDAO.class, "evenementExterneDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		service = getBean(EvenementExterneService.class, "evenementExterneService");

		listener = new EvenementExterneListenerImpl();
		listener.setHandler(service);

		loadDatabase(DB_UNIT_DATA_FILE);
	}

	@Test
	public void testEventImpotSource() throws Exception {
		final String message = createMessageQuittancement(TypeQuittance.QUITTANCEMENT);
		listener.onMessage(message, "TEST-" + System.currentTimeMillis());
	}

	@Test
	public void testEventQuittancement() throws Exception {

		final String message = createMessageQuittancement(TypeQuittance.QUITTANCEMENT);
		listener.onMessage(message, "TEST-" + System.currentTimeMillis());

		assertEquals(1, evenementExterneDAO.getAll().size());

		final DeclarationImpotSource declaration = getDeclarationImpotSource(getDefaultTiers(), DATE_DEBUT_PERIODE);
		assertNotNull(declaration);
		final EtatDeclaration etatDeclaration = declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		assertNotNull(etatDeclaration);
		assertEquals(DATE_QUITTANCEMENT, etatDeclaration.getDateObtention());
		assertEquals(Boolean.FALSE, etatDeclaration.isAnnule());
	}

	@Test
	public void testEventAnnulationEtatRetourNonExiste() throws Exception{

		final String message = createMessageQuittancement(TypeQuittance.ANNULATION);
		listener.onMessage(message, "TEST-" + System.currentTimeMillis());

		assertEquals(1 ,evenementExterneDAO.getAll().size());

		DeclarationImpotSource declaration = getDeclarationImpotSource(getDefaultTiers(), DATE_DEBUT_PERIODE);
		assertNotNull(declaration);
		EtatDeclaration etatDeclaration = declaration.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		assertNull(etatDeclaration);
	}

	@Test
	@NotTransactional
	public void testEventAnnulation() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// envoi d'un événement de quittancement
				String message = createMessageQuittancement(TypeQuittance.QUITTANCEMENT);
				listener.onMessage(message, "TEST-" + System.currentTimeMillis());

				// envoi d'un événement qui annule l'événement de quittancement précédent
				message = createMessageQuittancement(TypeQuittance.ANNULATION);
				listener.onMessage(message, "TEST-" + System.currentTimeMillis());

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
	public void testEventAnnulationWithError() throws Exception {
		assertEquals(0, evenementExterneDAO.getAll().size()); // précondition

		// envoi d'un événement d'annulation d'un événement précédent mais qui n'existe pas -> erreur
		String message = createMessageQuittancement(TypeQuittance.ANNULATION);
		listener.onMessage(message, "TEST-" + System.currentTimeMillis());

		final List<EvenementExterne> all = evenementExterneDAO.getAll();
		assertEquals(1, all.size());

		final EvenementExterne event0 = all.get(0);
		assertNotNull(event0);
		assertEquals(EtatEvenementExterne.ERREUR, event0.getEtat());
		assertEquals("La déclaration impôt source sélectionné ne contient pas de retour à annuler.", event0.getErrorMessage());
	}

	@Test
	@NotTransactional
	public void testEvenementQuittancementTwice() throws Exception {
		
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				assertEquals(0, evenementExterneDAO.getAll().size()); // précondition
				return null;
			}
		});

		// envoi d'un premier événement externe avec insertion de l'événement dans la base
		String message = createMessageQuittancement(TypeQuittance.QUITTANCEMENT);
		final String businessId = "TEST-" + System.currentTimeMillis();
		listener.onMessage(message, businessId);

		// vérification que un seul événement a été inséré dans la base
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> all = evenementExterneDAO.getAll();
				assertEquals(1, all.size());
				assertQuittanceLR(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(), null, DATE_DEBUT_PERIODE, DATE_FIN_PERIODE,
						ch.vd.uniregctb.evenement.externe.TypeQuittance.QUITTANCEMENT,
						all.get(0));
				return null;
			}
		});

		// envoi du même message une seconde fois
		listener.onMessage(message, businessId);

		// vérification que la base n'a pas changé
		doInTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final List<EvenementExterne> all = evenementExterneDAO.getAll();
				assertEquals(1, all.size());
				assertQuittanceLR(NUMERO_CONTRIBUABLE, EtatEvenementExterne.TRAITE, DATE_QUITTANCEMENT.asJavaDate(), DATE_QUITTANCEMENT.asJavaDate(), null, DATE_DEBUT_PERIODE, DATE_FIN_PERIODE,
						ch.vd.uniregctb.evenement.externe.TypeQuittance.QUITTANCEMENT, all.get(0));
				return null;
			}
		});
	}

	private String createMessageQuittancement(TypeQuittance.Enum quitancement) {
		final EvenementImpotSourceQuittanceDocument doc = createEvenementQuittancement(quitancement);
		return doc.xmlText();
	}
	
	private EvenementImpotSourceQuittanceDocument createEvenementQuittancement(TypeQuittance.Enum quitancement) {
		return service.createEvenementQuittancement(quitancement, NUMERO_CONTRIBUABLE, DATE_DEBUT_PERIODE,
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

	private static void assertQuittanceLR(Long numeroTiers, EtatEvenementExterne etat, Date dateEvenement, Date dateTraitement, String errorMessage, RegDate dateDebut, RegDate dateFin,
	                                      ch.vd.uniregctb.evenement.externe.TypeQuittance type, EvenementExterne event) {
		assertNotNull(event);
		assertNotNull(event.getBusinessId());
		assertEquals(dateTraitement, event.getDateTraitement());
		assertEquals(errorMessage, event.getErrorMessage());
		assertEquals(etat, event.getEtat());
		assertEquals(dateEvenement, event.getDateEvenement());
		
		final QuittanceLR q = (QuittanceLR) event;
		assertEquals(numeroTiers, q.getTiersId());
		assertEquals(dateDebut, q.getDateDebut());
		assertEquals(dateFin, q.getDateFin());
		assertEquals(type, q.getType());
	}
}
