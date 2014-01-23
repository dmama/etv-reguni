package ch.vd.uniregctb.webservices.v5;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationKey;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.security.v1.AllowedAccess;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.xml.DataHelper;

public class BusinessWebServiceTest extends WebserviceTest {

	private BusinessWebService service;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(BusinessWebService.class, "v5Business");
	}

	private static void assertValidInteger(long value) {
		Assert.assertTrue(Long.toString(value), value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE);
	}

	@Test
	public void testBlocageRemboursementAuto() throws Exception {

		final UserLogin login = new UserLogin(getDefaultOperateurName(), 22);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Francis", "Noire", date(1965, 8, 31), Sexe.MASCULIN);
				pp.setBlocageRemboursementAutomatique(false);
				return pp.getNumero();
			}
		});
		assertValidInteger(ppId);

		// vérification du point de départ
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});

		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId, login));

		// appel du WS (= sans changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, login, false);

		// vérification
		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId, login));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, login, true);

		// vérification
		Assert.assertTrue(service.getAutomaticRepaymentBlockingFlag((int) ppId, login));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, login, false);

		// vérification
		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId, login));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});
	}

	private void assertAllowedAccess(String visa, int partyNo, AllowedAccess expectedAccess) {
		final SecurityResponse access = service.getSecurityOnParty(visa, partyNo);
		Assert.assertNotNull(access);
		Assert.assertEquals(visa, access.getUser());
		Assert.assertEquals(partyNo, access.getPartyNo());
		Assert.assertEquals(expectedAccess, access.getAllowedAccess());
	}

	@Test
	public void testSecurite() throws Exception {

		final String visaOmnipotent = "omnis";
		final long noIndividuOmnipotent = 45121L;
		final String visaActeur = "bébel";
		final long noIndividuActeur = 4131544L;
		final String visaVoyeur = "fouineur";
		final long noIndividuVoyeur = 857451L;
		final String visaGrouillot = "larve";
		final long noIndividuGrouillot = 378362L;

		// mise en place du service sécurité
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOmnipotent, noIndividuOmnipotent, Role.VISU_ALL.getIfosecCode(), Role.ECRITURE_DOSSIER_PROTEGE.getIfosecCode());
				addOperateur(visaActeur, noIndividuActeur, Role.VISU_ALL.getIfosecCode());
				addOperateur(visaVoyeur, noIndividuVoyeur, Role.VISU_ALL.getIfosecCode());
				addOperateur(visaGrouillot, noIndividuGrouillot, Role.VISU_ALL.getIfosecCode());
			}
		});

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		final class Ids {
			long idProtege;
			long idConjointDeProtege;
			long idCoupleProtege;
			long idNormal;
			long idCoupleNormal;
		}

		// création des contribuables avec éventuelle protections
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique protege = addNonHabitant("Jürg", "Bunker", date(1975, 4, 23), Sexe.MASCULIN);
				final PersonnePhysique conjointDeProtege = addNonHabitant("Adelheid", "Bunker", date(1974, 7, 31), Sexe.FEMININ);
				final EnsembleTiersCouple coupleProtege = addEnsembleTiersCouple(protege, conjointDeProtege, date(2010, 6, 3), null);
				addDroitAcces(noIndividuActeur, protege, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(2010, 1, 1), null);
				addDroitAcces(noIndividuVoyeur, protege, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2010, 1, 1), null);

				final PersonnePhysique normal = addNonHabitant("Emile", "Gardavou", date(1962, 7, 4), Sexe.MASCULIN);
				final EnsembleTiersCouple coupleNormal = addEnsembleTiersCouple(normal, null, date(1987, 6, 5), null);

				final Ids ids = new Ids();
				ids.idProtege = protege.getNumero();
				ids.idConjointDeProtege = conjointDeProtege.getNumero();
				ids.idCoupleProtege = coupleProtege.getMenage().getNumero();
				ids.idNormal = normal.getNumero();
				ids.idCoupleNormal = coupleNormal.getMenage().getNumero();
				return ids;
			}
		});
		assertValidInteger(ids.idProtege);
		assertValidInteger(ids.idConjointDeProtege);
		assertValidInteger(ids.idCoupleProtege);
		assertValidInteger(ids.idNormal);
		assertValidInteger(ids.idCoupleNormal);

		// vérifications de la réponse du service

		// l'omnipotent peut tout faire
		{
			assertAllowedAccess(visaOmnipotent, (int) ids.idProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, (int) ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, (int) ids.idCoupleProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, (int) ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, (int) ids.idCoupleNormal, AllowedAccess.READ_WRITE);
		}

		// l'acteur peut tout faire également
		{
			assertAllowedAccess(visaActeur, (int) ids.idProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, (int) ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, (int) ids.idCoupleProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, (int) ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, (int) ids.idCoupleNormal, AllowedAccess.READ_WRITE);
		}

		// le voyeur, lui, ne peut pas modifier ce qui est protégé, mais peut le voir
		{
			assertAllowedAccess(visaVoyeur, (int) ids.idProtege, AllowedAccess.READ_ONLY);
			assertAllowedAccess(visaVoyeur, (int) ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaVoyeur, (int) ids.idCoupleProtege, AllowedAccess.READ_ONLY);
			assertAllowedAccess(visaVoyeur, (int) ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaVoyeur, (int) ids.idCoupleNormal, AllowedAccess.READ_WRITE);
		}

		// le voyeur, lui, ne peut pas voir ce qui est protégé
		{
			assertAllowedAccess(visaGrouillot, (int) ids.idProtege, AllowedAccess.NONE);
			assertAllowedAccess(visaGrouillot, (int) ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaGrouillot, (int) ids.idCoupleProtege, AllowedAccess.NONE);
			assertAllowedAccess(visaGrouillot, (int) ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaGrouillot, (int) ids.idCoupleNormal, AllowedAccess.READ_WRITE);
		}
	}

	@Test
	public void testQuittancementDeclaration() throws Exception {

		final int annee = 2012;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		final class Data {
			long pp1;
			long pp2;
		}

		// mise en place fiscale
		final Data data = doInNewTransactionAndSession(new TransactionCallback<Data>() {
			@Override
			public Data doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addCedi();
				final RegDate debut = date(annee, 1, 1);
				final RegDate fin = date(annee, 12, 31);

				final PersonnePhysique pp1 = addNonHabitant("Francis", "Noire", date(1965, 8, 31), Sexe.MASCULIN);
				addForPrincipal(pp1, debut, MotifFor.ARRIVEE_HS, fin, MotifFor.DEPART_HS, MockCommune.Aigle);
				final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp1, pf, debut, fin, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di1, date(annee + 1, 1, 22));

				final PersonnePhysique pp2 = addNonHabitant("Louise", "Defuneste", date(1943, 5, 12), Sexe.FEMININ);
				addForPrincipal(pp2, debut, MotifFor.ARRIVEE_HS, fin, MotifFor.DEPART_HS, MockCommune.Aubonne);
				final DeclarationImpotOrdinaire di2 = addDeclarationImpot(pp2, pf, debut, fin, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di2, date(annee + 1, 1, 22));

				final Data data = new Data();
				data.pp1 = pp1.getNumero();
				data.pp2 = pp2.getNumero();
				return data;
			}
		});
		assertValidInteger(data.pp1);
		assertValidInteger(data.pp2);

		// quittancement
		final OrdinaryTaxDeclarationKey key1 = new OrdinaryTaxDeclarationKey((int) data.pp1, annee, 2);
		final OrdinaryTaxDeclarationKey key2 = new OrdinaryTaxDeclarationKey((int) data.pp2, annee, 1);

		final Map<OrdinaryTaxDeclarationKey, AckStatus> expected = new HashMap<>();
		expected.put(key1, AckStatus.ERROR_UNKNOWN_TAX_DECLARATION);
		expected.put(key2, AckStatus.OK);

		final List<OrdinaryTaxDeclarationKey> keys = Arrays.asList(key1, key2);
		final OrdinaryTaxDeclarationAckRequest req = new OrdinaryTaxDeclarationAckRequest("ADDO", DataHelper.coreToXML(DateHelper.getCurrentDate()), keys);
		final OrdinaryTaxDeclarationAckResponse resp = service.ackOrdinaryTaxDeclarations(new UserLogin(getDefaultOperateurName(), 22), req);
		Assert.assertNotNull(resp);

		// vérification des codes retour
		final List<OrdinaryTaxDeclarationAckResult> result = resp.getAckResult();
		Assert.assertNotNull(result);
		Assert.assertEquals(keys.size(), result.size());
		for (OrdinaryTaxDeclarationAckResult ack : result) {
			Assert.assertNotNull(ack);

			final AckStatus expectedStatus = expected.get(ack.getDeclaration());
			Assert.assertNotNull(ack.toString(), expectedStatus);
			Assert.assertEquals(ack.toString(), expectedStatus, ack.getStatus());
		}

		// vérification en base
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				for (Map.Entry<OrdinaryTaxDeclarationKey, AckStatus> entry : expected.entrySet()) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(entry.getKey().getPartyNo(), false);
					final List<Declaration> decls = pp.getDeclarationsForPeriode(annee, false);
					Assert.assertNotNull(decls);
					Assert.assertEquals(1, decls.size());

					final Declaration decl = decls.get(0);
					Assert.assertNotNull(decl);

					final EtatDeclaration etat = decl.getDernierEtat();
					if (entry.getValue() == AckStatus.OK) {
						Assert.assertEquals(TypeEtatDeclaration.RETOURNEE, etat.getEtat());
					}
					else {
						Assert.assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
					}
				}
			}
		});
	}

	@Test
	public void testNouveauDelaiDeclarationImpot() throws Exception {

		final int annee = 2012;
		final RegDate delaiInitial = date(annee + 1, 6, 30);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// personne...
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				addCedi();
				final RegDate debut = date(annee, 1, 1);
				final RegDate fin = date(annee, 12, 31);

				final PersonnePhysique pp = addNonHabitant("Francis", "Noire", date(1965, 8, 31), Sexe.MASCULIN);
				addForPrincipal(pp, debut, MotifFor.ARRIVEE_HS, fin, MotifFor.DEPART_HS, MockCommune.Aigle);
				final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, pf, debut, fin, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di1, date(annee + 1, 1, 22));
				addDelaiDeclaration(di1, date(annee + 1, 1, 22), delaiInitial);
				return pp.getNumero();
			}
		});
		assertValidInteger(ppId);

		// vérification du délai existant
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final Declaration di = pp.getDeclarationActive(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelais();
				Assert.assertNotNull(delai);
				Assert.assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});

		// demande de délai qui échoue (délai plus ancien)
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToXML(delaiInitial.addMonths(-2)), DataHelper.coreToXML(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, new UserLogin(getDefaultOperateurName(), 22), req);
			Assert.assertNotNull(resp);
			Assert.assertEquals(DeadlineStatus.ERROR_INVALID_DEADLINE, resp.getStatus());
		}

		// vérification du délai qui ne devrait pas avoir bougé
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final Declaration di = pp.getDeclarationActive(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelais();
				Assert.assertNotNull(delai);
				Assert.assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});


		// demande de délai qui marche
		final RegDate nouveauDelai = RegDateHelper.maximum(delaiInitial.addMonths(1), RegDate.get(), NullDateBehavior.LATEST);
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToXML(nouveauDelai), DataHelper.coreToXML(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, new UserLogin(getDefaultOperateurName(), 22), req);
			Assert.assertNotNull(resp);
			Assert.assertEquals(resp.getAdditionalMessage(), DeadlineStatus.OK, resp.getStatus());
		}

		// vérification du nouveau délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final Declaration di = pp.getDeclarationActive(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelais();
				Assert.assertNotNull(delai);
				Assert.assertEquals(nouveauDelai, delai.getDelaiAccordeAu());
			}
		});
	}
}
