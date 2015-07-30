package ch.vd.uniregctb.evenement.organisation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.mock.DefaultMockServiceOrganisation;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.type.EmetteurEvenementOrganisation;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;
import ch.vd.uniregctb.type.TypeEvenementOrganisation;

public class EvenementOrganisationRecuperateurTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationRecuperateurTest.class);

	private EvenementOrganisationDAO evtOrganisationDao;
	private EvenementOrganisationService evtOrganisationService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtOrganisationDao = getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO");
		evtOrganisationService = getBean(EvenementOrganisationService.class, "evtOrganisationService");
	}

	private EvenementOrganisation addEvent(Long noOrganisation, long id, TypeEvenementOrganisation type, RegDate date, EtatEvenementOrganisation etat,
	                                       EmetteurEvenementOrganisation senderId, @Nullable String refDataEmetteur) {
		final EvenementOrganisation event = new EvenementOrganisation();
		event.setId(id);
		event.setNoOrganisation(noOrganisation);
		event.setType(type);
		event.setDateEvenement(date);
		event.setEtat(etat);
		event.setIdentiteEmetteur(senderId);
		event.setRefDataEmetteur(refDataEmetteur);
		return hibernateTemplate.merge(event);
	}

	private EvenementOrganisationRecuperateur buildRecuperateur(EvenementOrganisationReceptionHandler handler) {
		final EvenementOrganisationDAO evtOrganisationDAO = getBean(EvenementOrganisationDAO.class, "evenementOrganisationDAO");
		final EvenementOrganisationRecuperateurImpl recuperateur = new EvenementOrganisationRecuperateurImpl();
		recuperateur.setEvtOrganisationDAO(evtOrganisationDAO);
		recuperateur.setReceptionHandler(handler);
		recuperateur.setTransactionManager(transactionManager);
		return recuperateur;
	}

	@Test
	public void testBasics() throws Exception {

		final long noOrganisation = 12345678L;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvent(noOrganisation, 1L, TypeEvenementOrganisation.FOSC_NOUVELLE_ENTREPRISE, date(2010, 4, 12), EtatEvenementOrganisation.A_TRAITER, EmetteurEvenementOrganisation.FOSC, null);
				addEvent(noOrganisation, 2L, TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE, date(2010, 4, 12), EtatEvenementOrganisation.A_TRAITER, EmetteurEvenementOrganisation.FOSC, null);
				addEvent(noOrganisation, 3L, TypeEvenementOrganisation.FOSC_NOUVELLE_SUCCURSALE, date(2010, 4, 12), EtatEvenementOrganisation.TRAITE, EmetteurEvenementOrganisation.FOSC, null);
				return null;
			}
		});

		final List<EvenementOrganisation> found = new ArrayList<>();
		final EvenementOrganisationReceptionHandler handler = new EvenementOrganisationReceptionHandler() {
			@Override
			public EvenementOrganisation saveIncomingEvent(EvenementOrganisation event) {
				throw new RuntimeException("Should not be called!");
			}

			@Override
			public EvenementOrganisation handleEvent(EvenementOrganisation event, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
				found.add(event);
				return event;
			}
		};

		final EvenementOrganisationRecuperateur recuperateur = buildRecuperateur(handler);
		recuperateur.recupererEvenementsOrganisation();

		Assert.assertEquals(2, found.size());

		boolean foundOne = false;
		boolean foundTwo = false;
		for (EvenementOrganisation e : found) {
			if (e.getId() == 1L) {
				Assert.assertFalse(foundOne);
				foundOne = true;
			}
			else if (e.getId() == 2L) {
				Assert.assertFalse(foundTwo);
				foundTwo = true;
			}
		}
		Assert.assertTrue(foundOne);
		Assert.assertTrue(foundTwo);
	}

	/**
	 * [SIFISC-9534]
	 */ // FIXME: Probablement pas a garder, sauf a etre repris pour faire autre chose
	@Ignore
	@Test
	public void testRecuperationNumeroIndividuParDependances() throws Exception {

		final long noOrganisation = 101202100L;

		serviceOrganisation.setUp(new DefaultMockServiceOrganisation());

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvent(noOrganisation, 1L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2015, 7, 20), EtatEvenementOrganisation.TRAITE, EmetteurEvenementOrganisation.FOSC, null);
				addEvent(noOrganisation, 2L, TypeEvenementOrganisation.FOSC_AUTRE_MUTATION, date(2010, 7, 30), EtatEvenementOrganisation.A_TRAITER, EmetteurEvenementOrganisation.FOSC, null);
				return null;
			}
		});

		final EvenementOrganisationReceptionHandlerImpl handler = new EvenementOrganisationReceptionHandlerImpl() {
			@Override
			public void demanderTraitementQueue(long noOrganisation, EvenementOrganisationProcessingMode mode) {
				Assert.fail("Le récupérateur ne doit plus lancer le traitement des événements à relancer");
			}
		};
		handler.setTransactionManager(transactionManager);
		handler.setEvtOrganisationDAO(evtOrganisationDao);
		handler.afterPropertiesSet();

		final EvenementOrganisationRecuperateur recuperateur = buildRecuperateur(handler);
		recuperateur.recupererEvenementsOrganisation();

		// vérification du numéro d'organisation présent dans l'événement qui n'en avait pas jusqu'ici
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementOrganisation ech = evtOrganisationDao.get(2L);
				Assert.assertNotNull(ech);
				Assert.assertEquals(noOrganisation, ech.getNoOrganisation());
				Assert.assertEquals(EtatEvenementOrganisation.A_TRAITER, ech.getEtat());
				return null;
			}
		});
	}
}
