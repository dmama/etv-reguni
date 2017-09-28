package ch.vd.uniregctb.evenement.fiscal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.technical.esb.store.raft.RaftEsbStore;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.EvenementTest;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalCommunaute;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroit;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalDroitPropriete;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalImmeuble;
import ch.vd.uniregctb.evenement.fiscal.registrefoncier.EvenementFiscalServitude;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommuneRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.MockRegistreFoncierService;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.SituationRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

@SuppressWarnings("Duplicates")
public class EvenementFiscalV5SenderItTest extends EvenementTest {

	private final static Long NUMERO_CONTRIBUABLE = 12300652L;

	private String OUTPUT_QUEUE;
	private EvenementFiscalV5SenderImpl sender;
	private EvenementFiscalV5FactoryImpl evenementFiscalV5Factory;

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

		buildEsbMessageValidator(new String[] {
				"classpath*:ws/*.xsd",
				"classpath*:party/*.xsd",
				"unireg-common-2.xsd",
				"event/fiscal/evt-fiscal-5.xsd",
		});

		evenementFiscalV5Factory = new EvenementFiscalV5FactoryImpl();
		evenementFiscalV5Factory.afterPropertiesSet();

		sender = new EvenementFiscalV5SenderImpl();
		sender.setServiceDestination("test");
		sender.setOutputQueue(OUTPUT_QUEUE);
		sender.setEsbTemplate(esbTemplate);
		sender.setEsbValidator(esbValidator);
		sender.setEvenementFiscalV5Factory(evenementFiscalV5Factory);
		sender.afterPropertiesSet();
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
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:familyStatusEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON_OR_DEEMED_EQUIVALENT</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></fisc-evt-5:date>" +
					"</fisc-evt-5:fiscalEvent>";
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
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:taxResidenceStartEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON_OR_DEEMED_EQUIVALENT</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2009</common-2:year><common-2:month>12</common-2:month><common-2:day>9</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:mainTaxResidence>true</fisc-evt-5:mainTaxResidence>" +
					"<fisc-evt-5:typeAuthority>VAUD_MUNICIPALITY</fisc-evt-5:typeAuthority>" +
					"<fisc-evt-5:startReason>MOVE_IN_FROM_FOREIGN_COUNTRY</fisc-evt-5:startReason>" +
					"<fisc-evt-5:padding>0</fisc-evt-5:padding>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementImpressionFourreNeutre() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final ContribuableImpositionPersonnesPhysiques pp = new PersonnePhysique(false);
			pp.setNumero(NUMERO_CONTRIBUABLE);
			final EvenementFiscalImpressionFourreNeutre event = new EvenementFiscalImpressionFourreNeutre(pp,2015,RegDate.get(2017, 1, 20));
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:neutralJacketPrintEventType\">" +
					"<fisc-evt-5:partyKind>NATURAL_PERSON_OR_DEEMED_EQUIVALENT</fisc-evt-5:partyKind>" +
					"<fisc-evt-5:partyNumber>12300652</fisc-evt-5:partyNumber>" +
					"<fisc-evt-5:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>20</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:period>2015</fisc-evt-5:period>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementCreationImmeuble() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message

			final CommuneRF commune = new CommuneRF(12, "Echallens", 2322);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(2000, 1, 1));
			situation.setNoParcelle(212);
			situation.setCommune(commune);

			final BienFondsRF immeuble = new BienFondsRF();
			immeuble.setId(94949L);
			immeuble.setIdRF("39393");
			immeuble.setEgrid("CH28282");
			immeuble.addSituation(situation);

			final EvenementFiscalImmeuble event = new EvenementFiscalImmeuble(RegDate.get(2017, 1, 1), immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.CREATION);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:immovablePropertyStartEventType\">" +
					"<fisc-evt-5:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>1</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:immovablePropertyId>94949</fisc-evt-5:immovablePropertyId>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementModificationEgrid() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message

			final CommuneRF commune = new CommuneRF(12, "Echallens", 2322);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(2000, 1, 1));
			situation.setNoParcelle(212);
			situation.setCommune(commune);

			final BienFondsRF immeuble = new BienFondsRF();
			immeuble.setId(94949L);
			immeuble.setIdRF("39393");
			immeuble.setEgrid("CH28282");
			immeuble.addSituation(situation);

			final EvenementFiscalImmeuble event = new EvenementFiscalImmeuble(RegDate.get(2017, 1, 1), immeuble, EvenementFiscalImmeuble.TypeEvenementFiscalImmeuble.MODIFICATION_EGRID);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:futureEventType\">" +
					"<fisc-evt-5:type>IMMO_EGRID_UPDATE</fisc-evt-5:type>" +
					"<fisc-evt-5:data1>2017-01-01</fisc-evt-5:data1>" +
					"<fisc-evt-5:data2>94949</fisc-evt-5:data2>" +
					"<fisc-evt-5:padding>0</fisc-evt-5:padding>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementOuvertureServitude() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message

			final CommuneRF commune = new CommuneRF(12, "Echallens", 2322);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(2000, 1, 1));
			situation.setNoParcelle(212);
			situation.setCommune(commune);

			final BienFondsRF immeuble = new BienFondsRF();
			immeuble.setId(94949L);
			immeuble.setIdRF("39393");
			immeuble.setEgrid("CH28282");
			immeuble.addSituation(situation);

			final TiersRF tiers = new PersonnePhysiqueRF();
			tiers.setId(281819L);

			final UsufruitRF servitude = new UsufruitRF();
			servitude.addImmeuble(immeuble);
			servitude.addAyantDroit(tiers);

			final EvenementFiscalServitude event = new EvenementFiscalServitude(RegDate.get(2017, 1, 1), servitude, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			evenementFiscalV5Factory.setRegistreFoncierService(new MockRegistreFoncierService() {
				@Override
				public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
					return 1111222L;
				}
			});

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:easementRightStartEventType\">" +
					"<fisc-evt-5:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>1</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:rightHolders><land-1:taxPayerNumber>1111222</land-1:taxPayerNumber><land-1:padding>0</land-1:padding></fisc-evt-5:rightHolders>" +
					"<fisc-evt-5:immovablePropertyIds>94949</fisc-evt-5:immovablePropertyIds>" +
					"<fisc-evt-5:easementType>USUFRUCT</fisc-evt-5:easementType>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementOuvertureDroitSurEntrepriseSansNumeroRC() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message

			final CommuneRF commune = new CommuneRF(12, "Echallens", 2322);

			final SituationRF situation = new SituationRF();
			situation.setDateDebut(RegDate.get(2000, 1, 1));
			situation.setNoParcelle(212);
			situation.setCommune(commune);

			final BienFondsRF immeuble = new BienFondsRF();
			immeuble.setId(94949L);
			immeuble.setIdRF("39393");
			immeuble.setEgrid("CH28282");
			immeuble.addSituation(situation);

			final PersonneMoraleRF tiers = new PersonneMoraleRF();
			tiers.setId(281819L);
			tiers.setRaisonSociale("Ma moyenne entreprise");
			tiers.setNumeroRC(null);    // <---- ici !

			final DroitProprietePersonneMoraleRF droit = new DroitProprietePersonneMoraleRF();
			droit.setImmeuble(immeuble);
			droit.setAyantDroit(tiers);

			final EvenementFiscalDroitPropriete event = new EvenementFiscalDroitPropriete(RegDate.get(2017, 1, 1), droit, EvenementFiscalDroit.TypeEvenementFiscalDroitPropriete.OUVERTURE);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			evenementFiscalV5Factory.setRegistreFoncierService(new MockRegistreFoncierService() {
				@Override
				@Nullable
				public Long getContribuableIdFor(@NotNull TiersRF tiersRF) {
					return null;
				}
			});

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message (= pas d'erreur de validation)
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:landOwnershipRightStartEventType\">" +
					"<fisc-evt-5:date><common-2:year>2017</common-2:year><common-2:month>1</common-2:month><common-2:day>1</common-2:day></fisc-evt-5:date>" +
					"<fisc-evt-5:rightHolder>" +
					"<land-1:identity xsi:type=\"land-1:corporationIdentityType\">" +
					"<land-1:id>0</land-1:id>" +
					"<land-1:name>Ma moyenne entreprise</land-1:name>" +
					"<land-1:commercialRegisterNumber/>" +
					"<land-1:padding>0</land-1:padding>" +
					"</land-1:identity>" +
					"<land-1:padding>0</land-1:padding>" +
					"</fisc-evt-5:rightHolder>" +
					"<fisc-evt-5:immovablePropertyId>94949</fisc-evt-5:immovablePropertyId>" +
					"<fisc-evt-5:padding>0</fisc-evt-5:padding>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	@Test(timeout = 10000L)
	public void testSendEvenementModificationPrincipalCommunaute() throws Exception {
		AuthenticationHelper.pushPrincipal("EvenementFiscalSenderTest");
		try {
			// Création du message
			final CommunauteRF communaute = new CommunauteRF();
			communaute.setId(3737L);

			final EvenementFiscalCommunaute event = new EvenementFiscalCommunaute(RegDate.get(2017, 1, 1), communaute, EvenementFiscalCommunaute.TypeEvenementFiscalCommunaute.CHANGEMENT_PRINCIPAL);
			event.setId(1234L);
			event.setLogCreationUser("Toto");       // on s'en sert comme businessUser lors de l'envoi, et celui-ci est obligatoire

			// Envoi du message
			sender.sendEvent(event);

			// On vérifie que l'on a bien envoyé le message
			final String texte = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><fisc-evt-5:fiscalEvent xmlns:fisc-evt-5=\"http://www.vd.ch/fiscalite/unireg/event/fiscal/5\" xmlns:common-2=\"http://www.vd.ch/fiscalite/unireg/common/2\" xmlns:corp-5=\"http://www.vd.ch/fiscalite/unireg/party/corporation/5\" xmlns:land-1=\"http://www.vd.ch/fiscalite/unireg/party/landregistry/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"fisc-evt-5:futureEventType\">" +
					"<fisc-evt-5:type>COMMUNITY_LEADER_UPDATE</fisc-evt-5:type>" +
					"<fisc-evt-5:data1>2017-01-01</fisc-evt-5:data1>" +
					"<fisc-evt-5:data2>3737</fisc-evt-5:data2>" +
					"<fisc-evt-5:padding>0</fisc-evt-5:padding>" +
					"</fisc-evt-5:fiscalEvent>";
			assertTextMessage(OUTPUT_QUEUE, texte);
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}
}
