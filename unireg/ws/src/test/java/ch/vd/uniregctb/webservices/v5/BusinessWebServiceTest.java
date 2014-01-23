package ch.vd.uniregctb.webservices.v5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.ws.ack.v1.AckStatus;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v1.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v1.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v1.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v1.DeadlineStatus;
import ch.vd.unireg.ws.modifiedtaxpayers.v1.PartyNumberList;
import ch.vd.unireg.ws.security.v1.AllowedAccess;
import ch.vd.unireg.ws.security.v1.SecurityResponse;
import ch.vd.unireg.ws.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.address.v2.Address;
import ch.vd.unireg.xml.party.address.v2.AddressInformation;
import ch.vd.unireg.xml.party.address.v2.AddressType;
import ch.vd.unireg.xml.party.address.v2.FormattedAddress;
import ch.vd.unireg.xml.party.address.v2.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v2.TariffZone;
import ch.vd.unireg.xml.party.adminauth.v3.AdministrativeAuthority;
import ch.vd.unireg.xml.party.corporation.v3.Corporation;
import ch.vd.unireg.xml.party.debtor.v3.Debtor;
import ch.vd.unireg.xml.party.immovableproperty.v2.ImmovableProperty;
import ch.vd.unireg.xml.party.othercomm.v1.LegalForm;
import ch.vd.unireg.xml.party.othercomm.v1.OtherCommunity;
import ch.vd.unireg.xml.party.person.v3.CommonHousehold;
import ch.vd.unireg.xml.party.person.v3.NaturalPerson;
import ch.vd.unireg.xml.party.person.v3.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v3.NaturalPersonCategoryType;
import ch.vd.unireg.xml.party.person.v3.Sex;
import ch.vd.unireg.xml.party.relation.v2.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v2.RelationBetweenPartiesType;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationKey;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxdeclaration.v3.TaxPeriod;
import ch.vd.unireg.xml.party.taxpayer.v3.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v3.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v3.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v2.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v2.ManagingTaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v2.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v2.SimplifiedTaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v2.SimplifiedTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationMethod;
import ch.vd.unireg.xml.party.taxresidence.v2.TaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v2.WithholdingTaxationPeriodType;
import ch.vd.unireg.xml.party.v3.Party;
import ch.vd.unireg.xml.party.v3.PartyInfo;
import ch.vd.unireg.xml.party.v3.PartyPart;
import ch.vd.unireg.xml.party.v3.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;
import ch.vd.uniregctb.common.ObjectNotFoundException;
import ch.vd.uniregctb.common.WebserviceTest;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.interfaces.service.mock.MockServicePM;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.rf.TypeImmeuble;
import ch.vd.uniregctb.rf.TypeMutation;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.FormeJuridique;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeDroitAcces;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.webservices.common.UserLogin;

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
		final TaxDeclarationKey key1 = new TaxDeclarationKey((int) data.pp1, annee, 2);
		final TaxDeclarationKey key2 = new TaxDeclarationKey((int) data.pp2, annee, 1);

		final Map<TaxDeclarationKey, AckStatus> expected = new HashMap<>();
		expected.put(key1, AckStatus.ERROR_UNKNOWN_TAX_DECLARATION);
		expected.put(key2, AckStatus.OK);

		final List<TaxDeclarationKey> keys = Arrays.asList(key1, key2);
		final OrdinaryTaxDeclarationAckRequest req = new OrdinaryTaxDeclarationAckRequest("ADDO", DataHelper.coreToWeb(DateHelper.getCurrentDate()), keys);
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
				for (Map.Entry<TaxDeclarationKey, AckStatus> entry : expected.entrySet()) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(entry.getKey().getTaxpayerNumber(), false);
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
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(delaiInitial.addMonths(-2)), DataHelper.coreToWeb(RegDate.get()));
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
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(nouveauDelai), DataHelper.coreToWeb(RegDate.get()));
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

	@Test
	public void testGetTaxOffices() throws Exception {

		final class Ids {
			long idVevey;
			long idPaysDEnhaut;
		}

		// préparation
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final CollectiviteAdministrative pays = addCollAdm(MockOfficeImpot.OID_PAYS_D_ENHAUT);
				final CollectiviteAdministrative vevey = addCollAdm(MockOfficeImpot.OID_VEVEY);

				final Ids ids = new Ids();
				ids.idVevey = vevey.getNumero();
				ids.idPaysDEnhaut = pays.getNumero();
				return ids;
			}
		});

		// une commune vaudoise
		{
			final TaxOffices taxOffices = service.getTaxOffices(MockCommune.ChateauDoex.getNoOFS(), null);
			Assert.assertNotNull(taxOffices);
			Assert.assertNotNull(taxOffices.getDistrict());
			Assert.assertNotNull(taxOffices.getRegion());
			Assert.assertEquals(ids.idPaysDEnhaut, taxOffices.getDistrict().getPartyNo());
			Assert.assertEquals(MockOfficeImpot.OID_PAYS_D_ENHAUT.getNoColAdm(), taxOffices.getDistrict().getAdmCollNo());
			Assert.assertEquals(ids.idVevey, taxOffices.getRegion().getPartyNo());
			Assert.assertEquals(MockOfficeImpot.OID_VEVEY.getNoColAdm(), taxOffices.getRegion().getAdmCollNo());
		}

		// une commune hors-canton
		try {
			final TaxOffices taxOffices = service.getTaxOffices(MockCommune.Bern.getNoOFS(), null);
			Assert.fail();
		}
		catch (ObjectNotFoundException e) {
			Assert.assertEquals(String.format("Commune %d inconnue dans le canton de Vaud.", MockCommune.Bern.getNoOFS()), e.getMessage());
		}

		// une commune inconnue
		try {
			final TaxOffices taxOffices = service.getTaxOffices(99999, null);
			Assert.fail();
		}
		catch (ObjectNotFoundException e) {
			Assert.assertEquals("Commune 99999 inconnue dans le canton de Vaud.", e.getMessage());
		}
	}

	@Test
	public void testGetModifiedTaxPayers() throws Exception {

		final Pair<Long, Date> pp1 = doInNewTransactionAndSession(new TransactionCallback<Pair<Long, Date>>() {
			@Override
			public Pair<Long, Date> doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Philippe", "Lemol", date(1956, 9, 30), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return Pair.<Long, Date>of(pp.getNumero(), pp.getLogModifDate());
			}
		});

		Thread.sleep(1000);
		final Pair<Long, Date> pp2 = doInNewTransactionAndSession(new TransactionCallback<Pair<Long, Date>>() {
			@Override
			public Pair<Long, Date> doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Albert", "Duchmol", date(1941, 5, 4), Sexe.MASCULIN);
				addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
				return Pair.<Long, Date>of(pp.getNumero(), pp.getLogModifDate());
			}
		});

		final Date start = new Date(pp1.getRight().getTime() - 100);
		final Date middle = new Date(pp2.getRight().getTime() - 100);
		Assert.assertTrue(pp1.getRight().before(middle));
		final Date end = new Date(pp2.getRight().getTime() + 100);
		final Date now = new Date(pp2.getRight().getTime() + 200);

		// rien
		final PartyNumberList none = service.getModifiedTaxPayers(new UserLogin(getDefaultOperateurName(), 22), end, now);
		Assert.assertNotNull(none);
		Assert.assertNotNull(none.getPartyNo());
		Assert.assertEquals(0, none.getPartyNo().size());

		// 1 contribuable
		final PartyNumberList one = service.getModifiedTaxPayers(new UserLogin(getDefaultOperateurName(), 22), middle, now);
		Assert.assertNotNull(one);
		Assert.assertNotNull(one.getPartyNo());
		Assert.assertEquals(1, one.getPartyNo().size());
		Assert.assertEquals(pp2.getLeft().longValue(), one.getPartyNo().get(0).longValue());

		// 2 contribuables
		final PartyNumberList two = service.getModifiedTaxPayers(new UserLogin(getDefaultOperateurName(), 22), start, now);
		Assert.assertNotNull(two);
		Assert.assertNotNull(two.getPartyNo());
		Assert.assertEquals(2, two.getPartyNo().size());

		final List<Integer> sortedList = new ArrayList<>(two.getPartyNo());
		Collections.sort(sortedList);
		Assert.assertEquals(pp1.getLeft().longValue(), sortedList.get(0).longValue());
		Assert.assertEquals(pp2.getLeft().longValue(), sortedList.get(1).longValue());
	}

	@Test
	public void testDebtorInfo() throws Exception {

		final long dpiId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
				final PeriodeFiscale pf = addPeriodeFiscale(2013);
				final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				addListeRecapitulative(dpi, pf, date(2013, 4, 1), date(2013, 4, 30), md);
				addListeRecapitulative(dpi, pf, date(2013, 10, 1), date(2013, 10, 31), md);
				return dpi.getNumero();
			}
		});

		Assert.assertTrue(dpiId >= Integer.MIN_VALUE && dpiId <= Integer.MAX_VALUE);
		{
			final DebtorInfo info = service.getDebtorInfo(new UserLogin(getDefaultOperateurName(), 22), (int) dpiId, 2012);
			Assert.assertEquals((int) dpiId, info.getNumber());
			Assert.assertEquals(2012, info.getTaxPeriod());
			Assert.assertEquals(0, info.getNumberOfWithholdingTaxDeclarationsIssued());
			Assert.assertEquals(12, info.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
		{
			final DebtorInfo info = service.getDebtorInfo(new UserLogin(getDefaultOperateurName(), 22), (int) dpiId, 2013);
			Assert.assertEquals((int) dpiId, info.getNumber());
			Assert.assertEquals(2013, info.getTaxPeriod());
			Assert.assertEquals(2, info.getNumberOfWithholdingTaxDeclarationsIssued());
			Assert.assertEquals(12, info.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
	}

	@Test
	public void testSearchParty() throws Exception {

		final class Ids {
			long pp;
			long dpi;
		}

		final boolean onTheFly = globalTiersIndexer.isOnTheFlyIndexation();
		globalTiersIndexer.setOnTheFlyIndexation(true);
		final Ids ids;
		try {
			globalTiersIndexer.overwriteIndex();

			ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Gérard", "Nietmochevillage", date(1979, 5, 31), Sexe.MASCULIN);
					final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2013, 1, 1));
					addForDebiteur(dpi, date(2013, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Bussigny);

					final Ids ids = new Ids();
					ids.pp = pp.getNumero();
					ids.dpi = dpi.getNumero();
					return ids;
				}
			});

			// attente de la fin de l'indexation des deux tiers
			globalTiersIndexer.sync();
		}
		finally {
			globalTiersIndexer.setOnTheFlyIndexation(onTheFly);
		}

		// recherche avec le numéro
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), Long.toString(ids.pp),
			                                                null, SearchMode.IS_EXACTLY, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
			}
		}

		// recherche avec le numéro et une donnée bidon à côté (qui doit donc être ignorée)
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), Long.toString(ids.pp),
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
			}
		}

		// recherche sans le numéro et une donnée bidon à côté -> aucun résultat
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}

		// recherche par nom -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			Collections.sort(sortedRes, new Comparator<PartyInfo>() {
				@Override
				public int compare(PartyInfo o1, PartyInfo o2) {
					return o1.getNumber() - o2.getNumber();
				}
			});

			{
				final PartyInfo info = sortedRes.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.dpi, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.DEBTOR, info.getType());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
			}
		}

		// recherche par nom avec liste de types vide -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, false, Collections.<PartyType>emptySet(), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			Collections.sort(sortedRes, new Comparator<PartyInfo>() {
				@Override
				public int compare(PartyInfo o1, PartyInfo o2) {
					return o1.getNumber() - o2.getNumber();
				}
			});

			{
				final PartyInfo info = sortedRes.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.dpi, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.DEBTOR, info.getType());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
			}
		}

		// recherche par nom avec liste de types mauvaise -> aucun ne vient
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, false, EnumSet.of(PartyType.HOUSEHOLD), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}

		// recherche par nom avec liste de types des deux -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, false, EnumSet.of(PartyType.DEBTOR, PartyType.NATURAL_PERSON), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			Collections.sort(sortedRes, new Comparator<PartyInfo>() {
				@Override
				public int compare(PartyInfo o1, PartyInfo o2) {
					return o1.getNumber() - o2.getNumber();
				}
			});

			{
				final PartyInfo info = sortedRes.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.dpi, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.DEBTOR, info.getType());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
			}
		}

		// recherche par nom avec liste de types d'un seul -> seul celui-là vient
		{
			final List<PartyInfo> res = service.searchParty(new UserLogin(getDefaultOperateurName(), 22), null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, false, EnumSet.of(PartyType.DEBTOR), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.dpi, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.DEBTOR, info.getType());
			}
		}
	}

	@Test
	public void testGetParty() throws Exception {

		final long noEntreprise = 423672L;
		final long noIndividu = 2114324L;
		final RegDate dateNaissance = date(1964, 8, 31);
		final RegDate datePermisC = date(1990, 4, 21);
		final RegDate dateMariage = date(1987, 5, 1);
		final RegDate dateDebutContactIS = date(2012, 5, 1);
		final RegDate pmActivityStartDate = date(2000, 1, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Dufoin", "Balthazar", Sexe.MASCULIN);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				addPermis(ind, TypePermis.ETABLISSEMENT, datePermisC, null, false);
				marieIndividu(ind, dateMariage);
			}
		});

		servicePM.setUp(new MockServicePM() {
			@Override
			protected void init() {
				addPM(noEntreprise, "Au petit coin", "SA", pmActivityStartDate, null);
			}
		});

		final class Ids {
			long pp;
			long mc;
			long dpi;
			long pm;
			long ca;
			long ac;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				final DebiteurPrestationImposable dpi = addDebiteur("Débiteur IS", mc, dateDebutContactIS);
				dpi.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				final Entreprise pm = addEntreprise(noEntreprise);
				final CollectiviteAdministrative ca = addCollAdm(MockCollectiviteAdministrative.CAT);
				final AutreCommunaute ac = addAutreCommunaute("Tata!!");
				ac.setFormeJuridique(FormeJuridique.ASS);

				final Ids ids = new Ids();
				ids.pp = pp.getNumero();
				ids.mc = mc.getNumero();
				ids.dpi = dpi.getNumero();
				ids.pm = pm.getNumero();
				ids.ca = ca.getNumero();
				ids.ac = ac.getNumero();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		// get PP
		{
			final Party party = service.getParty(userLogin, (int) ids.pp, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());
			Assert.assertEquals(ids.pp, party.getNumber());

			final NaturalPerson pp = (NaturalPerson) party;
			Assert.assertEquals(dateNaissance, ch.vd.uniregctb.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
			Assert.assertEquals("Dufoin", pp.getOfficialName());
			Assert.assertEquals("Balthazar", pp.getFirstName());
			Assert.assertEquals(Sex.MALE, pp.getSex());

			final List<NaturalPersonCategory> categories = pp.getCategories();
			Assert.assertNotNull(categories);
			Assert.assertEquals(1, categories.size());

			final NaturalPersonCategory category = categories.get(0);
			Assert.assertNotNull(category);
			Assert.assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, category.getCategory());
			Assert.assertEquals(datePermisC, ch.vd.uniregctb.xml.DataHelper.xmlToCore(category.getDateFrom()));
			Assert.assertNull(category.getDateTo());
		}
		// get MC
		{
			final Party party = service.getParty(userLogin, (int) ids.mc, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(CommonHousehold.class, party.getClass());
			Assert.assertEquals(ids.mc, party.getNumber());
		}
		// get DPI
		{
			final Party party = service.getParty(userLogin, (int) ids.dpi, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(Debtor.class, party.getClass());
			Assert.assertEquals(ids.dpi, party.getNumber());

			final Debtor dpi = (Debtor) party;
			Assert.assertEquals("Balthazar Dufoin", dpi.getName());
			Assert.assertEquals("Débiteur IS", dpi.getComplementaryName());
			Assert.assertEquals(ModeCommunication.ELECTRONIQUE, ch.vd.uniregctb.xml.EnumHelper.xmlToCore(dpi.getCommunicationMode()));
		}
		// get PM
		{
			final Party party = service.getParty(userLogin, (int) ids.pm, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(Corporation.class, party.getClass());
			Assert.assertEquals(ids.pm, party.getNumber());

			final Corporation pm = (Corporation) party;
			Assert.assertEquals("Au petit coin", pm.getName1());
			Assert.assertNull(pm.getName2());
			Assert.assertNull(pm.getName3());
			Assert.assertEquals(pmActivityStartDate, ch.vd.uniregctb.xml.DataHelper.xmlToCore(pm.getActivityStartDate()));
		}
		// get CA
		{
			final Party party = service.getParty(userLogin, (int) ids.ca, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(AdministrativeAuthority.class, party.getClass());
			Assert.assertEquals(ids.ca, party.getNumber());

			final AdministrativeAuthority ca = (AdministrativeAuthority) party;
			Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNomCourt(), ca.getName());
			Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNoColAdm(), ca.getAdministrativeAuthorityId());
		}
		// get AC
		{
			final Party party = service.getParty(userLogin, (int) ids.ac, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(OtherCommunity.class, party.getClass());
			Assert.assertEquals(ids.ac, party.getNumber());

			final OtherCommunity ac = (OtherCommunity) party;
			Assert.assertEquals("Tata!!", ac.getName());
			Assert.assertEquals(LegalForm.ASSOCIATION, ac.getLegalForm());
		}
	}

	@Test
	public void testGetPartyWithAddresses() throws Exception {

		final long noIndividu = 32672456L;
		final RegDate dateArrivee = date(2000, 1, 23);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Delagrange", "Arthur", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, null);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getRepresentationAddresses());
		Assert.assertNotNull(partySans.getDebtProsecutionAddressesOfOtherParty());
		Assert.assertNotNull(partySans.getRepresentationAddresses());
		Assert.assertNotNull(partySans.getMailAddresses());
		Assert.assertNotNull(partySans.getResidenceAddresses());
		Assert.assertEquals(0, partySans.getRepresentationAddresses().size());
		Assert.assertEquals(0, partySans.getDebtProsecutionAddressesOfOtherParty().size());
		Assert.assertEquals(0, partySans.getRepresentationAddresses().size());
		Assert.assertEquals(0, partySans.getMailAddresses().size());
		Assert.assertEquals(0, partySans.getResidenceAddresses().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.ADDRESSES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getRepresentationAddresses());
		Assert.assertNotNull(partyAvec.getDebtProsecutionAddressesOfOtherParty());
		Assert.assertNotNull(partyAvec.getRepresentationAddresses());
		Assert.assertNotNull(partyAvec.getMailAddresses());
		Assert.assertNotNull(partyAvec.getResidenceAddresses());
		Assert.assertEquals(1, partyAvec.getRepresentationAddresses().size());
		Assert.assertEquals(0, partyAvec.getDebtProsecutionAddressesOfOtherParty().size());
		Assert.assertEquals(1, partyAvec.getRepresentationAddresses().size());
		Assert.assertEquals(1, partyAvec.getMailAddresses().size());
		Assert.assertEquals(1, partyAvec.getResidenceAddresses().size());

		{
			final Address address = partyAvec.getRepresentationAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.REPRESENTATION, address.getType());
			Assert.assertNull(address.getCouple());
			Assert.assertNull(address.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(address.isIncomplete());

			final AddressInformation info = address.getAddressInformation();
			Assert.assertNotNull(info);
			Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			Assert.assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			Assert.assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			Assert.assertNull(info.getAddressLine1());
			Assert.assertNull(info.getAddressLine2());
			Assert.assertNull(info.getCareOf());
			Assert.assertNull(info.getComplementaryInformation());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			Assert.assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbregeMinuscule(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = address.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = address.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.uniregctb.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getDebtProsecutionAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.DEBT_PROSECUTION, address.getType());
			Assert.assertNull(address.getCouple());
			Assert.assertNull(address.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(address.isIncomplete());

			final AddressInformation info = address.getAddressInformation();
			Assert.assertNotNull(info);
			Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			Assert.assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			Assert.assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			Assert.assertNull(info.getAddressLine1());
			Assert.assertNull(info.getAddressLine2());
			Assert.assertNull(info.getCareOf());
			Assert.assertNull(info.getComplementaryInformation());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			Assert.assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbregeMinuscule(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = address.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = address.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.uniregctb.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getMailAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.MAIL, address.getType());
			Assert.assertNull(address.getCouple());
			Assert.assertNull(address.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(address.isIncomplete());

			final AddressInformation info = address.getAddressInformation();
			Assert.assertNotNull(info);
			Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			Assert.assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			Assert.assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			Assert.assertNull(info.getAddressLine1());
			Assert.assertNull(info.getAddressLine2());
			Assert.assertNull(info.getCareOf());
			Assert.assertNull(info.getComplementaryInformation());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			Assert.assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbregeMinuscule(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = address.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = address.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.uniregctb.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getResidenceAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.RESIDENCE, address.getType());
			Assert.assertNull(address.getCouple());
			Assert.assertNull(address.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(address.isIncomplete());

			final AddressInformation info = address.getAddressInformation();
			Assert.assertNotNull(info);
			Assert.assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			Assert.assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			Assert.assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			Assert.assertNull(info.getAddressLine1());
			Assert.assertNull(info.getAddressLine2());
			Assert.assertNull(info.getCareOf());
			Assert.assertNull(info.getComplementaryInformation());
			Assert.assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			Assert.assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbregeMinuscule(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = address.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = address.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.uniregctb.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
	}

	@Test
	public void testGetPartyWithTaxResidences() throws Exception {

		final long noIndividu = 32672456L;
		final RegDate dateArrivee = date(2000, 1, 23);
		final RegDate dateAchat = dateArrivee.addYears(1);
		final RegDate dateVente = dateAchat.addYears(5).addMonths(-3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, null);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.TAX_RESIDENCES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getMainTaxResidences());
		Assert.assertNotNull(partyAvec.getOtherTaxResidences());
		Assert.assertNotNull(partyAvec.getManagingTaxResidences());
		Assert.assertEquals(1, partyAvec.getMainTaxResidences().size());
		Assert.assertEquals(1, partyAvec.getOtherTaxResidences().size());
		Assert.assertEquals(0, partyAvec.getManagingTaxResidences().size());

		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(0);
			Assert.assertNotNull(tr);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, tr.getStartReason());
			Assert.assertNull(tr.getDateTo());
			Assert.assertNull(tr.getEndReason());
			Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			Assert.assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			Assert.assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			Assert.assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			Assert.assertFalse(tr.isVirtual());
		}
		{
			final TaxResidence tr = partyAvec.getOtherTaxResidences().get(0);
			Assert.assertNotNull(tr);
			Assert.assertEquals(dateAchat, ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, tr.getStartReason());
			Assert.assertEquals(dateVente, ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateTo()));
			Assert.assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, tr.getEndReason());
			Assert.assertEquals(MockCommune.Echallens.getNoOFS(), tr.getTaxationAuthorityFSOId());
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			Assert.assertNull(tr.getTaxationMethod());
			Assert.assertEquals(TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY, tr.getTaxLiabilityReason());
			Assert.assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			Assert.assertFalse(tr.isVirtual());
		}
	}

	@Test
	public void testGetPartyWithVirtualTaxResidences() throws Exception {

		final long noIndividu = 32672456L;
		final RegDate dateArrivee = date(2000, 1, 23);
		final RegDate dateAchat = dateArrivee.addYears(1);
		final RegDate dateVente = dateAchat.addYears(5).addMonths(-3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, null);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee.addYears(-1), MotifFor.DEBUT_EXPLOITATION, dateArrivee.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				                MockPays.Allemagne);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateArrivee, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(mc, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.VIRTUAL_TAX_RESIDENCES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getMainTaxResidences());
		Assert.assertNotNull(partyAvec.getOtherTaxResidences());
		Assert.assertNotNull(partyAvec.getManagingTaxResidences());
		Assert.assertEquals(2, partyAvec.getMainTaxResidences().size());
		Assert.assertEquals(0, partyAvec.getOtherTaxResidences().size());
		Assert.assertEquals(0, partyAvec.getManagingTaxResidences().size());

		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(0);
			Assert.assertNotNull(tr);
			Assert.assertEquals(dateArrivee.addYears(-1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, tr.getStartReason());
			Assert.assertEquals(dateArrivee.getOneDayBefore(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateTo()));
			Assert.assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, tr.getEndReason());
			Assert.assertEquals(MockPays.Allemagne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			Assert.assertEquals(TaxationAuthorityType.FOREIGN_COUNTRY, tr.getTaxationAuthorityType());
			Assert.assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			Assert.assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			Assert.assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			Assert.assertFalse(tr.isVirtual());
		}
		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(1);
			Assert.assertNotNull(tr);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, tr.getStartReason());
			Assert.assertNull(tr.getDateTo());
			Assert.assertNull(tr.getEndReason());
			Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			Assert.assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			Assert.assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			Assert.assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			Assert.assertTrue(tr.isVirtual());
		}
	}

	@Test
	public void testGetPartyWithManagingTaxResidences() throws Exception {

		final long noIndividu = 32672456L;
		final RegDate dateArrivee = date(2000, 1, 23);
		final RegDate dateAchat = dateArrivee.addYears(1);
		final RegDate dateVente = dateAchat.addYears(5).addMonths(-3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, null);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
				addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);
				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.MANAGING_TAX_RESIDENCES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getMainTaxResidences());
		Assert.assertNotNull(partyAvec.getOtherTaxResidences());
		Assert.assertNotNull(partyAvec.getManagingTaxResidences());
		Assert.assertEquals(0, partyAvec.getMainTaxResidences().size());
		Assert.assertEquals(0, partyAvec.getOtherTaxResidences().size());
		Assert.assertEquals(1, partyAvec.getManagingTaxResidences().size());

		{
			final ManagingTaxResidence mtr = partyAvec.getManagingTaxResidences().get(0);
			Assert.assertNotNull(mtr);
			Assert.assertEquals(dateArrivee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(mtr.getDateFrom()));
			Assert.assertNull(mtr.getDateTo());
			Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), mtr.getMunicipalityFSOId());
		}
	}

	@Test
	public void testGetPartyWithHouseholdMembers() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateMariage = date(1999, 8, 3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final CommonHousehold mcSans = (CommonHousehold) partySans;
		Assert.assertNull(mcSans.getMainTaxpayer());
		Assert.assertNull(mcSans.getSecondaryTaxpayer());

		final Party partyAvec = service.getParty(userLogin, ids.mc, EnumSet.of(PartyPart.HOUSEHOLD_MEMBERS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final CommonHousehold mcAvec = (CommonHousehold) partyAvec;
		{
			final NaturalPerson pp = mcAvec.getMainTaxpayer();
			Assert.assertNotNull(pp);
			Assert.assertEquals(Sex.MALE, pp.getSex());
			Assert.assertEquals("Delagrange", pp.getOfficialName());
			Assert.assertEquals("Marcel", pp.getFirstName());
			Assert.assertEquals(dateNaissance, ch.vd.uniregctb.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
			Assert.assertEquals(ids.lui, pp.getNumber());
		}
		{
			final NaturalPerson pp = mcAvec.getSecondaryTaxpayer();
			Assert.assertNotNull(pp);
			Assert.assertEquals(Sex.FEMALE, pp.getSex());
			Assert.assertEquals("Delagrange", pp.getOfficialName());
			Assert.assertEquals("Marceline", pp.getFirstName());
			Assert.assertNull(pp.getDateOfBirth());
			Assert.assertEquals(ids.elle, pp.getNumber());
		}
	}

	@Test
	public void testGetPartyWithTaxLiabilities() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateMariage = date(1999, 8, 3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getTaxLiabilities());
		Assert.assertNotNull(tpSans.getSimplifiedTaxLiabilityCH());
		Assert.assertNotNull(tpSans.getSimplifiedTaxLiabilityVD());
		Assert.assertEquals(0, tpSans.getTaxLiabilities().size());
		Assert.assertEquals(0, tpSans.getSimplifiedTaxLiabilityCH().size());
		Assert.assertEquals(0, tpSans.getSimplifiedTaxLiabilityVD().size());

		final Party partyAvec = service.getParty(userLogin, ids.mc, EnumSet.of(PartyPart.TAX_LIABILITIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getTaxLiabilities());
		Assert.assertNotNull(tpAvec.getSimplifiedTaxLiabilityCH());
		Assert.assertNotNull(tpAvec.getSimplifiedTaxLiabilityVD());
		Assert.assertEquals(1, tpAvec.getTaxLiabilities().size());
		Assert.assertEquals(0, tpAvec.getSimplifiedTaxLiabilityCH().size());
		Assert.assertEquals(0, tpAvec.getSimplifiedTaxLiabilityVD().size());

		final TaxLiability tl = tpAvec.getTaxLiabilities().get(0);
		Assert.assertNotNull(tl);
		Assert.assertEquals(date(dateMariage.year(), 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tl.getDateFrom()));
		Assert.assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, tl.getStartReason());
		Assert.assertNull(tl.getDateTo());
		Assert.assertNull(tl.getEndReason());
		Assert.assertEquals(OrdinaryResident.class, tl.getClass());
	}

	@Test
	public void testGetPartyWithSimplifiedTaxLiabilities() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateMariage = date(1999, 8, 3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getTaxLiabilities());
		Assert.assertNotNull(tpSans.getSimplifiedTaxLiabilityCH());
		Assert.assertNotNull(tpSans.getSimplifiedTaxLiabilityVD());
		Assert.assertEquals(0, tpSans.getTaxLiabilities().size());
		Assert.assertEquals(0, tpSans.getSimplifiedTaxLiabilityCH().size());
		Assert.assertEquals(0, tpSans.getSimplifiedTaxLiabilityVD().size());

		final Party partyAvec = service.getParty(userLogin, ids.mc, EnumSet.of(PartyPart.SIMPLIFIED_TAX_LIABILITIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getTaxLiabilities());
		Assert.assertNotNull(tpAvec.getSimplifiedTaxLiabilityCH());
		Assert.assertNotNull(tpAvec.getSimplifiedTaxLiabilityVD());
		Assert.assertEquals(0, tpAvec.getTaxLiabilities().size());
		Assert.assertEquals(1, tpAvec.getSimplifiedTaxLiabilityCH().size());
		Assert.assertEquals(1, tpAvec.getSimplifiedTaxLiabilityVD().size());

		{
			final SimplifiedTaxLiability tl = tpAvec.getSimplifiedTaxLiabilityCH().get(0);
			Assert.assertNotNull(tl);
			Assert.assertEquals(date(dateMariage.year(), 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tl.getDateFrom()));
			Assert.assertNull(tl.getDateTo());
			Assert.assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, tl.getType());
		}
		{
			final SimplifiedTaxLiability tl = tpAvec.getSimplifiedTaxLiabilityVD().get(0);
			Assert.assertNotNull(tl);
			Assert.assertEquals(date(dateMariage.year(), 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tl.getDateFrom()));
			Assert.assertNull(tl.getDateTo());
			Assert.assertEquals(SimplifiedTaxLiabilityType.UNLIMITED, tl.getType());
		}
	}

	@Test
	public void testGetPartyWithTaxationPeriods() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1965, 3, 12);
		final RegDate dateMariage = date(2005, 8, 3);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getTaxationPeriods());
		Assert.assertEquals(0, tpSans.getTaxationPeriods().size());

		final Party partyAvec = service.getParty(userLogin, ids.mc, EnumSet.of(PartyPart.TAXATION_PERIODS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getTaxationPeriods());
		Assert.assertEquals(RegDate.get().year() - dateMariage.year() + 1, tpAvec.getTaxationPeriods().size());

		for (int year = dateMariage.year() ; year <= RegDate.get().year() ; ++ year) {
			final TaxationPeriod tp = tpAvec.getTaxationPeriods().get(year - dateMariage.year());
			Assert.assertNotNull(tp);
			Assert.assertEquals(date(year, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tp.getDateFrom()));
			if (year == RegDate.get().year()) {
				Assert.assertNull(tp.getDateTo());
			}
			else {
				Assert.assertEquals(date(year, 12, 31), ch.vd.uniregctb.xml.DataHelper.xmlToCore(tp.getDateTo()));
			}
			Assert.assertNull(tp.getTaxDeclarationId());
		}
	}

	@Test
	public void testGetPartyWithWithholdingTaxationPeriods() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1990, 5, 12);
		final RegDate dateMariage = date(2010, 8, 3);
		final RegDate dateDeces = date(2013, 9, 4);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
				lui.setDateDeces(dateDeces);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.lui, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());

		final NaturalPerson tpSans = (NaturalPerson) partySans;
		Assert.assertNotNull(tpSans.getWithholdingTaxationPeriods());
		Assert.assertEquals(0, tpSans.getWithholdingTaxationPeriods().size());

		final Party partyAvec = service.getParty(userLogin, ids.lui, EnumSet.of(PartyPart.WITHHOLDING_TAXATION_PERIODS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

		final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
		Assert.assertNotNull(tpAvec.getWithholdingTaxationPeriods());
		Assert.assertEquals(7, tpAvec.getWithholdingTaxationPeriods().size());

		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(0);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2008, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(dateNaissance.addYears(18).getOneDayBefore(),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertNull(wtp.getTaxationAuthority());
			Assert.assertNull(wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(1);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(dateNaissance.addYears(18), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2008, 12, 31),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(2);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2009, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2009, 12, 31),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(3);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2010, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2010, 12, 31),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(4);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2011, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2011, 12, 31),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(5);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2012, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2012, 12, 31),  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(6);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(dateDeces,  ch.vd.uniregctb.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
	}

	@Test
	public void testGetPartyWithRelationsBetweenParties() throws Exception {

		final long noIndividuLui = 32672456L;
		final long noIndividuElle = 46245L;
		final RegDate dateNaissance = date(1990, 5, 12);
		final RegDate dateMariage = date(2010, 8, 3);
		final RegDate dateDebutRT = date(2011, 8, 1);
		final RegDate dateDeces = date(2013, 9, 4);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
				lui.setDateDeces(dateDeces);

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Delagrange", "Marceline", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
			}
		});

		final class Ids {
			int lui;
			int elle;
			int mc;
			int dpi;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);

				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);

				final DebiteurPrestationImposable dpi = addDebiteur("Débiteur lié", mc, dateMariage);
				addRapportPrestationImposable(dpi, lui, dateDebutRT, dateDeces, true);

				final Ids ids = new Ids();
				ids.lui = lui.getNumero().intValue();
				ids.elle = elle.getNumero().intValue();
				ids.mc = mc.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.lui, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getRelationsBetweenParties());
		Assert.assertEquals(0, partySans.getRelationsBetweenParties().size());

		final Party partyAvec = service.getParty(userLogin, ids.lui, EnumSet.of(PartyPart.RELATIONS_BETWEEN_PARTIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
		Assert.assertEquals(2, partyAvec.getRelationsBetweenParties().size());

		final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
		Collections.sort(sortedRelations, new Comparator<RelationBetweenParties>() {
			@Override
			public int compare(RelationBetweenParties o1, RelationBetweenParties o2) {
				final DateRange r1 = new DateRangeHelper.Range(ch.vd.uniregctb.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.uniregctb.xml.DataHelper.xmlToCore(o1.getDateTo()));
				final DateRange r2 = new DateRangeHelper.Range(ch.vd.uniregctb.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.uniregctb.xml.DataHelper.xmlToCore(o2.getDateTo()));
				return DateRangeComparator.compareRanges(r1, r2);
			}
		});

		{
			final RelationBetweenParties rel = sortedRelations.get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateMariage, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertNull(rel.getDateTo());
			Assert.assertEquals(RelationBetweenPartiesType.HOUSEHOLD_MEMBER, rel.getType());
			Assert.assertEquals(ids.mc, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getEndDateOfLastTaxableItem());
			Assert.assertNull(rel.getCancellationDate());
			Assert.assertNull(rel.isExtensionToForcedExecution());
		}
		{
			final RelationBetweenParties rel = sortedRelations.get(1);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateDebutRT, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertEquals(dateDeces, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateTo()));
			Assert.assertEquals(RelationBetweenPartiesType.TAXABLE_REVENUE, rel.getType());
			Assert.assertEquals(ids.dpi, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getEndDateOfLastTaxableItem());
			Assert.assertNotNull(rel.getCancellationDate());
			Assert.assertNull(rel.isExtensionToForcedExecution());
		}
	}

	@Test
	public void testGetPartyWithFamilyStatuses() throws Exception {

		final long noIndividuLui = 32672456L;
		final RegDate dateNaissance = date(1990, 5, 12);
		final RegDate dateMariage = date(2010, 8, 3);
		final RegDate dateSeparation = date(2012, 9, 3);
		final RegDate dateDivorce = date(2013, 4, 5);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateNaissance, null);
				marieIndividu(lui, dateMariage);
				separeIndividu(lui, dateSeparation);
				divorceIndividu(lui, dateDivorce);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				return lui.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getFamilyStatuses());
		Assert.assertEquals(0, tpSans.getFamilyStatuses().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.FAMILY_STATUSES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getFamilyStatuses());
		Assert.assertEquals(4, tpAvec.getFamilyStatuses().size());

		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(0);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateNaissance, ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateMariage.getOneDayBefore(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.SINGLE, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(1);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateMariage, ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateSeparation.getOneDayBefore(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.MARRIED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(2);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateSeparation, ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateDivorce.getOneDayBefore(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.SEPARATED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(3);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateDivorce, ch.vd.uniregctb.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertNull(fs.getDateTo());
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.DIVORCED, fs.getMaritalStatus());
		}
	}

	@Test
	public void testGetPartyWithTaxDeclarations() throws Exception {

		final long noIndividu = 32672456L;
		final RegDate dateArrivee = date(2000, 1, 23);
		final RegDate dateEmissionDi = RegDate.get();
		final RegDate dateDelaiDi = dateEmissionDi.addDays(30);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Delagrange", "Marcel", Sexe.MASCULIN);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateArrivee, null);
			}
		});

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

				final CollectiviteAdministrative cedi = addCedi();
				final PeriodeFiscale pf = addPeriodeFiscale(2013);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(pf.getAnnee(), 1, 1), date(pf.getAnnee(), 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, dateEmissionDi);
				addDelaiDeclaration(di, dateEmissionDi, dateDelaiDi);

				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getTaxDeclarations());
		Assert.assertEquals(0, partySans.getTaxDeclarations().size());

		{
			final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(0, di.getDeadlines().size());
			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			Assert.assertNotNull(period);
			Assert.assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS_DEADLINES));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(1, di.getDeadlines().size());

			final TaxDeclarationDeadline delai = di.getDeadlines().get(0);
			Assert.assertNotNull(delai);
			Assert.assertEquals(dateDelaiDi, ch.vd.uniregctb.xml.DataHelper.xmlToCore(delai.getDeadline()));
			Assert.assertNull(delai.getCancellationDate());
			Assert.assertEquals(dateEmissionDi, ch.vd.uniregctb.xml.DataHelper.xmlToCore(delai.getApplicationDate()));
			Assert.assertEquals(dateEmissionDi, ch.vd.uniregctb.xml.DataHelper.xmlToCore(delai.getProcessingDate()));

			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			Assert.assertNotNull(period);
			Assert.assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS_STATUSES));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.uniregctb.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(0, di.getDeadlines().size());
			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(1, di.getStatuses().size());

			final TaxDeclarationStatus status = di.getStatuses().get(0);
			Assert.assertNotNull(status);
			Assert.assertEquals(dateEmissionDi, ch.vd.uniregctb.xml.DataHelper.xmlToCore(status.getDateFrom()));
			Assert.assertNull(status.getCancellationDate());
			Assert.assertEquals(TaxDeclarationStatusType.SENT, status.getType());

			final TaxPeriod period = di.getTaxPeriod();
			Assert.assertNotNull(period);
			Assert.assertEquals(2013, period.getYear());
		}
	}

	@Test
	public void testGetPartyWithDebtorPeriodicities() throws Exception {

		final RegDate dateDebutPeriodiciteInitiale = date(2010, 1, 1);
		final RegDate dateDebutPeriodiciteModifiee = date(2013, 7, 1);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// .. personne ..
			}
		});

		final int dpiId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebutPeriodiciteInitiale);
				final Periodicite periodiciteInitiale = dpi.getPeriodicites().iterator().next();
				periodiciteInitiale.setDateFin(dateDebutPeriodiciteModifiee.getOneDayBefore());
				dpi.getPeriodicites().add(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, dateDebutPeriodiciteModifiee, null));
				return dpi.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, dpiId, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(Debtor.class, partySans.getClass());

		final Debtor dpiSans = (Debtor) partySans;
		Assert.assertNotNull(dpiSans.getPeriodicities());
		Assert.assertEquals(0, dpiSans.getPeriodicities().size());

		final Party partyAvec = service.getParty(userLogin, dpiId, EnumSet.of(PartyPart.DEBTOR_PERIODICITIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(Debtor.class, partyAvec.getClass());

		final Debtor dpiAvec = (Debtor) partyAvec;
		Assert.assertNotNull(dpiAvec.getPeriodicities());
		Assert.assertEquals(2, dpiAvec.getPeriodicities().size());

		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(0);
			Assert.assertNotNull(dp);
			Assert.assertNull(dp.getCancellationDate());
			Assert.assertEquals(dateDebutPeriodiciteInitiale, ch.vd.uniregctb.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			Assert.assertEquals(dateDebutPeriodiciteModifiee.getOneDayBefore(), ch.vd.uniregctb.xml.DataHelper.xmlToCore(dp.getDateTo()));
			Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, dp.getPeriodicity());
			Assert.assertNull(dp.getSpecificPeriod());
		}
		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(1);
			Assert.assertNotNull(dp);
			Assert.assertNull(dp.getCancellationDate());
			Assert.assertEquals(dateDebutPeriodiciteModifiee, ch.vd.uniregctb.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			Assert.assertNull(dp.getDateTo());
			Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, dp.getPeriodicity());
			Assert.assertNull(dp.getSpecificPeriod());
		}
	}

	@Test
	public void testGetPartyWithImmovableProperties() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// .. personne ..
			}
		});

		final String numeroImmeuble = "3424-13234";
		final RegDate dateAchat = date(2011, 5, 1);
		final RegDate dateVente = date(2012, 8, 31);
		final MockCommune commune = MockCommune.Aubonne;
		final String natureImmeuble = "villa individuelle";
		final TypeImmeuble typeImmeuble = TypeImmeuble.BIEN_FOND;
		final GenrePropriete genrePropriete = GenrePropriete.INDIVIDUELLE;
		final int estimationFiscale = 740000;
		final String refEstimationFiscale = "mon estimation";
		final String partPropriete = "5/12";
		final RegDate dateDerniereMutation = date(2012, 3, 1);
		final TypeMutation derniereMutation = TypeMutation.AUGMENTATION;

		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Arthur", "de Saint André", null, Sexe.MASCULIN);
				addImmeuble(pp, numeroImmeuble, dateAchat, dateVente, commune.getNomCourt(), natureImmeuble, typeImmeuble, genrePropriete, estimationFiscale, refEstimationFiscale,
				            partPropriete, dateDerniereMutation, derniereMutation);
				return pp.getNumero().intValue();
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getImmovableProperties());
		Assert.assertEquals(0, tpSans.getImmovableProperties().size());

		final Party partyAvec = service.getParty(userLogin, ppId, EnumSet.of(PartyPart.IMMOVABLE_PROPERTIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getImmovableProperties());
		Assert.assertEquals(1, tpAvec.getImmovableProperties().size());

		final ImmovableProperty ip = tpAvec.getImmovableProperties().get(0);
		Assert.assertNotNull(ip);
		Assert.assertEquals(numeroImmeuble, ip.getNumber());
		Assert.assertEquals(dateAchat, ch.vd.uniregctb.xml.DataHelper.xmlToCore(ip.getDateFrom()));
		Assert.assertEquals(dateVente, ch.vd.uniregctb.xml.DataHelper.xmlToCore(ip.getDateTo()));
		Assert.assertEquals(commune.getNomCourt(), ip.getMunicipalityName());
		Assert.assertEquals(natureImmeuble, ip.getNature());
		Assert.assertEquals(genrePropriete, ch.vd.uniregctb.xml.EnumHelper.xmlToCore(ip.getOwnershipType()));
		Assert.assertEquals((Integer) estimationFiscale, ip.getEstimatedTaxValue());
		Assert.assertEquals(refEstimationFiscale, ip.getEstimatedTaxValueReference());
		Assert.assertNotNull(ip.getShare());
		Assert.assertEquals(5, ip.getShare().getNumerator());
		Assert.assertEquals(12, ip.getShare().getDenominator());
		Assert.assertEquals(dateDerniereMutation, ch.vd.uniregctb.xml.DataHelper.xmlToCore(ip.getLastMutationDate()));
		Assert.assertEquals(derniereMutation, ch.vd.uniregctb.xml.EnumHelper.xmlToCore(ip.getLastMutationType()));
	}

	@Test
	public void testGetPartyWithChildrenParents() throws Exception {

		final long noIndividuPapa = 423564L;
		final long noIndividu = 3232141L;
		final long noIndividuFiston = 32141413L;
		final RegDate dateNaissance = date(1967, 6, 14);
		final RegDate dateNaissanceFiston = date(2002, 8, 5);
		final RegDate dateDecesPapa = date(1999, 12, 27);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndividuPapa, null, "Dupondt", "Alexandre Senior", Sexe.MASCULIN);
				papa.setDateDeces(dateDecesPapa);
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Dupondt", "Alexandre", Sexe.MASCULIN);
				final MockIndividu fiston = addIndividu(noIndividuFiston, dateNaissanceFiston, "Dupondt", "Alexandre Junior", Sexe.MASCULIN);
				addLiensFiliation(ind, papa, null, dateNaissance, dateDecesPapa);
				addLiensFiliation(fiston, ind, null, dateNaissanceFiston, null);
			}
		});

		final class Ids {
			int papa;
			int moi;
			int fiston;
		}

		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique papa = addHabitant(noIndividuPapa);
				final PersonnePhysique moi = addHabitant(noIndividu);
				final PersonnePhysique fiston = addHabitant(noIndividuFiston);
				final Ids ids = new Ids();
				ids.papa = papa.getNumero().intValue();
				ids.moi = moi.getNumero().intValue();
				ids.fiston = fiston.getNumero().intValue();
				return ids;
			}
		});

		final UserLogin userLogin = new UserLogin(getDefaultOperateurName(), 22);

		final Party partySans = service.getParty(userLogin, ids.moi, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());
		Assert.assertNotNull(partySans.getRelationsBetweenParties());
		Assert.assertEquals(0, partySans.getRelationsBetweenParties().size());

		{
			final Party partyAvec = service.getParty(userLogin, ids.moi, EnumSet.of(PartyPart.PARENTS));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

			final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
			Assert.assertNotNull(tpAvec.getRelationsBetweenParties());
			Assert.assertEquals(1, tpAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = tpAvec.getRelationsBetweenParties().get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateNaissance, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertEquals(dateDecesPapa, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateTo()));
			Assert.assertEquals(RelationBetweenPartiesType.PARENT, rel.getType());
			Assert.assertEquals(ids.papa, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getEndDateOfLastTaxableItem());
			Assert.assertNull(rel.getCancellationDate());
			Assert.assertNull(rel.isExtensionToForcedExecution());
		}
		{
			final Party partyAvec = service.getParty(userLogin, ids.moi, EnumSet.of(PartyPart.CHILDREN));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());
			Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
			Assert.assertEquals(1, partyAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = partyAvec.getRelationsBetweenParties().get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateNaissanceFiston, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertNull(rel.getDateTo());
			Assert.assertEquals(RelationBetweenPartiesType.CHILD, rel.getType());
			Assert.assertEquals(ids.fiston, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getEndDateOfLastTaxableItem());
			Assert.assertNull(rel.getCancellationDate());
			Assert.assertNull(rel.isExtensionToForcedExecution());
		}
		{
			final Party partyAvec = service.getParty(userLogin, ids.moi, EnumSet.of(PartyPart.CHILDREN, PartyPart.PARENTS));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());
			Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
			Assert.assertEquals(2, partyAvec.getRelationsBetweenParties().size());

			final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
			Collections.sort(sortedRelations, new Comparator<RelationBetweenParties>() {
				@Override
				public int compare(RelationBetweenParties o1, RelationBetweenParties o2) {
					final DateRange r1 = new DateRangeHelper.Range(ch.vd.uniregctb.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.uniregctb.xml.DataHelper.xmlToCore(o1.getDateTo()));
					final DateRange r2 = new DateRangeHelper.Range(ch.vd.uniregctb.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.uniregctb.xml.DataHelper.xmlToCore(o2.getDateTo()));
					return DateRangeComparator.compareRanges(r1, r2);
				}
			});

			{
				final RelationBetweenParties rel = sortedRelations.get(0);
				Assert.assertNotNull(rel);
				Assert.assertEquals(dateNaissance, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateDecesPapa, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertEquals(RelationBetweenPartiesType.PARENT, rel.getType());
				Assert.assertEquals(ids.papa, rel.getOtherPartyNumber());
				Assert.assertNull(rel.getEndDateOfLastTaxableItem());
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertNull(rel.isExtensionToForcedExecution());
			}
			{
				final RelationBetweenParties rel = sortedRelations.get(1);
				Assert.assertNotNull(rel);
				Assert.assertEquals(dateNaissanceFiston, ch.vd.uniregctb.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertNull(rel.getDateTo());
				Assert.assertEquals(RelationBetweenPartiesType.CHILD, rel.getType());
				Assert.assertEquals(ids.fiston, rel.getOtherPartyNumber());
				Assert.assertNull(rel.getEndDateOfLastTaxableItem());
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertNull(rel.isExtensionToForcedExecution());
			}
		}
	}
}
