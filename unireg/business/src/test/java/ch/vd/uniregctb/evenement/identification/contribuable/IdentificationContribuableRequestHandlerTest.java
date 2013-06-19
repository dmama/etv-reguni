package ch.vd.uniregctb.evenement.identification.contribuable;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.unireg.xml.common.v1.PartialDate;
import ch.vd.unireg.xml.event.identification.request.v2.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.response.v2.IdentificationContribuableResponse;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IdentificationContribuableRequestHandlerTest extends BusinessTest {

	private IdentificationContribuableRequestHandler handler;

	public IdentificationContribuableRequestHandlerTest() {
		setWantIndexation(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		handler = new IdentificationContribuableRequestHandler();
		handler.setEsbTemplate(null);       // comme ça, ça pête si on envoie un message dans l'ESB, ce que ce test n'est pas censé faire...
		handler.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		handler.setTiersService(tiersService);
		handler.afterPropertiesSet();
	}

	/**
	 * Le principe est d'avoir plus de 100 contribuables avec le même nom de famille, mais seulement deux qui ont la bonne date de naissance
	 */
	@Test
	public void testGrandNombreResultatsPremierePhaseSeTermineEnPlusieurs() throws Exception {

		final int nbCtb = 150;

		// création des contribuables avec le même nom et une date de naissance différente à chaque fois
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (int i = 0 ; i < nbCtb ; ++ i) {
					addNonHabitant(null, "Pittet", date(1980, 1, 1).addDays(i / 2), Sexe.MASCULIN);
				}
				return null;
			}
		});

		// attente de la fin de l'indexation
		globalTiersIndexer.sync();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
				request.setNom("Pittet");
				request.setDateNaissance(new PartialDate(1980, 1, 24));

				final IdentificationContribuableResponse response = handler.handle(request, "toto");
				assertNotNull(response);
				assertNotNull(response.getErreur());
				assertNull(response.getContribuable());
				assertNull("Pourquoi aucun, il y en a deux...", response.getErreur().getAucun());            // on en a trouvé deux...
				assertNotNull("Pourquoi pas plusieurs ? il y en a deux, non ?", response.getErreur().getPlusieurs());     // on en a trouvé deux...
				return null;
			}
		});
	}

	/**
	 * Le principe est d'avoir plus de 100 contribuables avec le même nom de famille, mais seulement un qui a la bonne date de naissance
	 */
	@Test
	public void testGrandNombreResultatsPremierePhaseSeTermineEnUnSeul() throws Exception {

		final int nbCtb = 150;

		// création des contribuables avec le même nom et une date de naissance différente à chaque fois
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				for (int i = 0 ; i < nbCtb ; ++ i) {
					addNonHabitant(null, "Pittet", date(1980, 1, 1).addDays(i), Sexe.MASCULIN);
				}
				return null;
			}
		});

		// attente de la fin de l'indexation
		globalTiersIndexer.sync();

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest();
				request.setNom("Pittet");
				request.setDateNaissance(new PartialDate(1980, 1, 24));

				final IdentificationContribuableResponse response = handler.handle(request, "toto");
				assertNotNull(response);
				assertNull(response.getErreur());
				assertNotNull(response.getContribuable());
				return null;
			}
		});
	}

	/**
	 * Les champs très grands, s'ils ne sont pas traités correctement, pourraient causer l'invalidité du message de réponse
	 */
	@Test
	public void testNomTresGrand() throws Exception {

		final String prenom = "Marie-Madeleine Albertine Françoise Blandine Catherine Raphaëlle Renée Céline Charlotte Nicole Lisa Laetitia";
		final String nom = "De la sublime croix comme on n'en voit    plus du tout depuis beaucoup trop longtemps par chez nous et ailleurs";
		assertTrue(prenom.length() > 100);
		assertTrue(nom.length() > 100);

		// création du contribuable
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				addNonHabitant(prenom, nom, null, Sexe.FEMININ);
				return null;
			}
		});

		// attente de la fin de l'indexation
		globalTiersIndexer.sync();

		// identification
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest(null, null, nom, prenom, null, null);
				final IdentificationContribuableResponse response = handler.handle(request, "toto");
				assertNotNull(response);
				assertNull(response.getErreur());

				final IdentificationContribuableResponse.Contribuable ctb = response.getContribuable();
				assertNotNull(ctb);
				assertEquals(StringUtils.abbreviate(prenom, 100), ctb.getPrenom());
				assertEquals(StringUtils.abbreviate(nom.replaceAll("\\s+", " "), 100), ctb.getNom());
				return null;
			}
		});
	}
}
