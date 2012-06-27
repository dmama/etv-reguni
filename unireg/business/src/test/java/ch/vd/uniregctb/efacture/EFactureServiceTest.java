package ch.vd.uniregctb.efacture;

import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.evd0025.v1.PayerId;
import ch.vd.evd0025.v1.PayerWithHistory;
import ch.vd.evd0025.v1.RegistrationRequestStatus;
import ch.vd.evd0025.v1.RegistrationRequestWithHistory;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.wsclient.efacture.EFactureClient;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeAdresseCivil;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class EFactureServiceTest extends BusinessTest {

	private EFactureServiceImpl efactureService;
	private EFactureClient mockEfactureClient;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		efactureService = getBean(EFactureServiceImpl.class, "efactureService");
		mockEfactureClient = EasyMock.createMock(EFactureClient.class);
		efactureService.seteFactureClient(mockEfactureClient);
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


		RegistrationRequestWithHistory mockReturn1 =
				new DemandeValidationInscriptionAvecHistoriqueBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteEFacture.EN_ATTENTE_CONTACT.getCode(), "", "")
						.buildRegistrationRequestWithHistory();

		RegistrationRequestWithHistory mockReturn2 =
				new DemandeValidationInscriptionAvecHistoriqueBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 5, 26), RegistrationRequestStatus.REFUSEE, TypeAttenteEFacture.PAS_EN_ATTENTE.getCode(), "","")
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteEFacture.EN_ATTENTE_CONTACT.getCode(), "", "")
						.buildRegistrationRequestWithHistory();

		RegistrationRequestWithHistory mockReturn3 =
				new DemandeValidationInscriptionAvecHistoriqueBuilderForUnitTests()
						.addHistoryEntry(RegDate.get(2012, 6, 26), RegistrationRequestStatus.VALIDATION_EN_COURS, TypeAttenteEFacture.PAS_EN_ATTENTE.getCode(), "", "")
						.buildRegistrationRequestWithHistory();

		RegistrationRequestWithHistory mockReturn4 =
				new DemandeValidationInscriptionAvecHistoriqueBuilderForUnitTests()
						.buildRegistrationRequestWithHistory();

		expect(
				mockEfactureClient.getHistory(anyLong(), anyObject(String.class)))
					.andReturn(buildPayerWithHistory(Collections.singletonList(mockReturn1)))
					.andReturn(buildPayerWithHistory(Collections.singletonList(mockReturn2)))
					.andReturn(buildPayerWithHistory(Collections.singletonList(mockReturn3)))
					.andReturn(buildPayerWithHistory(Collections.singletonList(mockReturn4)));
		replay(mockEfactureClient);

		assertNotNull("Une demande est déjà en cours; getInscriptionEnCoursDeTraitement() ne doit pas renvoyer null", efactureService.getDemandeInscriptionEnCoursDeTraitement(123));
		assertNotNull("Une demande est déjà en cours; getInscriptionEnCoursDeTraitement() ne doit pas renvoyer null", efactureService.getDemandeInscriptionEnCoursDeTraitement(123));
		assertNull("Aucune demande n'est en cours; getInscriptionEnCoursDeTraitement() doit renvoyer null", efactureService.getDemandeInscriptionEnCoursDeTraitement(123));
		try {
			efactureService.getDemandeInscriptionEnCoursDeTraitement(123);
			fail("Une demande sans historique doit lever une exception");
		} catch (IllegalArgumentException e) {}

		verify(mockEfactureClient);
	}


	private PayerWithHistory buildPayerWithHistory(List<RegistrationRequestWithHistory> history) {
		return new PayerWithHistory(new PayerId("business-Id", EFactureEvent.ACI_BILLER_ID),null, new PayerWithHistory.HistoryOfRequests(history));
	}

	@Test
	public void testIdentifieContribuablePourInscription () throws Exception {
		// Echantillon de Test :
		//  - Mr Jacquier n'a pas d'adresse et ne doit pas pouvoir s'inscrire
		//  - Mr Chollet a un test est doit pouvoir s'incrire a condition que l'on fournisse le bon numéro avs.
		//  - Le ménage commun Sherley Bochuz & Stève Berclaz devrait pouvoir également s'incrire a condition que l'on fournisse le bon numéro avs.

		final long noJacquier= 123456L, noChollet= 223456L, noBochuz = 345667L, noBerclaz = 987734L ;
		final String noAvsJacquier = "7562025802593", noAvsChollet = "7568700351431", noAvsBerclaz = "7565817249033", noAvsBochuz = "7567902948722";
		final RegDate dateMariage = date(2002,3,4);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu jacquier = addIndividu(noJacquier, date(1980,1,2), "Sébastien", "Jacquier", true); // sans adresse
				jacquier.setNouveauNoAVS(noAvsJacquier);
				MockIndividu chollet = addIndividu(noChollet , date(1980, 1, 2), "Ignacio", "Chollet", true);
				chollet.setNouveauNoAVS(noAvsChollet);
				addAdresse(chollet, TypeAdresseCivil.COURRIER, MockRue.Moudon.LeBourg, null, date(1980, 1, 2), null);
				MockIndividu bochuz = addIndividu(noBochuz, date(1980,1,2), "Sherley", "Bochuz", false);
				addAdresse(bochuz, TypeAdresseCivil.COURRIER, MockRue.Bex.RouteDuBoet, null, date(1980, 1, 2), dateMariage.getOneDayBefore());
				addAdresse(bochuz, TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateMariage, null);
				bochuz.setNouveauNoAVS(noAvsBochuz);
				MockIndividu berclaz = addIndividu(noBerclaz, date(1980,1,2), "Stève", "Berclaz", true);
				berclaz.setNouveauNoAVS(noAvsBerclaz);
				addAdresse(berclaz, TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, date(1980, 1, 2), null);
				marieIndividus(berclaz,bochuz, dateMariage);
			}
		});

		final Long[] ctbIds = doInNewTransaction(new TransactionCallback<Long[]>() {
			@Override
			public Long[] doInTransaction(TransactionStatus status) {
				PersonnePhysique pp1 = addHabitant(noJacquier);
				PersonnePhysique pp2 = addHabitant(noChollet);
				PersonnePhysique pp3 = addHabitant(noBerclaz);
				PersonnePhysique pp4 = addHabitant(noBochuz);
				MenageCommun mc = addEnsembleTiersCouple(pp3,pp4, dateMariage, null).getMenage();
				return new Long[] {pp1.getNumero(), pp2.getNumero(), mc.getNumero()};
			}
		});

		final long idJacquier = ctbIds [0], idChollet = ctbIds [1], idMenageBerclaz = ctbIds[2];

		doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				try {
					assertEquals(TypeRefusEFacture.NUMERO_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(56758 ,noAvsBerclaz));
					assertEquals(TypeRefusEFacture.ADRESSE_COURRIER_INEXISTANTE, efactureService.identifieContribuablePourInscription(idJacquier ,noAvsJacquier));
					assertEquals(TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(idChollet ,noAvsBerclaz));
					assertEquals(null, efactureService.identifieContribuablePourInscription(idChollet ,noAvsChollet));
					assertEquals(TypeRefusEFacture.NUMERO_AVS_CTB_INCOHERENT, efactureService.identifieContribuablePourInscription(idMenageBerclaz ,noAvsChollet));
					assertEquals(null, efactureService.identifieContribuablePourInscription(idMenageBerclaz ,noAvsBerclaz));
					assertEquals(null, efactureService.identifieContribuablePourInscription(idMenageBerclaz ,noAvsBochuz));
				}
				catch (AdresseException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

	}

}
