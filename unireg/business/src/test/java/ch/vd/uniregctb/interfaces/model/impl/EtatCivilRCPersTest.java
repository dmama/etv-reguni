package ch.vd.uniregctb.interfaces.model.impl;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.evd0001.v3.MaritalData;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.EtatCivilRCPers;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.common.XmlUtils;

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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.CELIBATAIRE, etats.get(0));
	}

	@Test
	public void testGetMarie() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("2");

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(1, etats.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, etats.get(0));
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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, etats.get(1));
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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, etats.get(2));
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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.MARIE, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.MARIE, etats.get(2));
		assertEtatCivil(date(2006, 1, 4), null, TypeEtatCivil.SEPARE, etats.get(3));
	}

	@Test
	public void testGetPacse() throws Exception {
		final MaritalData data = new MaritalData();
		data.setDateOfMaritalStatus(XmlUtils.regdate2xmlcal(date(2000, 1, 1)));
		data.setMaritalStatus("6");

		final List<EtatCivil> etats = EtatCivilRCPers.get(data);
		assertNotNull(etats);
		assertEquals(1, etats.size());
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, etats.get(0));
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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, etats.get(1));
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
		assertEtatCivil(date(2000, 1, 1), null, TypeEtatCivil.PACS, etats.get(0));
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.PACS_INTERROMPU, etats.get(1));
		assertEtatCivil(date(2005, 10, 4), null, TypeEtatCivil.PACS, etats.get(2));
		assertEtatCivil(date(2006, 1, 4), null, TypeEtatCivil.PACS_INTERROMPU, etats.get(3));
	}

	/**
	 * [SIFISC-4995] Dans le cas où un individu est célibataire puis immédiatement séparé, RcPers insère un état 'marié' avec date nulle.
	 * Ce test est là pour vérifier que cet état 'marié' artificiel est ignoré chez nous.
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
		assertEquals(1, etats.size());
		assertEtatCivil(date(2005, 5, 29), null, TypeEtatCivil.SEPARE, etats.get(0));
	}

	private static void assertEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil type, EtatCivil etat) {
		assertNotNull(etat);
		assertEquals(dateDebut, etat.getDateDebut());
		assertEquals(dateFin, etat.getDateFin());
		assertEquals(type, etat.getTypeEtatCivil());
	}
}
