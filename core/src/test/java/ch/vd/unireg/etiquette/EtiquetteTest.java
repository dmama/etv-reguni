package ch.vd.unireg.etiquette;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.type.TypeTiersEtiquette;

public class EtiquetteTest extends CoreDAOTest {

	private static Decalage buildDecalage(int decalage, UniteDecalageDate uniteDecalage) {
		return new Decalage(decalage, uniteDecalage);
	}

	private static DecalageAvecCorrection buildDecalage(int decalage, UniteDecalageDate uniteDecalage, CorrectionSurDate correction) {
		return new DecalageAvecCorrection(decalage, uniteDecalage, correction);
	}

	private static ActionAutoEtiquette buildActionAuto(Function<RegDate, RegDate> dateDebut, Function<RegDate, RegDate> dateFin) {
		return new ActionAutoEtiquette(dateDebut, dateFin);
	}

	@Test
	public void testPersistenceEtRecovery() throws Exception {
		// création d'une étiquette
		final Long id = doInNewTransaction(status -> {
			final Etiquette etiquette = new Etiquette("TEST", "Test étiquette", true, TypeTiersEtiquette.PP_MC, null);
			etiquette.setActionSurDeces(buildActionAuto(buildDecalage(1, UniteDecalageDate.JOUR),
			                                            buildDecalage(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_ANNEE)));
			return hibernateTemplate.merge(etiquette).getId();
		});

		// relecture depuis la base
		doInNewReadOnlyTransaction(status -> {
			final Etiquette etiquette = hibernateTemplate.get(Etiquette.class, id);
			Assert.assertNotNull(etiquette);
			Assert.assertEquals("TEST", etiquette.getCode());
			Assert.assertEquals("Test étiquette", etiquette.getLibelle());
			Assert.assertEquals(true, etiquette.isActive());
			Assert.assertEquals(TypeTiersEtiquette.PP_MC, etiquette.getTypeTiers());

			final ActionAutoEtiquette actionDeces = etiquette.getActionSurDeces();
			Assert.assertNotNull(actionDeces);
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateDebut();
				Assert.assertNotNull(fn);
				Assert.assertEquals(Decalage.class, fn.getClass());

				final Decalage d = (Decalage) fn;
				Assert.assertEquals(1, d.getDecalage());
				Assert.assertEquals(UniteDecalageDate.JOUR, d.getUniteDecalage());
			}
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateFin();
				Assert.assertNotNull(fn);
				Assert.assertEquals(DecalageAvecCorrection.class, fn.getClass());

				final DecalageAvecCorrection dac = (DecalageAvecCorrection) fn;
				Assert.assertEquals(2, dac.getDecalage());
				Assert.assertEquals(UniteDecalageDate.ANNEE, dac.getUniteDecalage());
				Assert.assertEquals(CorrectionSurDate.FIN_ANNEE, dac.getCorrection());
			}
			return null;
		});
	}

	@Test
	public void testPersistenceEtRecoverySansAutoDeces() throws Exception {
		// création d'une étiquette
		final Long id = doInNewTransaction(status -> {
			final Etiquette etiquette = new Etiquette("TESTo", "Testo étiquette", true, TypeTiersEtiquette.PP_MC_PM, null);
			return hibernateTemplate.merge(etiquette).getId();
		});

		// relecture depuis la base
		doInNewReadOnlyTransaction(status -> {
			final Etiquette etiquette = hibernateTemplate.get(Etiquette.class, id);
			Assert.assertNotNull(etiquette);
			Assert.assertEquals("TESTo", etiquette.getCode());
			Assert.assertEquals("Testo étiquette", etiquette.getLibelle());
			Assert.assertEquals(true, etiquette.isActive());
			Assert.assertEquals(TypeTiersEtiquette.PP_MC_PM, etiquette.getTypeTiers());
			Assert.assertNull(etiquette.getActionSurDeces());
			return null;
		});
	}

	@Test
	public void testPersistenceEtRecoveryDecesSeulementDateDebut() throws Exception {
		// création d'une étiquette
		final Long id = doInNewTransaction(status -> {
			final Etiquette etiquette = new Etiquette("TESTA", "Testa étiquette", true, TypeTiersEtiquette.PP, null);
			etiquette.setActionSurDeces(buildActionAuto(buildDecalage(-1, UniteDecalageDate.SEMAINE, CorrectionSurDate.DEBUT_ANNEE), null));
			return hibernateTemplate.merge(etiquette).getId();
		});

		// relecture depuis la base
		doInNewReadOnlyTransaction(status -> {
			final Etiquette etiquette = hibernateTemplate.get(Etiquette.class, id);
			Assert.assertNotNull(etiquette);
			Assert.assertEquals("TESTA", etiquette.getCode());
			Assert.assertEquals("Testa étiquette", etiquette.getLibelle());
			Assert.assertEquals(true, etiquette.isActive());
			Assert.assertEquals(TypeTiersEtiquette.PP, etiquette.getTypeTiers());

			final ActionAutoEtiquette actionDeces = etiquette.getActionSurDeces();
			Assert.assertNotNull(actionDeces);
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateDebut();
				Assert.assertNotNull(fn);
				Assert.assertEquals(DecalageAvecCorrection.class, fn.getClass());

				final DecalageAvecCorrection dac = (DecalageAvecCorrection) fn;
				Assert.assertEquals(-1, dac.getDecalage());
				Assert.assertEquals(UniteDecalageDate.SEMAINE, dac.getUniteDecalage());
				Assert.assertEquals(CorrectionSurDate.DEBUT_ANNEE, dac.getCorrection());
			}
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateFin();
				Assert.assertNull(fn);
			}
			return null;
		});
	}

	@Test
	public void testPersistenceEtRecoveryDecesSeulementDateFin() throws Exception {
		// création d'une étiquette
		final Long id = doInNewTransaction(status -> {
			final Etiquette etiquette = new Etiquette("TESTI", "Testi étiquette", false, TypeTiersEtiquette.PP, null);
			etiquette.setActionSurDeces(buildActionAuto(null, buildDecalage(0, UniteDecalageDate.JOUR, CorrectionSurDate.FIN_MOIS)));
			return hibernateTemplate.merge(etiquette).getId();
		});

		// relecture depuis la base
		doInNewReadOnlyTransaction(status -> {
			final Etiquette etiquette = hibernateTemplate.get(Etiquette.class, id);
			Assert.assertNotNull(etiquette);
			Assert.assertEquals("TESTI", etiquette.getCode());
			Assert.assertEquals("Testi étiquette", etiquette.getLibelle());
			Assert.assertEquals(false, etiquette.isActive());
			Assert.assertEquals(TypeTiersEtiquette.PP, etiquette.getTypeTiers());

			final ActionAutoEtiquette actionDeces = etiquette.getActionSurDeces();
			Assert.assertNotNull(actionDeces);
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateDebut();
				Assert.assertNull(fn);
			}
			{
				final Function<RegDate, RegDate> fn = actionDeces.getDateFin();
				Assert.assertNotNull(fn);
				Assert.assertEquals(DecalageAvecCorrection.class, fn.getClass());

				final DecalageAvecCorrection dac = (DecalageAvecCorrection) fn;
				Assert.assertEquals(0, dac.getDecalage());
				Assert.assertEquals(UniteDecalageDate.JOUR, dac.getUniteDecalage());
				Assert.assertEquals(CorrectionSurDate.FIN_MOIS, dac.getCorrection());
			}
			return null;
		});
	}
}
