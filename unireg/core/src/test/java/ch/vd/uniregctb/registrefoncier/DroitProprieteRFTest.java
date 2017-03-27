package ch.vd.uniregctb.registrefoncier;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DroitProprieteRFTest {

	@Test
	public void testCalculateDateEtMotifDebutAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.calculateDateEtMotifDebut();
		assertNull(d.getDateDebutMetier());
		assertNull(d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		d.calculateDateEtMotifDebut();
		assertEquals(RegDate.get(2000, 3, 23), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));
		d.calculateDateEtMotifDebut();
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutPlusieursRaisonsAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1996, 10, 1), "Achat", null));
		d.calculateDateEtMotifDebut();
		assertEquals(RegDate.get(1996, 10, 1), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutPlusieursRaisonsAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));
		d.calculateDateEtMotifDebut();
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}
}