package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerSituationHistoryEntry;
import ch.vd.evd0025.v1.PayerStatus;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationMode;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.ResultatQuittancement;
import ch.vd.unireg.interfaces.efacture.data.TypeAttenteDemande;
import ch.vd.unireg.interfaces.efacture.data.TypeRefusDemande;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;



public class EFactureServiceTest extends BusinessTest {

	private static final String BUSINESS_ID = "BUSINESS-ID";
	private EFactureServiceImpl efactureService;

	private static final List<RegistrationRequestWithHistory> DUMMY_HISTORY_OF_REQUEST = Collections.singletonList(
			new DemandeAvecHistoBuilderForUnitTests()
					.addHistoryEntry(date(2012, 1, 1), RegistrationRequestStatus.A_TRAITER, null, "", "")
					.buildRegistrationRequestWithHistory());


	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		efactureService = getBean(EFactureServiceImpl.class, "efactureService");
	}

	@Test
	public void testQuittancer() throws Exception {
		final TestSamples ts = new TestSamples();
		EFactureClient mockEfactureClient = createMock(EFactureClient.class);

		expect(mockEfactureClient.getHistory(anyLong(), anyObject(String.class)))
				// 1er appel au client eFacture renvoie un contribuable inscrit
				.andReturn(buildPayerWithHistory(PayerStatus.INSCRIT,
						Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.INSCRIT).build()),DUMMY_HISTORY_OF_REQUEST)).times(2)
				// 2eme appel au client eFacture renvoie un contribualble avec la situation DESINSCRIT avec une demande en attente de signature
				.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,
					Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
						.addHistoryEntry(date(2012,6,1),RegistrationRequestStatus.VALIDATION_EN_COURS,TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(),"","").buildRegistrationRequestWithHistory()))).times(2)
				// 3eme appel au client eFacture renvoie un contribualble avec la situation DESINSCRIT avec une demande en attente de contact
				.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,
						Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
						Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
								.addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), "", "")
								.buildRegistrationRequestWithHistory()))).times(2)
				// 4eme appel au client eFacture renvoie un contribualble avec la situation DESINSCRIT avec une demande validée
				.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,
						Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
						Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
								.addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.VALIDEE, null, "", "").buildRegistrationRequestWithHistory()))).times(2)
				// 5eme appel au client eFacture renvoie un contribualble avec la situation DESINSCRIT avec une demande refusée
				.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,
						Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
						Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
								.addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.REFUSEE, null, "", "").buildRegistrationRequestWithHistory()))).times(2)
				// Le dernier appelle renvoye null
				.andReturn(null);

		EFactureMessageSender eFactureMessageSender = createMock(EFactureMessageSender.class);
		expect(eFactureMessageSender.envoieAcceptationDemandeInscription(anyObject(String.class), eq(true), anyObject(String.class))).andStubReturn(BUSINESS_ID);

		efactureService.seteFactureClient(mockEfactureClient);
		efactureService.seteFactureMessageSender(eFactureMessageSender);

		replay(mockEfactureClient, eFactureMessageSender);

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				try {
					Assert.assertEquals("le contribuable 123 n'existe pas", ResultatQuittancement.contribuableInexistant(), efactureService.quittancer(123L));
					Assert.assertEquals("le contribuable chollet existe est inscrit", ResultatQuittancement.dejaInscrit(), efactureService.quittancer(ts.idChollet));
					Assert.assertEquals("le contribuable chollet existe, n'est pas inscrit, et sa demande est en attente de signature", ResultatQuittancement.enCours(BUSINESS_ID), efactureService.quittancer(ts.idChollet));
					Assert.assertEquals("le contribuable chollet existe, n'est pas inscrit, et sa demande est en attente de contact", ResultatQuittancement.aucuneDemandeEnAttenteDeSignature(), efactureService.quittancer(ts.idChollet));
					Assert.assertEquals("le contribuable chollet existe n'est pas inscrit et sa demande est validée", ResultatQuittancement.aucuneDemandeEnAttenteDeSignature(), efactureService.quittancer(ts.idChollet));
					Assert.assertEquals("le contribuable chollet existe n'est pas inscrit et sa demande est refusée", ResultatQuittancement.aucuneDemandeEnAttenteDeSignature(), efactureService.quittancer(ts.idChollet));
					Assert.assertEquals("le contribuable Jacquier n'est pas dans un etat fiscale coherent (pas de for)", ResultatQuittancement.etatFiscalIncoherent(), efactureService.quittancer(ts.idJacquier));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		verify(mockEfactureClient, eFactureMessageSender);

	}

	@Test
	public void testDemandeInscriptionEnCoursDeTraitement () {

		// Initialisation du mock client eFacture pour qu'il renvoye :
		// -----------------------------------------------------------
		//
		// - au 1er appel,  une RegistrationRequest avec le status VALIDATION_EN_COURS et avec une codeReason non null
		//      ==> UNIREG doit interpreter ça comme une demande d'inscription en cours de traitement
		//
		// - au 2nd appel,  une RegistrationRequest avec le status VALIDATION_EN_COURS et avec une codeReason non null
		//      ==> UNIREG doit interpreter ça comme une demande d'inscription en cours de traitement
		//   la difference est que la registration request a 2 entrées dans son historique (la 1ere refusée doit être ignorée)
		//
		// - au 3eme appel, une RegistrationRequest avec le status VALIDATION_EN_COURS et avec une codeReason null
		//       ==> UNIREG doit interpreter ça comme une nouvelle demande d'inscription qui n'a pas encore commencé son traitement.
		//
		// - au 4eme appel, une RegistrationRequest sans historique
		//       ==> Ce cas ne doit pas ce produire eFactureService lève une IllegalArgumentException

		EFactureClient mockEfactureClient = EasyMock.createMock(EFactureClient.class);
		efactureService.seteFactureClient(mockEfactureClient);

		RegistrationRequestWithHistory mockReturn1 =
				new DemandeAvecHistoBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), "", "")
						.buildRegistrationRequestWithHistory();

		RegistrationRequestWithHistory mockReturn2 =
				new DemandeAvecHistoBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 5, 26), RegistrationRequestStatus.REFUSEE, null, "","")
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), "", "")
						.buildRegistrationRequestWithHistory();

		RegistrationRequestWithHistory mockReturn3 =
				new DemandeAvecHistoBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, null, "", "")
						.buildRegistrationRequestWithHistory();

		expect(
				mockEfactureClient.getHistory(anyLong(), anyObject(String.class)))
					.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT, null, Collections.singletonList(mockReturn1)))
					.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,null, Collections.singletonList(mockReturn2)))
					.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT,null, Collections.singletonList(mockReturn3)));
		replay(mockEfactureClient);

		assertNotNull("Une demande est déjà en cours; getInscriptionEnCoursDeTraitement() ne doit pas renvoyer null", efactureService.getDemandeEnAttente(123));
		assertNotNull("Une demande est déjà en cours; getInscriptionEnCoursDeTraitement() ne doit pas renvoyer null", efactureService.getDemandeEnAttente(123));
		assertNull("Aucune demande n'est en cours; getInscriptionEnCoursDeTraitement() doit renvoyer null", efactureService.getDemandeEnAttente(123));

		verify(mockEfactureClient);
	}


	private PayerWithHistory buildPayerWithHistory(
			PayerStatus status,
			@Nullable List<PayerSituationHistoryEntry> sitHistory,
			@Nullable List<RegistrationRequestWithHistory> regHistory
	) {
		return new PayerWithHistory(new PayerId("business-Id", EFactureService.ACI_BILLER_ID),
				status,
				new PayerWithHistory.HistoryOfSituations(sitHistory),
				new PayerWithHistory.HistoryOfRequests(regHistory));
	}

	@Test
	public void testIdentifieContribuablePourInscription () throws Exception {

		final TestSamples ts = new TestSamples();

		doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				try {
					assertEquals(TypeRefusDemande.NUMERO_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(56758 ,NO_AVS_BERCLAZ));
					assertEquals(TypeRefusDemande.ADRESSE_COURRIER_INEXISTANTE, efactureService.identifieContribuablePourInscription(ts.idJacquier, NO_AVS_JACQUIER));
					assertEquals(TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(ts.idChollet ,NO_AVS_BERCLAZ));
					assertEquals(null, efactureService.identifieContribuablePourInscription(ts.idChollet ,NO_AVS_CHOLLET));
					assertEquals(TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(ts.idMenageBerclaz ,NO_AVS_CHOLLET));
					assertEquals(null, efactureService.identifieContribuablePourInscription(ts.idMenageBerclaz ,NO_AVS_BERCLAZ));
					assertEquals(null, efactureService.identifieContribuablePourInscription(ts.idMenageBerclaz ,NO_AVS_BOCHUZ));
				}
				catch (AdresseException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});
	}

	@Test
	public void testValideEtatContribuablePourInscription () throws Exception {

		EFactureClient mockEfactureClient = EasyMock.createMock(EFactureClient.class);
		efactureService.seteFactureClient(mockEfactureClient);
		expect(
				mockEfactureClient.getHistory(anyLong(), anyObject(String.class)))
				// 1er appel au mock renvoye une situation ou le contribuable a un DESINSCRIT_SUSPENDU
				.andReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT_SUSPENDU, Collections.singletonList(
						new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT_SUSPENDU).build()), DUMMY_HISTORY_OF_REQUEST))
				// appels suivants au mock renvoye une situation ou le contribuable a un statut DESINSCRIT
				.andStubReturn(buildPayerWithHistory(PayerStatus.DESINSCRIT, Collections.singletonList(new PayerSituationHistoryEntryBuilder().build()), DUMMY_HISTORY_OF_REQUEST));
		replay(mockEfactureClient);

		final TestSamples ts = new TestSamples();

		doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				assertFalse("la situation de Mr Cholet dans e-facture est a l'état desinscrit-suspendu, son état n'est pas valide ",
						efactureService.valideEtatFiscalContribuablePourInscription(ts.idChollet));
				assertTrue("la situation du ménage Berclaz est desinscrit et leur fors ouverts, son état est valide ", efactureService.valideEtatFiscalContribuablePourInscription(ts.idMenageBerclaz));
				assertFalse("Mr Jacquier n'a pas de for principal, son état n'est pas valide", efactureService.valideEtatFiscalContribuablePourInscription(ts.idJacquier));
				assertFalse("Mme Dessources est à la source et ne doit pas pouvoir s'inscrire",
						efactureService.valideEtatFiscalContribuablePourInscription(ts.idDessources));
				assertTrue("Mr Plutat n'habite plus le canton de VD mais devrait pouvoir tout de meme s'inscrire",
						efactureService.valideEtatFiscalContribuablePourInscription(ts.idPlulat));

				PersonnePhysique ctbJacquier = (PersonnePhysique) tiersService.getTiers(ts.idJacquier);
				// Correction d'une erreur honteuse, mr Jacquier a bien evidement un for principal a Lausanne !
				// mais  il est mort, du coup ça ne valide pas :(
				addForPrincipal(ctbJacquier, date(1998, 2, 1), MotifFor.MAJORITE, date(2012,1,1), MotifFor.VEUVAGE_DECES, MockCommune.Lausanne);
				assertFalse("Mr Jacquier est décédé, son état est invalide ", efactureService.valideEtatFiscalContribuablePourInscription(ts.idJacquier));
				return null;
			}
		});

		verify(mockEfactureClient);

	}

	private static final long NO_JACQUIER = 123456L;
	private static final long NO_CHOLLET = 223456L;
	private static final long NO_BOCHUZ = 345667L;
	private static final long NO_BERCLAZ = 987734L;
	private static final long NO_DESSOURCES = 987735L;
	private static final long NO_PLULAT = 987736L;

	private static final String NO_AVS_JACQUIER = "7562025802593";
	private static final String NO_AVS_CHOLLET = "7568700351431";
	private static final String NO_AVS_BERCLAZ = "7565817249033";
	private static final String NO_AVS_BOCHUZ = "7567902948722";

	private class TestSamples {

		long idJacquier, idChollet, idMenageBerclaz, idDessources, idPlulat ;

		TestSamples () throws Exception {

			final RegDate dateMariage = date(2002, 3, 4);

			serviceCivil.setUp(new MockServiceCivil() {
				@Override
				protected void init() {
					MockIndividu jacquier = addIndividu(NO_JACQUIER, date(1980,1,2), "Sébastien", "Jacquier", true); // sans adresse
					jacquier.setNouveauNoAVS(NO_AVS_JACQUIER);
					MockIndividu chollet = addIndividu(NO_CHOLLET, date(1980, 1, 2), "Ignacio", "Chollet", true);
					chollet.setNouveauNoAVS(NO_AVS_CHOLLET);
					addAdresse(chollet, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, date(1980, 1, 2), null);
					MockIndividu bochuz = addIndividu(NO_BOCHUZ, date(1980,1,2), "Sherley", "Bochuz", false);
					addAdresse(bochuz, TypeAdresseCivil.COURRIER, MockRue.Bex.RouteDuBoet, null, date(1980, 1, 2), dateMariage.getOneDayBefore());
					addAdresse(bochuz, TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateMariage, null);
					bochuz.setNouveauNoAVS(NO_AVS_BOCHUZ);
					MockIndividu berclaz = addIndividu(NO_BERCLAZ, date(1980,1,2), "Stève", "Berclaz", true);
					berclaz.setNouveauNoAVS(NO_AVS_BERCLAZ);
					addAdresse(berclaz, TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, date(1980, 1, 2), null);
					marieIndividus(berclaz, bochuz, dateMariage);
					MockIndividu dessources = addIndividu(NO_DESSOURCES, date(1980,1,2), "Manon", "Dessources", false);
					addNationalite(dessources, MockPays.France, date(1980,1,2), null);
					addAdresse(dessources, TypeAdresseCivil.COURRIER, MockRue.Lausanne.AvenueDeBeaulieu, null, date(2006, 1, 1), null);
					MockIndividu plulat = addIndividu(NO_PLULAT, date(1980, 1, 2), "Nabit", "Plulat", true);
					addAdresse(plulat, TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, date(1980, 1, 2), date(2010,1,1));
					addAdresse(plulat, TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2010,1,2), null);
				}
			});

			final Long[] ctbIds = doInNewTransaction(new TransactionCallback<Long[]>() {
				@Override
				public Long[] doInTransaction(TransactionStatus status) {
					PersonnePhysique pp1 = addHabitant(NO_JACQUIER);
					PersonnePhysique pp2 = addHabitant(NO_CHOLLET);
					addForPrincipal(pp2,date(1998,1,2), MotifFor.MAJORITE, MockCommune.Moudon);
					PersonnePhysique pp3 = addHabitant(NO_BERCLAZ);
					addForPrincipal(pp3,date(1998,1,2), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
					PersonnePhysique pp4 = addHabitant(NO_BOCHUZ);
					addForPrincipal(pp4,date(1998,1,2), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);
					MenageCommun mc = addEnsembleTiersCouple(pp3,pp4, dateMariage, null).getMenage();
					addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bussigny);
					PersonnePhysique pp5 = addHabitant(NO_DESSOURCES);
					addForPrincipal(pp5, date(2006,1,1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne, ModeImposition.SOURCE);
					PersonnePhysique pp6 = addNonHabitant("Nabit", "Plulat", date(1980,1,2), Sexe.MASCULIN);
					addForPrincipal(pp6, date(1998,1,2), MotifFor.MAJORITE, date(2010,1,1), MotifFor.DEPART_HC, MockCommune.Lausanne);
					return new Long[] {pp1.getNumero(), pp2.getNumero(), mc.getNumero(), pp5.getNumero(), pp6.getNumero()};
				}
			});
			idJacquier = ctbIds [0];
			idChollet = ctbIds [1];
			idMenageBerclaz = ctbIds[2];
			idDessources = ctbIds[3];
			idPlulat = ctbIds[4];
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	private static class PayerSituationHistoryEntryBuilder {

		private RegDate date = date(2012, 6, 1);
		private PayerStatus status = PayerStatus.DESINSCRIT;
		private String providerId = EFactureService.ACI_BILLER_ID;
		private BigInteger eBillAcountId = BigInteger.ZERO;
		private RegistrationMode regMode = RegistrationMode.STANDARD;
		private Integer reasonCode = null;
		private String descr = "";
		private String customField = "";

		PayerSituationHistoryEntry build() {
			return new PayerSituationHistoryEntry(
					XmlUtils.regdate2xmlcal(date),status,providerId,
					eBillAcountId, regMode, reasonCode, descr, customField);
		}

		PayerSituationHistoryEntryBuilder setDate(RegDate date) {
			this.date = date;
			return this;
		}

		PayerSituationHistoryEntryBuilder setStatus(PayerStatus status) {
			this.status = status;
			return this;
		}

		PayerSituationHistoryEntryBuilder setProviderId(String providerId) {
			this.providerId = providerId;
			return this;
		}

		PayerSituationHistoryEntryBuilder seteBillAcountId(BigInteger eBillAcountId) {
			this.eBillAcountId = eBillAcountId;
			return this;
		}

		PayerSituationHistoryEntryBuilder setRegMode(RegistrationMode regMode) {
			this.regMode = regMode;
			return this;
		}

		PayerSituationHistoryEntryBuilder setReasonCode(Integer reasonCode) {
			this.reasonCode = reasonCode;
			return this;
		}

		PayerSituationHistoryEntryBuilder setDescr(String descr) {
			this.descr = descr;
			return this;
		}

		PayerSituationHistoryEntryBuilder setCustomField(String customField) {
			this.customField = customField;
			return this;
		}
	}

	@Test
	public void testIdentificationMenageCommunDontMembrePrincipalSansNoAVS13() throws Exception {

		final long noIndLui = 436544748L;
		final long noIndElle = 4378456427L;
		final String noAvs = "7568700351431";
		final RegDate dateMariage = date(2008, 4, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndLui, null, "Chollet", "Alfonso", true);
				addAdresse(lui, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				final MockIndividu elle = addIndividu(noIndElle, null, "Chollet", "Sigourney", false);
				addAdresse(elle, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				elle.setNouveauNoAVS(noAvs);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndLui);
				final PersonnePhysique elle = addHabitant(noIndElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// tentative d'identification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TypeRefusDemande typeRefus = efactureService.identifieContribuablePourInscription(mcId, noAvs);
				assertNull(typeRefus);
				return null;
			}
		});
	}

	@Test
	public void testIdentificationMenageCommunDontMembreConjointSansNoAVS13() throws Exception {

		final long noIndLui = 436544748L;
		final long noIndElle = 4378456427L;
		final String noAvs = "7568700351431";
		final RegDate dateMariage = date(2008, 4, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndLui, null, "Chollet", "Alfonso", true);
				lui.setNouveauNoAVS(noAvs);
				addAdresse(lui, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				final MockIndividu elle = addIndividu(noIndElle, null, "Chollet", "Sigourney", false);
				addAdresse(elle, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndLui);
				final PersonnePhysique elle = addHabitant(noIndElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// tentative d'identification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TypeRefusDemande typeRefus = efactureService.identifieContribuablePourInscription(mcId, noAvs);
				assertNull(typeRefus);
				return null;
			}
		});
	}

	@Test
	public void testIdentificationPersonnePhysiqueSansNoAVS13() throws Exception {

		final long noInd = 436544748L;
		final String noAvs = "7568700351431";
		final RegDate dateNaissance = date(1950, 8, 25);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noInd, dateNaissance, "Chollet", "Alfonso", true);
				addAdresse(lui, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateNaissance, null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		// tentative d'identification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TypeRefusDemande typeRefus = efactureService.identifieContribuablePourInscription(ppId, noAvs);
				assertEquals(TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT, typeRefus);
				return null;
			}
		});
	}

	@Test
	public void testIdentificationMenageCommunSansAucunNoAVS13() throws Exception {

		final long noIndLui = 436544748L;
		final long noIndElle = 4378456427L;
		final String noAvs = "7568700351431";
		final RegDate dateMariage = date(2008, 4, 12);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndLui, null, "Chollet", "Alfonso", true);
				addAdresse(lui, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				final MockIndividu elle = addIndividu(noIndElle, null, "Chollet", "Sigourney", false);
				addAdresse(elle, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, dateMariage, null);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		// mise en place fiscale
		final long mcId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndLui);
				final PersonnePhysique elle = addHabitant(noIndElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				return couple.getMenage().getNumero();
			}
		});

		// tentative d'identification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final TypeRefusDemande typeRefus = efactureService.identifieContribuablePourInscription(mcId, noAvs);
				assertEquals(TypeRefusDemande.NUMERO_AVS_CTB_INCOHERENT, typeRefus);
				return null;
			}
		});
	}

	@Test
	public void testIdentificationContribuablePasDonneeNoAvs() throws Exception {
		final long noInd = 436544748L;
		final String noAvs = "7568700351431";

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noInd, null, "Chollet", "Alfonso", true);
				lui.setNouveauNoAVS(noAvs);
				addAdresse(lui, TypeAdresseCivil.COURRIER, MockRue.Prilly.RueDesMetiers, null, date(2007, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noInd);
				return lui.getNumero();
			}
		});

		// tentative d'identification
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				assertEquals(TypeRefusDemande.NUMERO_AVS_INVALIDE, efactureService.identifieContribuablePourInscription(ppId, null));
				assertEquals(TypeRefusDemande.NUMERO_AVS_INVALIDE, efactureService.identifieContribuablePourInscription(ppId, ""));
				assertEquals(TypeRefusDemande.NUMERO_AVS_INVALIDE, efactureService.identifieContribuablePourInscription(ppId, "     "));
				assertEquals(TypeRefusDemande.NUMERO_AVS_INVALIDE, efactureService.identifieContribuablePourInscription(ppId, "effewe"));
				return null;
			}
		});
	}
}
