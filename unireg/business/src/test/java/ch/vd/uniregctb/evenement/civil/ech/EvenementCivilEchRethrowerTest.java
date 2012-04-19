package ch.vd.uniregctb.evenement.civil.ech;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchRethrowerTest extends BusinessTest {

	private EvenementCivilEch addEvent(@Nullable Long noIndividu, long id, TypeEvenementCivilEch type, ActionEvenementCivilEch action, RegDate date, EtatEvenementCivil etat) {
		final EvenementCivilEch event = new EvenementCivilEch();
		event.setId(id);
		event.setNumeroIndividu(noIndividu);
		event.setType(type);
		event.setAction(action);
		event.setDateEvenement(date);
		event.setEtat(etat);
		return hibernateTemplate.merge(event);
	}

	private EvenementCivilEchRethrower buildRethrower(EvenementCivilEchReceptionHandler handler) {
		final EvenementCivilEchDAO evtCivilDAO = getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO");
		final EvenementCivilEchRethrowerImpl rethrower = new EvenementCivilEchRethrowerImpl();
		rethrower.setEvtCivilDAO(evtCivilDAO);
		rethrower.setReceptionHandler(handler);
		rethrower.setTransactionManager(transactionManager);
		return rethrower;
	}

	@Test
	public void testBasics() throws Exception {

		final long noIndividu = 1748265328L;

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addEvent(null, 1L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.A_TRAITER);
				addEvent(noIndividu, 2L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.A_TRAITER);
				addEvent(noIndividu, 3L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, date(2010, 4, 12), EtatEvenementCivil.TRAITE);
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final List<EvenementCivilEch> found = new ArrayList<EvenementCivilEch>();
				final EvenementCivilEchReceptionHandler handler = new EvenementCivilEchReceptionHandler() {
					@Override
					public EvenementCivilEch saveIncomingEvent(EvenementCivilEch event) {
						throw new RuntimeException("Should not be called!");
					}

					@Override
					public EvenementCivilEch handleEvent(EvenementCivilEch event) throws EvenementCivilException {
						found.add(event);
						return event;
					}
				};

				final EvenementCivilEchRethrower rethrower = buildRethrower(handler);
				rethrower.fetchAndRethrowEvents();

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

				return null;
			}
		});
	}
}
