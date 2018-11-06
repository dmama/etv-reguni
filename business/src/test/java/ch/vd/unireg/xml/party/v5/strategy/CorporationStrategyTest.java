package ch.vd.unireg.xml.party.v5.strategy;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.adresse.AdresseService;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.documentfiscal.DelaiAutreDocumentFiscal;
import ch.vd.unireg.foncier.DegrevementICI;
import ch.vd.unireg.foncier.DemandeDegrevementICI;
import ch.vd.unireg.foncier.ExonerationIFONC;
import ch.vd.unireg.interfaces.entreprise.mock.MockServiceEntreprise;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.regimefiscal.RegimeFiscalService;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.ProprieteParEtageRF;
import ch.vd.unireg.registrefoncier.RegistreFoncierService;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.FormeJuridiqueEntreprise;
import ch.vd.unireg.type.TypeRapprochementRF;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;
import ch.vd.unireg.xml.common.v2.Date;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.landregistry.v1.CaseIdentifier;
import ch.vd.unireg.xml.party.landregistry.v1.LandOwnershipRight;
import ch.vd.unireg.xml.party.landregistry.v1.LandRight;
import ch.vd.unireg.xml.party.landregistry.v1.OwnershipType;
import ch.vd.unireg.xml.party.landregistry.v1.Share;
import ch.vd.unireg.xml.party.landregistry.v1.UsufructRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualInheritedLandRight;
import ch.vd.unireg.xml.party.landregistry.v1.VirtualLandOwnershipRight;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory;
import ch.vd.unireg.xml.party.v5.InternalPartyPart;

import static ch.vd.unireg.xml.DataHelper.xmlToCore;
import static ch.vd.unireg.xml.party.v5.LandRightBuilderTest.assertCaseIdentifier;
import static ch.vd.unireg.xml.party.v5.LandRightBuilderTest.assertShare;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class CorporationStrategyTest extends BusinessTest {

	private CorporationStrategy strategy;
	private Context context;

	@Before
	public void setUp() throws Exception {
		strategy = new CorporationStrategy();

		context = new Context();
		context.tiersService = getBean(TiersService.class, "tiersService");
		context.regimeFiscalService = getBean(RegimeFiscalService.class, "regimeFiscalService");
		context.registreFoncierService = getBean(RegistreFoncierService.class, "serviceRF");
		context.adresseService = getBean(AdresseService.class, "adresseService");
	}

	/**
	 * Ce test vérifie que les collections des allègements fonciers sont bien ordonnées correctement.
	 */
	@Test
	public void testInitLandTaxLightenings() throws Exception {

		final BienFondsRF immeuble0 = new BienFondsRF();
		immeuble0.setId(0L);

		final BienFondsRF immeuble1 = new BienFondsRF();
		immeuble1.setId(1L);

		final BienFondsRF immeuble2 = new BienFondsRF();
		immeuble2.setId(2L);


		final Entreprise entreprise = new Entreprise();
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble2));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble1));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2005, 1, 1), null, immeuble0));
		entreprise.addAllegementFoncier(newExoneration(RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), immeuble0));

		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble2));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble1));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), immeuble0));
		entreprise.addAllegementFoncier(newDegrevement(RegDate.get(1990, 1, 1), null, immeuble2));

		entreprise.addAutreDocumentFiscal(newDemandeDegrevement(RegDate.get(2006, 3, 11), RegDate.get(2006, 6, 30), RegDate.get(2006, 2, 1), 2005, 1, immeuble0));
		entreprise.addAutreDocumentFiscal(newDemandeDegrevement(RegDate.get(2005, 3, 15), RegDate.get(2005, 6, 30), RegDate.get(2005, 5, 22), 2004, 1, immeuble0));

		final Corporation corporation = new Corporation();
		strategy.initLandTaxLightenings(corporation, entreprise, EnumSet.of(InternalPartyPart.LAND_TAX_LIGHTENINGS), context);

		// l'ordre doit être :
		// - chronologique croissant
		// - id d'immeuble croissant
		final List<IfoncExemption> exemptions = corporation.getIfoncExemptions();
		assertNotNull(exemptions);
		assertEquals(4, exemptions.size());
		assertExemption(RegDate.get(2000, 1, 1), RegDate.get(2009, 12, 31), 0L, exemptions.get(0));
		assertExemption(RegDate.get(2005, 1, 1), null, 0L, exemptions.get(1));
		assertExemption(RegDate.get(2005, 1, 1), null, 1L, exemptions.get(2));
		assertExemption(RegDate.get(2005, 1, 1), null, 2L, exemptions.get(3));

		// l'ordre doit être :
		// - chronologique croissant
		// - id d'immeuble croissant
		final List<IciAbatement> abatements = corporation.getIciAbatements();
		assertNotNull(abatements);
		assertEquals(4, abatements.size());
		assertAbatement(RegDate.get(1990, 1, 1), null, 2L, abatements.get(0));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 0L, abatements.get(1));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 1L, abatements.get(2));
		assertAbatement(RegDate.get(2003, 1, 1), RegDate.get(2003, 12, 13), 2L, abatements.get(3));

		// l'ordre doit être :
		// - chronologique croissant
		// - id d'immeuble croissant
		final List<IciAbatementRequest> requests = corporation.getIciAbatementRequests();
		assertNotNull(requests);
		assertEquals(2, requests.size());
		assertAbatamentRequest(RegDate.get(2005, 3, 15), RegDate.get(2005, 6, 30), RegDate.get(2005, 5, 22), 2004, 1, 0, requests.get(0));
		assertAbatamentRequest(RegDate.get(2006, 3, 11), RegDate.get(2006, 6, 30), RegDate.get(2006, 2, 1), 2005, 1, 0, requests.get(1));
	}

	/**
	 * [SIFISC-24999] Teste que la construction d'un party à partir d'une entreprise absorbée renseigne bien la date 'dateInheritedTo' sur les droits de l'entreprise.
	 */
	@Test
	public void testNewFromPartLandRightWithFusion() throws Exception {

		final RegDate dateFusion = RegDate.get(2005, 1, 1);

		class Ids {
			Long absorbee;
			Long absorbante;
			long bienFonds;
			long ppe;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {
			// données fiscales
			final Entreprise absorbee = addEntrepriseInconnueAuCivil();
			final Entreprise absorbante = addEntrepriseInconnueAuCivil();
			addFusionEntreprises(absorbante, absorbee, dateFusion);

			// données RF
			final PersonneMoraleRF entRF = addPersonneMoraleRF("PM0", "RC444", "Ent0", 44, null);
			addRapprochementRF(absorbee, entRF, null, null, TypeRapprochementRF.AUTO);

			final CommuneRF commune = addCommuneRF(61, "La Sarraz", 5498);
			final BienFondsRF bienFonds = addBienFondsRF("BienFonds2", "CHBF2", commune, 30);
			final ProprieteParEtageRF ppe = addProprieteParEtageRF("PPE", "CHPPE", new Fraction(1, 6), commune, 230, null, null, null);

			// un droit de propriété
			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(61, 2000, 1, 1);
			addDroitPersonneMoraleRF(null, RegDate.get(2000, 1, 1), null, null, "Achat", null, "DROIT0", "1", numeroAffaire,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, entRF, bienFonds, null);

			// une servitude
			addUsufruitRF(null, RegDate.get(2000, 1, 1), null, null, "Donation", null, "USU0", "1", numeroAffaire, new IdentifiantDroitRF(61, 2000, 2), entRF, ppe);

			ids.absorbee = absorbee.getId();
			ids.absorbante = absorbante.getId();
			ids.bienFonds = bienFonds.getId();
			ids.ppe = ppe.getId();
			return null;
		});

		final Corporation absorbante = newFrom(ids.absorbee, InternalPartyPart.REAL_LAND_RIGHTS);
		final List<LandRight> landRights = absorbante.getLandRights();
		assertNotNull(landRights);
		assertEquals(2, landRights.size());

		// on vérifie que la date 'dateInheritedTo' est bien renseignée sur le droit de propriété
		final LandOwnershipRight landRight0 = (LandOwnershipRight) landRights.get(0);
		assertNotNull(landRight0);
		assertNull(landRight0.getCommunityId());
		assertEquals(OwnershipType.SOLE_OWNERSHIP, landRight0.getType());
		assertShare(1, 1, landRight0.getShare());
		assertEquals(date(2000, 1, 1), xmlToCore(landRight0.getDateFrom()));
		assertNull(landRight0.getDateTo());
		assertEquals("Achat", landRight0.getStartReason());
		assertNull(landRight0.getEndReason());
		assertCaseIdentifier(61, "2000/1/1", landRight0.getCaseIdentifier());
		assertEquals(ids.bienFonds, landRight0.getImmovablePropertyId());
		assertEquals(dateFusion, xmlToCore(landRight0.getDateInheritedTo()));

		// [IMM-1105] on vérifie que la date 'dateInheritedTo' est aussi bien renseignée sur la servitude
		final UsufructRight landRight1 = (UsufructRight) landRights.get(1);
		assertNotNull(landRight1);
		assertEquals(date(2000, 1, 1), xmlToCore(landRight1.getDateFrom()));
		assertNull(landRight1.getDateTo());
		assertEquals("Donation", landRight1.getStartReason());
		assertNull(landRight1.getEndReason());
		assertCaseIdentifier(61, "2000/1/1", landRight1.getCaseIdentifier());
		assertEquals(ids.ppe, landRight1.getImmovablePropertyId());
		assertEquals(dateFusion, xmlToCore(landRight1.getDateInheritedTo())); // <-- renseignée sur les usufruits pour les personnes morales
	}

	/**
	 * [SIFISC-28888] Ce test vérifie que la méthode 'clone' ne retourne pas de droits virtuels d'héritage sur droits virtuels transitifs
	 *                lorsqu'on ne demande que la part VIRTUAL_INHERITED_REAL_LAND_RIGHTS.
	 */
	@Test
	public void testCloneLandRightsWithVirtualInheritanceAndTransitiveRights() {

		// un droit de propriété défunt -> immeuble1
		final LandOwnershipRight reference0 = new LandOwnershipRight(new Date(1990, 2, 12), null, "Achat", null, new CaseIdentifier(12, 2000, 23, null, 0, null, 0, null),
		                                                             null, 123456789L, new Share(1, 1), OwnershipType.SOLE_OWNERSHIP, 0L, null, 0, 222L, new Date(2000, 1, 1), 0, null);
		// un droit virtuel défunt -> immeuble1 -> immeuble2
		final VirtualLandOwnershipRight reference1 = new VirtualLandOwnershipRight(new Date(1990, 2, 12), null, "Achat", null, new CaseIdentifier(12, 2000, 23, null, 0, null, 0, null),
		                                                                           null, 444848484L, null, null, 0, new Date(2000, 1, 1), 0, null);

		final Corporation from = new Corporation();
		// un droit virtuel d'héritage sur le droit réel défunt -> immeuble1
		from.getLandRights().add(new VirtualInheritedLandRight(new Date(2000, 1, 1), null, "Achat", null, new CaseIdentifier(12, 2000, 23, null, 0, null, 0, null),
		                                                       null, 123456789L, 34343434L, false, null, reference0, null));
		// un droit virtuel d'héritage sur un droit virtuel transitif défunt -> immeuble1 -> immeuble2
		from.getLandRights().add(new VirtualInheritedLandRight(new Date(2000, 1, 1), null, "Achat", null, new CaseIdentifier(12, 2000, 23, null, 0, null, 0, null),
		                                                       null, 444848484L, 34343434L, false, null, reference1, null));
		final Corporation to = strategy.clone(from, EnumSet.of(InternalPartyPart.VIRTUAL_INHERITED_REAL_LAND_RIGHTS));

		// on ne devrait avoir que le droit virtuel d'héritage sur le droit réel
		final List<LandRight> toLandRights = to.getLandRights();
		assertNotNull(toLandRights);
		assertEquals(1, toLandRights.size());

		final VirtualInheritedLandRight toLandRight0 = (VirtualInheritedLandRight) toLandRights.get(0);
		assertNotNull(toLandRight0);
		assertEquals(new Date(2000, 1, 1), toLandRight0.getDateFrom());
		assertNull(toLandRight0.getDateTo());
		assertEquals(123456789L, toLandRight0.getImmovablePropertyId());
		assertEquals(34343434L, toLandRight0.getInheritedFromId());

		final LandOwnershipRight toReference0 = (LandOwnershipRight) toLandRight0.getReference();
		assertNotNull(toReference0);
		assertEquals(222L, toReference0.getId());
	}


	/**
	 * [SIFISC-24467] porblème apparu pour les formes juridiques dont la date de début est très antérieure à la date
	 * de début du premier régime fiscal vaudois... la catégorie d'entreprise, tirée du régime fiscal, n'était alors
	 * pas déterminée et BOOM...
	 */
	@Test
	public void testFormesLegalesEtCategoriesEntreprise() throws Exception {

		final RegDate dateDebut = date(1974, 2, 5);
		final RegDate dateDebutRegimeFiscal = date(1994, 1, 1);

		// mise en place civile
		serviceEntreprise.setUp(new MockServiceEntreprise() {
			@Override
			protected void init() {
				// non, rien...
			}
		});

		// mise en place fiscale
		final long idpm = doInNewTransactionAndSession(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSociale(entreprise, dateDebut, null, "Les joyeux lurons");
			addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.ASSOCIATION);
			addRegimeFiscalCH(entreprise, dateDebutRegimeFiscal, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			addRegimeFiscalVD(entreprise, dateDebutRegimeFiscal, null, MockTypeRegimeFiscal.ORDINAIRE_APM);
			return entreprise.getNumero();
		});

		// appel de la stratégie
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final Entreprise entreprise = (Entreprise) tiersDAO.get(idpm);
				assertNotNull(entreprise);

				final Corporation corp = new Corporation();
				strategy.initBase(corp, entreprise, context);
				strategy.initParts(corp, entreprise, EnumSet.of(InternalPartyPart.LEGAL_FORMS), context);

				// vérifions les formes légales
				final List<LegalForm> legalForms = corp.getLegalForms();
				assertNotNull(legalForms);
				assertEquals(2, legalForms.size());
				{
					final LegalForm lf = legalForms.get(0);
					assertNotNull(lf);
					assertEquals(dateDebut, DataHelper.xmlToCore(lf.getDateFrom()));
					assertEquals(dateDebutRegimeFiscal.getOneDayBefore(), DataHelper.xmlToCore(lf.getDateTo()));
					assertEquals(LegalFormCategory.OTHER, lf.getLegalFormCategory());       // pas de catégorie définie -> AUTRE
					assertEquals(ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.ASSOCIATION, lf.getType());
				}
				{
					final LegalForm lf = legalForms.get(1);
					assertNotNull(lf);
					assertEquals(dateDebutRegimeFiscal, DataHelper.xmlToCore(lf.getDateFrom()));
					assertNull(lf.getDateTo());
					assertEquals(LegalFormCategory.ASSOCIATION_FOUNDATION, lf.getLegalFormCategory());
					assertEquals(ch.vd.unireg.xml.party.taxpayer.v5.LegalForm.ASSOCIATION, lf.getType());
				}
			}
		});
	}

	/**
	 * [SIFISC-29739] Vérifie que les salutations sont bien retournées.
	 */
	@Test
	public void testGetSalutation() throws Exception {

		final Long id = doInNewTransaction(status -> addEntrepriseInconnueAuCivil().getId());

		final Corporation corp = newFrom(id);
		assertNotNull(corp);
		assertEquals("Madame, Monsieur", corp.getFormalGreeting());
	}

	private Corporation newFrom(long id, InternalPartyPart... parts) throws Exception {
		return doInNewTransaction(status -> {
			final Entreprise pm = hibernateTemplate.get(Entreprise.class, id);
			try {
				final Set<InternalPartyPart> p = (parts == null || parts.length == 0 ? null : new HashSet<>(Arrays.asList(parts)));
				return strategy.newFrom(pm, p, context);
			}
			catch (ServiceException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static void assertAbatamentRequest(RegDate sendDate, RegDate deadline, RegDate returnDate, int taxPeriod, int sequenceNumber, int immovablePropId, IciAbatementRequest request) {
		assertNotNull(request);
		assertEquals(sendDate, DataHelper.xmlToCore(request.getSendDate()));
		assertEquals(deadline, DataHelper.xmlToCore(request.getDeadline()));
		assertEquals(returnDate, DataHelper.xmlToCore(request.getReturnDate()));
		assertEquals(taxPeriod, request.getTaxPeriod());
		assertEquals(sequenceNumber, request.getSequenceNumber());
		assertEquals(immovablePropId, request.getImmovablePropertyId());
	}

	private static void assertExemption(RegDate dateFrom, RegDate dateTo, long immovablePropId, IfoncExemption exemption) {
		assertNotNull(exemption);
		assertEquals(dateFrom, DataHelper.xmlToCore(exemption.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(exemption.getDateTo()));
		assertEquals(immovablePropId, exemption.getImmovablePropertyId());
	}

	private static void assertAbatement(RegDate dateFrom, RegDate dateTo, long immovablePropId, IciAbatement abatement) {
		assertNotNull(abatement);
		assertEquals(dateFrom, DataHelper.xmlToCore(abatement.getDateFrom()));
		assertEquals(dateTo, DataHelper.xmlToCore(abatement.getDateTo()));
		assertEquals(immovablePropId, abatement.getImmovablePropertyId());
	}

	private static DemandeDegrevementICI newDemandeDegrevement(RegDate dateTraitement, RegDate delaiRetour, RegDate dateRetour, int periodeFiscale, int numeroSequence, ImmeubleRF immeuble) {
		final DemandeDegrevementICI demande = new DemandeDegrevementICI();
		demande.setDateEnvoi(dateTraitement);
		demande.setDateRetour(dateRetour);
		demande.setPeriodeFiscale(periodeFiscale);
		demande.setNumeroSequence(numeroSequence);
		demande.setImmeuble(immeuble);

		final DelaiAutreDocumentFiscal delai = new DelaiAutreDocumentFiscal();
		delai.setEtat(EtatDelaiDocumentFiscal.ACCORDE);
		delai.setDateTraitement(dateTraitement);
		delai.setDateDemande(dateTraitement);
		delai.setDelaiAccordeAu(delaiRetour);
		demande.addDelai(delai);

		return demande;
	}

	private static DegrevementICI newDegrevement(RegDate dateDebut, RegDate dateFin, BienFondsRF immeuble) {
		final DegrevementICI deg = new DegrevementICI();
		deg.setDateDebut(dateDebut);
		deg.setDateFin(dateFin);
		deg.setImmeuble(immeuble);
		return deg;
	}

	private static ExonerationIFONC newExoneration(RegDate dateDebut, RegDate dateFin, BienFondsRF immeuble) {
		final ExonerationIFONC exo0 = new ExonerationIFONC();
		exo0.setDateDebut(dateDebut);
		exo0.setDateFin(dateFin);
		exo0.setImmeuble(immeuble);
		return exo0;
	}
}