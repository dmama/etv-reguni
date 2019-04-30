package ch.vd.unireg.evenement.identification.contribuable;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v3.IdentificationData;
import ch.vd.unireg.xml.event.identification.request.v3.ObjectFactory;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v3.IdentificationResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IdentificationContribuableRequestV3ListenerItTest extends IdentificationContribuableRequestListenerItTest {

	private static String requestToString(IdentificationContribuableRequest request) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		marshaller.marshal(new ObjectFactory().createIdentificationContribuableRequest(request), out);
		return out.toString();
	}

	@Override
	@NotNull
	protected String getHandlerName() {
		return "identificationContribuableRequestHandlerV3";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueOK() throws Exception {

		final RegDate dateNaissance = RegDate.get(1982, 6);     // date partielle, pour le faire au moins une fois

		final long id = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier Vallard", dateNaissance, Sexe.MASCULIN);
			return christophe.getNumero();
		});
		globalTiersIndexer.sync();

		final IdentificationData data = new IdentificationData(7569396525489L, null, "Monnier", "Christophe", null, null, null);
		final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Collections.singletonList(data));

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);
		final IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getIdentificationResult());
		assertEquals(1, response.getIdentificationResult().size());

		final IdentificationResult result = response.getIdentificationResult().get(0);
		if (result.getErreur() != null) {
			fail(result.getErreur().toString());
		}
		assertNull(result.getId());

		final IdentificationResult.Contribuable infoCtb = result.getContribuable();
		assertNotNull(infoCtb);
		assertEquals(id, infoCtb.getNumeroContribuableIndividuel());
		assertEquals("Monnier Vallard", infoCtb.getNom());
		assertEquals("Christophe", infoCtb.getPrenom());
		assertEquals(dateNaissance, DataHelper.xmlToCore(infoCtb.getDateNaissance()));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiquePlusieurs() throws Exception {

		final Long id1 = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
		});
		final Long id2 = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1964, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
		});

		globalTiersIndexer.sync();

		final IdentificationData data = new IdentificationData(null, null, "Monnier", "Christophe", null, null, "Monnier, tu dors...?");
		final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Collections.singletonList(data));

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getIdentificationResult());
		assertEquals(1, response.getIdentificationResult().size());

		final IdentificationResult result = response.getIdentificationResult().get(0);
		assertNotNull(result.getErreur());
		assertNull(result.getContribuable());
		assertNotNull(result.getErreur().getPlusieurs());
		assertEquals("Monnier, tu dors...?", result.getId());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueAucun() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
			return christophe.getNumero();
		});

		globalTiersIndexer.sync();

		final IdentificationData data = new IdentificationData(null, null, "Adam", "Raphaël", null, null, "Raphaello");
		final IdentificationContribuableRequest request = new IdentificationContribuableRequest(Collections.singletonList(data));

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getIdentificationResult());
		assertEquals(1, response.getIdentificationResult().size());

		final IdentificationResult result = response.getIdentificationResult().get(0);
		assertNotNull(result.getErreur());
		assertNull(result.getContribuable());
		assertNotNull(result.getErreur().getAucun());
		assertEquals("Raphaello", result.getId());
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeMultiple() throws Exception {

		final class Ids {
			int ppUn;
			int ppDeux;
			int ppTrois;
		}

		final Ids ids = doInNewTransaction(status -> {
			final PersonnePhysique ppUn = addNonHabitant("Alphonse", "Baudet", null, Sexe.MASCULIN);
			final PersonnePhysique ppDeux = addNonHabitant("Richard", "Basquette", null, Sexe.MASCULIN);
			final PersonnePhysique ppTrois = addNonHabitant("Albus", "Trumbledaure", null, Sexe.MASCULIN);

			// on crée 150 "Georges Pittet" pour vérifier aussi le cas du trop grand nombre de résultats
			for (int i = 0; i < 150; ++i) {
				addNonHabitant("Georges", "Pittet", null, Sexe.MASCULIN);
			}

			final Ids ids1 = new Ids();
			ids1.ppUn = ppUn.getNumero().intValue();
			ids1.ppDeux = ppDeux.getNumero().intValue();
			ids1.ppTrois = ppTrois.getNumero().intValue();
			return ids1;
		});

		globalTiersIndexer.sync();

		final int nbGroupes = 13;
		final int tailleGroupe = 5;     // pour chacun des cas et une réponse négative (aucun) et une réponse négative (plusieurs)
		final List<IdentificationData> identificationDataList = new ArrayList<>(nbGroupes * tailleGroupe);
		for (int idxGroupe = 0; idxGroupe < nbGroupes; ++idxGroupe) {
			for (int idx = 0; idx < tailleGroupe; ++idx) {
				final String nom;
				final String prenom;
				final String id;
				switch (idx) {
				case 0:
					nom = "Baudet";
					prenom = "Alphonse";
					id = "AB";
					break;
				case 1:
					nom = "Basquette";
					prenom = "Richard";
					id = "Riri";
					break;
				case 2:
					nom = "Trumbledaure";
					prenom = "Albus";
					id = "Shazam";
					break;
				case 3:
					nom = "Peticlou";
					prenom = "Justin";
					id = null;
					break;
				case 4:
					nom = "Pittet";
					prenom = "Georges";
					id = "pg";
					break;
				default:
					throw new RuntimeException("On a prévu des groupes de 5... s'ils sont plus gros, il y a des choses à changer ici...");
				}

				final IdentificationData data = new IdentificationData(null, null, nom, prenom, null, null, id);
				identificationDataList.add(data);
			}
		}

		final IdentificationContribuableRequest request = new IdentificationContribuableRequest(identificationDataList);

		// Envoie le message
		sendTextMessage(getInputQueue(), requestToString(request), getOutputQueue());

		final EsbMessage message = getEsbMessage(getOutputQueue());
		assertNotNull(message);

		final IdentificationContribuableResponse response = parseResponse(message);
		assertNotNull(response);
		assertNotNull(response.getIdentificationResult());
		assertEquals(nbGroupes * tailleGroupe, response.getIdentificationResult().size());

		for (int index = 0 ; index < nbGroupes * tailleGroupe; ++ index) {
			final IdentificationResult result = response.getIdentificationResult().get(index);
			assertNotNull(result);
			switch (index % tailleGroupe) {
			case 0:
				// Alphonse Baudet doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppUn, result.getContribuable().getNumeroContribuableIndividuel());
				assertEquals("AB", result.getId());
				break;
			case 1:
				// Richard Basquette doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppDeux, result.getContribuable().getNumeroContribuableIndividuel());
				assertEquals("Riri", result.getId());
				break;
			case 2:
				// Albus Trumbledaure doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppTrois, result.getContribuable().getNumeroContribuableIndividuel());
				assertEquals("Shazam", result.getId());
				break;
			case 3:
				// Personne ne doit avoir été trouvé (aucun)
				assertNull(Integer.toString(index), result.getContribuable());
				assertNotNull(Integer.toString(index), result.getErreur());
				assertNotNull(Integer.toString(index), result.getErreur().getAucun());
				assertNull(result.getId());
				break;
			case 4:
				// Personne ne doit avoir été trouvé (plusieurs)
				assertNull(Integer.toString(index), result.getContribuable());
				assertNotNull(Integer.toString(index), result.getErreur());
				assertNotNull(Integer.toString(index), result.getErreur().getPlusieurs());
				assertEquals("pg", result.getId());
				break;
			default:
				throw new RuntimeException("On a prévu des groupes de 5... s'ils sont plus gros, il y a des choses à changer ici...");
			}
		}
	}

	private IdentificationContribuableResponse parseResponse(EsbMessage message) throws Exception {
		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.identification.response.v3.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		final Schema schema = sf.newSchema(XmlUtils.toSourcesArray(xsdPathes));
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		final IdentificationContribuableResponse reponse = (IdentificationContribuableResponse)element.getValue();
		return reponse;
	}
}
