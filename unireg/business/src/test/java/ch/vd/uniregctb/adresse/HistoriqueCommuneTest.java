package ch.vd.uniregctb.adresse;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.WithoutSpringTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;

public class HistoriqueCommuneTest extends WithoutSpringTest {

	@Test
	public void testCollationCommune() throws Exception {

		final RegDate frontiere = RegDate.get(2004, 6, 30);

		// même commune, dates ne matchent pas -> pas collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, MockCommune.Aubonne);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(2), null, MockCommune.Aubonne);
			Assert.assertFalse("Décalage de date", avant.isCollatable(apres));
		}

		// même communes, dates matchent -> collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, MockCommune.Aubonne);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(1), null, MockCommune.Aubonne);
			Assert.assertTrue(avant.isCollatable(apres));

			final HistoriqueCommune collation = (HistoriqueCommune) avant.collate(apres);
			Assert.assertEquals(avant.getDateDebut(), collation.getDateDebut());
			Assert.assertNull(collation.getDateFin());
			Assert.assertEquals(MockCommune.Aubonne.getNoOFSEtendu(), collation.getCommune().getNoOFSEtendu());
		}

		// communes nulles, dates matchent -> collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, null);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(1), null, null);
			Assert.assertTrue(avant.isCollatable(apres));

			final HistoriqueCommune collation = (HistoriqueCommune) avant.collate(apres);
			Assert.assertEquals(avant.getDateDebut(), collation.getDateDebut());
			Assert.assertNull(collation.getDateFin());
			Assert.assertNull(collation.getCommune());
		}

		// communes non nulles et différentes -> non collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, MockCommune.Aubonne);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(1), null, MockCommune.CheseauxSurLausanne);
			Assert.assertFalse("Communes différentes", avant.isCollatable(apres));
		}

		// une commune non-nulle, l'autre nulle -> non collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, MockCommune.Aubonne);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(1), null, null);
			Assert.assertFalse("Communes différentes", avant.isCollatable(apres));
		}

		// une commune non-nulle, l'autre nulle -> non collatable
		{
			final HistoriqueCommune avant = new HistoriqueCommune(RegDate.get(1996, 1, 1), frontiere, null);
			final HistoriqueCommune apres = new HistoriqueCommune(frontiere.addDays(1), null, MockCommune.CheseauxSurLausanne);
			Assert.assertFalse("Communes différentes", avant.isCollatable(apres));
		}
	}
}
