package ch.vd.uniregctb.supergra;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.ModelAndView;

import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.supergra.delta.AddSubEntity;
import ch.vd.uniregctb.supergra.delta.AttributeUpdate;
import ch.vd.uniregctb.supergra.delta.Delta;
import ch.vd.uniregctb.supergra.view.CollectionView;
import ch.vd.uniregctb.supergra.view.EntityView;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class SuperGraControllerTest extends WebTest {

	SuperGraEntityController entityController;
	SuperGraCollectionController collController;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		entityController = getBean(SuperGraEntityController.class, "superGraEntityController");
		collController = getBean(SuperGraCollectionController.class, "superGraCollectionController");
	}

	/**
	 * [UNIREG-3160] Vérifie que l'ajout d'un rapport d'appartenance ménage fonctionne correctement (pas d'exception 'TransientObjectException' notamment)
	 */
	@Test
	public void testAjouterRapportAppartenanceMenage() throws Exception {

		class Ids {
			long olivier;
			long menage;
			long rapport;
		}
		final Ids ids = new Ids();

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique olivier = addNonHabitant("Olivier", "RockFeller", date(1950, 1, 1), Sexe.MASCULIN);
				ids.olivier = olivier.getId();
				final MenageCommun menage = (MenageCommun) hibernateTemplate.merge(new MenageCommun());
				ids.menage = menage.getId();
				return null;
			}
		});

		{
			request.setMethod("GET");
			request.removeAllParameters();
			request.addParameter("id", String.valueOf(ids.olivier));
			request.addParameter("class", EntityType.Tiers.name());
			request.addParameter("name", "rapportsSujet");
			final ModelAndView mav = collController.handleRequest(request, response);
			final CollectionView view = (CollectionView) mav.getModel().get(collController.getCommandName());
			assertNotNull(view);

			// On n'a encore rien ajouté -> pas de delta
			final SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
			assertNotNull(session);
			assertEquals(0, session.deltaSize());
		}

		// On ajoute un rapport d'appartenance ménage
		{
			request.setMethod("POST");
			request.removeAllParameters();
			request.addParameter("newClass", AppartenanceMenage.class.getName());
			request.addParameter("add", "Ajouter");
			request.addParameter("dateFin", "29.11.2010"); // <-- date de fin renseignée
			collController.handleRequest(request, response);

			// on devrait maintenant avoir l'ajout de l'appartenance ménage
			final SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
			assertNotNull(session);
			assertEquals(1, session.deltaSize());

			final AddSubEntity addSubEntity = (AddSubEntity) session.getDeltas().get(0);
			assertEquals(new EntityKey(EntityType.Tiers, ids.olivier), addSubEntity.getKey());
			assertEquals("rapportsSujet", addSubEntity.getCollName());
			assertEquals(AppartenanceMenage.class, addSubEntity.getSubClass());

			ids.rapport = addSubEntity.getId();
		}

		// On affichage la page d'édition du nouveau rapport ménage
		{
			request.setMethod("GET");
			request.removeAllParameters();
			request.addParameter("id", String.valueOf(ids.rapport));
			request.addParameter("class", EntityType.RapportEntreTiers.name());
			final ModelAndView mav = entityController.handleRequest(request, response);
			final EntityView view = (EntityView) mav.getModel().get(entityController.getCommandName());
			assertNotNull(view);

			// on devrait toujours avoir l'ajout de l'appartenance ménage
			final SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
			assertNotNull(session);
			assertEquals(1, session.deltaSize());
		}

		// On renseigne les différents attributs du rapport ménage
		{
			request.setMethod("POST");
			request.removeAllParameters();
			request.addParameter("id", String.valueOf(ids.rapport));
			request.addParameter("class", EntityType.RapportEntreTiers.name());
			request.addParameter("attributes[4].value", "01.01.2001"); // dateDebut
			request.addParameter("attributes[10].value", Long.toString(ids.menage)); // objetId
			entityController.handleRequest(request, response);

			// on devrait maintenant avoir 3 deltas
			final SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
			assertNotNull(session);
			assertEquals(3, session.deltaSize());

			final List<Delta> deltas = session.getDeltas();
			assertNotNull(deltas);

			// 1. l'ajout du rapport ménage
			final AddSubEntity addSubEntity = (AddSubEntity) deltas.get(0);
			assertEquals(new EntityKey(EntityType.Tiers, ids.olivier), addSubEntity.getKey());
			assertEquals("rapportsSujet", addSubEntity.getCollName());
			assertEquals(AppartenanceMenage.class, addSubEntity.getSubClass());

			// 2. le renseignement de la date de début
			final AttributeUpdate updateDateDebut = (AttributeUpdate) deltas.get(1);
			assertEquals(new EntityKey(EntityType.RapportEntreTiers, ids.rapport), updateDateDebut.getKey());
			assertEquals("dateDebut", updateDateDebut.getName());
			assertNull(updateDateDebut.getOldValue());
			assertEquals(date(2001, 1, 1), updateDateDebut.getNewValue());

			// 3. le renseignement de l'id du ménage
			final AttributeUpdate updateObjectId = (AttributeUpdate) deltas.get(2);
			assertEquals(new EntityKey(EntityType.RapportEntreTiers, ids.rapport), updateObjectId.getKey());
			assertEquals("objetId", updateObjectId.getName());
			assertNull(updateObjectId.getOldValue());
			assertEquals(new EntityKey(EntityType.Tiers, ids.menage), updateObjectId.getNewValue());
		}

		// On sauvegarde toutes les modifications
		{
			request.setMethod("POST");
			request.removeAllParameters();
			request.addParameter("id", String.valueOf(ids.rapport));
			request.addParameter("class", EntityType.RapportEntreTiers.name());
			request.addParameter("commitAll", "Sauvegarder");
			entityController.handleRequest(request, response);

			// on ne devrait plus avoir de deltas, maintenant
			final SuperGraSession session = (SuperGraSession) request.getSession().getAttribute("superGraSession");
			assertNotNull(session);
			assertEquals(0, session.deltaSize());
		}

		// On s'assure que le rapport d'appartenance ménage existe bien en base de données
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique olivier = (PersonnePhysique) hibernateTemplate.get(PersonnePhysique.class, ids.olivier);
				assertNotNull(olivier);

				final MenageCommun menage = (MenageCommun) hibernateTemplate.get(MenageCommun.class, ids.menage);
				assertNotNull(menage);

				final Set<RapportEntreTiers> rapportSujet = olivier.getRapportsSujet();
				assertNotNull(rapportSujet);
				assertEquals(1, rapportSujet.size());

				final RapportEntreTiers rapport = rapportSujet.iterator().next();
				assertNotNull(rapport);
				assertEquals(olivier.getId(), rapport.getSujetId());
				assertEquals(menage.getId(), rapport.getObjetId());
				assertEquals(date(2001, 1, 1), rapport.getDateDebut());
				assertNull(rapport.getDateFin());
				return null;
			}
		});
	}
}
