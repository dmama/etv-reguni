package ch.vd.unireg.registrefoncier.dao;

import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.TypeDroit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AyantDroitRFDAOTest extends CoreDAOTest {

	private AyantDroitRFDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.dao = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
	}

	/**
	 * Ce test vérifie que la méthode retourne bien les droits actifs d'un certain type, y compris les sous-classes.
	 */
	@Test
	public void testFindAvecDroitsActifs() throws Exception {

		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		final String idRfArnold = "3783737";
		final String idRfEvelyne = "472382";

		doInNewTransaction(status -> {

			// on crée les tiers RF
			final PersonnePhysiqueRF arnoldRf = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), idRfArnold, 1233L, null);
			final PersonnePhysiqueRF evelyneRf = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), idRfEvelyne, 9239L, null);

			// on crée l'immeuble
			final BienFondsRF immeuble = addImmeubleRF("382929efa218");

			// on crée un droit de propriété pour Arnold
			final IdentifiantAffaireRF affaireAchat = new IdentifiantAffaireRF(123, 1995, 23, 3);
			addDroitPersonnePhysiqueRF(dateAchat, dateAchat, null, null, "Achat", null, "47840038",
			                           "47840037", affaireAchat, new Fraction(1, 3), GenrePropriete.COMMUNE, arnoldRf, immeuble, null);

			// on crée un usufruit pour Evelnye
			final IdentifiantAffaireRF affaireUsfruit = new IdentifiantAffaireRF(7, 2000, 2, null);
			addUsufruitRF(RegDate.get(2000, 1, 1), RegDate.get(2000, 1, 1), null, null, "Achat", null, "34898934",
			              "34898933", affaireUsfruit, new IdentifiantDroitRF(7, 2000, 121), evelyneRf, immeuble);
			return null;
		});

		// on demande les droits de propriété
		doInNewTransaction(status -> {
			final Set<String> set = dao.findAvecDroitsActifs(TypeDroit.DROIT_PROPRIETE);
			assertNotNull(set);
			assertEquals(1, set.size());
			assertEquals(idRfArnold, set.iterator().next());
			return null;
		});

		// on demande les droits de usufruits
		doInNewTransaction(status -> {
			final Set<String> set = dao.findAvecDroitsActifs(TypeDroit.SERVITUDE);
			assertNotNull(set);
			assertEquals(1, set.size());
			assertEquals(idRfEvelyne, set.iterator().next());
			return null;
		});
	}
}