package ch.vd.uniregctb.mouvement.manager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.mouvement.BordereauMouvementDossier;
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

	private static final int NB_MAX_MOUVEMENTS_GARDES = AbstractMouvementManagerImpl.NB_MAX_MOUVEMENTS_GARDES;

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

		final long noAchille = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", RegDate.get(1956, 12, 4), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});
		
		for (int i = 0 ; i < NB_MAX_MOUVEMENTS_GARDES + 5 ; ++ i) {

			// première étape, on rajoute un mouvement de dossier, et on limite au nombre max
			final int index = i;
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
						@Override
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
				}
			});

			// deuxième étape, on vérifie que cette limitation n'est pas abusive, mais qu'elle est bien respectée
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					return hibernateTemplate.executeWithNewSession(new HibernateCallback<Object>() {
						@Override
						public Object doInHibernate(Session session) throws HibernateException, SQLException {
							final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);

							if (index < NB_MAX_MOUVEMENTS_GARDES) {
								// aucun mouvement ne doit avoir été enlevé
								Assert.assertEquals(index + 1, pp.getMouvementsDossier().size());
							}
							else {
								// là on doit commencer à avoir enlevé des trucs
								Assert.assertEquals(NB_MAX_MOUVEMENTS_GARDES, pp.getMouvementsDossier().size());

								// vérifions ceux qui sont encore là...
								for (MouvementDossier mvt : pp.getMouvementsDossier()) {
									Assert.assertTrue(mvt instanceof ReceptionDossierClassementGeneral);

									final ReceptionDossierClassementGeneral reception = (ReceptionDossierClassementGeneral) mvt;
									Assert.assertTrue(reception.getCollectiviteAdministrativeReceptrice().getNumeroCollectiviteAdministrative() > index - NB_MAX_MOUVEMENTS_GARDES);
								}
							}
							return null;
						}
					});
				}
			});

			// on attend un peu pour être certain que les mouvements pourront être triés dans
			// le bon ordre (et donc que les plus vieux seront bien effacés d'abord)
			Thread.sleep(300);
		}
	}

	@Ignore("Tant que SIFISC-4378 n'est pas résolu, on suppose ici que les mouvements non-annulés qui sont par ailleurs présents sur un bordereau doivent être éliminés quand-même")
	@Test
	public void testDestructionMouvementNonAnnuleTropVieuxPresentSurBordereau() throws Exception {

		final long noAchille = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", RegDate.get(1956, 12, 4), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);

				// on crée n+1 mouvements de dossier, le dernier étant le plus ancien (= celui qui va partir)
				for (int index = 0 ; index < NB_MAX_MOUVEMENTS_GARDES + 1 ; ++ index) {
					final CollectiviteAdministrative caArrivee = tiersService.getOrCreateCollectiviteAdministrative(index + 1);
					final ReceptionDossierClassementGeneral reception = new ReceptionDossierClassementGeneral();
					reception.setCollectiviteAdministrativeReceptrice(caArrivee);
					reception.setDateMouvement(RegDate.get().addDays(-index));
					reception.setEtat(EtatMouvementDossier.TRAITE);
					pp.addMouvementDossier(reception);
				}
				return null;
			}
		});

		// ajout du mouvement à un bordereau
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);
				final List<MouvementDossier> mvts = new ArrayList<MouvementDossier>(pp.getMouvementsDossier());
				Collections.sort(mvts, new AntiChronologiqueMouvementComparator());
				
				final List<MouvementDossier> mvtsDansBordereau = mvts.subList(mvts.size() - 1, mvts.size());        // seulement le dernier
				final BordereauMouvementDossier bordereau = new BordereauMouvementDossier();
				bordereau.setContenu(new HashSet<MouvementDossier>(mvtsDansBordereau));
				hibernateTemplate.save(bordereau);
				return null;
			}
		});

		// essai de limitation au nombre de mouvements max
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);
				Assert.assertEquals(NB_MAX_MOUVEMENTS_GARDES + 1, pp.getMouvementsDossier().size());

				AbstractMouvementManagerImpl.detruireMouvementsTropVieux(mvtDAO, pp);

				Assert.assertEquals(NB_MAX_MOUVEMENTS_GARDES, pp.getMouvementsDossier().size());
				return null;
			}
		});
	}

	@Ignore("Tant que SIFISC-4378 n'est pas résolu, on suppose ici que les mouvements annulés qui sont par ailleurs présents sur un bordereau doivent être éliminés quand-même")
	@Test
	public void testDestructionMouvementAnnuleTropVieuxPresentSurBordereau() throws Exception {

		final long noAchille = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addNonHabitant("Achille", "Talon", RegDate.get(1956, 12, 4), Sexe.MASCULIN);
				return pp.getNumero();
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);

				// on crée n+1 mouvements de dossier, le dernier étant le plus ancien (= celui qui va partir)
				for (int index = 0 ; index < NB_MAX_MOUVEMENTS_GARDES + 1 ; ++ index) {
					final CollectiviteAdministrative caArrivee = tiersService.getOrCreateCollectiviteAdministrative(index + 1);
					final ReceptionDossierClassementGeneral reception = new ReceptionDossierClassementGeneral();
					reception.setCollectiviteAdministrativeReceptrice(caArrivee);
					reception.setDateMouvement(RegDate.get().addDays(-index));
					reception.setEtat(EtatMouvementDossier.TRAITE);
					reception.setAnnule(true);
					pp.addMouvementDossier(reception);
				}
				return null;
			}
		});

		// ajout du mouvement à un bordereau
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);
				final List<MouvementDossier> mvts = new ArrayList<MouvementDossier>(pp.getMouvementsDossier());
				Collections.sort(mvts, new AntiChronologiqueMouvementComparator());

				final List<MouvementDossier> mvtsDansBordereau = mvts.subList(mvts.size() - 1, mvts.size());        // seulement le dernier
				final BordereauMouvementDossier bordereau = new BordereauMouvementDossier();
				bordereau.setContenu(new HashSet<MouvementDossier>(mvtsDansBordereau));
				hibernateTemplate.save(bordereau);
				return null;
			}
		});

		// essai de limitation au nombre de mouvements max
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(noAchille);
				Assert.assertEquals(NB_MAX_MOUVEMENTS_GARDES + 1, pp.getMouvementsDossier().size());

				AbstractMouvementManagerImpl.detruireMouvementsTropVieux(mvtDAO, pp);

				Assert.assertEquals(NB_MAX_MOUVEMENTS_GARDES, pp.getMouvementsDossier().size());
				return null;
			}
		});
	}
}
