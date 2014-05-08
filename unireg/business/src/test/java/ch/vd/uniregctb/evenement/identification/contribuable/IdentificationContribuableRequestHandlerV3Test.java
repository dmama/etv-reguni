package ch.vd.uniregctb.evenement.identification.contribuable;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.xml.common.v2.PartialDate;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationData;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationResult;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.identification.contribuable.IdentificationContribuableService;
import ch.vd.uniregctb.jms.EsbBusinessCode;
import ch.vd.uniregctb.jms.EsbBusinessException;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentificationContribuableRequestHandlerV3Test extends BusinessTest {

	private IdentificationContribuableRequestHandlerV3 handler;

	public IdentificationContribuableRequestHandlerV3Test() {
		setWantIndexation(true);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		handler = new IdentificationContribuableRequestHandlerV3();
		handler.setIdentCtbService(getBean(IdentificationContribuableService.class, "identCtbService"));
		handler.setTiersService(tiersService);
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

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final IdentificationData data = new IdentificationData(null, null, "Pittet", null, new PartialDate(1980, 1, 24), null);
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Arrays.asList(data));

				final JAXBElement<IdentificationContribuableResponse> jaxbResponse = handler.handle(request, "toto");
				assertNotNull(jaxbResponse);

				final IdentificationContribuableResponse response = jaxbResponse.getValue();
				assertNotNull(response);
				assertNotNull(response.getIdentificationResult());
				assertEquals(1, response.getIdentificationResult().size());

				final IdentificationResult result = response.getIdentificationResult().get(0);
				assertNotNull(result.getErreur());
				assertNull(result.getContribuable());
				assertNull("Pourquoi aucun, il y en a deux...", result.getErreur().getAucun());            // on en a trouvé deux...
				assertNotNull("Pourquoi pas plusieurs ? il y en a deux, non ?", result.getErreur().getPlusieurs());     // on en a trouvé deux...
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

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final IdentificationData data = new IdentificationData(null, null, "Pittet", null, new PartialDate(1980, 1, 24), null);
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Arrays.asList(data));

				final JAXBElement<IdentificationContribuableResponse> jaxbResponse = handler.handle(request, "toto");
				assertNotNull(jaxbResponse);

				final IdentificationContribuableResponse response = jaxbResponse.getValue();
				assertNotNull(response);
				assertNotNull(response.getIdentificationResult());
				assertEquals(1, response.getIdentificationResult().size());

				final IdentificationResult result = response.getIdentificationResult().get(0);
				assertNull(result.getErreur());
				assertNotNull(result.getContribuable());
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
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final IdentificationData data = new IdentificationData(null, null, nom, prenom, null, null);
				final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Arrays.asList(data));
				final JAXBElement<IdentificationContribuableResponse> jaxbResponse = handler.handle(request, "toto");
				assertNotNull(jaxbResponse);

				final IdentificationContribuableResponse response = jaxbResponse.getValue();
				assertNotNull(response);
				assertNotNull(response.getIdentificationResult());
				assertEquals(1, response.getIdentificationResult().size());

				final IdentificationResult result = response.getIdentificationResult().get(0);
				assertNull(result.getErreur());

				final IdentificationResult.Contribuable ctb = result.getContribuable();
				assertNotNull(ctb);
				assertEquals(StringUtils.abbreviate(prenom, 100), ctb.getPrenom());
				assertEquals(StringUtils.abbreviate(nom.replaceAll("\\s+", " "), 100), ctb.getNom());
				return null;
			}
		});
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testDateNaissancePartielleInvalide() throws Exception {
		final PartialDate dateNaissanceBidon = new PartialDate(2000, null, 20);     // cette date ne devrait pas être acceptée... notre XSD est trop lâche...
		final IdentificationData data = new IdentificationData(null, null, "Tartempion", null, dateNaissanceBidon, null);
		final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Arrays.asList(data));
		try {
			handler.handle(request, "toto");
			fail("Aurait dû se plaindre de la date partielle pourrie");
		}
		catch (EsbBusinessException e) {
			assertEquals(EsbBusinessCode.XML_INVALIDE, e.getCode());
			assertTrue(e.getMessage(), e.getMessage().endsWith("Date partielle avec jour connu mais pas le mois : " + dateNaissanceBidon));
		}
	}

	@Test
	public void testMultipleQuestions() throws Exception {

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne... où sont-ils tous partis ?
			}
		});

		final class Ids {
			int ppUn;
			int ppDeux;
			int ppTrois;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ppUn = addNonHabitant("Alphonse", "Baudet", null, Sexe.MASCULIN);
				final PersonnePhysique ppDeux = addNonHabitant("Richard", "Basquette", null, Sexe.MASCULIN);
				final PersonnePhysique ppTrois = addNonHabitant("Albus", "Trumbledaure", null, Sexe.MASCULIN);

				// on crée 150 "Georges Pittet" pour vérifier aussi le cas du trop grand nombre de résultats
				for (int i = 0; i < 150; ++i) {
					addNonHabitant("Georges", "Pittet", null, Sexe.MASCULIN);
				}

				final Ids ids = new Ids();
				ids.ppUn = ppUn.getNumero().intValue();
				ids.ppDeux = ppDeux.getNumero().intValue();
				ids.ppTrois = ppTrois.getNumero().intValue();
				return ids;
			}
		});

		// attente de la fin de l'indexation
		globalTiersIndexer.sync();

		// demande d'identifications multiples
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final int nbGroupes = 13;
				final int tailleGroupe = 5;     // pour chacun des cas et une réponse négative
				final List<IdentificationData> dataList = new ArrayList<>(nbGroupes * tailleGroupe);
				for (int idxGroupe = 0; idxGroupe < nbGroupes; ++idxGroupe) {
					for (int idx = 0; idx < tailleGroupe; ++idx) {
						final String nom;
						final String prenom;
						switch (idx) {
						case 0:
							nom = "Baudet";
							prenom = "Alphonse";
							break;
						case 1:
							nom = "Basquette";
							prenom = "Richard";
							break;
						case 2:
							nom = "Trumbledaure";
							prenom = "Albus";
							break;
						case 3:
							nom = "Peticlou";
							prenom = "Justin";
							break;
						case 4:
							nom = "Pittet";
							prenom = "Georges";
							break;
						default:
							throw new RuntimeException("On a prévu des groupes de 5... s'ils sont plus gros, il y a des choses à changer ici...");
						}

						final IdentificationData data = new IdentificationData(null, null, nom, prenom, null, null);
						dataList.add(data);
					}
				}

				final JAXBElement<IdentificationContribuableResponse> jaxbResponse = handler.handle(new IdentificationContribuableRequest(dataList), "toto");
				assertNotNull(jaxbResponse);

				final IdentificationContribuableResponse response = jaxbResponse.getValue();
				assertNotNull(response);
				assertNotNull(response.getIdentificationResult());
				assertEquals(nbGroupes * tailleGroupe, response.getIdentificationResult().size());

				for (int index = 0; index < nbGroupes * tailleGroupe; ++index) {
					final IdentificationResult result = response.getIdentificationResult().get(index);
					assertNotNull(result);
					switch (index % tailleGroupe) {
					case 0:
						// Alphonse Baudet doit avoir été trouvé
						assertNotNull(Integer.toString(index), result.getContribuable());
						assertNull(Integer.toString(index), result.getErreur());
						assertEquals(Integer.toString(index), ids.ppUn, result.getContribuable().getNumeroContribuableIndividuel());
						break;
					case 1:
						// Richard Basquette doit avoir été trouvé
						assertNotNull(Integer.toString(index), result.getContribuable());
						assertNull(Integer.toString(index), result.getErreur());
						assertEquals(Integer.toString(index), ids.ppDeux, result.getContribuable().getNumeroContribuableIndividuel());
						break;
					case 2:
						// Albus Trumbledaure doit avoir été trouvé
						assertNotNull(Integer.toString(index), result.getContribuable());
						assertNull(Integer.toString(index), result.getErreur());
						assertEquals(Integer.toString(index), ids.ppTrois, result.getContribuable().getNumeroContribuableIndividuel());
						break;
					case 3:
						// Personne ne doit avoir été trouvé (aucun)
						assertNull(Integer.toString(index), result.getContribuable());
						assertNotNull(Integer.toString(index), result.getErreur());
						assertNotNull(Integer.toString(index), result.getErreur().getAucun());
						break;
					case 4:
						// Personne ne doit avoir été trouvé (plusieurs)
						assertNull(Integer.toString(index), result.getContribuable());
						assertNotNull(Integer.toString(index), result.getErreur());
						assertNotNull(Integer.toString(index), result.getErreur().getPlusieurs());
						break;
					default:
						throw new RuntimeException("On a prévu des groupes de 5... s'ils sont plus gros, il y a des choses à changer ici...");
					}
				}
			}
		});
	}
}
