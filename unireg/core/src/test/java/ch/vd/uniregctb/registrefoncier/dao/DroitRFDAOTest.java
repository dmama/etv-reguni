package ch.vd.uniregctb.registrefoncier.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.rf.GenrePropriete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DroitRFDAOTest extends CoreDAOTest {

	private DroitRFDAO droitRFDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.droitRFDAO = getBean(DroitRFDAO.class, "droitRFDAO");
	}

	@Test
	public void testFindForAyantDroit() throws Exception {

		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		class Ids {
			Long bernard;
			Long arnold;
			Long evelyne;
			Long immeuble1;
			Long immeuble2;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			// on crée quelques tiers RF
			final PersonnePhysiqueRF bernard = addPersonnePhysiqueRF("Bernanrd", "Cinoud", RegDate.get(1933, 4, 2), "4848834", 1L, null);
			final PersonnePhysiqueRF arnold = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), "3783737", 1233L, null);
			final PersonnePhysiqueRF evelyne = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), "472382", 9239L, null);
			ids.bernard = bernard.getId();
			ids.arnold = arnold.getId();
			ids.evelyne = evelyne.getId();

			// on crée quelques immeubles qui appartiennent tous à Bernard
			final BienFondRF immeuble1 = addImmeubleRF("382929efa218");
			ids.immeuble1 = immeuble1.getId();

			final BienFondRF immeuble2 = addImmeubleRF("14524242172");
			ids.immeuble2 = immeuble2.getId();

			final IdentifiantAffaireRF affaireAchat = new IdentifiantAffaireRF(123, 1995, 23, 3);
			addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "47840038", affaireAchat,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, bernard, immeuble1, null);
			addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "84893923", affaireAchat,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, bernard, immeuble2, null);

			// on crée un usufruit en faveur d'Arnold et Evelyne sur les deux immeubles de Bernard
			final IdentifiantAffaireRF affaireUsufruit = new IdentifiantAffaireRF(123, 1995, 23, 4);
			addUsufruitRF(dateAchat, dateAchat, null, null, "Cadeau", null, "1717171", affaireUsufruit,
			              new IdentifiantDroitRF(2, 1995, 2), Arrays.asList(arnold, evelyne), Arrays.asList(immeuble1, immeuble2));
			return null;
		});

		// on demande les droits de Bernard
		doInNewTransaction(status -> {
			final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ids.bernard, true);
			assertNotNull(droits);
			assertEquals(2, droits.size());
			droits.sort(new DroitRFRangeMetierComparator());

			final DroitProprieteRF droit0 = (DroitProprieteRF) droits.get(0);
			assertNotNull(droit0);
			assertEquals(ids.bernard, droit0.getAyantDroit().getId());
			assertEquals(ids.immeuble1, droit0.getImmeuble().getId());

			final DroitProprieteRF droit1 = (DroitProprieteRF) droits.get(1);
			assertNotNull(droit1);
			assertEquals(ids.bernard, droit1.getAyantDroit().getId());
			assertEquals(ids.immeuble2, droit1.getImmeuble().getId());
			return null;
		});

		// on demande les droits d'Arnold
		doInNewTransaction(status -> {
			final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ids.arnold, false);
			assertNotNull(droits);
			assertEquals(1, droits.size());

			final UsufruitRF usufruit0 = (UsufruitRF) droits.get(0);
			assertNotNull(usufruit0);

			final List<AyantDroitRF> ayantDroits = new ArrayList<>(usufruit0.getAyantDroits());
			assertEquals(2, ayantDroits.size());
			ayantDroits.sort(Comparator.comparing(AyantDroitRF::getId));
			assertEquals(ids.arnold, ayantDroits.get(0).getId());
			assertEquals(ids.evelyne, ayantDroits.get(1).getId());

			final List<ImmeubleRF> immeubles = new ArrayList<>(usufruit0.getImmeubles());
			assertEquals(2, immeubles.size());
			immeubles.sort(Comparator.comparing(ImmeubleRF::getId));
			assertEquals(ids.immeuble1, immeubles.get(0).getId());
			assertEquals(ids.immeuble2, immeubles.get(1).getId());
			return null;
		});

		// on demande les droits d'Evelyne
		doInNewTransaction(status -> {
			final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ids.evelyne, true);
			assertNotNull(droits);
			assertEquals(1, droits.size());

			final UsufruitRF usufruit0 = (UsufruitRF) droits.get(0);
			assertNotNull(usufruit0);

			final List<AyantDroitRF> ayantDroits = new ArrayList<>(usufruit0.getAyantDroits());
			assertEquals(2, ayantDroits.size());
			ayantDroits.sort(Comparator.comparing(AyantDroitRF::getId));
			assertEquals(ids.arnold, ayantDroits.get(0).getId());
			assertEquals(ids.evelyne, ayantDroits.get(1).getId());

			final List<ImmeubleRF> immeubles = new ArrayList<>(usufruit0.getImmeubles());
			assertEquals(2, immeubles.size());
			immeubles.sort(Comparator.comparing(ImmeubleRF::getId));
			assertEquals(ids.immeuble1, immeubles.get(0).getId());
			assertEquals(ids.immeuble2, immeubles.get(1).getId());
			return null;
		});

	}
}