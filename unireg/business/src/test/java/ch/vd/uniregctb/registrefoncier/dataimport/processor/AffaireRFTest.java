package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.Collections;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AffaireRFTest {

	private ImmeubleRF immeuble = new BienFondsRF();

	@Test
	public void testCalculateDateEtMotifDebutAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		muts.refreshDatesDebutMetier(null);
		assertNull(d.getDateDebutMetier());
		assertNull(d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		muts.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(2000, 3, 23), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testCalculateDateEtMotifDebutUneRaisonAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		muts.refreshDatesDebutMetier(null);
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

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		muts.refreshDatesDebutMetier(null);
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

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		muts.refreshDatesDebutMetier(null);
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

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("28288228");
		nouveau.setVersionIdRF("2");
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final AffaireRF muts = new AffaireRF(null, immeuble, Collections.singletonList(nouveau), Collections.emptyList(), Collections.singletonList(precedent));
		muts.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}
}