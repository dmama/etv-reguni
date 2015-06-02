package ch.vd.uniregctb.evenement.reqdes;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.ech.ech0007.v4.CantonAbbreviation;
import ch.ech.ech0010.v4.SwissAddressInformation;
import ch.ech.ech0011.v5.PlaceOfOrigin;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.technical.esb.jms.EsbJmsTemplate;
import ch.vd.unireg.interfaces.infra.mock.MockCanton;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.common.v2.PartialDate;
import ch.vd.unireg.xml.event.reqdes.v1.Actor;
import ch.vd.unireg.xml.event.reqdes.v1.CreationModification;
import ch.vd.unireg.xml.event.reqdes.v1.FullName;
import ch.vd.unireg.xml.event.reqdes.v1.Identity;
import ch.vd.unireg.xml.event.reqdes.v1.InscriptionMode;
import ch.vd.unireg.xml.event.reqdes.v1.InscriptionType;
import ch.vd.unireg.xml.event.reqdes.v1.MaritalStatus;
import ch.vd.unireg.xml.event.reqdes.v1.Nationality;
import ch.vd.unireg.xml.event.reqdes.v1.NotarialDeed;
import ch.vd.unireg.xml.event.reqdes.v1.NotarialInformation;
import ch.vd.unireg.xml.event.reqdes.v1.ObjectFactory;
import ch.vd.unireg.xml.event.reqdes.v1.Partner;
import ch.vd.unireg.xml.event.reqdes.v1.RegistryOrigin;
import ch.vd.unireg.xml.event.reqdes.v1.Residence;
import ch.vd.unireg.xml.event.reqdes.v1.Stakeholder;
import ch.vd.unireg.xml.event.reqdes.v1.StakeholderReferenceWithRole;
import ch.vd.unireg.xml.event.reqdes.v1.StakeholderRole;
import ch.vd.unireg.xml.event.reqdes.v1.SwissResidence;
import ch.vd.unireg.xml.event.reqdes.v1.Swissness;
import ch.vd.unireg.xml.event.reqdes.v1.Transaction;
import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.evenement.EvenementHelper;
import ch.vd.uniregctb.evenement.reqdes.engine.MockEvenementReqDesProcessor;
import ch.vd.uniregctb.jms.EsbMessageValidator;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.ModeInscription;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TransactionImmobiliere;
import ch.vd.uniregctb.reqdes.TypeInscription;
import ch.vd.uniregctb.reqdes.TypeRole;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.Sexe;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessit-jms.xml",
		"classpath:ut/unireg-businessit-jms-evt-reqdes.xml"
})
public class ReqDesEventHandlerITTest extends BusinessItTest {

	private EsbJmsTemplate esbTemplate;
	private EsbMessageValidator esbValidator;
	private String inputQueue;
	private MockEvenementReqDesProcessor processor;

	private static NotarialDeed buildNotarialDeed(RegDate refDate, String numeroMinute) {
		return new NotarialDeed(XmlUtils.regdate2xmlcal(refDate), numeroMinute);
	}

	private static NotarialInformation buildNotarialInformation(Actor notaire, @Nullable Actor operateur) {
		return new NotarialInformation(notaire, operateur);
	}

	private static Actor buildActor(String visa, String nom, String prenom) {
		return new Actor(visa, nom, prenom);
	}

	private static CreationModification buildMsg(NotarialDeed notarialDeed, NotarialInformation notarialInformation, List<Stakeholder> stakeholders, List<Transaction> transactions) {
		return new CreationModification(notarialDeed, notarialInformation, stakeholders, transactions);
	}

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		esbTemplate = getBean(EsbJmsTemplate.class, "esbJmsTemplate");
		processor = getBean(MockEvenementReqDesProcessor.class, "reqdesEventProcessor");

		esbValidator = buildEsbMessageValidator(new Resource[]{new ClassPathResource("event/reqdes/creation-modification-contribuables-1.xsd")});

		inputQueue = uniregProperties.getProperty("testprop.jms.queue.reqdes");

		EvenementHelper.clearQueue(esbTemplate, inputQueue);
	}

	private static String toString(CreationModification cm) throws IOException, JAXBException {
		final JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		final Marshaller marshaller = context.createMarshaller();
		try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			marshaller.marshal(new ObjectFactory().createCreationModification(cm), out);
			return out.toString();
		}
	}

	private static EsbMessage buildTextMessage(String queueName, String texte) throws Exception {
		final EsbMessage m = EsbMessageFactory.createMessage();
		m.setBusinessUser("EvenementTest");
		m.setBusinessId(String.valueOf(m.hashCode()));
		m.setContext("test");
		m.setServiceDestination(queueName);
		m.setBody(texte);
		return m;
	}

	private void sendTextMessage(String queueName, String texte) throws Exception {
		final EsbMessage m = buildTextMessage(queueName, texte);
		esbValidator.validate(m);
		esbTemplate.send(m);
	}

	@Test(timeout = BusinessItTest.JMS_TIMEOUT)
	public void testReceptionMessage() throws Exception {
		final int stakeholderId = 42;
		final RegDate dateActe = RegDate.get().addDays(-12);
		final String noMinute = "123456789B";

		final RegistryOrigin registryOrigin = new RegistryOrigin(new RegistryOrigin.New(), null, null);
		final Identity identity = new Identity(new FullName("De la campagnole", "Alfred Henri André"), "Della Campagnola", "1", new PartialDate(1967, 10, 23), null, new FullName("Crettaz", "Gladys Henriette"), null);
		final Residence residence = new Residence(new SwissResidence(MockCommune.Neuchatel.getNoOFS(), new SwissAddressInformation("Château Hautesrives",
		                                                                                                                           null,
		                                                                                                                           "Place du château",
		                                                                                                                           "1a",
		                                                                                                                           null,
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNomComplet(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNomComplet(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNPA().longValue(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getComplementNPA().toString(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNoOrdre(),
		                                                                                                                           "CH")), null);

		final PlaceOfOrigin origin = new PlaceOfOrigin(MockCommune.Bale.getNomOfficiel(), CantonAbbreviation.valueOf(MockCommune.Bale.getSigleCanton()));
		final List<Stakeholder> stakeholders = Arrays.asList(new Stakeholder(registryOrigin, identity, null, new MaritalStatus("2", null, new Date(2000, 1, 1), null, new Partner(new FullName("De la campagnola", "Philippine"), null)), new Nationality(new Swissness(origin), null, null, null), residence, stakeholderId));
		final List<StakeholderReferenceWithRole> refWithRoles = Arrays.asList(new StakeholderReferenceWithRole(stakeholderId, StakeholderRole.BUYER));

		final List<Transaction> transactions = Arrays.asList(new Transaction("Une transaction", Arrays.asList(MockCommune.Leysin.getNoOFS(), MockCommune.Aigle.getNoOFS()), refWithRoles, InscriptionMode.INSCRIPTION, InscriptionType.PROPERTY));
		final NotarialInformation notarialInformation = buildNotarialInformation(buildActor("fr32ghs", "Garibaldi", "Alfredo"), null);
		final CreationModification creationModification = buildMsg(buildNotarialDeed(dateActe, noMinute), notarialInformation, stakeholders, transactions);

		// génération et envoi du message XML
		sendTextMessage(inputQueue, toString(creationModification));

		// on attend le message reçu
		while (!processor.hasCollectedIds()) {
			Thread.sleep(100);
		}
		final List<Long> collectedIds = processor.drainCollectedUniteTraitementIds();
		Assert.assertNotNull(collectedIds);
		Assert.assertEquals(1, collectedIds.size());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = hibernateTemplate.get(UniteTraitement.class, collectedIds.get(0));
				Assert.assertNotNull(ut);

				// l'événement
				final EvenementReqDes evt = ut.getEvenement();
				Assert.assertNotNull(evt);
				Assert.assertFalse(evt.isDoublon());
				Assert.assertFalse(evt.isAnnule());
				Assert.assertNotNull(evt.getXml());
				Assert.assertEquals(noMinute, evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertNull(evt.getOperateur());
				Assert.assertEquals("fr32ghs", evt.getNotaire().getVisa());
				Assert.assertEquals("Garibaldi", evt.getNotaire().getNom());
				Assert.assertEquals("Alfredo", evt.getNotaire().getPrenom());
				Assert.assertEquals(dateActe, evt.getDateActe());
				Assert.assertNotNull(evt.getTransactions());
				Assert.assertEquals(2, evt.getTransactions().size());

				final List<TransactionImmobiliere> sortedTransactions = new ArrayList<>(evt.getTransactions());
				Collections.sort(sortedTransactions, new Comparator<TransactionImmobiliere>() {
					@Override
					public int compare(TransactionImmobiliere o1, TransactionImmobiliere o2) {
						// d'abord Aigle, puis Leysin
						return o1.getOfsCommune() - o2.getOfsCommune();
					}
				});
				{
					final TransactionImmobiliere ti = sortedTransactions.get(0);
					Assert.assertNotNull(ti);
					Assert.assertFalse(ti.isAnnule());
					Assert.assertEquals(MockCommune.Aigle.getNoOFS(), ti.getOfsCommune());
					Assert.assertEquals(ModeInscription.INSCRIPTION, ti.getModeInscription());
					Assert.assertEquals(TypeInscription.PROPRIETE, ti.getTypeInscription());
					Assert.assertEquals("Une transaction", ti.getDescription());
				}
				{
					final TransactionImmobiliere ti = sortedTransactions.get(1);
					Assert.assertNotNull(ti);
					Assert.assertFalse(ti.isAnnule());
					Assert.assertEquals(MockCommune.Leysin.getNoOFS(), ti.getOfsCommune());
					Assert.assertEquals(ModeInscription.INSCRIPTION, ti.getModeInscription());
					Assert.assertEquals(TypeInscription.PROPRIETE, ti.getTypeInscription());
					Assert.assertEquals("Une transaction", ti.getDescription());
				}

				// l'unité de traitement elle-même
				Assert.assertEquals(EtatTraitement.A_TRAITER, ut.getEtat());
				Assert.assertNull(ut.getDateTraitement());
				Assert.assertFalse(ut.isAnnule());
				Assert.assertNotNull(ut.getErreurs());
				Assert.assertEquals(0, ut.getErreurs().size());
				Assert.assertNotNull(ut.getPartiesPrenantes());
				Assert.assertEquals(1, ut.getPartiesPrenantes().size());

				// la partie prenante
				final PartiePrenante pp = ut.getPartiesPrenantes().iterator().next();
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertEquals("De la campagnole", pp.getNom());
				Assert.assertEquals("Della Campagnola", pp.getNomNaissance());
				Assert.assertEquals("Alfred Henri André", pp.getPrenoms());
				Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
				Assert.assertEquals(date(1967, 10, 23), pp.getDateNaissance());
				Assert.assertEquals(EtatCivil.MARIE, pp.getEtatCivil());
				Assert.assertEquals(date(2000, 1, 1), pp.getDateEtatCivil());
				Assert.assertEquals("Crettaz", pp.getNomMere());
				Assert.assertEquals("Gladys Henriette", pp.getPrenomsMere());
				Assert.assertNull(pp.getNomPere());
				Assert.assertNull(pp.getPrenomsPere());
				Assert.assertEquals("De la campagnola", pp.getNomConjoint());
				Assert.assertEquals("Philippine", pp.getPrenomConjoint());
				Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), pp.getOfsPaysNationalite());
				Assert.assertNotNull(pp.getOrigine());
				Assert.assertEquals(MockCommune.Bale.getNomOfficiel(), pp.getOrigine().getLibelle());
				Assert.assertEquals(MockCanton.BaleVille.getSigleOFS(), pp.getOrigine().getSigleCanton());

				Assert.assertEquals("Place du château", pp.getRue());
				Assert.assertEquals("1a", pp.getNumeroMaison());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNomComplet(), pp.getLocalite());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNPA().toString(), pp.getNumeroPostal());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getComplementNPA(), pp.getNumeroPostalComplementaire());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNoOrdre(), pp.getNumeroOrdrePostal());
				Assert.assertEquals("Château Hautesrives", pp.getTitre());
				Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), pp.getOfsPays());
				Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), pp.getOfsCommune());

				// ... et ses rôles
				final List<RolePartiePrenante> roles = new ArrayList<>(pp.getRoles());
				Assert.assertNotNull(roles);
				Assert.assertEquals(2, roles.size());

				Collections.sort(roles, new Comparator<RolePartiePrenante>() {
					@Override
					public int compare(RolePartiePrenante o1, RolePartiePrenante o2) {
						// d'abord Aigle, puis Leysin
						return o1.getTransaction().getOfsCommune() - o2.getTransaction().getOfsCommune();
					}
				});
				{
					final RolePartiePrenante role = roles.get(0);
					Assert.assertNotNull(role);
					Assert.assertFalse(role.isAnnule());
					Assert.assertEquals(TypeRole.ACQUEREUR, role.getRole());
					Assert.assertSame(sortedTransactions.get(0), role.getTransaction());
				}
				{
					final RolePartiePrenante role = roles.get(1);
					Assert.assertNotNull(role);
					Assert.assertFalse(role.isAnnule());
					Assert.assertEquals(TypeRole.ACQUEREUR, role.getRole());
					Assert.assertSame(sortedTransactions.get(1), role.getTransaction());
				}
			}
		});
	}

	@Test
	public void testReceptionMessageDoublon() throws Exception {
		final int stakeholderId = 42;
		final RegDate dateActe = RegDate.get().addDays(-12);
		final String noMinute = "123456789B";

		final RegistryOrigin registryOrigin = new RegistryOrigin(new RegistryOrigin.New(), null, null);
		final Identity identity = new Identity(new FullName("De la campagnole", "Alfred Henri André"), "De la campagnole", "1", new PartialDate(1967, 10, 23), null, new FullName("Crettaz", "Gladys Henriette"), null);
		final Residence residence = new Residence(new SwissResidence(MockCommune.Neuchatel.getNoOFS(), new SwissAddressInformation("Château Hautesrives",
		                                                                                                                           null,
		                                                                                                                           "Place du château",
		                                                                                                                           "1a",
		                                                                                                                           null,
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNomComplet(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNomComplet(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNPA().longValue(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getComplementNPA().toString(),
		                                                                                                                           MockLocalite.Neuchatel1Cases.getNoOrdre(),
		                                                                                                                           MockPays.Suisse.getCodeIso2())), null);

		final PlaceOfOrigin origin = new PlaceOfOrigin(MockCommune.Zurich.getNomOfficiel(), CantonAbbreviation.valueOf(MockCommune.Zurich.getSigleCanton()));
		final List<Stakeholder> stakeholders = Arrays.asList(new Stakeholder(registryOrigin, identity, null, new MaritalStatus("2", null, new Date(2000, 1, 1), null, new Partner(new FullName("De la campagnola", "Philippine"), null)), new Nationality(new Swissness(origin), null, null, null), residence, stakeholderId));
		final List<StakeholderReferenceWithRole> refWithRoles = Arrays.asList(new StakeholderReferenceWithRole(stakeholderId, StakeholderRole.BUYER));

		final List<Transaction> transactions = Arrays.asList(new Transaction("Une transaction", Arrays.asList(MockCommune.Leysin.getNoOFS(), MockCommune.Aigle.getNoOFS()), refWithRoles, InscriptionMode.INSCRIPTION, InscriptionType.PROPERTY));
		final NotarialInformation notarialInformation = buildNotarialInformation(buildActor("fr32ghs", "Garibaldi", "Alfredo"), null);
		final CreationModification creationModification = buildMsg(buildNotarialDeed(dateActe, noMinute), notarialInformation, stakeholders, transactions);

		// sauvegarde en base d'un événement avec un numéro de minute et un visa de notaire identiques à ce qui arrive maintenant
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final EvenementReqDes evt = new EvenementReqDes();
				evt.setDateActe(RegDate.get());
				evt.setNumeroMinute(noMinute);
				evt.setNotaire(new InformationsActeur("fr32ghs", "Garibaldo", "Alessandro"));
				evt.setXml("<bidon/>");
				hibernateTemplate.merge(evt);
			}
		});

		// génération et envoi du message XML
		sendTextMessage(inputQueue, toString(creationModification));

		// on attend le message reçu
		while (!processor.hasCollectedIds()) {
			Thread.sleep(100);
		}
		final List<Long> collectedIds = processor.drainCollectedUniteTraitementIds();
		Assert.assertNotNull(collectedIds);
		Assert.assertEquals(1, collectedIds.size());

		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final UniteTraitement ut = hibernateTemplate.get(UniteTraitement.class, collectedIds.get(0));
				Assert.assertNotNull(ut);

				// l'événement
				final EvenementReqDes evt = ut.getEvenement();
				Assert.assertNotNull(evt);
				Assert.assertTrue(evt.isDoublon());     // c'est un doublon (mais tout doit être sauvegardé quand-même) !
				Assert.assertFalse(evt.isAnnule());
				Assert.assertNotNull(evt.getXml());
				Assert.assertEquals(noMinute, evt.getNumeroMinute());
				Assert.assertNotNull(evt.getNotaire());
				Assert.assertNull(evt.getOperateur());
				Assert.assertEquals("fr32ghs", evt.getNotaire().getVisa());
				Assert.assertEquals("Garibaldi", evt.getNotaire().getNom());
				Assert.assertEquals("Alfredo", evt.getNotaire().getPrenom());
				Assert.assertEquals(dateActe, evt.getDateActe());
				Assert.assertNotNull(evt.getTransactions());
				Assert.assertEquals(2, evt.getTransactions().size());

				final List<TransactionImmobiliere> sortedTransactions = new ArrayList<>(evt.getTransactions());
				Collections.sort(sortedTransactions, new Comparator<TransactionImmobiliere>() {
					@Override
					public int compare(TransactionImmobiliere o1, TransactionImmobiliere o2) {
						// d'abord Aigle, puis Leysin
						return o1.getOfsCommune() - o2.getOfsCommune();
					}
				});
				{
					final TransactionImmobiliere ti = sortedTransactions.get(0);
					Assert.assertNotNull(ti);
					Assert.assertFalse(ti.isAnnule());
					Assert.assertEquals(MockCommune.Aigle.getNoOFS(), ti.getOfsCommune());
					Assert.assertEquals(ModeInscription.INSCRIPTION, ti.getModeInscription());
					Assert.assertEquals(TypeInscription.PROPRIETE, ti.getTypeInscription());
					Assert.assertEquals("Une transaction", ti.getDescription());
				}
				{
					final TransactionImmobiliere ti = sortedTransactions.get(1);
					Assert.assertNotNull(ti);
					Assert.assertFalse(ti.isAnnule());
					Assert.assertEquals(MockCommune.Leysin.getNoOFS(), ti.getOfsCommune());
					Assert.assertEquals(ModeInscription.INSCRIPTION, ti.getModeInscription());
					Assert.assertEquals(TypeInscription.PROPRIETE, ti.getTypeInscription());
					Assert.assertEquals("Une transaction", ti.getDescription());
				}

				// l'unité de traitement elle-même
				Assert.assertEquals(EtatTraitement.A_TRAITER, ut.getEtat());
				Assert.assertNull(ut.getDateTraitement());
				Assert.assertFalse(ut.isAnnule());
				Assert.assertNotNull(ut.getErreurs());
				Assert.assertEquals(0, ut.getErreurs().size());
				Assert.assertNotNull(ut.getPartiesPrenantes());
				Assert.assertEquals(1, ut.getPartiesPrenantes().size());

				// la partie prenante
				final PartiePrenante pp = ut.getPartiesPrenantes().iterator().next();
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.isAnnule());
				Assert.assertEquals("De la campagnole", pp.getNom());
				Assert.assertEquals("De la campagnole", pp.getNomNaissance());
				Assert.assertEquals("Alfred Henri André", pp.getPrenoms());
				Assert.assertEquals(Sexe.MASCULIN, pp.getSexe());
				Assert.assertEquals(date(1967, 10, 23), pp.getDateNaissance());
				Assert.assertEquals(EtatCivil.MARIE, pp.getEtatCivil());
				Assert.assertEquals(date(2000, 1, 1), pp.getDateEtatCivil());
				Assert.assertEquals("Crettaz", pp.getNomMere());
				Assert.assertEquals("Gladys Henriette", pp.getPrenomsMere());
				Assert.assertNull(pp.getNomPere());
				Assert.assertNull(pp.getPrenomsPere());
				Assert.assertEquals("De la campagnola", pp.getNomConjoint());
				Assert.assertEquals("Philippine", pp.getPrenomConjoint());
				Assert.assertNotNull(pp.getOrigine());
				Assert.assertEquals(MockCommune.Zurich.getNomOfficiel(), pp.getOrigine().getLibelle());
				Assert.assertEquals(MockCanton.Zurich.getSigleOFS(), pp.getOrigine().getSigleCanton());

				Assert.assertEquals("Place du château", pp.getRue());
				Assert.assertEquals("1a", pp.getNumeroMaison());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNomComplet(), pp.getLocalite());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNPA().toString(), pp.getNumeroPostal());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getComplementNPA(), pp.getNumeroPostalComplementaire());
				Assert.assertEquals(MockLocalite.Neuchatel1Cases.getNoOrdre(), pp.getNumeroOrdrePostal());
				Assert.assertEquals("Château Hautesrives", pp.getTitre());
				Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), pp.getOfsPays());
				Assert.assertEquals((Integer) MockCommune.Neuchatel.getNoOFS(), pp.getOfsCommune());

				// ... et ses rôles
				final List<RolePartiePrenante> roles = new ArrayList<>(pp.getRoles());
				Assert.assertNotNull(roles);
				Assert.assertEquals(2, roles.size());

				Collections.sort(roles, new Comparator<RolePartiePrenante>() {
					@Override
					public int compare(RolePartiePrenante o1, RolePartiePrenante o2) {
						// d'abord Aigle, puis Leysin
						return o1.getTransaction().getOfsCommune() - o2.getTransaction().getOfsCommune();
					}
				});
				{
					final RolePartiePrenante role = roles.get(0);
					Assert.assertNotNull(role);
					Assert.assertFalse(role.isAnnule());
					Assert.assertEquals(TypeRole.ACQUEREUR, role.getRole());
					Assert.assertSame(sortedTransactions.get(0), role.getTransaction());
				}
				{
					final RolePartiePrenante role = roles.get(1);
					Assert.assertNotNull(role);
					Assert.assertFalse(role.isAnnule());
					Assert.assertEquals(TypeRole.ACQUEREUR, role.getRole());
					Assert.assertSame(sortedTransactions.get(1), role.getTransaction());
				}
			}
		});
	}
}
