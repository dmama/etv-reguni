package ch.vd.uniregctb.efacture;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

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
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.junit.Assert.assertEquals;

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

		EFactureClient mockEfactureClient = new EFactureClient() {

			int callCount = 0;

			PayerWithHistory[] mockResult = new PayerWithHistory[] {
					// 1er appel au client eFacture renvoie un contribuable inscrit
					buildPayerWithHistory(PayerStatus.INSCRIT, Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.INSCRIT).build()),DUMMY_HISTORY_OF_REQUEST),

					// 2eme appel au client eFacture renvoie un contribuable avec la situation DESINSCRIT avec une demande en attente de signature
					buildPayerWithHistory(PayerStatus.DESINSCRIT,
					                      Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					                      Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
							                                                .addHistoryEntry(date(2012,6,1),RegistrationRequestStatus.VALIDATION_EN_COURS,TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(),"","").buildRegistrationRequestWithHistory())),

					// 3eme appel au client eFacture renvoie un contribuable avec la situation DESINSCRIT avec une demande en attente de contact
					buildPayerWithHistory(PayerStatus.DESINSCRIT,
					                      Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					                      Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
							                                                .addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteDemande.EN_ATTENTE_CONTACT.getCode(), "", "")
							                                                .buildRegistrationRequestWithHistory())),

					// 4eme appel au client eFacture renvoie un contribuable avec la situation DESINSCRIT avec une demande validée
					buildPayerWithHistory(PayerStatus.DESINSCRIT,
					                      Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					                      Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
							                                                .addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.VALIDEE, null, "", "").buildRegistrationRequestWithHistory())),

					// 5eme appel au client eFacture renvoie un contribualble avec la situation DESINSCRIT avec une demande refusée
					buildPayerWithHistory(PayerStatus.DESINSCRIT,
					                      Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					                      Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
							                                                .addHistoryEntry(date(2012, 6, 1), RegistrationRequestStatus.REFUSEE, null, "", "").buildRegistrationRequestWithHistory())),

					// 6ème appel renvoie null (contribuable inconnu d'e-facture)
					null,

					// 7ème appel au client eFacture renvoie un contribuable avec la situation DESINSCRIT et une demande en attente de signature
					buildPayerWithHistory(PayerStatus.DESINSCRIT,
					                      Collections.singletonList(new PayerSituationHistoryEntryBuilder().setStatus(PayerStatus.DESINSCRIT).build()),
					                      Collections.singletonList(new DemandeAvecHistoBuilderForUnitTests()
							                                                .addHistoryEntry(date(2012,6,1),RegistrationRequestStatus.VALIDATION_EN_COURS,TypeAttenteDemande.EN_ATTENTE_SIGNATURE.getCode(),"","").buildRegistrationRequestWithHistory()))

			};

			@Override
			public PayerWithHistory getHistory(long ctbId, String billerId) {
				return mockResult[callCount++];
			}
		};

		final EFactureMessageSender eFactureMessageSender = new MockEFactureMessageSender() {
			@Override
			public String envoieAcceptationDemandeInscription(String idDemande, boolean retourAttendu, String description) throws EvenementEfactureException {
				assertEquals(true, retourAttendu);
				return BUSINESS_ID;
			}
		};
		efactureService.seteFactureClient(mockEfactureClient);
		efactureService.seteFactureMessageSender(eFactureMessageSender);

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
					Assert.assertEquals("le contribuable Jacquier existe, mais est inconnu d'e-facture", ResultatQuittancement.aucuneDemandeEnAttenteDeSignature(), efactureService.quittancer(ts.idJacquier));
					Assert.assertEquals("le contribuable Jacquier n'est pas dans un etat fiscale coherent (pas de for)", ResultatQuittancement.etatFiscalIncoherent(), efactureService.quittancer(ts.idJacquier));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

	}

	private PayerWithHistory buildPayerWithHistory(
			PayerStatus status,
			@Nullable List<PayerSituationHistoryEntry> sitHistory,
			@Nullable List<RegistrationRequestWithHistory> regHistory) {

		return new PayerWithHistory(new PayerId("business-Id", EFactureService.ACI_BILLER_ID),
				status,
				new PayerWithHistory.HistoryOfSituations(sitHistory),
				new PayerWithHistory.HistoryOfRequests(regHistory));
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
					addAdresse(bochuz, TypeAdresseCivil.COURRIER, MockRue.Bex.CheminDeLaForet, null, date(1980, 1, 2), dateMariage.getOneDayBefore());
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
		private String email = "toto@titi.org";

		PayerSituationHistoryEntry build() {
			return new PayerSituationHistoryEntry(
					XmlUtils.regdate2xmlcal(date),status, providerId,
					eBillAcountId, email, regMode, reasonCode, null, descr, customField);
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
}
