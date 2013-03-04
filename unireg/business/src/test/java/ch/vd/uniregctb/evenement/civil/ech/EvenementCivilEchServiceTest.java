package ch.vd.uniregctb.evenement.civil.ech;

import junit.framework.Assert;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchServiceTest extends BusinessTest {

	private EvenementCivilEchServiceImpl service;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		service = new EvenementCivilEchServiceImpl();
		service.setEvenementCivilEchDAO(getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO"));
		service.setHibernateTemplate(hibernateTemplate);
		service.setServiceCivil(serviceCivil);
		service.setTiersDAO(tiersDAO);
		service.setTiersService(tiersService);
		service.setTransactionManager(transactionManager);
		service.afterPropertiesSet();
	}

	@Test
	public void testGetNumeroIndividuPourEventAvecNumeroDejaConnu() throws Exception {

		final long noIndividu = 436784252347L;
		final long noEvt = 3234124L;
		final TypeEvenementCivilEch typeEvt = TypeEvenementCivilEch.ARRIVEE;
		final RegDate dateEvenement = RegDate.get();
		final ActionEvenementCivilEch actionEvt = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = createIndividu(noIndividu, null, "Leblanc", "Claude", Sexe.MASCULIN);
				addIndividuAfterEvent(noEvt, individu, dateEvenement, typeEvt, actionEvt, null);
			}

			@Override
			public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
				throw new RuntimeException("Ne devrait pas être appelé, le numéro est déjà connu");
			}

			@Override
			public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
				throw new RuntimeException("Ne devrait pas être appelé, le numéro est déjà connu");
			}
		});

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(noEvt);
		evt.setAction(actionEvt);
		evt.setDateEvenement(dateEvenement);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setType(typeEvt);
		evt.setNumeroIndividu(noIndividu);
		final long noIndividuDepuisEvt = service.getNumeroIndividuPourEvent(evt);
		Assert.assertEquals(noIndividu, noIndividuDepuisEvt);
	}

	/**
	 * A l'arrivée d'un nouvel événement civil, il faut établir quel individu est concerné, d'abord en appelant getIndividuAfterEvent
	 * du service civil)
	 */
	@Test
	public void testGetNumeroIndividuPourEventAvecGetIndividuAfterEvent() throws Exception {

		final long noIndividu = 436784252347L;
		final long noEvt = 3234124L;
		final TypeEvenementCivilEch typeEvt = TypeEvenementCivilEch.ARRIVEE;
		final RegDate dateEvenement = RegDate.get();
		final ActionEvenementCivilEch actionEvt = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

		final MutableBoolean called = new MutableBoolean(false);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = createIndividu(noIndividu, null, "Leblanc", "Claude", Sexe.MASCULIN);
				addIndividuAfterEvent(noEvt, individu, dateEvenement, typeEvt, actionEvt, null);
			}

			@Override
			public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
				called.setValue(true);
				return super.getIndividuAfterEvent(eventId);
			}

			@Override
			public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
				throw new RuntimeException("Ne devrait pas être appelé, le getIndividuAfterEvent aurait dû suffire");
			}
		});

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(noEvt);
		evt.setAction(actionEvt);
		evt.setDateEvenement(dateEvenement);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setType(typeEvt);
		final long noIndividuDepuisEvt = service.getNumeroIndividuPourEvent(evt);
		Assert.assertEquals(noIndividu, noIndividuDepuisEvt);
		Assert.assertTrue(called.booleanValue());
	}

	/**
	 * A l'arrivée d'un nouvel événement civil, il faut établir quel individu est concerné (si le getIndividuAfterEvent ne renvoie
	 * rien, on appelle le getIndividuByEvent du service civil)
	 */
	@Test
	public void testGetNumeroIndividuPourEventAvecGetIndividuByEventNullResponse() throws Exception {
		final long noIndividu = 436784252347L;
		final long noEvt = 3234124L;
		final TypeEvenementCivilEch typeEvt = TypeEvenementCivilEch.ARRIVEE;
		final RegDate dateEvenement = RegDate.get();
		final ActionEvenementCivilEch actionEvt = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

		final MutableBoolean called = new MutableBoolean(false);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = createIndividu(noIndividu, null, "Leblanc", "Claude", Sexe.MASCULIN);
				addIndividuAfterEvent(noEvt, individu, dateEvenement, typeEvt, actionEvt, null);
			}

			@Override
			public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
				// pas de réponse par ce canal
				return null;
			}

			@Override
			public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
				called.setValue(true);
				final IndividuApresEvenement indApres = super.getIndividuAfterEvent(evtId);
				return indApres.getIndividu();
			}
		});

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(noEvt);
		evt.setAction(actionEvt);
		evt.setDateEvenement(dateEvenement);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setType(typeEvt);
		final long noIndividuDepuisEvt = service.getNumeroIndividuPourEvent(evt);
		Assert.assertEquals(noIndividu, noIndividuDepuisEvt);
		Assert.assertTrue(called.booleanValue());
	}

	/**
	 * A l'arrivée d'un nouvel événement civil, il faut établir quel individu est concerné (si le getIndividuAfterEvent ne renvoie
	 * rien, on appelle le getIndividuByEvent du service civil)
	 */
	@Test
	public void testGetNumeroIndividuPourEventAvecGetIndividuByEventException() throws Exception {
		final long noIndividu = 436784252347L;
		final long noEvt = 3234124L;
		final TypeEvenementCivilEch typeEvt = TypeEvenementCivilEch.ARRIVEE;
		final RegDate dateEvenement = RegDate.get();
		final ActionEvenementCivilEch actionEvt = ActionEvenementCivilEch.PREMIERE_LIVRAISON;

		final MutableBoolean called = new MutableBoolean(false);
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = createIndividu(noIndividu, null, "Leblanc", "Claude", Sexe.MASCULIN);
				addIndividuAfterEvent(noEvt, individu, dateEvenement, typeEvt, actionEvt, null);
			}

			@Override
			public IndividuApresEvenement getIndividuAfterEvent(long eventId) {
				throw new ServiceCivilException("Boom !");
			}

			@Override
			public Individu getIndividuByEvent(long evtId, AttributeIndividu... parties) throws ServiceCivilException {
				called.setValue(true);
				final IndividuApresEvenement indApres = super.getIndividuAfterEvent(evtId);
				return indApres.getIndividu();
			}
		});

		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(noEvt);
		evt.setAction(actionEvt);
		evt.setDateEvenement(dateEvenement);
		evt.setEtat(EtatEvenementCivil.A_TRAITER);
		evt.setType(typeEvt);
		final long noIndividuDepuisEvt = service.getNumeroIndividuPourEvent(evt);
		Assert.assertEquals(noIndividu, noIndividuDepuisEvt);
		Assert.assertTrue(called.booleanValue());
	}
}
