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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.unireg.common.BusinessItTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.event.identification.request.v4.CorporationIdentificationData;
import ch.vd.unireg.xml.event.identification.request.v4.IdentificationContribuableRequest;
import ch.vd.unireg.xml.event.identification.request.v4.IdentificationData;
import ch.vd.unireg.xml.event.identification.request.v4.NaturalPersonIdentificationData;
import ch.vd.unireg.xml.event.identification.request.v4.ObjectFactory;
import ch.vd.unireg.xml.event.identification.response.v4.IdentificationContribuableResponse;
import ch.vd.unireg.xml.event.identification.response.v4.IdentificationResult;
import ch.vd.unireg.xml.event.identification.response.v4.IdentifiedNaturalPerson;
import ch.vd.unireg.xml.event.identification.response.v4.IdentifiedTaxpayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentificationContribuableRequestV4ListenerItTest extends IdentificationContribuableRequestListenerItTest {

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
		return "identificationContribuableRequestHandlerV4";
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiquePersonnePhysiqueOK() throws Exception {

		final RegDate dateNaissance = RegDate.get(1982, 6);     // date partielle, pour le faire au moins une fois

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier Vallard", dateNaissance, Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		globalTiersIndexer.sync();

		final IdentificationData data = new NaturalPersonIdentificationData("MaDemande", 7569396525489L, null, "Monnier", "Christophe", null, null);
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
		assertEquals("MaDemande", result.getId());

		final IdentifiedTaxpayer infoCtb = result.getContribuable();
		assertNotNull(infoCtb);
		assertEquals(id, infoCtb.getNumeroContribuable());
		assertEquals(IdentifiedNaturalPerson.class, infoCtb.getClass());

		final IdentifiedNaturalPerson infoPersonnePhysique = (IdentifiedNaturalPerson) infoCtb;
		assertEquals("Monnier Vallard", infoPersonnePhysique.getNom());
		assertEquals("Christophe", infoPersonnePhysique.getPrenom());
		assertEquals(dateNaissance, DataHelper.xmlToCore(infoPersonnePhysique.getDateNaissance()));
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiqueEntrepriseOK() throws Exception {

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final AutreCommunaute ac = addAutreCommunaute("Pittet Echaffaudages SA");
				return ac.getNumero();
			}
		});
		globalTiersIndexer.sync();

		final IdentificationData data = new CorporationIdentificationData("MaDemandePM", null, "Pittet Echaffaudages", null);
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
		assertEquals("MaDemandePM", result.getId());
		assertTrue(StringUtils.containsIgnoreCase(result.getErreur().getAucun().toString(), "Aucun contribuable trouvé pour le message"));

	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testDemandeIdentificationAutomatiquePlusieurs() throws Exception {

		final Long id1 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});
		final Long id2 = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1964, 6, 29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.sync();

		final IdentificationData data = new NaturalPersonIdentificationData("Monnier, tu dors...?", null, null, "Monnier", "Christophe", null, null);
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

		final Long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				final PersonnePhysique christophe = addNonHabitant("Christophe", "Monnier", date(1982, 6, 29), Sexe.MASCULIN);
				return christophe.getNumero();
			}
		});

		globalTiersIndexer.sync();

		final IdentificationData data = new NaturalPersonIdentificationData("Raphaello", null, null, "Adam", "Raphaël", null, null);
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
			int pm;
		}

		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique ppUn = addNonHabitant("Alphonse", "Baudet", null, Sexe.MASCULIN);
				final PersonnePhysique ppDeux = addNonHabitant("Richard", "Basquette", null, Sexe.MASCULIN);
				final PersonnePhysique ppTrois = addNonHabitant("Albus", "Trumbledaure", null, Sexe.MASCULIN);
				final Entreprise pm = addEntrepriseInconnueAuCivil();
				addRaisonSociale(pm, date(1883, 6, 1), null, "Banque cantonale vaudoise");
				addFormeJuridique(pm, date(1883, 6, 1), null, FormeJuridiqueEntreprise.CORP_DP_ENT);
				addCapitalEntreprise(pm, date(1883, 6, 1), null, new MontantMonetaire(1000000000L, MontantMonetaire.CHF));

				// on crée 150 "Georges Pittet" pour vérifier aussi le cas du trop grand nombre de résultats
				for (int i = 0; i < 150; ++i) {
					addNonHabitant("Georges", "Pittet", null, Sexe.MASCULIN);
				}

				final Ids ids = new Ids();
				ids.ppUn = ppUn.getNumero().intValue();
				ids.ppDeux = ppDeux.getNumero().intValue();
				ids.ppTrois = ppTrois.getNumero().intValue();
				ids.pm = pm.getNumero().intValue();
				return ids;
			}
		});

		globalTiersIndexer.sync();

		final int nbGroupes = 13;
		final int tailleGroupe = 6;     // pour chacun des cas et une réponse négative (aucun) et une réponse négative (plusieurs)
		final List<IdentificationData> identificationDataList = new ArrayList<>(nbGroupes * tailleGroupe);
		for (int idxGroupe = 0; idxGroupe < nbGroupes; ++idxGroupe) {
			for (int idx = 0; idx < tailleGroupe; ++idx) {
				final String nom;
				final String prenom;
				final String id;
				final boolean pp;
				switch (idx) {
				case 0:
					nom = "Baudet";
					prenom = "Alphonse";
					id = "AB";
					pp = true;
					break;
				case 1:
					nom = "Basquette";
					prenom = "Richard";
					id = "Riri";
					pp = true;
					break;
				case 2:
					nom = "Trumbledaure";
					prenom = "Albus";
					id = "Shazam";
					pp = true;
					break;
				case 3:
					nom = "Peticlou";
					prenom = "Justin";
					id = null;
					pp = true;
					break;
				case 4:
					nom = "Pittet";
					prenom = "Georges";
					id = "pg";
					pp = true;
					break;
				case 5:
					nom = "Banque Cantonale Vaudoise";
					prenom = null;
					id = "bcv";
					pp = false;
					break;
				default:
					throw new RuntimeException("On a prévu des groupes de 6... s'ils sont plus gros, il y a des choses à changer ici...");
				}

				final IdentificationData data;
				if (pp) {
					data = new NaturalPersonIdentificationData(id, null, null, nom, prenom, null, null);
				}
				else {
					data = new CorporationIdentificationData(id, null, nom, null);
				}
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

		for (int index = 0; index < nbGroupes * tailleGroupe; ++index) {
			final IdentificationResult result = response.getIdentificationResult().get(index);
			assertNotNull(result);
			switch (index % tailleGroupe) {
			case 0:
				// Alphonse Baudet doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppUn, result.getContribuable().getNumeroContribuable());
				assertEquals("AB", result.getId());
				break;
			case 1:
				// Richard Basquette doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppDeux, result.getContribuable().getNumeroContribuable());
				assertEquals("Riri", result.getId());
				break;
			case 2:
				// Albus Trumbledaure doit avoir été trouvé
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.ppTrois, result.getContribuable().getNumeroContribuable());
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
			case 5:
				// La BCV doit avoir été trouvée
				assertNotNull(Integer.toString(index), result.getContribuable());
				assertNull(Integer.toString(index), result.getErreur());
				assertEquals(Integer.toString(index), ids.pm, result.getContribuable().getNumeroContribuable());
				assertEquals("bcv", result.getId());
				break;
			default:
				throw new RuntimeException("On a prévu des groupes de 6... s'ils sont plus gros, il y a des choses à changer ici...");
			}
		}
	}

	private IdentificationContribuableResponse parseResponse(EsbMessage message) throws Exception {
		final JAXBContext context = JAXBContext.newInstance(ch.vd.unireg.xml.event.identification.response.v4.ObjectFactory.class.getPackage().getName());
		final Unmarshaller u = context.createUnmarshaller();
		final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
		final Schema schema = sf.newSchema(XmlUtils.toSourcesArray(xsdPathes));
		u.setSchema(schema);

		final JAXBElement element = (JAXBElement) u.unmarshal(message.getBodyAsSource());
		return (IdentificationContribuableResponse) element.getValue();
	}
}
