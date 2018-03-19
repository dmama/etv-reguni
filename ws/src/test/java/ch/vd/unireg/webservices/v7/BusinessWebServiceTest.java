package ch.vd.unireg.webservices.v7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
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
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.common.ObjectNotFoundException;
import ch.vd.unireg.common.WebserviceTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.declaration.Declaration;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.efacture.EFactureServiceProxy;
import ch.vd.unireg.efacture.MockEFactureService;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteService;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.ApplicationFiscale;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCollectiviteAdministrative;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.MockOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockOrganisationFactory;
import ch.vd.unireg.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.unireg.registrefoncier.BatimentRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.security.Role;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.DroitAcces;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.IdentificationEntreprise;
import ch.vd.unireg.tiers.IndividuNotFoundException;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.MontantMonetaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.CategorieImpotSource;
import ch.vd.unireg.type.DayMonth;
import ch.vd.unireg.type.EtatCivil;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridique;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Niveau;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.type.TypeTiersEtiquette;
import ch.vd.unireg.ws.ack.v7.AckStatus;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineStatus;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvent;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.landregistry.v7.BuildingEntry;
import ch.vd.unireg.ws.landregistry.v7.BuildingList;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersEntry;
import ch.vd.unireg.ws.landregistry.v7.CommunityOfOwnersList;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyEntry;
import ch.vd.unireg.ws.landregistry.v7.ImmovablePropertyList;
import ch.vd.unireg.ws.modifiedtaxpayers.v7.PartyNumberList;
import ch.vd.unireg.ws.parties.v7.Entry;
import ch.vd.unireg.ws.parties.v7.Parties;
import ch.vd.unireg.ws.security.v7.AllowedAccess;
import ch.vd.unireg.ws.security.v7.PartyAccess;
import ch.vd.unireg.ws.security.v7.SecurityListResponse;
import ch.vd.unireg.ws.security.v7.SecurityResponse;
import ch.vd.unireg.xml.error.v1.Error;
import ch.vd.unireg.xml.error.v1.ErrorType;
import ch.vd.unireg.xml.event.fiscal.v3.CategorieTiers;
import ch.vd.unireg.xml.event.fiscal.v3.EvenementFiscal;
import ch.vd.unireg.xml.event.fiscal.v3.OuvertureFor;
import ch.vd.unireg.xml.infra.taxoffices.v1.TaxOffices;
import ch.vd.unireg.xml.party.address.v3.Address;
import ch.vd.unireg.xml.party.address.v3.AddressInformation;
import ch.vd.unireg.xml.party.address.v3.AddressType;
import ch.vd.unireg.xml.party.address.v3.FormattedAddress;
import ch.vd.unireg.xml.party.address.v3.PersonMailAddressInfo;
import ch.vd.unireg.xml.party.address.v3.PostAddress;
import ch.vd.unireg.xml.party.address.v3.Recipient;
import ch.vd.unireg.xml.party.address.v3.TariffZone;
import ch.vd.unireg.xml.party.adminauth.v5.AdministrativeAuthority;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirLeader;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirMember;
import ch.vd.unireg.xml.party.communityofheirs.v1.CommunityOfHeirs;
import ch.vd.unireg.xml.party.corporation.v5.BusinessYear;
import ch.vd.unireg.xml.party.corporation.v5.Capital;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlag;
import ch.vd.unireg.xml.party.corporation.v5.CorporationFlagType;
import ch.vd.unireg.xml.party.debtor.v5.Debtor;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatus;
import ch.vd.unireg.xml.party.ebilling.v1.EbillingStatusType;
import ch.vd.unireg.xml.party.landregistry.v1.Building;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingDescription;
import ch.vd.unireg.xml.party.landregistry.v1.BuildingSetting;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwners;
import ch.vd.unireg.xml.party.landregistry.v1.CommunityOfOwnersType;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.Location;
import ch.vd.unireg.xml.party.landregistry.v1.NaturalPersonIdentity;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.RealEstate;
import ch.vd.unireg.xml.party.landregistry.v1.RightHolder;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.othercomm.v3.OtherCommunity;
import ch.vd.unireg.xml.party.person.v5.CommonHousehold;
import ch.vd.unireg.xml.party.person.v5.Nationality;
import ch.vd.unireg.xml.party.person.v5.NaturalPerson;
import ch.vd.unireg.xml.party.person.v5.NaturalPersonCategory;
import ch.vd.unireg.xml.party.person.v5.NaturalPersonCategoryType;
import ch.vd.unireg.xml.party.person.v5.Origin;
import ch.vd.unireg.xml.party.person.v5.ResidencyPeriod;
import ch.vd.unireg.xml.party.person.v5.Sex;
import ch.vd.unireg.xml.party.relation.v4.Child;
import ch.vd.unireg.xml.party.relation.v4.HouseholdMember;
import ch.vd.unireg.xml.party.relation.v4.Parent;
import ch.vd.unireg.xml.party.relation.v4.RelationBetweenParties;
import ch.vd.unireg.xml.party.relation.v4.Representative;
import ch.vd.unireg.xml.party.relation.v4.TaxableRevenue;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclaration;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationDeadline;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationKey;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatus;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxDeclarationStatusType;
import ch.vd.unireg.xml.party.taxdeclaration.v5.TaxPeriod;
import ch.vd.unireg.xml.party.taxpayer.v5.FamilyStatus;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalForm;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory;
import ch.vd.unireg.xml.party.taxpayer.v5.MaritalStatus;
import ch.vd.unireg.xml.party.taxpayer.v5.Taxpayer;
import ch.vd.unireg.xml.party.taxresidence.v4.CorporationTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v4.IndividualTaxLiabilityType;
import ch.vd.unireg.xml.party.taxresidence.v4.LiabilityChangeReason;
import ch.vd.unireg.xml.party.taxresidence.v4.ManagingTaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v4.OrdinaryResident;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiability;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxLiabilityReason;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxResidence;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxType;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationAuthorityType;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationMethod;
import ch.vd.unireg.xml.party.taxresidence.v4.TaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriod;
import ch.vd.unireg.xml.party.taxresidence.v4.WithholdingTaxationPeriodType;
import ch.vd.unireg.xml.party.v5.AdministrativeAuthorityLink;
import ch.vd.unireg.xml.party.v5.NaturalPersonSubtype;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyLabel;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.unireg.xml.party.v5.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;

import static ch.vd.unireg.xml.party.v5.strategy.NaturalPersonStrategyTest.assertInheritanceTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("Duplicates")
public class BusinessWebServiceTest extends WebserviceTest {

	private BusinessWebService service;
	private EFactureServiceProxy efactureService;
	private EtiquetteService etiquetteService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(BusinessWebService.class, "wsv7Business");
		efactureService = getBean(EFactureServiceProxy.class, "efactureService");
		etiquetteService = getBean(EtiquetteService.class, "etiquetteService");

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService() {
			@Override
			public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
				Assert.assertNull(parametres);
				return "https://secure.vd.ch/territoire/intercapi/faces?bfs={noCommune}&kr=0&n1={noParcelle}&n2={index1}&n3={index2}&n4={index3}&type=grundstueck_grundbuch_auszug";
			}
		});
	}

	private static void assertValidInteger(long value) {
		Assert.assertTrue(Long.toString(value), value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE);
	}

	@Test
	public void testBlocageRemboursementAuto() throws Exception {

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

		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));

		// appel du WS (= sans changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, false);

		// vérification
		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, true);

		// vérification
		Assert.assertTrue(service.getAutomaticRepaymentBlockingFlag((int) ppId));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);
				Assert.assertTrue(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, false);

		// vérification
		Assert.assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));
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
				addOperateur(visaOmnipotent, noIndividuOmnipotent, Role.VISU_ALL, Role.ECRITURE_DOSSIER_PROTEGE);
				addOperateur(visaActeur, noIndividuActeur, Role.VISU_ALL);
				addOperateur(visaVoyeur, noIndividuVoyeur, Role.VISU_ALL);
				addOperateur(visaGrouillot, noIndividuGrouillot, Role.VISU_ALL);
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
			int idProtege;
			int idConjointDeProtege;
			int idCoupleProtege;
			int idNormal;
			int idCoupleNormal;
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
				ids.idProtege = protege.getNumero().intValue();
				ids.idConjointDeProtege = conjointDeProtege.getNumero().intValue();
				ids.idCoupleProtege = coupleProtege.getMenage().getNumero().intValue();
				ids.idNormal = normal.getNumero().intValue();
				ids.idCoupleNormal = coupleNormal.getMenage().getNumero().intValue();
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
			// accès un-à-un
			assertAllowedAccess(visaOmnipotent, ids.idProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, ids.idCoupleProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaOmnipotent, ids.idCoupleNormal, AllowedAccess.READ_WRITE);

			// accès batch
			final SecurityListResponse response = service.getSecurityOnParties(visaOmnipotent, Arrays.asList(ids.idProtege, ids.idConjointDeProtege, ids.idCoupleProtege, ids.idNormal, ids.idCoupleNormal));
			Assert.assertEquals(visaOmnipotent, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
		}

		// l'acteur peut tout faire également
		{
			// accès un-à-un
			assertAllowedAccess(visaActeur, ids.idProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, ids.idCoupleProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaActeur, ids.idCoupleNormal, AllowedAccess.READ_WRITE);

			// accès batch
			final SecurityListResponse response = service.getSecurityOnParties(visaActeur, Arrays.asList(ids.idProtege, ids.idConjointDeProtege, ids.idCoupleProtege, ids.idNormal, ids.idCoupleNormal));
			Assert.assertEquals(visaActeur, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
		}

		// le voyeur, lui, ne peut pas modifier ce qui est protégé, mais peut le voir
		{
			// accès un-à-un
			assertAllowedAccess(visaVoyeur, ids.idProtege, AllowedAccess.READ_ONLY);
			assertAllowedAccess(visaVoyeur, ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaVoyeur, ids.idCoupleProtege, AllowedAccess.READ_ONLY);
			assertAllowedAccess(visaVoyeur, ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaVoyeur, ids.idCoupleNormal, AllowedAccess.READ_WRITE);

			// accès batch
			final SecurityListResponse response = service.getSecurityOnParties(visaVoyeur, Arrays.asList(ids.idProtege, ids.idConjointDeProtege, ids.idCoupleProtege, ids.idNormal, ids.idCoupleNormal));
			Assert.assertEquals(visaVoyeur, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			Assert.assertEquals(AllowedAccess.READ_ONLY, accessesMap.get(ids.idProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			Assert.assertEquals(AllowedAccess.READ_ONLY, accessesMap.get(ids.idCoupleProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
		}

		// le grouillot, lui, ne peut pas voir ce qui est protégé
		{
			// accès un-à-un
			assertAllowedAccess(visaGrouillot, ids.idProtege, AllowedAccess.NONE);
			assertAllowedAccess(visaGrouillot, ids.idConjointDeProtege, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaGrouillot, ids.idCoupleProtege, AllowedAccess.NONE);
			assertAllowedAccess(visaGrouillot, ids.idNormal, AllowedAccess.READ_WRITE);
			assertAllowedAccess(visaGrouillot, ids.idCoupleNormal, AllowedAccess.READ_WRITE);

			// accès batch
			final SecurityListResponse response = service.getSecurityOnParties(visaGrouillot, Arrays.asList(ids.idProtege, ids.idConjointDeProtege, ids.idCoupleProtege, ids.idNormal, ids.idCoupleNormal));
			Assert.assertEquals(visaGrouillot, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			Assert.assertEquals(AllowedAccess.NONE, accessesMap.get(ids.idProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			Assert.assertEquals(AllowedAccess.NONE, accessesMap.get(ids.idCoupleProtege));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			Assert.assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
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
		final OrdinaryTaxDeclarationAckResponse resp = service.ackOrdinaryTaxDeclarations(req);
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
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					Assert.assertNotNull(decls);
					Assert.assertEquals(1, decls.size());

					final Declaration decl = decls.get(0);
					Assert.assertNotNull(decl);

					final EtatDeclaration etat = decl.getDernierEtatDeclaration();
					if (entry.getValue() == AckStatus.OK) {
						Assert.assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());
					}
					else {
						Assert.assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
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
				final RegDate debut = date(annee, 1, 1);
				final RegDate fin = date(annee, 12, 31);

				final PersonnePhysique pp = addNonHabitant("Francis", "Noire", date(1965, 8, 31), Sexe.MASCULIN);
				addForPrincipal(pp, debut, MotifFor.ARRIVEE_HS, fin, MotifFor.DEPART_HS, MockCommune.Aigle);
				final DeclarationImpotOrdinaire di1 = addDeclarationImpot(pp, pf, debut, fin, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di1, date(annee + 1, 1, 22));
				addDelaiDeclaration(di1, date(annee + 1, 1, 22), delaiInitial, EtatDelaiDocumentFiscal.ACCORDE);
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

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
				Assert.assertNotNull(delai);
				Assert.assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});

		// demande de délai qui échoue (délai plus ancien)
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(delaiInitial.addMonths(-2)), DataHelper.coreToWeb(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, req);
			Assert.assertNotNull(resp);
			Assert.assertEquals(DeadlineStatus.ERROR_INVALID_DEADLINE, resp.getStatus());
		}

		// vérification du délai qui ne devrait pas avoir bougé
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
				Assert.assertNotNull(delai);
				Assert.assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});


		// demande de délai qui marche
		final RegDate nouveauDelai = RegDateHelper.maximum(delaiInitial.addMonths(1), RegDate.get(), NullDateBehavior.LATEST);
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(nouveauDelai), DataHelper.coreToWeb(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, req);
			Assert.assertNotNull(resp);
			Assert.assertEquals(resp.getAdditionalMessage(), DeadlineStatus.OK, resp.getStatus());
		}

		// vérification du nouveau délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				Assert.assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
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
				final CollectiviteAdministrative pays = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PAYS_D_ENHAUT.getNoColAdm());
				final CollectiviteAdministrative vevey = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_VEVEY.getNoColAdm());

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
		final PartyNumberList none = service.getModifiedTaxPayers(end, now);
		Assert.assertNotNull(none);
		Assert.assertNotNull(none.getPartyNo());
		Assert.assertEquals(0, none.getPartyNo().size());

		// 1 contribuable
		final PartyNumberList one = service.getModifiedTaxPayers(middle, now);
		Assert.assertNotNull(one);
		Assert.assertNotNull(one.getPartyNo());
		Assert.assertEquals(1, one.getPartyNo().size());
		Assert.assertEquals(pp2.getLeft().longValue(), one.getPartyNo().get(0).longValue());

		// 2 contribuables
		final PartyNumberList two = service.getModifiedTaxPayers(start, now);
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
			final DebtorInfo info = service.getDebtorInfo((int) dpiId, 2012);
			Assert.assertEquals((int) dpiId, info.getNumber());
			Assert.assertEquals(2012, info.getTaxPeriod());
			Assert.assertEquals(0, info.getNumberOfWithholdingTaxDeclarationsIssued());
			Assert.assertEquals(12, info.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
		{
			final DebtorInfo info = service.getDebtorInfo((int) dpiId, 2013);
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

		final boolean onTheFly = globalTiersIndexer.onTheFlyIndexationSwitch().isEnabled();
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
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
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(onTheFly);
		}

		// recherche avec le numéro
		{
			final List<PartyInfo> res = service.searchParty(Long.toString(ids.pp),
			                                                null, SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

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
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche avec le numéro et une donnée bidon à côté (qui doit donc être ignorée)
		{
			final List<PartyInfo> res = service.searchParty(Long.toString(ids.pp),
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

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
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche sans le numéro et une donnée bidon à côté -> aucun résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}

		// recherche par nom -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

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
				Assert.assertNull(info.getNaturalPersonSubtype());
				Assert.assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types vide -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.<PartySearchType>emptySet(), null, null, null);

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
				Assert.assertNull(info.getNaturalPersonSubtype());
				Assert.assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types mauvaise -> aucun ne vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.HOUSEHOLD), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}

		// recherche par nom avec liste de types mauvaise -> aucun ne vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.RESIDENT_NATURAL_PERSON), null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}

		// recherche par nom avec liste de types des deux -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.DEBTOR, PartySearchType.NATURAL_PERSON), null, null, null);

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
				Assert.assertNull(info.getNaturalPersonSubtype());
				Assert.assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types d'un seul -> seul celui-là vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.DEBTOR), null, null, null);

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
				Assert.assertNull(info.getNaturalPersonSubtype());
				Assert.assertNull(info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types d'un seul -> seul celui-là vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.NON_RESIDENT_NATURAL_PERSON), null, null, null);

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
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}
	}

	@Test
	public void testSearchPartyByNumeroIDE() throws Exception {

		final class Ids {
			int ppAvecUn;
			int ppAvecAutre;
			int ppSans;
		}

		final boolean onTheFly = globalTiersIndexer.onTheFlyIndexationSwitch().isEnabled();
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		final Ids ids;
		try {
			globalTiersIndexer.overwriteIndex();

			ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final PersonnePhysique ppUn = addNonHabitant("Gérard", "AvecUn", null, Sexe.MASCULIN);
					final IdentificationEntreprise identUn = new IdentificationEntreprise();
					identUn.setNumeroIde("CHE123456789");
					ppUn.addIdentificationEntreprise(identUn);

					final PersonnePhysique ppAutre = addNonHabitant("Gérard", "AvecAutre", null, Sexe.MASCULIN);
					final IdentificationEntreprise identAutre = new IdentificationEntreprise();
					identAutre.setNumeroIde("CHE987654321");
					ppAutre.addIdentificationEntreprise(identAutre);

					final PersonnePhysique ppSans = addNonHabitant("Gérard", "Sans", null, Sexe.MASCULIN);

					final Ids ids = new Ids();
					ids.ppAvecUn = ppUn.getNumero().intValue();
					ids.ppAvecAutre = ppAutre.getNumero().intValue();
					ids.ppSans = ppSans.getNumero().intValue();
					return ids;
				}
			});

			// attente de la fin de l'indexation des deux tiers
			globalTiersIndexer.sync();
		}
		finally {
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(onTheFly);
		}

		// recherche sans critère d'IDE
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(3, res.size());

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
				Assert.assertEquals(ids.ppAvecUn, info.getNumber());
				Assert.assertEquals("Gérard AvecUn", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				Assert.assertNotNull(info.getUidNumbers());
				Assert.assertEquals(Collections.singletonList("CHE123456789"), info.getUidNumbers().getUidNumber());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.ppAvecAutre, info.getNumber());
				Assert.assertEquals("Gérard AvecAutre", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				Assert.assertNotNull(info.getUidNumbers());
				Assert.assertEquals(Collections.singletonList("CHE987654321"), info.getUidNumbers().getUidNumber());
			}
			{
				final PartyInfo info = sortedRes.get(2);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.ppSans, info.getNumber());
				Assert.assertEquals("Gérard Sans", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				Assert.assertNull(info.getUidNumbers());
			}
		}

		// recherche avec critère d'IDE CHE123456789 -> un résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE123456789", null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.ppAvecUn, info.getNumber());
				Assert.assertEquals("Gérard AvecUn", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				Assert.assertNotNull(info.getUidNumbers());
				Assert.assertEquals(Collections.singletonList("CHE123456789"), info.getUidNumbers().getUidNumber());
			}
		}

		// recherche avec critère d'IDE CHE987654321 -> un autre résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE987654321", null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.ppAvecAutre, info.getNumber());
				Assert.assertEquals("Gérard AvecAutre", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				Assert.assertNotNull(info.getUidNumbers());
				Assert.assertEquals(Collections.singletonList("CHE987654321"), info.getUidNumbers().getUidNumber());
			}
		}

		// recherche avec critère d'IDE CHE111222333 -> aucun résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE111222333", null, false, null, null, null, null);

			Assert.assertNotNull(res);
			Assert.assertEquals(0, res.size());
		}
	}

	@Test
	public void testSearchPartyIndividualTaxLiability() throws Exception {

		final class Ids {
			long pp;
			long mc;
		}

		final RegDate dateNaissance = date(1979, 5, 31);
		final RegDate dateMariage = date(2008, 5, 1);

		final boolean onTheFly = globalTiersIndexer.onTheFlyIndexationSwitch().isEnabled();
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		final Ids ids;
		try {
			globalTiersIndexer.overwriteIndex();

			ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final PersonnePhysique pp = addNonHabitant("Gérard", "Nietmochevillage", dateNaissance, Sexe.MASCULIN);
					addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
					final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
					final MenageCommun mc = couple.getMenage();
					addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne, ModeImposition.ORDINAIRE);

					final Ids ids = new Ids();
					ids.pp = pp.getNumero();
					ids.mc = mc.getNumero();
					return ids;
				}
			});

			// attente de la fin de l'indexation des deux tiers
			globalTiersIndexer.sync();
		}
		finally {
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(onTheFly);
		}

		// recherche par nom avec liste de types vide -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.<PartySearchType>emptySet(), null, null, null);

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
				Assert.assertEquals(ids.pp, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertEquals(dateNaissance, DataHelper.webToRegDate(info.getDateOfBirth()));
				Assert.assertEquals(PartyType.NATURAL_PERSON, info.getType());
				Assert.assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());     // il est maintenant marié
				Assert.assertEquals(dateNaissance.addYears(18), DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				Assert.assertEquals(dateMariage.getOneDayBefore(), DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
				Assert.assertNull(info.getCorporationTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.mc, info.getNumber());
				Assert.assertEquals("Gérard Nietmochevillage", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.HOUSEHOLD, info.getType());
				Assert.assertEquals(IndividualTaxLiabilityType.ORDINARY_RESIDENT, info.getIndividualTaxLiability());
				Assert.assertEquals(dateMariage, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				Assert.assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
				Assert.assertNull(info.getCorporationTaxLiability());
			}
		}
	}

	@Test
	public void testSearchPartyCorporationTaxLiability() throws Exception {

		final RegDate dateDebut = date(2006, 5, 12);

		final class Ids {
			long entreprise1;
			long entreprise2;
		}

		final boolean onTheFly = globalTiersIndexer.onTheFlyIndexationSwitch().isEnabled();
		globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(true);
		final Ids ids;
		try {
			globalTiersIndexer.overwriteIndex();

			ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
				@Override
				public Ids doInTransaction(TransactionStatus status) {
					final Entreprise entreprise1 = addEntrepriseInconnueAuCivil();
					addFormeJuridique(entreprise1, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
					addRaisonSociale(entreprise1, dateDebut, null, "Association pour la protection des petits oiseaux des parcs");
					addRegimeFiscalVD(entreprise1, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
					addRegimeFiscalCH(entreprise1, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
					addForPrincipal(entreprise1, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne);

					final Entreprise entreprise2 = addEntrepriseInconnueAuCivil();
					addFormeJuridique(entreprise2, dateDebut, null, FormeJuridiqueEntreprise.SARL);
					addRaisonSociale(entreprise2, dateDebut, null, "Gros-bras protection");
					addRegimeFiscalVD(entreprise2, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addRegimeFiscalCH(entreprise2, dateDebut, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
					addForPrincipal(entreprise2, dateDebut, null, MockCommune.Geneve);
					addForSecondaire(entreprise2, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

					final Ids ids = new Ids();
					ids.entreprise1 = entreprise1.getNumero();
					ids.entreprise2 = entreprise2.getNumero();
					return ids;
				}
			});

			// attente de la fin de l'indexation des tiers
			globalTiersIndexer.sync();
		}
		finally {
			globalTiersIndexer.onTheFlyIndexationSwitch().setEnabled(onTheFly);
		}

		// recherche par nom avec liste de types vide -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "protection", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.<PartySearchType>emptySet(), null, null, null);

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
				Assert.assertEquals(ids.entreprise1, info.getNumber());
				Assert.assertEquals("Association pour la protection des petits oiseaux des parcs", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.CORPORATION, info.getType());
				Assert.assertNull(info.getIndividualTaxLiability());
				Assert.assertEquals(CorporationTaxLiabilityType.ORDINARY_RESIDENT, info.getCorporationTaxLiability());
				Assert.assertEquals(dateDebut, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				Assert.assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
			}
			{
				final PartyInfo info = sortedRes.get(1);
				Assert.assertNotNull(info);
				Assert.assertEquals(ids.entreprise2, info.getNumber());
				Assert.assertEquals("Gros-bras protection", info.getName1());
				Assert.assertEquals(StringUtils.EMPTY, info.getName2());
				Assert.assertNull(info.getDateOfBirth());
				Assert.assertEquals(PartyType.CORPORATION, info.getType());
				Assert.assertNull(info.getIndividualTaxLiability());
				Assert.assertEquals(CorporationTaxLiabilityType.OTHER_CANTON, info.getCorporationTaxLiability());
				Assert.assertEquals(dateDebut, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				Assert.assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
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
				ind.setNomOfficielMere(new NomPrenom("Delagrange", "Martine"));
				ind.setNomOfficielPere(new NomPrenom("Dufoin", "Melchior"));
				marieIndividu(ind, dateMariage);
			}
		});

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createSimpleEntrepriseRC(noEntreprise, noEntreprise + 1011, "Au petit coin", pmActivityStartDate, null,
				                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Cossonay, "CHE123456788");
				addOrganisation(org);
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

				final Entreprise pm = addEntrepriseConnueAuCivil(noEntreprise);
				addRegimeFiscalVD(pm, pmActivityStartDate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(pm, pmActivityStartDate, null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(pm, pmActivityStartDate, MotifFor.DEBUT_EXPLOITATION, MockCommune.Morges);

				final CollectiviteAdministrative ca = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCAT);
				final AutreCommunaute ac = addAutreCommunaute("Tata!!");
				ac.setFormeJuridique(FormeJuridique.ASS);
				final IdentificationEntreprise ide = new IdentificationEntreprise();
				ide.setNumeroIde("CHE999999996");
				ac.addIdentificationEntreprise(ide);

				final IdentificationEntreprise idePP = new IdentificationEntreprise();
				idePP.setNumeroIde("CHE100001000");
				pp.addIdentificationEntreprise(idePP);

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

		// get PP
		{
			final Party party = service.getParty((int) ids.pp, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());
			Assert.assertEquals(ids.pp, party.getNumber());


			final NaturalPerson pp = (NaturalPerson) party;
			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
			Assert.assertEquals("Dufoin", pp.getOfficialName());
			Assert.assertEquals("Balthazar", pp.getFirstName());
			Assert.assertEquals(Sex.MALE, pp.getSex());

			final List<NaturalPersonCategory> categories = pp.getCategories();
			Assert.assertNotNull(categories);
			Assert.assertEquals(1, categories.size());

			final NaturalPersonCategory category = categories.get(0);
			Assert.assertNotNull(category);
			Assert.assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, category.getCategory());
			Assert.assertEquals(datePermisC, ch.vd.unireg.xml.DataHelper.xmlToCore(category.getDateFrom()));
			Assert.assertNull(category.getDateTo());

			Assert.assertNotNull(pp.getMotherName());
			Assert.assertEquals("Delagrange", pp.getMotherName().getLastName());
			Assert.assertEquals("Martine", pp.getMotherName().getFirstNames());
			Assert.assertNotNull(pp.getFatherName());
			Assert.assertEquals("Dufoin", pp.getFatherName().getLastName());
			Assert.assertEquals("Melchior", pp.getFatherName().getFirstNames());
			Assert.assertNotNull(pp.getUidNumbers());
			Assert.assertEquals(1, pp.getUidNumbers().getUidNumber().size());
			Assert.assertEquals("CHE100001000", pp.getUidNumbers().getUidNumber().get(0));
		}
		// get MC
		{
			final Party party = service.getParty((int) ids.mc, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(CommonHousehold.class, party.getClass());
			Assert.assertEquals(ids.mc, party.getNumber());
		}
		// get DPI
		{
			final Party party = service.getParty((int) ids.dpi, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(Debtor.class, party.getClass());
			Assert.assertEquals(ids.dpi, party.getNumber());

			final Debtor dpi = (Debtor) party;
			Assert.assertEquals("Balthazar Dufoin", dpi.getName());
			Assert.assertEquals("Débiteur IS", dpi.getComplementaryName());
			Assert.assertEquals(ModeCommunication.ELECTRONIQUE, ch.vd.unireg.xml.EnumHelper.xmlToCore(dpi.getCommunicationMode()));
		}
		// get PM
		{
			final Party party = service.getParty((int) ids.pm, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(Corporation.class, party.getClass());
			Assert.assertEquals(ids.pm, party.getNumber());

			final Corporation pm = (Corporation) party;
			Assert.assertEquals("Au petit coin", pm.getName());
			Assert.assertNotNull(pm.getUidNumbers());
			Assert.assertEquals(1, pm.getUidNumbers().getUidNumber().size());
			Assert.assertEquals("CHE123456788", pm.getUidNumbers().getUidNumber().get(0));
		}
		// get CA
		{
			final Party party = service.getParty((int) ids.ca, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(AdministrativeAuthority.class, party.getClass());
			Assert.assertEquals(ids.ca, party.getNumber());

			final AdministrativeAuthority ca = (AdministrativeAuthority) party;
			Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNomCourt(), ca.getName());
			Assert.assertEquals(MockCollectiviteAdministrative.CAT.getNoColAdm(), ca.getAdministrativeAuthorityId());
		}
		// get AC
		{
			final Party party = service.getParty((int) ids.ac, null);
			Assert.assertNotNull(party);
			Assert.assertEquals(OtherCommunity.class, party.getClass());
			Assert.assertEquals(ids.ac, party.getNumber());

			final OtherCommunity ac = (OtherCommunity) party;
			Assert.assertEquals("Tata!!", ac.getName());
			Assert.assertEquals(LegalForm.ASSOCIATION, ac.getLegalForm());

			Assert.assertNotNull(ac.getUidNumbers());
			Assert.assertEquals(1, ac.getUidNumbers().getUidNumber().size());
			Assert.assertEquals("CHE999999996", ac.getUidNumbers().getUidNumber().get(0));
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

		final Party partySans = service.getParty(ppId, null);
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

		final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.ADDRESSES));
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
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.REPRESENTATION, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			Assert.assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			Assert.assertNotNull(recipient);
			Assert.assertNull(recipient.getCouple());
			Assert.assertNull(recipient.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
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
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getDebtProsecutionAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.DEBT_PROSECUTION, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			Assert.assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			Assert.assertNotNull(recipient);
			Assert.assertNull(recipient.getCouple());
			Assert.assertNull(recipient.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
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
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getMailAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.MAIL, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			Assert.assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			Assert.assertNotNull(recipient);
			Assert.assertNull(recipient.getCouple());
			Assert.assertNull(recipient.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
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
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getResidenceAddresses().get(0);
			Assert.assertNotNull(address);
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			Assert.assertEquals(AddressType.RESIDENCE, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			Assert.assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			Assert.assertNotNull(recipient);
			Assert.assertNull(recipient.getCouple());
			Assert.assertNull(recipient.getOrganisation());
			Assert.assertFalse(address.isFake());
			Assert.assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
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
			Assert.assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			Assert.assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			Assert.assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			Assert.assertNotNull(formatted);
			Assert.assertEquals("Monsieur", formatted.getLine1());
			Assert.assertEquals("Arthur Delagrange", formatted.getLine2());
			Assert.assertEquals("Avenue du Funiculaire", formatted.getLine3());
			Assert.assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			Assert.assertNull(formatted.getLine5());
			Assert.assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			Assert.assertNotNull(person);
			Assert.assertEquals("Monsieur", person.getFormalGreeting());
			Assert.assertEquals("Monsieur", person.getSalutation());
			Assert.assertEquals("Delagrange", person.getLastName());
			Assert.assertEquals("Arthur", person.getFirstName());
			Assert.assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
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

		final Party partySans = service.getParty(ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.TAX_RESIDENCES));
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
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
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
			Assert.assertEquals(dateAchat, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, tr.getStartReason());
			Assert.assertEquals(dateVente, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateTo()));
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

		final Party partySans = service.getParty(ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.VIRTUAL_TAX_RESIDENCES));
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
			Assert.assertEquals(dateArrivee.addYears(-1), ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			Assert.assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, tr.getStartReason());
			Assert.assertEquals(dateArrivee.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateTo()));
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
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
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

		final Party partySans = service.getParty(ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getMainTaxResidences());
		Assert.assertNotNull(partySans.getOtherTaxResidences());
		Assert.assertNotNull(partySans.getManagingTaxResidences());
		Assert.assertEquals(0, partySans.getMainTaxResidences().size());
		Assert.assertEquals(0, partySans.getOtherTaxResidences().size());
		Assert.assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.MANAGING_TAX_RESIDENCES));
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
			Assert.assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(mtr.getDateFrom()));
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

		final Party partySans = service.getParty(ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final CommonHousehold mcSans = (CommonHousehold) partySans;
		Assert.assertNull(mcSans.getMainTaxpayer());
		Assert.assertNull(mcSans.getSecondaryTaxpayer());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(PartyPart.HOUSEHOLD_MEMBERS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final CommonHousehold mcAvec = (CommonHousehold) partyAvec;
		{
			final NaturalPerson pp = mcAvec.getMainTaxpayer();
			Assert.assertNotNull(pp);
			Assert.assertEquals(Sex.MALE, pp.getSex());
			Assert.assertEquals("Delagrange", pp.getOfficialName());
			Assert.assertEquals("Marcel", pp.getFirstName());
			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
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

		final Party partySans = service.getParty(ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getTaxLiabilities());
		Assert.assertEquals(0, tpSans.getTaxLiabilities().size());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(PartyPart.TAX_LIABILITIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getTaxLiabilities());
		Assert.assertEquals(1, tpAvec.getTaxLiabilities().size());

		final TaxLiability tl = tpAvec.getTaxLiabilities().get(0);
		Assert.assertNotNull(tl);
		Assert.assertEquals(date(dateMariage.year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
		Assert.assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, tl.getStartReason());
		Assert.assertNull(tl.getDateTo());
		Assert.assertNull(tl.getEndReason());
		Assert.assertEquals(OrdinaryResident.class, tl.getClass());
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

		final Party partySans = service.getParty(ids.mc, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getTaxationPeriods());
		Assert.assertEquals(0, tpSans.getTaxationPeriods().size());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(PartyPart.TAXATION_PERIODS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getTaxationPeriods());
		Assert.assertEquals(RegDate.get().year() - dateMariage.year() + 1, tpAvec.getTaxationPeriods().size());

		for (int year = dateMariage.year(); year <= RegDate.get().year(); ++year) {
			final TaxationPeriod tp = tpAvec.getTaxationPeriods().get(year - dateMariage.year());
			Assert.assertNotNull(tp);
			Assert.assertEquals(date(year, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
			if (year == RegDate.get().year()) {
				Assert.assertNull(tp.getDateTo());
			}
			else {
				Assert.assertEquals(date(year, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
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

		final Party partySans = service.getParty(ids.lui, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());

		final NaturalPerson tpSans = (NaturalPerson) partySans;
		Assert.assertNotNull(tpSans.getWithholdingTaxationPeriods());
		Assert.assertEquals(0, tpSans.getWithholdingTaxationPeriods().size());

		final Party partyAvec = service.getParty(ids.lui, EnumSet.of(PartyPart.WITHHOLDING_TAXATION_PERIODS));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

		final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
		Assert.assertNotNull(tpAvec.getWithholdingTaxationPeriods());
		Assert.assertEquals(8, tpAvec.getWithholdingTaxationPeriods().size());

		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(0);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(dateNaissance.addYears(18).getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertNull(wtp.getTaxationAuthority());
			Assert.assertNull(wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(1);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(2);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(3);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(dateMariage.getLastDayOfTheMonth(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(4);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(dateMariage.getLastDayOfTheMonth().getOneDayAfter(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(5);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(6);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			Assert.assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(7);
			Assert.assertNotNull(wtp);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			Assert.assertEquals(dateDeces, ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
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

		final Party partySans = service.getParty(ids.lui, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getRelationsBetweenParties());
		Assert.assertEquals(0, partySans.getRelationsBetweenParties().size());

		final Party partyAvec = service.getParty(ids.lui, EnumSet.of(PartyPart.RELATIONS_BETWEEN_PARTIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
		Assert.assertEquals(2, partyAvec.getRelationsBetweenParties().size());

		final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
		Collections.sort(sortedRelations, new Comparator<RelationBetweenParties>() {
			@Override
			public int compare(RelationBetweenParties o1, RelationBetweenParties o2) {
				final DateRange r1 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateTo()));
				final DateRange r2 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateTo()));
				return DateRangeComparator.compareRanges(r1, r2);
			}
		});

		{
			final RelationBetweenParties rel = sortedRelations.get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateMariage, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertNull(rel.getDateTo());
			Assert.assertTrue(rel instanceof HouseholdMember);
			Assert.assertEquals(ids.mc, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getCancellationDate());
		}
		{
			final RelationBetweenParties rel = sortedRelations.get(1);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertEquals(dateDeces, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
			Assert.assertTrue(rel instanceof TaxableRevenue);
			Assert.assertEquals(ids.dpi, rel.getOtherPartyNumber());
			Assert.assertNotNull(rel.getCancellationDate());
			Assert.assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
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

		final Party partySans = service.getParty(ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		Assert.assertNotNull(tpSans.getFamilyStatuses());
		Assert.assertEquals(0, tpSans.getFamilyStatuses().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.FAMILY_STATUSES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		Assert.assertNotNull(tpAvec.getFamilyStatuses());
		Assert.assertEquals(4, tpAvec.getFamilyStatuses().size());

		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(0);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateMariage.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.SINGLE, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(1);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateMariage, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateSeparation.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.MARRIED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(2);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateSeparation, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			Assert.assertEquals(dateDivorce.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			Assert.assertNull(fs.getCancellationDate());
			Assert.assertNull(fs.getApplicableTariff());
			Assert.assertNull(fs.getMainTaxpayerNumber());
			Assert.assertNull(fs.getNumberOfChildren());
			Assert.assertEquals(MaritalStatus.SEPARATED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(3);
			Assert.assertNotNull(fs);
			Assert.assertEquals(dateDivorce, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
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

				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(2013);
				final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(pf.getAnnee(), 1, 1), date(pf.getAnnee(), 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
				addEtatDeclarationEmise(di, dateEmissionDi);
				addDelaiDeclaration(di, dateEmissionDi, dateDelaiDi, EtatDelaiDocumentFiscal.ACCORDE);

				return pp.getNumero().intValue();
			}
		});

		final Party partySans = service.getParty(ppId, null);
		Assert.assertNotNull(partySans);
		Assert.assertNotNull(partySans.getTaxDeclarations());
		Assert.assertEquals(0, partySans.getTaxDeclarations().size());

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(0, di.getDeadlines().size());
			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			Assert.assertNotNull(period);
			Assert.assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS_DEADLINES));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(1, di.getDeadlines().size());

			final TaxDeclarationDeadline delai = di.getDeadlines().get(0);
			Assert.assertNotNull(delai);
			Assert.assertEquals(dateDelaiDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getDeadline()));
			Assert.assertNull(delai.getCancellationDate());
			Assert.assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getApplicationDate()));
			Assert.assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getProcessingDate()));

			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			Assert.assertNotNull(period);
			Assert.assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(PartyPart.TAX_DECLARATIONS_STATUSES));
			Assert.assertNotNull(partyAvec);
			Assert.assertNotNull(partyAvec.getTaxDeclarations());
			Assert.assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			Assert.assertNotNull(di);
			Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			Assert.assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			Assert.assertNotNull(di.getDeadlines());
			Assert.assertEquals(0, di.getDeadlines().size());
			Assert.assertNotNull(di.getStatuses());
			Assert.assertEquals(1, di.getStatuses().size());

			final TaxDeclarationStatus status = di.getStatuses().get(0);
			Assert.assertNotNull(status);
			Assert.assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
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

		final Party partySans = service.getParty(dpiId, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(Debtor.class, partySans.getClass());

		final Debtor dpiSans = (Debtor) partySans;
		Assert.assertNotNull(dpiSans.getPeriodicities());
		Assert.assertEquals(0, dpiSans.getPeriodicities().size());

		final Party partyAvec = service.getParty(dpiId, EnumSet.of(PartyPart.DEBTOR_PERIODICITIES));
		Assert.assertNotNull(partyAvec);
		Assert.assertEquals(Debtor.class, partyAvec.getClass());

		final Debtor dpiAvec = (Debtor) partyAvec;
		Assert.assertNotNull(dpiAvec.getPeriodicities());
		Assert.assertEquals(2, dpiAvec.getPeriodicities().size());

		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(0);
			Assert.assertNotNull(dp);
			Assert.assertNull(dp.getCancellationDate());
			Assert.assertEquals(dateDebutPeriodiciteInitiale, ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			Assert.assertEquals(dateDebutPeriodiciteModifiee.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateTo()));
			Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, dp.getPeriodicity());
			Assert.assertNull(dp.getSpecificPeriod());
		}
		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(1);
			Assert.assertNotNull(dp);
			Assert.assertNull(dp.getCancellationDate());
			Assert.assertEquals(dateDebutPeriodiciteModifiee, ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			Assert.assertNull(dp.getDateTo());
			Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, dp.getPeriodicity());
			Assert.assertNull(dp.getSpecificPeriod());
		}
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

		final Party partySans = service.getParty(ids.moi, null);
		Assert.assertNotNull(partySans);
		Assert.assertEquals(NaturalPerson.class, partySans.getClass());
		Assert.assertNotNull(partySans.getRelationsBetweenParties());
		Assert.assertEquals(0, partySans.getRelationsBetweenParties().size());

		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(PartyPart.PARENTS));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());

			final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
			Assert.assertNotNull(tpAvec.getRelationsBetweenParties());
			Assert.assertEquals(1, tpAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = tpAvec.getRelationsBetweenParties().get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertEquals(dateDecesPapa, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
			Assert.assertTrue(rel instanceof Parent);
			Assert.assertEquals(ids.papa, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getCancellationDate());
		}
		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(PartyPart.CHILDREN));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());
			Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
			Assert.assertEquals(1, partyAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = partyAvec.getRelationsBetweenParties().get(0);
			Assert.assertNotNull(rel);
			Assert.assertEquals(dateNaissanceFiston, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			Assert.assertNull(rel.getDateTo());
			Assert.assertTrue(rel instanceof Child);
			Assert.assertEquals(ids.fiston, rel.getOtherPartyNumber());
			Assert.assertNull(rel.getCancellationDate());
		}
		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(PartyPart.CHILDREN, PartyPart.PARENTS));
			Assert.assertNotNull(partyAvec);
			Assert.assertEquals(NaturalPerson.class, partyAvec.getClass());
			Assert.assertNotNull(partyAvec.getRelationsBetweenParties());
			Assert.assertEquals(2, partyAvec.getRelationsBetweenParties().size());

			final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
			Collections.sort(sortedRelations, new Comparator<RelationBetweenParties>() {
				@Override
				public int compare(RelationBetweenParties o1, RelationBetweenParties o2) {
					final DateRange r1 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateTo()));
					final DateRange r2 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateTo()));
					return DateRangeComparator.compareRanges(r1, r2);
				}
			});

			{
				final RelationBetweenParties rel = sortedRelations.get(0);
				Assert.assertNotNull(rel);
				Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateDecesPapa, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertTrue(rel instanceof Parent);
				Assert.assertEquals(ids.papa, rel.getOtherPartyNumber());
				Assert.assertNull(rel.getCancellationDate());
			}
			{
				final RelationBetweenParties rel = sortedRelations.get(1);
				Assert.assertNotNull(rel);
				Assert.assertEquals(dateNaissanceFiston, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertNull(rel.getDateTo());
				Assert.assertTrue(rel instanceof Child);
				Assert.assertEquals(ids.fiston, rel.getOtherPartyNumber());
				Assert.assertNull(rel.getCancellationDate());
			}
		}
	}

	@Test
	public void testGetPartyAllPartsOnNaturalPerson() throws Exception {
		final long noIndividu = 320327L;
		final RegDate dateNaissance = date(1990, 10, 25);
		final int anneeDI = 2013;
		final RegDate dateEmissionDI = date(anneeDI + 1, 1, 6);
		final RegDate dateDelaiDI = date(anneeDI + 1, 6, 30);
		final RegDate dateEmissionLR = date(anneeDI, 1, 20);
		final RegDate dateDelaiLR = date(anneeDI, 2, 28);
		final RegDate dateSommationLR = date(anneeDI, 4, 10);
		final RegDate dateDebutRT = date(2009, 5, 1);
		final RegDate dateFinRT = date(2010, 9, 12);
		final RegDate dateDepartHS = date(anneeDI + 1, 1, 12);
		final Date dateInscriptionEfacture = DateHelper.getDateTime(2014, 1, 23, 22, 10, 46);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Gautier", "Mafalda", Sexe.FEMININ);
				ind.setTousPrenoms("Mafalda Henriette");
				ind.setNomNaissance("Dupont");
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeLaGare, null, dateNaissance, dateDepartHS);
				addOrigine(ind, MockCommune.Bern);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				addPermis(ind, TypePermis.ETABLISSEMENT, dateNaissance, null, false);
			}
		});

		final class Ids {
			int pp;
			int dpi;
			long di;
			long immeuble;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addSituation(pp, dateNaissance, null, 0, EtatCivil.CELIBATAIRE);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(anneeDI);
				final ModeleDocument mdDi = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeDI, 1, 1), date(anneeDI, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, mdDi);
				addEtatDeclarationEmise(di, dateEmissionDI);
				addDelaiDeclaration(di, dateEmissionDI, dateDelaiDI, EtatDelaiDocumentFiscal.ACCORDE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aubonne);
				dpi.setNom1("MonTestAdoré");
				dpi.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				addRapportPrestationImposable(dpi, pp, dateDebutRT, dateFinRT, false);
				final ModeleDocument mdLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, date(anneeDI, 1, 1), date(anneeDI, 1, 31), mdLr);
				addEtatDeclarationEmise(lr, dateEmissionLR);
				addDelaiDeclaration(lr, dateEmissionLR, dateDelaiLR, EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(lr, dateSommationLR, dateSommationLR.addDays(3), null);

				assertValidInteger(pp.getNumero());
				assertValidInteger(dpi.getNumero());

				// un droit de propriété sur un immeuble
				final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
				final BienFondsRF immeuble = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
				final PersonnePhysiqueRF tiersRF = addPersonnePhysiqueRF("Eric", "Bolomey", dateNaissance, "38383830ae3ff", 15615151L, null);
				addDroitPersonnePhysiqueRF(RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, null, "Achat", null, "48390a0e044", "48390a0e043",
				                           new IdentifiantAffaireRF(123, 2004, 202, 3), new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, tiersRF, immeuble, null);
				addRapprochementRF(pp, tiersRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);

				final Ids ids = new Ids();
				ids.pp = pp.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				ids.di = di.getId();
				ids.immeuble = immeuble.getId();
				return ids;
			}
		});

		efactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.pp);
				addEtatDestinataire(ids.pp, dateInscriptionEfacture, "Incription validée", null, TypeEtatDestinataire.INSCRIT, "mafalda@gautier.me", null);
			}
		});

		final Party party = service.getParty(ids.pp, EnumSet.allOf(PartyPart.class));
		Assert.assertNotNull(party);

		{
			Assert.assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			Assert.assertEquals(ids.pp, np.getNumber());
			Assert.assertEquals("Mafalda Henriette", np.getFirstNames());
			Assert.assertEquals("Mafalda", np.getFirstName());
			Assert.assertEquals("Gautier", np.getOfficialName());
			Assert.assertEquals("Dupont", np.getBirthName());
			Assert.assertEquals(Sex.FEMALE, np.getSex());

			final List<Nationality> nationalities = np.getNationalities();
			Assert.assertNotNull(nationalities);
			Assert.assertEquals(1, nationalities.size());
			{
				final Nationality nat = nationalities.get(0);
				Assert.assertNotNull(nat);
				Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(nat.getDateFrom()));
				Assert.assertNull(nat.getDateTo());
				Assert.assertNull(nat.getSwiss());
				Assert.assertNull(nat.getStateless());
				Assert.assertEquals((Integer) MockPays.France.getNoOFS(), nat.getForeignCountry());
			}

			final List<Origin> origins = np.getOrigins();
			Assert.assertNotNull(origins);
			Assert.assertEquals(1, origins.size());
			{
				final Origin orig = origins.get(0);
				Assert.assertNotNull(orig);
				Assert.assertEquals("BE", orig.getCanton().value());
				Assert.assertEquals("Bern", orig.getOriginName());
			}

			final List<TaxDeclaration> decls = np.getTaxDeclarations();
			Assert.assertNotNull(decls);
			Assert.assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				Assert.assertNotNull(decl);
				Assert.assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				Assert.assertEquals(date(anneeDI, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				Assert.assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				Assert.assertNotNull(deadlines);
				Assert.assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					Assert.assertNotNull(deadline);
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					Assert.assertEquals(dateDelaiDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					Assert.assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				Assert.assertNotNull(statuses);
				Assert.assertEquals(1, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertNull(status.getFee());
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
			}

			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getDateOfBirth()));
			Assert.assertNull(np.getDateOfDeath());

			final List<NaturalPersonCategory> cats = np.getCategories();
			Assert.assertNotNull(cats);
			Assert.assertEquals(1, cats.size());
			{
				final NaturalPersonCategory cat = cats.get(0);
				Assert.assertNotNull(cat);
				Assert.assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, cat.getCategory());
				Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(cat.getDateFrom()));
				Assert.assertNull(cat.getDateTo());
			}

			final List<EbillingStatus> ebillingStatuses = np.getEbillingStatuses();
			Assert.assertNotNull(ebillingStatuses);
			Assert.assertEquals(2, ebillingStatuses.size());
			{
				final EbillingStatus st = ebillingStatuses.get(0);
				Assert.assertNotNull(st);
				Assert.assertEquals(EbillingStatusType.NOT_REGISTERED, st.getType());
				Assert.assertNull(st.getSince());
			}
			{
				final EbillingStatus st = ebillingStatuses.get(1);
				Assert.assertNotNull(st);
				Assert.assertEquals(EbillingStatusType.REGISTERED, st.getType());
				Assert.assertEquals(dateInscriptionEfacture, XmlUtils.xmlcal2date(st.getSince()));
			}

			final List<WithholdingTaxationPeriod> wtps = np.getWithholdingTaxationPeriods();
			Assert.assertNotNull(wtps);
			Assert.assertEquals(2, wtps.size());
			{
				final WithholdingTaxationPeriod wtp = wtps.get(0);
				Assert.assertNotNull(wtp);
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}
			{
				final WithholdingTaxationPeriod wtp = wtps.get(1);
				Assert.assertNotNull(wtp);
				Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				Assert.assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}

			Assert.assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityStartDate()));
			Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityEndDate()));

			final List<TaxResidence> fors = np.getMainTaxResidences();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateTo()));
				Assert.assertEquals(LiabilityChangeReason.MAJORITY, ff.getStartReason());
				Assert.assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, ff.getEndReason());
				Assert.assertEquals(TaxType.INCOME_WEALTH, ff.getTaxType());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertEquals(TaxationMethod.ORDINARY, ff.getTaxationMethod());
				Assert.assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = np.getRelationsBetweenParties();
			Assert.assertNotNull(rels);
			Assert.assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				Assert.assertNotNull(rel);
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertEquals(ids.dpi, rel.getOtherPartyNumber());
				Assert.assertTrue(rel instanceof TaxableRevenue);
				Assert.assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}

			final List<TaxLiability> tls = np.getTaxLiabilities();
			Assert.assertNotNull(tls);
			Assert.assertEquals(1, tls.size());
			{
				final TaxLiability tl = tls.get(0);
				Assert.assertNotNull(tl);
				Assert.assertEquals(OrdinaryResident.class, tl.getClass());
				Assert.assertEquals(date(dateNaissance.addYears(18).year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateTo()));
				Assert.assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
				Assert.assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, tl.getEndReason());
			}

			final List<TaxationPeriod> tps = np.getTaxationPeriods();
			Assert.assertNotNull(tps);
			Assert.assertEquals(7, tps.size());
			{
				final TaxationPeriod tp = tps.get(0);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(1);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(2);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(3);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(4);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(5);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertEquals((Long) ids.di, tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(6);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2014, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}

			final List<ResidencyPeriod> residencyPeriods = np.getResidencyPeriods();
			Assert.assertNotNull(residencyPeriods);
			Assert.assertEquals(1, residencyPeriods.size());
			{
				final ResidencyPeriod rp = residencyPeriods.get(0);
				Assert.assertNotNull(rp);
				Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rp.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(rp.getDateTo()));
			}

			final List<LandRight> landRights = np.getLandRights();
			Assert.assertNotNull(landRights);
			Assert.assertEquals(1, landRights.size());
			{
				final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
				Assert.assertNotNull(landRight0);
				Assert.assertNull(landRight0.getCommunityId());
				Assert.assertEquals(OwnershipType.SOLE_OWNERSHIP, landRight0.getType());
				assertShare(1, 1, landRight0.getShare());
				Assert.assertEquals(date(2004, 4, 12), ch.vd.unireg.xml.DataHelper.xmlToCore(landRight0.getDateFrom()));
				Assert.assertNull(landRight0.getDateTo());
				Assert.assertEquals("Achat", landRight0.getStartReason());
				Assert.assertNull(landRight0.getEndReason());
				assertCaseIdentifier(123, "2004/202/3", landRight0.getCaseIdentifier());
				Assert.assertEquals(ids.immeuble, landRight0.getImmovablePropertyId());
			}
		}
	}

	@Test
	public void testGetPartyAllPartsOnDebtor() throws Exception {
		final long noIndividu = 320327L;
		final RegDate dateNaissance = date(1990, 10, 25);
		final int anneeDI = 2013;
		final RegDate dateEmissionDI = date(anneeDI + 1, 1, 6);
		final RegDate dateDelaiDI = date(anneeDI + 1, 6, 30);
		final RegDate dateEmissionLR = date(anneeDI, 1, 20);
		final RegDate dateDelaiLR = date(anneeDI, 2, 28);
		final RegDate dateSommationLR = date(anneeDI, 4, 10);
		final RegDate dateDebutRT = date(2009, 5, 1);
		final RegDate dateFinRT = date(2010, 9, 12);
		final RegDate dateDepartHS = date(anneeDI + 1, 1, 12);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Gautier", "Mafalda", Sexe.FEMININ);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				addPermis(ind, TypePermis.ETABLISSEMENT, dateNaissance, null, false);
			}
		});

		final class Ids {
			int pp;
			int dpi;
			long di;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addSituation(pp, dateNaissance, null, 0, EtatCivil.CELIBATAIRE);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(anneeDI);
				final ModeleDocument mdDi = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeDI, 1, 1), date(anneeDI, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, mdDi);
				addEtatDeclarationEmise(di, dateEmissionDI);
				addDelaiDeclaration(di, dateEmissionDI, dateDelaiDI, EtatDelaiDocumentFiscal.ACCORDE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aubonne);
				dpi.setNom1("MonTestAdoré");
				dpi.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				addRapportPrestationImposable(dpi, pp, dateDebutRT, dateFinRT, false);
				final ModeleDocument mdLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, date(anneeDI, 1, 1), date(anneeDI, 1, 31), mdLr);
				addEtatDeclarationEmise(lr, dateEmissionLR);
				addDelaiDeclaration(lr, dateEmissionLR, dateDelaiLR, EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(lr, dateSommationLR, dateSommationLR.addDays(3), null);

				assertValidInteger(pp.getNumero());
				assertValidInteger(dpi.getNumero());

				final Ids ids = new Ids();
				ids.pp = pp.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				ids.di = di.getId();
				return ids;
			}
		});

		final Party party = service.getParty(ids.dpi, EnumSet.allOf(PartyPart.class));
		Assert.assertNotNull(party);
		{
			Assert.assertEquals(Debtor.class, party.getClass());

			final Debtor dpi = (Debtor) party;
			Assert.assertEquals(ids.dpi, dpi.getNumber());
			Assert.assertEquals("MonTestAdoré", dpi.getName());

			final List<TaxDeclaration> decls = dpi.getTaxDeclarations();
			Assert.assertNotNull(decls);
			Assert.assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				Assert.assertNotNull(decl);
				Assert.assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				Assert.assertEquals(date(anneeDI, 1, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				Assert.assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				Assert.assertNotNull(deadlines);
				Assert.assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					Assert.assertNotNull(deadline);
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					Assert.assertEquals(dateDelaiLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					Assert.assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				Assert.assertNotNull(statuses);
				Assert.assertEquals(2, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertNull(status.getFee());
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
				{
					final TaxDeclarationStatus status = statuses.get(1);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertNull(status.getFee());
					Assert.assertEquals(dateSommationLR.addDays(3), ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, status.getType());
				}
			}

			final List<DebtorPeriodicity> periodicities = dpi.getPeriodicities();
			Assert.assertNotNull(periodicities);
			Assert.assertEquals(1, periodicities.size());
			{
				final DebtorPeriodicity periodicity = periodicities.get(0);
				Assert.assertNotNull(periodicity);
				Assert.assertNull(periodicity.getCancellationDate());
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(periodicity.getDateFrom()));
				Assert.assertNull(periodicity.getDateTo());
				Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, periodicity.getPeriodicity());
				Assert.assertNull(periodicity.getSpecificPeriod());
			}

			Assert.assertNull(dpi.getAssociatedTaxpayerNumber());
			Assert.assertEquals(DebtorCategory.REGULAR, dpi.getCategory());
			Assert.assertEquals(CommunicationMode.UPLOAD, dpi.getCommunicationMode());
			Assert.assertFalse(dpi.isWithoutReminder());
			Assert.assertFalse(dpi.isWithoutWithholdingTaxDeclaration());

			Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(dpi.getActivityStartDate()));
			Assert.assertNull(dpi.getActivityEndDate());

			final List<TaxResidence> fors = dpi.getMainTaxResidences();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertNull(ff.getDateTo());
				Assert.assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, ff.getStartReason());
				Assert.assertNull(ff.getEndReason());
				Assert.assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, ff.getTaxType());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertNull(ff.getTaxationMethod());
				Assert.assertNull(ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = dpi.getRelationsBetweenParties();
			Assert.assertNotNull(rels);
			Assert.assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				Assert.assertNotNull(rel);
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertEquals(ids.pp, rel.getOtherPartyNumber());
				Assert.assertTrue(rel instanceof TaxableRevenue);
				Assert.assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}
		}
	}

	@Test
	public void testGetPartyAllPartsOnCorporation() throws Exception {

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// rien
			}
		});

		efactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				// rien non plus...
			}
		});


		final long idpm = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();

				addRaisonSociale(entreprise, date(2000, 3, 1), date(2013, 5, 12), "Ma petite entreprise");
				addRaisonSociale(entreprise, date(2013, 5, 13), null, "Ma grande entreprise");
				addFormeJuridique(entreprise, date(2000, 3, 1), null, FormeJuridiqueEntreprise.SARL);
				addCapitalEntreprise(entreprise, date(2000, 3, 1), date(2009, 12, 31), new MontantMonetaire(1000L, "CHF"));
				addCapitalEntreprise(entreprise, date(2010, 1, 1), date(2013, 5, 12), new MontantMonetaire(1100L, "CHF"));
				addCapitalEntreprise(entreprise, date(2013, 5, 13), null, new MontantMonetaire(100000L, "CHF"));
				addFlagEntreprise(entreprise, date(2010, 6, 2), RegDate.get(2013, 5, 26), TypeFlagEntreprise.UTILITE_PUBLIQUE);

				addRegimeFiscalVD(entreprise, date(2000, 3, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addRegimeFiscalCH(entreprise, date(2000, 3, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
				addForPrincipal(entreprise, date(2000, 3, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Geneve);
				addForSecondaire(entreprise, date(2005, 3, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay.getNoOFS(), MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

				addBouclement(entreprise, date(2001, 6, 1), DayMonth.get(6, 30), 12);
				return entreprise.getNumero();
			}
		});
		assertValidInteger(idpm);

		final Set<PartyPart> parts = EnumSet.allOf(PartyPart.class);
		final Party party = service.getParty((int) idpm, parts);
		Assert.assertNotNull(party);
		{
			Assert.assertEquals(Corporation.class, party.getClass());

			final Corporation corp = (Corporation) party;
			Assert.assertEquals((int) idpm, corp.getNumber());
			Assert.assertEquals("Ma grande entreprise", corp.getName());

			Assert.assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(corp.getActivityStartDate()));
			Assert.assertNull(corp.getActivityEndDate());

			final List<TaxResidence> prnFors = corp.getMainTaxResidences();
			Assert.assertNotNull(prnFors);
			Assert.assertEquals(1, prnFors.size());
			{
				final TaxResidence ff = prnFors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertNull(ff.getDateTo());
				Assert.assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, ff.getStartReason());
				Assert.assertNull(ff.getEndReason());
				Assert.assertEquals(TaxType.PROFITS_CAPITAL, ff.getTaxType());
				Assert.assertEquals(MockCommune.Geneve.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertNull(ff.getTaxationMethod());
				Assert.assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}
			final List<TaxResidence> secFors = corp.getOtherTaxResidences();
			Assert.assertNotNull(secFors);
			Assert.assertEquals(1, secFors.size());
			{
				final TaxResidence ff = secFors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(date(2005, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertNull(ff.getDateTo());
				Assert.assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, ff.getStartReason());
				Assert.assertNull(ff.getEndReason());
				Assert.assertEquals(TaxType.PROFITS_CAPITAL, ff.getTaxType());
				Assert.assertEquals(MockCommune.Cossonay.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertNull(ff.getTaxationMethod());
				Assert.assertEquals(TaxLiabilityReason.STABLE_ESTABLISHMENT, ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}

			final List<Capital> caps = corp.getCapitals();
			Assert.assertNotNull(caps);
			Assert.assertEquals(3, caps.size());
			{
				final Capital cap = caps.get(0);
				Assert.assertNotNull(cap);
				Assert.assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateTo()));
				Assert.assertNotNull(cap.getPaidInCapital());
				Assert.assertEquals(1000L, cap.getPaidInCapital().getAmount());
				Assert.assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}
			{
				final Capital cap = caps.get(1);
				Assert.assertNotNull(cap);
				Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				Assert.assertEquals(date(2013, 5, 12), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateTo()));
				Assert.assertNotNull(cap.getPaidInCapital());
				Assert.assertEquals(1100L, cap.getPaidInCapital().getAmount());
				Assert.assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}
			{
				final Capital cap = caps.get(2);
				Assert.assertNotNull(cap);
				Assert.assertEquals(date(2013, 5, 13), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				Assert.assertNull(cap.getDateTo());
				Assert.assertNotNull(cap.getPaidInCapital());
				Assert.assertEquals(100000L, cap.getPaidInCapital().getAmount());
				Assert.assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}

			final List<ch.vd.unireg.xml.party.corporation.v5.LegalForm> lfs = corp.getLegalForms();
			Assert.assertNotNull(lfs);
			Assert.assertEquals(1, lfs.size());
			{
				final ch.vd.unireg.xml.party.corporation.v5.LegalForm lf = lfs.get(0);
				Assert.assertNotNull(lf);
				Assert.assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(lf.getDateFrom()));
				Assert.assertNull(lf.getDateTo());
				Assert.assertEquals(LegalForm.LIMITED_LIABILITY_COMPANY, lf.getType());
				Assert.assertEquals("Société à responsabilité limitée", lf.getLabel());
				Assert.assertEquals(LegalFormCategory.CAPITAL_COMPANY, lf.getLegalFormCategory());
			}

			final RegDate today = RegDate.get();
			final List<BusinessYear> bys = corp.getBusinessYears();
			Assert.assertNotNull(bys);
			final int nbExpectedExercices = today.year() - 2000 + (today.month() > 6 ? 1 : 0);
			Assert.assertEquals(nbExpectedExercices, bys.size());
			for (int i = 0; i < nbExpectedExercices; ++i) {
				final BusinessYear by = bys.get(i);
				Assert.assertNotNull(by);
				if (i == 0) {
					Assert.assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateFrom()));
				}
				else {
					Assert.assertEquals(date(2000 + i, 7, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateFrom()));
				}
				Assert.assertEquals(date(2001 + i, 6, 30), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateTo()));
			}

			final List<CorporationFlag> flags = corp.getCorporationFlags();
			Assert.assertNotNull(flags);
			Assert.assertEquals(1, flags.size());
			{
				final CorporationFlag flag = flags.get(0);
				Assert.assertNotNull(flag);
				Assert.assertEquals(date(2010, 6, 2), ch.vd.unireg.xml.DataHelper.xmlToCore(flag.getDateFrom()));
				Assert.assertEquals(date(2013, 5, 26), ch.vd.unireg.xml.DataHelper.xmlToCore(flag.getDateTo()));
				Assert.assertEquals(CorporationFlagType.PUBLIC_INTEREST, flag.getType());
			}
		}
	}

	@Test
	public void testGetPartiesMaxNumber() throws Exception {

		// la limite du nombre de tiers demandables en une fois est de 100 -> "100" fonctionne, "101" ne doit plus fonctionner
		final int max = BusinessWebServiceImpl.MAX_BATCH_SIZE;

		final Random rnd = new Random();
		{
			final List<Integer> nos = new ArrayList<>(max);
			while (nos.size() < max) {
				final int data = rnd.nextInt(100000000);
				if (!nos.contains(data)) {
					nos.add(data);
				}
			}
			final Parties res = service.getParties(nos, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(max, res.getEntries().size());
		}
		{
			final List<Integer> nos = new ArrayList<>(max + 1);
			while (nos.size() <= max) {
				final int data = rnd.nextInt(100000000);
				if (!nos.contains(data)) {
					nos.add(data);
				}
			}

			try {
				service.getParties(nos, null);
				Assert.fail("Nombre de tiers demandés trop élevé... L'appel aurait dû échouer.");
			}
			catch (BadRequestException e) {
				Assert.assertEquals("Le nombre de tiers demandés ne peut dépasser " + max, e.getMessage());
			}
		}

		// 101 peut passer s'il n'y a que 100 (ou moins) éléments distincts
		{
			final List<Integer> nos = new ArrayList<>(max + 1);
			while (nos.size() < max) {
				final int data = rnd.nextInt(100000000);
				if (!nos.contains(data)) {
					nos.add(data);
				}
			}
			nos.add(nos.get(0));

			final Parties res = service.getParties(nos, null);
			Assert.assertNotNull(res);
			Assert.assertEquals(max, res.getEntries().size());
		}
	}

	@Test
	public void testGetParties() throws Exception {

		final long noEntreprise = 423672L;
		final RegDate pmActivityStartDate = date(2000, 1, 1);

		serviceOrganisation.setUp(new MockServiceOrganisation() {
			@Override
			protected void init() {
				final MockOrganisation org = MockOrganisationFactory.createSimpleEntrepriseRC(noEntreprise, noEntreprise + 1011, "Au petit coin", pmActivityStartDate, null,
				                                                                              FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Cossonay, "CHE123456788");
				addOrganisation(org);
			}
		});

		final class Ids {
			int pp1;
			int pp2;
			int ppProtege;
			int pm;
			int ac;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp1 = addNonHabitant("Daubet", "Philibert", null, Sexe.MASCULIN);
				final PersonnePhysique pp2 = addNonHabitant("Baudet", "Ernestine", null, Sexe.FEMININ);
				final PersonnePhysique ppProtege = addNonHabitant("Knox", "Fort", null, null);
				final DroitAcces da = new DroitAcces();
				da.setDateDebut(date(2000, 1, 1));
				da.setNiveau(Niveau.LECTURE);
				da.setNoIndividuOperateur(798455L);
				da.setType(TypeDroitAcces.AUTORISATION);
				da.setTiers(ppProtege);
				hibernateTemplate.merge(da);

				final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntreprise);

				final AutreCommunaute ac = addAutreCommunaute("Tata!!");
				ac.setFormeJuridique(FormeJuridique.ASS);
				final IdentificationEntreprise ide = new IdentificationEntreprise();
				ide.setNumeroIde("CHE999999996");
				ac.addIdentificationEntreprise(ide);

				final Ids ids = new Ids();
				ids.pp1 = pp1.getNumero().intValue();
				ids.pp2 = pp2.getNumero().intValue();
				ids.ppProtege = ppProtege.getNumero().intValue();
				ids.pm = entreprise.getNumero().intValue();
				ids.ac = ac.getNumero().intValue();
				return ids;
			}
		});

		AuthenticationHelper.pushPrincipal("TOTO", 22);
		try {
			final Parties parties = service.getParties(Arrays.asList(ids.pp1, ids.ppProtege, 99999, ids.pp2, ids.pm, ids.ac), null);
			Assert.assertNotNull(parties);
			Assert.assertNotNull(parties.getEntries());
			Assert.assertEquals(6, parties.getEntries().size());

			final List<Entry> sorted = new ArrayList<>(parties.getEntries());
			Collections.sort(sorted, new Comparator<Entry>() {
				@Override
				public int compare(Entry o1, Entry o2) {
					return o1.getPartyNo() - o2.getPartyNo();
				}
			});

			{
				final Entry e = sorted.get(0);
				Assert.assertNotNull(e.getParty());
				Assert.assertNull(e.getError());
				Assert.assertEquals(Corporation.class, e.getParty().getClass());
				Assert.assertEquals(ids.pm, e.getPartyNo());
				Assert.assertEquals(ids.pm, e.getParty().getNumber());

				final Corporation corp = (Corporation) e.getParty();
				Assert.assertNotNull(corp.getUidNumbers());
				Assert.assertEquals(1, corp.getUidNumbers().getUidNumber().size());
				Assert.assertEquals("CHE123456788", corp.getUidNumbers().getUidNumber().get(0));
			}
			{
				final Entry e = sorted.get(1);
				Assert.assertNull(e.getParty());
				Assert.assertNotNull(e.getError());
				Assert.assertEquals(99999, e.getPartyNo());
				Assert.assertEquals("Le tiers n°999.99 n'existe pas", e.getError().getErrorMessage());
				Assert.assertEquals(ErrorType.BUSINESS, e.getError().getType());
			}
			{
				final Entry e = sorted.get(2);
				Assert.assertNotNull(e.getParty());
				Assert.assertNull(e.getError());
				Assert.assertEquals(OtherCommunity.class, e.getParty().getClass());
				Assert.assertEquals(ids.ac, e.getPartyNo());
				Assert.assertEquals(ids.ac, e.getParty().getNumber());

				final OtherCommunity otherComm = (OtherCommunity) e.getParty();
				Assert.assertNotNull(otherComm.getUidNumbers());
				Assert.assertEquals(1, otherComm.getUidNumbers().getUidNumber().size());
				Assert.assertEquals("CHE999999996", otherComm.getUidNumbers().getUidNumber().get(0));
			}
			{
				final Entry e = sorted.get(3);
				Assert.assertNotNull(e.getParty());
				Assert.assertNull(e.getError());
				Assert.assertEquals(NaturalPerson.class, e.getParty().getClass());
				Assert.assertEquals(ids.pp1, e.getPartyNo());
				Assert.assertEquals(ids.pp1, e.getParty().getNumber());
			}
			{
				final Entry e = sorted.get(4);
				Assert.assertNotNull(e.getParty());
				Assert.assertNull(e.getError());
				Assert.assertEquals(NaturalPerson.class, e.getParty().getClass());
				Assert.assertEquals(ids.pp2, e.getPartyNo());
				Assert.assertEquals(ids.pp2, e.getParty().getNumber());
			}
			{
				final Entry e = sorted.get(5);
				Assert.assertNull(e.getParty());
				Assert.assertNotNull(e.getError());
				Assert.assertEquals(ids.ppProtege, e.getPartyNo());
				Assert.assertEquals("L'utilisateur TOTO/22 ne possède aucun droit de lecture sur le dossier " + ids.ppProtege, e.getError().getErrorMessage());
				Assert.assertEquals(ErrorType.ACCESS, e.getError().getType());
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Pour vérifier que les requêtes SQL sur toutes les parts fonctionnent
	 */
	@Test
	public void testGetPartiesAllParts() throws Exception {

		final long noIndividu = 320327L;
		final RegDate dateNaissance = date(1990, 10, 25);
		final int anneeDI = 2013;
		final RegDate dateEmissionDI = date(anneeDI + 1, 1, 6);
		final RegDate dateDelaiDI = date(anneeDI + 1, 6, 30);
		final RegDate dateEmissionLR = date(anneeDI, 1, 20);
		final RegDate dateDelaiLR = date(anneeDI, 2, 28);
		final RegDate dateSommationLR = date(anneeDI, 4, 10);
		final RegDate dateDebutRT = date(2009, 5, 1);
		final RegDate dateFinRT = date(2010, 9, 12);
		final RegDate dateDepartHS = date(anneeDI + 1, 1, 12);
		final Date dateInscriptionEfacture = DateHelper.getDateTime(2014, 1, 23, 22, 10, 46);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Gautier", "Mafalda", Sexe.FEMININ);
				addNationalite(ind, MockPays.France, dateNaissance, null);
				addPermis(ind, TypePermis.ETABLISSEMENT, dateNaissance, null, false);
			}
		});

		final class Ids {
			int pp;
			int dpi;
			long di;
		}

		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addSituation(pp, dateNaissance, null, 0, EtatCivil.CELIBATAIRE);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateDepartHS, MotifFor.DEPART_HS, MockCommune.Lausanne);
				final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
				final PeriodeFiscale pf = addPeriodeFiscale(anneeDI);
				final ModeleDocument mdDi = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
				final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(anneeDI, 1, 1), date(anneeDI, 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, mdDi);
				addEtatDeclarationEmise(di, dateEmissionDI);
				addDelaiDeclaration(di, dateEmissionDI, dateDelaiDI, EtatDelaiDocumentFiscal.ACCORDE);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addForDebiteur(dpi, date(2009, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Aubonne);
				dpi.setNom1("MonTestAdoré");
				dpi.setModeCommunication(ModeCommunication.ELECTRONIQUE);
				addRapportPrestationImposable(dpi, pp, dateDebutRT, dateFinRT, false);
				final ModeleDocument mdLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
				final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, date(anneeDI, 1, 1), date(anneeDI, 1, 31), mdLr);
				addEtatDeclarationEmise(lr, dateEmissionLR);
				addDelaiDeclaration(lr, dateEmissionLR, dateDelaiLR, EtatDelaiDocumentFiscal.ACCORDE);
				addEtatDeclarationSommee(lr, dateSommationLR, dateSommationLR.addDays(3), 10);

				assertValidInteger(pp.getNumero());
				assertValidInteger(dpi.getNumero());

				final Ids ids = new Ids();
				ids.pp = pp.getNumero().intValue();
				ids.dpi = dpi.getNumero().intValue();
				ids.di = di.getId();
				return ids;
			}
		});

		efactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.pp);
				addEtatDestinataire(ids.pp, dateInscriptionEfacture, "Incription validée", null, TypeEtatDestinataire.INSCRIT, "mafalda@gautier.me", null);
			}
		});

		final Parties parties = service.getParties(Arrays.asList(ids.pp, ids.dpi), EnumSet.allOf(PartyPart.class));
		Assert.assertNotNull(parties);
		Assert.assertNotNull(parties.getEntries());
		Assert.assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		Collections.sort(sorted, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				return o1.getPartyNo() - o2.getPartyNo();
			}
		});

		{
			final Entry e = sorted.get(0);
			Assert.assertNotNull(e);
			Assert.assertNull(e.getError());
			Assert.assertNotNull(e.getParty());
			Assert.assertEquals(Debtor.class, e.getParty().getClass());

			final Debtor dpi = (Debtor) e.getParty();
			Assert.assertEquals(ids.dpi, dpi.getNumber());
			Assert.assertEquals("MonTestAdoré", dpi.getName());

			final List<TaxDeclaration> decls = dpi.getTaxDeclarations();
			Assert.assertNotNull(decls);
			Assert.assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				Assert.assertNotNull(decl);
				Assert.assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				Assert.assertEquals(date(anneeDI, 1, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				Assert.assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				Assert.assertNotNull(deadlines);
				Assert.assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					Assert.assertNotNull(deadline);
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					Assert.assertEquals(dateDelaiLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					Assert.assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				Assert.assertNotNull(statuses);
				Assert.assertEquals(2, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertNull(status.getFee());
					Assert.assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
				{
					final TaxDeclarationStatus status = statuses.get(1);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertEquals((Integer) 10, status.getFee());
					Assert.assertEquals(dateSommationLR.addDays(3), ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, status.getType());
				}
			}

			final List<DebtorPeriodicity> periodicities = dpi.getPeriodicities();
			Assert.assertNotNull(periodicities);
			Assert.assertEquals(1, periodicities.size());
			{
				final DebtorPeriodicity periodicity = periodicities.get(0);
				Assert.assertNotNull(periodicity);
				Assert.assertNull(periodicity.getCancellationDate());
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(periodicity.getDateFrom()));
				Assert.assertNull(periodicity.getDateTo());
				Assert.assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, periodicity.getPeriodicity());
				Assert.assertNull(periodicity.getSpecificPeriod());
			}

			Assert.assertNull(dpi.getAssociatedTaxpayerNumber());
			Assert.assertEquals(DebtorCategory.REGULAR, dpi.getCategory());
			Assert.assertEquals(CommunicationMode.UPLOAD, dpi.getCommunicationMode());
			Assert.assertFalse(dpi.isWithoutReminder());
			Assert.assertFalse(dpi.isWithoutWithholdingTaxDeclaration());

			Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(dpi.getActivityStartDate()));
			Assert.assertNull(dpi.getActivityEndDate());

			final List<TaxResidence> fors = dpi.getMainTaxResidences();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertNull(ff.getDateTo());
				Assert.assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, ff.getStartReason());
				Assert.assertNull(ff.getEndReason());
				Assert.assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, ff.getTaxType());
				Assert.assertEquals(MockCommune.Aubonne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertNull(ff.getTaxationMethod());
				Assert.assertNull(ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = dpi.getRelationsBetweenParties();
			Assert.assertNotNull(rels);
			Assert.assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				Assert.assertNotNull(rel);
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertEquals(ids.pp, rel.getOtherPartyNumber());
				Assert.assertTrue(rel instanceof TaxableRevenue);
				Assert.assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}
		}
		{
			final Entry o = sorted.get(1);
			Assert.assertNotNull(o);
			Assert.assertNull(o.getError());
			Assert.assertNotNull(o.getParty());
			Assert.assertEquals(NaturalPerson.class, o.getParty().getClass());

			final NaturalPerson np = (NaturalPerson) o.getParty();
			Assert.assertEquals(ids.pp, np.getNumber());
			Assert.assertEquals("Mafalda", np.getFirstName());
			Assert.assertEquals("Gautier", np.getOfficialName());
			Assert.assertEquals(Sex.FEMALE, np.getSex());

			final List<TaxDeclaration> decls = np.getTaxDeclarations();
			Assert.assertNotNull(decls);
			Assert.assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				Assert.assertNotNull(decl);
				Assert.assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				Assert.assertEquals(date(anneeDI, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				Assert.assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				Assert.assertNotNull(deadlines);
				Assert.assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					Assert.assertNotNull(deadline);
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					Assert.assertEquals(dateDelaiDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					Assert.assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				Assert.assertNotNull(statuses);
				Assert.assertEquals(1, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					Assert.assertNotNull(status);
					Assert.assertNull(status.getCancellationDate());
					Assert.assertNull(status.getSource());
					Assert.assertNull(status.getFee());
					Assert.assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					Assert.assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
			}

			Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getDateOfBirth()));
			Assert.assertNull(np.getDateOfDeath());

			final List<NaturalPersonCategory> cats = np.getCategories();
			Assert.assertNotNull(cats);
			Assert.assertEquals(1, cats.size());
			{
				final NaturalPersonCategory cat = cats.get(0);
				Assert.assertNotNull(cat);
				Assert.assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, cat.getCategory());
				Assert.assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(cat.getDateFrom()));
				Assert.assertNull(cat.getDateTo());
			}

			final List<EbillingStatus> ebillingStatuses = np.getEbillingStatuses();
			Assert.assertNotNull(ebillingStatuses);
			Assert.assertEquals(2, ebillingStatuses.size());
			{
				final EbillingStatus st = ebillingStatuses.get(0);
				Assert.assertNotNull(st);
				Assert.assertEquals(EbillingStatusType.NOT_REGISTERED, st.getType());
				Assert.assertNull(st.getSince());
			}
			{
				final EbillingStatus st = ebillingStatuses.get(1);
				Assert.assertNotNull(st);
				Assert.assertEquals(EbillingStatusType.REGISTERED, st.getType());
				Assert.assertEquals(dateInscriptionEfacture, XmlUtils.xmlcal2date(st.getSince()));
			}

			final List<WithholdingTaxationPeriod> wtps = np.getWithholdingTaxationPeriods();
			Assert.assertNotNull(wtps);
			Assert.assertEquals(2, wtps.size());
			{
				final WithholdingTaxationPeriod wtp = wtps.get(0);
				Assert.assertNotNull(wtp);
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}
			{
				final WithholdingTaxationPeriod wtp = wtps.get(1);
				Assert.assertNotNull(wtp);
				Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				Assert.assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				Assert.assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				Assert.assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}

			Assert.assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityStartDate()));
			Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityEndDate()));

			final List<TaxResidence> fors = np.getMainTaxResidences();
			Assert.assertNotNull(fors);
			Assert.assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				Assert.assertNotNull(ff);
				Assert.assertNull(ff.getCancellationDate());
				Assert.assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateTo()));
				Assert.assertEquals(LiabilityChangeReason.MAJORITY, ff.getStartReason());
				Assert.assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, ff.getEndReason());
				Assert.assertEquals(TaxType.INCOME_WEALTH, ff.getTaxType());
				Assert.assertEquals(MockCommune.Lausanne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				Assert.assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				Assert.assertEquals(TaxationMethod.ORDINARY, ff.getTaxationMethod());
				Assert.assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				Assert.assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = np.getRelationsBetweenParties();
			Assert.assertNotNull(rels);
			Assert.assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				Assert.assertNotNull(rel);
				Assert.assertNull(rel.getCancellationDate());
				Assert.assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				Assert.assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				Assert.assertEquals(ids.dpi, rel.getOtherPartyNumber());
				Assert.assertTrue(rel instanceof TaxableRevenue);
				Assert.assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}

			final List<TaxLiability> tls = np.getTaxLiabilities();
			Assert.assertNotNull(tls);
			Assert.assertEquals(1, tls.size());
			{
				final TaxLiability tl = tls.get(0);
				Assert.assertNotNull(tl);
				Assert.assertEquals(OrdinaryResident.class, tl.getClass());
				Assert.assertEquals(date(dateNaissance.addYears(18).year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateTo()));
				Assert.assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
				Assert.assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, tl.getEndReason());
			}

			final List<TaxationPeriod> tps = np.getTaxationPeriods();
			Assert.assertNotNull(tps);
			Assert.assertEquals(7, tps.size());
			{
				final TaxationPeriod tp = tps.get(0);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(1);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(2);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(3);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(4);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(5);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertEquals((Long) ids.di, tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(6);
				Assert.assertNotNull(tp);
				Assert.assertEquals(date(2014, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				Assert.assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				Assert.assertNull(tp.getTaxDeclarationId());
			}
		}
	}

	/**
	 * On vérifie ici que les fors fiscaux du ménage sont bien pris en compte malgré un "préchauffage" des données
	 * extraites de la base qui ne va justement pas jusqu'à aller chercher les fors sur un contribuable non demandé en entrée
	 */
	@Test
	public void testGetPartiesIndividuMariePeriodesImpositionIS() throws Exception {

		final long noIndividu = 427842L;
		final RegDate dateNaissance = date(1975, 7, 31);
		final RegDate dateMariage = date(2005, 4, 12);
		final RegDate dateDebutRT = date(2003, 2, 1);
		final RegDate dateFinRT = date(2006, 6, 30);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, dateNaissance, "Labaffe", "Melchior", Sexe.MASCULIN);
				marieIndividu(ind, dateMariage);
			}
		});

		final class Ids {
			int ppHabitant;
			int ppNonHabitant;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final PersonnePhysique conjoint = addNonHabitant("Mariam", "Labaffe", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, conjoint, dateMariage, null);

				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.ChateauDoex);
				addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.ChateauDoex);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, pp, dateDebutRT, dateFinRT, false);

				assertValidInteger(pp.getNumero());
				assertValidInteger(conjoint.getNumero());
				final Ids ids = new Ids();
				ids.ppHabitant = pp.getNumero().intValue();
				ids.ppNonHabitant = conjoint.getNumero().intValue();
				return ids;
			}
		});

		final Parties parties = service.getParties(Arrays.asList(ids.ppHabitant, ids.ppNonHabitant), EnumSet.of(PartyPart.WITHHOLDING_TAXATION_PERIODS));
		Assert.assertNotNull(parties);
		Assert.assertNotNull(parties.getEntries());
		Assert.assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		Collections.sort(sorted, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				return o1.getPartyNo() - o2.getPartyNo();
			}
		});

		{
			final Entry entry = sorted.get(0);
			Assert.assertEquals(ids.ppHabitant, entry.getPartyNo());

			final Party party = entry.getParty();
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			Assert.assertNotNull(np.getWithholdingTaxationPeriods());
			Assert.assertEquals(4, np.getWithholdingTaxationPeriods().size());      // on vérifie juste qu'elles sont bien là... 2003 à 2006 = 4
		}
		{
			final Entry entry = sorted.get(1);
			Assert.assertEquals(ids.ppNonHabitant, entry.getPartyNo());

			final Party party = entry.getParty();
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			Assert.assertNotNull(np.getWithholdingTaxationPeriods());
			Assert.assertEquals(0, np.getWithholdingTaxationPeriods().size());      // elle n'a rien du tout
		}
	}

	/**
	 * SIFISC-11713: appel de /parties avec un tiers dont le numéro d'individu est inconnu au civil
	 */
	@Test
	public void testGetPartiesAvecIndividuInconnu() throws Exception {

		final long noIndividu = 427842L;
		final long noIndividuAbsent = 4538735674L;
		final RegDate dateNaissance = date(1975, 7, 31);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividu, dateNaissance, "Labaffe", "Melchior", Sexe.MASCULIN);
			}
		});

		final class Ids {
			int ppConnu;
			int ppInconnu;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique connu = addHabitant(noIndividu);
				final PersonnePhysique inconnu = addHabitant(noIndividuAbsent);
				final Ids ids = new Ids();
				ids.ppConnu = connu.getNumero().intValue();
				ids.ppInconnu = inconnu.getNumero().intValue();
				return ids;
			}
		});

		final Parties parties = service.getParties(Arrays.asList(ids.ppConnu, ids.ppInconnu), null);
		Assert.assertNotNull(parties);
		Assert.assertNotNull(parties.getEntries());
		Assert.assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		Collections.sort(sorted, new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				return o1.getPartyNo() - o2.getPartyNo();
			}
		});

		{
			final Entry entry = sorted.get(0);
			Assert.assertEquals(ids.ppConnu, entry.getPartyNo());

			final Party party = entry.getParty();
			Assert.assertNotNull(party);
			Assert.assertEquals(NaturalPerson.class, party.getClass());
		}
		{
			final Entry entry = sorted.get(1);
			Assert.assertEquals(ids.ppInconnu, entry.getPartyNo());

			final Party party = entry.getParty();
			Assert.assertNull(party);

			final Error error = entry.getError();
			Assert.assertEquals("Impossible de trouver l'individu n°" + noIndividuAbsent + " pour l'habitant n°" + ids.ppInconnu, error.getErrorMessage());
			Assert.assertEquals(ErrorType.BUSINESS, error.getType());
		}
	}

	/**
	 * SIFISC-11713: appel de /party avec un tiers dont le numéro d'individu est inconnu au civil
	 */
	@Test
	public void testGetPartyAvecIndividuInconnu() throws Exception {

		final long noIndividuAbsent = 4538735674L;

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// persone...
			}
		});

		// mise en place fiscale
		final int ppId = doInNewTransactionAndSession(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction(TransactionStatus status) {
				final PersonnePhysique inconnu = addHabitant(noIndividuAbsent);
				return inconnu.getNumero().intValue();
			}
		});

		try {
			final Party party = service.getParty(ppId, null);
			Assert.fail("Aurait dû partir en erreur...");
		}
		catch (IndividuNotFoundException e) {
			Assert.assertEquals("Impossible de trouver l'individu n°" + noIndividuAbsent + " pour l'habitant n°" + ppId, e.getMessage());
		}
	}

	/**
	 * [SIFISC-13461] ajout du flag "ACI autre canton" sur les débiteurs IS
	 */
	@Test
	public void testFlagAciAutreCantonSurDebiteurIS() throws Exception {

		final class Ids {
			int dpiAvecFlag;
			int dpiSansFlag;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final DebiteurPrestationImposable dpiSans = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				dpiSans.setAciAutreCanton(Boolean.FALSE);

				final DebiteurPrestationImposable dpiAvec = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
				dpiAvec.setAciAutreCanton(Boolean.TRUE);

				final Ids ids = new Ids();
				ids.dpiAvecFlag = dpiAvec.getNumero().intValue();
				ids.dpiSansFlag = dpiSans.getNumero().intValue();
				return ids;
			}
		});

		// interrogation

		// cas avec flag
		final Party avec = service.getParty(ids.dpiAvecFlag, null);
		Assert.assertNotNull(avec);
		Assert.assertEquals(Debtor.class, avec.getClass());
		final Debtor avecDebtor = (Debtor) avec;
		Assert.assertTrue(avecDebtor.isOtherCantonTaxAdministration());

		// cas sans flag
		final Party sans = service.getParty(ids.dpiSansFlag, null);
		Assert.assertNotNull(sans);
		Assert.assertEquals(Debtor.class, sans.getClass());
		final Debtor sansDebtor = (Debtor) sans;
		Assert.assertFalse(sansDebtor.isOtherCantonTaxAdministration());
	}

	@Test
	public void testGetFiscalEvents() throws Exception {

		final class Ids {
			int ppAvec;
			int ppSans;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique avec = addNonHabitant("Albert", "Pommery", date(1985, 3, 15), Sexe.MASCULIN);
				final ForFiscalPrincipal ffp = addForPrincipal(avec, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addEvenementFiscalFor(avec, ffp, date(2000, 1, 1), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);

				final PersonnePhysique sans = addNonHabitant("Bartholomé", "Mazo", date(1956, 8, 3), Sexe.MASCULIN);

				final Ids ids = new Ids();
				ids.ppAvec = avec.getNumero().intValue();
				ids.ppSans = sans.getNumero().intValue();
				return ids;
			}
		});

		// interrogation

		// sans événement fiscal
		final FiscalEvents sans = service.getFiscalEvents(ids.ppSans);
		Assert.assertNotNull(sans);
		Assert.assertNotNull(sans.getEvents());
		Assert.assertEquals(0, sans.getEvents().size());

		// avec un événement fiscal
		final FiscalEvents avec = service.getFiscalEvents(ids.ppAvec);
		Assert.assertNotNull(avec);
		Assert.assertNotNull(avec.getEvents());
		Assert.assertEquals(1, avec.getEvents().size());
		{
			final FiscalEvent evt = avec.getEvents().get(0);
			Assert.assertNotNull(evt);
			Assert.assertEquals(AuthenticationHelper.getCurrentPrincipal(), evt.getUser());
			Assert.assertNotNull(evt.getTreatmentDate());
			Assert.assertEquals("Ouverture d'un for principal pour motif 'Arrivée de hors-Suisse'", evt.getDescription());

			final EvenementFiscal xml = evt.getEvent();
			Assert.assertNotNull(xml);
			Assert.assertEquals(OuvertureFor.class, xml.getClass());
			Assert.assertEquals(date(2000, 1, 1), DataHelper.webToRegDate(xml.getDate()));
			Assert.assertEquals(CategorieTiers.PP, xml.getCategorieTiers());
			Assert.assertEquals(ids.ppAvec, xml.getNumeroTiers());

			final OuvertureFor ouverture = (OuvertureFor) xml;
			Assert.assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, ouverture.getMotifOuverture());
		}
	}

	@Test
	public void testLabels() throws Exception {

		final String codeCollaborateur = CODE_ETIQUETTE_COLLABORATEUR;
		final String codeToto = "TOTO";

		// mise en place des étiquettes manquantes (héritage et collaborateur sont mis en place par le AbstractBusinessTest...)
		final long idCollAdmNouvelleEntite = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				addEtiquette(codeToto, "Etiquette TOTO", TypeTiersEtiquette.PP_MC, null);

				final CollectiviteAdministrative nouvelleEntite = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.noNouvelleEntite);
				return nouvelleEntite.getNumero();
			}
		});

		final class Ids {
			int prn;
			int cjt;
			int mc;
		}

		// mise en place des individus avec leur liens d'étiquette
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique prn = addNonHabitant("Albert", "Mucas", null, Sexe.MASCULIN);
				final PersonnePhysique cjt = addNonHabitant("Françoise", "Mucas", null, Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(prn, cjt, date(2008, 1, 1), null);
				final MenageCommun menage = couple.getMenage();

				final Etiquette collaborateurs = etiquetteService.getEtiquette(codeCollaborateur);
				final Etiquette toto = etiquetteService.getEtiquette(codeToto);

				addEtiquetteTiers(collaborateurs, prn, date(2008, 5, 12), date(2010, 8, 31));
				addEtiquetteTiers(collaborateurs, prn, date(2015, 2, 1), null);
				addEtiquetteTiers(collaborateurs, cjt, date(2009, 6, 1), date(2014, 5, 30));

				addEtiquetteTiers(toto, cjt, date(2005, 3, 1), null);
				addEtiquetteTiers(toto, menage, date(2010, 1, 1), date(2010, 7, 15));

				final Ids ids = new Ids();
				ids.prn = prn.getNumero().intValue();
				ids.cjt = cjt.getNumero().intValue();
				ids.mc = menage.getNumero().intValue();
				return ids;
			}
		});

		// interrogation
		final Parties parties = service.getParties(Arrays.asList(ids.prn, ids.cjt, ids.mc), EnumSet.of(PartyPart.LABELS));
		final Map<Integer, Party> mapParties = parties.getEntries().stream()
				.collect(Collectors.toMap(Entry::getPartyNo, Entry::getParty));
		Assert.assertEquals(3, mapParties.size());

		// principal
		{
			final Party party = mapParties.get(ids.prn);
			Assert.assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			Assert.assertNotNull(labels);
			Assert.assertEquals(2, labels.size());
			{
				final PartyLabel label = labels.get(0);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2008, 5, 12), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertEquals(date(2010, 8, 31), DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeCollaborateur, label.getLabel());
				Assert.assertEquals("DS Collaborateur", label.getDisplayLabel());
				Assert.assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNotNull(ca);
				Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				Assert.assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
			{
				final PartyLabel label = labels.get(1);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2015, 2, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertNull(DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeCollaborateur, label.getLabel());
				Assert.assertEquals("DS Collaborateur", label.getDisplayLabel());
				Assert.assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNotNull(ca);
				Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				Assert.assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
		}

		// conjoint
		{
			final Party party = mapParties.get(ids.cjt);
			Assert.assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			Assert.assertNotNull(labels);
			Assert.assertEquals(2, labels.size());
			{
				final PartyLabel label = labels.get(0);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2005, 3, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertNull(DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeToto, label.getLabel());
				Assert.assertEquals("Etiquette TOTO", label.getDisplayLabel());
				Assert.assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(1);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2009, 6, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertEquals(date(2014, 5, 30), DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeCollaborateur, label.getLabel());
				Assert.assertEquals("DS Collaborateur", label.getDisplayLabel());
				Assert.assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNotNull(ca);
				Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				Assert.assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
		}

		// ménage
		{
			final Party party = mapParties.get(ids.mc);
			Assert.assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			Assert.assertNotNull(labels);
			Assert.assertEquals(5, labels.size());
			{
				final PartyLabel label = labels.get(0);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2005, 3, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertEquals(date(2009, 12, 31), DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeToto, label.getLabel());
				Assert.assertEquals("Etiquette TOTO", label.getDisplayLabel());
				Assert.assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(1);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2008, 5, 12), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertEquals(date(2014, 5, 30), DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeCollaborateur, label.getLabel());
				Assert.assertEquals("DS Collaborateur", label.getDisplayLabel());
				Assert.assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNotNull(ca);
				Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				Assert.assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
			{
				final PartyLabel label = labels.get(2);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2010, 1, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertEquals(date(2010, 7, 15), DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeToto, label.getLabel());
				Assert.assertEquals("Etiquette TOTO", label.getDisplayLabel());
				Assert.assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(3);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2010, 7, 16), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertNull(DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeToto, label.getLabel());
				Assert.assertEquals("Etiquette TOTO", label.getDisplayLabel());
				Assert.assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(4);
				Assert.assertNotNull(label);
				Assert.assertEquals(date(2015, 2, 1), DataHelper.webToRegDate(label.getDateFrom()));
				Assert.assertNull(DataHelper.webToRegDate(label.getDateTo()));
				Assert.assertEquals(codeCollaborateur, label.getLabel());
				Assert.assertEquals("DS Collaborateur", label.getDisplayLabel());
				Assert.assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				Assert.assertNotNull(ca);
				Assert.assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				Assert.assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
		}
	}

	/**
	 * [SIFISC-24999] Vérifie que les rapports-entre-tiers sont bien exposées dans le WS lorsqu'on les demande, mais que par défaut, cela <b>n'inclut pas</b> les parentés ni les relations d'héritage.
	 */
	@Test
	public void testPartyPartRelationsBetweenParties() throws Exception {

		final RegDate dateDeces = RegDate.get(2005, 1, 1);

		class Ids {
			Long decede;
			Long representant;
		}
		final Ids ids = new Ids();

		// on ajoute un tiers avec des rapports-entre-tiers
		doInNewTransaction(status -> {
			final PersonnePhysique decede = addNonHabitant("Rodolf", "Laplancha", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Gudule", "Laplancha", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			final PersonnePhysique heritier2 = addNonHabitant("Morissonnette", "Laplancha", RegDate.get(1990, 1, 1), Sexe.FEMININ);
			final PersonnePhysique representant = addNonHabitant("Rodolf", "Prou", RegDate.get(1990, 1, 1), Sexe.MASCULIN);
			addParente(heritier1, decede, RegDate.get(1980, 1, 1), null);
			addParente(heritier2, decede, RegDate.get(1990, 1, 1), null);
			addHeritage(heritier1, decede, dateDeces, null, true);
			addHeritage(heritier2, decede, dateDeces, null, false);
			addRepresentationConventionnelle(decede, representant, RegDate.get(2000, 1, 1), false);
			ids.decede = decede.getId();
			ids.representant = representant.getId();
			return null;
		});

		// on demande les rapports-entre-tiers
		final Party party = service.getParty(ids.decede.intValue(), Collections.singleton(PartyPart.RELATIONS_BETWEEN_PARTIES));
		Assert.assertNotNull(party);

		// on doit bien recevoir tous les rapports-entre-tiers *sauf* les parentés et les relations d'héritages
		// qui - pour des raisons de comptabilité ascendante - ne sont exposés que sur demande explicite.
		final List<RelationBetweenParties> relations = party.getRelationsBetweenParties();
		Assert.assertNotNull(relations);
		Assert.assertEquals(1, relations.size());
		assertRepresentative(ids.representant.intValue(), RegDate.get(2000, 1, 1), null, false, relations.get(0));
	}

	/**
	 * [SIFISC-24999] Vérifie que les relations d'héritage sont bien exposées dans le WS lorsqu'on les demande.
	 */
	@Test
	public void testPartyPartInheritanceRelationships() throws Exception {

		final RegDate dateDeces = RegDate.get(2005, 1, 1);

		class Ids {
			Long decede;
			Long heritier1;
			Long heritier2;
		}
		final Ids ids = new Ids();

		// on ajoute un tiers avec des relations d'héritage
		doInNewTransaction(status -> {
			final PersonnePhysique decede = addNonHabitant("Rodolf", "Laplancha", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Gudule", "Laplancha", RegDate.get(1980, 1, 1), Sexe.FEMININ);
			final PersonnePhysique heritier2 = addNonHabitant("Morissonnette", "Laplancha", RegDate.get(1990, 1, 1), Sexe.FEMININ);
			addParente(heritier1, decede, RegDate.get(1980, 1, 1), null);
			addParente(heritier2, decede, RegDate.get(1990, 1, 1), null);
			addHeritage(heritier1, decede, dateDeces, null, true);
			addHeritage(heritier2, decede, dateDeces, null, false);
			ids.decede = decede.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			return null;
		});

		// on demande les relations d'héritage : on ne reçoit qu'elles
		final Party party = service.getParty(ids.decede.intValue(), Collections.singleton(PartyPart.INHERITANCE_RELATIONSHIPS));
		Assert.assertNotNull(party);

		final List<RelationBetweenParties> relations = party.getRelationsBetweenParties();
		Assert.assertNotNull(relations);
		Assert.assertEquals(2, relations.size());
		relations.sort(Comparator.comparing(RelationBetweenParties::getOtherPartyNumber));
		assertInheritanceTo(ids.heritier1.intValue(), dateDeces, null, true, relations.get(0));
		assertInheritanceTo(ids.heritier2.intValue(), dateDeces, null, false, relations.get(1));
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que le WS de récupération d'un immeuble fonctionne bien dans le cas passant.
	 */
	@Test
	public void testGetImmovableProperty() throws Exception {

		// on ajoute un immeuble dans la base
		final Long id = doInNewTransaction(status -> {
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			return immeuble.getId();
		});

		final ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty immo = service.getImmovableProperty(id);
		Assert.assertNotNull(immo);
		Assert.assertTrue(immo instanceof RealEstate);

		final RealEstate realEstate = (RealEstate) immo;
		Assert.assertEquals(id.longValue(), realEstate.getId());
		Assert.assertEquals("some egrid", realEstate.getEgrid());
		Assert.assertNull(realEstate.getCancellationDate());

		final List<Location> locations = realEstate.getLocations();
		Assert.assertEquals(1, locations.size());
		assertLocation(RegDate.get(2000, 1, 1), null, 579, null, null, null, 5498, locations.get(0));
	}

	/**
	 * Ce test vérifie que le WS de récupération d'un immeuble fonctionne bien dans le cas passant.
	 */
	@Test
	public void testGetImmovableProperties() throws Exception {

		// on ajoute deux immeubles dans la base
		final List<Long> ids = doInNewTransaction(status -> {
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble1 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BienFondsRF immeuble2 = addBienFondsRF("covfefe", "other egrid", laSarraz, 580);
			return Arrays.asList(immeuble1.getId(), immeuble2.getId());
		});

		// on demande trois immeubles : deux existants et un inconnu
		final long idInexistant = -1;
		final ImmovablePropertyList immovableProperties = service.getImmovableProperties(Arrays.asList(ids.get(0), ids.get(1), idInexistant));

		// on vérifie qu'on reçoit bien trois réponses
		final List<ImmovablePropertyEntry> entries = immovableProperties.getEntries();
		Assert.assertNotNull(entries);
		Assert.assertEquals(3, entries.size());
		assertNotFoundEntry(idInexistant, entries.get(0));
		assertFoundEntry(ids.get(0), entries.get(1));
		assertFoundEntry(ids.get(1), entries.get(2));
	}


	@Test
	public void testGetBuilding() throws Exception {

		class Ids {
			long immeuble;
			long batiment;

			public Ids(long immeuble, long batiment) {
				this.immeuble = immeuble;
				this.batiment = batiment;
			}
		}

		// on ajoute un immeuble dans la base
		final Ids ids = doInNewTransaction(status -> {
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			final BatimentRF batiment = addBatimentRF("483838ace8e8");
			addDescriptionBatimentRF(RegDate.get(2000, 1, 1), null, "Centrale électrique", 300, batiment);
			addImplantationRF(RegDate.get(2000, 1, 1), null, 310, immeuble, batiment);
			return new Ids(immeuble.getId(), batiment.getId());
		});

		final Building building = service.getBuilding(ids.batiment);
		Assert.assertNotNull(building);
		Assert.assertEquals(ids.batiment, building.getId());

		final List<BuildingDescription> descriptions = building.getDescriptions();
		Assert.assertEquals(1, descriptions.size());
		final BuildingDescription description0 = descriptions.get(0);
		Assert.assertEquals(RegDate.get(2000, 1, 1), DataHelper.webToRegDate(description0.getDateFrom()));
		Assert.assertNull(description0.getDateTo());
		Assert.assertEquals("Centrale électrique", description0.getType());
		Assert.assertEquals(Integer.valueOf(300), description0.getArea());

		final List<BuildingSetting> settings = building.getSettings();
		Assert.assertEquals(1, settings.size());
		final BuildingSetting setting0 = settings.get(0);
		Assert.assertEquals(RegDate.get(2000, 1, 1), DataHelper.webToRegDate(setting0.getDateFrom()));
		Assert.assertNull(setting0.getDateTo());
		Assert.assertEquals(Integer.valueOf(310), setting0.getArea());
	}

	@Test
	public void testGetBuildings() throws Exception {

		// on ajoute deux bâtiments dans la base
		final List<Long> ids = doInNewTransaction(status -> {
			final BatimentRF batiment1 = addBatimentRF("483838ace8e8");
			final BatimentRF batiment2 = addBatimentRF("473727217111");
			return Arrays.asList(batiment1.getId(), batiment2.getId());
		});

		// on demande trois bâtiments : deux existants et un inconnu
		final long idInexistant = -1;
		final BuildingList buildings = service.getBuildings(Arrays.asList(ids.get(0), ids.get(1), idInexistant));

		// on vérifie qu'on reçoit bien trois réponses
		final List<BuildingEntry> entries = buildings.getEntries();
		Assert.assertNotNull(entries);
		Assert.assertEquals(3, entries.size());
		assertNotFoundEntry(idInexistant, entries.get(0));
		assertFoundEntry(ids.get(0), entries.get(1));
		assertFoundEntry(ids.get(1), entries.get(2));
	}

	/**
	 * Ce test vérifie que la méthode 'getCommunityOfOwners' fonctionne bien dans le cas passant.
	 */
	@Test
	public void testGetCommunityOfOwners() throws Exception {

		class Ids {
			long pp;
			long immeuble;
			long communaute;
		}
		final Ids ids = new Ids();

		// on crée un immeuble possédé par une communauté dans la base
		doInNewTransaction(status -> {

			// un immeuble
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			ids.immeuble = immeuble.getId();

			// deux tiers RF et une communauté
			final PersonnePhysiqueRF ericRF = addPersonnePhysiqueRF("38383830ae3ff", "Eric", "Bolomey", RegDate.get(1966,3,30));
			final PersonnePhysiqueRF attilaRF = addPersonnePhysiqueRF("828e8a828", "Attila", "Misère", RegDate.get(2002,12,22));
			final CommunauteRF communauteRF = addCommunauteRF("78282828", TypeCommunaute.COMMUNAUTE_DE_BIENS);
			ids.communaute = communauteRF.getId();

			// Les deux tiers RF possèdent l'immeuble à travers une communauté de biens
			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 2004, 202, 3);
			addDroitPropriete(ericRF, immeuble, communauteRF, GenrePropriete.COMMUNE, new Fraction(1, 2), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "48390a0e044", "48390a0e043");
			addDroitPropriete(attilaRF, immeuble, communauteRF, GenrePropriete.COMMUNE, new Fraction(1, 2), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "a88e883c73", "a88e883c72");
			addDroitPropriete(communauteRF, immeuble, GenrePropriete.INDIVIDUELLE, new Fraction(1, 1), RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "2890cc033a", "2890cc033b");

			// le tiers Unireg rapproché
			final PersonnePhysique pp = addNonHabitant("Eric", "Bolomey", RegDate.get(1966, 3, 30), Sexe.MASCULIN);
			ids.pp = pp.getId();
			addRapprochementRF(pp, ericRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);

			return null;

		});

		// on fait l'appel
		final CommunityOfOwners community = service.getCommunityOfOwners(ids.communaute);
		Assert.assertNotNull(community);
		Assert.assertEquals(ids.communaute, community.getId());
		Assert.assertEquals(CommunityOfOwnersType.COMMUNITY_OF_PROPERTY, community.getType());

		final List<RightHolder> members = community.getMembers();
		Assert.assertEquals(2, members.size());
		assertOwnerParty(ids.pp, members.get(0));
		assertOwnerNaturalPerson("Attila", "Misère", RegDate.get(2002, 12, 22), members.get(1));
	}

	/**
	 * Ce test vérifie que la méthode 'getCommunitiesOfOwners' fonctionne bien dans le cas passant.
	 */
	@Test
	public void testGetCommunitiesOfOwners() throws Exception {

		class Ids {
			long pp;
			long immeuble;
			long communaute1;
			long communaute2;
		}
		final Ids ids = new Ids();

		// on crée un immeuble possédé par deux communauté dans la base
		doInNewTransaction(status -> {

			// un immeuble
			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);
			ids.immeuble = immeuble.getId();

			// trois tiers RF
			final PersonnePhysiqueRF ericRF = addPersonnePhysiqueRF("38383830ae3ff", "Eric", "Bolomey", RegDate.get(1966,3,30));
			final PersonnePhysiqueRF attilaRF = addPersonnePhysiqueRF("828e8a828", "Attila", "Misère", RegDate.get(2002,12,22));
			final PersonnePhysiqueRF gudrunRF = addPersonnePhysiqueRF("0ea0e020", "Gudrun", "Chaud", RegDate.get(1996,2,4));

			// deux communautés
			final CommunauteRF communauteRF1 = addCommunauteRF("78282828", TypeCommunaute.COMMUNAUTE_DE_BIENS);
			final CommunauteRF communauteRF2 = addCommunauteRF("20826216", TypeCommunaute.COMMUNAUTE_HEREDITAIRE);
			ids.communaute1 = communauteRF1.getId();
			ids.communaute2 = communauteRF2.getId();

			// Les deux tiers RF possèdent l'immeuble à travers une communauté de biens
			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 2004, 202, 3);
			addDroitPropriete(ericRF, immeuble, communauteRF1, GenrePropriete.COMMUNE, new Fraction(1, 3), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "48390a0e044", "48390a0e043");
			addDroitPropriete(attilaRF, immeuble, communauteRF1, GenrePropriete.COMMUNE, new Fraction(1, 3), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "a88e883c73", "a88e883c72");
			addDroitPropriete(communauteRF1, immeuble, GenrePropriete.COPROPRIETE, new Fraction(1, 3), RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, "Achat", null, numeroAffaire, "2890cc033a", "2890cc033b");

			addDroitPropriete(ericRF, immeuble, communauteRF2, GenrePropriete.COMMUNE, new Fraction(2, 3), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Succession", null, numeroAffaire, "7833737", "47838282");
			addDroitPropriete(gudrunRF, immeuble, communauteRF2, GenrePropriete.COMMUNE, new Fraction(2, 3), RegDate.get(2004, 5, 21), null, RegDate.get(2004, 4, 12), null, "Succession", null, numeroAffaire, "739237329", "34727222");
			addDroitPropriete(communauteRF2, immeuble, GenrePropriete.COPROPRIETE, new Fraction(2, 3), RegDate.get(2004, 5, 21), RegDate.get(2004, 4, 12), null, "Succession", null, numeroAffaire, "4782372172", "9033900");

			// le tiers Unireg rapproché
			final PersonnePhysique pp = addNonHabitant("Eric", "Bolomey", RegDate.get(1966, 3, 30), Sexe.MASCULIN);
			ids.pp = pp.getId();
			addRapprochementRF(pp, ericRF, RegDate.get(2000, 1, 1), null, TypeRapprochementRF.MANUEL);

			return null;

		});

		// on demande trois communautés : deux existantes et une inconnue
		final Long idCommunauteInconnue = -1L;
		final CommunityOfOwnersList list = service.getCommunitiesOfOwners(Arrays.asList(ids.communaute1, ids.communaute2, idCommunauteInconnue));
		Assert.assertNotNull(list);

		// on vérifie qu'on reçoit bien trois réponses
		final List<CommunityOfOwnersEntry> entries = list.getEntries();
		Assert.assertNotNull(entries);
		Assert.assertEquals(3, entries.size());
		assertNotFoundEntry(idCommunauteInconnue, entries.get(0));
		assertFoundEntry(ids.communaute1, entries.get(1));
		assertFoundEntry(ids.communaute2, entries.get(2));
	}

	/**
	 * Ce test vérifie que la méthode 'getCommunitiesOfHeirs' fonctionne bien dans le cas passant.
	 */
	@Test
	public void testGetCommunityOfHeirs() throws Exception {

		class Ids {
			long defunt;
			long heritier1;
			long heritier2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			final PersonnePhysique defunt = addNonHabitant("Jean", "Peuplus", RegDate.get(1920, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier1 = addNonHabitant("Gemme", "Réjouï", RegDate.get(1990, 1, 1), Sexe.MASCULIN);
			final PersonnePhysique heritier2 = addNonHabitant("Carla", "Parselaibel", RegDate.get(1990, 1, 1), Sexe.FEMININ);
			addHeritage(heritier1, defunt, RegDate.get(2016, 5, 13), null, true);
			addHeritage(heritier2, defunt, RegDate.get(2016, 5, 15), null, false);

			ids.defunt = defunt.getId();
			ids.heritier1 = heritier1.getId();
			ids.heritier2 = heritier2.getId();
			return null;
		});

		// on demande deux communautés : une existante et une inconnue
		Assert.assertNull(service.getCommunityOfHeirs(-1));

		// on vérifie qu'on reçoit bien trois réponses
		final CommunityOfHeirs community = service.getCommunityOfHeirs((int) ids.defunt);
		Assert.assertNotNull(community);
		Assert.assertEquals(ids.defunt, community.getInheritedFromNumber());
		Assert.assertEquals(RegDate.get(2016, 5, 13), DataHelper.webToRegDate(community.getInheritanceDateFrom()));

		final List<CommunityOfHeirMember> members = community.getMembers();
		Assert.assertNotNull(members);
		Assert.assertEquals(2, members.size());
		assertMember(ids.heritier1, RegDate.get(2016, 5, 13), null, null, members.get(0));
		assertMember(ids.heritier2, RegDate.get(2016, 5, 15), null, null, members.get(1));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		Assert.assertNotNull(leaders);
		Assert.assertEquals(1, leaders.size());
		assertLeader(ids.heritier1, RegDate.get(2016, 5, 13), null, null, leaders.get(0));
	}

	private static void assertOwnerNaturalPerson(String firstName, String lastName, RegDate dateOfBirth, RightHolder owner) {
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		Assert.assertEquals(firstName, identity.getFirstName());
		Assert.assertEquals(lastName, identity.getLastName());
		Assert.assertEquals(dateOfBirth, DataHelper.webToRegDate(identity.getDateOfBirth()));
	}

	private static void assertOwnerParty(long id, RightHolder rightHolder) {
		Assert.assertEquals(Integer.valueOf((int) id), rightHolder.getTaxPayerNumber());
	}

	private void assertShare(int numerator, int denominator, Share share) {
		Assert.assertNotNull(share);
		Assert.assertEquals(numerator, share.getNumerator());
		Assert.assertEquals(denominator, share.getDenominator());
	}

	private void assertCaseIdentifier(int officeNumber, String caseNumber, CaseIdentifier caseIdentifier) {
		Assert.assertNotNull(caseIdentifier);
		Assert.assertEquals(officeNumber, caseIdentifier.getOfficeNumber());
		Assert.assertEquals(caseNumber, caseIdentifier.getCaseNumberText());
	}

	private static void assertLocation(RegDate dateFrom, RegDate dateTo, int parcelNumber, Integer index1, Integer index2, Integer index3, int noOfsCommune, Location location) {
		Assert.assertNotNull(location);
		Assert.assertEquals(dateFrom, DataHelper.webToRegDate(location.getDateFrom()));
		Assert.assertEquals(dateTo, DataHelper.webToRegDate(location.getDateTo()));
		Assert.assertEquals(parcelNumber, location.getParcelNumber());
		Assert.assertEquals(index1, location.getIndex1());
		Assert.assertEquals(index2, location.getIndex2());
		Assert.assertEquals(index3, location.getIndex3());
		Assert.assertEquals(noOfsCommune, location.getMunicipalityFsoId());
	}

	public static void assertFoundEntry(long immoId, ImmovablePropertyEntry entry) {
		Assert.assertEquals(immoId, entry.getImmovablePropertyId());
		final ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty immo = entry.getImmovableProperty();
		Assert.assertNotNull(immo);
		Assert.assertEquals(immoId, immo.getId());
		Assert.assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long immoId, ImmovablePropertyEntry entry) {
		Assert.assertEquals(immoId, entry.getImmovablePropertyId());
		Assert.assertNull(entry.getImmovableProperty());
		Assert.assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		Assert.assertEquals("L'immeuble n°[" + immoId + "] n'existe pas.", entry.getError().getErrorMessage());
	}

	public static void assertFoundEntry(long buildingId, BuildingEntry entry) {
		Assert.assertEquals(buildingId, entry.getBuildingId());
		final Building building = entry.getBuilding();
		Assert.assertNotNull(building);
		Assert.assertEquals(buildingId, building.getId());
		Assert.assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long buildingId, BuildingEntry entry) {
		Assert.assertEquals(buildingId, entry.getBuildingId());
		Assert.assertNull(entry.getBuilding());
		Assert.assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		Assert.assertEquals("Le bâtiment n°[" + buildingId + "] n'existe pas.", entry.getError().getErrorMessage());
	}

	public static void assertFoundEntry(long communityId, CommunityOfOwnersEntry entry) {
		Assert.assertEquals(communityId, entry.getCommunityOfOwnersId());
		final CommunityOfOwners community = entry.getCommunityOfOwners();
		Assert.assertNotNull(community);
		Assert.assertEquals(communityId, community.getId());
		Assert.assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long communityId, CommunityOfOwnersEntry entry) {
		Assert.assertEquals(communityId, entry.getCommunityOfOwnersId());
		Assert.assertNull(entry.getCommunityOfOwners());
		Assert.assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		Assert.assertEquals("La communauté n°[" + communityId + "] n'existe pas.", entry.getError().getErrorMessage());
	}

	private static void assertRepresentative(int id, RegDate dateFrom, RegDate dateTo, boolean forced, RelationBetweenParties relation) {
		assertTrue(relation instanceof Representative);
		final Representative representative = (Representative) relation;
		assertEquals(id, representative.getOtherPartyNumber());
		assertEquals(dateFrom, ch.vd.unireg.xml.DataHelper.xmlToCore(representative.getDateFrom()));
		assertEquals(dateTo, ch.vd.unireg.xml.DataHelper.xmlToCore(representative.getDateTo()));
		assertEquals(forced, representative.isExtensionToForcedExecution());
	}

	private static void assertMember(long taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirMember member) {
		assertEquals(taxPayerNumber, member.getTaxPayerNumber());
		assertEquals(dateFrom, ch.vd.unireg.xml.DataHelper.xmlToCore(member.getDateFrom()));
		assertEquals(dateTo, ch.vd.unireg.xml.DataHelper.xmlToCore(member.getDateTo()));
		assertEquals(cancellationDate, ch.vd.unireg.xml.DataHelper.xmlToCore(member.getCancellationDate()));
	}

	private static void assertLeader(long taxPayerNumber, RegDate dateFrom, RegDate dateTo, RegDate cancellationDate, CommunityOfHeirLeader leader) {
		assertEquals(taxPayerNumber, leader.getTaxPayerNumber());
		assertEquals(dateFrom, ch.vd.unireg.xml.DataHelper.xmlToCore(leader.getDateFrom()));
		assertEquals(dateTo, ch.vd.unireg.xml.DataHelper.xmlToCore(leader.getDateTo()));
		assertEquals(cancellationDate, ch.vd.unireg.xml.DataHelper.xmlToCore(leader.getCancellationDate()));
	}
}