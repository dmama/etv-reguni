package ch.vd.unireg.interfaces.organisation.rcent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ch.vd.evd0022.v3.BurLocalUnitStatus;
import ch.vd.evd0022.v3.Capital;
import ch.vd.evd0022.v3.CommercialRegisterStatus;
import ch.vd.evd0022.v3.LegalForm;
import ch.vd.evd0022.v3.TypeOfLocation;
import ch.vd.evd0022.v3.UidRegisterStatus;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper.Ranged;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockServiceInfrastructureService;
import ch.vd.unireg.interfaces.organisation.data.Domicile;
import ch.vd.unireg.interfaces.organisation.data.EntreeJournalRC;
import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.OrganisationHelper;
import ch.vd.unireg.interfaces.organisation.data.PublicationFOSC;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.data.StatusInscriptionRC;
import ch.vd.unireg.interfaces.organisation.data.StatusRegistreIDE;
import ch.vd.unireg.interfaces.organisation.data.TypeDeSite;
import ch.vd.unireg.interfaces.organisation.data.TypeOrganisationRegistreIDE;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationFunction;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2015-11-05
 */
public class RCEntSiteOrganisationHelperTest extends WithoutSpringTest {

	private final MockServiceInfrastructureService serviceInfra = new MockServiceInfrastructureService() {
		@Override
		protected void init() {
			add(MockCommune.Lausanne);
		}
	};

	@Test
	public void conversionSiteSimple() throws Exception {

		final RegDate refDate = RegDate.get(2015, 6, 1);

		final List<Ranged<String>> nom = new ArrayList<>(1);
		nom.add(new Ranged<>(refDate, null, "Ma boîte"));

		final Map<String, List<Ranged<String>>> identifiers = new HashMap<>();
		final List<Ranged<String>> additionalName = new ArrayList<>();
		final List<Ranged<TypeOfLocation>> typeOfLocation = new ArrayList<>();
		typeOfLocation.add(new Ranged<>(refDate, null, TypeOfLocation.ETABLISSEMENT_PRINCIPAL));
		final List<Ranged<Integer>> municipality = new ArrayList<>();
		municipality.add(new Ranged<>(refDate, null, MockCommune.Lausanne.getNoOFS()));
		final List<Ranged<LegalForm>> legalForm = new ArrayList<>();
		legalForm.add(new Ranged<>(refDate, null, LegalForm.N_0106_SOCIETE_ANONYME));
		final Map<String, List<Ranged<OrganisationFunction>>> function = new HashMap<>();
		final List<Ranged<Long>> replacedBy = new ArrayList<>();
		final List<Ranged<Long>> inReplacementOf = new ArrayList<>();

		final List<Ranged<CommercialRegisterStatus>> statusInscription = new ArrayList<>(1);
		statusInscription.add(new Ranged<>(refDate, null, CommercialRegisterStatus.ACTIF));
		final List<Ranged<Capital>> capital = new ArrayList<>(1);

		// Capital libéré correctement réglés, les autres champs sont optionels jusqu'à preuve du contraire.
		capital.add(new Ranged<>(refDate, null, new Capital(null, null, null, BigDecimal.valueOf(100000), null)));

		final OrganisationLocation.RCEntRCData rc = new OrganisationLocation.RCEntRCData(statusInscription, null, capital, null, null, null, null, null, null, null, null);

		final List<Ranged<UidRegisterStatus>> statusIde = new ArrayList<>();
		statusIde.add(new Ranged<>(refDate, null, UidRegisterStatus.DEFINITIF));
		final OrganisationLocation.RCEntUIDData uid = new OrganisationLocation.RCEntUIDData(null, statusIde, null, null, null);

		final OrganisationLocation.RCEntBURData bur = new OrganisationLocation.RCEntBURData(null, null);

		final OrganisationLocation loc = new OrganisationLocation(4567, nom, rc, uid, bur, identifiers, additionalName, typeOfLocation, legalForm, municipality, null, function, null, null, replacedBy, inReplacementOf);

		// Conversion
		final SiteOrganisation site = RCEntSiteOrganisationHelper.get(loc, serviceInfra);

		assertEquals(4567, site.getNumeroSite());
		assertEquals("Ma boîte", site.getNom().get(0).getPayload());
		assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, site.getTypeDeSite().get(0).getPayload());
		assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), site.getDomiciles().get(0).getNumeroOfsAutoriteFiscale());
		assertEquals(StatusInscriptionRC.ACTIF, site.getDonneesRC().getStatusInscription().get(0).getPayload());
		assertEquals(StatusRegistreIDE.DEFINITIF, site.getDonneesRegistreIDE().getStatus().get(0).getPayload());

		assertEquals(100000L, site.getDonneesRC().getCapital().get(0).getCapitalLibere().longValue());
	}

	@Test
	public void conversionSiteCapitalSansMontantLibere() throws Exception {

		final RegDate refDate = RegDate.get(2015, 6, 1);

		final List<Ranged<String>> nom = new ArrayList<>(1);
		nom.add(new Ranged<>(refDate, null, "Ma boîte"));

		final Map<String, List<Ranged<String>>> identifiers = new HashMap<>();
		final List<Ranged<String>> additionalName = new ArrayList<>();
		final List<Ranged<TypeOfLocation>> typeOfLocation = new ArrayList<>();
		typeOfLocation.add(new Ranged<>(refDate, null, TypeOfLocation.ETABLISSEMENT_PRINCIPAL));
		final List<Ranged<Integer>> municipality = new ArrayList<>();
		municipality.add(new Ranged<>(refDate, null, MockCommune.Lausanne.getNoOFS()));
		final List<Ranged<LegalForm>> legalForm = new ArrayList<>();
		legalForm.add(new Ranged<>(refDate, null, LegalForm.N_0106_SOCIETE_ANONYME));
		final Map<String, List<Ranged<OrganisationFunction>>> function = new HashMap<>();
		final List<Ranged<Long>> replacedBy = new ArrayList<>();
		final List<Ranged<Long>> inReplacementOf = new ArrayList<>();

		final List<Ranged<CommercialRegisterStatus>> statusInscription = new ArrayList<>(1);
		statusInscription.add(new Ranged<>(refDate, null, CommercialRegisterStatus.ACTIF));
		final List<Ranged<Capital>> capital = new ArrayList<>(1);

		// Capital libéré et devise correctement réglés
		capital.add(new Ranged<>(refDate, null, new Capital(null, null, BigDecimal.valueOf(100000), null, null)));

		final OrganisationLocation.RCEntRCData rc = new OrganisationLocation.RCEntRCData(statusInscription, null, capital, null, null, null, null, null, null, null, null);

		final List<Ranged<UidRegisterStatus>> statusIde = new ArrayList<>();
		statusIde.add(new Ranged<>(refDate, null, UidRegisterStatus.DEFINITIF));
		final OrganisationLocation.RCEntUIDData uid = new OrganisationLocation.RCEntUIDData(null, statusIde, null, null, null);

		final List<Ranged<BurLocalUnitStatus>> burLocalUnitStatus = new ArrayList<>();
		burLocalUnitStatus.add(new Ranged<>(refDate, null, BurLocalUnitStatus.ACTIF));
		final List<Ranged<RegDate>> burRegistrationDate = new ArrayList<>();
		burRegistrationDate.add(new Ranged<>(refDate, null, refDate.getOneDayBefore()));
		final OrganisationLocation.RCEntBURData bur = new OrganisationLocation.RCEntBURData(burLocalUnitStatus, burRegistrationDate);

		final OrganisationLocation loc = new OrganisationLocation(4567, nom, rc, uid, bur, identifiers, additionalName, typeOfLocation, legalForm, municipality, null, function, null, null, replacedBy, inReplacementOf);

		// Conversion
		final SiteOrganisation site = RCEntSiteOrganisationHelper.get(loc, serviceInfra);

		assertEquals(4567, site.getNumeroSite());
		assertEquals("Ma boîte", site.getNom().get(0).getPayload());
		assertEquals(TypeDeSite.ETABLISSEMENT_PRINCIPAL, site.getTypeDeSite().get(0).getPayload());
		assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), site.getDomiciles().get(0).getNumeroOfsAutoriteFiscale());
		assertEquals(StatusInscriptionRC.ACTIF, site.getDonneesRC().getStatusInscription().get(0).getPayload());
		assertEquals(StatusRegistreIDE.DEFINITIF, site.getDonneesRegistreIDE().getStatus().get(0).getPayload());

		assertTrue(site.getDonneesRC().getCapital().isEmpty());
	}

	@Test
	public void testActiviteActiveInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                                  FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                                  MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                                  StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteActiveNonInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeStatusInscription(date(2015, 7, 8), StatusInscriptionRC.RADIE);
		mockSite.getDonneesRC().changeDateRadiation(date(2015, 7, 8), date(2015, 7, 5));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 5), periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeNonInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), date(2015, 7, 7), "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Test
	public void testActiviteActiveReInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeStatusInscription(date(2012, 2, 8), StatusInscriptionRC.RADIE);
		mockSite.getDonneesRC().changeDateRadiation(date(2012, 2, 8), date(2012, 2, 5));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2012, 2, 8), StatusRegistreIDE.RADIE);

		mockSite.getDonneesRC().changeStatusInscription(date(2015, 7, 8), StatusInscriptionRC.ACTIF);
		mockSite.getDonneesRC().changeDateRadiation(date(2015, 7, 8), null);
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.DEFINITIF);

		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeLongtempsAvantInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeStatusInscription(date(12, 3, 4), StatusInscriptionRC.RADIE);
		mockSite.getDonneesRC().changeDateRadiation(date(12, 3, 4), date(2012, 3, 1));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = OrganisationHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Test
	public void testDomicilesRadieeInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 27), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeStatusInscription(date(2015, 7, 8), StatusInscriptionRC.RADIE);
		mockSite.getDonneesRC().changeDateRadiation(date(2015, 7, 8), date(2015, 7, 5));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<Domicile> domiciles = mockSite.getDomicilesEnActivite();

		assertFalse(domiciles.isEmpty());
		assertEquals(1, domiciles.size());
		final DateRange periode1 = domiciles.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 5), periode1.getDateFin());
	}

	@Test
	public void testFiltrageEntreesJournalRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 27), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		{
			final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2010, 6, 27), "77777", "Nouvelle inscription journal RC.");
			final EntreeJournalRC entreeJournalRC = new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2010, 6, 24), 111111L, publicationFOSC);
			mockSite.getDonneesRC().addEntreeJournal(entreeJournalRC);
		}
		{
			final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2012, 6, 5), "88888", "Mutation au journal RC.");
			final EntreeJournalRC entreeJournalRC = new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2012, 6, 1), 222222L, publicationFOSC);
			mockSite.getDonneesRC().addEntreeJournal(entreeJournalRC);
		}
		{
			final PublicationFOSC publicationFOSC = new PublicationFOSC(date(2015, 7, 8), "99999", "Encore une mutation au journal RC.");
			final EntreeJournalRC entreeJournalRC = new EntreeJournalRC(EntreeJournalRC.TypeEntree.NORMAL, date(2015, 7, 5), 333333L, publicationFOSC);
			mockSite.getDonneesRC().addEntreeJournal(entreeJournalRC);
		}

		final List<EntreeJournalRC> entreesJournalPourDate = mockSite.getDonneesRC().getEntreesJournal(date(2012, 6, 5));

		assertEquals(1, entreesJournalPourDate.size());
		final EntreeJournalRC entreeJournalRC = entreesJournalPourDate.get(0);
		assertEquals(date(2012, 6, 1), entreeJournalRC.getDate());
		assertEquals(222222, entreeJournalRC.getNumero().intValue());
		assertEquals(EntreeJournalRC.TypeEntree.NORMAL, entreeJournalRC.getType());
		final PublicationFOSC publicationFOSC = entreeJournalRC.getPublicationFOSC();
		assertEquals(date(2012, 6, 5), publicationFOSC.getDate());
		assertEquals("88888", publicationFOSC.getNumero());
		assertEquals("Mutation au journal RC.", publicationFOSC.getTexte());
	}
}