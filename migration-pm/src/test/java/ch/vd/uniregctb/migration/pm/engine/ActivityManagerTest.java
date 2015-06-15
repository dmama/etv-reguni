package ch.vd.uniregctb.migration.pm.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;

public class ActivityManagerTest {

	/**
	 * Construit une instance de {@link ActivityManager} pour lequel les identifiants founis par la perception sont donnés
	 * @param seuil seuil d'activité
	 * @param idsActifsPerception identifiants de personnes morales (contribuables) que la perception considère comme actifs
	 * @return une instance de {@link ActivityManager}
	 * @throws IOException en cas de souci à la création de l'instance...
	 */
	private static ActivityManager buildInstance(RegDate seuil, long... idsActifsPerception) throws IOException {
		final InputStream is;
		if (idsActifsPerception != null && idsActifsPerception.length > 0) {
			final StringBuilder b = new StringBuilder();
			for (long id : idsActifsPerception) {
				b.append(id).append(System.lineSeparator());
			}
			is = new ByteArrayInputStream(b.toString().getBytes());     // ces streams-là n'ont pas besoin d'un appel à "close"...
		}
		else {
			is = null;
		}
		return new ActivityManagerImpl(is, seuil);
	}

	@Test
	public void testActiviteAssujettissementSansAssujettissement() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2000, 1, 1));
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(54252L);
		Assert.assertFalse(mgr.isActive(e));
	}

	@Test
	public void testActiviteAssujettissementAvecAssujettissementAvantDateSeuil() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2000, 1, 1));
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(142324L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(1995, 1, 1), RegDate.get(1999, 12, 31), RegpmTypeAssujettissement.LILIC);
			Assert.assertFalse(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(3422342L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(1995, 1, 1), RegDate.get(1999, 12, 31), RegpmTypeAssujettissement.LILIC);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.SANS);
			Assert.assertFalse(mgr.isActive(e));
		}
	}

	@Test
	public void testActiviteAssujettissementAvecAssujettissementADateSeuil() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2000, 1, 1));
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(142324L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(1995, 1, 1), RegDate.get(2000, 12, 31), RegpmTypeAssujettissement.LILIC);
			Assert.assertTrue(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(3422342L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(1995, 1, 1), RegDate.get(2000, 12, 31), RegpmTypeAssujettissement.LIFD);
			Assert.assertTrue(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(59665535);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(1995, 1, 1), RegDate.get(2000, 12, 31), RegpmTypeAssujettissement.SANS);
			Assert.assertFalse(mgr.isActive(e));
		}
	}

	@Test
	public void testActiviteAssujettissementAvecAssujettissementApresDateSeuil() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2000, 1, 1));
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(142324L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2001, 1, 1), RegDate.get(2010, 12, 31), RegpmTypeAssujettissement.LILIC);
			Assert.assertTrue(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(3422342L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2001, 1, 1), RegDate.get(2000, 12, 31), RegpmTypeAssujettissement.LIFD);
			Assert.assertTrue(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(5659654L);
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2001, 1, 1), RegDate.get(2000, 12, 31), RegpmTypeAssujettissement.SANS);
			Assert.assertFalse(mgr.isActive(e));
		}
	}

	@Test
	public void testActivitePerception() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2000, 1, 1), 42L);
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(42L);
			Assert.assertTrue(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(43L);
			Assert.assertFalse(mgr.isActive(e));
		}
	}
}
