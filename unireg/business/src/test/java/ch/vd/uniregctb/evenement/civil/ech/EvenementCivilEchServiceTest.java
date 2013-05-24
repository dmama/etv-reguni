package ch.vd.uniregctb.evenement.civil.ech;

import java.util.List;

import junit.framework.Assert;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.ServiceCivilException;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

public class EvenementCivilEchServiceTest extends BusinessTest {

	protected EvenementCivilEchServiceImpl buildService(boolean collapseReferences) throws Exception {
		final EvenementCivilEchServiceImpl service = new EvenementCivilEchServiceImpl();
		service.setEvenementCivilEchDAO(getBean(EvenementCivilEchDAO.class, "evenementCivilEchDAO"));
		service.setHibernateTemplate(hibernateTemplate);
		service.setServiceCivil(serviceCivil);
		service.setTiersDAO(tiersDAO);
		service.setTiersService(tiersService);
		service.setTransactionManager(transactionManager);
		service.setCollapseReferringEvents(collapseReferences);
		service.afterPropertiesSet();
		return service;
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

		final EvenementCivilEchServiceImpl service = buildService(false);
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

		final EvenementCivilEchServiceImpl service = buildService(false);
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

		final EvenementCivilEchServiceImpl service = buildService(false);
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

		final EvenementCivilEchServiceImpl service = buildService(false);
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
	 * Cas du SIFISC-8300 : un contribuable marié il y a longtemps (mais on ne connaissait pas la date dans le civil jusqu'ici, donc enregistré comme célibataire)
	 * pour lequel on fait une correction de mariage avec ajout de la date maintenant connue... Le flag "habitant" doit rester correct après forçage de l'événement
	 * civil de correction
	 */
	@Test
	public void testForcageVieilEvenementEtFlagHabitant() throws Exception {

		final long noIndividu = 27825764L;
		final RegDate dateNaissance = date(1962, 12, 27);
		final RegDate dateArrivee = date(1993, 4, 12);
		final RegDate dateMariage = date(1986, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Peticou", "Justin", Sexe.MASCULIN);
				marieIndividu(individu, dateMariage);
				addNationalite(individu, MockPays.Suisse, dateNaissance, null);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Aubonne.RueTrevelin, null, dateArrivee, null);
			}
		});

		// mise en place fiscale (avant traitement du mariage)
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				return pp.getNumero();
			}
		});

		// création de l'événement civil de correction mariage (en erreur pour le moment) pour y mettre la bonne date
		final long evtId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				return createEvent(dateMariage, noIndividu, 32673627L, TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.EN_ERREUR, null);
			}
		});

		// vérification de l'état actuel du flag "habitant" du contribuable
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue(pp.isHabitantVD());
				return null;
			}
		});

		final EvenementCivilEchServiceImpl service = buildService(false);

		// forçage de l'événement civil de correction de mariage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				service.forceEvenement(evtId);
				return null;
			}
		});

		// vérification de l'état du flag "habitant" du contribuable après forçage
		// (le contribuable était certes non-habitant au moment de son mariage, mais actuellement, il est bien habitant)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue("Pourquoi le flag habitant a-t-il changé?", pp.isHabitantVD());
				return null;
			}
		});
	}

	private Long createEvent(RegDate dateEvt, Long noIndividu, long evtId, TypeEvenementCivilEch type,
	                         ActionEvenementCivilEch action, EtatEvenementCivil etat, @Nullable Long refMessageId) {
		final EvenementCivilEch evt = new EvenementCivilEch();
		evt.setId(evtId);
		evt.setEtat(etat);
		evt.setNumeroIndividu(noIndividu);
		evt.setType(type);
		evt.setDateEvenement(dateEvt);
		evt.setAction(action);
		evt.setRefMessageId(refMessageId);
		return hibernateTemplate.merge(evt).getId();
	}

	@Test
	public void testConstitutionGroupeReferencesCorrections() throws Exception {

		final long noIndividu = 4236784567L;
		final RegDate dateNaissance = date(1980, 3, 12);
		final RegDate dateNaturalisation = date(1998, 3, 12);
		final RegDate dateMariage = date(2000, 5, 31);

		final EvenementCivilEchServiceImpl service = buildService(true);

		// création de l'individu
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				createIndividu(noIndividu, dateNaissance, "Dupont", "Albert", Sexe.MASCULIN);
			}
		});

		// mise en place des données
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				// cas simple de l'événement traité
				createEvent(dateNaissance, noIndividu, 1L, TypeEvenementCivilEch.NAISSANCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE, null);

				// cas de l'arrivée en erreur
				createEvent(dateNaissance, noIndividu, 2L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);

				// cas d'une naturalisation corrigée en erreur
				createEvent(dateNaturalisation, noIndividu, 3L, TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);
				createEvent(dateNaturalisation, noIndividu, 4L, TypeEvenementCivilEch.NATURALISATION, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.EN_ATTENTE, 3L);

				// cas d'un mariage traité avec correction en erreur
				createEvent(dateMariage, noIndividu, 5L, TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE, null);
				createEvent(dateMariage, noIndividu, 6L, TypeEvenementCivilEch.MARIAGE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.EN_ATTENTE, 5L);

				// cas d'un changement de relation d'annonce dont la correction n'a pas de numéro d'individu
				createEvent(dateMariage, noIndividu, 7L, TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE, null);
				createEvent(dateMariage, null, 8L, TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.A_TRAITER, 7L);
				createEvent(dateMariage, null, 9L, TypeEvenementCivilEch.CHGT_RELATION_ANNONCE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.A_TRAITER, 8L);

				// cas d'une correction de relations dont une correction est traitée mais pas la suivante
				createEvent(dateMariage, noIndividu, 10L, TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);
				createEvent(dateMariage, noIndividu, 11L, TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.TRAITE, 10L);
				createEvent(dateMariage, noIndividu, 12L, TypeEvenementCivilEch.CORR_RELATIONS, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.EN_ERREUR, 11L);

				// cas de la correction d'arrivée (= événement de migration) en erreur
				createEvent(dateMariage, noIndividu, 13L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.EN_ERREUR, -1L);
				return null;
			}
		});

		// vérification du comportement du service
		final List<EvenementCivilEchBasicInfo> infos = service.buildLotEvenementsCivils(noIndividu);
		Assert.assertNotNull(infos);
		Assert.assertEquals(6, infos.size());

		// l'arrivée en erreur (elle est toute seule)
		{
			final EvenementCivilEchBasicInfo info = infos.get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(2L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// naturalisation (un événement et sa correction tous deux en erreur)
		{
			final EvenementCivilEchBasicInfo info = infos.get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(3L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(1, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(4L, refInfo.getId());
			}
		}

		// correction d'arrivée sur un événement inconnu (= migré de Reg-PP, donc non-envoyé à Unireg sous cet identifiant)
		{
			final EvenementCivilEchBasicInfo info = infos.get(2);
			Assert.assertNotNull(info);
			Assert.assertEquals(13L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// mariage (seule la correction est non-traitée)
		{
			final EvenementCivilEchBasicInfo info = infos.get(3);
			Assert.assertNotNull(info);
			Assert.assertEquals(6L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// changement de relation d'annonce (récupération des numéros d'individu d'après les dépendances)
		{
			final EvenementCivilEchBasicInfo info = infos.get(4);
			Assert.assertNotNull(info);
			Assert.assertEquals(8L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(1, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(9L, refInfo.getId());
				Assert.assertEquals(noIndividu, refInfo.getNoIndividu());
			}
		}

		// les corrections de relations (on trouve quand-même la dépendance malgré l'élément traité au milieu de la chaîne)
		{
			final EvenementCivilEchBasicInfo info = infos.get(5);
			Assert.assertNotNull(info);
			Assert.assertEquals(10L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(2, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(11L, refInfo.getId());
			}
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(1);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(12L, refInfo.getId());
			}
		}
	}

	@Test
	public void testConstitutionGroupesReferencesAvecAnnulation() throws Exception {

		final long noIndividu = 21745624L;
		final RegDate dateNaissance = date(1980, 10, 25);
		final RegDate dateArrivee = date(2000, 7, 12);

		final EvenementCivilEchServiceImpl service = buildService(true);

		// création de l'individu
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				createIndividu(noIndividu, dateNaissance, "Dupont", "Albert", Sexe.MASCULIN);
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// annulation d'arrivée déjà traitée
				createEvent(dateArrivee, noIndividu, 1L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.TRAITE, -1L);

				// arrivée traitée annulée
				createEvent(dateArrivee, noIndividu, 2L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE, null);
				createEvent(dateArrivee, noIndividu, 3L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.A_TRAITER, 2L);

				// arrivée en erreur annulée
				createEvent(dateArrivee.addDays(1), noIndividu, 4L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);
				createEvent(dateArrivee.addDays(1), noIndividu, 5L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.A_TRAITER, 4L);

				// arrivée corrigée et annulée
				createEvent(dateArrivee.addDays(2), noIndividu, 6L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);
				createEvent(dateArrivee.addDays(2), noIndividu, 7L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.A_TRAITER, 6L);
				createEvent(dateArrivee.addDays(2), noIndividu, 8L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.A_TRAITER, 6L);

				// arrivée corrigée, correction annulée
				createEvent(dateArrivee.addDays(3), noIndividu, 9L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.EN_ERREUR, null);
				createEvent(dateArrivee.addDays(3), noIndividu, 10L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.A_TRAITER, 9L);
				createEvent(dateArrivee.addDays(3), noIndividu, 11L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.A_TRAITER, 10L);

				// arrivée traitée, correction et annulation de l'arrivée en erreur
				createEvent(dateArrivee.addDays(4), noIndividu, 12L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.PREMIERE_LIVRAISON, EtatEvenementCivil.TRAITE, null);
				createEvent(dateArrivee.addDays(4), noIndividu, 13L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.CORRECTION, EtatEvenementCivil.A_TRAITER, 12L);
				createEvent(dateArrivee.addDays(4), noIndividu, 14L, TypeEvenementCivilEch.ARRIVEE, ActionEvenementCivilEch.ANNULATION, EtatEvenementCivil.A_TRAITER, 12L);

				return null;
			}
		});

		// vérification du comportement du service
		final List<EvenementCivilEchBasicInfo> infos = service.buildLotEvenementsCivils(noIndividu);
		Assert.assertNotNull(infos);
		Assert.assertEquals(7, infos.size());

		// l'annulation d'arrivée traitée
		{
			final EvenementCivilEchBasicInfo info = infos.get(0);
			Assert.assertNotNull(info);
			Assert.assertEquals(3L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// arrivée en erreur et annulée
		{
			final EvenementCivilEchBasicInfo info = infos.get(1);
			Assert.assertNotNull(info);
			Assert.assertEquals(4L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(1, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(5L, refInfo.getId());
			}
		}

		// arrivée corrigée puis annulée
		{
			final EvenementCivilEchBasicInfo info = infos.get(2);
			Assert.assertNotNull(info);
			Assert.assertEquals(6L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(2, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(7L, refInfo.getId());
			}
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(1);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(8L, refInfo.getId());
			}
		}

		// arrivée corrigée, correction annulée -> partie arrivée seule
		{
			final EvenementCivilEchBasicInfo info = infos.get(3);
			Assert.assertNotNull(info);
			Assert.assertEquals(9L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// arrivée corrigée, correction annulée -> partie correction annulée
		{
			final EvenementCivilEchBasicInfo info = infos.get(4);
			Assert.assertNotNull(info);
			Assert.assertEquals(10L, info.getId());

			final List<EvenementCivilEchBasicInfo> referers = info.getSortedReferers();
			Assert.assertNotNull(referers);
			Assert.assertEquals(1, referers.size());
			{
				final EvenementCivilEchBasicInfo refInfo = referers.get(0);
				Assert.assertNotNull(refInfo);
				Assert.assertEquals(11L, refInfo.getId());
			}
		}

		// arrivée traitée, correction et annulation de l'arrivée en erreur -> partie correction seule
		{
			final EvenementCivilEchBasicInfo info = infos.get(5);
			Assert.assertNotNull(info);
			Assert.assertEquals(13L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}

		// arrivée traitée, correction et annulation de l'arrivée en erreur -> partie annulation seule
		{
			final EvenementCivilEchBasicInfo info = infos.get(6);
			Assert.assertNotNull(info);
			Assert.assertEquals(14L, info.getId());
			Assert.assertEquals(0, info.getReferers().size());
		}
	}
}
