package ch.vd.uniregctb.mouvement;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.junit.Test;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeMouvement;

public class MouvementDossierDAOTest extends AbstractMouvementDossierDAOTest {

	private MouvementDossierDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(MouvementDossierDAO.class, "mouvementDossierDAO");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testSave() {

		final Long mvtId = getHibernateTemplate().executeWithNewSession(new HibernateCallback<Long>() {
			@Override
			public Long doInHibernate(Session session) throws HibernateException, SQLException {
				final CollectiviteAdministrative oid = addCollectiviteAdministrative(7);
				final PersonnePhysique pp = addNonHabitant("Gudule", "Tartempion", RegDate.get(1960, 3, 12), Sexe.FEMININ);
				final MouvementDossier mvt = addMouvementDossierClassementGeneral(pp, oid, EtatMouvementDossier.A_TRAITER);
				return mvt.getId();
			}
		});
		Assert.assertNotNull(mvtId);

		final MouvementDossier mvt = dao.get(mvtId);
		Assert.assertNotNull(mvt);
		Assert.assertTrue(mvt instanceof ReceptionDossierClassementGeneral);
		Assert.assertEquals(EtatMouvementDossier.A_TRAITER, mvt.getEtat());
		Assert.assertNotNull(mvt.getContribuable());
		Assert.assertTrue(mvt.getContribuable() instanceof PersonnePhysique);

		final PersonnePhysique pp = (PersonnePhysique) mvt.getContribuable();
		Assert.assertEquals("Gudule", pp.getPrenom());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testProtoBordereaux() throws Exception {

		doInNewTransaction(new TxCallback<Object>() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// on crée deux mvts de dossier de l'OID 1 à l'OID 5, trois réceptions pour archive à l'OID 5
				final CollectiviteAdministrative oid1 = addCollectiviteAdministrative(1);
				final CollectiviteAdministrative oid5 = addCollectiviteAdministrative(5);

				// il me faut donc 7 contribuables (un de plus pour un mouvement déjà traité, un autre pour un mouvement à traité)
				final PersonnePhysique pp1 = addNonHabitant("Alphonsine", "Dubois", RegDate.get(1923, 2, 12), Sexe.FEMININ);
				final PersonnePhysique pp2 = addNonHabitant("Ernestine", "Dupont", RegDate.get(1922, 4, 5), Sexe.FEMININ);
				final PersonnePhysique pp3 = addNonHabitant("Emile", "Pittet", RegDate.get(1912, 6, 12), Sexe.MASCULIN);
				final PersonnePhysique pp4 = addNonHabitant("Robert", "Debout", RegDate.get(1965, 12, 4), Sexe.MASCULIN);
				final PersonnePhysique pp5 = addNonHabitant("Valérie", "Marchand", RegDate.get(1970, 1, 3), Sexe.FEMININ);
				final PersonnePhysique pp6 = addNonHabitant("Marcel", "Petit", RegDate.get(1945, 1, 3), Sexe.MASCULIN);
				final PersonnePhysique pp7 = addNonHabitant("Kevin", "Martin", RegDate.get(1985, 3, 2), Sexe.MASCULIN);

				// création des mouvements
				addMouvementDossierEnvoi(pp1, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				addMouvementDossierEnvoi(pp2, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				addMouvementDossierEnvoi(pp3, oid1, oid5, EtatMouvementDossier.A_TRAITER);
				addMouvementDossierArchives(pp4, oid5, EtatMouvementDossier.A_ENVOYER);
				addMouvementDossierArchives(pp5, oid5, EtatMouvementDossier.A_ENVOYER);
				addMouvementDossierArchives(pp6, oid5, EtatMouvementDossier.A_ENVOYER);
				addMouvementDossierArchives(pp7, oid5, EtatMouvementDossier.TRAITE);

				return null;
			}
		});

		{
			final List<ProtoBordereauMouvementDossier> bordereaux = dao.getAllProtoBordereaux(null);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals(2, bordereaux.size());

			final ProtoBordereauMouvementDossier envoi = bordereaux.get(0).type == TypeMouvement.EnvoiDossier ? bordereaux.get(0) : bordereaux.get(1);
			final ProtoBordereauMouvementDossier archives = bordereaux.get(0).type == TypeMouvement.ReceptionDossier ? bordereaux.get(0) : bordereaux.get(1);
			Assert.assertNotNull(envoi);
			Assert.assertNotNull(archives);

			// envoi ?
			Assert.assertEquals(2, envoi.nbMouvements);
			Assert.assertEquals(1, envoi.noCollAdmInitiatrice);
			Assert.assertEquals(5, (int) envoi.noCollAdmDestinataire);

			// réception archives ?
			Assert.assertEquals(3, archives.nbMouvements);
			Assert.assertEquals(5, archives.noCollAdmInitiatrice);
			Assert.assertNull(archives.noCollAdmDestinataire);
		}

		{
			final List<ProtoBordereauMouvementDossier> bordereaux = dao.getAllProtoBordereaux(1);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals(1, bordereaux.size());

			final ProtoBordereauMouvementDossier envoi = bordereaux.get(0);
			Assert.assertNotNull(envoi);
			Assert.assertEquals(TypeMouvement.EnvoiDossier, envoi.type);

			// envoi ?
			Assert.assertEquals(2, envoi.nbMouvements);
			Assert.assertEquals(1, envoi.noCollAdmInitiatrice);
			Assert.assertEquals(5, (int) envoi.noCollAdmDestinataire);
		}

		{
			final List<ProtoBordereauMouvementDossier> bordereaux = dao.getAllProtoBordereaux(5);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals(1, bordereaux.size());

			final ProtoBordereauMouvementDossier archives = bordereaux.get(0);
			Assert.assertNotNull(archives);
			Assert.assertEquals(TypeMouvement.ReceptionDossier, archives.type);

			// réception archives ?
			Assert.assertEquals(3, archives.nbMouvements);
			Assert.assertEquals(5, archives.noCollAdmInitiatrice);
			Assert.assertNull(archives.noCollAdmDestinataire);
		}

		{
			final List<ProtoBordereauMouvementDossier> bordereaux = dao.getAllProtoBordereaux(12);
			Assert.assertNull(bordereaux);
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testRechercheSeulementDerniersMouvements() throws Exception {

		final PersonnePhysique pp = addNonHabitant("Toto", "Tartempion", date(1980, 10, 25), Sexe.MASCULIN);
		final CollectiviteAdministrative oid1 = addCollectiviteAdministrative(1);
		final CollectiviteAdministrative oid5 = addCollectiviteAdministrative(5);

		final MouvementDossierCriteria criteria = new MouvementDossierCriteria();
		criteria.setSeulementDerniersMouvements(true);
		criteria.setInclureMouvementsAnnules(true);
		criteria.setNoCtb(pp.getNumero());
		criteria.setEtatsMouvement(Arrays.asList(EtatMouvementDossier.A_TRAITER, EtatMouvementDossier.A_ENVOYER, EtatMouvementDossier.TRAITE));

		// les mouvements annulés ne doivent pas apparaître quand on demande seulement les derniers mouvements (même si explicitement demandés)
		final MouvementDossier mvtAnnule = addMouvementDossierArchives(pp, oid5, EtatMouvementDossier.TRAITE);
		mvtAnnule.setAnnule(true);
		mvtAnnule.setDateMouvement(RegDate.get());

		{
			final List<MouvementDossier> trouves = dao.find(criteria, null);
			Assert.assertEquals(0, trouves.size());
		}

		// les mouvement non-traités ne doivent pas apparaître non-plus (même s'ils sont demandés)
		final MouvementDossier mvtNonTraite = addMouvementDossierClassementGeneral(pp, oid5, EtatMouvementDossier.A_TRAITER);
		mvtNonTraite.setDateMouvement(RegDate.get().getOneDayBefore());

		{
			final List<MouvementDossier> trouves = dao.find(criteria, null);
			Assert.assertEquals(0, trouves.size());
		}

		// c'est le dernier mouvement traité, donc il doit apparaître
		final MouvementDossier mvtTraiteThreeDays = addMouvementDossierEnvoi(pp, oid1, oid5, EtatMouvementDossier.TRAITE);
		mvtTraiteThreeDays.setDateMouvement(RegDate.get().addDays(-3));

		{
			final List<MouvementDossier> trouves = dao.find(criteria, null);
			Assert.assertEquals(1, trouves.size());

			final MouvementDossier trouve = trouves.get(0);
			Assert.assertEquals(mvtTraiteThreeDays.getId(), trouve.getId());
		}

		// il devient maintenant le dernier mouvement traité, donc il doit apparaître
		final MouvementDossier mvtTraiteTwoDays = addMouvementDossierEnvoi(pp, oid1, oid5, EtatMouvementDossier.TRAITE);
		mvtTraiteTwoDays.setDateMouvement(RegDate.get().addDays(-2));

		{
			final List<MouvementDossier> trouves = dao.find(criteria, null);
			Assert.assertEquals(1, trouves.size());

			final MouvementDossier trouve = trouves.get(0);
			Assert.assertEquals(mvtTraiteTwoDays.getId(), trouve.getId());
		}

		// on attend un peu pour être sur que sur une même date de mouvement, le logmdate de celui qui vient est bien après le logmdate du précédent
		Thread.sleep(100);

		// il devient maintenant le dernier mouvement traité, donc il doit apparaître
		final MouvementDossier mvtTraiteTwoDaysALittleLater = addMouvementDossierEnvoi(pp, oid1, oid5, EtatMouvementDossier.TRAITE);
		mvtTraiteTwoDaysALittleLater.setDateMouvement(RegDate.get().addDays(-2));

		{
			final List<MouvementDossier> trouves = dao.find(criteria, null);
			Assert.assertEquals(1, trouves.size());

			final MouvementDossier trouve = trouves.get(0);
			Assert.assertEquals(mvtTraiteTwoDaysALittleLater.getId(), trouve.getId());
		}
	}
}
