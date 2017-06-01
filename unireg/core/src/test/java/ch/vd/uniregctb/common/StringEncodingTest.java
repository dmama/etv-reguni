package ch.vd.uniregctb.common;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.RaisonSocialeFiscaleEntreprise;

public class StringEncodingTest extends CoreDAOTest {

	@Test
	public void testRaisonSocialeEntrepriseAvecCaractereTresSpecial() throws Exception {

		final String raisonSociale = "l’idée";      // c'est l'apostrophe qui a posé problème à Oracle avec Atomikos
		final RegDate dateDebut = date(2009, 1, 1);

		// création d'une entreprise
		final long id = doInNewTransaction(status -> {
			final Entreprise entreprise = addEntrepriseInconnueAuCivil();
			addRaisonSocialeFiscaleEntreprise(entreprise, dateDebut, null, raisonSociale);
			return entreprise.getNumero();
		});

		// vérification de ce qui est stoké en base (on a eu des cas où l'apostrophe avait été transformé en '¿'
		doInNewReadOnlyTransaction(status -> {
			final Entreprise entreprise = (Entreprise) tiersDAO.get(id);
			Assert.assertNotNull(entreprise);
			final Set<DonneeCivileEntreprise> donneesCiviles = entreprise.getDonneesCiviles();
			Assert.assertNotNull(donneesCiviles);
			Assert.assertEquals(1, donneesCiviles.size());
			final DonneeCivileEntreprise donneeCivile = donneesCiviles.iterator().next();
			Assert.assertNotNull(donneeCivile);
			Assert.assertEquals(RaisonSocialeFiscaleEntreprise.class, donneeCivile.getClass());
			final RaisonSocialeFiscaleEntreprise raisonSocialeEntreprise = (RaisonSocialeFiscaleEntreprise) donneeCivile;
			Assert.assertFalse(raisonSocialeEntreprise.isAnnule());
			Assert.assertEquals(dateDebut, raisonSocialeEntreprise.getDateDebut());
			Assert.assertNull(raisonSocialeEntreprise.getDateFin());
			Assert.assertEquals(raisonSociale, raisonSocialeEntreprise.getRaisonSociale());     // <-- ici, on avait "l¿idée"...
			return null;
		});
	}
}
