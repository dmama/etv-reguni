package ch.vd.uniregctb.migration.pm.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.regpm.RegpmCategoriePersonneMorale;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeAssujettissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeEtatQuestionnaireSNC;
import ch.vd.uniregctb.migration.pm.regpm.RegpmTypeFormeJuridique;

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
				b.append(id).append(";N").append(System.lineSeparator());       // "N" pour "pas seulement ADB"
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

	@Test
	public void testActiviteSNCsansQuestionnaireSNC() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2015, 1, 1));
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(1243L);
			EntrepriseMigratorTest.addFormeJuridique(e, RegDate.get(2000, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C.", RegpmCategoriePersonneMorale.SP));
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
			Assert.assertFalse(mgr.isActive(e));
		}
		{
			final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(1243L);
			EntrepriseMigratorTest.addFormeJuridique(e, RegDate.get(2000, 1, 1), EntrepriseMigratorTest.createTypeFormeJuridique("S. COMM.", RegpmCategoriePersonneMorale.SP));
			EntrepriseMigratorTest.addAssujettissement(e, RegDate.get(2000, 1, 1), null, RegpmTypeAssujettissement.LILIC);
			Assert.assertFalse(mgr.isActive(e));
		}
	}

	@Test
	public void testActiviteAvecFormeJuridiqueDateNulle() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2015, 1, 1));
		final RegpmTypeFormeJuridique typeFormeJuridique = EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C", RegpmCategoriePersonneMorale.SP);
		final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(1243L);
		EntrepriseMigratorTest.addFormeJuridique(e, null, typeFormeJuridique);
		EntrepriseMigratorTest.addQuestionnaireSNC(e, 2015, RegpmTypeEtatQuestionnaireSNC.RECU, RegDate.get(2016, 2, 1), RegDate.get(2016, 9, 30), RegDate.get(2016, 2, 10), null, null);
		Assert.assertTrue(mgr.isActive(e));         // on a quand-même reconnu la catégorie SP malgré sa date nulle !
	}

	@Test
	public void testActiviteSNCsansQuestionnaireSNCdepuisSeuil() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2015, 1, 1));
		final RegpmTypeFormeJuridique[] formesJuridiques = {
				EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C", RegpmCategoriePersonneMorale.SP),
				EntrepriseMigratorTest.createTypeFormeJuridique("S. COMM.", RegpmCategoriePersonneMorale.SP),
				EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM),
				EntrepriseMigratorTest.createTypeFormeJuridique("FOND", RegpmCategoriePersonneMorale.APM)
		};

		for (RegpmTypeFormeJuridique typeFormeJuridique : formesJuridiques) {
			for (RegpmTypeEtatQuestionnaireSNC etatQuestionnaire : RegpmTypeEtatQuestionnaireSNC.values()) {
				final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(1243L);
				EntrepriseMigratorTest.addFormeJuridique(e, RegDate.get(2000, 1, 1), typeFormeJuridique);
				EntrepriseMigratorTest.addQuestionnaireSNC(e, 2014, etatQuestionnaire, RegDate.get(2015, 2, 1), RegDate.get(2015, 9, 30), null, null, null);
				Assert.assertFalse(String.format("%s / %s", typeFormeJuridique, etatQuestionnaire), mgr.isActive(e));         // questionnaire trop vieux !
			}
		}
	}

	@Test
	public void testActiviteSNCavecQuestionnaireSNCdepuisSeuil() throws Exception {
		final ActivityManager mgr = buildInstance(RegDate.get(2015, 1, 1));
		final RegpmTypeFormeJuridique[] formesJuridiques = {
				EntrepriseMigratorTest.createTypeFormeJuridique("S.N.C", RegpmCategoriePersonneMorale.SP),
				EntrepriseMigratorTest.createTypeFormeJuridique("S. COMM.", RegpmCategoriePersonneMorale.SP),
				EntrepriseMigratorTest.createTypeFormeJuridique("S.A.", RegpmCategoriePersonneMorale.PM),
				EntrepriseMigratorTest.createTypeFormeJuridique("FOND", RegpmCategoriePersonneMorale.APM)
		};

		for (RegpmTypeFormeJuridique typeFormeJuridique : formesJuridiques) {
			for (RegpmTypeEtatQuestionnaireSNC etatQuestionnaire : RegpmTypeEtatQuestionnaireSNC.values()) {
				final RegpmEntreprise e = EntrepriseMigratorTest.buildEntreprise(1243L);
				EntrepriseMigratorTest.addFormeJuridique(e, RegDate.get(2000, 1, 1), typeFormeJuridique);
				EntrepriseMigratorTest.addQuestionnaireSNC(e, 2015, etatQuestionnaire, RegDate.get(2016, 2, 1), RegDate.get(2016, 9, 30), RegDate.get(2016, 2, 10), null, null);
				Assert.assertEquals(String.format("%s / %s", typeFormeJuridique, etatQuestionnaire),
				                    typeFormeJuridique.getCategorie() == RegpmCategoriePersonneMorale.SP && etatQuestionnaire != RegpmTypeEtatQuestionnaireSNC.ANNULE,
				                    mgr.isActive(e));
			}
		}
	}

	@Test
	public void testChargementFichierPerception() throws Exception {
		final String contenuFichier = "NDC;ADBSEUL\n" +
				"1;N\n" +
				"2;N\n" +
				"28;O\n" +
				"30;N\n" +
				"43;O\n" +
				"54;O\n" +
				"60;O\n" +
				"61;N\n";

		final ActivityManagerImpl mgr;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(contenuFichier.getBytes())) {
			 mgr = new ActivityManagerImpl(bais, RegDate.get(2008, 1, 1));
		}
		Assert.assertTrue(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(1L)));
		Assert.assertTrue(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(2L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(9L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(23L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(28L)));
		Assert.assertTrue(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(30L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(35L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(43L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(54L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(60L)));
		Assert.assertTrue(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(61L)));
		Assert.assertFalse(mgr.isActive(EntrepriseMigratorTest.buildEntreprise(100L)));
	}
}
