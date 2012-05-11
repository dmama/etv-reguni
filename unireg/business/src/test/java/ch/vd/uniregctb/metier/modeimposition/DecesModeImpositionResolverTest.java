package ch.vd.uniregctb.metier.modeimposition;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * La spécification dit : le mode d'imposition du ou des contribuables imposés séparément est déterminé selon le tableau suivant :
 * <table>
 *      <tr><td><b>Mode original</b></td><td><b>Mode résultant</b></td></tr>
 *      <tr><td>Ordinaire</td><td>Reste à 'Ordinaire' s'il est suisse ou titulaire d'un permis d'établissement.<br/>Passe à 'Mixte 137 al. 1' dans le cas contraire.</td></tr>
 *      <tr><td>Dépense</td><td>Dépense</td></tr>
 *      <tr><td>Indigent</td><td>Indigent</td></tr>
 *      <tr><td>Mixte 137 al. 1</td><td>Mixte 137 al. 1</td></tr>
 *      <tr><td>Mixte 137 al. 2</td><td>Mixte 137 al. 2</td></tr>
 *      <tr><td>Source</td><td>Source</td></tr>
 * </table>
 */
public class DecesModeImpositionResolverTest extends BusinessTest {

	private DecesModeImpositionResolver resolver;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		final TiersService tiersService = getBean(TiersService.class, "tiersService");
		resolver = new DecesModeImpositionResolver(tiersService, null);
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testResolveOrdinaireResteOrdinaireNationaliteSuisse() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_HC);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.PAYS_HS);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testResolveOrdinaireResteOrdinairePermisC() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);

		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_HC);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.PAYS_HS);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testResolveOrdinairePasseMixte1() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);

		// passe mixte-1 dans le canton...
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.MIXTE_137_1, res.getModeImposition());
		}
		// ... mais source hors-canton ...
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.COMMUNE_HC);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.SOURCE, res.getModeImposition());
		}
		// .. ou hors-Suisse
		{
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE, TypeAutoriteFiscale.PAYS_HS);
			Assert.assertNotNull(res);
			Assert.assertEquals(date, res.getDateDebut());
			Assert.assertEquals(ModeImposition.SOURCE, res.getModeImposition());
		}
	}

	private static boolean isMixte(ModeImposition mode) {
		return mode == ModeImposition.MIXTE_137_1 || mode == ModeImposition.MIXTE_137_2;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testResolvePermisBNonOrdinaire() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);

		final ModeImposition[] modes = { ModeImposition.DEPENSE, ModeImposition.INDIGENT, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2, ModeImposition.SOURCE };
		for (ModeImposition mode : modes) {
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, mode, res.getModeImposition());
			}
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.COMMUNE_HC);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, isMixte(mode) ? ModeImposition.SOURCE : mode, res.getModeImposition());
			}
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.PAYS_HS);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, isMixte(mode) ? ModeImposition.SOURCE : mode, res.getModeImposition());
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testResolveNeChangePasSuisse() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

		final ModeImposition[] modes = { ModeImposition.DEPENSE, ModeImposition.INDIGENT, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2, ModeImposition.SOURCE };
		for (ModeImposition mode : modes) {
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, mode, res.getModeImposition());
			}
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.COMMUNE_HC);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, isMixte(mode) ? ModeImposition.SOURCE : mode, res.getModeImposition());
			}
			{
				final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode, TypeAutoriteFiscale.PAYS_HS);
				Assert.assertNotNull("Mode " + mode, res);
				Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
				Assert.assertEquals("Mode " + mode, isMixte(mode) ? ModeImposition.SOURCE : mode, res.getModeImposition());
			}
		}
	}
}
