package ch.vd.uniregctb.metier;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.registre.civil.model.EnumAttributeIndividu;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockNationalite;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.Tiers.ForsParType;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseTiers;
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
	public void testMarieHorsCanton() throws Exception {

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
	public void testSepareHorsCanton() throws Exception {

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
				addAdresseSuisse(fabrice, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);
				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseSuisse(georgette, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);
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
	 * Teste que la séparation/divorce d'un couple hors-Suisse ouvre bien les fors principaux hors-Suisse sur le contribuables séparés
	 */
	@Test
	public void testSepareHorsSuisse() throws Exception {

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-Suisse
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				fabrice.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseEtrangere(fabrice, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, "Nizzaallee", "52072 Aachen", MockPays.Allemagne);
				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseEtrangere(georgette, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, "Nizzaallee", "52072 Aachen", MockPays.Allemagne);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, date(2001, 1, 6), null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, date(2001, 1, 6), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
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

		// Vérifie que les fors principals ouvert sur les contribuables sont bien hors-Suisse
		final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
		assertNotNull(fabrice);
		final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
		assertNotNull(georgette);

		{
			final ForsParType fors = fabrice.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principaux.size());
			assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.PAYS_HS,
					MockPays.Allemagne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
		}
		{
			final ForsParType fors = georgette.getForsParType(true);
			assertNotNull(fors);
			assertEquals(1, fors.principaux.size());
			assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.PAYS_HS,
					MockPays.Allemagne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
		}
	}

	/**
	 * [UNIREG-1121] Teste que le décès d'un contribuable marié hors-canton ouvre bien un for principal hors-canton sur le conjoint
	 * survivant
	 */
	@Test
	public void testDecesHorsCanton() throws Exception {

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
				addAdresseSuisse(fabrice, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);

				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseSuisse(georgette, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);

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
	 * Teste que le décès d'un contribuable marié hors-Suisse ouvre bien un for principal hors-Suisse sur le conjoint survivant
	 */
	@Test
	public void testDecesHorsSuisse() throws Exception {

		class Ids {
			long fabrice;
			long georgette;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-Suisse
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique fabrice = addNonHabitant("Fabrice", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);
				fabrice.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseEtrangere(fabrice, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, "Nizzaallee", "52072 Aachen", MockPays.Allemagne);

				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseEtrangere(georgette, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, "Nizzaallee", "52072 Aachen", MockPays.Allemagne);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, date(2001, 1, 6), null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();

				addForPrincipal(menage, date(2001, 1, 6), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockPays.Allemagne);
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

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-Suisse
		final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
		assertNotNull(georgette);

		final ForsParType fors = georgette.getForsParType(true);
		assertNotNull(fors);
		assertEquals(1, fors.principaux.size());
		assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.PAYS_HS, MockPays.Allemagne.getNoOFS(),
				MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principaux.get(0));
	}

	/**
	 * [UNIREG-1121] Teste que le veuvage d'un contribuable marié hors-canton ouvre bien un for principal hors-canton sur lui-même
	 */
	@Test
	public void testVeuvageHorsCanton() throws Exception {

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
				addAdresseSuisse(fabrice, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);

				final PersonnePhysique georgette = addNonHabitant("Georgette", "Dunant", date(1975, 1, 1), Sexe.FEMININ);
				georgette.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
				addAdresseSuisse(georgette, TypeAdresseTiers.DOMICILE, date(2001, 1, 6), null, MockRue.Neuchatel.RueDesBeauxArts);

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

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes
	 * crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseDomicileOuCourrierHorsCanton() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2008, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable à Lausanne
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique georgette = addHabitant(noIndGeorgette);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return null;
			}
		});

		// Sépare fiscalement les époux après avoir changé les adresses
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndFabrice, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

					// domicile passe à Bex -> le for devra s'ouvrir là
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
				}

				 // changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndGeorgette, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

					// on ne connait que l'adresse courrier sur Genève (= prise par défaut pour l'adresse de domicile)
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateSeparation, null));
				}

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, true, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette à Lausanne

		// For fermé sur le couple
		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
			assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Fabrice : à Bex, car son adresse de domicile est là-bas
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
			assertNotNull(fabrice);

			final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateSeparation, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
			assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Georgette : à Genève, car l'adresse courrier est utilisée en lieu et place de l'adresse de domicile, inconnue
		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
			assertNotNull(georgette);

			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateSeparation, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
			assertEquals(MockCommune.Geneve.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
		}
	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes
	 * crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseDomicileOuCourrierHorsSuisse() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2008, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable à Lausanne
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique georgette = addHabitant(noIndGeorgette);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return null;
			}
		});

		// Sépare fiscalement les époux après avoir changé les adresses
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndFabrice, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

					// domicile passe à Bex -> le for devra s'ouvrir là
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
				}

				 // changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndGeorgette, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

					// on ne connait que l'adresse courrier à Paris (= prise par défaut pour l'adresse de domicile)
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, "5 Avenue des Champs-Elysées", null, "75017 Paris", MockPays.France, dateSeparation, null));
				}

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				try {
					metierService.separe(mc, dateSeparation, "test", null, true, null);
					fail("La séparation aurait dû partir en erreur puisque l'on passe pour Georgette d'un couple vaudois à un for hors-Suisse");
				}
				catch (EvenementCivilHandlerException e) {
					final String attendu = String.format("D'après son adresse de domicile, on devrait ouvrir un for hors-Suisse pour le contribuable %s alors que le for du ménage %s était vaudois",
														FormatNumeroHelper.numeroCTBToDisplay(ids.georgette), FormatNumeroHelper.numeroCTBToDisplay(ids.menage));
					assertEquals(attendu, e.getMessage());
				}
				return null;
			}
		});

	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes
	 * crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseInconnue() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2008, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				// pas d'adresse connue sur madame (oubli ?)

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable à Lausanne
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique georgette = addHabitant(noIndGeorgette);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return null;
			}
		});

		// Sépare fiscalement les époux après avoir changé les adresses
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndFabrice, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

					// domicile passe à Bex -> le for devra s'ouvrir là
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
					individu.getAdresses().add(MockServiceCivil.newAdresse(EnumTypeAdresse.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
				}

				 // vérification de l'absence d'adresse sur Georgette
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndGeorgette, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(0, individu.getAdresses().size());
				}

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, true, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette sans for

		// For fermé sur le couple
		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
			assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Fabrice : à Bex, car son adresse de domicile est là-bas
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
			assertNotNull(fabrice);

			final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateSeparation, ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
			assertEquals(MockCommune.Bex.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Georgette : aucun, car son adresse de domicile est inconnue
		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
			assertNotNull(georgette);

			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNull(ffp);
		}
	}

	/**
	 * Teste que la clôture d'un ménage par décès pour lequel les adresses des membres du couple sont différentes
	 * crée bien les fors au bon endroit
	 */
	@Test
	public void testDecesAdresseDomicileHorsCanton() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateDeces = date(2008, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateMariage.addMonths(10));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateMariage.addMonths(10).getOneDayAfter(), null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable à Lausanne
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);

				final PersonnePhysique georgette = addNonHabitant("Dunand", "Georgette", naissanceGeorgette, Sexe.FEMININ);
				georgette.setNumeroIndividu(noIndGeorgette);
				georgette.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return null;
			}
		});

		// Fabrice passe l'arme à gauche
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndFabrice, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateDeces);

					individu.setDateDeces(dateDeces);
				}

				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, dateDeces, "test", null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les contribuables : Fabrice est mort, Georgette est à Genève

		// For fermé sur le couple
		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
			assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(dateDeces, ffp.getDateFin());
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Fabrice : aucun, car il est mort
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
			assertNotNull(fabrice);

			final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
			assertNull(ffp);
		}

		// For ouvert sur Georgette : passe hors-canton à Genève
		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
			assertNotNull(georgette);

			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateDeces.getOneDayAfter(), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
			assertEquals(MockCommune.Geneve.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
		}
	}

	/**
	 * Teste que la clôture d'un ménage par décès pour lequel les adresses des membres du couple sont différentes
	 * crée bien les fors au bon endroit
	 */
	@Test
	public void testDecesAdresseDomicileHorsSuisse() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateDeces = date(2008, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateMariage.addMonths(10));
				addAdresse(georgette, EnumTypeAdresse.PRINCIPALE, "Avenue des Champs-Elysées", "5", null, null, "75017 Paris", MockPays.France, dateMariage.addMonths(10).getOneDayAfter(), null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable à Lausanne
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);

				final PersonnePhysique georgette = addNonHabitant("Dunand", "Georgette", naissanceGeorgette, Sexe.FEMININ);
				georgette.setNumeroIndividu(noIndGeorgette);
				georgette.setNumeroOfsNationalite(ServiceInfrastructureService.noOfsSuisse);

				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);
				return null;
			}
		});

		// Fabrice passe l'arme à gauche
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				{
					final MockIndividu individu = (MockIndividu) serviceCivil.getIndividu(noIndFabrice, null, EnumAttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(1, individu.getAdresses().size());

					final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
					assertNull(adresse.getDateFin());
					assertEquals(dateMariage, adresse.getDateDebut());
					adresse.setDateFinValidite(dateDeces);

					individu.setDateDeces(dateDeces);
				}

				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, dateDeces, "test", null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les contribuables : Fabrice est mort, Georgette est à Genève

		// For fermé sur le couple
		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
			assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateMariage, ffp.getDateDebut());
			assertEquals(dateDeces, ffp.getDateFin());
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
			assertEquals(MockCommune.Lausanne.getNoOFSEtendu(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		}

		// For ouvert sur Fabrice : aucun, car il est mort
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
			assertNotNull(fabrice);

			final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
			assertNull(ffp);
		}

		// For ouvert sur Georgette : passe hors-canton à Genève
		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
			assertNotNull(georgette);

			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertNotNull(ffp);
			assertEquals(dateDeces.getOneDayAfter(), ffp.getDateDebut());
			assertNull(ffp.getDateFin());
			assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifOuverture());
			assertEquals(MockPays.France.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
			assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
		}
	}

	/**
	 * C'est le cas du cas JIRA UNIREG-1623
	 */
	@Test
	public void testVeuvageCivilSurCoupleSepareDepuisTellementLongtempsQueLeCoupleEstAbsentDUnireg() throws Exception {

		final long noIndFabrice = 12541L;
		final RegDate dateNaissance = date(1970, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2000, 5, 13);
		final RegDate dateVeuvage = date(2009, 9, 16);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, dateNaissance, "Dutrou", "Fabrice", true);
				addEtatCivil(fabrice, dateNaissance, EnumTypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, EnumTypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, EnumTypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, EnumTypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				addForPrincipal(fabrice, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ppId);
			final ValidationResults resultatValidation = metierService.validateVeuvage(fabrice, dateVeuvage);
			Assert.assertNotNull(resultatValidation);
			Assert.assertEquals(0, resultatValidation.errorsCount());

			metierService.veuvage(fabrice, dateVeuvage, "Test pour cas UNIREG-1623", null);

			final Set<ForFiscal> ff = fabrice.getForsFiscaux();
			Assert.assertEquals(1, ff.size());

			final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff.iterator().next();
			Assert.assertNotNull(ffp);
			Assert.assertNull(ffp.getAnnulationDate());
			Assert.assertNull(ffp.getDateFin());
		}
	}

	/**
	 * C'est un cas dérivé du cas JIRA UNIREG-1623
	 */
	@Test
	public void testVeuvageSurSepare() throws Exception {

		final long noIndFabrice = 12541L;
		final RegDate dateNaissance = date(1970, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2000, 5, 13);
		final RegDate dateVeuvage = date(2009, 9, 16);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, dateNaissance, "Dutrou", "Fabrice", true);
				addEtatCivil(fabrice, dateNaissance, EnumTypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, EnumTypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, EnumTypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, EnumTypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique patricia = addNonHabitant("Patricia", "Dutrou", date(1969, 4, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fabrice, patricia, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun menage = couple.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				addForPrincipal(fabrice, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ppId);
			final ValidationResults resultatValidation = metierService.validateVeuvage(fabrice, dateVeuvage);
			Assert.assertNotNull(resultatValidation);
			Assert.assertEquals(0, resultatValidation.errorsCount());

			metierService.veuvage(fabrice, dateVeuvage, null, null);

			final Set<ForFiscal> ff = fabrice.getForsFiscaux();
			Assert.assertEquals(1, ff.size());

			final ForFiscalPrincipal ffp = (ForFiscalPrincipal) ff.iterator().next();
			Assert.assertNotNull(ffp);
			Assert.assertNull(ffp.getAnnulationDate());
			Assert.assertNull(ffp.getDateFin());
		}
	}

	/**
	 * C'est un cas dérivé du cas JIRA UNIREG-1623
	 */
	@Test
	public void testVeuvageSurSepareSansFor() throws Exception {

		final long noIndFabrice = 12541L;
		final RegDate dateNaissance = date(1970, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2000, 5, 13);
		final RegDate dateVeuvage = date(2009, 9, 16);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, dateNaissance, "Dutrou", "Fabrice", true);
				addEtatCivil(fabrice, dateNaissance, EnumTypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, EnumTypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, EnumTypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, EnumTypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique patricia = addNonHabitant("Patricia", "Dutrou", date(1969, 4, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fabrice, patricia, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun menage = couple.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ppId);
			final ValidationResults resultatValidation = metierService.validateVeuvage(fabrice, dateVeuvage);
			Assert.assertNotNull(resultatValidation);
			Assert.assertEquals(1, resultatValidation.errorsCount());

			final String erreur = resultatValidation.getErrors().get(0);
			Assert.assertEquals("L'individu veuf n'a ni couple connu ni for valide à la date de veuvage : problème d'assujettissement ?", erreur);
		}
	}
}
