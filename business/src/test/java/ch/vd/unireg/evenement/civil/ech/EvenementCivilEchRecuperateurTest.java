package ch.vd.unireg.evenement.civil.ech;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeEvenementCivilEch;

public class EvenementCivilEchRecuperateurTest extends BusinessTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchRecuperateurTest.class);

	private EvenementCivilEchDAO evtCivilDao;
	private EvenementCivilEchService evtCivilService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		evtCivilDao = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		evtCivilService = getBean(EvenementCivilEchService.class, "evtCivilEchService");
	}

	private EvenementCivilEch addEvent(@Nullable Long noIndividu, long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate date, EtatEvenementCivil etat, @Nullable Long refEvtId) {
		final EvenementCivilEch event = new EvenementCivilEch();
		event.setId(id);
		event.setNumeroIndividu(noIndividu);
		event.setType(type);
		event.setAction(action);
		event.setDateEvenement(date);
		event.setEtat(etat);
		event.setRefMessageId(refEvtId);
		return hibernateTemplate.merge(event);
	}

	private EvenementCivilEchRecuperateur buildRecuperateur(EvenementCivilEchReceptionHandler handler) {
		final EvenementCivilEchDAO evtCivilDAO = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		final EvenementCivilEchRecuperateurImpl recuperateur = new EvenementCivilEchRecuperateurImpl();
		recuperateur.setEvtCivilDAO(evtCivilDAO);
		recuperateur.setReceptionHandler(handler);
		recuperateur.setTransactionManager(transactionManager);
		return recuperateur;
	}

	@Test
	public void testBasics() throws Exception {

		final long noIndividu = 1748265328L;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvent(null, 1L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.A_TRAITER, null);
				addEvent(noIndividu, 2L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.A_TRAITER, null);
				addEvent(noIndividu, 3L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.TRAITE, null);
				return null;
			}
		});

		final List<EvenementCivilEch> found = new ArrayList<>();
		final EvenementCivilEchReceptionHandler handler = new EvenementCivilEchReceptionHandler() {
			@Override
			public EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
				throw new RuntimeException("Should not be called!");
			}

			@Override
			public EvenementCivilEch handleEvent(EvenementCivilEch event, EvenementCivilEchProcessingMode mode) throws EvenementCivilException {
				found.add(event);
				return event;
			}
		};

		final EvenementCivilEchRecuperateur recuperateur = buildRecuperateur(handler);
		recuperateur.recupererEvenementsCivil();

		Assert.assertEquals(2, found.size());

		boolean foundOne = false;
		boolean foundTwo = false;
		for (EvenementCivilEch e : found) {
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
	 */
	@Test
	public void testRecuperationNumeroIndividuParDependances() throws Exception {

		final long noIndividu = 1748265328L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = createIndividu(noIndividu, null, "Tartempion", "Mo", Sexe.MASCULIN);
				addIndividuAfterEvent(1L, ind, date(2010, 4, 12), TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, null);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvent(noIndividu, 1L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.TRAITE, null);
				addEvent(null, 2L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.CORRECTION, date(2010, 4, 12), EtatEvenementCivil.A_TRAITER, 1L);
				return null;
			}
		});

		final EvenementCivilEchReceptionHandlerImpl handler = new EvenementCivilEchReceptionHandlerImpl() {
			@Override
			public void demanderTraitementQueue(long noIndividu, EvenementCivilEchProcessingMode mode) {
				Assert.fail("Le récupérateur ne doit plus lancer le traitement des événements à relancer");
			}
		};
		handler.setTransactionManager(transactionManager);
		handler.setEvtCivilDAO(evtCivilDao);
		handler.setEvtCivilService(evtCivilService);
		handler.afterPropertiesSet();

		final EvenementCivilEchRecuperateur recuperateur = buildRecuperateur(handler);
		recuperateur.recupererEvenementsCivil();

		// vérification du numéro d'individu présent dans l'événement qui n'en avait pas jusqu'ici
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch ech = evtCivilDao.get(2L);
				Assert.assertNotNull(ech);
				Assert.assertEquals((Long) noIndividu, ech.getNumeroIndividu());
				Assert.assertEquals(EtatEvenementCivil.A_TRAITER, ech.getEtat());
				return null;
			}
		});
	}
}
