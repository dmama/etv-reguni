package ch.vd.unireg.registrefoncier.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.DroitRFRangeMetierComparator;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;

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
			final BienFondsRF immeuble1 = addImmeubleRF("382929efa218");
			ids.immeuble1 = immeuble1.getId();

			final BienFondsRF immeuble2 = addImmeubleRF("14524242172");
			ids.immeuble2 = immeuble2.getId();

			final IdentifiantAffaireRF affaireAchat = new IdentifiantAffaireRF(123, 1995, 23, 3);
			addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "47840038", "47840037", affaireAchat,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, bernard, immeuble1, null);
			addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "84893923", "84893922", affaireAchat,
			                           new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, bernard, immeuble2, null);

			// on crée un usufruit en faveur d'Arnold et Evelyne sur les deux immeubles de Bernard
			final IdentifiantAffaireRF affaireUsufruit = new IdentifiantAffaireRF(123, 1995, 23, 4);
			addUsufruitRF(dateAchat, dateAchat, null, null, "Cadeau", null, "1717171", "1717170", affaireUsufruit,
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

			final List<BeneficeServitudeRF> benefices = new ArrayList<>(usufruit0.getBenefices());
			assertEquals(2, benefices.size());
			benefices.sort(Comparator.comparing(bene -> bene.getAyantDroit().getId()));
			assertEquals(ids.arnold, benefices.get(0).getAyantDroit().getId());
			assertEquals(ids.evelyne, benefices.get(1).getAyantDroit().getId());

			final List<ChargeServitudeRF> charges = new ArrayList<>(usufruit0.getCharges());
			assertEquals(2, charges.size());
			charges.sort(Comparator.comparing(charge -> charge.getImmeuble().getId()));
			assertEquals(ids.immeuble1, charges.get(0).getImmeuble().getId());
			assertEquals(ids.immeuble2, charges.get(1).getImmeuble().getId());
			return null;
		});

		// on demande les droits d'Evelyne
		doInNewTransaction(status -> {
			final List<DroitRF> droits = droitRFDAO.findForAyantDroit(ids.evelyne, true);
			assertNotNull(droits);
			assertEquals(1, droits.size());

			final UsufruitRF usufruit0 = (UsufruitRF) droits.get(0);
			assertNotNull(usufruit0);

			final List<BeneficeServitudeRF> benefices = new ArrayList<>(usufruit0.getBenefices());
			assertEquals(2, benefices.size());
			benefices.sort(Comparator.comparing(bene -> bene.getAyantDroit().getId()));
			assertEquals(ids.arnold, benefices.get(0).getAyantDroit().getId());
			assertEquals(ids.evelyne, benefices.get(1).getAyantDroit().getId());

			final List<ChargeServitudeRF> charges = new ArrayList<>(usufruit0.getCharges());
			assertEquals(2, charges.size());
			charges.sort(Comparator.comparing(charge -> charge.getImmeuble().getId()));
			assertEquals(ids.immeuble1, charges.get(0).getImmeuble().getId());
			assertEquals(ids.immeuble2, charges.get(1).getImmeuble().getId());
			return null;
		});

	}
}