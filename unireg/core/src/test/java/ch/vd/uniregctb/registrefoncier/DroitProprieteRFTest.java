package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("Duplicates")
public class DroitProprieteRFTest {

	@Test
	public void testCalculateDateEtMotifDebutAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.calculateDateEtMotifDebut(p -> null);
		assertNull(d.getDateDebutMetier());
		assertNull(d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		d.calculateDateEtMotifDebut(p -> null);
		assertEquals(RegDate.get(2000, 3, 23), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));
		d.calculateDateEtMotifDebut(p -> null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutPlusieursRaisonsAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1996, 10, 1), "Achat", null));
		d.calculateDateEtMotifDebut(p -> null);
		assertEquals(RegDate.get(1996, 10, 1), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutPlusieursRaisonsAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));
		d.calculateDateEtMotifDebut(p -> null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testCalculateDateEtMotifDebutAvecDroitPrecedent() throws Exception {
		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		precedent.calculateDateEtMotifDebut(p -> null);

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("28288228");
		nouveau.setVersionIdRF("2");
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 8, 2), "Remaniement PPE", null));
		nouveau.calculateDateEtMotifDebut(p -> precedent);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}
}