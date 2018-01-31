package ch.vd.uniregctb.tiers;

import org.junit.Test;

import ch.vd.uniregctb.common.WithoutSpringTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Raphaël Marmier, 2016-09-29, <raphael.marmier@vd.ch>
 */
public class EntrepriseTest extends WithoutSpringTest {

	@Test
	public void testGetRapportEtablissementPrincipalValidaAt() {
		final Entreprise entreprise = new Entreprise();
		entreprise.setNumero(1L);

		final ActiviteEconomique uneActiviteEcoSecondaire = new ActiviteEconomique();
		uneActiviteEcoSecondaire.setPrincipal(false);
		uneActiviteEcoSecondaire.setDateDebut(date(2010, 1, 1));
		uneActiviteEcoSecondaire.setDateFin(null);
		uneActiviteEcoSecondaire.setId(9001L);
		uneActiviteEcoSecondaire.setSujetId(1L);
		uneActiviteEcoSecondaire.setObjetId(10L);
		entreprise.addRapportSujet(uneActiviteEcoSecondaire);

		final ActiviteEconomique premiereActiviteEcoPrincipale = new ActiviteEconomique();
		premiereActiviteEcoPrincipale.setPrincipal(true);
		premiereActiviteEcoPrincipale.setDateDebut(date(2010, 1, 1));
		premiereActiviteEcoPrincipale.setDateFin(date(2014, 12, 31));
		premiereActiviteEcoPrincipale.setId(8001L);
		premiereActiviteEcoPrincipale.setSujetId(1L);
		premiereActiviteEcoPrincipale.setObjetId(20L);
		entreprise.addRapportSujet(premiereActiviteEcoPrincipale);

		final ActiviteEconomique secondeActiviteEcoPrincipale = new ActiviteEconomique();
		secondeActiviteEcoPrincipale.setPrincipal(true);
		secondeActiviteEcoPrincipale.setDateDebut(date(2015, 1, 1));
		secondeActiviteEcoPrincipale.setDateFin(null);
		secondeActiviteEcoPrincipale.setId(8002L);
		secondeActiviteEcoPrincipale.setSujetId(1L);
		secondeActiviteEcoPrincipale.setObjetId(30L);
		entreprise.addRapportSujet(secondeActiviteEcoPrincipale);

		final ActiviteEconomique uneAutreactiviteEcoSecondaire = new ActiviteEconomique();
		uneAutreactiviteEcoSecondaire.setPrincipal(false);
		uneAutreactiviteEcoSecondaire.setDateDebut(date(2015, 1, 1));
		uneAutreactiviteEcoSecondaire.setDateFin(null);
		uneAutreactiviteEcoSecondaire.setId(9002L);
		uneAutreactiviteEcoSecondaire.setSujetId(1L);
		uneAutreactiviteEcoSecondaire.setObjetId(40L);
		entreprise.addRapportSujet(uneAutreactiviteEcoSecondaire);

		final ActiviteEconomique aePrincipale = entreprise.getActiviteEconomiquePrincipaleValidAt(date(2016, 1, 1));
		assertNotNull(aePrincipale);
		assertTrue(aePrincipale.isPrincipal());
		assertEquals(date(2015, 1, 1), aePrincipale.getDateDebut());
		assertNull(aePrincipale.getDateFin());
	}
}
