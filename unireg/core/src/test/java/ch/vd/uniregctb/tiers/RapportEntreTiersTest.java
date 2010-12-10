package ch.vd.uniregctb.tiers;

import java.util.Set;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
public class RapportEntreTiersTest extends CoreDAOTest {

	// private static final Logger LOGGER = Logger.getLogger(RapportEntreTiersTest.class);

	private TiersDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	/**
	 * Teste que la création d'un ensemble tiers-menagecommun-tiers est possible.
	 */
	@Test
	public void testCreateEnsembleTiersMenageCommun() throws Exception {

		final class Numeros {

			Long hab1Num;
			Long hab2Num;
			Long mcNum;
		}

		Numeros numeros = (Numeros)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// CTB 1
				PersonnePhysique ctb1 = new PersonnePhysique(true);
				ctb1.setNumeroIndividu(12345L);

				// CTB 2
				PersonnePhysique ctb2 = new PersonnePhysique(true);
				ctb2.setNumeroIndividu(23456L);

				// Menage
				MenageCommun menage = new MenageCommun();

				ctb1 = (PersonnePhysique) dao.save(ctb1);
				ctb2 = (PersonnePhysique) dao.save(ctb2);
				menage = (MenageCommun) dao.save(menage);

				// Rattachement
				RapportEntreTiers rapport1 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb1, menage);
				RapportEntreTiers rapport2 = new AppartenanceMenage(RegDate.get(2002, 2, 1), null, ctb2, menage);

				dao.save(rapport1);
				dao.save(rapport2);

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
			assertEquals("Nombre de tiers incorrect", 3, dao.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 2, dao.getCount(RapportEntreTiers.class));
		}

		// Depuis le point de vue du ménage
		{
			final MenageCommun menage = (MenageCommun) dao.get(mcNum);
			assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsSujet = menage.getRapportsSujet();
			assertTrue(rapportsSujet == null || rapportsSujet.size() == 0);

			final Set<RapportEntreTiers> rapportsObjet = menage.getRapportsObjet();
			assertNotNull(rapportsObjet);
			assertEquals(2, rapportsObjet.size());

			final RapportEntreTiers ret1 = (RapportEntreTiers) rapportsObjet.toArray()[0];
			final RapportEntreTiers ret2 = (RapportEntreTiers) rapportsObjet.toArray()[1];
			assertEquals(menage.getId(), ret1.getObjetId());
			assertEquals(menage.getId(), ret2.getObjetId());

			final PersonnePhysique habitant1 = (PersonnePhysique) dao.get(ret1.getSujetId());
			final PersonnePhysique habitant2 = (PersonnePhysique) dao.get(ret2.getSujetId());
			assertNotNull(habitant1);
			assertNotNull(habitant2);
			assertTrue(habitant1.getNumeroIndividu() == 12345L || habitant1.getNumeroIndividu() == 23456L);
			assertTrue(habitant2.getNumeroIndividu() == 12345L || habitant2.getNumeroIndividu() == 23456L);
		}

		// Depuis le point de vue de l'habitant n°1
		{
			final PersonnePhysique habitant1 = (PersonnePhysique) dao.get(hab1Num);
			assertNotNull(habitant1);
			assertTrue(habitant1.getNumeroIndividu() == 12345L);

			final MenageCommun menage = (MenageCommun) dao.get(mcNum);
			assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsObjet = habitant1.getRapportsObjet();
			assertTrue(rapportsObjet == null || rapportsObjet.size() == 0);

			final Set<RapportEntreTiers> rapportsSujet = habitant1.getRapportsSujet();
			assertNotNull(rapportsSujet);
			assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			assertNotNull(ret);
			assertEquals(habitant1.getId(), ret.getSujetId());
			assertEquals(menage.getId(), ret.getObjetId());
		}

		// Tests depuis le point de vue de l'habitant n°2
		{
			final PersonnePhysique habitant2 = (PersonnePhysique) dao.get(hab2Num);
			assertNotNull(habitant2);
			assertTrue(habitant2.getNumeroIndividu() == 23456L);

			final MenageCommun menage = (MenageCommun) dao.get(mcNum);
			assertNotNull(menage);

			final Set<RapportEntreTiers> rapportsObjet = habitant2.getRapportsObjet();
			assertTrue(rapportsObjet == null || rapportsObjet.size() == 0);

			final Set<RapportEntreTiers> rapportsSujet = habitant2.getRapportsSujet();
			assertNotNull(rapportsSujet);
			assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			assertNotNull(ret);
			assertEquals(habitant2.getId(), ret.getSujetId());
			assertEquals(menage.getId(), ret.getObjetId());
		}
	}

	/**
	 * Teste que l'ajout d'une tutelle entre deux tiers existant est possible.
	 */
	@Test
	public void testCreateTutelleEntreTiersExistant() throws Exception {

		final class Numeros {

			Long tuteurNum;
			Long pupilleNum;
		}

		Numeros numeros = (Numeros)doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique tuteur = new PersonnePhysique(true);
				tuteur.setNumeroIndividu(12345L);

				PersonnePhysique pupille = new PersonnePhysique(true);
				pupille.setNumeroIndividu(23456L);

				tuteur = (PersonnePhysique) dao.save(tuteur);
				pupille = (PersonnePhysique) dao.save(pupille);

				Numeros numeros = new Numeros();
				numeros.tuteurNum = tuteur.getNumero();
				numeros.pupilleNum = pupille.getNumero();
				return numeros;
			}
		});

		final Long tuteurNum = numeros.tuteurNum;
		final Long pupilleNum = numeros.pupilleNum;

		// Création d'une tutelle
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique tuteur = (PersonnePhysique) dao.get(tuteurNum);
				PersonnePhysique pupille = (PersonnePhysique) dao.get(pupilleNum);

				RapportEntreTiers tutelle = new Tutelle(RegDate.get(2006, 1, 12), null, pupille, tuteur, null);

				tutelle = dao.save(tutelle);
				return null;
			}
		});

		// Nombre d'éléments stockés dans la base
		{
			assertEquals("Nombre de tiers incorrect", 2, dao.getCount(Tiers.class));
			assertEquals("Nombre de rapport-entre-tiers incorrect", 1, dao.getCount(RapportEntreTiers.class));
		}

		// Depuis le point de vue du tuteur
		{
			final PersonnePhysique tuteur = (PersonnePhysique) dao.get(tuteurNum);
			assertNotNull(tuteur);
			assertTrue(tuteur.getNumeroIndividu() == 12345L);

			final PersonnePhysique pupille = (PersonnePhysique) dao.get(pupilleNum);
			assertNotNull(pupille);
			assertTrue(pupille.getNumeroIndividu() == 23456L);

			final Set<RapportEntreTiers> rapportsSujet = tuteur.getRapportsSujet();
			assertTrue(rapportsSujet == null || rapportsSujet.size() == 0);

			final Set<RapportEntreTiers> rapportsObjet = tuteur.getRapportsObjet();
			assertNotNull(rapportsObjet);
			assertEquals(1, rapportsObjet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsObjet.toArray()[0];
			assertNotNull(ret);
			assertEquals(pupille.getId(), ret.getSujetId());
			assertEquals(tuteur.getId(), ret.getObjetId());
		}

		// Depuis le point de vue de la pupille
		{
			final PersonnePhysique tuteur = (PersonnePhysique) dao.get(tuteurNum);
			assertNotNull(tuteur);
			assertTrue(tuteur.getNumeroIndividu() == 12345L);

			final PersonnePhysique pupille = (PersonnePhysique) dao.get(pupilleNum);
			assertNotNull(pupille);
			assertTrue(pupille.getNumeroIndividu() == 23456L);

			final Set<RapportEntreTiers> rapportsObjet = pupille.getRapportsObjet();
			assertTrue(rapportsObjet == null || rapportsObjet.size() == 0);

			final Set<RapportEntreTiers> rapportsSujet = pupille.getRapportsSujet();
			assertNotNull(rapportsSujet);
			assertEquals(1, rapportsSujet.size());

			final RapportEntreTiers ret = (RapportEntreTiers) rapportsSujet.toArray()[0];
			assertNotNull(ret);
			assertEquals(TypeRapportEntreTiers.TUTELLE, ret.getType());
			assertEquals(pupille.getId(), ret.getSujetId());
			assertEquals(tuteur.getId(), ret.getObjetId());
		}
	}

	@Test
	public void testIsValid() {

		final RapportEntreTiers rapport = new AppartenanceMenage();
		rapport.setDateDebut(RegDate.get(2000, 1, 1));
		rapport.setDateFin(RegDate.get(2009, 12, 31));

		rapport.setAnnule(false);
		assertTrue(rapport.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(rapport.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(rapport.isValidAt(RegDate.get(2060, 1, 1)));

		rapport.setAnnule(true);
		assertFalse(rapport.isValidAt(RegDate.get(2004, 1, 1)));
		assertFalse(rapport.isValidAt(RegDate.get(1990, 1, 1)));
		assertFalse(rapport.isValidAt(RegDate.get(2060, 1, 1)));
	}
}
