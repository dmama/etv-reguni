package ch.vd.unireg.avatar;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.CollectiviteAdministrative;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.Sexe;

public class AvatarServiceTest extends BusinessTest {

	private AvatarService avatarService;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();
		this.avatarService = getBean(AvatarService.class, "avatarService");
	}

	/**
	 * On vérifie que tous les types d'avatar ont une image accessible
	 */
	@Test
	public void testImagePath() throws Exception {
		for (TypeAvatar type : TypeAvatar.values()) {

			// sans lien
			{
				final String path = AvatarServiceImpl.getImagePath(type, false);
				final URL url = AvatarServiceImpl.class.getResource(path);
				Assert.assertNotNull(type.name(), url);
			}

			// avec lien
			{
				final String path = AvatarServiceImpl.getImagePath(type, true);
				final URL url = AvatarServiceImpl.class.getResource(path);
				Assert.assertNotNull(type.name(), url);
			}
		}
	}

	@Test
	public void testTypeTiersNull() throws Exception {
		Assert.assertNull(avatarService.getTypeAvatar(null));
	}

	@Test
	public void testTypeEntreprise() throws Exception {
		Assert.assertEquals(TypeAvatar.ENTREPRISE, avatarService.getTypeAvatar(new Entreprise()));
	}

	@Test
	public void testTypeEtablissement() throws Exception {
		Assert.assertEquals(TypeAvatar.ETABLISSEMENT, avatarService.getTypeAvatar(new Etablissement()));
	}

	@Test
	public void testTypeAutreCommunaute() throws Exception {
		Assert.assertEquals(TypeAvatar.AUTRE_COMM, avatarService.getTypeAvatar(new AutreCommunaute()));
	}

	@Test
	public void testTypeCollectiviteAdministrative() throws Exception {
		Assert.assertEquals(TypeAvatar.COLLECT_ADMIN, avatarService.getTypeAvatar(new CollectiviteAdministrative()));
	}

	@Test
	public void testTypeDebiteurPrestationImposable() throws Exception {
		Assert.assertEquals(TypeAvatar.DEBITEUR, avatarService.getTypeAvatar(new DebiteurPrestationImposable()));
	}

	@Test
	public void testPersonnePhysique() throws Exception {

		final class Ids {
			long lui;
			long elle;
			long androgyne;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final PersonnePhysique lui = addNonHabitant("Francis", "Rose", date(1965, 8, 3), Sexe.MASCULIN);
			final PersonnePhysique elle = addNonHabitant("Francesca", "Albina", date(1967, 8, 3), Sexe.FEMININ);
			final PersonnePhysique androgyne = addNonHabitant("Claude", "Dumoulin", date(1965, 8, 3), null);

			final Ids res = new Ids();
			res.lui = lui.getNumero();
			res.elle = elle.getNumero();
			res.androgyne = androgyne.getNumero();
			return res;
		});

		// vérifications
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				Assert.assertNotNull(lui);
				Assert.assertEquals(TypeAvatar.HOMME, avatarService.getTypeAvatar(lui));

				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);
				Assert.assertNotNull(elle);
				Assert.assertEquals(TypeAvatar.FEMME, avatarService.getTypeAvatar(elle));

				final PersonnePhysique androgyne = (PersonnePhysique) tiersDAO.get(ids.androgyne);
				Assert.assertNotNull(androgyne);
				Assert.assertEquals(TypeAvatar.SEXE_INCONNU, avatarService.getTypeAvatar(androgyne));
			}
		});
	}

	@Test
	public void testMenageCommun() throws Exception {

		final class Ids {
			long luiSeul;
			long elleSeule;
			long androgynes;
			long luiAvecAndrogyne;
			long elleAvecAndrogyne;
			long elleEtLui;
			long elles;
			long eux;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(status -> {
			final MenageCommun luiSeul;
			{
				final PersonnePhysique lui = addNonHabitant("Francis", "Rose", date(1965, 8, 3), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, null, date(2001, 5, 2), null);
				luiSeul = couple.getMenage();
			}

			final MenageCommun elleSeule;
			{
				final PersonnePhysique elle = addNonHabitant("Francesca", "Albina", date(1967, 8, 3), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(elle, null, date(2001, 5, 2), null);
				elleSeule = couple.getMenage();
			}

			final MenageCommun androgynes;
			{
				final PersonnePhysique androgyne1 = addNonHabitant("Claude", "Albina", date(1967, 8, 3), null);
				final PersonnePhysique androgyne2 = addNonHabitant("Camille", "Albina", date(1967, 8, 3), null);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(androgyne1, androgyne2, date(2001, 5, 2), null);
				androgynes = couple.getMenage();
			}

			final MenageCommun luiAvecAndrogyne;
			{
				final PersonnePhysique lui = addNonHabitant("Claude", "Albina", date(1967, 8, 3), Sexe.MASCULIN);
				final PersonnePhysique androgyne = addNonHabitant("Camille", "Albina", date(1967, 8, 3), null);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, androgyne, date(2001, 5, 2), null);
				luiAvecAndrogyne = couple.getMenage();
			}

			final MenageCommun elleAvecAndrogyne;
			{
				final PersonnePhysique elle = addNonHabitant("Claude", "Albina", date(1967, 8, 3), Sexe.FEMININ);
				final PersonnePhysique androgyne = addNonHabitant("Camille", "Albina", date(1967, 8, 3), null);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(elle, androgyne, date(2001, 5, 2), null);
				elleAvecAndrogyne = couple.getMenage();
			}

			final MenageCommun elleEtLui;
			{
				final PersonnePhysique lui = addNonHabitant("Francis", "Rose", date(1965, 8, 3), Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Francesca", "Albina", date(1967, 8, 3), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, date(2001, 5, 2), null);
				elleEtLui = couple.getMenage();
			}

			final MenageCommun elles;
			{
				final PersonnePhysique elle1 = addNonHabitant("Claude", "Albina", date(1967, 8, 3), Sexe.FEMININ);
				final PersonnePhysique elle2 = addNonHabitant("Camille", "Albina", date(1967, 8, 3), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(elle1, elle2, date(2001, 5, 2), null);
				elles = couple.getMenage();
			}

			final MenageCommun eux;
			{
				final PersonnePhysique lui1 = addNonHabitant("Claude", "Albina", date(1967, 8, 3), Sexe.MASCULIN);
				final PersonnePhysique lui2 = addNonHabitant("Camille", "Albina", date(1967, 8, 3), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui1, lui2, date(2001, 5, 2), null);
				eux = couple.getMenage();
			}

			final Ids res = new Ids();
			res.luiSeul = luiSeul.getNumero();
			res.elleSeule = elleSeule.getNumero();
			res.androgynes = androgynes.getNumero();
			res.luiAvecAndrogyne = luiAvecAndrogyne.getNumero();
			res.elleAvecAndrogyne = elleAvecAndrogyne.getNumero();
			res.elleEtLui = elleEtLui.getNumero();
			res.elles = elles.getNumero();
			res.eux = eux.getNumero();
			return res;
		});

		// vérification
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final MenageCommun luiSeul = (MenageCommun) tiersDAO.get(ids.luiSeul);
				Assert.assertNotNull(luiSeul);
				Assert.assertEquals(TypeAvatar.MC_HOMME_SEUL, avatarService.getTypeAvatar(luiSeul));

				final MenageCommun elleSeule = (MenageCommun) tiersDAO.get(ids.elleSeule);
				Assert.assertNotNull(elleSeule);
				Assert.assertEquals(TypeAvatar.MC_FEMME_SEULE, avatarService.getTypeAvatar(elleSeule));

				final MenageCommun androgynes = (MenageCommun) tiersDAO.get(ids.androgynes);
				Assert.assertNotNull(androgynes);
				Assert.assertEquals(TypeAvatar.MC_SEXE_INCONNU, avatarService.getTypeAvatar(androgynes));

				final MenageCommun luiAvecAndrogyne = (MenageCommun) tiersDAO.get(ids.luiAvecAndrogyne);
				Assert.assertNotNull(luiAvecAndrogyne);
				Assert.assertEquals(TypeAvatar.MC_HOMME_SEUL, avatarService.getTypeAvatar(luiAvecAndrogyne));

				final MenageCommun elleAvecAndrogyne = (MenageCommun) tiersDAO.get(ids.elleAvecAndrogyne);
				Assert.assertNotNull(elleAvecAndrogyne);
				Assert.assertEquals(TypeAvatar.MC_FEMME_SEULE, avatarService.getTypeAvatar(elleAvecAndrogyne));

				final MenageCommun elleEtLui = (MenageCommun) tiersDAO.get(ids.elleEtLui);
				Assert.assertNotNull(elleEtLui);
				Assert.assertEquals(TypeAvatar.MC_MIXTE, avatarService.getTypeAvatar(elleEtLui));

				final MenageCommun elles = (MenageCommun) tiersDAO.get(ids.elles);
				Assert.assertNotNull(elles);
				Assert.assertEquals(TypeAvatar.MC_FEMME_FEMME, avatarService.getTypeAvatar(elles));

				final MenageCommun eux = (MenageCommun) tiersDAO.get(ids.eux);
				Assert.assertNotNull(eux);
				Assert.assertEquals(TypeAvatar.MC_HOMME_HOMME, avatarService.getTypeAvatar(eux));
			}
		});
	}
}
