package ch.vd.unireg.registrefoncier.dao;

import org.junit.Test;

import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.registrefoncier.BienFondsRF;
import ch.vd.unireg.registrefoncier.CommuneRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ImmeubleRFDAOTest extends CoreDAOTest {

	private ImmeubleRFDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		this.dao = getBean(ImmeubleRFDAO.class, "immeubleRFDAO");
	}

	@Test
	public void testGetBySituation() throws Exception {

		class Ids {
			Long imm1;
			Long imm2;
			Long imm3;
			Long imm4;
			Long imm5;
		}
		final Ids ids = new Ids();

		doInNewTransaction(status -> {

			final CommuneRF lausanne = addCommuneRF("Lausanne", 1, 5586);

			final BienFondsRF imm1 = addImmeubleRF("1", lausanne, 2345, null, null, null);
			final BienFondsRF imm2 = addImmeubleRF("2", lausanne, 2345, 1, null, null);
			final BienFondsRF imm3 = addImmeubleRF("3", lausanne, 2345, 2, null, null);
			final BienFondsRF imm4 = addImmeubleRF("4", lausanne, 2345, 2, 3, null);
			final BienFondsRF imm5 = addImmeubleRF("5", lausanne, 2345, 2, 3, 4);

			ids.imm1 = imm1.getId();
			ids.imm2 = imm2.getId();
			ids.imm3 = imm3.getId();
			ids.imm4 = imm4.getId();
			ids.imm5 = imm5.getId();
			return null;
		});

		doInNewTransaction(status -> {

			// numéro de parcelle différent
			assertNull(dao.getBySituation(5586, 9999, null, null, null));
			assertNull(dao.getBySituation(5586, 9999, 1, null, null));
			assertNull(dao.getBySituation(5586, 9999, 1, 2, null));
			assertNull(dao.getBySituation(5586, 9999, 1, 2, 3));

			// numéro de parcelle correct, sans index
			{
				final ImmeubleRF immeuble = dao.getBySituation(5586, 2345, null, null, null);
				assertNotNull(immeuble);
				assertEquals(ids.imm1, immeuble.getId());
			}

			// numéro de parcelle correct, avec index1 de renseigné
			{
				final ImmeubleRF immeuble = dao.getBySituation(5586, 2345, 1, null, null);
				assertNotNull(immeuble);
				assertEquals(ids.imm2, immeuble.getId());
			}
			{
				final ImmeubleRF immeuble = dao.getBySituation(5586, 2345, 2, null, null);
				assertNotNull(immeuble);
				assertEquals(ids.imm3, immeuble.getId());
			}

			// numéro de parcelle correct, avec index1 et index2 de renseignés
			{
				final ImmeubleRF immeuble = dao.getBySituation(5586, 2345, 2, 3, null);
				assertNotNull(immeuble);
				assertEquals(ids.imm4, immeuble.getId());
			}

			// numéro de parcelle correct, avec index1, index2 et index3 de renseignés
			{
				ImmeubleRF immeuble = dao.getBySituation(5586, 2345, 2, 3, 4);
				assertNotNull(immeuble);
				assertEquals(ids.imm5, immeuble.getId());
			}
			return null;
		});
	}
}