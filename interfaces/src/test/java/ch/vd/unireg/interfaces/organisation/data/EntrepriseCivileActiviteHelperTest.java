package ch.vd.unireg.interfaces.organisation.data;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.organisation.mock.data.MockEtablissementCivil;
import ch.vd.unireg.interfaces.organisation.mock.data.builder.MockEtablissementCivilFactory;
import ch.vd.unireg.interfaces.organisation.rcent.RCEntEtablissementHelperTest;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * Classe de test de {@link EntrepriseActiviteHelper}, helper spécialisé dans le calcul de l'activité des entreprises et établissements.
 *
 * 6 tests existants déplacés de {@link RCEntEtablissementHelperTest} le 2017-08-14.
 *
 * @author Raphaël Marmier, 2017-08-14, <raphael.marmier@vd.ch>
 */
public class EntrepriseCivileActiviteHelperTest extends WithoutSpringTest {
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testActiviteActiveInscritRC() {
		/*
		 * Site en activité depuis son inscription au RC:
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.24              ...-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, date(2010, 6, 24),
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteActiveNonInscritRC() {
		/*
		 * Site en activité depuis son inscription à l'IDE:
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.26              ...-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeInscritRC() {
		/*
		 * Site en activité sur une seule période de son inscription au RC à sa radiation
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.24       2015.07.05-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2015, 7, 5);
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 5), periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeNonInscritRC() {
		/*
		 * Site en activité sur une seule période de son inscription à l'IDE à sa radiation
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.26       2015.07.07-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Test
	public void testActiviteActiveReInscritRC() {
		/*
		 * Site en activité sur deux périodes, une première à l'issue de laquelle il est radié du RC. Une seconde ouverte par une seconde inscription et toujours en cours.
		 *
		 * Cependant, pour des raisons historiques, l'algorithme de calcul est limité et ne produit qu'une seule plage d'activité couvrant une période ouverte
		 * à partir de sa première inscription au RC et non fermée.
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.24              ...-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2012, 2, 5);

		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2012, 2, 8), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2012, 2, 8), StatusRegistreIDE.RADIE);

		mockSite.getDonneesRC().changeInscription(date(2015, 7, 8), new InscriptionRC(StatusInscriptionRC.ACTIF, null,
		                                                                              dateInscriptionRC, null,
		                                                                              dateInscriptionRC, null));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.DEFINITIF);

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeRCLongtempsAvantRadiationIDE() {
		/*
		 * Site en activité sur une seule période de son inscription au RC à sa radiation finale de l'IDE, bien après sa radiation du RC.
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.24       2015.07.07-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;

		final RegDate dateInscriptionRC = date(2010, 6, 24);
		final RegDate dateRadiationRC = date(2012, 3, 1);
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRC().changeInscription(date(2012, 3, 4), new InscriptionRC(StatusInscriptionRC.RADIE, null,
		                                                                              dateInscriptionRC, dateRadiationRC,
		                                                                              dateInscriptionRC, dateRadiationRC));
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeREE() {
		/*
		 * Site en activité sur une seule période de son inscription au REE à sa radiation.
		 *
		 * Sauf qu'il y a une limitation: on ne tient pas compte de la date fournie pour l'inscription REE, mais on procède
		 * comme pour l'IDE en ne tenant compte que de la date de l'événement RCEnt.
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.26       2015.07.07-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                         null, null, "CHE999999996", null, null);
		mockSite.getDonneesREE().changeInscriptionREE(date(2010, 6, 26), new InscriptionREE(StatusREE.ACTIF, date(2010, 6, 24)) );
		mockSite.getDonneesREE().changeInscriptionREE(date(2015, 7, 8), new InscriptionREE(StatusREE.RADIE, date(2015, 7, 4)));

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertEquals(date(2015, 7, 7), periode1.getDateFin());
	}

	@Ignore
	@Test
	public void testActiviteRadieeParErreurIDEActifRC() {
		/*
		 * Site en activité sur une seule période à partir de son inscription au RC. Radié par erreur de l'IDE. Pas de radiation au RC. Il doit rester actif.
		 *
		 *              +-----------------------------+
	     * Plage:       |-2010.06.24       2015.07.07-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;

		final RegDate dateInscriptionRC = date(2010, 6, 24);
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), StatusInscriptionRC.ACTIF, dateInscriptionRC,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 24), periode1.getDateDebut());
		assertNull(periode1.getDateFin());
	}

	@Test
	public void testActiviteRadieeREEApresIde() {
		/*
		 * Site en activité sur une seule période de son inscription à sa radiation. Site présent à l'IDE et au REE mais pas au RC.
		 * On considère la radiation la plus tardive des deux.
		 *
		 * Voir: SIFISC-25855
		 *              +-----------------------------+
	     * Plage:       |-2010.06.26       2015.07.09-|
	     *              +-----------------------------+
		 */

		final long noEtablissement = 10000;
		MockEtablissementCivil mockSite = MockEtablissementCivilFactory.mockSite(noEtablissement, date(2010, 6, 26), null, "Synergy Conception Aubonne SA",
		                                                                         FormeLegale.N_0106_SOCIETE_ANONYME, false, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD,
		                                                                         MockCommune.Aubonne.getNoOFS(), null, null,
		                                                                         StatusRegistreIDE.DEFINITIF, TypeEntrepriseRegistreIDE.PERSONNE_JURIDIQUE, "CHE999999996", null, null);
		mockSite.getDonneesRegistreIDE().changeStatus(date(2015, 7, 8), StatusRegistreIDE.RADIE);
		mockSite.getDonneesREE().changeInscriptionREE(date(2010, 6, 26), new InscriptionREE(StatusREE.ACTIF, date(2010, 6, 24)) );
		mockSite.getDonneesREE().changeInscriptionREE(date(2015, 7, 10), new InscriptionREE(StatusREE.RADIE, date(2015, 7, 6)));

		List<DateRange> activite = EntrepriseActiviteHelper.activite(mockSite);

		assertFalse(activite.isEmpty());
		assertEquals(1, activite.size());
		final DateRange periode1 = activite.get(0);
		assertEquals(date(2010, 6, 26), periode1.getDateDebut());
		assertEquals(date(2015, 7, 9), periode1.getDateFin());
	}
}