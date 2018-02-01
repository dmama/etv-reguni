package ch.vd.unireg.interfaces.civil.data;

import java.util.List;

import org.junit.Test;

import ch.vd.evd0001.v5.MaritalData;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.WithoutSpringTest;
import ch.vd.unireg.common.XmlUtils;

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

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(1, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.CELIBATAIRE, etats.get(0));
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(1, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, etats.get(0));
	}

	@Test
	public void testGetMariePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		
		final MaritalData.Separation separation = new MaritalData.Separation();
		separation.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.getSeparation().add(separation);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(2, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, etats.get(1));
	}

	@Test
	public void testGetMariePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		
		final MaritalData.Separation separation = new MaritalData.Separation();
		separation.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		separation.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));
		data.getSeparation().add(separation);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(3, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.MARIE, etats.get(2));
	}

	@Test
	public void testGetMariePuisSeparePuisReconciliePuisResepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");
		
		final MaritalData.Separation separation1 = new MaritalData.Separation();
		separation1.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		separation1.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));
		data.getSeparation().add(separation1);
		
		final MaritalData.Separation separation2 = new MaritalData.Separation();
		separation2.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2006, 1, 4)));
		data.getSeparation().add(separation2);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(4, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.MARIE, etats.get(2));
		assertEtatCivil(date(2006, 1, 4), TypeEtatCivil.SEPARE, etats.get(3));
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(1, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, etats.get(0));
	}

	@Test
	public void testGetPacsePuisSepare() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");
		
		final MaritalData.Separation separation = new MaritalData.Separation();
		separation.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.getSeparation().add(separation);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(2, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, etats.get(1));
	}

	@Test
	public void testGetPacsePuisSeparePuisReconcilie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");

		final MaritalData.Separation separation1 = new MaritalData.Separation();
		separation1.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		separation1.setSeparationTill(XmlUtils.regdate2xmlcal(date(2005, 10, 4)));
		data.getSeparation().add(separation1);

		final MaritalData.Separation separation2 = new MaritalData.Separation();
		separation2.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2006, 1, 4)));
		data.getSeparation().add(separation2);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(4, etats.size());
		assertEtatCivil(date(2000, 1, 1), TypeEtatCivil.PACS, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.PACS_SEPARE, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), TypeEtatCivil.PACS, etats.get(2));
		assertEtatCivil(date(2006, 1, 4), TypeEtatCivil.PACS_SEPARE, etats.get(3));
	}

	/**
	 * [SIFISC-4995] Dans le cas où un individu est célibataire puis immédiatement séparé, RcPers insère un état 'marié' avec date nulle.
	 * [SIFISC-xxxx] On n'ignore plus cet état intermédiaire
	 */
	@Test
	public void testGetCelibatairePuisSepare() throws Exception {

		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(null);
		data.setMaritalStatus("2");

		final MaritalData.Separation separation = new MaritalData.Separation();
		separation.setDateOfSeparation(XmlUtils.regdate2xmlcal(date(2005, 5, 29)));
		data.getSeparation().add(separation);

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(2, etats.size());
		assertEtatCivil(null, TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), TypeEtatCivil.SEPARE, etats.get(1));
	}

	private static void assertEtatCivil(RegDate dateDebut, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(type, etat.getTypeEtatCivil());
	}
}
