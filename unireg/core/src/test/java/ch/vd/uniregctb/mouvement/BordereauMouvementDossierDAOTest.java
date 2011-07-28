package ch.vd.uniregctb.mouvement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.Sexe;

public class BordereauMouvementDossierDAOTest extends AbstractMouvementDossierDAOTest {

	private BordereauMouvementDossierDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(BordereauMouvementDossierDAO.class, "bordereauMouvementDossierDAO");
	}

	private BordereauMouvementDossier addBordereau(List<? extends ElementDeBordereau> mvts, EtatMouvementDossier nouvelEtat) {
		final BordereauMouvementDossier bordereau = (BordereauMouvementDossier) getHibernateTemplate().merge(new BordereauMouvementDossier());
		final Set<MouvementDossier> contenu = new HashSet<MouvementDossier>(mvts.size());
		for (ElementDeBordereau elt : mvts) {
			elt.setBordereau(bordereau);

			final MouvementDossier mvt = (MouvementDossier) elt;
			if (nouvelEtat != null) {
				mvt.setEtat(nouvelEtat);
			}
			contenu.add(mvt);
		}
		bordereau.setContenu(contenu);
		return bordereau;
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testBordereauxAReceptionner() throws Exception {

		// création des objets en base
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
				final EnvoiDossierVersCollectiviteAdministrative md1 = addMouvementDossierEnvoi(pp1, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				final EnvoiDossierVersCollectiviteAdministrative md2 = addMouvementDossierEnvoi(pp2, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				final EnvoiDossierVersCollectiviteAdministrative md3 = addMouvementDossierEnvoi(pp3, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				final EnvoiDossierVersCollectiviteAdministrative md4 = addMouvementDossierEnvoi(pp4, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				final EnvoiDossierVersCollectiviteAdministrative md5 = addMouvementDossierEnvoi(pp5, oid1, oid5, EtatMouvementDossier.A_ENVOYER);
				final EnvoiDossierVersCollectiviteAdministrative md6 = addMouvementDossierEnvoi(pp6, oid1, oid5, EtatMouvementDossier.TRAITE);        // déjà traité hors bordereau
				final ReceptionDossierArchives md7 = addMouvementDossierArchives(pp7, oid5, EtatMouvementDossier.A_ENVOYER);

				// mouvements à inclure dans un bordereau
				final List<EnvoiDossierVersCollectiviteAdministrative> bordereauEnvoi = Arrays.asList(md1, md2, md3, md4, md5);
				final List<ReceptionDossierArchives> bordereauReception = Arrays.asList(md7);
				addBordereau(bordereauEnvoi, EtatMouvementDossier.TRAITE);
				addBordereau(bordereauReception, EtatMouvementDossier.TRAITE);

				return null;
			}
		});

		{
			final List<BordereauMouvementDossier> bordereaux = dao.getBordereauxAReceptionner(null);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals("Les bordereaux de réceptions ne sont pas 'à réceptionner'", 1, bordereaux.size());

			final BordereauMouvementDossier bordereau = bordereaux.get(0);
			Assert.assertNotNull(bordereau);
			Assert.assertEquals(1, (int) bordereau.getExpediteur().getNumeroCollectiviteAdministrative());
			Assert.assertEquals(5, (int) bordereau.getDestinataire().getNumeroCollectiviteAdministrative());
			Assert.assertEquals(5, bordereau.getContenu().size());      // le dernier mouvement depuis OID1 vers OID5 ne fait pas partie du bordereau
			Assert.assertEquals(5, bordereau.getNombreMouvementsEnvoyes());
			Assert.assertEquals(0, bordereau.getNombreMouvementsRecus());
		}

		{
			final List<BordereauMouvementDossier> bordereaux = dao.getBordereauxAReceptionner(5);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals("l'OID 5 devrait réceptionner un bordereau", 1, bordereaux.size());

			final BordereauMouvementDossier bordereau = bordereaux.get(0);
			Assert.assertNotNull(bordereau);
			Assert.assertEquals(1, (int) bordereau.getExpediteur().getNumeroCollectiviteAdministrative());
			Assert.assertEquals(5, (int) bordereau.getDestinataire().getNumeroCollectiviteAdministrative());
			Assert.assertEquals(5, bordereau.getContenu().size());      // le dernier mouvement depuis OID1 vers OID5 ne fait pas partie du bordereau
			Assert.assertEquals(5, bordereau.getNombreMouvementsEnvoyes());
			Assert.assertEquals(0, bordereau.getNombreMouvementsRecus());
		}

		{
			final List<BordereauMouvementDossier> bordereaux = dao.getBordereauxAReceptionner(1);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals("L'OID 1 ne réceptionne rien du tout...", 0, bordereaux.size());
		}

		{
			final List<BordereauMouvementDossier> bordereaux = dao.getBordereauxAReceptionner(13);
			Assert.assertNotNull(bordereaux);
			Assert.assertEquals("L'OID 13 ne réceptionne rien du tout...", 0, bordereaux.size());
		}
	}
}
