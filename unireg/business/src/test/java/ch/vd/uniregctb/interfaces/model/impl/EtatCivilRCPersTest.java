package ch.vd.uniregctb.interfaces.model.impl;

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
import static org.junit.Assert.assertNull;

public class EtatCivilRCPersTest extends WithoutSpringTest {

	@Test
	public void testGetNull() throws Exception {
		assertNull(EtatCivilRCPers.get(null));
	}

	@Test
	public void testGetCelibataire() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("1");

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.CELIBATAIRE, list.get(0));
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, list.get(0));
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, list.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, list.get(1));
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, list.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, list.get(1));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, list.get(2));
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, list.get(0));
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(2, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, list.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, list.get(1));
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));

		final List<EtatCivil> list = EtatCivilRCPers.get(data);
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, list.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, list.get(1));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.PACS, list.get(2));
	}

	private static void assertEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(dateFin, etat.getDateFin());
		assertEquals(type, etat.getTypeEtatCivil());
	}
}
