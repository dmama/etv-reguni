package ch.vd.unireg.tiers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.common.pagination.ParamPagination;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeRapportEntreTiers;

@SuppressWarnings({"JavaDoc"})
public class RapportEntreTiersTest extends CoreDAOTest {

	private RapportEntreTiersDAO retDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		retDAO = getBean(RapportEntreTiersDAO.class, "rapportEntreTiersDAO");
	}

	/**
	 * Teste que la création d'un ensemble tiers-menagecommun-tiers est possible.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateEnsembleTiersMenageCommun() throws Exception {

		final class Numeros {

			Long hab1Num;
			Long hab2Num;
			Long mcNum;
		}

		Numeros numeros = doInNewTransaction(new TxCallback<Numeros>() {
			@Override
			public Numeros execute(TransactionStatus status) throws Exception {
				// CTB 1
				PersonnePhysique ctb1 = new PersonnePhysique(true);
				ctb1.setNumeroIndividu(12345L);

				// CTB 2
				PersonnePhysique ctb2 = new PersonnePhysique(true);
				ctb2.setNumeroIndividu(23456L);

				// Menage
				MenageCommun menage = new MenageCommun();

				ctb1 = (PersonnePhysique) tiersDAO.save(ctb1);
				ctb2 = (PersonnePhysique) tiersDAO.save(ctb2);
				menage = (MenageCommun) tiersDAO.save(menage);

				// Rattachement
				RapportEntreTiers rapport1 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb1, menage);
				RapportEntreTiers rapport2 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb2, menage);

				tiersDAO.save(rapport1);
				tiersDAO.save(rapport2);

				Numeros numeros = new Numeros();
				numeros.hab1Num = ctb1.getNumero();
				numeros.hab2Num = ctb2.getNumero();
				numeros.mcNum = menage.getNumero();
				return numeros;
			}
		});

		final Long hab1Num = numeros.hab1Num;
		final Long hab2Num = numeros.hab2Num;
		final Long mcNum = numeros.mcNum;

		// Nombre d'éléments stockés dans la base
		{
			Assert.assertEquals("Nombre de tiers incorrect", 3, tiersDAO.getCount(Tiers.class));
			Assert.assertEquals("Nombre de rapport-entre-tiers incorrect", 2, tiersDAO.getCount(RapportEntreTiers.class));
		}

		// Depuis le point de vue du ménage
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.get(mcNum);
			Assert.assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsSujet = menage.getRapportsSujet();
			Assert.assertTrue(rapportsSujet == null || rapportsSujet.isEmpty());

			final Set<RapportEntreTiers> rapportsObjet = menage.getRapportsObjet();
			Assert.assertNotNull(rapportsObjet);
			Assert.assertEquals(2, rapportsObjet.size());

			final RapportEntreTiers ret1 = (RapportEntreTiers) rapportsObjet.toArray()[0];
			final RapportEntreTiers ret2 = (RapportEntreTiers) rapportsObjet.toArray()[1];
			Assert.assertEquals(menage.getId(), ret1.getObjetId());
			Assert.assertEquals(menage.getId(), ret2.getObjetId());

			final PersonnePhysique habitant1 = (PersonnePhysique) tiersDAO.get(ret1.getSujetId());
			final PersonnePhysique habitant2 = (PersonnePhysique) tiersDAO.get(ret2.getSujetId());
			Assert.assertNotNull(habitant1);
			Assert.assertNotNull(habitant2);
			Assert.assertTrue(habitant1.getNumeroIndividu() == 12345L || habitant1.getNumeroIndividu() == 23456L);
			Assert.assertTrue(habitant2.getNumeroIndividu() == 12345L || habitant2.getNumeroIndividu() == 23456L);
		}

		// Depuis le point de vue de l'habitant n°1
		{
			final PersonnePhysique habitant1 = (PersonnePhysique) tiersDAO.get(hab1Num);
			Assert.assertNotNull(habitant1);
			Assert.assertTrue(habitant1.getNumeroIndividu() == 12345L);

			final MenageCommun menage = (MenageCommun) tiersDAO.get(mcNum);
			Assert.assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsObjet = habitant1.getRapportsObjet();
			Assert.assertTrue(rapportsObjet == null || rapportsObjet.isEmpty());

			final Set<RapportEntreTiers> rapportsSujet = habitant1.getRapportsSujet();
			Assert.assertNotNull(rapportsSujet);
			Assert.assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			Assert.assertNotNull(ret);
			Assert.assertEquals(habitant1.getId(), ret.getSujetId());
			Assert.assertEquals(menage.getId(), ret.getObjetId());
		}

		// Tests depuis le point de vue de l'habitant n°2
		{
			final PersonnePhysique habitant2 = (PersonnePhysique) tiersDAO.get(hab2Num);
			Assert.assertNotNull(habitant2);
			Assert.assertTrue(habitant2.getNumeroIndividu() == 23456L);

			final MenageCommun menage = (MenageCommun) tiersDAO.get(mcNum);
			Assert.assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsObjet = habitant2.getRapportsObjet();
			Assert.assertTrue(rapportsObjet == null || rapportsObjet.isEmpty());

			final Set<RapportEntreTiers> rapportsSujet = habitant2.getRapportsSujet();
			Assert.assertNotNull(rapportsSujet);
			Assert.assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			Assert.assertNotNull(ret);
			Assert.assertEquals(habitant2.getId(), ret.getSujetId());
			Assert.assertEquals(menage.getId(), ret.getObjetId());
		}
	}

	/**
	 * Teste que l'ajout d'une tutelle entre deux tiers existant est possible.
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateTutelleEntreTiersExistant() throws Exception {

		final class Numeros {

			Long tuteurNum;
			Long pupilleNum;
		}

		Numeros numeros = doInNewTransaction(new TxCallback<Numeros>() {
			@Override
			public Numeros execute(TransactionStatus status) throws Exception {
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(12345L);

				PersonnePhysique pupille = new PersonnePhysique(true);
				pupille.setNumeroIndividu(23456L);

				tuteur = (PersonnePhysique) tiersDAO.save(tuteur);
				pupille = (PersonnePhysique) tiersDAO.save(pupille);

				Numeros numeros = new Numeros();
				numeros.tuteurNum = tuteur.getNumero();
				numeros.pupilleNum = pupille.getNumero();
				return numeros;
			}
		});

		final Long tuteurNum = numeros.tuteurNum;
		final Long pupilleNum = numeros.pupilleNum;

		// Création d'une tutelle
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(tuteurNum);
				PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(pupilleNum);

				RapportEntreTiers tutelle = new Tutelle(RegDate.get(2006, 1, 12), null, pupille, tuteur, null);

				tutelle = tiersDAO.save(tutelle);
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			Assert.assertEquals("Nombre de tiers incorrect", 2, tiersDAO.getCount(Tiers.class));
			Assert.assertEquals("Nombre de rapport-entre-tiers incorrect", 1, tiersDAO.getCount(RapportEntreTiers.class));
		}

		// Depuis le point de vue du tuteur
		{
			final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(tuteurNum);
			Assert.assertNotNull(tuteur);
			Assert.assertTrue(tuteur.getNumeroIndividu() == 12345L);

			final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(pupilleNum);
			Assert.assertNotNull(pupille);
			Assert.assertTrue(pupille.getNumeroIndividu() == 23456L);

			final Set<RapportEntreTiers> rapportsSujet = tuteur.getRapportsSujet();
			Assert.assertTrue(rapportsSujet == null || rapportsSujet.isEmpty());

			final Set<RapportEntreTiers> rapportsObjet = tuteur.getRapportsObjet();
			Assert.assertNotNull(rapportsObjet);
			Assert.assertEquals(1, rapportsObjet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsObjet.toArray()[0];
			Assert.assertNotNull(ret);
			Assert.assertEquals(pupille.getId(), ret.getSujetId());
			Assert.assertEquals(tuteur.getId(), ret.getObjetId());
		}

		// Depuis le point de vue de la pupille
		{
			final PersonnePhysique tuteur = (PersonnePhysique) tiersDAO.get(tuteurNum);
			Assert.assertNotNull(tuteur);
			Assert.assertTrue(tuteur.getNumeroIndividu() == 12345L);

			final PersonnePhysique pupille = (PersonnePhysique) tiersDAO.get(pupilleNum);
			Assert.assertNotNull(pupille);
			Assert.assertTrue(pupille.getNumeroIndividu() == 23456L);

			final Set<RapportEntreTiers> rapportsObjet = pupille.getRapportsObjet();
			Assert.assertTrue(rapportsObjet == null || rapportsObjet.isEmpty());

			final Set<RapportEntreTiers> rapportsSujet = pupille.getRapportsSujet();
			Assert.assertNotNull(rapportsSujet);
			Assert.assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			Assert.assertNotNull(ret);
			Assert.assertEquals(TypeRapportEntreTiers.TUTELLE, ret.getType());
			Assert.assertEquals(pupille.getId(), ret.getSujetId());
			Assert.assertEquals(tuteur.getId(), ret.getObjetId());
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testIsValid() {

		final RapportEntreTiers rapport = new AppartenanceMenage();
		rapport.setDateDebut(RegDate.get(2000, 1, 1));
		rapport.setDateFin(RegDate.get(2009, 12, 31));

		rapport.setAnnule(false);
		Assert.assertTrue(rapport.isValidAt(RegDate.get(2004, 1, 1)));
		Assert.assertFalse(rapport.isValidAt(RegDate.get(1990, 1, 1)));
		Assert.assertFalse(rapport.isValidAt(RegDate.get(2060, 1, 1)));

		rapport.setAnnule(true);
		Assert.assertFalse(rapport.isValidAt(RegDate.get(2004, 1, 1)));
		Assert.assertFalse(rapport.isValidAt(RegDate.get(1990, 1, 1)));
		Assert.assertFalse(rapport.isValidAt(RegDate.get(2060, 1, 1)));
	}

	@Test
	public void testSortingOrderOnOtherPartysNumber() throws Exception {

		final class Ids {
			long m;
			long mc;
			long fiston;
			long gdpa;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransaction(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addNonHabitant("Francis", "D'aquitaine", date(1953, 7, 31), Sexe.MASCULIN);
				final PersonnePhysique fiston = addNonHabitant("Kevin", "D'aquitaine", date(1980, 10, 25), Sexe.MASCULIN);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, null, date(1973, 5, 1), null);
				final MenageCommun mc = couple.getMenage();
				final PersonnePhysique gdpa = addNonHabitant("Alphonse", "D'aquitaine", date(1920, 8, 4), Sexe.MASCULIN);
				addParente(m, gdpa, date(1953, 7, 31), null);
				addParente(fiston, m, date(1980, 10, 25), null);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mc = mc.getNumero();
				ids.fiston = fiston.getNumero();
				ids.gdpa = gdpa.getNumero();
				return ids;
			}
		});

		final Set<RapportEntreTiersKey> allKeys = new HashSet<>(RapportEntreTiersKey.maxCardinality());
		for (TypeRapportEntreTiers type : TypeRapportEntreTiers.values()) {
			for (RapportEntreTiersKey.Source source : RapportEntreTiersKey.Source.values()) {
				allKeys.add(new RapportEntreTiersKey(type, source));
			}
		}

		// tri des relations par "tiersId"
		doInNewReadOnlyTransaction(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final ParamPagination paginationAsc = new ParamPagination(1, 25, "tiersId", true);
				final List<RapportEntreTiers> asc = retDAO.findBySujetAndObjet(ids.m, true, allKeys, paginationAsc);
				Assert.assertEquals(3, asc.size());
				{
					final RapportEntreTiers ret = asc.get(0);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.PARENTE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getObjetId());
					Assert.assertEquals((Long) ids.fiston, ret.getSujetId());
				}
				{
					final RapportEntreTiers ret = asc.get(1);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.APPARTENANCE_MENAGE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getSujetId());
					Assert.assertEquals((Long) ids.mc, ret.getObjetId());
				}
				{
					final RapportEntreTiers ret = asc.get(2);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.PARENTE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getSujetId());
					Assert.assertEquals((Long) ids.gdpa, ret.getObjetId());
				}

				final ParamPagination paginationDesc = new ParamPagination(1, 25, "tiersId", false);
				final List<RapportEntreTiers> desc = retDAO.findBySujetAndObjet(ids.m, true, allKeys, paginationDesc);
				Assert.assertEquals(3, desc.size());
				{
					final RapportEntreTiers ret = desc.get(2);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.PARENTE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getObjetId());
					Assert.assertEquals((Long) ids.fiston, ret.getSujetId());
				}
				{
					final RapportEntreTiers ret = desc.get(1);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.APPARTENANCE_MENAGE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getSujetId());
					Assert.assertEquals((Long) ids.mc, ret.getObjetId());
				}
				{
					final RapportEntreTiers ret = desc.get(0);
					Assert.assertNotNull(ret);
					Assert.assertEquals(TypeRapportEntreTiers.PARENTE, ret.getType());
					Assert.assertEquals((Long) ids.m, ret.getSujetId());
					Assert.assertEquals((Long) ids.gdpa, ret.getObjetId());
				}
			}
		});
	}
}
