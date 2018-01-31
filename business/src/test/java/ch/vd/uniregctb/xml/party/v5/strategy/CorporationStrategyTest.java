package ch.vd.uniregctb.xml.party.v5.strategy;

import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.infra.mock.MockTypeRegimeFiscal;
import ch.vd.unireg.interfaces.organisation.mock.MockServiceOrganisation;
import ch.vd.unireg.xml.party.corporation.v5.Corporation;
import ch.vd.unireg.xml.party.corporation.v5.LegalForm;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatement;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IciAbatementRequest;
import ch.vd.unireg.xml.party.landtaxlightening.v1.IfoncExemption;
import ch.vd.unireg.xml.party.taxpayer.v5.LegalFormCategory;
import ch.vd.unireg.xml.party.v5.PartyPart;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.documentfiscal.DelaiAutreDocumentFiscal;
import ch.vd.uniregctb.foncier.DegrevementICI;
import ch.vd.uniregctb.foncier.DemandeDegrevementICI;
import ch.vd.uniregctb.foncier.ExonerationIFONC;
import ch.vd.uniregctb.regimefiscal.RegimeFiscalService;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatDelaiDocumentFiscal;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;

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
		strategy.initLandTaxLightenings(corporation, entreprise);

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
		serviceOrganisation.setUp(new MockServiceOrganisation() {
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
				strategy.initParts(corp, entreprise, EnumSet.of(PartyPart.LEGAL_FORMS), context);

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
}