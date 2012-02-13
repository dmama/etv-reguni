package ch.vd.uniregctb.interfaces.model.impl;

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

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.CELIBATAIRE, etat);
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, etat);
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, etat);
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, etat);
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, etat);
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, etat);
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");
		data.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));

		final EtatCivil etat = EtatCivilRCPers.get(data);
		assertNotNull(etat);
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.PACS, etat);
	}

	private static void assertEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(dateFin, etat.getDateFin());
		assertEquals(type, etat.getTypeEtatCivil());
	}
}
