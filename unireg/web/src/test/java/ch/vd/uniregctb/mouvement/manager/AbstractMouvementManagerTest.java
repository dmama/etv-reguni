package ch.vd.uniregctb.mouvement.manager;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.mouvement.EtatMouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossier;
import ch.vd.uniregctb.mouvement.MouvementDossierDAO;
import ch.vd.uniregctb.mouvement.ReceptionDossierClassementGeneral;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.Sexe;

public class AbstractMouvementManagerTest extends BusinessTest {

	public static final Logger LOGGER = Logger.getLogger(AbstractMouvementManagerTest.class);

	private MouvementDossierDAO mvtDAO;

	private TiersDAO tiersDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		mvtDAO = getBean(MouvementDossierDAO.class, "mouvementDossierDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testDestructionMouvementsTropVieux() throws Exception {

		final long noAchille = addNonHabitant("Achille", "Talon", RegDate.get(1956, 12, 4), Sexe.MASCULIN).getNumero();
		for (int i = 0 ; i < 15 ; ++ i) {

			// première étape, on rajoute un mouvement de dossier, et on limite au nombre max
			final int index = i;
			hibernateTemplate.executeWithNewSession(new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {

					final CollectiviteAdministrative caArrivee = tiersService.getOrCreateCollectiviteAdministrative(index + 1);
					final ReceptionDossierClassementGeneral reception = new ReceptionDossierClassementGeneral();
					reception.setCollectiviteAdministrativeReceptrice(caArrivee);
					reception.setDateMouvement(RegDate.get());
					reception.setEtat(EtatMouvementDossier.TRAITE);

					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);

					pp.addMouvementDossier(reception);

					LOGGER.debug(String.format("Après %d mouvement(s) ajouté(s)", index + 1));
					AbstractMouvementManagerImpl.detruireMouvementsTropVieux(mvtDAO, pp);
					return null;
				}
			});

			// deuxième étape, on vérifie que cette limitation n'est pas abusive, mais qu'elle est bien respectée
			hibernateTemplate.executeWithNewSession(new HibernateCallback() {
				public Object doInHibernate(Session session) throws HibernateException, SQLException {
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);

					if (index < 10) {
						// aucun mouvement ne doit avoir été enlevé
				        Assert.assertEquals(index + 1, pp.getMouvementsDossier().size());
					}
					else {
						// là on doit commencer à avoir enlevé des trucs
						Assert.assertEquals(10, pp.getMouvementsDossier().size());

						// vérifions ceux qui sont encore là...
						for (MouvementDossier mvt : pp.getMouvementsDossier()) {
							Assert.assertTrue(mvt instanceof ReceptionDossierClassementGeneral);

							final ReceptionDossierClassementGeneral reception = (ReceptionDossierClassementGeneral) mvt;
							Assert.assertTrue(reception.getCollectiviteAdministrativeReceptrice().getNumeroCollectiviteAdministrative() > index - 10);
						}
					}
					return null;
				}
			});

			// on attend un peu pour être certain que les mouvements pourront être triés dans
			// le bon ordre (et donc que les plus vieux seront bien effacés d'abord)
			Thread.sleep(300);
		}
	}
}
