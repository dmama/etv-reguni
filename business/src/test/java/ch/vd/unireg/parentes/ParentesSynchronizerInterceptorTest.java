package ch.vd.unireg.parentes;

import java.util.Set;

import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.type.Sexe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ParentesSynchronizerInterceptorTest extends BusinessTest {

	@Test
	public void testDeclenchement() throws Exception {

		final long noIndPapa = 2378326L;
		final long noIndFifille = 327326L;
		final RegDate dateNaissanceFifille = date(1978, 6, 2);

		// mise en place civile
		serviceCivil.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				final MockIndividu papa = addIndividu(noIndPapa, null, "Chollet", "Ignacio", Sexe.MASCULIN);
				final MockIndividu fifille = addIndividu(noIndFifille, dateNaissanceFifille, "Chollet", "Sigourney", Sexe.FEMININ);
				addLiensFiliation(fifille, papa, null, dateNaissanceFifille, null);
			}
		});

		final class Ids {
			long idPapa;
			long idFifille;
		}

		// mise en place fiscale avec synchronizer activé
		final Ids ids = doInNewTransactionAndSessionUnderSwitch(parentesSynchronizer, true, status -> {
			final PersonnePhysique papa = addHabitant(noIndPapa);
			final PersonnePhysique fifille = addHabitant(noIndFifille);

			final Ids ids1 = new Ids();
			ids1.idPapa = papa.getNumero();
			ids1.idFifille = fifille.getNumero();
			return ids1;
		});

		// vérification que les parentés ont été créées
		doInNewTransactionAndSession(status -> {
			final PersonnePhysique papa = (PersonnePhysique) tiersDAO.get(ids.idPapa);
			assertNotNull(papa);

			final Set<RapportEntreTiers> rapports = papa.getRapportsObjet();
			assertNotNull(rapports);
			assertEquals(1, rapports.size());

			final RapportEntreTiers rapport = rapports.iterator().next();
			assertNotNull(rapport);
			assertEquals((Long) ids.idPapa, rapport.getObjetId());
			assertEquals((Long) ids.idFifille, rapport.getSujetId());
			assertEquals(dateNaissanceFifille, rapport.getDateDebut());
			assertNull(rapport.getDateFin());
			assertFalse(rapport.isAnnule());
			return null;
		});
	}
}
