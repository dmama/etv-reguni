package ch.vd.uniregctb.registrefoncier.dao;

import java.util.Collection;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.registrefoncier.BienFondRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRFInfo;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.rf.GenrePropriete;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeRapprochementRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AyantDroitRFDAOTest extends CoreDAOTest {

	private AyantDroitRFDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.dao = getBean(AyantDroitRFDAO.class, "ayantDroitRFDAO");
	}

	@Test
	public void testGetCommunauteInfoCommunauteInconnue() throws Exception {
		final CommunauteRFInfo info = doInNewTransaction(status -> dao.getCommunauteInfo(666));
		assertNull(info);
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que les informations de la communauté sont bien retournées quand tous les membres de la communautés sont rapprochés (cas passant).
	 */
	@Test
	public void testGetCommunauteInfoCommunauteAvecTousLesTiersRapproches() throws Exception {

		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		class Ids {
			long arnoldRf;
			long evelyneRf;
			long communauteRf;
			long arnoldUnireg;
			long evelyneUnireg;
			long immeuble;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			// on crée les tiers RF
			final PersonnePhysiqueRF arnoldRf = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), "3783737", 1233L, null);
			final PersonnePhysiqueRF evelyneRf = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), "472382", 9239L, null);
			final CommunauteRF communauteRF = addCommunauteRF("4783882", TypeCommunaute.COMMUNAUTE_DE_BIENS);
			ids.communauteRf = communauteRF.getId();
			ids.arnoldRf = arnoldRf.getId();
			ids.evelyneRf = evelyneRf.getId();

			// on crée les tiers Unireg
			final PersonnePhysique arnoldUnireg = addNonHabitant("Arnold", "Totore", RegDate.get(1950, 4, 2), Sexe.MASCULIN);
			final PersonnePhysique evelyneUnireg = addNonHabitant("Evelyne", "Fondu", RegDate.get(1944, 12, 12), Sexe.FEMININ);
			ids.arnoldUnireg = arnoldUnireg.getId();
			ids.evelyneUnireg = evelyneUnireg.getId();

			// on crée les rapprochements qui vont bien
			addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, arnoldUnireg, arnoldRf, false);
			addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, evelyneUnireg, evelyneRf, false);

			// on crée l'immeuble et les droits associés
			final BienFondRF immeuble = addImmeubleRF("382929efa218");
			ids.immeuble = immeuble.getId();

			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 1995, 23, 3);
			addDroitCommunauteRF(dateAchat, null, "Achat", null, "48234923829", numeroAffaire, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, communauteRF, immeuble);
			addDroitPersonnePhysiqueRF(dateAchat, null, "Achat", null, "47840038", numeroAffaire, new Fraction(1, 2), GenrePropriete.COMMUNE, arnoldRf, immeuble, communauteRF);
			addDroitPersonnePhysiqueRF(dateAchat, null, "Achat", null, "84893923", numeroAffaire, new Fraction(1, 2), GenrePropriete.COMMUNE, evelyneRf, immeuble, communauteRF);

			return null;
		});

		final CommunauteRFInfo info = doInNewTransaction(status -> dao.getCommunauteInfo(ids.communauteRf));
		assertNotNull(info);
		assertEquals(2, info.getMemberCount());

		final Collection<Integer> memberIds = info.getMemberIds();
		assertNotNull(memberIds);
		assertTrue(memberIds.contains((int) ids.arnoldUnireg));
		assertTrue(memberIds.contains((int) ids.evelyneUnireg));
	}

	/**
	 * [SIFISC-20373] Ce test vérifie que le nombre de membres dans la communauté est bien renseigné, même si tous les tiers de la communauté ne sont pas rapprochés.
	 */
	@Test
	public void testGetCommunauteInfoCommunauteAvecCertainsTiersRapproches() throws Exception {

		final RegDate dateAchat = RegDate.get(1995, 3, 24);

		class Ids {
			long arnoldRf;
			long evelyneRf;
			long communauteRf;
			long arnoldUnireg;
			long evelyneUnireg;
			long immeuble;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			// on crée les tiers RF
			final PersonnePhysiqueRF arnoldRf = addPersonnePhysiqueRF("Arnold", "Totore", RegDate.get(1950, 4, 2), "3783737", 1233L, null);
			final PersonnePhysiqueRF evelyneRf = addPersonnePhysiqueRF("Evelyne", "Fondu", RegDate.get(1944, 12, 12), "472382", 9239L, null);
			final PersonnePhysiqueRF totrRf = addPersonnePhysiqueRF("Totor", "Fantomas", RegDate.get(1923, 6, 6), "3437", 28920L, null);
			final CommunauteRF communauteRF = addCommunauteRF("4783882", TypeCommunaute.COMMUNAUTE_DE_BIENS);
			ids.communauteRf = communauteRF.getId();
			ids.arnoldRf = arnoldRf.getId();
			ids.evelyneRf = evelyneRf.getId();

			// on crée les tiers Unireg
			final PersonnePhysique arnoldUnireg = addNonHabitant("Arnold", "Totore", RegDate.get(1950, 4, 2), Sexe.MASCULIN);
			final PersonnePhysique evelyneUnireg = addNonHabitant("Evelyne", "Fondu", RegDate.get(1944, 12, 12), Sexe.FEMININ);
			ids.arnoldUnireg = arnoldUnireg.getId();
			ids.evelyneUnireg = evelyneUnireg.getId();

			// on crée les rapprochements qui vont bien (attention, totor n'est pas rapproché)
			addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, arnoldUnireg, arnoldRf, false);
			addRapprochementRF(null, null, TypeRapprochementRF.MANUEL, evelyneUnireg, evelyneRf, false);

			// on crée l'immeuble et les droits associés
			final BienFondRF immeuble = addImmeubleRF("382929efa218");
			ids.immeuble = immeuble.getId();

			final IdentifiantAffaireRF numeroAffaire = new IdentifiantAffaireRF(123, 1995, 23, 3);
			addDroitCommunauteRF(dateAchat, null, "Achat", null, "48234923829", numeroAffaire, new Fraction(1, 1), GenrePropriete.INDIVIDUELLE, communauteRF, immeuble);
			addDroitPersonnePhysiqueRF(dateAchat, null, "Achat", null, "47840038", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, arnoldRf, immeuble, communauteRF);
			addDroitPersonnePhysiqueRF(dateAchat, null, "Achat", null, "84893923", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, evelyneRf, immeuble, communauteRF);
			addDroitPersonnePhysiqueRF(dateAchat, null, "Achat", null, "3403892", numeroAffaire, new Fraction(1, 3), GenrePropriete.COMMUNE, totrRf, immeuble, communauteRF);

			return null;
		});

		final CommunauteRFInfo info = doInNewTransaction(status -> dao.getCommunauteInfo(ids.communauteRf));
		assertNotNull(info);
		assertEquals(3, info.getMemberCount());

		final Collection<Integer> memberIds = info.getMemberIds();
		assertNotNull(memberIds);
		assertTrue(memberIds.contains((int) ids.arnoldUnireg));
		assertTrue(memberIds.contains((int) ids.evelyneUnireg));
	}

}