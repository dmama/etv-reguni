package ch.vd.uniregctb.metier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Classe de test du métier service.
 * <p>
 * <b>Note:</b> la majeure partie des tests du métier service sont fait au travers des test Norentes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class MetiersServiceTest extends BusinessTest {

	private TiersDAO tiersDAO;
	private MetierService metierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		metierService = getBean(MetierService.class, "metierService");
	}

	/**
	 * [UNIREG-1121] Teste que le mariage d'un contribuable hors-canton ouvre bien un for principal hors-canton sur le ménage commun
	 */
	@Test
	public void testMarie() throws Exception {

		class Ids {
			long fabrice;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un contribuable parti hors-canton en 2004 et qui a débuté une activité indépendante en 2005
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				addForPrincipal(fabrice, date(2001, 1, 6), MotifFor.ARRIVEE_HC, date(2004, 8, 15), MotifFor.DEPART_HC, MockCommune.Cossonay);
				addForPrincipal(fabrice, date(2004, 8, 16), MotifFor.DEPART_HC, MockCommune.Neuchatel);
				addForSecondaire(fabrice, date(2005, 1, 1), MotifFor.DEBUT_EXPLOITATION, MockCommune.Renens.getNoOFS(),
						MotifRattachement.ACTIVITE_INDEPENDANTE);
				ids.fabrice = fabrice.getNumero();
				return null;
			}
		});

		// Marie le contribuable (en mode marié-seul) le 23.11.2008
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				final MenageCommun menageCommun = metierService.marie(date(2008, 11, 23), fabrice, null, "test", EtatCivil.MARIE, true, null);
				assertNotNull(menageCommun);
				ids.menage = menageCommun.getNumero();
				return null;
			}
		});

		// Vérifie que le for principal ouvert sur le ménage est bien hors-canton
		final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
		assertNotNull(menage);

		final ForsParType fors = menage.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertEquals(1, fors.secondaires.size());
		assertForPrincipal(date(2008, 11, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, TypeAutoriteFiscale.COMMUNE_HC,
				MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
		assertForSecondaire(date(2008, 11, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE,
				fors.secondaires.get(0));
	}

	/**
	 * [UNIREG-1121] Teste que la séparation/divorce d'un couple hors-canton ouvre bien les fors principaux hors-canton sur le contribuables
	 * séparés
	 */
	@Test
	public void testSepare() throws Exception {

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-canton
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				fabrice.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, date(2001, 1, 6), null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, date(2001, 1, 6), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
				return null;
			}
		});

		// Sépare le contribuable le 23.11.2008
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(menage);
				metierService.separe(menage, date(2008, 11, 23), "test", EtatCivil.MARIE, true, null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur les contribuables sont bien hors-canton
		final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
		assertNotNull(fabrice);
		final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
		assertNotNull(georgette);

		{
			final ForsParType fors = fabrice.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principaux.size());
			assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.COMMUNE_HC,
					MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
		}
		{
			final ForsParType fors = georgette.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principaux.size());
			assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.COMMUNE_HC,
					MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
		}
	}

	/**
	 * [UNIREG-1121] Teste que le décès d'un contribuable marié hors-canton ouvre bien un for principal hors-canton sur le conjoint
	 * survivant
	 */
	@Test
	public void testDeces() throws Exception {

		class Ids {
			long fabrice;
			long georgette;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-canton
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				fabrice.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, date(2001, 1, 6), null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();

				addForPrincipal(menage, date(2001, 1, 6), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
				return null;
			}
		});

		// Décède le contribuable le 23.11.2008
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, date(2008, 11, 23), "test", null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-canton
		final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
		assertNotNull(georgette);

		final ForsParType fors = georgette.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
	}

	/**
	 * [UNIREG-1121] Teste que le veuvage d'un contribuable marié hors-canton ouvre bien un for principal hors-canton sur lui-même
	 */
	@Test
	public void testVeuvage() throws Exception {

		class Ids {
			long georgette;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-canton
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				fabrice.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, date(2001, 1, 6), null);
				final MenageCommun menage = ensemble.getMenage();

				ids.georgette = georgette.getNumero();

				addForPrincipal(menage, date(2001, 1, 6), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Neuchatel);
				return null;
			}
		});

		// Décède le contribuable le 23.11.2008
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);
				metierService.veuvage(georgette, date(2008, 11, 23), "test", null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-canton
		final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
		assertNotNull(georgette);

		final ForsParType fors = georgette.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
	}
}
