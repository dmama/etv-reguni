package ch.vd.uniregctb.metier.modeimposition;

import junit.framework.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.Sexe;

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
	public void testResolveOrdinaireResteOrdinaireNationaliteSuisse() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

		final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE);
		Assert.assertNotNull(res);
		Assert.assertEquals(date, res.getDateDebut());
		Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
	}

	@Test
	public void testResolveOrdinaireResteOrdinairePermisC() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._03_ETABLI_C);

		final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE);
		Assert.assertNotNull(res);
		Assert.assertEquals(date, res.getDateDebut());
		Assert.assertEquals(ModeImposition.ORDINAIRE, res.getModeImposition());
	}

	@Test
	public void testResolveOrdinairePasseMixte1() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);

		final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, ModeImposition.ORDINAIRE);
		Assert.assertNotNull(res);
		Assert.assertEquals(date, res.getDateDebut());
		Assert.assertEquals(ModeImposition.MIXTE_137_1, res.getModeImposition());
	}

	@Test
	public void testResolveNeChangePasEtranger() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.France.getNoOFS());
		pp.setCategorieEtranger(CategorieEtranger._02_PERMIS_SEJOUR_B);

		final ModeImposition[] modes = { ModeImposition.DEPENSE, ModeImposition.INDIGENT, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2, ModeImposition.SOURCE };
		for (ModeImposition mode : modes) {
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode);
			Assert.assertNotNull("Mode " + mode, res);
			Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
			Assert.assertEquals("Mode " + mode, mode, res.getModeImposition());
		}
	}

	@Test
	public void testResolveNeChangePasSuisse() throws Exception {

		final RegDate date = RegDate.get();

		final PersonnePhysique pp = addNonHabitant("Achille", "Talon", date(1956, 3, 5), Sexe.MASCULIN);
		pp.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());

		final ModeImposition[] modes = { ModeImposition.DEPENSE, ModeImposition.INDIGENT, ModeImposition.MIXTE_137_1, ModeImposition.MIXTE_137_2, ModeImposition.SOURCE };
		for (ModeImposition mode : modes) {
			final ModeImpositionResolver.Imposition res = resolver.resolve(pp, date, mode);
			Assert.assertNotNull("Mode " + mode, res);
			Assert.assertEquals("Mode " + mode, date, res.getDateDebut());
			Assert.assertEquals("Mode " + mode, mode, res.getModeImposition());
		}
	}
}
