package ch.vd.unireg.webservices.v7;

import java.math.BigDecimal;
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
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

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
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DeclarationImpotSource;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationEmise;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.ModeleDocument;
import ch.vd.unireg.declaration.PeriodeFiscale;
import ch.vd.unireg.declaration.PeriodeFiscaleDAO;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.declaration.ordinaire.DeclarationImpotService;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.efacture.EFactureServiceProxy;
import ch.vd.unireg.efacture.MockEFactureService;
import ch.vd.unireg.etiquette.Etiquette;
import ch.vd.unireg.etiquette.EtiquetteService;
import ch.vd.unireg.evenement.declaration.DemandeDelaisDeclarationsHandler;
import ch.vd.unireg.evenement.fiscal.EvenementFiscalFor;
import ch.vd.unireg.foncier.DonneesUtilisation;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.efacture.data.TypeEtatDestinataire;
import ch.vd.unireg.interfaces.entreprise.data.FormeLegale;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.entreprise.mock.data.MockEntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.mock.data.builder.MockEntrepriseFactory;
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
import ch.vd.unireg.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.unireg.metier.assujettissement.PeriodeImpositionService;
import ch.vd.unireg.metier.bouclement.BouclementService;
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
import ch.vd.unireg.type.TypeAdresseTiers;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeDroitAcces;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;
import ch.vd.unireg.type.TypeFlagEntreprise;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.type.TypeTiersEtiquette;
import ch.vd.unireg.validation.ValidationService;
import ch.vd.unireg.ws.ack.v7.AckStatus;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckRequest;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResponse;
import ch.vd.unireg.ws.ack.v7.OrdinaryTaxDeclarationAckResult;
import ch.vd.unireg.ws.deadline.v7.DeadlineRequest;
import ch.vd.unireg.ws.deadline.v7.DeadlineResponse;
import ch.vd.unireg.ws.deadline.v7.DeadlineStatus;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvent;
import ch.vd.unireg.ws.fiscalevents.v7.FiscalEvents;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationRequest;
import ch.vd.unireg.ws.groupdeadline.v7.GroupDeadlineValidationResponse;
import ch.vd.unireg.ws.groupdeadline.v7.RejectionReason;
import ch.vd.unireg.ws.groupdeadline.v7.TaxDeclarationInfo;
import ch.vd.unireg.ws.groupdeadline.v7.ValidationResult;
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
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.landtaxlightening.v1.RealLandTaxLightening;
import ch.vd.unireg.xml.party.landtaxlightening.v1.VirtualLandTaxLightening;
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
import ch.vd.unireg.xml.party.taxresidence.v4.OperatingPeriod;
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
import ch.vd.unireg.xml.party.v5.InternalPartyPart;
import ch.vd.unireg.xml.party.v5.NaturalPersonSubtype;
import ch.vd.unireg.xml.party.v5.Party;
import ch.vd.unireg.xml.party.v5.PartyInfo;
import ch.vd.unireg.xml.party.v5.PartyLabel;
import ch.vd.unireg.xml.party.v5.PartyType;
import ch.vd.unireg.xml.party.withholding.v1.CommunicationMode;
import ch.vd.unireg.xml.party.withholding.v1.DebtorCategory;
import ch.vd.unireg.xml.party.withholding.v1.DebtorInfo;
import ch.vd.unireg.xml.party.withholding.v1.DebtorPeriodicity;
import ch.vd.unireg.xml.party.withholding.v1.WithholdingTaxDeclarationPeriodicity;

import static ch.vd.unireg.xml.party.v5.strategy.NaturalPersonStrategyTest.assertInheritanceTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("Duplicates")
public class BusinessWebServiceTest extends WebserviceTest {

	private BusinessWebService service;
	private EFactureServiceProxy efactureService;
	private EtiquetteService etiquetteService;
	private ValidationService validationService;
	private PeriodeImpositionService periodeImpositionService;
	private BouclementService bouclementService;
	private PeriodeFiscaleDAO periodeFiscaleDAO;
	private DeclarationImpotService diService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		service = getBean(BusinessWebService.class, "wsv7Business");
		efactureService = getBean(EFactureServiceProxy.class, "efactureService");
		etiquetteService = getBean(EtiquetteService.class, "etiquetteService");
		validationService = getBean(ValidationService.class, "validationService");
		periodeImpositionService = getBean(PeriodeImpositionService.class, "periodeImpositionService");
		bouclementService = getBean(BouclementService.class, "bouclementService");
		periodeFiscaleDAO = getBean(PeriodeFiscaleDAO.class, "periodeFiscaleDAO");
		diService = getBean(DeclarationImpotService.class, "diService");

		serviceInfra.setUp(new DefaultMockServiceInfrastructureService() {
			@Override
			public String getUrl(ApplicationFiscale application, @Nullable Map<String, String> parametres) {
				assertNull(parametres);
				return "https://secure.vd.ch/territoire/intercapi/faces?bfs={noCommune}&kr=0&n1={noParcelle}&n2={index1}&n3={index2}&n4={index3}&type=grundstueck_grundbuch_auszug";
			}
		});
	}

	private static void assertValidInteger(long value) {
		assertTrue(Long.toString(value), value <= Integer.MAX_VALUE && value >= Integer.MIN_VALUE);
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
		final long ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Francis", "Noire", date(1965, 8, 31), Sexe.MASCULIN);
			pp.setBlocageRemboursementAutomatique(false);
			return pp.getNumero();
		});
		assertValidInteger(ppId);

		// vérification du point de départ
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});

		assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));

		// appel du WS (= sans changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, false);

		// vérification
		assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, true);

		// vérification
		assertTrue(service.getAutomaticRepaymentBlockingFlag((int) ppId));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertTrue(pp.getBlocageRemboursementAutomatique());
			}
		});

		// appel du WS (= avec changement)
		service.setAutomaticRepaymentBlockingFlag((int) ppId, false);

		// vérification
		assertFalse(service.getAutomaticRepaymentBlockingFlag((int) ppId));
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);
				assertFalse(pp.getBlocageRemboursementAutomatique());
			}
		});
	}

	private void assertAllowedAccess(String visa, int partyNo, AllowedAccess expectedAccess) {
		final SecurityResponse access = service.getSecurityOnParty(visa, partyNo);
		assertNotNull(access);
		assertEquals(visa, access.getUser());
		assertEquals(partyNo, access.getPartyNo());
		assertEquals(expectedAccess, access.getAllowedAccess());
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique protege = addNonHabitant("Jürg", "Bunker", date(1975, 4, 23), Sexe.MASCULIN);
			final PersonnePhysique conjointDeProtege = addNonHabitant("Adelheid", "Bunker", date(1974, 7, 31), Sexe.FEMININ);
			final EnsembleTiersCouple coupleProtege = addEnsembleTiersCouple(protege, conjointDeProtege, date(2010, 6, 3), null);
			addDroitAcces(visaActeur, protege, TypeDroitAcces.AUTORISATION, Niveau.ECRITURE, date(2010, 1, 1), null);
			addDroitAcces(visaVoyeur, protege, TypeDroitAcces.AUTORISATION, Niveau.LECTURE, date(2010, 1, 1), null);

			final PersonnePhysique normal = addNonHabitant("Emile", "Gardavou", date(1962, 7, 4), Sexe.MASCULIN);
			final EnsembleTiersCouple coupleNormal = addEnsembleTiersCouple(normal, null, date(1987, 6, 5), null);

			final Ids ids1 = new Ids();
			ids1.idProtege = protege.getNumero().intValue();
			ids1.idConjointDeProtege = conjointDeProtege.getNumero().intValue();
			ids1.idCoupleProtege = coupleProtege.getMenage().getNumero().intValue();
			ids1.idNormal = normal.getNumero().intValue();
			ids1.idCoupleNormal = coupleNormal.getMenage().getNumero().intValue();
			return ids1;
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
			assertEquals(visaOmnipotent, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
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
			assertEquals(visaActeur, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
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
			assertEquals(visaVoyeur, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			assertEquals(AllowedAccess.READ_ONLY, accessesMap.get(ids.idProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			assertEquals(AllowedAccess.READ_ONLY, accessesMap.get(ids.idCoupleProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
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
			assertEquals(visaGrouillot, response.getUser());
			final Map<Integer, AllowedAccess> accessesMap = response.getPartyAccesses().stream()
					.collect(Collectors.toMap(PartyAccess::getPartyNo, PartyAccess::getAllowedAccess));
			assertEquals(AllowedAccess.NONE, accessesMap.get(ids.idProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idConjointDeProtege));
			assertEquals(AllowedAccess.NONE, accessesMap.get(ids.idCoupleProtege));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idNormal));
			assertEquals(AllowedAccess.READ_WRITE, accessesMap.get(ids.idCoupleNormal));
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
		final Data data = doInNewTransactionAndSession(status -> {
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

			final Data data1 = new Data();
			data1.pp1 = pp1.getNumero();
			data1.pp2 = pp2.getNumero();
			return data1;
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
		assertNotNull(resp);

		// vérification des codes retour
		final List<OrdinaryTaxDeclarationAckResult> result = resp.getAckResult();
		assertNotNull(result);
		assertEquals(keys.size(), result.size());
		for (OrdinaryTaxDeclarationAckResult ack : result) {
			assertNotNull(ack);

			final AckStatus expectedStatus = expected.get(ack.getDeclaration());
			assertNotNull(ack.toString(), expectedStatus);
			assertEquals(ack.toString(), expectedStatus, ack.getStatus());
		}

		// vérification en base
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				for (Map.Entry<TaxDeclarationKey, AckStatus> entry : expected.entrySet()) {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(entry.getKey().getTaxpayerNumber(), false);
					final List<Declaration> decls = pp.getDeclarationsDansPeriode(Declaration.class, annee, false);
					assertNotNull(decls);
					assertEquals(1, decls.size());

					final Declaration decl = decls.get(0);
					assertNotNull(decl);

					final EtatDeclaration etat = decl.getDernierEtatDeclaration();
					if (entry.getValue() == AckStatus.OK) {
						assertEquals(TypeEtatDocumentFiscal.RETOURNE, etat.getEtat());
					}
					else {
						assertEquals(TypeEtatDocumentFiscal.EMIS, etat.getEtat());
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
		final long ppId = doInNewTransactionAndSession(status -> {
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
		});
		assertValidInteger(ppId);

		// vérification du délai existant
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
				assertNotNull(delai);
				assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});

		// demande de délai qui échoue (délai plus ancien)
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(delaiInitial.addMonths(-2)), DataHelper.coreToWeb(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, req);
			assertNotNull(resp);
			assertEquals(DeadlineStatus.ERROR_INVALID_DEADLINE, resp.getStatus());
		}

		// vérification du délai qui ne devrait pas avoir bougé
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
				assertNotNull(delai);
				assertEquals(delaiInitial, delai.getDelaiAccordeAu());
			}
		});


		// demande de délai qui marche
		final RegDate nouveauDelai = RegDateHelper.maximum(delaiInitial.addMonths(1), RegDate.get(), NullDateBehavior.LATEST);
		{
			final DeadlineRequest req = new DeadlineRequest(DataHelper.coreToWeb(nouveauDelai), DataHelper.coreToWeb(RegDate.get()));
			final DeadlineResponse resp = service.newOrdinaryTaxDeclarationDeadline((int) ppId, annee, 1, req);
			assertNotNull(resp);
			assertEquals(resp.getAdditionalMessage(), DeadlineStatus.OK, resp.getStatus());
		}

		// vérification du nouveau délai
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertNotNull(pp);

				final Declaration di = pp.getDeclarationActiveAt(date(annee, 1, 1));
				assertNotNull(di);

				final DelaiDeclaration delai = di.getDernierDelaiDeclarationAccorde();
				assertNotNull(delai);
				assertEquals(nouveauDelai, delai.getDelaiAccordeAu());
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final CollectiviteAdministrative pays = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PAYS_D_ENHAUT.getNoColAdm());
			final CollectiviteAdministrative vevey = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_VEVEY.getNoColAdm());

			final Ids ids1 = new Ids();
			ids1.idVevey = vevey.getNumero();
			ids1.idPaysDEnhaut = pays.getNumero();
			return ids1;
		});

		// une commune vaudoise
		{
			final TaxOffices taxOffices = service.getTaxOffices(MockCommune.ChateauDoex.getNoOFS(), null);
			assertNotNull(taxOffices);
			assertNotNull(taxOffices.getDistrict());
			assertNotNull(taxOffices.getRegion());
			assertEquals(ids.idPaysDEnhaut, taxOffices.getDistrict().getPartyNo());
			assertEquals(MockOfficeImpot.OID_PAYS_D_ENHAUT.getNoColAdm(), taxOffices.getDistrict().getAdmCollNo());
			assertEquals(ids.idVevey, taxOffices.getRegion().getPartyNo());
			assertEquals(MockOfficeImpot.OID_VEVEY.getNoColAdm(), taxOffices.getRegion().getAdmCollNo());
		}

		// une commune hors-canton
		try {
			service.getTaxOffices(MockCommune.Bern.getNoOFS(), null);
			fail();
		}
		catch (ObjectNotFoundException e) {
			assertEquals(String.format("Commune %d inconnue dans le canton de Vaud.", MockCommune.Bern.getNoOFS()), e.getMessage());
		}

		// une commune inconnue
		try {
			service.getTaxOffices(99999, null);
			fail();
		}
		catch (ObjectNotFoundException e) {
			assertEquals("Commune 99999 inconnue dans le canton de Vaud.", e.getMessage());
		}
	}

	@Test
	public void testGetModifiedTaxPayers() throws Exception {

		final Pair<Long, Date> pp1 = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Philippe", "Lemol", date(1956, 9, 30), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			return Pair.of(pp.getNumero(), pp.getLogModifDate());
		});

		Thread.sleep(1000);
		final Pair<Long, Date> pp2 = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addNonHabitant("Albert", "Duchmol", date(1941, 5, 4), Sexe.MASCULIN);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Lausanne);
			return Pair.of(pp.getNumero(), pp.getLogModifDate());
		});

		final Date start = new Date(pp1.getRight().getTime() - 100);
		final Date middle = new Date(pp2.getRight().getTime() - 100);
		assertTrue(pp1.getRight().before(middle));
		final Date end = new Date(pp2.getRight().getTime() + 100);
		final Date now = new Date(pp2.getRight().getTime() + 200);

		// rien
		final PartyNumberList none = service.getModifiedTaxPayers(end, now);
		assertNotNull(none);
		assertNotNull(none.getPartyNo());
		assertEquals(0, none.getPartyNo().size());

		// 1 contribuable
		final PartyNumberList one = service.getModifiedTaxPayers(middle, now);
		assertNotNull(one);
		assertNotNull(one.getPartyNo());
		assertEquals(1, one.getPartyNo().size());
		assertEquals(pp2.getLeft().longValue(), one.getPartyNo().get(0).longValue());

		// 2 contribuables
		final PartyNumberList two = service.getModifiedTaxPayers(start, now);
		assertNotNull(two);
		assertNotNull(two.getPartyNo());
		assertEquals(2, two.getPartyNo().size());

		final List<Integer> sortedList = new ArrayList<>(two.getPartyNo());
		Collections.sort(sortedList);
		assertEquals(pp1.getLeft().longValue(), sortedList.get(0).longValue());
		assertEquals(pp2.getLeft().longValue(), sortedList.get(1).longValue());
	}

	@Test
	public void testDebtorInfo() throws Exception {

		final long dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addForDebiteur(dpi, date(2009, 1, 1), MotifFor.INDETERMINE, null, null, MockCommune.Lausanne);
			final PeriodeFiscale pf = addPeriodeFiscale(2013);
			final ModeleDocument md = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);
			addListeRecapitulative(dpi, pf, date(2013, 4, 1), date(2013, 4, 30), md);
			addListeRecapitulative(dpi, pf, date(2013, 10, 1), date(2013, 10, 31), md);
			return dpi.getNumero();
		});

		assertTrue(dpiId >= Integer.MIN_VALUE && dpiId <= Integer.MAX_VALUE);
		{
			final DebtorInfo info = service.getDebtorInfo((int) dpiId, 2012);
			assertEquals((int) dpiId, info.getNumber());
			assertEquals(2012, info.getTaxPeriod());
			assertEquals(0, info.getNumberOfWithholdingTaxDeclarationsIssued());
			assertEquals(12, info.getTheoreticalNumberOfWithholdingTaxDeclarations());
		}
		{
			final DebtorInfo info = service.getDebtorInfo((int) dpiId, 2013);
			assertEquals((int) dpiId, info.getNumber());
			assertEquals(2013, info.getTaxPeriod());
			assertEquals(2, info.getNumberOfWithholdingTaxDeclarationsIssued());
			assertEquals(12, info.getTheoreticalNumberOfWithholdingTaxDeclarations());
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

			ids = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Gérard", "Nietmochevillage", date(1979, 5, 31), Sexe.MASCULIN);
				final DebiteurPrestationImposable dpi = addDebiteur(null, pp, date(2013, 1, 1));
				addForDebiteur(dpi, date(2013, 1, 1), MotifFor.DEBUT_PRESTATION_IS, null, null, MockCommune.Bussigny);

				final Ids ids1 = new Ids();
				ids1.pp = pp.getNumero();
				ids1.dpi = dpi.getNumero();
				return ids1;
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

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche avec le numéro et une donnée bidon à côté (qui doit donc être ignorée)
		{
			final List<PartyInfo> res = service.searchParty(Long.toString(ids.pp),
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche sans le numéro et une donnée bidon à côté -> aucun résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Daboville", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(0, res.size());
		}

		// recherche par nom -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.dpi, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.DEBTOR, info.getType());
				assertNull(info.getNaturalPersonSubtype());
				assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types vide -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.emptySet(), null, null, null);

			assertNotNull(res);
			assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.dpi, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.DEBTOR, info.getType());
				assertNull(info.getNaturalPersonSubtype());
				assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types mauvaise -> aucun ne vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.HOUSEHOLD), null, null, null);

			assertNotNull(res);
			assertEquals(0, res.size());
		}

		// recherche par nom avec liste de types mauvaise -> aucun ne vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.RESIDENT_NATURAL_PERSON), null, null, null);

			assertNotNull(res);
			assertEquals(0, res.size());
		}

		// recherche par nom avec liste de types des deux -> les deux viennent
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.DEBTOR, PartySearchType.NATURAL_PERSON), null, null, null);

			assertNotNull(res);
			assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.dpi, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.DEBTOR, info.getType());
				assertNull(info.getNaturalPersonSubtype());
				assertNull(info.getIndividualTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types d'un seul -> seul celui-là vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.DEBTOR), null, null, null);

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.dpi, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.DEBTOR, info.getType());
				assertNull(info.getNaturalPersonSubtype());
				assertNull(info.getIndividualTaxLiability());
			}
		}

		// recherche par nom avec liste de types d'un seul -> seul celui-là vient
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, EnumSet.of(PartySearchType.NON_RESIDENT_NATURAL_PERSON), null, null, null);

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(date(1979, 5, 31), DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
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

			ids = doInNewTransactionAndSession(status -> {
				final PersonnePhysique ppUn = addNonHabitant("Gérard", "AvecUn", null, Sexe.MASCULIN);
				final IdentificationEntreprise identUn = new IdentificationEntreprise();
				identUn.setNumeroIde("CHE123456789");
				ppUn.addIdentificationEntreprise(identUn);

				final PersonnePhysique ppAutre = addNonHabitant("Gérard", "AvecAutre", null, Sexe.MASCULIN);
				final IdentificationEntreprise identAutre = new IdentificationEntreprise();
				identAutre.setNumeroIde("CHE987654321");
				ppAutre.addIdentificationEntreprise(identAutre);

				final PersonnePhysique ppSans = addNonHabitant("Gérard", "Sans", null, Sexe.MASCULIN);

				final Ids ids1 = new Ids();
				ids1.ppAvecUn = ppUn.getNumero().intValue();
				ids1.ppAvecAutre = ppAutre.getNumero().intValue();
				ids1.ppSans = ppSans.getNumero().intValue();
				return ids1;
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

			assertNotNull(res);
			assertEquals(3, res.size());

			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.ppAvecUn, info.getNumber());
				assertEquals("Gérard AvecUn", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				assertNotNull(info.getUidNumbers());
				assertEquals(Collections.singletonList("CHE123456789"), info.getUidNumbers().getUidNumber());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.ppAvecAutre, info.getNumber());
				assertEquals("Gérard AvecAutre", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				assertNotNull(info.getUidNumbers());
				assertEquals(Collections.singletonList("CHE987654321"), info.getUidNumbers().getUidNumber());
			}
			{
				final PartyInfo info = sortedRes.get(2);
				assertNotNull(info);
				assertEquals(ids.ppSans, info.getNumber());
				assertEquals("Gérard Sans", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				assertNull(info.getUidNumbers());
			}
		}

		// recherche avec critère d'IDE CHE123456789 -> un résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE123456789", null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.ppAvecUn, info.getNumber());
				assertEquals("Gérard AvecUn", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				assertNotNull(info.getUidNumbers());
				assertEquals(Collections.singletonList("CHE123456789"), info.getUidNumbers().getUidNumber());
			}
		}

		// recherche avec critère d'IDE CHE987654321 -> un autre résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE987654321", null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(1, res.size());

			{
				final PartyInfo info = res.get(0);
				assertNotNull(info);
				assertEquals(ids.ppAvecAutre, info.getNumber());
				assertEquals("Gérard AvecAutre", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(NaturalPersonSubtype.NON_RESIDENT, info.getNaturalPersonSubtype());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());
				assertNotNull(info.getUidNumbers());
				assertEquals(Collections.singletonList("CHE987654321"), info.getUidNumbers().getUidNumber());
			}
		}

		// recherche avec critère d'IDE CHE111222333 -> aucun résultat
		{
			final List<PartyInfo> res = service.searchParty(null,
			                                                "Gérard", SearchMode.IS_EXACTLY, null, null, null, "CHE111222333", null, false, null, null, null, null);

			assertNotNull(res);
			assertEquals(0, res.size());
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

			ids = doInNewTransactionAndSession(status -> {
				final PersonnePhysique pp = addNonHabitant("Gérard", "Nietmochevillage", dateNaissance, Sexe.MASCULIN);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne, ModeImposition.MIXTE_137_1);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne, ModeImposition.ORDINAIRE);

				final Ids ids1 = new Ids();
				ids1.pp = pp.getNumero();
				ids1.mc = mc.getNumero();
				return ids1;
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
			                                                "Nietmochevillage", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.emptySet(), null, null, null);

			assertNotNull(res);
			assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.pp, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertEquals(dateNaissance, DataHelper.webToRegDate(info.getDateOfBirth()));
				assertEquals(PartyType.NATURAL_PERSON, info.getType());
				assertEquals(IndividualTaxLiabilityType.NONE, info.getIndividualTaxLiability());     // il est maintenant marié
				assertEquals(dateNaissance.addYears(18), DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				assertEquals(dateMariage.getOneDayBefore(), DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
				assertNull(info.getCorporationTaxLiability());
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.mc, info.getNumber());
				assertEquals("Gérard Nietmochevillage", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.HOUSEHOLD, info.getType());
				assertEquals(IndividualTaxLiabilityType.ORDINARY_RESIDENT, info.getIndividualTaxLiability());
				assertEquals(dateMariage, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
				assertNull(info.getCorporationTaxLiability());
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

			ids = doInNewTransactionAndSession(status -> {
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
				addForSecondaire(entreprise2, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Lausanne, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

				final Ids ids1 = new Ids();
				ids1.entreprise1 = entreprise1.getNumero();
				ids1.entreprise2 = entreprise2.getNumero();
				return ids1;
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
			                                                "protection", SearchMode.IS_EXACTLY, null, null, null, null, null, false, Collections.emptySet(), null, null, null);

			assertNotNull(res);
			assertEquals(2, res.size());

			// triage des résultats par ordre croissant de numéro de tiers (le DPI viendra donc toujours devant)
			final List<PartyInfo> sortedRes = new ArrayList<>(res);
			sortedRes.sort(Comparator.comparingInt(PartyInfo::getNumber));

			{
				final PartyInfo info = sortedRes.get(0);
				assertNotNull(info);
				assertEquals(ids.entreprise1, info.getNumber());
				assertEquals("Association pour la protection des petits oiseaux des parcs", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.CORPORATION, info.getType());
				assertNull(info.getIndividualTaxLiability());
				assertEquals(CorporationTaxLiabilityType.ORDINARY_RESIDENT, info.getCorporationTaxLiability());
				assertEquals(dateDebut, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
			}
			{
				final PartyInfo info = sortedRes.get(1);
				assertNotNull(info);
				assertEquals(ids.entreprise2, info.getNumber());
				assertEquals("Gros-bras protection", info.getName1());
				assertEquals(StringUtils.EMPTY, info.getName2());
				assertNull(info.getDateOfBirth());
				assertEquals(PartyType.CORPORATION, info.getType());
				assertNull(info.getIndividualTaxLiability());
				assertEquals(CorporationTaxLiabilityType.OTHER_CANTON, info.getCorporationTaxLiability());
				assertEquals(dateDebut, DataHelper.webToRegDate(info.getLastTaxResidenceBeginDate()));
				assertNull(DataHelper.webToRegDate(info.getLastTaxResidenceEndDate()));
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

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noEntreprise, noEntreprise + 1011, "Au petit coin", pmActivityStartDate, null,
				                                                                                FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Cossonay, "CHE123456788");
				addEntreprise(ent);
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

		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero();
			ids1.mc = mc.getNumero();
			ids1.dpi = dpi.getNumero();
			ids1.pm = pm.getNumero();
			ids1.ca = ca.getNumero();
			ids1.ac = ac.getNumero();
			return ids1;
		});

		// get PP
		{
			final Party party = service.getParty((int) ids.pp, null);
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());
			assertEquals(ids.pp, party.getNumber());


			final NaturalPerson pp = (NaturalPerson) party;
			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
			assertEquals("Dufoin", pp.getOfficialName());
			assertEquals("Balthazar", pp.getFirstName());
			assertEquals(Sex.MALE, pp.getSex());
			assertEquals("Monsieur", pp.getSalutation());
			assertEquals("Monsieur", pp.getFormalGreeting());

			final List<NaturalPersonCategory> categories = pp.getCategories();
			assertNotNull(categories);
			assertEquals(1, categories.size());

			final NaturalPersonCategory category = categories.get(0);
			assertNotNull(category);
			assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, category.getCategory());
			assertEquals(datePermisC, ch.vd.unireg.xml.DataHelper.xmlToCore(category.getDateFrom()));
			assertNull(category.getDateTo());

			assertNotNull(pp.getMotherName());
			assertEquals("Delagrange", pp.getMotherName().getLastName());
			assertEquals("Martine", pp.getMotherName().getFirstNames());
			assertNotNull(pp.getFatherName());
			assertEquals("Dufoin", pp.getFatherName().getLastName());
			assertEquals("Melchior", pp.getFatherName().getFirstNames());
			assertNotNull(pp.getUidNumbers());
			assertEquals(1, pp.getUidNumbers().getUidNumber().size());
			assertEquals("CHE100001000", pp.getUidNumbers().getUidNumber().get(0));
		}
		// get MC
		{
			final Party party = service.getParty((int) ids.mc, null);
			assertNotNull(party);
			assertEquals(CommonHousehold.class, party.getClass());
			assertEquals(ids.mc, party.getNumber());
		}
		// get DPI
		{
			final Party party = service.getParty((int) ids.dpi, null);
			assertNotNull(party);
			assertEquals(Debtor.class, party.getClass());
			assertEquals(ids.dpi, party.getNumber());

			final Debtor dpi = (Debtor) party;
			assertEquals("Balthazar Dufoin", dpi.getName());
			assertEquals("Débiteur IS", dpi.getComplementaryName());
			assertEquals(ModeCommunication.ELECTRONIQUE, ch.vd.unireg.xml.EnumHelper.xmlToCore(dpi.getCommunicationMode()));
		}
		// get PM
		{
			final Party party = service.getParty((int) ids.pm, null);
			assertNotNull(party);
			assertEquals(Corporation.class, party.getClass());
			assertEquals(ids.pm, party.getNumber());

			final Corporation pm = (Corporation) party;
			assertEquals("Au petit coin", pm.getName());
			assertNotNull(pm.getUidNumbers());
			assertEquals(1, pm.getUidNumbers().getUidNumber().size());
			assertEquals("CHE123456788", pm.getUidNumbers().getUidNumber().get(0));
		}
		// get CA
		{
			final Party party = service.getParty((int) ids.ca, null);
			assertNotNull(party);
			assertEquals(AdministrativeAuthority.class, party.getClass());
			assertEquals(ids.ca, party.getNumber());

			final AdministrativeAuthority ca = (AdministrativeAuthority) party;
			assertEquals(MockCollectiviteAdministrative.CAT.getNomCourt(), ca.getName());
			assertEquals(MockCollectiviteAdministrative.CAT.getNoColAdm(), ca.getAdministrativeAuthorityId());
		}
		// get AC
		{
			final Party party = service.getParty((int) ids.ac, null);
			assertNotNull(party);
			assertEquals(OtherCommunity.class, party.getClass());
			assertEquals(ids.ac, party.getNumber());

			final OtherCommunity ac = (OtherCommunity) party;
			assertEquals("Tata!!", ac.getName());
			assertEquals(LegalForm.ASSOCIATION, ac.getLegalForm());

			assertNotNull(ac.getUidNumbers());
			assertEquals(1, ac.getUidNumbers().getUidNumber().size());
			assertEquals("CHE999999996", ac.getUidNumbers().getUidNumber().get(0));
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			return pp.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getRepresentationAddresses());
		assertNotNull(partySans.getDebtProsecutionAddressesOfOtherParty());
		assertNotNull(partySans.getRepresentationAddresses());
		assertNotNull(partySans.getMailAddresses());
		assertNotNull(partySans.getResidenceAddresses());
		assertEquals(0, partySans.getRepresentationAddresses().size());
		assertEquals(0, partySans.getDebtProsecutionAddressesOfOtherParty().size());
		assertEquals(0, partySans.getRepresentationAddresses().size());
		assertEquals(0, partySans.getMailAddresses().size());
		assertEquals(0, partySans.getResidenceAddresses().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.ADDRESSES));
		assertNotNull(partyAvec);
		assertNotNull(partyAvec.getRepresentationAddresses());
		assertNotNull(partyAvec.getDebtProsecutionAddressesOfOtherParty());
		assertNotNull(partyAvec.getRepresentationAddresses());
		assertNotNull(partyAvec.getMailAddresses());
		assertNotNull(partyAvec.getResidenceAddresses());
		assertEquals(1, partyAvec.getRepresentationAddresses().size());
		assertEquals(0, partyAvec.getDebtProsecutionAddressesOfOtherParty().size());
		assertEquals(1, partyAvec.getRepresentationAddresses().size());
		assertEquals(1, partyAvec.getMailAddresses().size());
		assertEquals(1, partyAvec.getResidenceAddresses().size());

		{
			final Address address = partyAvec.getRepresentationAddresses().get(0);
			assertNotNull(address);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			assertEquals(AddressType.REPRESENTATION, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			assertNotNull(recipient);
			assertNull(recipient.getCouple());
			assertNull(recipient.getOrganisation());
			assertFalse(address.isFake());
			assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
			assertNotNull(info);
			assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			assertNull(info.getAddressLine1());
			assertNull(info.getAddressLine2());
			assertNull(info.getCareOf());
			assertNull(info.getComplementaryInformation());
			assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Monsieur", formatted.getLine1());
			assertEquals("Arthur Delagrange", formatted.getLine2());
			assertEquals("Avenue du Funiculaire", formatted.getLine3());
			assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			assertNotNull(person);
			assertEquals("Monsieur", person.getFormalGreeting());
			assertEquals("Monsieur", person.getSalutation());
			assertEquals("Delagrange", person.getLastName());
			assertEquals("Arthur", person.getFirstName());
			assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getDebtProsecutionAddresses().get(0);
			assertNotNull(address);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			assertEquals(AddressType.DEBT_PROSECUTION, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			assertNotNull(recipient);
			assertNull(recipient.getCouple());
			assertNull(recipient.getOrganisation());
			assertFalse(address.isFake());
			assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
			assertNotNull(info);
			assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			assertNull(info.getAddressLine1());
			assertNull(info.getAddressLine2());
			assertNull(info.getCareOf());
			assertNull(info.getComplementaryInformation());
			assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Monsieur", formatted.getLine1());
			assertEquals("Arthur Delagrange", formatted.getLine2());
			assertEquals("Avenue du Funiculaire", formatted.getLine3());
			assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			assertNotNull(person);
			assertEquals("Monsieur", person.getFormalGreeting());
			assertEquals("Monsieur", person.getSalutation());
			assertEquals("Delagrange", person.getLastName());
			assertEquals("Arthur", person.getFirstName());
			assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getMailAddresses().get(0);
			assertNotNull(address);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			assertEquals(AddressType.MAIL, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			assertNotNull(recipient);
			assertNull(recipient.getCouple());
			assertNull(recipient.getOrganisation());
			assertFalse(address.isFake());
			assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
			assertNotNull(info);
			assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			assertNull(info.getAddressLine1());
			assertNull(info.getAddressLine2());
			assertNull(info.getCareOf());
			assertNull(info.getComplementaryInformation());
			assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Monsieur", formatted.getLine1());
			assertEquals("Arthur Delagrange", formatted.getLine2());
			assertEquals("Avenue du Funiculaire", formatted.getLine3());
			assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			assertNotNull(person);
			assertEquals("Monsieur", person.getFormalGreeting());
			assertEquals("Monsieur", person.getSalutation());
			assertEquals("Delagrange", person.getLastName());
			assertEquals("Arthur", person.getFirstName());
			assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
		}
		{
			final Address address = partyAvec.getResidenceAddresses().get(0);
			assertNotNull(address);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(address.getDateFrom()));
			assertEquals(AddressType.RESIDENCE, address.getType());
			final PostAddress postAddress = address.getPostAddress();
			assertNotNull(postAddress);
			final Recipient recipient = postAddress.getRecipient();
			assertNotNull(recipient);
			assertNull(recipient.getCouple());
			assertNull(recipient.getOrganisation());
			assertFalse(address.isFake());
			assertFalse(postAddress.isIncomplete());

			final AddressInformation info = postAddress.getDestination();
			assertNotNull(info);
			assertEquals((Integer) MockPays.Suisse.getNoOFS(), info.getCountryId());
			assertEquals(MockPays.Suisse.getNomOfficiel(), info.getCountryName());
			assertEquals(MockRue.CossonayVille.AvenueDuFuniculaire.getDesignationCourrier(), info.getStreet());
			assertNull(info.getAddressLine1());
			assertNull(info.getAddressLine2());
			assertNull(info.getCareOf());
			assertNull(info.getComplementaryInformation());
			assertEquals((Integer) MockCommune.Cossonay.getNoOFS(), info.getMunicipalityId());
			assertEquals(TariffZone.SWITZERLAND, info.getTariffZone());
			assertEquals(MockLocalite.CossonayVille.getNomAbrege(), info.getTown());
			assertEquals((Long) MockLocalite.CossonayVille.getNPA().longValue(), info.getSwissZipCode());
			assertEquals(MockPays.Suisse.getCodeIso2(), info.getCountry());

			final FormattedAddress formatted = postAddress.getFormattedAddress();
			assertNotNull(formatted);
			assertEquals("Monsieur", formatted.getLine1());
			assertEquals("Arthur Delagrange", formatted.getLine2());
			assertEquals("Avenue du Funiculaire", formatted.getLine3());
			assertEquals("1304 Cossonay-Ville", formatted.getLine4());
			assertNull(formatted.getLine5());
			assertNull(formatted.getLine6());

			final PersonMailAddressInfo person = recipient.getPerson();
			assertNotNull(person);
			assertEquals("Monsieur", person.getFormalGreeting());
			assertEquals("Monsieur", person.getSalutation());
			assertEquals("Delagrange", person.getLastName());
			assertEquals("Arthur", person.getFirstName());
			assertEquals(ch.vd.unireg.xml.DataHelper.salutations2MrMrs("Monsieur"), person.getMrMrs());
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getMainTaxResidences());
		assertNotNull(partySans.getOtherTaxResidences());
		assertNotNull(partySans.getManagingTaxResidences());
		assertEquals(0, partySans.getMainTaxResidences().size());
		assertEquals(0, partySans.getOtherTaxResidences().size());
		assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.TAX_RESIDENCES));
		assertNotNull(partyAvec);
		assertNotNull(partyAvec.getMainTaxResidences());
		assertNotNull(partyAvec.getOtherTaxResidences());
		assertNotNull(partyAvec.getManagingTaxResidences());
		assertEquals(1, partyAvec.getMainTaxResidences().size());
		assertEquals(1, partyAvec.getOtherTaxResidences().size());
		assertEquals(0, partyAvec.getManagingTaxResidences().size());

		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(0);
			assertNotNull(tr);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, tr.getStartReason());
			assertNull(tr.getDateTo());
			assertNull(tr.getEndReason());
			assertEquals(MockCommune.Aubonne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			assertFalse(tr.isVirtual());
		}
		{
			final TaxResidence tr = partyAvec.getOtherTaxResidences().get(0);
			assertNotNull(tr);
			assertEquals(dateAchat, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			assertEquals(LiabilityChangeReason.PURCHASE_REAL_ESTATE, tr.getStartReason());
			assertEquals(dateVente, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateTo()));
			assertEquals(LiabilityChangeReason.SALE_REAL_ESTATE, tr.getEndReason());
			assertEquals(MockCommune.Echallens.getNoOFS(), tr.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			assertNull(tr.getTaxationMethod());
			assertEquals(TaxLiabilityReason.PRIVATE_IMMOVABLE_PROPERTY, tr.getTaxLiabilityReason());
			assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			assertFalse(tr.isVirtual());
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee.addYears(-1), MotifFor.DEBUT_EXPLOITATION, dateArrivee.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
			                MockPays.Allemagne);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, null, dateArrivee, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(mc, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getMainTaxResidences());
		assertNotNull(partySans.getOtherTaxResidences());
		assertNotNull(partySans.getManagingTaxResidences());
		assertEquals(0, partySans.getMainTaxResidences().size());
		assertEquals(0, partySans.getOtherTaxResidences().size());
		assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.VIRTUAL_TAX_RESIDENCES));
		assertNotNull(partyAvec);
		assertNotNull(partyAvec.getMainTaxResidences());
		assertNotNull(partyAvec.getOtherTaxResidences());
		assertNotNull(partyAvec.getManagingTaxResidences());
		assertEquals(2, partyAvec.getMainTaxResidences().size());
		assertEquals(0, partyAvec.getOtherTaxResidences().size());
		assertEquals(0, partyAvec.getManagingTaxResidences().size());

		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(0);
			assertNotNull(tr);
			assertEquals(dateArrivee.addYears(-1), ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, tr.getStartReason());
			assertEquals(dateArrivee.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateTo()));
			assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, tr.getEndReason());
			assertEquals(MockPays.Allemagne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.FOREIGN_COUNTRY, tr.getTaxationAuthorityType());
			assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			assertFalse(tr.isVirtual());
		}
		{
			final TaxResidence tr = partyAvec.getMainTaxResidences().get(1);
			assertNotNull(tr);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(tr.getDateFrom()));
			assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, tr.getStartReason());
			assertNull(tr.getDateTo());
			assertNull(tr.getEndReason());
			assertEquals(MockCommune.Aubonne.getNoOFS(), tr.getTaxationAuthorityFSOId());
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, tr.getTaxationAuthorityType());
			assertEquals(TaxationMethod.ORDINARY, tr.getTaxationMethod());
			assertEquals(TaxLiabilityReason.RESIDENCE, tr.getTaxLiabilityReason());
			assertEquals(TaxType.INCOME_WEALTH, tr.getTaxType());
			assertTrue(tr.isVirtual());
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);
			addForSecondaire(pp, dateAchat, MotifFor.ACHAT_IMMOBILIER, dateVente, MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getMainTaxResidences());
		assertNotNull(partySans.getOtherTaxResidences());
		assertNotNull(partySans.getManagingTaxResidences());
		assertEquals(0, partySans.getMainTaxResidences().size());
		assertEquals(0, partySans.getOtherTaxResidences().size());
		assertEquals(0, partySans.getManagingTaxResidences().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.MANAGING_TAX_RESIDENCES));
		assertNotNull(partyAvec);
		assertNotNull(partyAvec.getMainTaxResidences());
		assertNotNull(partyAvec.getOtherTaxResidences());
		assertNotNull(partyAvec.getManagingTaxResidences());
		assertEquals(0, partyAvec.getMainTaxResidences().size());
		assertEquals(0, partyAvec.getOtherTaxResidences().size());
		assertEquals(1, partyAvec.getManagingTaxResidences().size());

		{
			final ManagingTaxResidence mtr = partyAvec.getManagingTaxResidences().get(0);
			assertNotNull(mtr);
			assertEquals(dateArrivee, ch.vd.unireg.xml.DataHelper.xmlToCore(mtr.getDateFrom()));
			assertNull(mtr.getDateTo());
			assertEquals(MockCommune.Aubonne.getNoOFS(), mtr.getMunicipalityFSOId());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

			final Ids ids1 = new Ids();
			ids1.lui = lui.getNumero().intValue();
			ids1.elle = elle.getNumero().intValue();
			ids1.mc = mc.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.mc, null);
		assertNotNull(partySans);
		assertEquals(CommonHousehold.class, partySans.getClass());

		final CommonHousehold mcSans = (CommonHousehold) partySans;
		assertNull(mcSans.getMainTaxpayer());
		assertNull(mcSans.getSecondaryTaxpayer());
		assertEquals("Monsieur et Madame", mcSans.getSalutation());
		assertEquals("Monsieur et Madame", mcSans.getFormalGreeting());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(InternalPartyPart.HOUSEHOLD_MEMBERS));
		assertNotNull(partyAvec);
		assertEquals(CommonHousehold.class, partyAvec.getClass());

		final CommonHousehold mcAvec = (CommonHousehold) partyAvec;
		assertEquals("Monsieur et Madame", mcAvec.getSalutation());
		assertEquals("Monsieur et Madame", mcAvec.getFormalGreeting());
		{
			final NaturalPerson pp = mcAvec.getMainTaxpayer();
			assertNotNull(pp);
			assertEquals(Sex.MALE, pp.getSex());
			assertEquals("Delagrange", pp.getOfficialName());
			assertEquals("Marcel", pp.getFirstName());
			assertEquals("Monsieur", pp.getSalutation());
			assertEquals("Monsieur", pp.getFormalGreeting());
			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(pp.getDateOfBirth()));
			assertEquals(ids.lui, pp.getNumber());
		}
		{
			final NaturalPerson pp = mcAvec.getSecondaryTaxpayer();
			assertNotNull(pp);
			assertEquals(Sex.FEMALE, pp.getSex());
			assertEquals("Delagrange", pp.getOfficialName());
			assertEquals("Marceline", pp.getFirstName());
			assertEquals("Madame", pp.getSalutation());
			assertEquals("Madame", pp.getFormalGreeting());
			assertNull(pp.getDateOfBirth());
			assertEquals(ids.elle, pp.getNumber());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

			final Ids ids1 = new Ids();
			ids1.lui = lui.getNumero().intValue();
			ids1.elle = elle.getNumero().intValue();
			ids1.mc = mc.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.mc, null);
		assertNotNull(partySans);
		assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		assertNotNull(tpSans.getTaxLiabilities());
		assertEquals(0, tpSans.getTaxLiabilities().size());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(InternalPartyPart.TAX_LIABILITIES));
		assertNotNull(partyAvec);
		assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		assertNotNull(tpAvec.getTaxLiabilities());
		assertEquals(1, tpAvec.getTaxLiabilities().size());

		final TaxLiability tl = tpAvec.getTaxLiabilities().get(0);
		assertNotNull(tl);
		assertEquals(date(dateMariage.year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
		assertEquals(LiabilityChangeReason.MARRIAGE_PARTNERSHIP_END_OF_SEPARATION, tl.getStartReason());
		assertNull(tl.getDateTo());
		assertNull(tl.getEndReason());
		assertEquals(OrdinaryResident.class, tl.getClass());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aubonne);

			final Ids ids1 = new Ids();
			ids1.lui = lui.getNumero().intValue();
			ids1.elle = elle.getNumero().intValue();
			ids1.mc = mc.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.mc, null);
		assertNotNull(partySans);
		assertEquals(CommonHousehold.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		assertNotNull(tpSans.getTaxationPeriods());
		assertEquals(0, tpSans.getTaxationPeriods().size());

		final Party partyAvec = service.getParty(ids.mc, EnumSet.of(InternalPartyPart.TAXATION_PERIODS));
		assertNotNull(partyAvec);
		assertEquals(CommonHousehold.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		assertNotNull(tpAvec.getTaxationPeriods());
		assertEquals(RegDate.get().year() - dateMariage.year() + 1, tpAvec.getTaxationPeriods().size());

		for (int year = dateMariage.year(); year <= RegDate.get().year(); ++year) {
			final TaxationPeriod tp = tpAvec.getTaxationPeriods().get(year - dateMariage.year());
			assertNotNull(tp);
			assertEquals(date(year, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
			if (year == RegDate.get().year()) {
				assertNull(tp.getDateTo());
			}
			else {
				assertEquals(date(year, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
			}
			assertNull(tp.getTaxDeclarationId());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);

			final Ids ids1 = new Ids();
			ids1.lui = lui.getNumero().intValue();
			ids1.elle = elle.getNumero().intValue();
			ids1.mc = mc.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.lui, null);
		assertNotNull(partySans);
		assertEquals(NaturalPerson.class, partySans.getClass());

		final NaturalPerson tpSans = (NaturalPerson) partySans;
		assertNotNull(tpSans.getWithholdingTaxationPeriods());
		assertEquals(0, tpSans.getWithholdingTaxationPeriods().size());

		final Party partyAvec = service.getParty(ids.lui, EnumSet.of(InternalPartyPart.WITHHOLDING_TAXATION_PERIODS));
		assertNotNull(partyAvec);
		assertEquals(NaturalPerson.class, partyAvec.getClass());

		final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
		assertNotNull(tpAvec.getWithholdingTaxationPeriods());
		assertEquals(8, tpAvec.getWithholdingTaxationPeriods().size());

		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(0);
			assertNotNull(wtp);
			assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(dateNaissance.addYears(18).getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertNull(wtp.getTaxationAuthority());
			assertNull(wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(1);
			assertNotNull(wtp);
			assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(2);
			assertNotNull(wtp);
			assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aigle.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(3);
			assertNotNull(wtp);
			assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(dateMariage.getLastDayOfTheMonth(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.PURE, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(4);
			assertNotNull(wtp);
			assertEquals(dateMariage.getLastDayOfTheMonth().getOneDayAfter(), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(5);
			assertNotNull(wtp);
			assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(6);
			assertNotNull(wtp);
			assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
		}
		{
			final WithholdingTaxationPeriod wtp = tpAvec.getWithholdingTaxationPeriods().get(7);
			assertNotNull(wtp);
			assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
			assertEquals(dateDeces, ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
			assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
			assertEquals((Integer) MockCommune.Aubonne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
			assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			addForPrincipal(lui, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Aigle, ModeImposition.SOURCE);

			final PersonnePhysique elle = addHabitant(noIndividuElle);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
			final MenageCommun mc = couple.getMenage();
			addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateDeces, MotifFor.VEUVAGE_DECES, MockCommune.Aubonne, ModeImposition.MIXTE_137_2);

			final DebiteurPrestationImposable dpi = addDebiteur("Débiteur lié", mc, dateMariage);
			addRapportPrestationImposable(dpi, lui, dateDebutRT, dateDeces, true);

			final Ids ids1 = new Ids();
			ids1.lui = lui.getNumero().intValue();
			ids1.elle = elle.getNumero().intValue();
			ids1.mc = mc.getNumero().intValue();
			ids1.dpi = dpi.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.lui, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getRelationsBetweenParties());
		assertEquals(0, partySans.getRelationsBetweenParties().size());

		final Party partyAvec = service.getParty(ids.lui, EnumSet.of(InternalPartyPart.RELATIONS_BETWEEN_PARTIES));
		assertNotNull(partyAvec);
		assertNotNull(partyAvec.getRelationsBetweenParties());
		assertEquals(2, partyAvec.getRelationsBetweenParties().size());

		final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
		sortedRelations.sort((o1, o2) -> {
			final DateRange r1 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateTo()));
			final DateRange r2 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateTo()));
			return DateRangeComparator.compareRanges(r1, r2);
		});

		{
			final RelationBetweenParties rel = sortedRelations.get(0);
			assertNotNull(rel);
			assertEquals(dateMariage, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			assertNull(rel.getDateTo());
			assertTrue(rel instanceof HouseholdMember);
			assertEquals(ids.mc, rel.getOtherPartyNumber());
			assertNull(rel.getCancellationDate());
		}
		{
			final RelationBetweenParties rel = sortedRelations.get(1);
			assertNotNull(rel);
			assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			assertEquals(dateDeces, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
			assertTrue(rel instanceof TaxableRevenue);
			assertEquals(ids.dpi, rel.getOtherPartyNumber());
			assertNotNull(rel.getCancellationDate());
			assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addHabitant(noIndividuLui);
			return lui.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertEquals(NaturalPerson.class, partySans.getClass());

		final Taxpayer tpSans = (Taxpayer) partySans;
		assertNotNull(tpSans.getFamilyStatuses());
		assertEquals(0, tpSans.getFamilyStatuses().size());

		final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.FAMILY_STATUSES));
		assertNotNull(partyAvec);
		assertEquals(NaturalPerson.class, partyAvec.getClass());

		final Taxpayer tpAvec = (Taxpayer) partyAvec;
		assertNotNull(tpAvec.getFamilyStatuses());
		assertEquals(4, tpAvec.getFamilyStatuses().size());

		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(0);
			assertNotNull(fs);
			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			assertEquals(dateMariage.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			assertNull(fs.getCancellationDate());
			assertNull(fs.getApplicableTariff());
			assertNull(fs.getMainTaxpayerNumber());
			assertNull(fs.getNumberOfChildren());
			assertEquals(MaritalStatus.SINGLE, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(1);
			assertNotNull(fs);
			assertEquals(dateMariage, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			assertEquals(dateSeparation.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			assertNull(fs.getCancellationDate());
			assertNull(fs.getApplicableTariff());
			assertNull(fs.getMainTaxpayerNumber());
			assertNull(fs.getNumberOfChildren());
			assertEquals(MaritalStatus.MARRIED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(2);
			assertNotNull(fs);
			assertEquals(dateSeparation, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			assertEquals(dateDivorce.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateTo()));
			assertNull(fs.getCancellationDate());
			assertNull(fs.getApplicableTariff());
			assertNull(fs.getMainTaxpayerNumber());
			assertNull(fs.getNumberOfChildren());
			assertEquals(MaritalStatus.SEPARATED, fs.getMaritalStatus());
		}
		{
			final FamilyStatus fs = tpAvec.getFamilyStatuses().get(3);
			assertNotNull(fs);
			assertEquals(dateDivorce, ch.vd.unireg.xml.DataHelper.xmlToCore(fs.getDateFrom()));
			assertNull(fs.getDateTo());
			assertNull(fs.getCancellationDate());
			assertNull(fs.getApplicableTariff());
			assertNull(fs.getMainTaxpayerNumber());
			assertNull(fs.getNumberOfChildren());
			assertEquals(MaritalStatus.DIVORCED, fs.getMaritalStatus());
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

		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			addForPrincipal(pp, dateArrivee, MotifFor.ARRIVEE_HS, MockCommune.Aubonne);

			final CollectiviteAdministrative cedi = tiersService.getCollectiviteAdministrative(ServiceInfrastructureRaw.noCEDI);
			final PeriodeFiscale pf = addPeriodeFiscale(2013);
			final ModeleDocument md = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, pf);
			final DeclarationImpotOrdinaire di = addDeclarationImpot(pp, pf, date(pf.getAnnee(), 1, 1), date(pf.getAnnee(), 12, 31), cedi, TypeContribuable.VAUDOIS_ORDINAIRE, md);
			addEtatDeclarationEmise(di, dateEmissionDi);
			addDelaiDeclaration(di, dateEmissionDi, dateDelaiDi, EtatDelaiDocumentFiscal.ACCORDE);

			return pp.getNumero().intValue();
		});

		final Party partySans = service.getParty(ppId, null);
		assertNotNull(partySans);
		assertNotNull(partySans.getTaxDeclarations());
		assertEquals(0, partySans.getTaxDeclarations().size());

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.TAX_DECLARATIONS));
			assertNotNull(partyAvec);
			assertNotNull(partyAvec.getTaxDeclarations());
			assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			assertNotNull(di);
			assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			assertNotNull(di.getDeadlines());
			assertEquals(0, di.getDeadlines().size());
			assertNotNull(di.getStatuses());
			assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			assertNotNull(period);
			assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.TAX_DECLARATIONS_DEADLINES));
			assertNotNull(partyAvec);
			assertNotNull(partyAvec.getTaxDeclarations());
			assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			assertNotNull(di);
			assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			assertNotNull(di.getDeadlines());
			assertEquals(1, di.getDeadlines().size());

			final TaxDeclarationDeadline delai = di.getDeadlines().get(0);
			assertNotNull(delai);
			assertEquals(dateDelaiDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getDeadline()));
			assertNull(delai.getCancellationDate());
			assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getApplicationDate()));
			assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(delai.getProcessingDate()));

			assertNotNull(di.getStatuses());
			assertEquals(0, di.getStatuses().size());

			final TaxPeriod period = di.getTaxPeriod();
			assertNotNull(period);
			assertEquals(2013, period.getYear());
		}

		{
			final Party partyAvec = service.getParty(ppId, EnumSet.of(InternalPartyPart.TAX_DECLARATIONS_STATUSES));
			assertNotNull(partyAvec);
			assertNotNull(partyAvec.getTaxDeclarations());
			assertEquals(1, partyAvec.getTaxDeclarations().size());

			final TaxDeclaration di = partyAvec.getTaxDeclarations().get(0);
			assertNotNull(di);
			assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateFrom()));
			assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(di.getDateTo()));
			assertNotNull(di.getDeadlines());
			assertEquals(0, di.getDeadlines().size());
			assertNotNull(di.getStatuses());
			assertEquals(1, di.getStatuses().size());

			final TaxDeclarationStatus status = di.getStatuses().get(0);
			assertNotNull(status);
			assertEquals(dateEmissionDi, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
			assertNull(status.getCancellationDate());
			assertEquals(TaxDeclarationStatusType.SENT, status.getType());

			final TaxPeriod period = di.getTaxPeriod();
			assertNotNull(period);
			assertEquals(2013, period.getYear());
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

		final int dpiId = doInNewTransactionAndSession(status -> {
			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, dateDebutPeriodiciteInitiale);
			final Periodicite periodiciteInitiale = dpi.getPeriodicites().iterator().next();
			periodiciteInitiale.setDateFin(dateDebutPeriodiciteModifiee.getOneDayBefore());
			dpi.getPeriodicites().add(new Periodicite(PeriodiciteDecompte.SEMESTRIEL, null, dateDebutPeriodiciteModifiee, null));
			return dpi.getNumero().intValue();
		});

		final Party partySans = service.getParty(dpiId, null);
		assertNotNull(partySans);
		assertEquals(Debtor.class, partySans.getClass());

		final Debtor dpiSans = (Debtor) partySans;
		assertNotNull(dpiSans.getPeriodicities());
		assertEquals(0, dpiSans.getPeriodicities().size());

		final Party partyAvec = service.getParty(dpiId, EnumSet.of(InternalPartyPart.DEBTOR_PERIODICITIES));
		assertNotNull(partyAvec);
		assertEquals(Debtor.class, partyAvec.getClass());

		final Debtor dpiAvec = (Debtor) partyAvec;
		assertNotNull(dpiAvec.getPeriodicities());
		assertEquals(2, dpiAvec.getPeriodicities().size());

		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(0);
			assertNotNull(dp);
			assertNull(dp.getCancellationDate());
			assertEquals(dateDebutPeriodiciteInitiale, ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			assertEquals(dateDebutPeriodiciteModifiee.getOneDayBefore(), ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateTo()));
			assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, dp.getPeriodicity());
			assertNull(dp.getSpecificPeriod());
		}
		{
			final DebtorPeriodicity dp = dpiAvec.getPeriodicities().get(1);
			assertNotNull(dp);
			assertNull(dp.getCancellationDate());
			assertEquals(dateDebutPeriodiciteModifiee, ch.vd.unireg.xml.DataHelper.xmlToCore(dp.getDateFrom()));
			assertNull(dp.getDateTo());
			assertEquals(WithholdingTaxDeclarationPeriodicity.HALF_YEARLY, dp.getPeriodicity());
			assertNull(dp.getSpecificPeriod());
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

		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique papa = addHabitant(noIndividuPapa);
			final PersonnePhysique moi = addHabitant(noIndividu);
			final PersonnePhysique fiston = addHabitant(noIndividuFiston);
			final Ids ids1 = new Ids();
			ids1.papa = papa.getNumero().intValue();
			ids1.moi = moi.getNumero().intValue();
			ids1.fiston = fiston.getNumero().intValue();
			return ids1;
		});

		final Party partySans = service.getParty(ids.moi, null);
		assertNotNull(partySans);
		assertEquals(NaturalPerson.class, partySans.getClass());
		assertNotNull(partySans.getRelationsBetweenParties());
		assertEquals(0, partySans.getRelationsBetweenParties().size());

		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(InternalPartyPart.PARENTS));
			assertNotNull(partyAvec);
			assertEquals(NaturalPerson.class, partyAvec.getClass());

			final NaturalPerson tpAvec = (NaturalPerson) partyAvec;
			assertNotNull(tpAvec.getRelationsBetweenParties());
			assertEquals(1, tpAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = tpAvec.getRelationsBetweenParties().get(0);
			assertNotNull(rel);
			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			assertEquals(dateDecesPapa, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
			assertTrue(rel instanceof Parent);
			assertEquals(ids.papa, rel.getOtherPartyNumber());
			assertNull(rel.getCancellationDate());
		}
		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(InternalPartyPart.CHILDREN));
			assertNotNull(partyAvec);
			assertEquals(NaturalPerson.class, partyAvec.getClass());
			assertNotNull(partyAvec.getRelationsBetweenParties());
			assertEquals(1, partyAvec.getRelationsBetweenParties().size());

			final RelationBetweenParties rel = partyAvec.getRelationsBetweenParties().get(0);
			assertNotNull(rel);
			assertEquals(dateNaissanceFiston, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
			assertNull(rel.getDateTo());
			assertTrue(rel instanceof Child);
			assertEquals(ids.fiston, rel.getOtherPartyNumber());
			assertNull(rel.getCancellationDate());
		}
		{
			final Party partyAvec = service.getParty(ids.moi, EnumSet.of(InternalPartyPart.CHILDREN, InternalPartyPart.PARENTS));
			assertNotNull(partyAvec);
			assertEquals(NaturalPerson.class, partyAvec.getClass());
			assertNotNull(partyAvec.getRelationsBetweenParties());
			assertEquals(2, partyAvec.getRelationsBetweenParties().size());

			final List<RelationBetweenParties> sortedRelations = new ArrayList<>(partyAvec.getRelationsBetweenParties());
			sortedRelations.sort((o1, o2) -> {
				final DateRange r1 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o1.getDateTo()));
				final DateRange r2 = new DateRangeHelper.Range(ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateFrom()), ch.vd.unireg.xml.DataHelper.xmlToCore(o2.getDateTo()));
				return DateRangeComparator.compareRanges(r1, r2);
			});

			{
				final RelationBetweenParties rel = sortedRelations.get(0);
				assertNotNull(rel);
				assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertEquals(dateDecesPapa, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				assertTrue(rel instanceof Parent);
				assertEquals(ids.papa, rel.getOtherPartyNumber());
				assertNull(rel.getCancellationDate());
			}
			{
				final RelationBetweenParties rel = sortedRelations.get(1);
				assertNotNull(rel);
				assertEquals(dateNaissanceFiston, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertNull(rel.getDateTo());
				assertTrue(rel instanceof Child);
				assertEquals(ids.fiston, rel.getOtherPartyNumber());
				assertNull(rel.getCancellationDate());
			}
		}
	}

	/**
	 * [FISCPROJ-49] Vérifie que les périodes d'exploitation sont correctement exposées dans le WS v7.
	 */
	@Test
	public void testGetPartyWithOperatingPeriods() throws Exception {

		final Long id = doInNewTransaction(status -> {
			final RegDate dateCreationEntreprise = date(2010, 6, 1);
			final RegDate dateRadiationEntreprise = date(2014, 9, 11);
			final Entreprise entreprise = addEntrepriseInconnueAuCivil("Ma petite entreprise", dateCreationEntreprise);
			addRegimeFiscalVD(entreprise, dateCreationEntreprise, dateRadiationEntreprise, MockTypeRegimeFiscal.SOCIETE_PERS);
			addRegimeFiscalCH(entreprise, dateCreationEntreprise, dateRadiationEntreprise, MockTypeRegimeFiscal.SOCIETE_PERS);
			addAdresseSuisse(entreprise, TypeAdresseTiers.COURRIER, dateCreationEntreprise, dateRadiationEntreprise, MockRue.Lausanne.AvenueDeBeaulieu);
			addForPrincipal(entreprise, dateCreationEntreprise, MotifFor.DEBUT_EXPLOITATION, dateRadiationEntreprise, MotifFor.FIN_EXPLOITATION, MockCommune.Lausanne, GenreImpot.REVENU_FORTUNE);
			return entreprise.getNumero();
		});

		final Party party = service.getParty(id.intValue(), EnumSet.of(InternalPartyPart.OPERATING_PERIODS));
		assertNotNull(party);
		assertTrue(party instanceof Corporation);

		final Corporation corporation = (Corporation) party;
		final List<OperatingPeriod> operatingPeriods = corporation.getOperatingPeriods();
		assertNotNull(operatingPeriods);
		assertEquals(5, operatingPeriods.size());
		assertOperatingPeriod(date(2010, 6, 1), date(2010, 12, 31), operatingPeriods.get(0));
		assertOperatingPeriod(date(2011, 1, 1), date(2011, 12, 31), operatingPeriods.get(1));
		assertOperatingPeriod(date(2012, 1, 1), date(2012, 12, 31), operatingPeriods.get(2));
		assertOperatingPeriod(date(2013, 1, 1), date(2013, 12, 31), operatingPeriods.get(3));
		assertOperatingPeriod(date(2014, 1, 1), date(2014, 9, 11), operatingPeriods.get(4));
	}

	/**
	 * [IMM-1206] Vérifie que les allégements fonciers virtuels sont correctement exposées dans le WS v7.
	 */
	@Test
	public void testGetPartyWithVirtualLandTaxLightenings() throws Exception {

		final RegDate dateFusion = RegDate.get(2004, 4, 17);

		class Ids {
			Long absorbante;
			Long immeuble;

			public Ids(Long absorbante, Long immeuble) {
				this.absorbante = absorbante;
				this.immeuble = immeuble;
			}
		}

		final Ids ids = doInNewTransaction(status -> {
			// création de deux entreprises, une absorbée et l'autre absorbante
			final Entreprise absorbee = addEntrepriseInconnueAuCivil("Fantôme", RegDate.get(1990, 1, 1));
			final Entreprise absorbante = addEntrepriseInconnueAuCivil("Pacman", RegDate.get(1990, 1, 1));
			addFusionEntreprises(absorbante, absorbee, dateFusion);

			final CommuneRF laSarraz = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF immeuble0 = addBienFondsRF("01faeee", "some egrid", laSarraz, 579);

			// l'entreprise absorbée possède 3 allégements dont 1 qui se termine avant la fusion
			addExonerationIFONC(absorbee, immeuble0, date(2000, 1, 1), date(2009, 12, 31), BigDecimal.TEN);
			addDegrevementICI(absorbee, immeuble0, 2000, 2001,
			                  new DonneesUtilisation(10000L, 360L, 240L, BigDecimal.valueOf(100), BigDecimal.valueOf(100)),
			                  new DonneesUtilisation(10000L, 360L, 240L, BigDecimal.ZERO, BigDecimal.ZERO), null);
			addDegrevementICI(absorbee, immeuble0, 2002, null,
			                  new DonneesUtilisation(10000L, 360L, 240L, BigDecimal.valueOf(40), BigDecimal.valueOf(50)),
			                  new DonneesUtilisation(10000L, 360L, 240L, BigDecimal.valueOf(50), BigDecimal.valueOf(60)), null);

			return new Ids(absorbante.getId(), immeuble0.getId());
		});

		final Party party = service.getParty(ids.absorbante.intValue(), EnumSet.of(InternalPartyPart.VIRTUAL_LAND_TAX_LIGHTENINGS));
		assertNotNull(party);
		assertTrue(party instanceof Corporation);

		final Corporation corporation = (Corporation) party;
		final List<VirtualLandTaxLightening> lightenings = corporation.getVirtualLandTaxLightenings();
		assertNotNull(lightenings);
		assertEquals(2, lightenings.size());

		final VirtualLandTaxLightening lightening0 = lightenings.get(0);
		assertVirtualLandTaxLightening(dateFusion, date(2009, 12, 31), ids.immeuble, lightening0);
		assertIfoncExemption(10, lightening0.getReference());

		final VirtualLandTaxLightening lightening1 = lightenings.get(1);
		assertVirtualLandTaxLightening(dateFusion, null, ids.immeuble, lightening1);
		assertIciAbatement(60, lightening1.getReference());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero().intValue();
			ids1.dpi = dpi.getNumero().intValue();
			ids1.di = di.getId();
			ids1.immeuble = immeuble.getId();
			return ids1;
		});

		efactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.pp);
				addEtatDestinataire(ids.pp, dateInscriptionEfacture, "Incription validée", null, TypeEtatDestinataire.INSCRIT, "mafalda@gautier.me", null);
			}
		});

		final Party party = service.getParty(ids.pp, EnumSet.allOf(InternalPartyPart.class));
		assertNotNull(party);

		{
			assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			assertEquals(ids.pp, np.getNumber());
			assertEquals("Mafalda Henriette", np.getFirstNames());
			assertEquals("Mafalda", np.getFirstName());
			assertEquals("Gautier", np.getOfficialName());
			assertEquals("Dupont", np.getBirthName());
			assertEquals(Sex.FEMALE, np.getSex());
			assertEquals("Madame", np.getSalutation());
			assertEquals("Madame", np.getFormalGreeting());

			final List<Nationality> nationalities = np.getNationalities();
			assertNotNull(nationalities);
			assertEquals(1, nationalities.size());
			{
				final Nationality nat = nationalities.get(0);
				assertNotNull(nat);
				assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(nat.getDateFrom()));
				assertNull(nat.getDateTo());
				assertNull(nat.getSwiss());
				assertNull(nat.getStateless());
				assertEquals((Integer) MockPays.France.getNoOFS(), nat.getForeignCountry());
			}

			final List<Origin> origins = np.getOrigins();
			assertNotNull(origins);
			assertEquals(1, origins.size());
			{
				final Origin orig = origins.get(0);
				assertNotNull(orig);
				assertEquals("BE", orig.getCanton().value());
				assertEquals("Bern", orig.getOriginName());
			}

			final List<TaxDeclaration> decls = np.getTaxDeclarations();
			assertNotNull(decls);
			assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				assertNotNull(decl);
				assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				assertEquals(date(anneeDI, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				assertNotNull(deadlines);
				assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					assertNotNull(deadline);
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					assertEquals(dateDelaiDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				assertNotNull(statuses);
				assertEquals(1, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertNull(status.getFee());
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
			}

			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getDateOfBirth()));
			assertNull(np.getDateOfDeath());

			final List<NaturalPersonCategory> cats = np.getCategories();
			assertNotNull(cats);
			assertEquals(1, cats.size());
			{
				final NaturalPersonCategory cat = cats.get(0);
				assertNotNull(cat);
				assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, cat.getCategory());
				assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(cat.getDateFrom()));
				assertNull(cat.getDateTo());
			}

			final List<EbillingStatus> ebillingStatuses = np.getEbillingStatuses();
			assertNotNull(ebillingStatuses);
			assertEquals(2, ebillingStatuses.size());
			{
				final EbillingStatus st = ebillingStatuses.get(0);
				assertNotNull(st);
				assertEquals(EbillingStatusType.NOT_REGISTERED, st.getType());
				assertNull(st.getSince());
			}
			{
				final EbillingStatus st = ebillingStatuses.get(1);
				assertNotNull(st);
				assertEquals(EbillingStatusType.REGISTERED, st.getType());
				assertEquals(dateInscriptionEfacture, XmlUtils.xmlcal2date(st.getSince()));
			}

			final List<WithholdingTaxationPeriod> wtps = np.getWithholdingTaxationPeriods();
			assertNotNull(wtps);
			assertEquals(2, wtps.size());
			{
				final WithholdingTaxationPeriod wtp = wtps.get(0);
				assertNotNull(wtp);
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}
			{
				final WithholdingTaxationPeriod wtp = wtps.get(1);
				assertNotNull(wtp);
				assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}

			assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityStartDate()));
			assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityEndDate()));

			final List<TaxResidence> fors = np.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateTo()));
				assertEquals(LiabilityChangeReason.MAJORITY, ff.getStartReason());
				assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, ff.getEndReason());
				assertEquals(TaxType.INCOME_WEALTH, ff.getTaxType());
				assertEquals(MockCommune.Lausanne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertEquals(TaxationMethod.ORDINARY, ff.getTaxationMethod());
				assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = np.getRelationsBetweenParties();
			assertNotNull(rels);
			assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				assertNotNull(rel);
				assertNull(rel.getCancellationDate());
				assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				assertEquals(ids.dpi, rel.getOtherPartyNumber());
				assertTrue(rel instanceof TaxableRevenue);
				assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}

			final List<TaxLiability> tls = np.getTaxLiabilities();
			assertNotNull(tls);
			assertEquals(1, tls.size());
			{
				final TaxLiability tl = tls.get(0);
				assertNotNull(tl);
				assertEquals(OrdinaryResident.class, tl.getClass());
				assertEquals(date(dateNaissance.addYears(18).year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateTo()));
				assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
				assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, tl.getEndReason());
			}

			final List<TaxationPeriod> tps = np.getTaxationPeriods();
			assertNotNull(tps);
			assertEquals(7, tps.size());
			{
				final TaxationPeriod tp = tps.get(0);
				assertNotNull(tp);
				assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(1);
				assertNotNull(tp);
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(2);
				assertNotNull(tp);
				assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(3);
				assertNotNull(tp);
				assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(4);
				assertNotNull(tp);
				assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(5);
				assertNotNull(tp);
				assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertEquals((Long) ids.di, tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(6);
				assertNotNull(tp);
				assertEquals(date(2014, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}

			final List<ResidencyPeriod> residencyPeriods = np.getResidencyPeriods();
			assertNotNull(residencyPeriods);
			assertEquals(1, residencyPeriods.size());
			{
				final ResidencyPeriod rp = residencyPeriods.get(0);
				assertNotNull(rp);
				assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(rp.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(rp.getDateTo()));
			}

			final List<LandRight> landRights = np.getLandRights();
			assertNotNull(landRights);
			assertEquals(1, landRights.size());
			{
				final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
				assertNotNull(landRight0);
				assertNull(landRight0.getCommunityId());
				assertEquals(OwnershipType.SOLE_OWNERSHIP, landRight0.getType());
				assertShare(1, 1, landRight0.getShare());
				assertEquals(date(2004, 4, 12), ch.vd.unireg.xml.DataHelper.xmlToCore(landRight0.getDateFrom()));
				assertNull(landRight0.getDateTo());
				assertEquals("Achat", landRight0.getStartReason());
				assertNull(landRight0.getEndReason());
				assertCaseIdentifier(123, "2004/202/3", landRight0.getCaseIdentifier());
				assertEquals(ids.immeuble, landRight0.getImmovablePropertyId());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero().intValue();
			ids1.dpi = dpi.getNumero().intValue();
			ids1.di = di.getId();
			return ids1;
		});

		final Party party = service.getParty(ids.dpi, EnumSet.allOf(InternalPartyPart.class));
		assertNotNull(party);
		{
			assertEquals(Debtor.class, party.getClass());

			final Debtor dpi = (Debtor) party;
			assertEquals(ids.dpi, dpi.getNumber());
			assertEquals("MonTestAdoré", dpi.getName());

			final List<TaxDeclaration> decls = dpi.getTaxDeclarations();
			assertNotNull(decls);
			assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				assertNotNull(decl);
				assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				assertEquals(date(anneeDI, 1, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				assertNotNull(deadlines);
				assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					assertNotNull(deadline);
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					assertEquals(dateDelaiLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				assertNotNull(statuses);
				assertEquals(2, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertNull(status.getFee());
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
				{
					final TaxDeclarationStatus status = statuses.get(1);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertNull(status.getFee());
					assertEquals(dateSommationLR.addDays(3), ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, status.getType());
				}
			}

			final List<DebtorPeriodicity> periodicities = dpi.getPeriodicities();
			assertNotNull(periodicities);
			assertEquals(1, periodicities.size());
			{
				final DebtorPeriodicity periodicity = periodicities.get(0);
				assertNotNull(periodicity);
				assertNull(periodicity.getCancellationDate());
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(periodicity.getDateFrom()));
				assertNull(periodicity.getDateTo());
				assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, periodicity.getPeriodicity());
				assertNull(periodicity.getSpecificPeriod());
			}

			assertNull(dpi.getAssociatedTaxpayerNumber());
			assertEquals(DebtorCategory.REGULAR, dpi.getCategory());
			assertEquals(CommunicationMode.UPLOAD, dpi.getCommunicationMode());
			assertFalse(dpi.isWithoutReminder());
			assertFalse(dpi.isWithoutWithholdingTaxDeclaration());

			assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(dpi.getActivityStartDate()));
			assertNull(dpi.getActivityEndDate());

			final List<TaxResidence> fors = dpi.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertNull(ff.getDateTo());
				assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, ff.getStartReason());
				assertNull(ff.getEndReason());
				assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, ff.getTaxType());
				assertEquals(MockCommune.Aubonne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertNull(ff.getTaxationMethod());
				assertNull(ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = dpi.getRelationsBetweenParties();
			assertNotNull(rels);
			assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				assertNotNull(rel);
				assertNull(rel.getCancellationDate());
				assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				assertEquals(ids.pp, rel.getOtherPartyNumber());
				assertTrue(rel instanceof TaxableRevenue);
				assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
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


		final long idpm = doInNewTransactionAndSession(status -> {
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
			addForSecondaire(entreprise, date(2005, 3, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Cossonay, MotifRattachement.ETABLISSEMENT_STABLE, GenreImpot.BENEFICE_CAPITAL);

			addBouclement(entreprise, date(2001, 6, 1), DayMonth.get(6, 30), 12);
			return entreprise.getNumero();
		});
		assertValidInteger(idpm);

		final Set<InternalPartyPart> parts = EnumSet.allOf(InternalPartyPart.class);
		final Party party = service.getParty((int) idpm, parts);
		assertNotNull(party);
		{
			assertEquals(Corporation.class, party.getClass());

			final Corporation corp = (Corporation) party;
			assertEquals((int) idpm, corp.getNumber());
			assertEquals("Ma grande entreprise", corp.getName());

			assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(corp.getActivityStartDate()));
			assertNull(corp.getActivityEndDate());

			final List<TaxResidence> prnFors = corp.getMainTaxResidences();
			assertNotNull(prnFors);
			assertEquals(1, prnFors.size());
			{
				final TaxResidence ff = prnFors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertNull(ff.getDateTo());
				assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, ff.getStartReason());
				assertNull(ff.getEndReason());
				assertEquals(TaxType.PROFITS_CAPITAL, ff.getTaxType());
				assertEquals(MockCommune.Geneve.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.OTHER_CANTON_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertNull(ff.getTaxationMethod());
				assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}
			final List<TaxResidence> secFors = corp.getOtherTaxResidences();
			assertNotNull(secFors);
			assertEquals(1, secFors.size());
			{
				final TaxResidence ff = secFors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(date(2005, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertNull(ff.getDateTo());
				assertEquals(LiabilityChangeReason.START_COMMERCIAL_EXPLOITATION, ff.getStartReason());
				assertNull(ff.getEndReason());
				assertEquals(TaxType.PROFITS_CAPITAL, ff.getTaxType());
				assertEquals(MockCommune.Cossonay.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertNull(ff.getTaxationMethod());
				assertEquals(TaxLiabilityReason.STABLE_ESTABLISHMENT, ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}

			final List<Capital> caps = corp.getCapitals();
			assertNotNull(caps);
			assertEquals(3, caps.size());
			{
				final Capital cap = caps.get(0);
				assertNotNull(cap);
				assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateTo()));
				assertNotNull(cap.getPaidInCapital());
				assertEquals(1000L, cap.getPaidInCapital().getAmount());
				assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}
			{
				final Capital cap = caps.get(1);
				assertNotNull(cap);
				assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				assertEquals(date(2013, 5, 12), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateTo()));
				assertNotNull(cap.getPaidInCapital());
				assertEquals(1100L, cap.getPaidInCapital().getAmount());
				assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}
			{
				final Capital cap = caps.get(2);
				assertNotNull(cap);
				assertEquals(date(2013, 5, 13), ch.vd.unireg.xml.DataHelper.xmlToCore(cap.getDateFrom()));
				assertNull(cap.getDateTo());
				assertNotNull(cap.getPaidInCapital());
				assertEquals(100000L, cap.getPaidInCapital().getAmount());
				assertEquals("CHF", cap.getPaidInCapital().getCurrency());
			}

			final List<ch.vd.unireg.xml.party.corporation.v5.LegalForm> lfs = corp.getLegalForms();
			assertNotNull(lfs);
			assertEquals(1, lfs.size());
			{
				final ch.vd.unireg.xml.party.corporation.v5.LegalForm lf = lfs.get(0);
				assertNotNull(lf);
				assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(lf.getDateFrom()));
				assertNull(lf.getDateTo());
				assertEquals(LegalForm.LIMITED_LIABILITY_COMPANY, lf.getType());
				assertEquals("Société à responsabilité limitée", lf.getLabel());
				assertEquals(LegalFormCategory.CAPITAL_COMPANY, lf.getLegalFormCategory());
			}

			final RegDate today = RegDate.get();
			final List<BusinessYear> bys = corp.getBusinessYears();
			assertNotNull(bys);
			final int nbExpectedExercices = today.year() - 2000 + (today.month() > 6 ? 1 : 0);
			assertEquals(nbExpectedExercices, bys.size());
			for (int i = 0; i < nbExpectedExercices; ++i) {
				final BusinessYear by = bys.get(i);
				assertNotNull(by);
				if (i == 0) {
					assertEquals(date(2000, 3, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateFrom()));
				}
				else {
					assertEquals(date(2000 + i, 7, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateFrom()));
				}
				assertEquals(date(2001 + i, 6, 30), ch.vd.unireg.xml.DataHelper.xmlToCore(by.getDateTo()));
			}

			final List<CorporationFlag> flags = corp.getCorporationFlags();
			assertNotNull(flags);
			assertEquals(1, flags.size());
			{
				final CorporationFlag flag = flags.get(0);
				assertNotNull(flag);
				assertEquals(date(2010, 6, 2), ch.vd.unireg.xml.DataHelper.xmlToCore(flag.getDateFrom()));
				assertEquals(date(2013, 5, 26), ch.vd.unireg.xml.DataHelper.xmlToCore(flag.getDateTo()));
				assertEquals(CorporationFlagType.PUBLIC_INTEREST, flag.getType());
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
			assertNotNull(res);
			assertEquals(max, res.getEntries().size());
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
				fail("Nombre de tiers demandés trop élevé... L'appel aurait dû échouer.");
			}
			catch (BadRequestException e) {
				assertEquals("Le nombre de tiers demandés ne peut dépasser " + max, e.getMessage());
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
			assertNotNull(res);
			assertEquals(max, res.getEntries().size());
		}
	}

	@Test
	public void testGetParties() throws Exception {

		final long noEntreprise = 423672L;
		final RegDate pmActivityStartDate = date(2000, 1, 1);

		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				final MockEntrepriseCivile ent = MockEntrepriseFactory.createSimpleEntrepriseRC(noEntreprise, noEntreprise + 1011, "Au petit coin", pmActivityStartDate, null,
				                                                                                FormeLegale.N_0106_SOCIETE_ANONYME, MockCommune.Cossonay, "CHE123456788");
				addEntreprise(ent);
			}
		});

		final class Ids {
			int pp1;
			int pp2;
			int ppProtege;
			int pm;
			int ac;
		}

		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp1 = addNonHabitant("Daubet", "Philibert", null, Sexe.MASCULIN);
			final PersonnePhysique pp2 = addNonHabitant("Baudet", "Ernestine", null, Sexe.FEMININ);
			final PersonnePhysique ppProtege = addNonHabitant("Knox", "Fort", null, null);
			final DroitAcces da = new DroitAcces();
			da.setDateDebut(date(2000, 1, 1));
			da.setNiveau(Niveau.LECTURE);
			da.setVisaOperateur("zai455");
			da.setType(TypeDroitAcces.AUTORISATION);
			da.setTiers(ppProtege);
			hibernateTemplate.merge(da);

			final Entreprise entreprise = addEntrepriseConnueAuCivil(noEntreprise);

			final AutreCommunaute ac = addAutreCommunaute("Tata!!");
			ac.setFormeJuridique(FormeJuridique.ASS);
			final IdentificationEntreprise ide = new IdentificationEntreprise();
			ide.setNumeroIde("CHE999999996");
			ac.addIdentificationEntreprise(ide);

			final Ids ids1 = new Ids();
			ids1.pp1 = pp1.getNumero().intValue();
			ids1.pp2 = pp2.getNumero().intValue();
			ids1.ppProtege = ppProtege.getNumero().intValue();
			ids1.pm = entreprise.getNumero().intValue();
			ids1.ac = ac.getNumero().intValue();
			return ids1;
		});

		AuthenticationHelper.pushPrincipal("TOTO", 22);
		try {
			final Parties parties = service.getParties(Arrays.asList(ids.pp1, ids.ppProtege, 99999, ids.pp2, ids.pm, ids.ac), null);
			assertNotNull(parties);
			assertNotNull(parties.getEntries());
			assertEquals(6, parties.getEntries().size());

			final List<Entry> sorted = new ArrayList<>(parties.getEntries());
			sorted.sort(Comparator.comparingInt(Entry::getPartyNo));

			{
				final Entry e = sorted.get(0);
				assertNotNull(e.getParty());
				assertNull(e.getError());
				assertEquals(Corporation.class, e.getParty().getClass());
				assertEquals(ids.pm, e.getPartyNo());
				assertEquals(ids.pm, e.getParty().getNumber());

				final Corporation corp = (Corporation) e.getParty();
				assertNotNull(corp.getUidNumbers());
				assertEquals(1, corp.getUidNumbers().getUidNumber().size());
				assertEquals("CHE123456788", corp.getUidNumbers().getUidNumber().get(0));
			}
			{
				final Entry e = sorted.get(1);
				assertNull(e.getParty());
				assertNotNull(e.getError());
				assertEquals(99999, e.getPartyNo());
				assertEquals("Le tiers n°999.99 n'existe pas", e.getError().getErrorMessage());
				assertEquals(ErrorType.BUSINESS, e.getError().getType());
			}
			{
				final Entry e = sorted.get(2);
				assertNotNull(e.getParty());
				assertNull(e.getError());
				assertEquals(OtherCommunity.class, e.getParty().getClass());
				assertEquals(ids.ac, e.getPartyNo());
				assertEquals(ids.ac, e.getParty().getNumber());

				final OtherCommunity otherComm = (OtherCommunity) e.getParty();
				assertNotNull(otherComm.getUidNumbers());
				assertEquals(1, otherComm.getUidNumbers().getUidNumber().size());
				assertEquals("CHE999999996", otherComm.getUidNumbers().getUidNumber().get(0));
			}
			{
				final Entry e = sorted.get(3);
				assertNotNull(e.getParty());
				assertNull(e.getError());
				assertEquals(NaturalPerson.class, e.getParty().getClass());
				assertEquals(ids.pp1, e.getPartyNo());
				assertEquals(ids.pp1, e.getParty().getNumber());
			}
			{
				final Entry e = sorted.get(4);
				assertNotNull(e.getParty());
				assertNull(e.getError());
				assertEquals(NaturalPerson.class, e.getParty().getClass());
				assertEquals(ids.pp2, e.getPartyNo());
				assertEquals(ids.pp2, e.getParty().getNumber());
			}
			{
				final Entry e = sorted.get(5);
				assertNull(e.getParty());
				assertNotNull(e.getError());
				assertEquals(ids.ppProtege, e.getPartyNo());
				assertEquals("L'utilisateur TOTO/22 ne possède aucun droit de lecture sur le dossier " + ids.ppProtege, e.getError().getErrorMessage());
				assertEquals(ErrorType.ACCESS, e.getError().getType());
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

		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.pp = pp.getNumero().intValue();
			ids1.dpi = dpi.getNumero().intValue();
			ids1.di = di.getId();
			return ids1;
		});

		efactureService.setUp(new MockEFactureService() {
			@Override
			public void init() {
				addDestinataire(ids.pp);
				addEtatDestinataire(ids.pp, dateInscriptionEfacture, "Incription validée", null, TypeEtatDestinataire.INSCRIT, "mafalda@gautier.me", null);
			}
		});

		final Parties parties = service.getParties(Arrays.asList(ids.pp, ids.dpi), EnumSet.allOf(InternalPartyPart.class));
		assertNotNull(parties);
		assertNotNull(parties.getEntries());
		assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		sorted.sort(Comparator.comparingInt(Entry::getPartyNo));

		{
			final Entry e = sorted.get(0);
			assertNotNull(e);
			assertNull(e.getError());
			assertNotNull(e.getParty());
			assertEquals(Debtor.class, e.getParty().getClass());

			final Debtor dpi = (Debtor) e.getParty();
			assertEquals(ids.dpi, dpi.getNumber());
			assertEquals("MonTestAdoré", dpi.getName());

			final List<TaxDeclaration> decls = dpi.getTaxDeclarations();
			assertNotNull(decls);
			assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				assertNotNull(decl);
				assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				assertEquals(date(anneeDI, 1, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				assertNotNull(deadlines);
				assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					assertNotNull(deadline);
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					assertEquals(dateDelaiLR, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				assertNotNull(statuses);
				assertEquals(2, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertNull(status.getFee());
					assertEquals(dateEmissionLR, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
				{
					final TaxDeclarationStatus status = statuses.get(1);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertEquals((Integer) 10, status.getFee());
					assertEquals(dateSommationLR.addDays(3), ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SUMMONS_SENT, status.getType());
				}
			}

			final List<DebtorPeriodicity> periodicities = dpi.getPeriodicities();
			assertNotNull(periodicities);
			assertEquals(1, periodicities.size());
			{
				final DebtorPeriodicity periodicity = periodicities.get(0);
				assertNotNull(periodicity);
				assertNull(periodicity.getCancellationDate());
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(periodicity.getDateFrom()));
				assertNull(periodicity.getDateTo());
				assertEquals(WithholdingTaxDeclarationPeriodicity.MONTHLY, periodicity.getPeriodicity());
				assertNull(periodicity.getSpecificPeriod());
			}

			assertNull(dpi.getAssociatedTaxpayerNumber());
			assertEquals(DebtorCategory.REGULAR, dpi.getCategory());
			assertEquals(CommunicationMode.UPLOAD, dpi.getCommunicationMode());
			assertFalse(dpi.isWithoutReminder());
			assertFalse(dpi.isWithoutWithholdingTaxDeclaration());

			assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(dpi.getActivityStartDate()));
			assertNull(dpi.getActivityEndDate());

			final List<TaxResidence> fors = dpi.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertNull(ff.getDateTo());
				assertEquals(LiabilityChangeReason.START_WITHHOLDING_ACTIVITY, ff.getStartReason());
				assertNull(ff.getEndReason());
				assertEquals(TaxType.DEBTOR_TAXABLE_INCOME, ff.getTaxType());
				assertEquals(MockCommune.Aubonne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertNull(ff.getTaxationMethod());
				assertNull(ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = dpi.getRelationsBetweenParties();
			assertNotNull(rels);
			assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				assertNotNull(rel);
				assertNull(rel.getCancellationDate());
				assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				assertEquals(ids.pp, rel.getOtherPartyNumber());
				assertTrue(rel instanceof TaxableRevenue);
				assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}
		}
		{
			final Entry o = sorted.get(1);
			assertNotNull(o);
			assertNull(o.getError());
			assertNotNull(o.getParty());
			assertEquals(NaturalPerson.class, o.getParty().getClass());

			final NaturalPerson np = (NaturalPerson) o.getParty();
			assertEquals(ids.pp, np.getNumber());
			assertEquals("Mafalda", np.getFirstName());
			assertEquals("Gautier", np.getOfficialName());
			assertEquals(Sex.FEMALE, np.getSex());
			assertEquals("Madame", np.getSalutation());
			assertEquals("Madame", np.getFormalGreeting());

			final List<TaxDeclaration> decls = np.getTaxDeclarations();
			assertNotNull(decls);
			assertEquals(1, decls.size());
			{
				final TaxDeclaration decl = decls.get(0);
				assertNotNull(decl);
				assertEquals(date(anneeDI, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateFrom()));
				assertEquals(date(anneeDI, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(decl.getDateTo()));
				assertNull(decl.getCancellationDate());

				final List<TaxDeclarationDeadline> deadlines = decl.getDeadlines();
				assertNotNull(deadlines);
				assertEquals(1, deadlines.size());
				{
					final TaxDeclarationDeadline deadline = deadlines.get(0);
					assertNotNull(deadline);
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getApplicationDate()));
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getProcessingDate()));
					assertEquals(dateDelaiDI, ch.vd.unireg.xml.DataHelper.xmlToCore(deadline.getDeadline()));
					assertNull(deadline.getCancellationDate());
				}

				final List<TaxDeclarationStatus> statuses = decl.getStatuses();
				assertNotNull(statuses);
				assertEquals(1, statuses.size());
				{
					final TaxDeclarationStatus status = statuses.get(0);
					assertNotNull(status);
					assertNull(status.getCancellationDate());
					assertNull(status.getSource());
					assertNull(status.getFee());
					assertEquals(dateEmissionDI, ch.vd.unireg.xml.DataHelper.xmlToCore(status.getDateFrom()));
					assertEquals(TaxDeclarationStatusType.SENT, status.getType());
				}
			}

			assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getDateOfBirth()));
			assertNull(np.getDateOfDeath());

			final List<NaturalPersonCategory> cats = np.getCategories();
			assertNotNull(cats);
			assertEquals(1, cats.size());
			{
				final NaturalPersonCategory cat = cats.get(0);
				assertNotNull(cat);
				assertEquals(NaturalPersonCategoryType.C_03_C_PERMIT, cat.getCategory());
				assertEquals(dateNaissance, ch.vd.unireg.xml.DataHelper.xmlToCore(cat.getDateFrom()));
				assertNull(cat.getDateTo());
			}

			final List<EbillingStatus> ebillingStatuses = np.getEbillingStatuses();
			assertNotNull(ebillingStatuses);
			assertEquals(2, ebillingStatuses.size());
			{
				final EbillingStatus st = ebillingStatuses.get(0);
				assertNotNull(st);
				assertEquals(EbillingStatusType.NOT_REGISTERED, st.getType());
				assertNull(st.getSince());
			}
			{
				final EbillingStatus st = ebillingStatuses.get(1);
				assertNotNull(st);
				assertEquals(EbillingStatusType.REGISTERED, st.getType());
				assertEquals(dateInscriptionEfacture, XmlUtils.xmlcal2date(st.getSince()));
			}

			final List<WithholdingTaxationPeriod> wtps = np.getWithholdingTaxationPeriods();
			assertNotNull(wtps);
			assertEquals(2, wtps.size());
			{
				final WithholdingTaxationPeriod wtp = wtps.get(0);
				assertNotNull(wtp);
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}
			{
				final WithholdingTaxationPeriod wtp = wtps.get(1);
				assertNotNull(wtp);
				assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateFrom()));
				assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(wtp.getDateTo()));
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, wtp.getTaxationAuthority());
				assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), wtp.getTaxationAuthorityFSOId());
				assertEquals(WithholdingTaxationPeriodType.MIXED, wtp.getType());
			}

			assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityStartDate()));
			assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(np.getActivityEndDate()));

			final List<TaxResidence> fors = np.getMainTaxResidences();
			assertNotNull(fors);
			assertEquals(1, fors.size());
			{
				final TaxResidence ff = fors.get(0);
				assertNotNull(ff);
				assertNull(ff.getCancellationDate());
				assertEquals(dateNaissance.addYears(18), ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(ff.getDateTo()));
				assertEquals(LiabilityChangeReason.MAJORITY, ff.getStartReason());
				assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, ff.getEndReason());
				assertEquals(TaxType.INCOME_WEALTH, ff.getTaxType());
				assertEquals(MockCommune.Lausanne.getNoOFS(), ff.getTaxationAuthorityFSOId());
				assertEquals(TaxationAuthorityType.VAUD_MUNICIPALITY, ff.getTaxationAuthorityType());
				assertEquals(TaxationMethod.ORDINARY, ff.getTaxationMethod());
				assertEquals(TaxLiabilityReason.RESIDENCE, ff.getTaxLiabilityReason());
				assertFalse(ff.isVirtual());
			}

			final List<RelationBetweenParties> rels = np.getRelationsBetweenParties();
			assertNotNull(rels);
			assertEquals(1, rels.size());
			{
				final RelationBetweenParties rel = rels.get(0);
				assertNotNull(rel);
				assertNull(rel.getCancellationDate());
				assertEquals(dateDebutRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateFrom()));
				assertEquals(dateFinRT, ch.vd.unireg.xml.DataHelper.xmlToCore(rel.getDateTo()));
				assertEquals(ids.dpi, rel.getOtherPartyNumber());
				assertTrue(rel instanceof TaxableRevenue);
				assertNull(((TaxableRevenue) rel).getEndDateOfLastTaxableItem());
			}

			final List<TaxLiability> tls = np.getTaxLiabilities();
			assertNotNull(tls);
			assertEquals(1, tls.size());
			{
				final TaxLiability tl = tls.get(0);
				assertNotNull(tl);
				assertEquals(OrdinaryResident.class, tl.getClass());
				assertEquals(date(dateNaissance.addYears(18).year(), 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tl.getDateTo()));
				assertEquals(LiabilityChangeReason.MAJORITY, tl.getStartReason());
				assertEquals(LiabilityChangeReason.DEPARTURE_TO_FOREIGN_COUNTRY, tl.getEndReason());
			}

			final List<TaxationPeriod> tps = np.getTaxationPeriods();
			assertNotNull(tps);
			assertEquals(7, tps.size());
			{
				final TaxationPeriod tp = tps.get(0);
				assertNotNull(tp);
				assertEquals(date(2008, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2008, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(1);
				assertNotNull(tp);
				assertEquals(date(2009, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2009, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(2);
				assertNotNull(tp);
				assertEquals(date(2010, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2010, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(3);
				assertNotNull(tp);
				assertEquals(date(2011, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2011, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(4);
				assertNotNull(tp);
				assertEquals(date(2012, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2012, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(5);
				assertNotNull(tp);
				assertEquals(date(2013, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(date(2013, 12, 31), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertEquals((Long) ids.di, tp.getTaxDeclarationId());
			}
			{
				final TaxationPeriod tp = tps.get(6);
				assertNotNull(tp);
				assertEquals(date(2014, 1, 1), ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateFrom()));
				assertEquals(dateDepartHS, ch.vd.unireg.xml.DataHelper.xmlToCore(tp.getDateTo()));
				assertNull(tp.getTaxDeclarationId());
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique pp = addHabitant(noIndividu);
			final PersonnePhysique conjoint = addNonHabitant("Mariam", "Labaffe", null, Sexe.FEMININ);
			final EnsembleTiersCouple couple = addEnsembleTiersCouple(pp, conjoint, dateMariage, null);

			addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.ChateauDoex);
			addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.ChateauDoex);

			final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			addRapportPrestationImposable(dpi, pp, dateDebutRT, dateFinRT, false);

			assertValidInteger(pp.getNumero());
			assertValidInteger(conjoint.getNumero());
			final Ids ids1 = new Ids();
			ids1.ppHabitant = pp.getNumero().intValue();
			ids1.ppNonHabitant = conjoint.getNumero().intValue();
			return ids1;
		});

		final Parties parties = service.getParties(Arrays.asList(ids.ppHabitant, ids.ppNonHabitant), EnumSet.of(InternalPartyPart.WITHHOLDING_TAXATION_PERIODS));
		assertNotNull(parties);
		assertNotNull(parties.getEntries());
		assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		sorted.sort(Comparator.comparingInt(Entry::getPartyNo));

		{
			final Entry entry = sorted.get(0);
			assertEquals(ids.ppHabitant, entry.getPartyNo());

			final Party party = entry.getParty();
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			assertNotNull(np.getWithholdingTaxationPeriods());
			assertEquals(4, np.getWithholdingTaxationPeriods().size());      // on vérifie juste qu'elles sont bien là... 2003 à 2006 = 4
		}
		{
			final Entry entry = sorted.get(1);
			assertEquals(ids.ppNonHabitant, entry.getPartyNo());

			final Party party = entry.getParty();
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());

			final NaturalPerson np = (NaturalPerson) party;
			assertNotNull(np.getWithholdingTaxationPeriods());
			assertEquals(0, np.getWithholdingTaxationPeriods().size());      // elle n'a rien du tout
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
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique connu = addHabitant(noIndividu);
			final PersonnePhysique inconnu = addHabitant(noIndividuAbsent);
			final Ids ids1 = new Ids();
			ids1.ppConnu = connu.getNumero().intValue();
			ids1.ppInconnu = inconnu.getNumero().intValue();
			return ids1;
		});

		final Parties parties = service.getParties(Arrays.asList(ids.ppConnu, ids.ppInconnu), null);
		assertNotNull(parties);
		assertNotNull(parties.getEntries());
		assertEquals(2, parties.getEntries().size());

		final List<Entry> sorted = new ArrayList<>(parties.getEntries());
		sorted.sort(Comparator.comparingInt(Entry::getPartyNo));

		{
			final Entry entry = sorted.get(0);
			assertEquals(ids.ppConnu, entry.getPartyNo());

			final Party party = entry.getParty();
			assertNotNull(party);
			assertEquals(NaturalPerson.class, party.getClass());
		}
		{
			final Entry entry = sorted.get(1);
			assertEquals(ids.ppInconnu, entry.getPartyNo());

			final Party party = entry.getParty();
			assertNull(party);

			final Error error = entry.getError();
			assertEquals("Impossible de trouver l'individu n°" + noIndividuAbsent + " pour l'habitant n°" + ids.ppInconnu, error.getErrorMessage());
			assertEquals(ErrorType.BUSINESS, error.getType());
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
		final int ppId = doInNewTransactionAndSession(status -> {
			final PersonnePhysique inconnu = addHabitant(noIndividuAbsent);
			return inconnu.getNumero().intValue();
		});

		try {
			service.getParty(ppId, null);
			fail("Aurait dû partir en erreur...");
		}
		catch (IndividuNotFoundException e) {
			assertEquals("Impossible de trouver l'individu n°" + noIndividuAbsent + " pour l'habitant n°" + ppId, e.getMessage());
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
		final Ids ids = doInNewTransactionAndSession(status -> {

			final DebiteurPrestationImposable dpiSans = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			dpiSans.setAciAutreCanton(Boolean.FALSE);

			final DebiteurPrestationImposable dpiAvec = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.MENSUEL, date(2009, 1, 1));
			dpiAvec.setAciAutreCanton(Boolean.TRUE);

			final Ids ids1 = new Ids();
			ids1.dpiAvecFlag = dpiAvec.getNumero().intValue();
			ids1.dpiSansFlag = dpiSans.getNumero().intValue();
			return ids1;
		});

		// interrogation

		// cas avec flag
		final Party avec = service.getParty(ids.dpiAvecFlag, null);
		assertNotNull(avec);
		assertEquals(Debtor.class, avec.getClass());
		final Debtor avecDebtor = (Debtor) avec;
		assertTrue(avecDebtor.isOtherCantonTaxAdministration());

		// cas sans flag
		final Party sans = service.getParty(ids.dpiSansFlag, null);
		assertNotNull(sans);
		assertEquals(Debtor.class, sans.getClass());
		final Debtor sansDebtor = (Debtor) sans;
		assertFalse(sansDebtor.isOtherCantonTaxAdministration());
	}

	@Test
	public void testGetFiscalEvents() throws Exception {

		final class Ids {
			int ppAvec;
			int ppSans;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransaction(transactionStatus -> {
			final PersonnePhysique avec = addNonHabitant("Albert", "Pommery", date(1985, 3, 15), Sexe.MASCULIN);
			final ForFiscalPrincipal ffp = addForPrincipal(avec, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
			addEvenementFiscalFor(avec, ffp, date(2000, 1, 1), EvenementFiscalFor.TypeEvenementFiscalFor.OUVERTURE);

			final PersonnePhysique sans = addNonHabitant("Bartholomé", "Mazo", date(1956, 8, 3), Sexe.MASCULIN);

			final Ids ids1 = new Ids();
			ids1.ppAvec = avec.getNumero().intValue();
			ids1.ppSans = sans.getNumero().intValue();
			return ids1;
		});

		// interrogation

		// sans événement fiscal
		final FiscalEvents sans = service.getFiscalEvents(ids.ppSans);
		assertNotNull(sans);
		assertNotNull(sans.getEvents());
		assertEquals(0, sans.getEvents().size());

		// avec un événement fiscal
		final FiscalEvents avec = service.getFiscalEvents(ids.ppAvec);
		assertNotNull(avec);
		assertNotNull(avec.getEvents());
		assertEquals(1, avec.getEvents().size());
		{
			final FiscalEvent evt = avec.getEvents().get(0);
			assertNotNull(evt);
			assertEquals(AuthenticationHelper.getCurrentPrincipal(), evt.getUser());
			assertNotNull(evt.getTreatmentDate());
			assertEquals("Ouverture d'un for principal pour motif 'Arrivée de hors-Suisse'", evt.getDescription());

			final EvenementFiscal xml = evt.getEvent();
			assertNotNull(xml);
			assertEquals(OuvertureFor.class, xml.getClass());
			assertEquals(date(2000, 1, 1), DataHelper.webToRegDate(xml.getDate()));
			assertEquals(CategorieTiers.PP, xml.getCategorieTiers());
			assertEquals(ids.ppAvec, xml.getNumeroTiers());

			final OuvertureFor ouverture = (OuvertureFor) xml;
			assertEquals(LiabilityChangeReason.MOVE_IN_FROM_FOREIGN_COUNTRY, ouverture.getMotifOuverture());
		}
	}

	@Test
	public void testLabels() throws Exception {

		final String codeCollaborateur = CODE_ETIQUETTE_COLLABORATEUR;
		final String codeToto = "TOTO";

		// mise en place des étiquettes manquantes (héritage et collaborateur sont mis en place par le AbstractBusinessTest...)
		final long idCollAdmNouvelleEntite = doInNewTransaction(status -> {
			addEtiquette(codeToto, "Etiquette TOTO", TypeTiersEtiquette.PP_MC, null);

			final CollectiviteAdministrative nouvelleEntite = tiersService.getCollectiviteAdministrative(MockCollectiviteAdministrative.noNouvelleEntite);
			return nouvelleEntite.getNumero();
		});

		final class Ids {
			int prn;
			int cjt;
			int mc;
		}

		// mise en place des individus avec leur liens d'étiquette
		final Ids ids = doInNewTransactionAndSession(status -> {
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

			final Ids ids1 = new Ids();
			ids1.prn = prn.getNumero().intValue();
			ids1.cjt = cjt.getNumero().intValue();
			ids1.mc = menage.getNumero().intValue();
			return ids1;
		});

		// interrogation
		final Parties parties = service.getParties(Arrays.asList(ids.prn, ids.cjt, ids.mc), EnumSet.of(InternalPartyPart.LABELS));
		final Map<Integer, Party> mapParties = parties.getEntries().stream()
				.collect(Collectors.toMap(Entry::getPartyNo, Entry::getParty));
		assertEquals(3, mapParties.size());

		// principal
		{
			final Party party = mapParties.get(ids.prn);
			assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			assertNotNull(labels);
			assertEquals(2, labels.size());
			{
				final PartyLabel label = labels.get(0);
				assertNotNull(label);
				assertEquals(date(2008, 5, 12), DataHelper.webToRegDate(label.getDateFrom()));
				assertEquals(date(2010, 8, 31), DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeCollaborateur, label.getLabel());
				assertEquals("DS Collaborateur", label.getDisplayLabel());
				assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNotNull(ca);
				assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
			{
				final PartyLabel label = labels.get(1);
				assertNotNull(label);
				assertEquals(date(2015, 2, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertNull(DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeCollaborateur, label.getLabel());
				assertEquals("DS Collaborateur", label.getDisplayLabel());
				assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNotNull(ca);
				assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
		}

		// conjoint
		{
			final Party party = mapParties.get(ids.cjt);
			assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			assertNotNull(labels);
			assertEquals(2, labels.size());
			{
				final PartyLabel label = labels.get(0);
				assertNotNull(label);
				assertEquals(date(2005, 3, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertNull(DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeToto, label.getLabel());
				assertEquals("Etiquette TOTO", label.getDisplayLabel());
				assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(1);
				assertNotNull(label);
				assertEquals(date(2009, 6, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertEquals(date(2014, 5, 30), DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeCollaborateur, label.getLabel());
				assertEquals("DS Collaborateur", label.getDisplayLabel());
				assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNotNull(ca);
				assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
		}

		// ménage
		{
			final Party party = mapParties.get(ids.mc);
			assertNotNull(party);

			final List<PartyLabel> labels = party.getLabels();
			assertNotNull(labels);
			assertEquals(5, labels.size());
			{
				final PartyLabel label = labels.get(0);
				assertNotNull(label);
				assertEquals(date(2005, 3, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertEquals(date(2009, 12, 31), DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeToto, label.getLabel());
				assertEquals("Etiquette TOTO", label.getDisplayLabel());
				assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(1);
				assertNotNull(label);
				assertEquals(date(2008, 5, 12), DataHelper.webToRegDate(label.getDateFrom()));
				assertEquals(date(2014, 5, 30), DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeCollaborateur, label.getLabel());
				assertEquals("DS Collaborateur", label.getDisplayLabel());
				assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNotNull(ca);
				assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
			}
			{
				final PartyLabel label = labels.get(2);
				assertNotNull(label);
				assertEquals(date(2010, 1, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertEquals(date(2010, 7, 15), DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeToto, label.getLabel());
				assertEquals("Etiquette TOTO", label.getDisplayLabel());
				assertFalse(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(3);
				assertNotNull(label);
				assertEquals(date(2010, 7, 16), DataHelper.webToRegDate(label.getDateFrom()));
				assertNull(DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeToto, label.getLabel());
				assertEquals("Etiquette TOTO", label.getDisplayLabel());
				assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNull(ca);
			}
			{
				final PartyLabel label = labels.get(4);
				assertNotNull(label);
				assertEquals(date(2015, 2, 1), DataHelper.webToRegDate(label.getDateFrom()));
				assertNull(DataHelper.webToRegDate(label.getDateTo()));
				assertEquals(codeCollaborateur, label.getLabel());
				assertEquals("DS Collaborateur", label.getDisplayLabel());
				assertTrue(label.isVirtual());
				final AdministrativeAuthorityLink ca = label.getAdministrativeAuthority();
				assertNotNull(ca);
				assertEquals(MockCollectiviteAdministrative.noNouvelleEntite, ca.getAdministrativeAuthorityId());
				assertEquals(idCollAdmNouvelleEntite, ca.getPartyNumber());
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
		final Party party = service.getParty(ids.decede.intValue(), Collections.singleton(InternalPartyPart.RELATIONS_BETWEEN_PARTIES));
		assertNotNull(party);

		// on doit bien recevoir tous les rapports-entre-tiers *sauf* les parentés et les relations d'héritages
		// qui - pour des raisons de comptabilité ascendante - ne sont exposés que sur demande explicite.
		final List<RelationBetweenParties> relations = party.getRelationsBetweenParties();
		assertNotNull(relations);
		assertEquals(1, relations.size());
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
		final Party party = service.getParty(ids.decede.intValue(), Collections.singleton(InternalPartyPart.INHERITANCE_RELATIONSHIPS));
		assertNotNull(party);

		final List<RelationBetweenParties> relations = party.getRelationsBetweenParties();
		assertNotNull(relations);
		assertEquals(2, relations.size());
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
		assertNotNull(immo);
		assertTrue(immo instanceof RealEstate);

		final RealEstate realEstate = (RealEstate) immo;
		assertEquals(id.longValue(), realEstate.getId());
		assertEquals("some egrid", realEstate.getEgrid());
		assertNull(realEstate.getCancellationDate());

		final List<Location> locations = realEstate.getLocations();
		assertEquals(1, locations.size());
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
		assertNotNull(entries);
		assertEquals(3, entries.size());
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
		assertNotNull(building);
		assertEquals(ids.batiment, building.getId());

		final List<BuildingDescription> descriptions = building.getDescriptions();
		assertEquals(1, descriptions.size());
		final BuildingDescription description0 = descriptions.get(0);
		assertEquals(RegDate.get(2000, 1, 1), DataHelper.webToRegDate(description0.getDateFrom()));
		assertNull(description0.getDateTo());
		assertEquals("Centrale électrique", description0.getType());
		assertEquals(Integer.valueOf(300), description0.getArea());

		final List<BuildingSetting> settings = building.getSettings();
		assertEquals(1, settings.size());
		final BuildingSetting setting0 = settings.get(0);
		assertEquals(RegDate.get(2000, 1, 1), DataHelper.webToRegDate(setting0.getDateFrom()));
		assertNull(setting0.getDateTo());
		assertEquals(Integer.valueOf(310), setting0.getArea());
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
		assertNotNull(entries);
		assertEquals(3, entries.size());
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
		assertNotNull(community);
		assertEquals(ids.communaute, community.getId());
		assertEquals(CommunityOfOwnersType.COMMUNITY_OF_PROPERTY, community.getType());

		final List<RightHolder> members = community.getMembers();
		assertEquals(2, members.size());
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
		assertNotNull(list);

		// on vérifie qu'on reçoit bien trois réponses
		final List<CommunityOfOwnersEntry> entries = list.getEntries();
		assertNotNull(entries);
		assertEquals(3, entries.size());
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
		assertNull(service.getCommunityOfHeirs(-1));

		// on vérifie qu'on reçoit bien trois réponses
		final CommunityOfHeirs community = service.getCommunityOfHeirs((int) ids.defunt);
		assertNotNull(community);
		assertEquals(ids.defunt, community.getInheritedFromNumber());
		assertEquals(RegDate.get(2016, 5, 13), DataHelper.webToRegDate(community.getInheritanceDateFrom()));

		final List<CommunityOfHeirMember> members = community.getMembers();
		assertNotNull(members);
		assertEquals(2, members.size());
		assertMember(ids.heritier1, RegDate.get(2016, 5, 13), null, null, members.get(0));
		assertMember(ids.heritier2, RegDate.get(2016, 5, 15), null, null, members.get(1));

		final List<CommunityOfHeirLeader> leaders = community.getLeaders();
		assertNotNull(leaders);
		assertEquals(1, leaders.size());
		assertLeader(ids.heritier1, RegDate.get(2016, 5, 13), null, null, leaders.get(0));
	}

	@Test
	public void testValidateGroupDeadlineRequest() throws Exception {

		// on créé des tiers avec des situations différentes
		final int ctbIdInconnu = 12300301;
		final Long debiteurId = doInNewTransaction(status -> addDebiteur().getNumero());
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));

			return pp.getNumero();
		});


		final GroupDeadlineValidationRequest request = new GroupDeadlineValidationRequest(2017, Arrays.asList((int) ctbId, ctbIdInconnu, debiteurId.intValue()));
		final GroupDeadlineValidationResponse response = service.validateGroupDeadlineRequest(request);
		assertNotNull(response);

		final List<ValidationResult> results = response.getValidationResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		assertValidationSuccess(ctbId, PartyType.NATURAL_PERSON, date(2017, 1, 1), date(2017, 12, 31), 1, Collections.singletonList(date(2018, 6, 30)), results.get(0));
		assertIneligibleError(ctbIdInconnu, null, "Le contribuable n'existe pas.", results.get(1));
		assertIneligibleError(debiteurId, PartyType.DEBTOR, "Le tiers n'est pas un contribuable (Débiteur prestation imposable).", results.get(2));
	}

	@Test
	public void testValidateGroupDeadlineRequestDelaiExistantImplicite() throws Exception {

		// on créé un tiers avec un délai implicite (par exemple : automatiquement créé par le batch d'émission des DIs) au 30 juin
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));  // <--- délai avec un LOG_MUSER différent de JMS-EvtDelaisDeclaration
			return pp.getNumero();
		});

		final GroupDeadlineValidationRequest request = new GroupDeadlineValidationRequest(2017, Collections.singletonList((int) ctbId));
		final GroupDeadlineValidationResponse response = service.validateGroupDeadlineRequest(request);
		assertNotNull(response);

		// Il doit être possible d'ajouter un délai (= de rendre le délai explicite) au 30 juin, car le délai existant est implicite
		final List<ValidationResult> results = response.getValidationResults();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertValidationSuccess(ctbId, PartyType.NATURAL_PERSON, date(2017, 1, 1), date(2017, 12, 31), 1, Collections.singletonList(date(2018, 6, 30)), results.get(0));
	}

	@Test
	public void testValidateGroupDeadlineRequestDelaiExistantExpliciteEDelai() throws Exception {

		// on créé un tiers avec un délai explicite (c'est-à-dire déjà demandé par e-Délai) au 30 juin
		setAuthentication(DemandeDelaisDeclarationsHandler.PRINCIPAL);
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30))); // <--- délai avec un LOG_MUSER égal à JMS-EvtDelaisDeclaration
			return pp.getNumero();
		});

		final GroupDeadlineValidationRequest request = new GroupDeadlineValidationRequest(2017, Collections.singletonList((int) ctbId));
		final GroupDeadlineValidationResponse response = service.validateGroupDeadlineRequest(request);
		assertNotNull(response);

		// Il ne doit pas être possible de demander un nouveau délai au 30 juin, car le délai est déjà explicite
		final List<ValidationResult> results = response.getValidationResults();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertValidationError(ctbId, PartyType.NATURAL_PERSON, "09", "Il y a déjà un délai accordé au 30.06.2018.", date(2017, 1, 1), date(2017, 12, 31), 1, results.get(0));
	}

	@Test
	public void testValidateGroupDeadlineRequestDelaiExistantExpliciteZaixxx() throws Exception {

		// on créé un tiers avec un délai explicite (c'est-à-dire déjà demandé par un utilisateur zaixxx) au 30 juin
		setAuthentication("zaixxx");
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30))); // <--- délai avec un LOG_MUSER égal à zaixxx
			return pp.getNumero();
		});

		final GroupDeadlineValidationRequest request = new GroupDeadlineValidationRequest(2017, Collections.singletonList((int) ctbId));
		final GroupDeadlineValidationResponse response = service.validateGroupDeadlineRequest(request);
		assertNotNull(response);

		// Il ne doit pas être possible de demander un nouveau délai au 30 juin, car le délai est déjà explicite
		final List<ValidationResult> results = response.getValidationResults();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertValidationError(ctbId, PartyType.NATURAL_PERSON, "09", "Il y a déjà un délai accordé au 30.06.2018.", date(2017, 1, 1), date(2017, 12, 31), 1, results.get(0));
	}

	@Test
	public void testValidateGroupDeadlineRequestDelaiExistantAvecDemandeMandataire() throws Exception {

		// on créé un tiers avec un délai lié à une demande mandataire
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DelaiDocumentFiscal delai = newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30));
			delai.setDemandeMandataire(addDemandeMandataire("CHE1", "11111", "test"));

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(delai);
			return pp.getNumero();
		});

		final GroupDeadlineValidationRequest request = new GroupDeadlineValidationRequest(2017, Collections.singletonList((int) ctbId));
		final GroupDeadlineValidationResponse response = service.validateGroupDeadlineRequest(request);
		assertNotNull(response);

		// Il ne doit pas être possible de demander un nouveau délai au 30 juin, car le délai possède déjà une demande mandataire
		final List<ValidationResult> results = response.getValidationResults();
		assertNotNull(results);
		assertEquals(1, results.size());
		assertValidationError(ctbId, PartyType.NATURAL_PERSON, "09", "Il y a déjà un délai accordé au 30.06.2018.", date(2017, 1, 1), date(2017, 12, 31), 1, results.get(0));
	}

	@Test
	public void testValidateDeadlineRequestContribuableInconnu() throws Exception {

		final int ctbId = 12300301;

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);

		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, ctbId);
			assertIneligibleError(ctbId, null, "Le contribuable n'existe pas.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestDebiteur() throws Exception {

		final long ctbId = 1230001;

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);

		doInNewTransaction(status -> {

			// on crée un débiteur
			addDebiteur(ctbId);

			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.DEBTOR, "Le tiers n'est pas un contribuable (Débiteur prestation imposable).", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableAnnule() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);

		// on crée un contribuable annulé
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			pp.setAnnule(true);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Le contribuable est annulé.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableInvalide() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);

		// on crée un contribuable invalide
		final long ctbId = doInNewTransactionAndSessionWithoutValidation(status -> {
			PersonnePhysique pp = addNonHabitant("Rodolf", "Piedbor", date(1953, 12, 18), Sexe.MASCULIN);
			addForPrincipal(pp, date(1971, 12, 18), MotifFor.MAJORITE, MockCommune.Lausanne);
			// le for secondaire n'est pas couvert par le for principal
			addForSecondaire(pp, date(1920, 1, 1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Leysin, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Une incohérence de données sur le contribuable empêche sa modification (validation).", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableNonAssujetti() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable non-assujetti
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Le contribuable n'est pas éligible car il n'a pas de période d'imposition en 2018.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuablePasAssujettiAuRole() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable hors-canton avec un immeuble
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2000,1,1), MotifFor.ACHAT_IMMOBILIER, MockPays.France);
			addForSecondaire(pp, date(2000,1,1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Bex, MotifRattachement.IMMEUBLE_PRIVE);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Le contribuable n'est pas éligible car il n'est pas assujetti au rôle de manière illimitée en 2018 (hors Suisse).", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestPPPasAssujettiDeManiereIllimitee() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable PP vaudois parti HS dans l'année
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2000, 1, 1), MotifFor.ARRIVEE_HC, date(2018, 8, 22), MotifFor.DEPART_HS, MockCommune.Bex);
			addForPrincipal(pp, date(2018, 8, 23), MotifFor.DEPART_HS, MockPays.Liechtenstein);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Le contribuable n'est pas éligible car il n'est plus imposé en fin de période fiscale 2018.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestPMPasAssujettiDeManiereIllimitee() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);

		// on crée un contribuable PM vaudois parti HS dans l'année
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(12, 31), 12);              // tous les 31.12 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), date(2018, 8, 23), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), date(2018, 8, 23), MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, date(2018, 8, 22), MotifFor.DEPART_HS, MockCommune.Bex);
			return pm.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2018, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.CORPORATION, "Le contribuable n'est pas éligible car il n'est plus imposé à la date de son prochain bouclement pour la période fiscale 2018.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableSansDeclaration() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti mais sans déclaration (cas métier : contribuable dont l'arrivée
		// a été traité tardivement et qui possède des tâches d'émission des DIs pas encore traitées)
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);
			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.NATURAL_PERSON, "01", "Il n'existe aucune déclaration sur la période 2017.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableAvecDeclarationAnnulee() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec une déclaration annulée
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);
			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.setAnnule(true);

			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.NATURAL_PERSON, "02", "La déclaration existante sur la période 2017 est annulée.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableAvecUneDeclarationRetournee() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec un déclaration déjà retournée
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addEtat(new EtatDeclarationRetournee(date(2018, 5, 1), "TEST"));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));

			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.NATURAL_PERSON, "04", "La déclaration est déjà retournée sur la période 2017.",
			                      date(2017, 1, 1), date(2017, 12, 31), 1, results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestPPAvecUneDeclarationAvecUnDelaiDejaOctroye() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec un déclaration émise et un délai déjà octroyé
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));
			di.addDelai(newDelaiDeclaration(date(2018, 4, 16), date(2018, 11, 1)));   // <--- délai déjà octroyé

			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.NATURAL_PERSON, "09", "Il y a déjà un délai accordé au 01.11.2018.",
			                      date(2017, 1, 1), date(2017, 12, 31), 1, results);
			return null;
		});
	}

	/**
	 * [FISCPROJ-753] Ce test vérifie qu'une erreur est retournée s'il existe déjà un délai au-delà du délai par défaut sur une PM.
	 */
	@Test
	public void testValidateDeadlineRequestPMAvecUneDeclarationAvecUnDelaiDejaOctroye() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);
		service.setPeriodeFiscaleDAO(periodeFiscaleDAO);
		service.setDiService(diService);

		final RegDate dateBouclement = RegDate.get(2017, 10, 1);

		// on crée un contribuable PM vaudois avec un délai déjà octroyé au-délà de la valeur par défaut
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2009, 7, 15), DayMonth.get(9, 30), 12);              // bouclements aux 30 septembre depuis 2009
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 4, 16), dateBouclement.addDays(255)));   // <-- délai par défaut (2018-06-12)
			di.addDelai(newDelaiDeclaration(date(2018, 5, 1), RegDate.get(2018, 6, 25)));       // <-- délai supplémentaire au-delà du delai défaut

			return pm.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.CORPORATION, "09", "Il y a déjà un délai accordé au 25.06.2018.",
			                      date(2017, 1, 1), date(2017, 12, 31), 1, results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestPPAvecUneDeclarationEmise() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec un déclaration émise toute propre
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 11, 8), MotifFor.ARRIVEE_HC, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));

			return pp.getNumero();
		});

		// il doit être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationSuccess(ctbId, PartyType.NATURAL_PERSON, date(2017, 1, 1), date(2017, 12, 31), 1, Collections.singletonList(date(2018, 6, 30)), results);
			return null;
		});
	}

	/**
	 * [FISCPROJ-753][FISCPROJ-862] Ce test vérifie que le délai accordé pour une PM est : date de bouclement + 6 mois + 75 jours calendaires.
	 */
	@Test
	public void testValidateDeadlineRequestPMBouclement31Mars() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);
		service.setPeriodeFiscaleDAO(periodeFiscaleDAO);
		service.setDiService(diService);

		// on crée un contribuable PM vaudois
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(3, 31), 12);              // tous les 31.03 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2016, 4, 1), date(2017, 3, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2017, 5, 1)));
			di.addDelai(newDelaiDeclaration(date(2017, 5, 1), date(2017, 8, 1)));

			return pm.getNumero();
		});

		// [FISCPROJ-862] le délai proposé doit être le 14.12.2017 (31.3.2017 + 6 mois = 30.09.2017 ; 30.09.2017 + 75 jours = 14.12.2017)
		final RegDate dateBouclement = RegDate.get(2017, 3, 31);
		final RegDate dateDelai = dateBouclement.addMonths(6).addDays(75);
		assertEquals(RegDate.get(2017, 12, 14), dateDelai);

		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationSuccess(ctbId, PartyType.CORPORATION, date(2016, 4, 1), date(2017, 3, 31), 1, Collections.singletonList(dateDelai), results);
			return null;
		});
	}

	/**
	 * [FISCPROJ-753][FISCPROJ-862] Ce test vérifie que le délai accordé pour une PM est : date de bouclement + 6 mois + 75 jours calendaires.
	 */
	@Test
	public void testValidateDeadlineRequestPMBouclement30Juin() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);
		service.setPeriodeFiscaleDAO(periodeFiscaleDAO);
		service.setDiService(diService);

		// on crée un contribuable PM vaudois
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(6, 30), 12);              // tous les 30.06 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2016, 7, 1), date(2017, 6, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2017, 7, 1)));
			di.addDelai(newDelaiDeclaration(date(2017, 7, 1), date(2017, 10, 1)));

			return pm.getNumero();
		});

		// [FISCPROJ-862] le délai proposé doit être le 15.03.2018 (30.06.2017 + 6 mois = 30.12.2017 ; 30.12.2017 + 75 jours = 15.03.2018)
		final RegDate dateBouclement = RegDate.get(2017, 6, 30);
		final RegDate dateDelai = dateBouclement.addMonths(6).addDays(75);
		assertEquals(RegDate.get(2018, 3, 15), dateDelai);

		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationSuccess(ctbId, PartyType.CORPORATION, date(2016, 7, 1), date(2017, 6, 30), 1, Collections.singletonList(dateDelai), results);
			return null;
		});
	}

	/**
	 * [FISCPROJ-753][FISCPROJ-862] Ce test vérifie que le délai accordé pour une PM est : date de bouclement + 6 mois + 75 jours calendaires.
	 */
	@Test
	public void testValidateDeadlineRequestPMBouclement30Septembre() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);
		service.setPeriodeFiscaleDAO(periodeFiscaleDAO);
		service.setDiService(diService);

		// on crée un contribuable PM vaudois
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(9, 30), 12);              // tous les 30.09 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2016, 10, 1), date(2017, 9, 30), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2017, 10, 1)));
			di.addDelai(newDelaiDeclaration(date(2017, 10, 1), date(2018, 1, 1)));

			return pm.getNumero();
		});

		// [FISCPROJ-862] le délai proposé doit être le 13.06.2018 (30.09.2017 + 6 mois = 30.03.2018 ; 30.03.2018 + 75 jours = 13.06.2018)
		final RegDate dateBouclement = RegDate.get(2017, 9, 30);
		final RegDate dateDelai = dateBouclement.addMonths(6).addDays(75);
		assertEquals(RegDate.get(2018, 6, 13), dateDelai);

		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationSuccess(ctbId, PartyType.CORPORATION, date(2016, 10, 1), date(2017, 9, 30), 1, Collections.singletonList(dateDelai), results);
			return null;
		});
	}

	/**
	 * [FISCPROJ-753][FISCPROJ-862] Ce test vérifie que le délai accordé pour une PM est : date de bouclement + 6 mois + 75 jours calendaires.
	 */
	@Test
	public void testValidateDeadlineRequestPMBouclement31Decembre() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);
		service.setBouclementService(bouclementService);
		service.setPeriodeFiscaleDAO(periodeFiscaleDAO);
		service.setDiService(diService);

		// on crée un contribuable PM vaudois
		final long ctbId = doInNewTransaction(status -> {
			final Entreprise pm = addEntrepriseInconnueAuCivil("Ma petite entreprise", date(2000, 2, 1));
			addBouclement(pm, date(2000, 2, 1), DayMonth.get(12, 31), 12);              // tous les 31.12 depuis 2000
			addRegimeFiscalVD(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addRegimeFiscalCH(pm, date(2000, 2, 1), null, MockTypeRegimeFiscal.ORDINAIRE_PM);
			addForPrincipal(pm, date(2000, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Bex);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_PM_LOCAL, periode2017);

			final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
			final DeclarationImpotOrdinairePM di = addDeclarationImpot(pm, periode2017, date(2017, 1, 1), date(2017, 12, 31), oipm, TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2018, 6, 30)));

			return pm.getNumero();
		});

		// [FISCPROJ-862] le délai proposé doit être le 13.09.2018 (31.12.2017 + 6 mois = 30.06.2018 ; 30.06.2018 + 75 jours = 13.09.2018)
		final RegDate dateBouclement = RegDate.get(2017, 12, 31);
		final RegDate dateDelai = dateBouclement.addMonths(6).addDays(75);
		assertEquals(RegDate.get(2018, 9, 13), dateDelai);

		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationSuccess(ctbId, PartyType.CORPORATION, date(2017, 1, 1), date(2017, 12, 31), 1, Collections.singletonList(dateDelai), results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableAvecDeuxDeclarationsEmises() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec deux déclarations émises (touch'n go)
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 1, 1), MotifFor.ARRIVEE_HC, date(2017, 3, 31), MotifFor.DEPART_HS, MockCommune.Bex);
			addForPrincipal(pp, date(2017, 4, 1), MotifFor.DEPART_HS, date(2017, 8, 30), MotifFor.ARRIVEE_HS, MockPays.France);
			addForPrincipal(pp, date(2017, 9, 1), MotifFor.ARRIVEE_HS, MockCommune.Croy);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di1.addEtat(new EtatDeclarationEmise(date(2017, 4, 15)));
			di1.addDelai(newDelaiDeclaration(date(2017, 4, 15), date(2017, 4, 30)));

			final DeclarationImpotOrdinairePP di2 = addDeclarationImpot(pp, periode2017, date(2017, 9, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di2.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di2.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2017, 6, 30)));

			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertIneligibleError(ctbId, PartyType.NATURAL_PERSON, "Le contribuable n'est pas éligible car il possède plusieurs périodes d'imposition en 2017.", results);
			return null;
		});
	}

	@Test
	public void testValidateDeadlineRequestContribuableAvecDeuxDeclarationsEmisesMaisUneSeulePeriodeImposition() throws Exception {

		final BusinessWebServiceImpl service = new BusinessWebServiceImpl();
		service.setTiersDAO(tiersDAO);
		service.setValidationService(validationService);
		service.setPeriodeImpositionService(periodeImpositionService);

		// on crée un contribuable assujetti avec deux déclarations émises mais une seule période d'imposition
		final long ctbId = doInNewTransaction(status -> {
			final PersonnePhysique pp = addNonHabitant("Jackie", "Glutz", date(1950, 1, 1), Sexe.FEMININ);
			addForPrincipal(pp, date(2017, 1, 1), MotifFor.ARRIVEE_HC, MockCommune.Croy);

			final PeriodeFiscale periode2017 = addPeriodeFiscale(2017);
			final ModeleDocument modele = addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2017);

			final DeclarationImpotOrdinairePP di1 = addDeclarationImpot(pp, periode2017, date(2017, 1, 1), date(2017, 3, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di1.addEtat(new EtatDeclarationEmise(date(2017, 4, 15)));
			di1.addDelai(newDelaiDeclaration(date(2017, 4, 15), date(2017, 4, 30)));

			final DeclarationImpotOrdinairePP di2 = addDeclarationImpot(pp, periode2017, date(2017, 9, 1), date(2017, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele);
			di2.addEtat(new EtatDeclarationEmise(date(2018, 1, 15)));
			di2.addDelai(newDelaiDeclaration(date(2018, 1, 15), date(2017, 6, 30)));

			return pp.getNumero();
		});

		// il ne doit pas être possible de demander un délai
		doInNewTransaction(status -> {
			final ValidationResult results = service.validateDeadlineRequest(2017, (int) ctbId);
			assertValidationError(ctbId, PartyType.NATURAL_PERSON, "05", "Il existe plusieurs déclarations sur la période 2017.", results);
			return null;
		});
	}

	private static DelaiDocumentFiscal newDelaiDeclaration(RegDate dateTraitement, RegDate dateDelai) {
		DelaiDeclaration d = new DelaiDeclaration();
		d.setDateTraitement(dateTraitement);
		d.setDelaiAccordeAu(dateDelai);
		d.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		return d;
	}

	private static void assertIneligibleError(long ctbId, PartyType partyType, String message, ValidationResult result) {
		assertEquals(ctbId, result.getTaxPayerNumber());
		assertEquals(partyType, result.getTaxPayerType());
		assertFalse(result.isEligible());
		assertEquals(message, result.getNonEligibleReason());
		assertNull(result.getTaxDeclaration());
		assertEmpty(result.getProposedDeadlines());
		assertNull(result.getRejectionReason());
	}

	private static void assertValidationError(long ctbId, PartyType partyType, String code, String message, ValidationResult result) {
		assertEquals(ctbId, result.getTaxPayerNumber());
		assertEquals(partyType, result.getTaxPayerType());
		assertTrue(result.isEligible());
		assertEmpty(result.getProposedDeadlines());
		assertNull(result.getTaxDeclaration());

		final RejectionReason reason = result.getRejectionReason();
		assertEquals(code, reason.getCode());
		assertEquals(message, reason.getMessage());
	}

	private static void assertValidationError(long ctbId, PartyType partyType, String code, String message, RegDate dateDebutDeclaration, RegDate dateFinDeclaration, int numeroSequence, ValidationResult result) {
		assertEquals(ctbId, result.getTaxPayerNumber());
		assertEquals(partyType, result.getTaxPayerType());
		assertTrue(result.isEligible());
		assertEmpty(result.getProposedDeadlines());

		final TaxDeclarationInfo taxDeclaration = result.getTaxDeclaration();
		assertNotNull(taxDeclaration);
		assertDate(dateDebutDeclaration, taxDeclaration.getDateFrom());
		assertDate(dateFinDeclaration, taxDeclaration.getDateTo());
		assertEquals(numeroSequence, taxDeclaration.getSequenceNumber());

		final RejectionReason reason = result.getRejectionReason();
		assertEquals(code, reason.getCode());
		assertEquals(message, reason.getMessage());
	}

	private static void assertValidationSuccess(long ctbId, PartyType partyType, RegDate dateDebutDeclaration, RegDate dateFinDeclaration, int numeroSequence, List<RegDate> delais, ValidationResult result) {
		assertEquals(ctbId, result.getTaxPayerNumber());
		assertEquals(partyType, result.getTaxPayerType());
		assertTrue(result.isEligible());
		assertNull(result.getRejectionReason());

		final List<ch.vd.unireg.xml.common.v2.Date> proposedDeadlines = result.getProposedDeadlines();
		assertEquals(delais.size(), proposedDeadlines.size());
		assertEquals(delais, proposedDeadlines.stream().map(DataHelper::webToRegDate).collect(Collectors.toList()));

		final TaxDeclarationInfo taxDeclaration = result.getTaxDeclaration();
		assertNotNull(taxDeclaration);
		assertDate(dateDebutDeclaration, taxDeclaration.getDateFrom());
		assertDate(dateFinDeclaration, taxDeclaration.getDateTo());
		assertEquals(numeroSequence, taxDeclaration.getSequenceNumber());
	}

	public static void assertDate(@Nullable RegDate expected, @Nullable ch.vd.unireg.xml.common.v2.Date actual) {
		if (expected == null) {
			assertNull(actual);
		}
		else {
			assertNotNull(actual);
			assertEquals(expected.year(), actual.getYear());
			assertEquals(expected.month(), actual.getMonth());
			assertEquals(expected.day(), actual.getDay());
		}
	}

	private static void assertOwnerNaturalPerson(String firstName, String lastName, RegDate dateOfBirth, RightHolder owner) {
		final NaturalPersonIdentity identity = (NaturalPersonIdentity) owner.getIdentity();
		assertEquals(firstName, identity.getFirstName());
		assertEquals(lastName, identity.getLastName());
		assertEquals(dateOfBirth, DataHelper.webToRegDate(identity.getDateOfBirth()));
	}

	private static void assertOwnerParty(long id, RightHolder rightHolder) {
		assertEquals(Integer.valueOf((int) id), rightHolder.getTaxPayerNumber());
	}

	private void assertShare(int numerator, int denominator, Share share) {
		assertNotNull(share);
		assertEquals(numerator, share.getNumerator());
		assertEquals(denominator, share.getDenominator());
	}

	private void assertCaseIdentifier(int officeNumber, String caseNumber, CaseIdentifier caseIdentifier) {
		assertNotNull(caseIdentifier);
		assertEquals(officeNumber, caseIdentifier.getOfficeNumber());
		assertEquals(caseNumber, caseIdentifier.getCaseNumberText());
	}

	private static void assertLocation(RegDate dateFrom, RegDate dateTo, int parcelNumber, Integer index1, Integer index2, Integer index3, int noOfsCommune, Location location) {
		assertNotNull(location);
		assertEquals(dateFrom, DataHelper.webToRegDate(location.getDateFrom()));
		assertEquals(dateTo, DataHelper.webToRegDate(location.getDateTo()));
		assertEquals(parcelNumber, location.getParcelNumber());
		assertEquals(index1, location.getIndex1());
		assertEquals(index2, location.getIndex2());
		assertEquals(index3, location.getIndex3());
		assertEquals(noOfsCommune, location.getMunicipalityFsoId());
	}

	public static void assertFoundEntry(long immoId, ImmovablePropertyEntry entry) {
		assertEquals(immoId, entry.getImmovablePropertyId());
		final ch.vd.unireg.xml.party.landregistry.v1.ImmovableProperty immo = entry.getImmovableProperty();
		assertNotNull(immo);
		assertEquals(immoId, immo.getId());
		assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long immoId, ImmovablePropertyEntry entry) {
		assertEquals(immoId, entry.getImmovablePropertyId());
		assertNull(entry.getImmovableProperty());
		assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		assertEquals("L'immeuble n°[" + immoId + "] n'existe pas.", entry.getError().getErrorMessage());
	}

	public static void assertFoundEntry(long buildingId, BuildingEntry entry) {
		assertEquals(buildingId, entry.getBuildingId());
		final Building building = entry.getBuilding();
		assertNotNull(building);
		assertEquals(buildingId, building.getId());
		assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long buildingId, BuildingEntry entry) {
		assertEquals(buildingId, entry.getBuildingId());
		assertNull(entry.getBuilding());
		assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		assertEquals("Le bâtiment n°[" + buildingId + "] n'existe pas.", entry.getError().getErrorMessage());
	}

	public static void assertFoundEntry(long communityId, CommunityOfOwnersEntry entry) {
		assertEquals(communityId, entry.getCommunityOfOwnersId());
		final CommunityOfOwners community = entry.getCommunityOfOwners();
		assertNotNull(community);
		assertEquals(communityId, community.getId());
		assertNull(entry.getError());
	}

	private static void assertNotFoundEntry(long communityId, CommunityOfOwnersEntry entry) {
		assertEquals(communityId, entry.getCommunityOfOwnersId());
		assertNull(entry.getCommunityOfOwners());
		assertEquals(ErrorType.BUSINESS, entry.getError().getType());
		assertEquals("La communauté n°[" + communityId + "] n'existe pas.", entry.getError().getErrorMessage());
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

	private static void assertOperatingPeriod(RegDate dateDebut, RegDate dateFin, OperatingPeriod period) {
		assertNotNull(period);
		assertEquals(dateDebut, ch.vd.unireg.xml.DataHelper.xmlToCore(period.getDateFrom()));
		assertEquals(dateFin, ch.vd.unireg.xml.DataHelper.xmlToCore(period.getDateTo()));
	}

	private static void assertIfoncExemption(int percent, RealLandTaxLightening lightening) {
		final IfoncExemption exo =(IfoncExemption) lightening;
		assertEquals(percent, exo.getExemptionPercent().intValue());
	}

	private static void assertIciAbatement(int percent, RealLandTaxLightening lightening) {
		final IciAbatement aba =(IciAbatement) lightening;
		assertEquals(percent, aba.getAbatementPercent().intValue());
	}

	private static void assertVirtualLandTaxLightening(RegDate dateFrom, RegDate dateTo, long immeubleId, VirtualLandTaxLightening lightening) {
		assertNotNull(lightening);
		assertEquals(dateFrom, ch.vd.unireg.xml.DataHelper.xmlToCore(lightening.getDateFrom()));
		assertEquals(dateTo, ch.vd.unireg.xml.DataHelper.xmlToCore(lightening.getDateTo()));
		assertEquals(immeubleId, lightening.getImmovablePropertyId());
	}
}
