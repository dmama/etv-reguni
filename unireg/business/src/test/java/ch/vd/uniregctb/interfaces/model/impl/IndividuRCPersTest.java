package ch.vd.uniregctb.interfaces.model.impl;

import java.util.Arrays;
import java.util.List;

import ch.ech.ech0011.v5.MaritalData;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class IndividuRCPersTest extends WithoutSpringTest {

	@Test
	public void testGetCelibataire() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final List<MaritalData> statuses = Arrays.asList(celibataire);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(1960, 1, 1), null, TypeEtatCivil.CELIBATAIRE, list.get(0));
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2");
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, list.get(1));
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2");
		final MaritalData separe = newMaritalData(date(2000, 1, 1), "2", date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie, separe);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, list.get(2));
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData marie = newMaritalData(date(2000, 1, 1), "2");
		final MaritalData separe = newMaritalData(date(2000, 1, 1), "2", date(2005, 5, 29));
		final MaritalData reconcilie = newMaritalData(date(2000, 1, 1), "2", date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, marie, separe, reconcilie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.MARIE, list.get(1));
		assertEtatCivil(date(2005, 5, 29), date(2005, 10, 3), TypeEtatCivil.SEPARE, list.get(2));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, list.get(3));
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6");
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, list.get(1));
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6");
		final MaritalData separe = newMaritalData(date(2000, 1, 1), "6", date(2005, 5, 29));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse, separe);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, list.get(2));
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData celibataire = newMaritalData(date(1960, 1, 1), "1");
		final MaritalData pacse = newMaritalData(date(2000, 1, 1), "6");
		final MaritalData separe = newMaritalData(date(2000, 1, 1), "6", date(2005, 5, 29));
		final MaritalData reconcilie = newMaritalData(date(2000, 1, 1), "6", date(2005, 5, 29), date(2005, 10, 4));
		final List<MaritalData> statuses = Arrays.asList(celibataire, pacse, separe, reconcilie);

		final List<EtatCivil> list = IndividuRCPers.initEtatsCivils(statuses);
		assertNotNull(list);
		assertEquals(4, list.size());
		assertEtatCivil(date(1960, 1, 1), date(1999, 12, 31), TypeEtatCivil.CELIBATAIRE, list.get(0));
		assertEtatCivil(date(2000, 1, 1), date(2005, 5, 28), TypeEtatCivil.PACS, list.get(1));
		assertEtatCivil(date(2005, 5, 29), date(2005, 10, 3), TypeEtatCivil.PACS_INTERROMPU, list.get(2));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.PACS, list.get(3));
	}

	private static void assertEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(dateFin, etat.getDateFin());
		assertEquals(type, etat.getTypeEtatCivil());
	}

	private static MaritalData newMaritalData(RegDate date, String type) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		return data;
	}

	private static MaritalData newMaritalData(RegDate date, String type, RegDate separation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		return data;
	}

	private MaritalData newMaritalData(RegDate date, String type, RegDate separation, RegDate reconciliation) {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date));
		data.setMaritalStatus(type);
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(separation));
		data.setSeparationTill(XmlUtils.regdate2xmlcal(reconciliation));
		return data;
	}
}
