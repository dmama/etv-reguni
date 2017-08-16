package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.mock.data.MockSiteOrganisation;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockSiteOrganisationFactory;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Classe de test de {@link OrganisationActiviteHelper}, helper spécialisé dans le calcul de l'activité des entreprises et établissements.
 *
 * 6 tests existants déplacés de {@link ch.vd.unireg.interfaces.organisation.rcent.RCEntSiteOrganisationHelperTest} le 2017-08-14.
 *
 * @author Raphaël Marmier, 2017-08-14, <raphael.marmier@vd.ch>
 */
public class OrganisationActiviteHelperTest extends WithoutSpringTest {
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testActiviteActiveInscritRC() {
		final long noSite = 10000;
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

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
		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeInscritRC() {
		final long noSite = 10000;
		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2015, 7, 5);
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

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

		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Test
	public void testActiviteActiveReInscritRC() {
		final long noSite = 10000;
		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2012, 2, 5);

		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2012, 2, 8), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2012, 2, 8), StatusRegistreIDE.RADIE);

		mockSite.getDonneesRC().changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                                              dateInscriptionRC, null,
		                                                                              dateInscriptionRC, null));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.DEFINITIF);

		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeRCLongtempsAvantRadiationIDE() {
		final long noSite = 10000;

		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2012, 3, 1);
		MockSiteOrganisation mockSite = MockSiteOrganisationFactory.mockSite(noSite, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                     FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                     MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                     StatusRegistreIDE.DEFINITIF, TypeOrganisationRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2012, 3, 4), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = OrganisationActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}
}