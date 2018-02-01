package ch.vd.unireg.interfaces.civil.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.ech.ech0007.v4.SwissMunicipality;
import ch.ech.ech0008.v2.Country;
import ch.ech.ech0010.v4.AddressInformation;
import ch.ech.ech0010.v4.MailAddress;
import ch.ech.ech0010.v4.SwissAddressInformation;
import ch.ech.ech0011.v5.Destination;
import ch.ech.ech0044.v2.NamedPersonId;
import ch.ech.ech0044.v2.PersonIdentificationPartner;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.evd0001.v5.Contact;
import ch.vd.evd0001.v5.DwellingAddress;
import ch.vd.evd0001.v5.Identity;
import ch.vd.evd0001.v5.MaritalData;
import ch.vd.evd0001.v5.Person;
import ch.vd.evd0001.v5.PersonIdentification;
import ch.vd.evd0001.v5.Residence;
import ch.vd.evd0001.v5.ResidencePermit;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.rcpers.EchHelper;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.DefaultMockServiceInfrastructureService;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.common.XmlUtils;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypePermis;
import ch.vd.unireg.type.TypePermisInvalideException;

import static ch.vd.unireg.interfaces.InterfacesTestHelper.newLocalisation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class IndividuRCPersTest extends WithoutSpringTest {

	private ServiceInfrastructureRaw infraService = new DefaultMockServiceInfrastructureService();

	@Test
	public void testGetCelibataire() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final List<MaritalData> statuses = Collections.singletonList(celibataire);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetMarieSeul() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", null);
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testGetMarieSeulPuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", null, date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testGetMarieSeulPuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", null, date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.MARIE, list.get(3));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.MARIE, list.get(3));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testGetMariePuisDivorcePuisRemarieIdentique() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29));
		final MaritalData divorce = newMaritalData(date(2005, 12, 4), TypeEtatCivil.DIVORCE, null);
		final MaritalData remarie = newMaritalData(date(2007, 5, 1), TypeEtatCivil.MARIE, newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie, divorce, remarie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(5, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2005, 12, 4), TypeEtatCivil.DIVORCE, list.get(3));
		assertEtatCivil(date(2007, 5, 1), TypeEtatCivil.MARIE, list.get(4));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(2, conjoints.size());

		{
			final RelationVersIndividu conjoint = conjoints.get(0);
			assertNotNull(conjoint);
			assertEquals(1234, conjoint.getNumeroAutreIndividu());
			assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
			assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
			assertEquals(date(2005, 12, 3), conjoint.getDateFin());
		}
		{
			final RelationVersIndividu conjoint = conjoints.get(1);
			assertNotNull(conjoint);
			assertEquals(1234, conjoint.getNumeroAutreIndividu());
			assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
			assertEquals(date(2007, 5, 1), conjoint.getDateDebut());
			assertNull(conjoint.getDateFin());
		}
	}

	/**
	 * Mauvais mapping ? En tout cas, nous avons ici deux états-civils mariés qui se suivent avec le même conjoint... (alors qu'il y aurait dû
	 * y avoir une réconciliation) -> une seule relation de conjoint doit être créée, pas deux...
	 */
	@Test
	public void testCollateRelationsConjoint() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29));
		final MaritalData marie2 = newMaritalData(date(2007, 5, 1), TypeEtatCivil.MARIE, newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie, marie2);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2007, 5, 1), TypeEtatCivil.MARIE, list.get(3));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testMarieVeufLeMemeJour() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final MaritalData veuf = newMaritalData(date(2000, 1, 1), "3", null);
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie, veuf);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.VEUF, list.get(2));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertEquals(date(2000, 1, 1), conjoint.getDateFin());
	}

	@Test
	public void testGetPacseSeul() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", null);
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testGetPacseSeulPuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", null, date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, list.get(2));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, list.get(2));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testGetPacseSeulPuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", null, date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.PACS, list.get(3));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNull(conjoints);
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ), date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.PACS, list.get(3));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(null, statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.PARTENAIRE_ENREGISTRE, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	@Test
	public void testMarieDecede() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1", null);
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2", newPersonIdentification(1234, "Marie", "Epousée", date(1962, 4, 2), Sexe.FEMININ));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final EtatCivilList ecList = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, list.get(1));

		final List<RelationVersIndividu> conjoints = IndividuRCPers.initConjoints(date(2010, 5, 12), statuses);
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertNotNull(conjoint);
		assertEquals(1234, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(2000, 1, 1), conjoint.getDateDebut());
		assertEquals(date(2010, 5, 11), conjoint.getDateFin());     // limité par la veille de la date de décès
	}

	/**
	 * Un individu peut possèder une séries d'adresses secondaires en parallèle aux adresses principales. La seule contrainte est qu'une adresse secondaire ne peut pas être en même temps sur la même
	 * commune qu'une adresse principale.
	 */
	@Test
	public void initAdressesPrincipalesEtSecondairesMelangees() throws Exception {

		// 3 adresses principales à partir du 1er janvier 1970
		final Residence p1 = newResidencePrincipale(date(1970, 1, 1), null, date(1999, 12, 31), MockRue.CossonayVille.AvenueDuFuniculaire);
		final Residence p2 = newResidencePrincipale(date(1970, 1, 1), date(1982, 1, 1), date(1999, 12, 31), MockRue.CossonayVille.CheminDeRiondmorcel);
		final Residence p3 = newResidencePrincipale(date(2000, 1, 1), null, null, MockRue.Bussigny.RueDeLIndustrie);

		// 2 adresses secondaires à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence s1 = newResidenceSecondaire(date(1970, 1, 1), null, null, MockRue.Cully.ChDesColombaires);
		final Residence s2 = newResidenceSecondaire(date(1970, 1, 1), date(1985, 1, 1), null, MockRue.Cully.PlaceDuTemple);

		// initialisation des adresses
		final Collection<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2), null, infraService);
		assertNotNull(adresses);
		assertEquals(5, adresses.size());

		final List<Adresse> principales = new ArrayList<>();
		final List<Adresse> secondaires = new ArrayList<>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
				secondaires.add(a);
			}
			else {
				fail();
			}
		}

		// les dates de fin des adresses principales ne doivent pas prendre en compte les adresses secondaires
		assertEquals(3, principales.size());
		assertAdresse(date(1970, 1, 1), date(1981, 12, 31), "Avenue du Funiculaire", "Cossonay-Ville", principales.get(0));
		assertAdresse(date(1982, 1, 1), date(1999, 12, 31), "Chemin de Riondmorcel", "Cossonay-Ville", principales.get(1));
		assertAdresse(date(2000, 1, 1), null, "Rue de l'Industrie", "Bussigny", principales.get(2));

		// les dates de fin des adresses secondaires ne doivent pas prendre en compte les adresses principales
		assertEquals(2, secondaires.size());
		assertAdresse(date(1970, 1, 1), date(1984, 12, 31), "Chemin des Colombaires", "Cully", secondaires.get(0));
		assertAdresse(date(1985, 1, 1), null, "Place du Temple", "Cully", secondaires.get(1));
	}

	/**
	 * Un individu peut possèder <b>plusieurs</b> séries d'adresses secondaires en parallèle aux adresses principales. La seule contrainte est qu'une adresse secondaire ne peut pas être en même temps sur
	 * la même commune qu'une adresse principale.
	 */
	@Test
	public void initAdressesPrincipalesEtPlusieursSeriesAdressesSecondairesMelangees() throws Exception {

		// 3 adresses principales à partir du 1er janvier 1970
		final Residence p1 = newResidencePrincipale(date(1970, 1, 1), null, date(1999, 12, 31), MockRue.CossonayVille.AvenueDuFuniculaire);
		final Residence p2 = newResidencePrincipale(date(1970, 1, 1), date(1982, 1, 1), date(1999, 12, 31), MockRue.CossonayVille.CheminDeRiondmorcel);
		final Residence p3 = newResidencePrincipale(date(2000, 1, 1), null, null, MockRue.Bussigny.RueDeLIndustrie);

		// 2 adresses secondaires sur Cully à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence s1 = newResidenceSecondaire(date(1970, 1, 1), null, null, MockRue.Cully.ChDesColombaires);
		final Residence s2 = newResidenceSecondaire(date(1970, 1, 1), date(1985, 1, 1), null, MockRue.Cully.PlaceDuTemple);

		// 2 adresses secondaires sur Epesse à partir du 1er janvier 1970, avec des dates de déménagement entre-mêlées avec celles des adresses principales
		final Residence z1 = newResidenceSecondaire(date(1970, 1, 1), null, date(1981, 12, 31), MockRue.Epesses.LaPlace);
		final Residence z2 = newResidenceSecondaire(date(1970, 1, 1), date(1978, 1, 1), date(1981, 12, 31), MockRue.Epesses.ChDuMont);

		// initialisation des adresses
		final Collection<Adresse> adresses =
				IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), Arrays.asList(p1, p2, p3, s1, s2, z1, z2), null, infraService);
		assertNotNull(adresses);
		assertEquals(7, adresses.size());

		final List<Adresse> principales = new ArrayList<>();
		final List<Adresse> secondaires = new ArrayList<>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
				secondaires.add(a);
			}
			else {
				fail();
			}
		}

		// les dates de fin des adresses principales ne doivent pas prendre en compte les adresses secondaires
		assertEquals(3, principales.size());
		assertAdresse(date(1970, 1, 1), date(1981, 12, 31), "Avenue du Funiculaire", "Cossonay-Ville", principales.get(0));
		assertAdresse(date(1982, 1, 1), date(1999, 12, 31), "Chemin de Riondmorcel", "Cossonay-Ville", principales.get(1));
		assertAdresse(date(2000, 1, 1), null, "Rue de l'Industrie", "Bussigny", principales.get(2));

		// les séries (= une série par commune) d'adresses secondaires ne doivent pas interagir entres-elles.
		assertEquals(4, secondaires.size());
		assertAdresse(date(1970, 1, 1), date(1977, 12, 31), "La Place", "Epesses", secondaires.get(0));
		assertAdresse(date(1970, 1, 1), date(1984, 12, 31), "Chemin des Colombaires", "Cully", secondaires.get(1));
		assertAdresse(date(1978, 1, 1), date(1981, 12, 31), "Chemin du Mont", "Epesses", secondaires.get(2));
		assertAdresse(date(1985, 1, 1), null, "Place du Temple", "Cully", secondaires.get(3));
	}

	/**
	 * [SIFISC-4833] Vérifie que les localisations précédentes et suivantes sont bien ignorées lors des déménagements à l'intérieur de la même commune (malgré le fait que RcPers renseigne
	 * systématiquement ces informations).
	 */
	@Test
	public void initAdressesLocalisationAvecDemenagementsDansLaCommune() throws Exception {

		final Residence r1 = newResidencePrincipale(date(2009, 9, 4), null, date(2009, 10, 31), MockRue.Echallens.GrandRue);
		r1.setGoesTo(newDestination(MockCommune.Lausanne));

		// 3 adresses à Lausanne avec les mêmes localisations
		final Residence r2 = newResidencePrincipale(date(2009, 11, 1), null, date(2012, 7, 3), MockRue.Lausanne.AvenueDeBeaulieu);
		r2.setComesFrom(newDestination(MockCommune.Echallens));
		r2.setGoesTo(newDestination(MockPays.France));

		final Residence r3 = newResidencePrincipale(date(2009, 11, 1), date(2010, 9, 1), date(2012, 7, 3), MockRue.Lausanne.AvenueJolimont);
		r3.setComesFrom(newDestination(MockCommune.Echallens));
		r3.setGoesTo(newDestination(MockPays.France));

		final Residence r4 = newResidencePrincipale(date(2009, 11, 1), date(2011, 7, 1), date(2012, 7, 3), MockRue.Lausanne.BoulevardGrancy);
		r4.setComesFrom(newDestination(MockCommune.Echallens));
		r4.setGoesTo(newDestination(MockPays.France));

		// initialisation des adresses
		final Collection<Adresse> adresses =
				IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), Arrays.asList(r1, r2, r3, r4), null, infraService);
		assertNotNull(adresses);
		assertEquals(5, adresses.size());       // 4 principales + une courrier créée depuis le 4.7.2012...

		final List<Adresse> principales = new ArrayList<>();
		final List<Adresse> secondaires = new ArrayList<>();
		final List<Adresse> contacts = new ArrayList<>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.SECONDAIRE) {
				secondaires.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				contacts.add(a);
			}
			else {
				fail();
			}
		}
		assertEmpty(secondaires);

		assertEquals(1, contacts.size());
		assertAdresse(date(2012, 7, 4), null, null, null, null, null, contacts.get(0));

		// les localisations des adresses principales doivent être nulles lors des déménagements
		assertEquals(4, principales.size());
		assertAdresse(date(2009, 9, 4), null, date(2009, 10, 31), newLocalisation(MockCommune.Lausanne), "Grand'Rue", "Echallens", principales.get(0));
		assertAdresse(date(2009, 11, 1), newLocalisation(MockCommune.Echallens), date(2010, 8, 31), null, "Avenue de Beaulieu", "Lausanne", principales.get(1));
		assertAdresse(date(2010, 9, 1), null, date(2011, 6, 30), null, "Avenue Jolimont", "Lausanne", principales.get(2));
		assertAdresse(date(2011, 7, 1), null, date(2012, 7, 3), newLocalisation(MockPays.France), "Boulevard de Grancy", "Lausanne", principales.get(3));
	}

	@Test
	public void testGetPersonWithHistoryValues() throws Exception {

		final Person person = newPerson(123345L, "Jean", "Rucher", date(1965, 3, 12), Sexe.MASCULIN);
		// les adresses
		person.getContactHistory().add(newContact(date(1965, 3, 12), date(1983, 7, 4), MockRue.Cully.ChDesColombaires));
		person.getContactHistory().add(newContact(date(1983, 7, 5), null, MockRue.Chamblon.RueDesUttins));
		person.getResidenceHistory().add(newResidencePrincipale(date(1965, 3, 12), null, date(1983, 7, 4), MockRue.Cully.ChDesColombaires));
		person.getResidenceHistory().add(newResidencePrincipale(date(1983, 7, 5), null, null, MockRue.Chamblon.RueDesUttins));

		// les états-civils
		person.getMaritalStatusHistory().add(newMaritalData(date(1965, 3, 12), TypeEtatCivil.CELIBATAIRE, null));
		person.getMaritalStatusHistory().add(newMaritalData(date(1989, 5, 1), TypeEtatCivil.MARIE, newPersonIdentification(562841L, "Toto", "Titi", null, Sexe.FEMININ)));

		// les permis
		person.getResidencePermitHistory().add(newResidencePermit(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT));

		// on vérifie que les valeurs historisées sont bien lues
		final Individu ind = IndividuRCPers.get(person, true, infraService);
		assertNotNull(ind);
		assertEquals(123345L, ind.getNoTechnique());
		assertEquals("Jean", ind.getPrenomUsuel());
		assertEquals("Rucher", ind.getNom());
		assertEquals(date(1965, 3, 12), ind.getDateNaissance());
		assertEquals(Sexe.MASCULIN, ind.getSexe());

		final Collection<Adresse> adresses = ind.getAdresses();
		assertNotNull(adresses);
		assertEquals(4, adresses.size());

		final List<Adresse> courriers = new ArrayList<>();
		final List<Adresse> principales = new ArrayList<>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				courriers.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
		}

		assertEquals(2, courriers.size());
		assertAdresse(date(1965, 3, 12), date(1983, 7, 4), "Chemin des Colombaires", "Cully", courriers.get(0));
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", courriers.get(1));

		assertEquals(2, principales.size());
		assertAdresse(date(1965, 3, 12), date(1983, 7, 4), "Chemin des Colombaires", "Cully", principales.get(0));
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", principales.get(1));

		final EtatCivilList ecList = ind.getEtatsCivils();
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1965, 3, 12), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(1989, 5, 1), TypeEtatCivil.MARIE, list.get(1));

		final PermisList permis = ind.getPermis();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT, permis.get(0));

		final List<RelationVersIndividu> conjoints = ind.getConjoints();
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertEquals(562841L, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(1989, 5, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	/**
	 * [SIFISC-5181] Ce test s'assure que les valeurs courantes sont bien prises en compte en mode non-historique.
	 */
	@Test
	public void testGetPersonWithCurrentValues() throws Exception {

		final Person person = newPerson(123345L, "Jean", "Rucher", date(1965, 3, 12), Sexe.MASCULIN);
		// les adresses courantes
		person.setCurrentContact(newContact(null, null, MockRue.Chamblon.RueDesUttins));
		person.getCurrentResidence().add(newResidencePrincipale(date(1983, 7, 5), null, null, MockRue.Chamblon.RueDesUttins));

		// l'état-civil courant
		person.setCurrentMaritalStatus(newMaritalData(date(1989, 5, 1), TypeEtatCivil.MARIE, newPersonIdentification(562841L, "Toto", "Titi", null, Sexe.FEMININ)));

		// le permis courant
		person.setCurrentResidencePermit(newResidencePermit(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT));

		// on vérifie que les valeurs courantes sont bien lues
		final Individu ind = IndividuRCPers.get(person, false, infraService);
		assertNotNull(ind);
		assertEquals(123345L, ind.getNoTechnique());
		assertEquals("Jean", ind.getPrenomUsuel());
		assertEquals("Rucher", ind.getNom());
		assertEquals(date(1965, 3, 12), ind.getDateNaissance());
		assertEquals(Sexe.MASCULIN, ind.getSexe());

		final Collection<Adresse> adresses = ind.getAdresses();
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		final List<Adresse> courriers = new ArrayList<>();
		final List<Adresse> principales = new ArrayList<>();
		for (Adresse a : adresses) {
			if (a.getTypeAdresse() == TypeAdresseCivil.COURRIER) {
				courriers.add(a);
			}
			else if (a.getTypeAdresse() == TypeAdresseCivil.PRINCIPALE) {
				principales.add(a);
			}
		}

		assertEquals(1, courriers.size());
		assertAdresse(null, null, "Rue des Uttins", "Chamblon", courriers.get(0));

		assertEquals(1, principales.size());
		assertAdresse(date(1983, 7, 5), null, "Rue des Uttins", "Chamblon", principales.get(0));

		final EtatCivilList ecList = ind.getEtatsCivils();
		assertNotNull(ecList);

		final List<EtatCivil> list = ecList.asList();
		assertEquals(1, list.size());
		assertEtatCivil(date(1989, 5, 1), TypeEtatCivil.MARIE, list.get(0));

		final PermisList permis = ind.getPermis();
		assertNotNull(permis);
		assertEquals(1, permis.size());
		assertPermis(date(1965, 3, 12), null, TypePermis.ETABLISSEMENT, permis.get(0));

		final List<RelationVersIndividu> conjoints = ind.getConjoints();
		assertNotNull(conjoints);
		assertEquals(1, conjoints.size());

		final RelationVersIndividu conjoint = conjoints.get(0);
		assertEquals(562841L, conjoint.getNumeroAutreIndividu());
		assertEquals(TypeRelationVersIndividu.CONJOINT, conjoint.getTypeRelation());
		assertEquals(date(1989, 5, 1), conjoint.getDateDebut());
		assertNull(conjoint.getDateFin());
	}

	private static void assertPermis(RegDate dateDebut, @Nullable RegDate dateFin, TypePermis typePermis, Permis permis) {
		assertNotNull(permis);
		assertEquals(dateDebut, permis.getDateDebut());
		assertEquals(dateFin, permis.getDateFin());
		assertEquals(typePermis, permis.getTypePermis());
	}

	private enum TypeRelation {

		VERS_CONJOINT("1"),
		VERS_PARTENAIRE_ENREGISTRE("2"),
		VERS_MERE("3"),
		VERS_PERE("4"),
		VERS_FILLE("101"),
		VERS_FILS("102");

		private final String echCode;

		TypeRelation(String echCode) {
			this.echCode = echCode;
		}

		public String getEchCode() {
			return echCode;
		}
	}

	private static ResidencePermit newResidencePermit(RegDate dateDebut, @Nullable RegDate dateFin, TypePermis typePermis) {
		final ResidencePermit permit = new ResidencePermit();
		permit.setResidencePermitValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		permit.setResidencePermitTill(XmlUtils.regdate2xmlcal(dateFin));
		permit.setResidencePermit(TypePermis.toEch(typePermis));
		return permit;
	}

	private static ResidencePermit newResidencePermitTypeInvalide(RegDate dateDebut, @Nullable RegDate dateFin) {
		final ResidencePermit permit = new ResidencePermit();
		permit.setResidencePermitValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		permit.setResidencePermitTill(XmlUtils.regdate2xmlcal(dateFin));
		permit.setResidencePermit("23");
		return permit;
	}

	private static Contact newContact(RegDate dateDebut, @Nullable RegDate dateFin, MockRue rue) {
		final Contact contact = new Contact();
		final MailAddress mailAddress = newMailAddress(rue);
		contact.setContact(mailAddress);
		contact.setContactValidFrom(XmlUtils.regdate2xmlcal(dateDebut));
		contact.setContactValidTill(XmlUtils.regdate2xmlcal(dateFin));
		return contact;
	}

	private static MailAddress newMailAddress(MockRue rue) {
		final MailAddress mailAddress = new MailAddress();
		mailAddress.setAddressInformation(newAddressInformation(rue));
		return mailAddress;
	}

	private static Person newPerson(long noInd, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final Person person = new Person();
		final Identity identity = new Identity();
		identity.setCallName(prenom);
		identity.setLocalPersonId(new NamedPersonId("ch.vd.rcpers", String.valueOf(noInd)));
		identity.setOfficialName(nom);
		identity.setDateOfBirth(EchHelper.partialDateToEch44(dateNaissance));
		identity.setSex(EchHelper.sexeToEch44(sexe));
		person.setIdentity(identity);
		return person;
	}

	private static PersonIdentification newPersonIdentification(long noInd, String prenom, String nom, RegDate dateNaissance, Sexe sexe) {
		final PersonIdentificationPartner pip = new PersonIdentificationPartner();
		pip.setFirstName(prenom);
		pip.setLocalPersonId(new NamedPersonId("ch.vd.rcpers", String.valueOf(noInd)));
		pip.setOfficialName(nom);
		pip.setDateOfBirth(EchHelper.partialDateToEch44(dateNaissance));
		pip.setSex(EchHelper.sexeToEch44(sexe));

		final PersonIdentification pi = new PersonIdentification();
		pi.setIdentification(pip);
		pi.setPersonLink("toto/" + noInd);
		return pi;
	}

	public static void assertAdresse(@Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String rue, @Nullable String localite, Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
		assertEquals(rue, adresse.getRue());
		assertEquals(localite, adresse.getLocalite());
	}

	public static void assertAdresse(@NotNull TypeAdresseCivil type, @Nullable RegDate dateDebut, @Nullable RegDate dateFin, @Nullable String rue, @Nullable String localite, Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEquals(dateFin, adresse.getDateFin());
		assertEquals(rue, adresse.getRue());
		assertEquals(localite, adresse.getLocalite());
		assertEquals(type, adresse.getTypeAdresse());
	}

	public static void assertAdresse(@Nullable RegDate dateDebut, @Nullable Localisation provenance, @Nullable RegDate dateFin, @Nullable Localisation destination, @Nullable String rue,
	                                  @Nullable String localite, Adresse adresse) {
		assertNotNull(adresse);
		assertEquals(dateDebut, adresse.getDateDebut());
		assertEqualsLocalisations(provenance, adresse.getLocalisationPrecedente());
		assertEquals(dateFin, adresse.getDateFin());
		assertEqualsLocalisations(destination, adresse.getLocalisationSuivante());
		assertEquals(rue, adresse.getRue());
		assertEquals(localite, adresse.getLocalite());
	}

	private static void assertEqualsLocalisations(Localisation left, Localisation right) {
		if ((left == null && right != null) || (left != null && right == null)) {
			assertEquals(left, right);
		}
		else if (left == null) {
			// ok
		}
		else {
			assertEquals(left.getType(), right.getType());
			assertEquals(left.getNoOfs(), right.getNoOfs());
		}
	}

	private static Residence newResidencePrincipale(RegDate arrivalDate, @Nullable RegDate movingDate, @Nullable RegDate departureDate, MockRue rue) {
		Commune commune = rue.getLocalite().getCommuneLocalite();
		final SwissMunicipality municipality = newSwissMunicipality(commune);
		final DwellingAddress dwellingAddress = newDwellingAddress(movingDate, rue);
		return new Residence(new Residence.MainResidence(), null, null, municipality, municipality, XmlUtils.regdate2xmlcal(arrivalDate), null, dwellingAddress, XmlUtils.regdate2xmlcal(
				departureDate), null);
	}

	private static Residence newResidenceSecondaire(RegDate arrivalDate, @Nullable RegDate movingDate, @Nullable RegDate departureDate, MockRue rue) {
		Commune commune = rue.getLocalite().getCommuneLocalite();
		final SwissMunicipality municipality = newSwissMunicipality(commune);
		final DwellingAddress dwellingAddress = newDwellingAddress(movingDate, rue);
		return new Residence(null, null, new Residence.OtherResidence(), municipality, municipality, XmlUtils.regdate2xmlcal(arrivalDate), null, dwellingAddress, XmlUtils.regdate2xmlcal(
				departureDate), null);
	}

	private static Destination newUnknownDestination() {
		final Destination d = new Destination();
		d.setUnknown(StringUtils.EMPTY);
		return d;
	}

	private static Destination newDestination(MockCommune commune) {
		final Destination d = new Destination();
		d.setSwissTown(newSwissMunicipality(commune));
		return d;
	}

	private static Destination newDestination(MockPays pays) {
		final Destination d = new Destination();
		d.setForeignCountry(newForeignCountry(pays));
		return d;
	}

	private static Destination.ForeignCountry newForeignCountry(MockPays pays) {
		Destination.ForeignCountry fc = new Destination.ForeignCountry();
		fc.setCountry(newCountry(pays));
		return fc;
	}

	private static Country newCountry(MockPays pays) {
		final Country country = new Country();
		country.setCountryId(pays.getNoOFS());
		country.setCountryIdISO2(pays.getCodeIso2());
		country.setCountryNameShort(pays.getNomCourt());
		return country;
	}

	private static SwissMunicipality newSwissMunicipality(Commune commune) {
		return new SwissMunicipality(commune.getNoOFS(), commune.getNomOfficiel(), EchHelper.sigleCantonToAbbreviation(commune.getSigleCanton()), null);
	}

	private static DwellingAddress newDwellingAddress(RegDate movingDate, MockRue rue) {
		final DwellingAddress address = new DwellingAddress();
		address.setMovingDate(XmlUtils.regdate2xmlcal(movingDate));
		address.setAddress(newSwissAddressInformation(rue));
		return address;
	}

	private static SwissAddressInformation newSwissAddressInformation(MockRue rue) {
		final SwissAddressInformation info = new SwissAddressInformation();
		info.setStreet(rue.getDesignationCourrier());
		info.setSwissZipCode(rue.getLocalite().getNPA());
		info.setSwissZipCodeId(rue.getLocalite().getNoOrdre());
		info.setTown(rue.getLocalite().getNom());
		return info;
	}

	private static AddressInformation newAddressInformation(MockRue rue) {
		AddressInformation info = new AddressInformation();
		info.setStreet(rue.getDesignationCourrier());
		info.setSwissZipCodeId(rue.getLocalite().getNPA());
		info.setTown(rue.getLocalite().getNom());
		info.setCountry("CH"); // on ne connaît pas les rues des autres pays
		return info;
	}

	private static void assertEtatCivil(RegDate dateDebut, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(type, etat.getTypeEtatCivil());
	}

	private static MaritalData newMaritalData(RegDate date, String type, @Nullable PersonIdentification partner) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		data.setPartner(partner);
		return data;
	}

	private static MaritalData newMaritalData(RegDate date, TypeEtatCivil etatCivil, @Nullable PersonIdentification partner) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(EchHelper.etatCivilToEch11(etatCivil));
		data.setPartner(partner);
		return data;
	}

	private static MaritalData newMaritalData(RegDate date, String type, @Nullable PersonIdentification partner, RegDate separation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		data.setPartner(partner);

		final MaritalData.Separation sep = new MaritalData.Separation();
		sep.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		data.getSeparation().add(sep);

		return data;
	}

	private MaritalData newMaritalData(RegDate date, String type, @Nullable PersonIdentification partner, RegDate separation, RegDate reconciliation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		data.setPartner(partner);

		final MaritalData.Separation sep = new MaritalData.Separation();
		sep.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		sep.setSeparationTill(XmlUtils.regdate2xmlcal(reconciliation));
		data.getSeparation().add(sep);

		return data;
	}

	@Test
	public void testInitPrenom() throws Exception {
		assertNull(IndividuRCPers.initPrenom(null, null));
		assertNull(IndividuRCPers.initPrenom("", ""));
		assertNull(IndividuRCPers.initPrenom(" ", "  "));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", null));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", "Jacques Paul Henri"));
		assertEquals("Paul", IndividuRCPers.initPrenom("Paul", "Jacques Paul Henri"));
		assertEquals("Jacques", IndividuRCPers.initPrenom(null, "Jacques"));
		assertEquals("Jacques", IndividuRCPers.initPrenom(null, "Jacques Paul Henri"));
		assertEquals("Jacques", IndividuRCPers.initPrenom("  ", "Jacques Paul Henri"));
		assertEquals("Jacques-Martin", IndividuRCPers.initPrenom(null, "Jacques-Martin Paul Henri"));
		assertEquals("Jacques-Martin", IndividuRCPers.initPrenom(null, " Jacques-Martin Paul Henri"));
	}

	/**
	 * NPE au cas où deux adresses de résidence (par exemple de type différent) commencent à la même date dans
	 * le code d'initialisation des adresses en mode "non-historique" (on en a vu en TE...)
	 */
	@Test
	public void testInitAvecPlusieursResidencesDeTypesDifferentsALaMemeDateSurLaMemeCommune() throws Exception {
		final RegDate arrival = date(2007, 11, 4);
		final List<Residence> residences = new ArrayList<>();
		final Residence prn = newResidencePrincipale(arrival, null, null, MockRue.Lausanne.AvenueDeBeaulieu);
		final Residence sec = newResidenceSecondaire(arrival, null, null, MockRue.Lausanne.AvenueDeLaGare);
		residences.add(prn);
		residences.add(sec);
		final Collection<Adresse> adresses = IndividuRCPers.initAdresses(null, null, residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());
	}

	/**
	 * NPE au cas où deux adresses de résidence (par exemple de type différent) dans la même commune
	 * le code d'initialisation des adresses en mode "non-historique" (on en a vu en TE...)
	 */
	@Test
	public void testInitAvecPlusieursResidencesDeTypesDifferentsSurLaMemeCommune() throws Exception {
		final RegDate arrivalPrn = date(2007, 11, 4);
		final RegDate arrivalSec = date(2007, 11, 6);
		final List<Residence> residences = new ArrayList<>();
		final Residence prn = newResidencePrincipale(arrivalPrn, null, null, MockRue.Lausanne.AvenueDeBeaulieu);
		final Residence sec = newResidenceSecondaire(arrivalSec, null, null, MockRue.Lausanne.AvenueDeLaGare);
		residences.add(prn);
		residences.add(sec);
		final Collection<Adresse> adresses = IndividuRCPers.initAdresses(null, null, residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());
	}

	@Test
	public void testRemplissageTrouAdresseSansContactNiGoesTo() throws Exception {

		// cas sans aucune indication de destination

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());
			assertNull(adr.getLocalisationSuivante());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, adr.getNoOfsPays());
		}
	}

	@Test
	public void testRemplissageTrouAdresseSansContactGoesToInconnu() throws Exception {

		// cas avec indication de destination inconnue

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		res.setGoesTo(new Destination(StringUtils.EMPTY, null, null, null));    // <-- unknown

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());

			final Localisation goesTo = adr.getLocalisationSuivante();
			assertNotNull(goesTo);
			assertEquals(LocalisationType.HORS_SUISSE, goesTo.getType());
			assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, goesTo.getNoOfs());
			assertNull(goesTo.getAdresseCourrier());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, adr.getNoOfsPays());
		}
	}

	@Test
	public void testRemplissageTrouAdresseSansContactGoesToEtrangerAvecVille() throws Exception {

		// cas avec indication de destination à l'étranger avec une ville donnée

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final MockPays pays = MockPays.Espagne;
		final String ville = "Madrid";
		res.setGoesTo(new Destination(null, null, new Destination.ForeignCountry(newCountry(pays), ville), null));

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());

			final Localisation goesTo = adr.getLocalisationSuivante();
			assertNotNull(goesTo);
			assertEquals(LocalisationType.HORS_SUISSE, goesTo.getType());
			assertEquals((Integer) pays.getNoOFS(), goesTo.getNoOfs());

			final Adresse courrier = goesTo.getAdresseCourrier();
			assertNotNull(courrier);
			assertEquals(TypeAdresseCivil.COURRIER, courrier.getTypeAdresse());
			assertNull(courrier.getDateDebut());
			assertNull(courrier.getDateFin());
			assertEquals(ville, courrier.getLocalite());
			assertEquals((Integer) pays.getNoOFS(), courrier.getNoOfsPays());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(ville, adr.getLocalite());
			assertEquals((Integer) pays.getNoOFS(), adr.getNoOfsPays());
		}
	}

	@Test
	public void testRemplissageTrouAdresseSansContactGoesToEtrangerAvecVilleEtDetail() throws Exception {

		// cas avec indication de destination à l'étranger avec une ville donnée et un détail

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final MockPays pays = MockPays.Espagne;
		final String ville = "Madrid";
		res.setGoesTo(new Destination(null, null, new Destination.ForeignCountry(newCountry(pays), ville), newAddressInformation(MockRue.Echallens.GrandRue)));

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());

			final Localisation goesTo = adr.getLocalisationSuivante();
			assertNotNull(goesTo);
			assertEquals(LocalisationType.HORS_SUISSE, goesTo.getType());
			assertEquals((Integer) pays.getNoOFS(), goesTo.getNoOfs());

			final Adresse courrier = goesTo.getAdresseCourrier();
			assertNotNull(courrier);
			assertEquals(TypeAdresseCivil.COURRIER, courrier.getTypeAdresse());
			assertNull(courrier.getDateDebut());
			assertNull(courrier.getDateFin());
			assertEquals(MockLocalite.Echallens.getNom(), courrier.getLocalite());
			assertEquals(MockRue.Echallens.GrandRue.getDesignationCourrier(), courrier.getRue());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Echallens.getNom(), adr.getLocalite());
			assertEquals(MockRue.Echallens.GrandRue.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testRemplissageTrouAdresseSansContactGoesToSuisse() throws Exception {

		// cas avec indication de destination en Suisse sans détail

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final MockCommune commune = MockCommune.Aigle;
		res.setGoesTo(newDestination(commune));

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());

			final Localisation goesTo = adr.getLocalisationSuivante();
			assertNotNull(goesTo);
			assertEquals(LocalisationType.CANTON_VD, goesTo.getType());
			assertEquals((Integer) commune.getNoOFS(), goesTo.getNoOfs());

			final Adresse courrier = goesTo.getAdresseCourrier();
			assertNull(courrier);
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals((Integer) ServiceInfrastructureRaw.noOfsSuisse, adr.getNoOfsPays());
		}
	}

	@Test
	public void testRemplissageTrouAdresseSansContactGoesToSuisseAvecDetail() throws Exception {

		// cas avec indication de destination en Suisse avec détail

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final MockCommune commune = MockCommune.Aubonne;
		final Destination destination = newDestination(commune);
		destination.setMailAddress(newAddressInformation(MockRue.Aubonne.CheminTraverse));
		res.setGoesTo(destination);

		final List<Residence> residences = Collections.singletonList(res);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());

			final Localisation goesTo = adr.getLocalisationSuivante();
			assertNotNull(goesTo);
			assertEquals(LocalisationType.CANTON_VD, goesTo.getType());
			assertEquals((Integer) commune.getNoOFS(), goesTo.getNoOfs());

			final Adresse courrier = goesTo.getAdresseCourrier();
			assertNotNull(courrier);
			assertEquals(TypeAdresseCivil.COURRIER, courrier.getTypeAdresse());
			assertNull(courrier.getDateDebut());
			assertNull(courrier.getDateFin());
			assertEquals(MockLocalite.Aubonne.getNom(), courrier.getLocalite());
			assertEquals(MockRue.Aubonne.CheminTraverse.getDesignationCourrier(), courrier.getRue());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Aubonne.getNom(), adr.getLocalite());
			assertEquals(MockRue.Aubonne.CheminTraverse.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testRemplissageTrouSansRemplissagePourCauseAdresseCourrierExistante() throws Exception {

		// cas avec indication de destination en Suisse avec détail

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final Residence res = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final Contact ctct = newContact(depart.getOneDayAfter(), null, MockRue.Bussigny.RueDeLIndustrie);

		final List<Residence> residences = Collections.singletonList(res);
		final List<Contact> contacts = Collections.singletonList(ctct);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, contacts, residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(2, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());
			assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
			assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
		}

		// adresse courrier explicitement donnée
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Bussigny.getNom(), adr.getLocalite());
			assertEquals(MockRue.Bussigny.RueDeLIndustrie.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testRemplissageTrouAvecDateFin() throws Exception {

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final RegDate nouvelleArrivee = depart.addYears(2);
		final Residence res1 = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final Residence res2 = newResidencePrincipale(nouvelleArrivee, null, null, MockRue.Echallens.GrandRue);

		final List<Residence> residences = Arrays.asList(res1, res2);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(3, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());
			assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
			assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertEquals(nouvelleArrivee.getOneDayBefore(), adr.getDateFin());
			assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, adr.getNoOfsPays());
		}

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(2);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(nouvelleArrivee, adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Echallens.getNom(), adr.getLocalite());
			assertEquals(MockRue.Echallens.GrandRue.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testRemplissageTrouAvecAdresseSecondaire() throws Exception {

		// les adresses secondaires doivent être ignorées -> un trou reste à combler...

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final RegDate nouvelleArrivee = depart.addYears(2);
		final Residence res1 = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final Residence res2 = newResidenceSecondaire(nouvelleArrivee, null, null, MockRue.Echallens.GrandRue);

		final List<Residence> residences = Arrays.asList(res1, res2);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, Collections.<Contact>emptyList(), residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(3, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());
			assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
			assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
		}

		// adresse courrier ajoutée pour combler le trou depuis le départ
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.getOneDayAfter(), adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, adr.getNoOfsPays());
		}

		// adresse secondaire explicitement donnée
		{
			final Adresse adr = adresses.get(2);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.SECONDAIRE, adr.getTypeAdresse());
			assertEquals(nouvelleArrivee, adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Echallens.getNom(), adr.getLocalite());
			assertEquals(MockRue.Echallens.GrandRue.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testRemplissageTrouAvecDecalageDateDebut() throws Exception {

		// ici, on a une adresse courrier qui revouvre une partie du trou des adresses de résidence
		// on vérifie que l'adresse qui comble le trou restant :
		// 1. a bien les bonnes dates (i.e. commence après l'adresse courrier existante)
		// 2. a bien les bonnes information (récupérées sur la dernière adresse de résidence connue avant le trou)

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate depart = date(2008, 12, 3);
		final RegDate nouvelleArrivee = depart.addYears(2);
		final Residence res1 = newResidencePrincipale(arrivee, null, depart, MockRue.Lausanne.AvenueDeBeaulieu);
		final Destination destination = newDestination(MockPays.France);
		destination.getForeignCountry().setTown("Paris");
		res1.setGoesTo(destination);
		final Residence res2 = newResidencePrincipale(nouvelleArrivee, null, null, MockRue.Echallens.GrandRue);
		final Contact ctct = newContact(depart.addMonths(-1), depart.addMonths(1), MockRue.Bussigny.RueDeLIndustrie);

		final List<Residence> residences = Arrays.asList(res1, res2);
		final List<Contact> contacts = Collections.singletonList(ctct);
		final List<Adresse> adresses = IndividuRCPers.initAdresses(null, contacts, residences, null, infraService);
		assertNotNull(adresses);
		assertEquals(4, adresses.size());

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(0);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(arrivee, adr.getDateDebut());
			assertEquals(depart, adr.getDateFin());
			assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
			assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
		}

		// adresse courrier existante
		{
			final Adresse adr = adresses.get(1);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.addMonths(-1), adr.getDateDebut());
			assertEquals(depart.addMonths(1), adr.getDateFin());
			assertEquals(MockLocalite.Bussigny.getNom(), adr.getLocalite());
			assertEquals(MockRue.Bussigny.RueDeLIndustrie.getDesignationCourrier(), adr.getRue());
		}

		// adresse courrier ajoutée pour combler le trou depuis la fin de l'adresse courrier existante
		{
			final Adresse adr = adresses.get(2);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.COURRIER, adr.getTypeAdresse());
			assertEquals(depart.addMonths(1).getOneDayAfter(), adr.getDateDebut());
			assertEquals(nouvelleArrivee.getOneDayBefore(), adr.getDateFin());
			assertEquals((Integer) MockPays.France.getNoOFS(), adr.getNoOfsPays());
			assertEquals("Paris", adr.getLocalite());
		}

		// adresse principale explicitement donnée
		{
			final Adresse adr = adresses.get(3);
			assertNotNull(adr);
			assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
			assertEquals(nouvelleArrivee, adr.getDateDebut());
			assertNull(adr.getDateFin());
			assertEquals(MockLocalite.Echallens.getNom(), adr.getLocalite());
			assertEquals(MockRue.Echallens.GrandRue.getDesignationCourrier(), adr.getRue());
		}
	}

	@Test
	public void testIgnoranceFinAdresseResidencePourDeces() throws Exception {

		final RegDate arrivee = date(2001, 1, 1);
		final RegDate deces = date(2008, 12, 3);

		final Residence res = newResidencePrincipale(arrivee, null, deces, MockRue.Lausanne.AvenueDeBeaulieu);
		res.setGoesTo(newUnknownDestination());     // c'est en général le cas pour un décès...

		// premier test avec la date de décès (on doit donc ignorer la date de fin de l'adresse)
		{
			final List<Adresse> adresses = IndividuRCPers.initAdresses(null, null, Collections.singletonList(res), deces, infraService);
			assertNotNull(adresses);
			assertEquals(1, adresses.size());

			{
				final Adresse adr = adresses.get(0);
				assertNotNull(adr);
				assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
				assertEquals(arrivee, adr.getDateDebut());
				assertNull(adr.getDateFin());
				assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
				assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
				assertNull(adr.getLocalisationPrecedente());
				assertNull(adr.getLocalisationSuivante());
			}
		}

		// second test sans la date de décès (là, on n'ignore pas la fin civile de l'adresse)
		{
			final List<Adresse> adresses = IndividuRCPers.initAdresses(null, null, Collections.singletonList(res), null, infraService);
			assertNotNull(adresses);
			assertEquals(1, adresses.size());

			{
				final Adresse adr = adresses.get(0);
				assertNotNull(adr);
				assertEquals(TypeAdresseCivil.PRINCIPALE, adr.getTypeAdresse());
				assertEquals(arrivee, adr.getDateDebut());
				assertEquals(deces, adr.getDateFin());
				assertEquals(MockLocalite.Lausanne.getNom(), adr.getLocalite());
				assertEquals(MockRue.Lausanne.AvenueDeBeaulieu.getDesignationCourrier(), adr.getRue());
				assertNull(adr.getLocalisationPrecedente());
				assertNotNull(adr.getLocalisationSuivante());
				assertEquals(LocalisationType.HORS_SUISSE, adr.getLocalisationSuivante().getType());
				assertEquals((Integer) ServiceInfrastructureRaw.noPaysInconnu, adr.getLocalisationSuivante().getNoOfs());
			}
		}
	}

	@Test
	public void testGetPersonorWithTypePermisInvalide() throws Exception {

		final Person person = newPerson(123345L, "Jean", "Rucher", date(1965, 3, 12), Sexe.MASCULIN);

		// les permis
		person.getResidencePermitHistory().add(newResidencePermitTypeInvalide(date(1965, 3, 12), null));


		// on vérifie que les valeurs historisées sont bien lues
		try {
			final Individu ind = IndividuRCPers.get(person, true, infraService);
		}catch (TypePermisInvalideException e){
			assertEquals(e.getMessage(),"Impossible d'interpréter le type de  permis pour l'individu n°123345, détails: Erreur détectée: °Type de permis non reconnu: '23'");
		}


	}
}
