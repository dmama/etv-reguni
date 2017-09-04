package ch.vd.uniregctb.registrefoncier.dataimport.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.registrefoncier.BienFondsRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AffaireRFTest {

	private ImmeubleRF immeuble = new BienFondsRF();
	private Listener listener = new Listener();

	@Test
	public void testRefreshDateEtMotifDebutAucuneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesDebutMetier(null);
		assertNull(d.getDateDebutMetier());
		assertNull(d.getMotifDebut());
	}

	@Test
	public void testRefreshDateEtMotifDebutUneRaisonAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(2000, 3, 23), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDateEtMotifDebutUneRaisonAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesDebutMetier(null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDateEtMotifDebutPlusieursRaisonsAcquisition() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(1996, 10, 1), "Achat", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(1996, 10, 1), d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	@Test
	public void testRefreshDateEtMotifDebutPlusieursRaisonsAcquisitionDateNulle() throws Exception {
		final DroitProprieteRF d = new DroitProprietePersonnePhysiqueRF();
		d.setMasterIdRF("28288228");
		d.setVersionIdRF("1");
		d.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Succession", null));
		d.addRaisonAcquisition(new RaisonAcquisitionRF(null, "Achat", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(d), Collections.emptyList(), Collections.emptyList());
		affaire.refreshDatesDebutMetier(null);
		assertNull(d.getDateDebutMetier());
		assertEquals("Achat", d.getMotifDebut());
	}

	/**
	 * [SIFISC-24987] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDateEtMotifDebutAvecDroitPrecedentMemeMasterId() throws Exception {

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("28288228");
		nouveau.setVersionIdRF("2");
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(nouveau), Collections.emptyList(), Collections.singletonList(precedent));
		affaire.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}

	/**
	 * [SIFISC-25971] Ce test vérifie que la date de début métier d'un droit est bien déduite de la nouvelle raison d'acquisition pour un droit qui évolue (c'est-à-dire qu'il existe un droit précédent avec le même masterId).
	 */
	@Test
	public void testRefreshDateEtMotifDebutAvecDroitPrecedentMemeProprietaire() throws Exception {

		final PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
		pp.setId(1L);

		final DroitProprieteRF precedent = new DroitProprietePersonnePhysiqueRF();
		precedent.setMasterIdRF("28288228");
		precedent.setVersionIdRF("1");
		precedent.setAyantDroit(pp);
		precedent.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));

		final DroitProprieteRF nouveau = new DroitProprietePersonnePhysiqueRF();
		nouveau.setMasterIdRF("3838384444");
		nouveau.setVersionIdRF("1");
		nouveau.setAyantDroit(pp);
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2000, 3, 23), "Achat", null));
		nouveau.addRaisonAcquisition(new RaisonAcquisitionRF(RegDate.get(2005, 8, 2), "Remaniement PPE", null));

		final AffaireRF affaire = new AffaireRF(null, immeuble, Collections.singletonList(nouveau), Collections.emptyList(), Collections.singletonList(precedent));
		affaire.refreshDatesDebutMetier(null);
		assertEquals(RegDate.get(2005, 8, 2), nouveau.getDateDebutMetier());
		assertEquals("Remaniement PPE", nouveau.getMotifDebut());
	}

	/**
	 * [SIFISC-25583] Ce test vérifie que les dates de début métier sont bien calculées dans le cas où : <ul> <li>un droit existant est modifié (changement de part de propriété ou autre)</li> <li>aucun nouvelle raison d'acquisition n'existe sur le
	 * droit modifié</li> <li>un nouveau droit est ouvert avec une nouvelle raison d'acquisition</li> </ul> A ce moment-là, la date de début métier du droit modifié doit être égale à la date de début métier du nouveau droit.
	 * <p>
	 * <b>Cas métier:</b> CH707345958325 (succession au 31.07.2017)
	 */
	@Test
	public void testRefreshDatesDebutMetierAvecRaisonAcquisitionManquanteSurUnDesDroits() throws Exception {

		final RegDate dateAchat = RegDate.get(1970, 8, 20);
		final RegDate dateSuccession = RegDate.get(2017, 7, 31);

		// communauté André Piguet - Lucette Piguet
		final CommunauteRF communaute1 = new CommunauteRF();
		final PersonnePhysiqueRF andre = new PersonnePhysiqueRF();
		andre.setId(1L);
		final PersonnePhysiqueRF lucette = new PersonnePhysiqueRF();
		lucette.setId(2L);

		// droit initial de André Piguet
		final DroitProprietePersonnePhysiqueRF precedent1 = new DroitProprietePersonnePhysiqueRF();
		precedent1.setMasterIdRF("28288228");
		precedent1.setVersionIdRF("1");
		precedent1.setPart(new Fraction(1, 1));
		precedent1.setAyantDroit(andre);
		precedent1.setCommunaute(communaute1);
		precedent1.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// droit initial de Lucette Piguet
		final DroitProprietePersonnePhysiqueRF precedent2 = new DroitProprietePersonnePhysiqueRF();
		precedent2.setMasterIdRF("382818811");
		precedent2.setVersionIdRF("1");
		precedent2.setPart(new Fraction(1, 1));
		precedent2.setAyantDroit(lucette);
		precedent2.setCommunaute(communaute1);
		precedent2.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// communauté Lucette Piguet - Laurent Piguet - Evelyne Vultaggio
		final CommunauteRF communaute2 = new CommunauteRF();
		final PersonnePhysiqueRF laurent = new PersonnePhysiqueRF();
		laurent.setId(3L);
		final PersonnePhysiqueRF evelyne = new PersonnePhysiqueRF();
		evelyne.setId(4L);

		// droit modifié de Lucette Piguet <--- la raison d'acquisition ne change pas !
		final DroitProprietePersonnePhysiqueRF nouveau2 = new DroitProprietePersonnePhysiqueRF();
		nouveau2.setMasterIdRF("382818811");
		nouveau2.setVersionIdRF("2");
		nouveau2.setPart(new Fraction(1, 1));
		nouveau2.setAyantDroit(lucette);
		nouveau2.setCommunaute(communaute2);
		nouveau2.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// nouveau droit de Laurent Piguet
		final DroitProprietePersonnePhysiqueRF nouveau3 = new DroitProprietePersonnePhysiqueRF();
		nouveau3.setMasterIdRF("777433");
		nouveau3.setVersionIdRF("1");
		nouveau3.setPart(new Fraction(1, 1));
		nouveau3.setAyantDroit(laurent);
		nouveau3.setCommunaute(communaute2);
		nouveau3.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", null));

		// nouveau droit de Evelyne Vultaggio
		final DroitProprietePersonnePhysiqueRF nouveau4 = new DroitProprietePersonnePhysiqueRF();
		nouveau4.setMasterIdRF("91919191");
		nouveau4.setVersionIdRF("1");
		nouveau4.setPart(new Fraction(1, 1));
		nouveau4.setAyantDroit(evelyne);
		nouveau4.setCommunaute(communaute2);
		nouveau4.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession, "Succession", null));

		final AffaireRF affaire = new AffaireRF(dateSuccession, immeuble, Arrays.asList(nouveau2, nouveau3, nouveau4), Collections.emptyList(), Arrays.asList(precedent1, precedent2));
		affaire.refreshDatesDebutMetier(listener);
		assertNull(precedent1.getDateDebutMetier());    // pas de changement sur les droits précédents
		assertNull(precedent2.getDateDebutMetier());
		assertEquals(dateSuccession, nouveau2.getDateDebutMetier());   // <-- malgré l'absence de nouvelle raison d'acquisition, la raison d'acquisition "succession" est utilisée
		assertEquals(dateSuccession, nouveau3.getDateDebutMetier());
		assertEquals(dateSuccession, nouveau4.getDateDebutMetier());
		assertEquals("Succession", nouveau2.getMotifDebut());
		assertEquals("Succession", nouveau3.getMotifDebut());
		assertEquals("Succession", nouveau4.getMotifDebut());

		// les 3 droits sont mis-à-jour
		assertEquals(0, listener.getUntouched().size());
		assertEquals(0, listener.getCreated().size());
		final List<Listener.Update> updated = listener.getUpdated();
		assertEquals(3, updated.size());
		assertUpdated(nouveau2, null, null,  updated.get(0));
		assertUpdated(nouveau3, null, null,  updated.get(1));
		assertUpdated(nouveau4, null, null,  updated.get(2));
	}

	/**
	 * [SIFISC-25583] Variante du test {@link #testRefreshDatesDebutMetierAvecRaisonAcquisitionManquanteSurUnDesDroits()} avec des dates différentes sur les droits ouverts suite à la succession.
	 */
	@Test
	public void testRefreshDatesDebutMetierAvecRaisonAcquisitionManquanteSurUnDesDroitsVariante1() throws Exception {

		final RegDate dateAchat = RegDate.get(1970, 8, 20);
		final RegDate dateSuccession1 = RegDate.get(2017, 7, 26);
		final RegDate dateSuccession2 = RegDate.get(2017, 7, 31);

		// communauté André Piguet - Lucette Piguet
		final CommunauteRF communaute1 = new CommunauteRF();
		final PersonnePhysiqueRF andre = new PersonnePhysiqueRF();
		andre.setId(1L);
		final PersonnePhysiqueRF lucette = new PersonnePhysiqueRF();
		lucette.setId(2L);

		// droit initial de André Piguet
		final DroitProprietePersonnePhysiqueRF precedent1 = new DroitProprietePersonnePhysiqueRF();
		precedent1.setMasterIdRF("28288228");
		precedent1.setVersionIdRF("1");
		precedent1.setPart(new Fraction(1, 1));
		precedent1.setAyantDroit(andre);
		precedent1.setCommunaute(communaute1);
		precedent1.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// droit initial de Lucette Piguet
		final DroitProprietePersonnePhysiqueRF precedent2 = new DroitProprietePersonnePhysiqueRF();
		precedent2.setMasterIdRF("382818811");
		precedent2.setVersionIdRF("1");
		precedent2.setPart(new Fraction(1, 1));
		precedent2.setAyantDroit(lucette);
		precedent2.setCommunaute(communaute1);
		precedent2.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// communauté Lucette Piguet - Laurent Piguet - Evelyne Vultaggio
		final CommunauteRF communaute2 = new CommunauteRF();
		final PersonnePhysiqueRF laurent = new PersonnePhysiqueRF();
		laurent.setId(3L);
		final PersonnePhysiqueRF evelyne = new PersonnePhysiqueRF();
		evelyne.setId(4L);

		// droit modifié de Lucette Piguet <--- la raison d'acquisition ne change pas !
		final DroitProprietePersonnePhysiqueRF nouveau2 = new DroitProprietePersonnePhysiqueRF();
		nouveau2.setMasterIdRF("382818811");
		nouveau2.setVersionIdRF("2");
		nouveau2.setPart(new Fraction(1, 1));
		nouveau2.setAyantDroit(lucette);
		nouveau2.setCommunaute(communaute2);
		nouveau2.addRaisonAcquisition(new RaisonAcquisitionRF(dateAchat, "Achat", null));

		// nouveau droit de Laurent Piguet (avec une date de succession légèrement plus tardive)
		final DroitProprietePersonnePhysiqueRF nouveau3 = new DroitProprietePersonnePhysiqueRF();
		nouveau3.setMasterIdRF("777433");
		nouveau3.setVersionIdRF("1");
		nouveau3.setPart(new Fraction(1, 1));
		nouveau3.setAyantDroit(laurent);
		nouveau3.setCommunaute(communaute2);
		nouveau3.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession2, "Succession", null));

		// nouveau droit de Evelyne Vultaggio
		final DroitProprietePersonnePhysiqueRF nouveau4 = new DroitProprietePersonnePhysiqueRF();
		nouveau4.setMasterIdRF("91919191");
		nouveau4.setVersionIdRF("1");
		nouveau4.setPart(new Fraction(1, 1));
		nouveau4.setAyantDroit(evelyne);
		nouveau4.setCommunaute(communaute2);
		nouveau4.addRaisonAcquisition(new RaisonAcquisitionRF(dateSuccession1, "Succession", null));

		final AffaireRF affaire = new AffaireRF(dateSuccession1, immeuble, Arrays.asList(nouveau2, nouveau3, nouveau4), Collections.emptyList(), Arrays.asList(precedent1, precedent2));
		affaire.refreshDatesDebutMetier(null);
		assertNull(precedent1.getDateDebutMetier());    // pas de changement sur les droits précédents
		assertNull(precedent2.getDateDebutMetier());
		assertEquals(dateSuccession1, nouveau2.getDateDebutMetier());   // <-- la raison d'acquisition "succession" la plus ancienne doit être utilisée
		assertEquals(dateSuccession2, nouveau3.getDateDebutMetier());
		assertEquals(dateSuccession1, nouveau4.getDateDebutMetier());
		assertEquals("Succession", nouveau2.getMotifDebut());
		assertEquals("Succession", nouveau3.getMotifDebut());
		assertEquals("Succession", nouveau4.getMotifDebut());
	}

	private static class Listener implements AffaireRFListener {

		private static class Update {

			private final DroitProprieteRF droit;
			private final RegDate dateDebutMetierInitiale;
			private final String motifDebutInitial;

			public Update(DroitProprieteRF droit, RegDate dateDebutMetierInitiale, String motifDebutInitial) {
				this.dateDebutMetierInitiale = dateDebutMetierInitiale;
				this.motifDebutInitial = motifDebutInitial;
				this.droit = droit;
			}

			public DroitProprieteRF getDroit() {
				return droit;
			}

			public RegDate getDateDebutMetierInitiale() {
				return dateDebutMetierInitiale;
			}

			public String getMotifDebutInitial() {
				return motifDebutInitial;
			}
		}

		private final List<DroitProprieteRF> untouched = new ArrayList<>();
		private final List<DroitProprieteRF> created = new ArrayList<>();
		private final List<Update> updated = new ArrayList<>();

		@Override
		public void addUntouched(@NotNull DroitProprieteRF droit) {
			untouched.add(droit);
		}

		@Override
		public void addCreated(DroitProprieteRF droit) {
			created.add(droit);
		}

		@Override
		public void addUpdated(@NotNull DroitProprieteRF droit, @Nullable RegDate dateDebutMetierPrecedente, @Nullable String motifDebutPrecedent) {
			updated.add(new Update(droit, dateDebutMetierPrecedente, motifDebutPrecedent));
		}

		public List<DroitProprieteRF> getUntouched() {
			return untouched;
		}

		public List<DroitProprieteRF> getCreated() {
			return created;
		}

		public List<Update> getUpdated() {
			return updated;
		}
	}

	private static void assertUpdated(DroitProprieteRF droit, RegDate dateDebutInitiale, String motifDebutInitial, Listener.Update update) {
		assertNotNull(update);
		assertSame(droit, update.getDroit());
		assertEquals(dateDebutInitiale, update.getDateDebutMetierInitiale());
		assertEquals(motifDebutInitial, update.getMotifDebutInitial());
	}
}