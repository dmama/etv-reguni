package ch.vd.unireg.evenement.fiscal;

import java.util.EnumSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.evenement.EvenementTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.xml.event.fiscal.v2.TypeEvenementFiscalDeclaration;
import ch.vd.unireg.xml.event.fiscal.v2.TypeInformationComplementaire;

public class EvenementFiscalV2SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300002L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV2SenderImpl sender;

	@Before
	public void setUp() throws Exception {

		OUTPUT_QUEUE = uniregProperties.getProperty("testprop.jms.queue.evtFiscal");

		final RaftEsbStore esbStore = new RaftEsbStore();
		esbStore.setEndpoint("TestRaftStore");

		esbTemplate = new EsbJmsTemplate();
		esbTemplate.setConnectionFactory(jmsConnectionFactory);
		esbTemplate.setEsbStore(esbStore);
		esbTemplate.setReceiveTimeout(200);
		esbTemplate.setApplication("unireg");
		esbTemplate.setDomain("fiscalite");
		esbTemplate.setSessionTransacted(true);
		if (esbTemplate instanceof InitializingBean) {
			((InitializingBean) esbTemplate).afterPropertiesSet();
		}

		clearQueue(OUTPUT_QUEUE);

		buildEsbMessageValidator(new Resource[]{
				new ClassPathResource("eCH-0010-5-0.xsd"),
				new ClassPathResource("eCH-0044-3-0.xsd"),
				new ClassPathResource("unireg-common-2.xsd"),
				new ClassPathResource("party/unireg-party-address-2.xsd"),
				new ClassPathResource("party/unireg-party-relation-3.xsd"),
				new ClassPathResource("party/unireg-party-withholding-1.xsd"),
				new ClassPathResource("party/unireg-party-taxdeclaration-4.xsd"),
				new ClassPathResource("party/unireg-party-taxresidence-3.xsd"),
				new ClassPathResource("party/unireg-party-immovableproperty-2.xsd"),
				new ClassPathResource("party/unireg-party-ebilling-1.xsd"),
				new ClassPathResource("party/unireg-party-4.xsd"),
				new ClassPathResource("party/unireg-party-taxpayer-4.xsd"),
				new ClassPathResource("party/unireg-party-corporation-4.xsd"),
				new ClassPathResource("event/fiscal/evt-fiscal-2.xsd")
		});

		sender = new EvenementFiscalV2SenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		if (sender instanceof InitializingBean) {
			((InitializingBean) sender).afterPropertiesSet();
		}
	}

	@Test
	public void publierEvenementArgumentNull() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			sender.sendEvent(null);
			Assert.fail();
		}
		catch (IllegalArgumentException e) {
			Assert.assertEquals("Argument evenement ne peut être null.", e.getMessage());
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
	
	@Test(timeout = 10000L)
	public void testSendEvenementSituationFamille() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final EvenementFiscalSituationFamille event = new EvenementFiscalSituationFamille(RegDate.get(2009, 12, 9), pp);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-fiscal-2:evenementFiscal xmlns:ev-fiscal-2=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/2\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-4=\"http://www.vd.ch/fiscalite/unireg/party/corporation/4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ev-fiscal-2:changementSituationFamilleType\"><ev-fiscal-2:categorieTiers>PP</ev-fiscal-2:categorieTiers><ev-fiscal-2:numeroTiers>12300002</ev-fiscal-2:numeroTiers><ev-fiscal-2:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></ev-fiscal-2:date></ev-fiscal-2:evenementFiscal>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementFor() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final ForFiscalPrincipal ffp = new ForFiscalPrincipalPP(RegDate.get(2009, 12, 9), MotifFor.ARRIVEE_HS, null, null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE);
			pp.addForFiscal(ffp);
			final EvenementFiscalFor event = new EvenementFiscalFor(RegDate.get(2009, 12, 9), ffp, EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ev-fiscal-2:evenementFiscal xmlns:ev-fiscal-2=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/2\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-4=\"http://www.vd.ch/fiscalite/unireg/party/corporation/4\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ev-fiscal-2:ouvertureForType\"><ev-fiscal-2:categorieTiers>PP</ev-fiscal-2:categorieTiers><ev-fiscal-2:numeroTiers>12300002</ev-fiscal-2:numeroTiers><ev-fiscal-2:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></ev-fiscal-2:date><ev-fiscal-2:forPrincipal>true</ev-fiscal-2:forPrincipal><ev-fiscal-2:localisationFor>VAUD_MUNICIPALITY</ev-fiscal-2:localisationFor><ev-fiscal-2:motifOuverture>MOVE_IN_FROM_FOREIGN_COUNTRY</ev-fiscal-2:motifOuverture><ev-fiscal-2:padding>0</ev-fiscal-2:padding></ev-fiscal-2:evenementFiscal>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test
	public void testInstanciateAllegementFiscal() throws Exception {
		// on doit vérifier que types d'événement d'allègement fiscal sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement type : EvenementFiscalAllegementFiscal.TypeEvenementFiscalAllegement.values()) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalAllegementFiscal instance = EvenementFiscalV2SenderImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", instance);
		}
	}

	@Test
	public void testTypeActionEvenementDeclaration() throws Exception {
		// on doit vérifier que types d'événement autour des déclarations (dites 'sommables') sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalDeclarationSommable.TypeAction type : EvenementFiscalDeclarationSommable.TypeAction.values()) {
			final TypeEvenementFiscalDeclaration mapped = EvenementFiscalV2SenderImpl.mapType(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", mapped);
		}
	}

	@Test
	public void testInstanciateFor() throws Exception {
		// on doit vérifier que types d'événement de fors sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalFor.TypeEvenementFiscalFor type : EvenementFiscalFor.TypeEvenementFiscalFor.values()) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalFor instance = EvenementFiscalV2SenderImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", instance);
		}
	}

	@Test
	public void testTypeInformationComplementaire() throws Exception {
		final Set<EvenementFiscalInformationComplementaire.TypeInformationComplementaire> ignored = EnumSet.of(EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_FAILLITE,
		                                                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_FUSION,
		                                                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_SCISSION,
		                                                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.ANNULATION_TRANFERT_PATRIMOINE,
		                                                                                                       EvenementFiscalInformationComplementaire.TypeInformationComplementaire.TRANSFERT_PATRIMOINE);

		// on doit vérifier que types d'événement autour des informations complémentaires sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalInformationComplementaire.TypeInformationComplementaire type : EvenementFiscalInformationComplementaire.TypeInformationComplementaire.values()) {
			final TypeInformationComplementaire mapped = EvenementFiscalV2SenderImpl.mapType(type);
			if (ignored.contains(type)) {
				Assert.assertNull("type " + type + " ne devrait pas être connu dans la XSD des événements fiscaux v2", mapped);
			}
			else {
				Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", mapped);
			}
		}
	}

	@Test
	public void testInstanciateParente() throws Exception {
		// on doit vérifier que types d'événement de parenté sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalParente.TypeEvenementFiscalParente type : EvenementFiscalParente.TypeEvenementFiscalParente.values()) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalParente instance = EvenementFiscalV2SenderImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", instance);
		}
	}

	@Test
	public void testInstanciateRegimeFiscal() throws Exception {
		// on doit vérifier que types d'événement autour des régimes fiscaux sont acceptés par l'XSD des événements fiscaux v2
		for (EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime type : EvenementFiscalRegimeFiscal.TypeEvenementFiscalRegime.values()) {
			final ch.vd.unireg.xml.event.fiscal.v2.EvenementFiscalRegimeFiscal instance = EvenementFiscalV2SenderImpl.instanciate(type);
			Assert.assertNotNull("type " + type + " inconnu dans la XSD des événements fiscaux v2", instance);
		}
	}
}
