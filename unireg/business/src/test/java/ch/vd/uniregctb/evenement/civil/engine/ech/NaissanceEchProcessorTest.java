package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

@SuppressWarnings("JavaDoc")
public class NaissanceEchProcessorTest extends AbstractEvenementCivilEchProcessorTest {

	@Test(timeout = 10000L)
	public void testNaissance() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1);

		// le nouveau né apparaît au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Toubo", "Toupeti", true);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long naissanceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NAISSANCE);

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNull(pp);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(naissanceId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				return null;
			}
		});
	}

	/**
	 * En fait, une naissance "civile" nous envoie une naissance et une arrivée
	 */
	@Test(timeout = 10000L)
	public void testNaissancePuisArrivee() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1);

		// le nouveau né apparaît au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Toubo", "Toupeti", true);
			}
		});

		// événement civil (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long naissanceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NAISSANCE);

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNull(pp);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(naissanceId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				return null;
			}
		});

		// on ajoute maintenant une adresse sur le bébé et on génère une arrivée
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockAdresse adresse = MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, dateNaissance, null);
				adresse.setDateDebutValidite(dateNaissance);
				adresse.setTypeAdresse(TypeAdresseCivil.PRINCIPALE);
				individu.getAdresses().add(adresse);
			}
		});
		
		// événement civil d'arrivée
		final long arriveeId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);
				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(arriveeId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				return null;
			}
		});
	}

	/**
	 * En fait, une naissance "civile" nous envoie une naissance et une arrivée, mais les deux peuvent arriver dans l'ordre inverse
	 */
	@Test(timeout = 10000L)
	public void testArriveePuisNaissance() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1);

		// le nouveau né apparaît au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Toubo", "Toupeti", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, dateNaissance, null);
			}
		});

		// événement civil d'arrivée
		final long arriveeId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(456782456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.ARRIVEE);

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNull(pp);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(arriveeId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());

				final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
				Assert.assertNotNull(pp);

				return null;
			}
		});

		// événement civil de naissance (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long naissanceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NAISSANCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(naissanceId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat());
				Assert.assertEquals("Le contribuable correspondant au nouveau-né existe déjà, seuls les événements à destination des applications fiscales ont été envoyés.", evt.getCommentaireTraitement());
				return null;
			}
		});
	}

	/**
	 * En fait, une naissance "civile" nous envoie une naissance et une arrivée, mais les deux peuvent arriver dans l'ordre inverse
	 */
	@Test(timeout = 10000L)
	public void testNaissanceAvecPlusieursTiersCorrespondantExistants() throws Exception {

		final long noIndividu = 54678215611L;
		final RegDate dateNaissance = RegDate.get().addMonths(-1);

		// le nouveau né apparaît au civil
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, dateNaissance, "Toubo", "Toupeti", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Vallorbe.GrandRue, null, dateNaissance, null);
			}
		});

		final class Ids {
			public long id1;
			public long id2;

		}

		// création des tiers pré-existants
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp1 = addHabitant(noIndividu);
				final PersonnePhysique pp2 = addHabitant(noIndividu);
				final Ids ids = new Ids();
				ids.id1 = pp1.getNumero();
				ids.id2 = pp2.getNumero();
				return ids;
			}
		});

		// événement civil de naissance (avec individu déjà renseigné pour ne pas devoir appeler RCPers...)
		final long naissanceId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = new EvenementCivilEch();
				evt.setId(1235563456L);
				evt.setAction(ActionEvenementCivilEch.PREMIERE_LIVRAISON);
				evt.setDateEvenement(dateNaissance);
				evt.setEtat(EtatEvenementCivil.A_TRAITER);
				evt.setNumeroIndividu(noIndividu);
				evt.setType(TypeEvenementCivilEch.NAISSANCE);

				return hibernateTemplate.merge(evt).getId();
			}
		});

		// traitement synchrone de l'événement
		traiterEvenements(noIndividu);

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(naissanceId);
				Assert.assertNotNull(evt);
				Assert.assertEquals(EtatEvenementCivil.EN_ERREUR, evt.getEtat());

				final Set<EvenementCivilEchErreur> erreurs = evt.getErreurs();
				Assert.assertNotNull(erreurs);
				Assert.assertEquals(1, erreurs.size());
				final EvenementCivilEchErreur erreur = erreurs.iterator().next();
				Assert.assertNotNull(erreur);
				Assert.assertEquals(String.format("Plusieurs tiers non-annulés partagent le même numéro d'individu %d ([%d, %d])", noIndividu, ids.id1, ids.id2), erreur.getMessage());
				return null;
			}
		});
	}
}
