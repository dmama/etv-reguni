package ch.vd.uniregctb.metier;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.interfaces.civil.data.AttributeIndividu;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockServiceCivil;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockNationalite;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockBatiment;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.etiquette.ActionAutoEtiquette;
import ch.vd.uniregctb.etiquette.CorrectionSurDate;
import ch.vd.uniregctb.etiquette.Decalage;
import ch.vd.uniregctb.etiquette.DecalageAvecCorrection;
import ch.vd.uniregctb.etiquette.Etiquette;
import ch.vd.uniregctb.etiquette.EtiquetteDAO;
import ch.vd.uniregctb.etiquette.EtiquetteTiers;
import ch.vd.uniregctb.etiquette.UniteDecalageDate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.CoordonneesFinancieres;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TarifImpotSource;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;
import ch.vd.uniregctb.type.TypeTiersEtiquette;
import ch.vd.uniregctb.validation.fors.ForFiscalValidator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Classe de test du métier service.
 * <p/>
 * <b>Note:</b> la majeure partie des tests du métier service sont fait au travers des test Norentes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SuppressWarnings({"JavaDoc"})
public class MetiersServiceTest extends BusinessTest {

	private TiersDAO tiersDAO;
	private EtiquetteDAO etiquetteDAO;
	private MetierService metierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		etiquetteDAO = getBean(EtiquetteDAO.class, "etiquetteDAO");
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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				final MenageCommun menageCommun = metierService.marie(date(2008, 11, 23), fabrice, null, "test", EtatCivil.MARIE, null);
				assertNotNull(menageCommun);
				ids.menage = menageCommun.getNumero();
				return null;
			}
		});

		// Vérifie que le for principal ouvert sur le ménage est bien hors-canton
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(menage);

				final ForsParType fors = menage.getForsParType(true);
				assertNotNull(fors);
				assertEquals(1, fors.principauxPP.size());
				assertEquals(0, fors.principauxPM.size());
				assertEquals(1, fors.secondaires.size());
				assertForPrincipal(date(2008, 11, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, TypeAutoriteFiscale.COMMUNE_HC,
				                   MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
				assertForSecondaire(date(2008, 11, 23), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
				                    TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MockCommune.Renens.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE,
				                    fors.secondaires.get(0));
			}
		});
	}

	/**
	 * [UNIREG-1121] Teste que la séparation/divorce d'un couple hors-canton ouvre bien les fors principaux hors-canton sur le contribuables séparés
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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(menage);
				metierService.separe(menage, date(2008, 11, 23), "test", EtatCivil.MARIE, null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur les contribuables sont bien hors-canton
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);

				{
					final ForsParType fors = fabrice.getForsParType(true);
					assertNotNull(fors);
					assertEquals(1, fors.principauxPP.size());
					assertEquals(0, fors.principauxPM.size());
					assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.COMMUNE_HC,
					                   MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
				}
				{
					final ForsParType fors = georgette.getForsParType(true);
					assertNotNull(fors);
					assertEquals(1, fors.principauxPP.size());
					assertEquals(0, fors.principauxPM.size());
					assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.COMMUNE_HC,
					                   MockCommune.Neuchatel.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
				}

			}
		});
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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun menage = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(menage);
				metierService.separe(menage, date(2008, 11, 23), "test", EtatCivil.MARIE, null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur les contribuables sont bien hors-Suisse
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);

				{
					final ForsParType fors = fabrice.getForsParType(true);
					assertNotNull(fors);
					assertEquals(1, fors.principauxPP.size());
					assertEquals(0, fors.principauxPM.size());
					assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.PAYS_HS,
					                   MockPays.Allemagne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
				}
				{
					final ForsParType fors = georgette.getForsParType(true);
					assertNotNull(fors);
					assertEquals(1, fors.principauxPP.size());
					assertEquals(0, fors.principauxPM.size());
					assertForPrincipal(date(2008, 11, 23), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, TypeAutoriteFiscale.PAYS_HS,
					                   MockPays.Allemagne.getNoOFS(), MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
				}
			}
		});
	}

	/**
	 * UNIREG-2771 La fusion doit être empéchée s'il existe au moins un for ou une Di non annule
	 */
	@Test
	public void testFusionMenagePresenceForOuDiNonAnnulee() throws Exception {

		final RegDate dateMariageAlfredo = RegDate.get(2003, 1, 6);
		final RegDate dateMariageArmando = RegDate.get(2003, 7, 1);
		class Ids {
			private long noMenageAlfredo;
			private long noMenageArmando;

		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-Suisse
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Alfredo
				final PersonnePhysique alfredo = addNonHabitant("Alfredo", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);

				// ménage Alfredo
				{
					MenageCommun menage = new MenageCommun();
					menage = (MenageCommun) tiersDAO.save(menage);
					ids.noMenageAlfredo = menage.getNumero();
					tiersService.addTiersToCouple(menage, alfredo, dateMariageAlfredo, null);

					addForPrincipal(menage, dateMariageAlfredo, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2009, 3, 1), MotifFor.INDETERMINE, MockCommune.Lausanne,
					                MotifRattachement.DOMICILE);
					menage.setBlocageRemboursementAutomatique(false);
				}

				// Armando
				final PersonnePhysique armando = addNonHabitant("Armando", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);

				// ménage Armando
				{
					MenageCommun menage = new MenageCommun();
					menage = (MenageCommun) tiersDAO.save(menage);
					ids.noMenageArmando = menage.getNumero();
					tiersService.addTiersToCouple(menage, armando, dateMariageArmando, null);

					addForPrincipal(menage, dateMariageArmando, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, date(2009, 3, 1), MotifFor.INDETERMINE, MockCommune.Lausanne,
					                MotifRattachement.DOMICILE);
					menage.setBlocageRemboursementAutomatique(false);
				}
				return null;
			}
		});

		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				try {
					metierService.fusionneMenages((MenageCommun) tiersDAO.get(ids.noMenageAlfredo), (MenageCommun) tiersDAO.get(ids.noMenageArmando), null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE);
					ch.vd.registre.base.utils.Assert.fail();
				}
				catch (MetierServiceException e) {
					ch.vd.registre.base.utils.Assert.hasText(e.getMessage());
				}
			}
		});
	}


	/**
	 * [UNIREG-1121] Teste que le décès d'un contribuable marié hors-canton ouvre bien un for principal hors-canton sur le conjoint survivant
	 */
	@Test
	public void testDecesHorsCanton() throws Exception {

		class Ids {
			long fabrice;
			long georgette;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un contribuable hors-canton
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, date(2008, 11, 23), "test", null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-canton
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);

				final ForsParType fors = georgette.getForsParType(true);
				assertNotNull(fors);
				assertEquals(1, fors.principauxPP.size());
				assertEquals(0, fors.principauxPM.size());
				assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(),
				                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
			}
		});
	}

	@Test
	public void testDecesDansCouple() throws Exception {

		final long noIndividuM = 123546;
		final long noIndividuMme = 5156532;
		final RegDate dateMariage = date(1980, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final RegDate dateNaissanceM = date(1956, 5, 19);
				final RegDate dateNaissanceMme = date(1957, 12, 24);

				final MockIndividu m = addIndividu(noIndividuM, dateNaissanceM, "Maugrey", "Alastor", true);
				final MockIndividu mme = addIndividu(noIndividuMme, dateNaissanceMme, "McGonagall", "Minerva", false);
				marieIndividus(m, mme, dateMariage);
				addAdresse(m, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(mme, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addNationalite(m, MockPays.RoyaumeUni, dateNaissanceM, null);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissanceMme, null);
				addPermis(m, TypePermis.ETABLISSEMENT, dateMariage, null, false);
				addPermis(mme, TypePermis.ETABLISSEMENT, dateMariage, null, false);
			}
		});

		final class Ids {
			final long m;
			final long mme;
			final long mc;

			Ids(long m, long mme, long mc) {
				this.m = m;
				this.mme = mme;
				this.mc = mc;
			}
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addHabitant(noIndividuM);
				final PersonnePhysique mme = addHabitant(noIndividuMme);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				m.setBlocageRemboursementAutomatique(false);
				mme.setBlocageRemboursementAutomatique(false);

				final MenageCommun mc = couple.getMenage();
				mc.setBlocageRemboursementAutomatique(false);

				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

				return new Ids(m.getNumero(), mme.getNumero(), mc.getNumero());
			}
		});

		final RegDate dateDeces = date(2009, 8, 12);

		// petite vérification et décès de monsieur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.mc);
				Assert.assertFalse(mc.getBlocageRemboursementAutomatique());
				Assert.assertEquals(1, mc.getForsFiscaux().size());

				final ForFiscalPrincipal ffp = (ForFiscalPrincipal) mc.getForsFiscaux().iterator().next();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(couple);

				final PersonnePhysique m = couple.getPrincipal();
				Assert.assertNotNull(m);
				Assert.assertEquals(noIndividuM, (long) m.getNumeroIndividu());
				Assert.assertFalse(m.getBlocageRemboursementAutomatique());
				Assert.assertEquals(0, m.getForsFiscaux().size());

				final PersonnePhysique mme = couple.getConjoint();
				Assert.assertNotNull(mme);
				Assert.assertEquals(noIndividuMme, (long) mme.getNumeroIndividu());
				Assert.assertFalse(mme.getBlocageRemboursementAutomatique());
				Assert.assertEquals(0, mme.getForsFiscaux().size());

				doModificationIndividu(noIndividuM, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						individu.setDateDeces(dateDeces);
					}
				});

				metierService.deces(m, dateDeces, "Pour le test", null);
				return null;
			}
		});

		// vérification des fors et des blocages de remboursement automatique après décès
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.mc);
				Assert.assertTrue(mc.getBlocageRemboursementAutomatique());
				Assert.assertEquals(1, mc.getForsFiscaux().size());

				final ForFiscalPrincipal ffpMc = (ForFiscalPrincipal) mc.getForsFiscaux().iterator().next();
				Assert.assertNotNull(ffpMc);
				Assert.assertEquals(dateMariage, ffpMc.getDateDebut());
				Assert.assertEquals(dateDeces, ffpMc.getDateFin());
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffpMc.getMotifFermeture());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(mc, dateDeces);
				Assert.assertNotNull(couple);

				final PersonnePhysique m = couple.getPrincipal();
				Assert.assertNotNull(m);
				Assert.assertEquals(noIndividuM, (long) m.getNumeroIndividu());
				Assert.assertTrue(m.getBlocageRemboursementAutomatique());
				Assert.assertEquals(0, m.getForsFiscaux().size());

				final PersonnePhysique mme = couple.getConjoint();
				Assert.assertNotNull(mme);
				Assert.assertEquals(noIndividuMme, (long) mme.getNumeroIndividu());
				Assert.assertFalse(mme.getBlocageRemboursementAutomatique());
				Assert.assertEquals(1, mme.getForsFiscaux().size());

				final ForFiscalPrincipal ffpMme = (ForFiscalPrincipal) mme.getForsFiscaux().iterator().next();
				Assert.assertNotNull(ffpMme);
				Assert.assertEquals(dateDeces.addDays(1), ffpMme.getDateDebut());
				Assert.assertEquals(MotifFor.VEUVAGE_DECES, ffpMme.getMotifOuverture());
				Assert.assertNull(ffpMme.getDateFin());

				return null;
			}
		});

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, date(2008, 11, 23), "test", null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-Suisse
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);

				final ForsParType fors = georgette.getForsParType(true);
				assertNotNull(fors);
				assertEquals(1, fors.principauxPP.size());
				assertEquals(0, fors.principauxPM.size());
				assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.PAYS_HS, MockPays.Allemagne.getNoOFS(),
				                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
			}
		});
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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);
				metierService.veuvage(georgette, date(2008, 11, 23), "test", null);
				return null;
			}
		});

		// Vérifie que les fors principals ouvert sur le contribuable survivant est bien hors-canton
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
				assertNotNull(georgette);

				final ForsParType fors = georgette.getForsParType(true);
				assertNotNull(fors);
				assertEquals(1, fors.principauxPP.size());
				assertEquals(0, fors.principauxPM.size());
				assertForPrincipal(date(2008, 11, 24), MotifFor.VEUVAGE_DECES, TypeAutoriteFiscale.COMMUNE_HC, MockCommune.Neuchatel.getNoOFS(),
				                   MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, fors.principauxPP.get(0));
			}
		});
	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateSeparation, null));
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());
						adresse.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));

						// on ne connait que l'adresse courrier sur Genève (= prise par défaut pour l'adresse de domicile)
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateSeparation, null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette à Lausanne
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateMariage, ffp.getDateDebut());
					assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
					assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture());
					assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
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
					assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
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
					assertEquals(MockCommune.Geneve.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
					assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				}
			}
		});
	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationPartiHorsSuisseDepuisMemePf() throws Exception {

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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final RegDate dateDepart = dateSeparation.addDays(-20);

						final MockAdresse vieilleAdresse = (MockAdresse) individu.getAdresses().iterator().next();
						vieilleAdresse.setDateFinValidite(dateDepart);
						vieilleAdresse.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				try {
					metierService.separe(mc, dateSeparation, "test", null, null);
					fail("La séparation aurait dû partir en erreur puisque l'on passe pour Georgette d'un couple vaudois à un for hors-Suisse");
				}
				catch (MetierServiceException e) {
					final String attendu = String.format(
							"D'après son adresse de domicile, on devrait ouvrir un for hors-Suisse pour le contribuable %s (apparemment parti avant la clôture du ménage, mais dans la même période fiscale) alors que le for du ménage %s était vaudois",
							FormatNumeroHelper.numeroCTBToDisplay(ids.georgette), FormatNumeroHelper.numeroCTBToDisplay(ids.menage));
					assertEquals(attendu, e.getMessage());
				}
				return null;
			}
		});

	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseDomicileInconnueMaisForSurMenage() throws Exception {

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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(0, individu.getAdresses().size());

						final RegDate dateDepart = dateSeparation.addDays(-20);

						// on ne connait que l'adresse courrier à Paris (= non prise par défaut pour l'adresse de domicile)
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.COURRIER, "5 Avenue des Champs-Elysées", null, "75017 Paris", MockPays.France, dateDepart.addDays(1), null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				metierService.separe(mc, dateSeparation, "test", null, null);
				return null;
			}
		});

		// vérification des fors fiscaux
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateSeparation.getOneDayBefore(), ffpMc.getDateFin());
				assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpMc.getMotifFermeture());

				// pas d'adresse principale connue -> on reprend le for du couple
				{
					final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ids.georgette);
					assertNotNull(pp);

					final ForFiscalPrincipal ffp = pp.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateSeparation, ffp.getDateDebut());
					assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
					assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
					assertEquals((Integer) MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				}
				return null;
			}
		});
	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseDomicileOuCourrierHorsSuisseDepuisPfAnterieure() throws Exception {

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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());
						adresse.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Aubonne.getNoOFS(),
						                                                 null)); // cette localisation ne doit pas être prise en compte puisqu'une adresse suivante existe

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateSeparation, null));
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER (et ce depuis un an déjà au moment de la séparation -> période fiscale différente)
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse vieilleAdresse = (MockAdresse) individu.getAdresses().iterator().next();
						vieilleAdresse.setDateFinValidite(dateSeparation.addYears(-1));
						vieilleAdresse.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.France.getNoOFS(), null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				metierService.separe(mc, dateSeparation, "test", null, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette à Paris
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateMariage, ffp.getDateDebut());
					assertEquals(dateSeparation.getOneDayBefore(), ffp.getDateFin());
					assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifFermeture());
					assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
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
					assertEquals(MockCommune.Bex.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
					assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
				}

				// For ouvert sur Georgette : à Paris, car le goes-to de sa dernière adresse de domicile connue est utilisée -> France
				{
					final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
					assertNotNull(georgette);

					final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateSeparation, ffp.getDateDebut());
					assertNull(ffp.getDateFin());
					assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffp.getMotifOuverture());
					assertEquals(MockPays.France.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
					assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				}
			}
		});
	}

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseInconnueSansForSurMenage() throws Exception {

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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique georgette = addHabitant(noIndGeorgette);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				return null;
			}
		});

		// Sépare fiscalement les époux après avoir changé les adresses
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateSeparation, null));
						individu.getAdresses().add(new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// vérification de l'absence d'adresse sur Georgette
				{
					final Individu individu = serviceCivil.getIndividu(noIndGeorgette, null, AttributeIndividu.ADRESSES);
					assertNotNull(individu);
					assertNotNull(individu.getAdresses());
					assertEquals(0, individu.getAdresses().size());
				}

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette sans for
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {

				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
					assertNull(ffp);
				}

				// For ouvert sur Fabrice : aucun, car aucun for n'existait sur le ménage commun
				{
					final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
					assertNotNull(fabrice);

					final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
					assertNull(ffp);
				}

				// For ouvert sur Georgette : aucun, car son adresse de domicile est inconnue, et aucun for n'existe sur le ménage commun
				{
					final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
					assertNotNull(georgette);

					final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
					assertNull(ffp);
				}
			}
		});
	}

	/**
	 * [UNIREG-3379] Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit (cas spécial pour les communes fusionnées au
	 * civil mais pas encore au fiscal)
	 */
	@Test
	public void testSeparationAdresseDomicileSurCommunesFusionneesAuCivilMaisPasAuFiscal() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2010, 10, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRueSaintGeorges, null, null, dateMariage, dateSeparation.getOneDayBefore());
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockBatiment.Riex.BatimentRouteDeLaCorniche, null, null, dateSeparation, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRueSaintGeorges, null, null, dateMariage, null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// pour des raisons de validation, on va dire que l'on se place à un jour où la commune de Grandvaux est
		// encore active fiscalement (un mois avant la fin)
		ForFiscalValidator.setFutureBeginDate(MockCommune.Grandvaux.getDateFinValidite().addMonths(-1));
		try {
			// Crée un couple avec un for principal à Grandvaux
			doInTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					final PersonnePhysique fabrice = addHabitant(noIndFabrice);
					final PersonnePhysique georgette = addHabitant(noIndGeorgette);
					final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
					final MenageCommun menage = ensemble.getMenage();

					ids.fabrice = fabrice.getNumero();
					ids.georgette = georgette.getNumero();
					ids.menage = menage.getNumero();

					addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Grandvaux);
					return null;
				}
			});

			// Sépare les époux. Monsieur déménage alors à Riex et Madame garde le camion de Ken
			doInTransaction(new TxCallback<Object>() {
				@Override
				public Object execute(TransactionStatus status) throws Exception {
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);
					metierService.separe(mc, dateSeparation, "test", null, null);
					return null;
				}
			});

			// vérifie les fors principaux ouverts sur les séparés : Monsieur à Riex, Madame à Grandvaux
			doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					// For fermé sur le couple
					{
						final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
						assertNotNull(mc);

						final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
						assertForPrincipal(dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
						                   MockCommune.Grandvaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
					}

					// For ouvert sur Monsieur : à Riex, car sa nouvelle adresse de domicile est là-bas
					{
						final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
						assertNotNull(fabrice);

						final ForFiscalPrincipalPP ffp = fabrice.getDernierForFiscalPrincipal();
						assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Riex, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
					}

					// For ouvert sur Georgette : à Grandvaux, car son domicile n'a pas changé
					{
						final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
						assertNotNull(georgette);

						final ForFiscalPrincipalPP ffp = georgette.getDernierForFiscalPrincipal();
						assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Grandvaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
					}
				}
			});
		}
		finally {
			ForFiscalValidator.setFutureBeginDate(null);
		}
	}

	/**
	 * [UNIREG-3379] Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit (cas spécial pour les communes fusionnées au
	 * civil et au fiscal)
	 */
	@Test
	public void testSeparationAdresseDomicileSurCommunesFusionneesAuCivilEtAuFiscal() throws Exception {

		final long noIndFabrice = 12541L;
		final long noIndGeorgette = 12542L;
		final RegDate naissanceFabrice = date(1970, 1, 1);
		final RegDate naissanceGeorgette = date(1975, 1, 1);
		final RegDate dateMariage = date(1995, 1, 1);
		final RegDate dateSeparation = date(2011, 2, 2);

		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu fabrice = addIndividu(noIndFabrice, naissanceFabrice, "Dunant", "Fabrice", true);
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRueSaintGeorges, null, null, dateMariage, dateSeparation.getOneDayBefore());
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockBatiment.Riex.BatimentRouteDeLaCorniche, null, null, dateSeparation, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockBatiment.Grandvaux.BatimentRueSaintGeorges, null, null, dateMariage, null);

				marieIndividus(fabrice, georgette, dateMariage);
			}
		});

		class Ids {
			long fabrice;
			long georgette;
			long menage;
		}
		final Ids ids = new Ids();

		// Crée un couple avec un for principal à Grandvaux
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique georgette = addHabitant(noIndGeorgette);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(fabrice, georgette, dateMariage, null);
				final MenageCommun menage = ensemble.getMenage();

				ids.fabrice = fabrice.getNumero();
				ids.georgette = georgette.getNumero();
				ids.menage = menage.getNumero();

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Grandvaux.getDateFinValidite(), MotifFor.FUSION_COMMUNES,
				                MockCommune.Grandvaux);
				addForPrincipal(menage, MockCommune.Grandvaux.getDateFinValidite().getOneDayAfter(), MotifFor.FUSION_COMMUNES, MockCommune.BourgEnLavaux);
				return null;
			}
		});

		// Sépare les époux. Monsieur déménage alors à Riex et Madame garde le camion de Ken
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Monsieur à Bourg-en-Lavaux (anciennement Riex), Madame à Bourg-en-Lavaux (anciennement Grandvaux)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipalPP ffp = mc.getDernierForFiscalPrincipal();
					assertForPrincipal(MockCommune.Grandvaux.getDateFinValidite().getOneDayAfter(), MotifFor.FUSION_COMMUNES, dateSeparation.getOneDayBefore(),
					                   MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					                   MockCommune.BourgEnLavaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
				}

				// For ouvert sur Monsieur : à Riex, car sa nouvelle adresse de domicile est là-bas
				{
					final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
					assertNotNull(fabrice);

					final ForFiscalPrincipalPP ffp = fabrice.getDernierForFiscalPrincipal();
					assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.BourgEnLavaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
				}

				// For ouvert sur Georgette : à Grandvaux, car son domicile n'a pas changé
				{
					final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
					assertNotNull(georgette);

					final ForFiscalPrincipalPP ffp = georgette.getDernierForFiscalPrincipal();
					assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.BourgEnLavaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
				}
			}
		});
	}

	/**
	 * Teste que la clôture d'un ménage par décès pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, dateMariage.addMonths(10));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, dateMariage.addMonths(10).getOneDayAfter(), null);

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateDeces);

						individu.setDateDeces(dateDeces);
					}
				});

				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, dateDeces, "test", null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les contribuables : Fabrice est mort, Georgette est à Genève
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateMariage, ffp.getDateDebut());
					assertEquals(dateDeces, ffp.getDateFin());
					assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
					assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
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
					assertEquals(MockCommune.Geneve.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
					assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale());
				}
			}
		});
	}

	/**
	 * Teste que la clôture d'un ménage par décès pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse);
				fabrice.setNationalites(Collections.singletonList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse);
				georgette.setNationalites(Collections.singletonList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueJolimont, null, dateMariage, dateMariage.addMonths(10));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, "Avenue des Champs-Elysées", "5", null, null, "75017 Paris", MockPays.France, dateMariage.addMonths(10).getOneDayAfter(), null);

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
		doInTransaction(new TxCallback<Object>() {
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
		doInTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// changement d'adresse de Fabrice
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					@Override
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateDeces);

						individu.setDateDeces(dateDeces);
					}
				});

				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
				assertNotNull(fabrice);
				metierService.deces(fabrice, dateDeces, "test", null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les contribuables : Fabrice est mort, Georgette est à Genève
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// For fermé sur le couple
				{
					final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
					assertNotNull(mc);

					final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
					assertNotNull(ffp);
					assertEquals(dateMariage, ffp.getDateDebut());
					assertEquals(dateDeces, ffp.getDateFin());
					assertEquals(MotifFor.VEUVAGE_DECES, ffp.getMotifFermeture());
					assertEquals(MockCommune.Lausanne.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
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
		});
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				addForPrincipal(fabrice, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
		});
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique patricia = addNonHabitant("Patricia", "Dutrou", date(1969, 4, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fabrice, patricia, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun menage = couple.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				                MockCommune.Lausanne);
				addForPrincipal(fabrice, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
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
		});
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransaction(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus transactionStatus) {
				final PersonnePhysique fabrice = addHabitant(noIndFabrice);
				final PersonnePhysique patricia = addNonHabitant("Patricia", "Dutrou", date(1969, 4, 12), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(fabrice, patricia, dateMariage, dateSeparation.getOneDayBefore());
				final MenageCommun menage = couple.getMenage();
				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
				                MockCommune.Lausanne);
				return fabrice.getNumero();
			}
		});

		// vérification/application du veuvage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ppId);
				final ValidationResults resultatValidation = metierService.validateVeuvage(fabrice, dateVeuvage);
				Assert.assertNotNull(resultatValidation);
				Assert.assertEquals(1, resultatValidation.errorsCount());

				final String erreur = resultatValidation.getErrors().get(0);
				Assert.assertEquals("L'individu veuf n'a ni couple connu ni for valide à la date de veuvage : problème d'assujettissement ?", erreur);
			}
		});
	}

	@Test
	public void testDecesDansCoupleNonAssujetti() throws Exception {

		final long noIndMr = 124578L;
		final long noIndMme = 235689L;

		final RegDate dateDeces = date(2010, 4, 12);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu mr = addIndividu(noIndMr, date(1948, 1, 26), "Dupetipont", "Martin", true);
				final MockIndividu mme = addIndividu(noIndMme, date(1948, 9, 4), "Dupetipont", "Martine", false);
				addNationalite(mr, MockPays.Suisse, date(1948, 1, 26), null);
				addNationalite(mme, MockPays.Suisse, date(1948, 9, 4), null);
				marieIndividus(mr, mme, date(1971, 4, 17));
				addEtatCivil(mme, dateDeces, TypeEtatCivil.VEUF);
				mr.setDateDeces(dateDeces);
			}
		});

		class Ids {
			long mrId;
			long mmeId;
			long mcId;
		}

		// mise en place fiscale : couple non-assujetti
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique mr = addHabitant(noIndMr);
				final PersonnePhysique mme = addHabitant(noIndMme);
				final EnsembleTiersCouple ensemble = addEnsembleTiersCouple(mr, mme, date(1971, 4, 17), null);

				final Ids ids = new Ids();
				ids.mrId = mr.getNumero();
				ids.mmeId = mme.getNumero();
				ids.mcId = ensemble.getMenage().getNumero();
				return ids;
			}
		});

		// traitement du décès de monsieur
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique mr = (PersonnePhysique) tiersService.getTiers(ids.mrId);
				metierService.deces(mr, dateDeces, "Décès de Monsieur", 1L);
				return null;
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique mr = (PersonnePhysique) tiersService.getTiers(ids.mrId);
				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mmeId);
				final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.mcId);

				// jour du décès : le couple est toujours là
				{
					final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, dateDeces);
					Assert.assertNotNull(ensemble);
					Assert.assertTrue(ensemble.estComposeDe(mr, mme));
				}
				// lendemain du décès : le couple est maintenant vide (donc les rapports entre tiers ont bien été fermés)
				{
					final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, dateDeces.getOneDayAfter());
					Assert.assertNotNull(ensemble);
					Assert.assertNull(ensemble.getPrincipal());
					Assert.assertNull(ensemble.getConjoint());
				}

				// tests des fors :
				//
				// dans l'état actuel de l'implémentation, aucun for n'est créé sur le survivant si
				// le couple n'avait pas de for
				//
				final Set<ForFiscal> forsMc = mc.getForsFiscaux();
				Assert.assertNotNull(forsMc);
				Assert.assertEquals(0, forsMc.size());
				final Set<ForFiscal> forsMr = mr.getForsFiscaux();
				Assert.assertNotNull(forsMr);
				Assert.assertEquals(0, forsMr.size());
				final Set<ForFiscal> forsMme = mme.getForsFiscaux();
				Assert.assertNotNull(forsMme);
				Assert.assertEquals(0, forsMme.size());

				return null;
			}
		});
	}

	/**
	 * Cas UNIREG-2653 le seul décès fiscal d'un habitant toujours vivant dans le civil ne doit pas faire passer la personne physique en non-habitant
	 */
	@Test
	public void testDecesFiscalHabitantToujoursVivantDansLeCivil() throws Exception {

		final long noInd = 435241L;
		final RegDate dateDecesFiscal = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noInd, date(1958, 6, 23), "Moutarde", "Colonel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		// décès fiscal
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());
				metierService.deces(pp, dateDecesFiscal, "Décédé", null);
				return null;
			}
		});

		// vérification de l'impact du décès fiscal (ne doit pas être passé non-habitant!)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());
				return null;
			}
		});
	}

	/**
	 * Cas UNIREG-2653 le décès fiscal d'un habitant également décédé au civil doit faire passer la personne physique en non-habitant [SIFISC-6841] la gestion du flag habitant est désormais séparée de la
	 * gestion des fors fiscaux, le test a été mis-à-jour pour appeler tiersService.updateHabitantFlag() lorsque c'est nécessaire.
	 */
	@Test
	public void testDecesFiscalHabitantDecedeDansLeCivil() throws Exception {

		final long noInd = 435241L;
		final RegDate dateDeces = date(2010, 6, 12);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noInd, date(1958, 6, 23), "Moutarde", "Colonel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), dateDeces);
				individu.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		// décès fiscal
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());
				metierService.deces(pp, dateDeces, "Décédé", null);
				tiersService.updateHabitantStatus(pp, noInd, dateDeces, null);
				return null;
			}
		});

		// vérification de l'impact du décès fiscal (doit être passé non-habitant!)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				return null;
			}
		});
	}

	/**
	 * UNIREG-2653 une annulation de décès dans le civil doit repasser la personne physique en habitant [SIFISC-6841] la gestion du flag habitant est désormais séparée de la gestion des fors fiscaux, le
	 * test a été mis-à-jour pour appeler tiersService.updateHabitantFlag() lorsque c'est nécessaire.
	 */
	@Test
	public void testAnnulationDecesCivil() throws Exception {

		final long noIndDecede = 1234524L;
		final RegDate dateDeces = date(2010, 7, 24);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndDecede, date(1958, 4, 12), "Moutarde", "Colonel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndDecede);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", 1L);
				tiersService.updateHabitantStatus(pp, noIndDecede, dateDeces, null);
				return null;
			}
		});

		// vérification de l'impact fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));
				return null;
			}
		});

		// annulation du décès dans le civil déjà
		doModificationIndividu(noIndDecede, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(null);
			}
		});

		// impact fiscal de l'annulation de décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.annuleDeces(pp, dateDeces);
				tiersService.updateHabitantStatus(pp, noIndDecede, dateDeces, null); // [SIFISC-6841] la gestion du flag habitant est désormais séparée de la gestion des fors fiscaux
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());
				Assert.assertNull(tiersService.getDateDeces(pp));
				return null;
			}
		});
	}

	/**
	 * UNIREG-2653 une annulation de décès dans le civil doit repasser la personne physique en habitant (sauf s'il n'est pas résident sur le canton...)
	 */
	@Test
	public void testAnnulationDecesCivilResidentHorsCanton() throws Exception {

		final long noIndDecede = 1234524L;
		final RegDate dateDeces = date(2010, 7, 24);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndDecede, date(1958, 4, 12), "Moutarde", "Colonel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Geneve.AvenueGuiseppeMotta, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = tiersService.createNonHabitantFromIndividu(noIndDecede);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				metierService.deces(pp, dateDeces, "Décédé", 1L);
				return null;
			}
		});

		// vérification de l'impact fiscal
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));
				return null;
			}
		});

		// annulation du décès dans le civil déjà
		doModificationIndividu(noIndDecede, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(null);
			}
		});

		// impact fiscal de l'annulation de décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.annuleDeces(pp, dateDeces);
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès (la personne habite hors-canton, elle ne doit donc pas passer habitante!)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertNull(tiersService.getDateDeces(pp));
				return null;
			}
		});
	}

	/**
	 * UNIREG-2653 une annulation de décès seulement fiscale ne doit pas repasser la personne physique en habitant [SIFISC-6841] la gestion du flag habitant est désormais séparée de la gestion des fors
	 * fiscaux, le test a été mis-à-jour pour appeler tiersService.updateHabitantFlag() lorsque c'est nécessaire.
	 */
	@Test
	public void testAnnulationDecesFiscalSeulement() throws Exception {

		final long noIndDecede = 1234524L;
		final RegDate dateDeces = date(2010, 7, 24);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndDecede, date(1958, 4, 12), "Moutarde", "Colonel", true);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndDecede);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", 1L);
				tiersService.updateHabitantStatus(pp, noIndDecede, dateDeces, null);
				return null;
			}
		});

		// vérification de l'impact fiscal et annulation fiscale du décès
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));

				metierService.annuleDeces(pp, dateDeces);
				tiersService.updateHabitantStatus(pp, noIndDecede, dateDeces, null);
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertNull(tiersService.getDateDeces(pp));
				return null;
			}
		});
	}

	/**
	 * [SIFISC-14437] il faut ré-ouvrir tous les rapports entre tiers précédemment fermés à la date du décès (et pas seulement les rapports "sujet")
	 */
	@Test
	public void testAnnulationDecesEtReouvertureRapportsEntreTiers() throws Exception {

		final long noIndividu = 436782456L;
		final RegDate dateDeces = date(2015, 3, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu individu = addIndividu(noIndividu, date(1958, 4, 12), "Gump", "Forrest", Sexe.MASCULIN);
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.BoulevardGrancy, null, date(2000, 1, 1), null);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				final PersonnePhysique pupille = addNonHabitant("Philomène", "Smith", date(1945, 6, 30), Sexe.FEMININ);
				addTutelle(pupille, pp, null, date(2014, 1, 1), null);

				final PersonnePhysique epouse = addNonHabitant("Lee", "Harper-Gump", date(1960, 7, 21), Sexe.FEMININ);
				addEnsembleTiersCouple(pp, epouse, date(1995, 8, 3), null);

				final DebiteurPrestationImposable dpi = addDebiteur(CategorieImpotSource.REGULIERS, PeriodiciteDecompte.ANNUEL, date(2009, 1, 1));
				addRapportPrestationImposable(dpi, epouse, date(2000, 7, 1), dateDeces, false);     // par hasard, la même date -> ce rapport ne doit en aucun cas être réouvert à l'annulation du décès du mari !

				return pp.getNumero();
			}
		});

		// on décède le monsieur
		doModificationIndividu(noIndividu, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// on traite le décès fiscal
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique defunt = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.deces(defunt, dateDeces, null, null);
			}
		});

		// on vérifie maintenant que tous les rapports sont fermés à la date de décès
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final Set<RapportEntreTiers> objets = pp.getRapportsObjet();
				Assert.assertEquals(1, objets.size());
				for (RapportEntreTiers ret : objets) {
					Assert.assertEquals(ret.toString(), dateDeces, ret.getDateFin());
					Assert.assertFalse(ret.toString(), ret.isAnnule());
				}
				final Set<RapportEntreTiers> sujets = pp.getRapportsSujet();
				Assert.assertEquals(1, sujets.size());
				for (RapportEntreTiers ret : sujets) {
					Assert.assertEquals(ret.toString(), dateDeces, ret.getDateFin());
					Assert.assertFalse(ret.toString(), ret.isAnnule());
				}
			}
		});

		// maintenant, on annule le décès
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique defunt = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.annuleDeces(defunt, dateDeces);
			}
		});

		// et on vérifie que tous les rapports précédemment fermés à la date de décès sont bien ré-ouverts
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				int decompte = 0;

				final Set<RapportEntreTiers> objets = pp.getRapportsObjet();
				Assert.assertEquals(2, objets.size());
				for (RapportEntreTiers ret : objets) {
					if (ret.isAnnule()) {
						++ decompte;
						Assert.assertEquals(ret.toString(), dateDeces, ret.getDateFin());
					}
					else {
						-- decompte;
						Assert.assertNull(ret.toString(), ret.getDateFin());
					}
				}
				Assert.assertEquals(0, decompte);

				final Set<RapportEntreTiers> sujets = pp.getRapportsSujet();
				Assert.assertEquals(2, sujets.size());
				for (RapportEntreTiers ret : sujets) {
					if (ret.isAnnule()) {
						++ decompte;
						Assert.assertEquals(ret.toString(), dateDeces, ret.getDateFin());
					}
					else {
						-- decompte;
						Assert.assertNull(ret.toString(), ret.getDateFin());
					}
				}
				Assert.assertEquals(0, decompte);
			}
		});

		// vérification sur l'épouse, le rapport de prestation imposable ne doit pas avoir bougé (= pas de ré-ouverture!)
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateDeces.getOneDayAfter());      // le couple revit, normalement
				Assert.assertNotNull(couple);
				final PersonnePhysique epouse = couple.getConjoint(pp);
				Assert.assertNotNull("Le rapport d'appartenance ménage n'aurait-il pas été ré-ouvert ?", epouse);

				final RapportEntreTiers rt = epouse.getDernierRapportSujet(TypeRapportEntreTiers.PRESTATION_IMPOSABLE);
				Assert.assertNotNull(rt);
				Assert.assertEquals(dateDeces, rt.getDateFin());
			}
		});
	}

	@Test
	public void testVenteImmeubleVeilleMariage() throws Exception {

		final long noIndividuMonsieur = 123456789L;
		final long noIndividuMadame = 987654321L;
		final RegDate dateMariage = date(2009, 5, 1);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuMonsieur, date(1965, 9, 5), "Dursley", "Vernon", true);
				final MockIndividu mme = addIndividu(noIndividuMadame, date(1965, 4, 26), "Dursley", "Petunia", false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		final class Ids {
			long idMonsieur;
			long idMadame;
			long idMenage;
		}

		// on crée les deux célibataires, avec monsieur propriétaire d'un immeuble vendu à la veille de son mariage, puis on enregistre le mariage
		final Ids ids = doInNewTransactionAndSession(new TxCallback<Ids>() {
			@Override
			public Ids execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = addHabitant(noIndividuMonsieur);
				addForPrincipal(m, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addForSecondaire(m, date(2004, 4, 28), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens.getNoOFS(),
				                 MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addHabitant(noIndividuMadame);
				addForPrincipal(mme, date(2002, 8, 12), MotifFor.ARRIVEE_HS, MockCommune.Renens);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, null, EtatCivil.MARIE, null);
				Assert.assertNotNull(mc);

				final Ids ids = new Ids();
				ids.idMonsieur = m.getNumero();
				ids.idMadame = mme.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		// et on vérifie les fors après l'enregistrement du mariage
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.idMenage);
				Assert.assertNotNull(mc);

				final EnsembleTiersCouple ensemble = tiersService.getEnsembleTiersCouple(mc, null);
				Assert.assertNotNull(ensemble);

				final PersonnePhysique principal = ensemble.getPrincipal();
				Assert.assertNotNull(principal);
				Assert.assertEquals(ids.idMonsieur, (long) principal.getNumero());

				final PersonnePhysique conjoint = ensemble.getConjoint();
				Assert.assertNotNull(conjoint);
				Assert.assertEquals(ids.idMadame, (long) conjoint.getNumero());

				// fors de monsieur : doivent tous être fermés à la veille du mariage
				{
					final List<ForFiscal> fors = principal.getForsFiscauxSorted();
					Assert.assertNotNull(fors);
					Assert.assertEquals(2, fors.size());
					for (ForFiscal ff : fors) {
						Assert.assertNotNull(ff);
						Assert.assertFalse(ff.toString(), ff.isAnnule());
						Assert.assertEquals(ff.toString(), dateMariage.getOneDayBefore(), ff.getDateFin());
					}
				}

				// for de madame : doit être fermé à la veille du mariage également
				{
					final List<ForFiscal> fors = conjoint.getForsFiscauxSorted();
					Assert.assertNotNull(fors);
					Assert.assertEquals(1, fors.size());

					final ForFiscal ff = fors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertFalse(ff.isAnnule());
					Assert.assertEquals(dateMariage.getOneDayBefore(), ff.getDateFin());
				}

				// for du couple : seul le for principal doit être créé (pas de for secondaire, puisque l'immeuble avait déjà été vendu)
				{
					final List<ForFiscal> fors = mc.getForsFiscauxSorted();
					Assert.assertNotNull(fors);
					Assert.assertEquals(1, fors.size());

					final ForFiscal ff = fors.get(0);
					Assert.assertNotNull(ff);
					Assert.assertFalse(ff.isAnnule());
					Assert.assertTrue(ff instanceof ForFiscalPrincipal);
					Assert.assertEquals(dateMariage, ff.getDateDebut());
					Assert.assertNull(ff.getDateFin());
				}
				return null;
			}
		});
	}

	/**
	 * Cas UNIREG-1599
	 */
	@Test
	public void testAnnulationMariageEtReouvertureDeFors() throws Exception {

		final long noIndividuMonsieur = 123456789L;
		final long noIndividuMadame = 987654321L;
		final RegDate dateMariage = date(2004, 6, 25);

		// mise en place civile
		serviceCivil.setUp(new DefaultMockServiceCivil(false) {
			@Override
			protected void init() {
				final MockIndividu m = addIndividu(noIndividuMonsieur, date(1972, 11, 4), "Favre", "Jean-Jacques", true);
				final MockIndividu mme = addIndividu(noIndividuMadame, date(1977, 8, 16), "Favre", "Chrystèle", false);

				addNationalite(m, MockPays.Suisse, date(1972, 11, 4), null);
				addNationalite(mme, MockPays.France, date(1977, 8, 16), null);
				addPermis(mme, TypePermis.SEJOUR, date(2002, 7, 18), null, false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		final class Ids {
			long idMonsieur;
			long idMadame;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = addHabitant(noIndividuMonsieur);

				// c'est ce for annulé qui faisait échouer le test : en raison de sa présence, la mécanique (implémentée pour
				// le cas jira UNIREG-1157) ne re-créait pas les fors
				final ForFiscalPrincipal ffpAnnule = addForPrincipal(m, dateMariage, MotifFor.DEMENAGEMENT_VD, MockCommune.Lausanne);
				ffpAnnule.setAnnule(true);

				addForPrincipal(m, date(2002, 12, 1), MotifFor.DEMENAGEMENT_VD, dateMariage.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final PersonnePhysique mme = addHabitant(noIndividuMadame);

				final EnsembleTiersCouple couple = addEnsembleTiersCouple(m, mme, dateMariage, null);
				final MenageCommun mc = couple.getMenage();

				// pour coller au cas qui n'a pas fonctionné en vrai, on va également ajouter des rapports annulés
				final RegDate dateMariageAnnule = date(2008, 1, 1);
				addAppartenanceMenage(mc, m, dateMariageAnnule, null, true);
				addAppartenanceMenage(mc, mme, dateMariageAnnule, null, true);

				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Lausanne);

				final Ids ids = new Ids();
				ids.idMonsieur = m.getNumero();
				ids.idMadame = mme.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		// annulation du mariage
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(ids.idMenage);
				final EnsembleTiersCouple ensembleTiersCouple = tiersService.getEnsembleTiersCouple(mc, null);

				// retour par le couple comme dans l'IHM...
				final MenageCommun menageCommun = ensembleTiersCouple.getMenage();
				Assert.assertNotNull(menageCommun);

				final RapportEntreTiers dernierRapport = menageCommun.getDernierRapportObjet(TypeRapportEntreTiers.APPARTENANCE_MENAGE);
				final RegDate dateReference = dernierRapport.getDateDebut();

				metierService.annuleMariage(ensembleTiersCouple.getPrincipal(), ensembleTiersCouple.getConjoint(), dateReference, null);
				return null;
			}
		});

		// test de résultat -> le for de monsieur devrait avoir été ré-ouvert
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.idMonsieur);
				Assert.assertNotNull(m);

				final ForFiscalPrincipal ffp = m.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals(date(2002, 12, 1), ffp.getDateDebut());
				return null;
			}
		});
	}

	/**
	 * Cas du JIRA UNIREG-2323 : si les deux membres du couple ont tous deux le même for secondaire (= même date d'ouverture, même commune), il ne doit y avoir qu'un seul for équivalent ouvert sur le
	 * couple
	 */
	@Test
	public void testMariageAvecForsSecondairesIdentiques() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires identiques", EtatCivil.MARIE, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principauxPP);
				Assert.assertNotNull(fors.principauxPM);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principauxPP.size());
				Assert.assertEquals(0, fors.principauxPM.size());
				Assert.assertEquals(1, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockPays.RoyaumeUni.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());

				final ForFiscalSecondaire ffs = fors.secondaires.get(0);
				Assert.assertNotNull(ffs);
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
				Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffs.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateMariage, ffs.getDateDebut());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffs.getMotifOuverture());
				Assert.assertNull(ffs.getDateFin());
				Assert.assertNull(ffs.getMotifFermeture());
				return null;
			}
		});
	}

	/**
	 * Cas du JIRA UNIREG-2323 : si les fors secondaires sur les membres du couple n'ont pas la même commune, ils doivent tous deux se retrouver sur le couple
	 */
	@Test
	public void testMariageAvecForsSecondairesDifferentsParCommune() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires différents sur la commune", EtatCivil.MARIE, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principauxPP);
				Assert.assertNotNull(fors.principauxPM);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principauxPP.size());
				Assert.assertEquals(0, fors.principauxPM.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockPays.RoyaumeUni.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());

				boolean foundCroy = false;
				boolean foundEchallens = false;
				for (ForFiscalSecondaire ffs : fors.secondaires) {
					Assert.assertNotNull(ffs);
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());

					if (ffs.getNumeroOfsAutoriteFiscale() == MockCommune.Echallens.getNoOFS()) {
						Assert.assertFalse("Double for à Echallens trouvé", foundEchallens);
						foundEchallens = true;
					}
					else if (ffs.getNumeroOfsAutoriteFiscale() == MockCommune.Croy.getNoOFS()) {
						Assert.assertFalse("Double for à Croy trouvé", foundCroy);
						foundCroy = true;
					}

					Assert.assertEquals(dateMariage, ffs.getDateDebut());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertNull(ffs.getMotifFermeture());
				}
				Assert.assertTrue(foundEchallens);
				Assert.assertTrue(foundCroy);

				return null;
			}
		});
	}

	/**
	 * Cas du JIRA UNIREG-3074 : si les fors secondaires sur les membres du couple n'ont pas le même motif de rattachement, ils doivent tous deux se retrouver sur le couple (immeuble et activité
	 * indépendante)
	 */
	@Test
	public void testMariageAvecForsSecondairesDifferentsParMotifRattachement() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens.getNoOFS(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = doInNewTransactionAndSession(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires différents selon leur motif de rattachement", EtatCivil.MARIE, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principauxPP);
				Assert.assertNotNull(fors.principauxPM);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principauxPP.size());
				Assert.assertEquals(0, fors.principauxPM.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockPays.RoyaumeUni.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());

				boolean foundImmeuble = false;
				boolean foundActiviteIndependante = false;
				for (ForFiscalSecondaire ffs : fors.secondaires) {
					Assert.assertNotNull(ffs);
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffs.getNumeroOfsAutoriteFiscale());

					if (ffs.getMotifRattachement() == MotifRattachement.ACTIVITE_INDEPENDANTE) {
						Assert.assertFalse("Double for pour activité indépendante trouvé", foundActiviteIndependante);
						foundActiviteIndependante = true;
					}
					else if (ffs.getMotifRattachement() == MotifRattachement.IMMEUBLE_PRIVE) {
						Assert.assertFalse("Double for pour immeuble trouvé", foundImmeuble);
						foundImmeuble = true;
					}

					Assert.assertEquals(dateMariage, ffs.getDateDebut());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertNull(ffs.getMotifFermeture());
				}
				Assert.assertTrue(foundActiviteIndependante);
				Assert.assertTrue(foundImmeuble);

				return null;
			}
		});
	}

	/**
	 * Cas du JIRA UNIREG-2323 : si les fors secondaires sur les membres du couple n'ont pas la même date d'ouverture (même s'ils sont sur la même commune), ils doivent tous deux se retrouver sur le
	 * couple
	 */
	@Test
	public void testMariageAvecForsSecondairesDifferentsParDateOuverture() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut.addMonths(1), MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut.addMonths(1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires identiques", EtatCivil.MARIE, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principauxPP);
				Assert.assertNotNull(fors.principauxPM);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principauxPP.size());
				Assert.assertEquals(0, fors.principauxPM.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principauxPP.get(0);
				Assert.assertNotNull(ffp);
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				Assert.assertEquals(MockPays.RoyaumeUni.getNoOFS(), (int) ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertNull(ffp.getMotifFermeture());

				for (ForFiscalSecondaire ffs : fors.secondaires) {
					Assert.assertNotNull(ffs);
					Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
					Assert.assertEquals(MockCommune.Echallens.getNoOFS(), (int) ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(dateMariage, ffs.getDateDebut());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertNull(ffs.getMotifFermeture());
				}
				return null;
			}
		});
	}

	/**
	 * SIFISC-485 : un mariage doit pouvoir être saisi le jour du décès
	 */
	@Test
	public void testMariageJourDuDeces() throws Exception {

		final long noIndividu = 23781537537L;
		final RegDate dateNaissance = date(1974, 1, 1);
		final RegDate dateDeces = date(2010, 4, 16);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Frankenstein", "Junior", true);
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateNaissance, null);
				ind.setDateDeces(dateDeces);
			}
		});

		// mise en place fisccale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		final RegDate dateMariage = dateDeces;

		// appel du service métier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				try {
					metierService.marie(dateMariage, pp, null, null, EtatCivil.MARIE, null);
				}
				catch (MetierServiceException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		// vérification de la création du ménage (juste pour voir s'il a fait quelque chose...
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				assertEquals(dateDeces, tiersService.getDateDeces(pp));

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateMariage);
				assertNotNull(couple);

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffp);
				assertEquals(dateMariage, ffp.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffp.getMotifOuverture());
				assertNull(ffp.getDateFin());
				assertNull(ffp.getMotifFermeture());

				return null;
			}
		});
	}

	/**
	 * SIFISC-485 : un mariage doit pouvoir être saisi le jour du décès... mais pas le lendemain
	 */
	@Test
	public void testMariageLendemainDuDeces() throws Exception {

		final long noIndividu = 23781537537L;
		final RegDate dateNaissance = date(1974, 1, 1);
		final RegDate dateDeces = date(2010, 4, 16);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noIndividu, null, "Frankenstein", "Junior", true);
				addNationalite(ind, MockPays.Suisse, dateNaissance, null);
				addAdresse(ind, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateNaissance, null);
				ind.setDateDeces(dateDeces);
			}
		});

		// mise en place fiscale
		final long ppId = doInNewTransactionAndSession(new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndividu);
				addForPrincipal(pp, dateNaissance.addYears(18), MotifFor.MAJORITE, MockCommune.Bex);
				return pp.getNumero();
			}
		});

		final RegDate dateMariage = dateDeces.getOneDayAfter();

		// appel du service métier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				try {
					metierService.marie(dateMariage, pp, null, null, EtatCivil.MARIE, null);
					fail("Mariage post-mortem interdit !");
				}
				catch (MetierServiceException e) {
					assertEquals("Il n'est pas possible de créer un rapport d'appartenance ménage après la date de décès d'une personne physique", e.getMessage());
				}
				status.setRollbackOnly();
				return null;
			}
		});
	}

	/**
	 * [SIFISC-7881] Vérifie qu'il est possible de marier deux personnes dont l'une des deux ne possède ni permis ni nationalité, du moment que l'autre possède un permis C ou la nationalité suisse.
	 */
	@Test
	public void testMariageMonsieurAvecPermisCMadameSansPermisNiNationalite() throws Exception {

		final Long noInd = 12334L;

		class Ids {
			long principal;
			long conjoint;
		}
		final Ids ids = new Ids();

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu ind = addIndividu(noInd, date(1963, 4, 22), "Roberelle", "René", Sexe.MASCULIN);
				addPermis(ind, TypePermis.SEJOUR, date(1998, 5, 12), null, false);
			}
		});

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique principal = addHabitant(noInd);
				addForPrincipal(principal, date(1998, 5, 12), MotifFor.ARRIVEE_HS, MockCommune.Echallens);
				ids.principal = principal.getId();
				final PersonnePhysique conjoint = addNonHabitant("Agathe", "Di", date(1973, 3, 2), Sexe.FEMININ);
				ids.conjoint = conjoint.getId();
			}
		});

		final RegDate dateMariage = date(2007, 2, 12);

		// appel du service métier
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(ids.principal);
				final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.get(ids.conjoint);
				metierService.marie(dateMariage, principal, conjoint, null, EtatCivil.MARIE, null);
				return null;
			}
		});

		// Vérifie que le couple a été créé dans la base
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(ids.principal);
				assertNotNull(principal);

				final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(principal, null);
				assertNotNull(etc);

				final MenageCommun menage = etc.getMenage();
				assertNotNull(menage);

				final ForFiscalPrincipalPP ffp = menage.getForFiscalPrincipalAt(null);
				assertForPrincipal(date(2007, 2, 12), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Echallens, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
				return null;
			}
		});
	}

	/**
	 * [SIFISC-7881] Vérifie qu'il n'est *pas* possible de marier deux personnes lorsqu'aucune ne possède ni permis ni nationalité.
	 */
	@Test
	public void testMariageMonsieurEtMadameSansPermisNiNationalite() throws Exception {

		class Ids {
			long principal;
			long conjoint;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique principal = addNonHabitant("René", "Roberelle", date(1963, 4, 22), Sexe.MASCULIN);
				ids.principal = principal.getId();
				final PersonnePhysique conjoint = addNonHabitant("Agathe", "Di", date(1973, 3, 2), Sexe.FEMININ);
				ids.conjoint = conjoint.getId();
			}
		});

		final RegDate dateMariage = date(2007, 2, 12);

		// appel du service métier
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(ids.principal);
				final PersonnePhysique conjoint = (PersonnePhysique) tiersDAO.get(ids.conjoint);
				try {
					metierService.marie(dateMariage, principal, conjoint, null, EtatCivil.MARIE, null);
				}
				catch (MetierServiceException e) {
					assertEquals("Impossible de déterminer le mode d'imposition requis (que ce soit sur le principal ou le conjoint)", e.getMessage());
					status.setRollbackOnly();
				}
				return null;
			}
		});

		// Vérifie que le couple n'a pas été créé dans la base
		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				final PersonnePhysique principal = (PersonnePhysique) tiersDAO.get(ids.principal);
				assertNotNull(principal);

				final EnsembleTiersCouple etc = tiersService.getEnsembleTiersCouple(principal, null);
				assertNull(etc);
				return null;
			}
		});
	}

	/**
	 * SIFISC-5323 : en cas d'annulation de séparation, pour un "partenaire" seul, sa situation de famille doit revenir à partenariat enregistré
	 */
	@Test
	public void testAnnulationSeparationPartenariatSeul() throws Exception {

		final long noIndiv = 123456L;
		final RegDate dateNaissance = RegDate.get(1963, 6, 25);
		final RegDate dateMajorite = dateNaissance.addYears(18);
		final RegDate datePartenariat = dateMajorite.addYears(4);
		final RegDate dateSeparation = datePartenariat.addYears(2);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu georges = addIndividu(noIndiv, dateNaissance, "Georges", "Michael", true);
				addEtatCivil(georges, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(georges, datePartenariat, TypeEtatCivil.PACS);
			}
		});

		// mise en place fiscale
		final Long[] Ids = doInNewTransactionAndSession(new TransactionCallback<Long[]>() {
			@Override
			public Long[] doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndiv);
				addForPrincipal(pp, dateMajorite, MotifFor.MAJORITE, datePartenariat.getOneDayBefore(), MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Bex);
				addForPrincipal(pp, dateSeparation.getOneDayAfter(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bex);

				EnsembleTiersCouple etc = addEnsembleTiersCouple(pp, null, datePartenariat, dateSeparation);
				final MenageCommun mc = etc.getMenage();
				addForPrincipal(mc, datePartenariat, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Bex);
				SituationFamilleMenageCommun sfmc = addSituation(mc, datePartenariat, dateSeparation, 0, TarifImpotSource.NORMAL);
				sfmc.setEtatCivil(EtatCivil.LIE_PARTENARIAT_ENREGISTRE);
				SituationFamillePersonnePhysique sfpp = addSituation(pp, dateSeparation.getOneDayAfter(), null, 0);
				sfpp.setEtatCivil(EtatCivil.SEPARE);
				return new Long[]{pp.getNumero(), mc.getNumero()};
			}
		});

		final long ppId = Ids[0];
		final long menageId = Ids[1];

		// appel du service métier
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(menageId);
				try {
					metierService.annuleSeparation(mc, dateSeparation.getOneDayAfter(), 1234L);
				}
				catch (MetierServiceException e) {
					Assert.fail(e.getMessage());
				}
				return null;
			}
		});

		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(menageId);
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);

				assertEquals("Le ménage commun devrait avoir 2 situations de famille, 1 annulée et 1 active",
				             2, mc.getSituationsFamille().size());

				assertEquals("La personne physique devrait avoir 1 situation de famille annulée",
				             1, pp.getSituationsFamille().size());

				final SituationFamille sfmcActive = mc.getSituationFamilleActive();
				final SituationFamille sfppActive = pp.getSituationFamilleActive();

				assertEquals("La situation de famille active sur le ménage commun doit etre " + EtatCivil.LIE_PARTENARIAT_ENREGISTRE,
				             EtatCivil.LIE_PARTENARIAT_ENREGISTRE,
				             sfmcActive.getEtatCivil());
				assertNull("Il ne doit pas y avoir de situation de famille active sur la personne physique", sfppActive);

				final SituationFamille sfppAnnulee = pp.getSituationsFamille().iterator().next();
				assertTrue("La situation de famille doit etre annulée sur la personne physique", sfppAnnulee.isAnnule());

				return null;
			}
		});
	}

	@Test
	public void testDepartConjointHorsCantonPuisSeparation() throws Exception {

		final long noIndividuElle = 34645672456723L;
		final long noIndividuLui = 346782457L;
		final RegDate dateNaissance = date(1970, 1, 17);
		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDepartElle = date(2011, 11, 12);
		final RegDate dateSeparation = dateDepartElle.addMonths(4);     // changement de période fiscale, pour ne pas empiéter sur d'autres règles... cd UNIREG-2143

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Dus", "Jean-Paul", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, null);
				addNationalite(lui, MockPays.Suisse, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissance, "D'escampette", "Poudre", Sexe.FEMININ);
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, dateDepartElle);
				adrElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Bern.getNoOFS(), null));
				addNationalite(elle, MockPays.Suisse, dateNaissance, null);

				marieIndividus(elle, lui, dateMariage);
				separeIndividus(elle, lui, dateSeparation);
			}
		});

		// mise en place fiscale
		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);

				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				return ids;
			}
		});

		// traitement de la séparation...
		// ce qu'on veut obtenir, c'est la fermeture du for du couple à la date de la séparation, l'ouverture d'un for sur chacun des
		// ancien conjoints (lui à Moudon, elle à Berne - malgré l'absence d'adresse de domicile sur Berne renvoyée par le civil)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				metierService.separe(mc, dateSeparation, null, null, null);
				return null;
			}
		});

		// vérification du résultat : for fermé sur le couple, et for ouvert sur les deux compères
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffpMc);
				Assert.assertEquals(dateSeparation.getOneDayBefore(), ffpMc.getDateFin());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpMc.getMotifFermeture());

				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final ForFiscalPrincipal ffpLui = lui.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Monsieur ?", ffpLui);
				Assert.assertEquals(dateSeparation, ffpLui.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpLui.getMotifOuverture());
				Assert.assertNull(ffpLui.getDateFin());
				Assert.assertNull(ffpLui.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpLui.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Moudon.getNoOFS(), ffpLui.getNumeroOfsAutoriteFiscale());

				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
				final ForFiscalPrincipal ffpElle = elle.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Madame ?", ffpElle);
				Assert.assertEquals(dateSeparation, ffpElle.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpElle.getMotifOuverture());
				Assert.assertNull(ffpElle.getDateFin());
				Assert.assertNull(ffpElle.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffpElle.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Bern.getNoOFS(), ffpElle.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}

	@Test
	public void testDepartConjointHorsSuissePuisSeparation() throws Exception {

		final long noIndividuElle = 34645672456723L;
		final long noIndividuLui = 346782457L;
		final RegDate dateNaissance = date(1970, 1, 17);
		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDepartElle = date(2011, 11, 12);
		final RegDate dateSeparation = dateDepartElle.addMonths(4);     // changement de période fiscale, pour ne pas empiéter sur d'autres règles... cd UNIREG-2143

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Dus", "Jean-Paul", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, null);
				addNationalite(lui, MockPays.Suisse, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissance, "D'escampette", "Poudre", Sexe.FEMININ);
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, dateDepartElle);
				adrElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Albanie.getNoOFS(), null));
				addNationalite(elle, MockPays.Suisse, dateNaissance, null);

				marieIndividus(elle, lui, dateMariage);
				separeIndividus(elle, lui, dateSeparation);
			}
		});

		// mise en place fiscale
		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);

				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				return ids;
			}
		});

		// traitement de la séparation...
		// ce qu'on veut obtenir, c'est la fermeture du for du couple à la date de la séparation, l'ouverture d'un for sur chacun des
		// ancien conjoints (lui à Moudon, elle en Albanie - malgré l'absence d'adresse de domicile renvoyée par le civil)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				metierService.separe(mc, dateSeparation, null, null, null);
				return null;
			}
		});

		// vérification du résultat : for fermé sur le couple, et for ouvert sur les deux compères
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffpMc);
				Assert.assertEquals(dateSeparation.getOneDayBefore(), ffpMc.getDateFin());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpMc.getMotifFermeture());

				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final ForFiscalPrincipal ffpLui = lui.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Monsieur ?", ffpLui);
				Assert.assertEquals(dateSeparation, ffpLui.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpLui.getMotifOuverture());
				Assert.assertNull(ffpLui.getDateFin());
				Assert.assertNull(ffpLui.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpLui.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Moudon.getNoOFS(), ffpLui.getNumeroOfsAutoriteFiscale());

				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
				final ForFiscalPrincipal ffpElle = elle.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Madame ?", ffpElle);
				Assert.assertEquals(dateSeparation, ffpElle.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpElle.getMotifOuverture());
				Assert.assertNull(ffpElle.getDateFin());
				Assert.assertNull(ffpElle.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffpElle.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockPays.Albanie.getNoOFS(), ffpElle.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}

	@Test
	public void testDepartConjointVaudoisPuisSeparation() throws Exception {

		final long noIndividuElle = 34645672456723L;
		final long noIndividuLui = 346782457L;
		final RegDate dateNaissance = date(1970, 1, 17);
		final RegDate dateMariage = date(2000, 1, 1);
		final RegDate dateDepartElle = date(2011, 11, 12);
		final RegDate dateSeparation = dateDepartElle.addMonths(4);     // changement de période fiscale, pour ne pas empiéter sur d'autres règles... cd UNIREG-2143

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, dateNaissance, "Dus", "Jean-Paul", Sexe.MASCULIN);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, null);
				addNationalite(lui, MockPays.Suisse, dateNaissance, null);

				final MockIndividu elle = addIndividu(noIndividuElle, dateNaissance, "D'escampette", "Poudre", Sexe.FEMININ);
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Moudon.LeBourg, null, dateMariage, dateDepartElle);
				adrElle.setLocalisationSuivante(new Localisation(LocalisationType.CANTON_VD, MockCommune.Lausanne.getNoOFS(), null));
				addNationalite(elle, MockPays.Suisse, dateNaissance, null);

				marieIndividus(elle, lui, dateMariage);
				separeIndividus(elle, lui, dateSeparation);
			}
		});

		// mise en place fiscale
		class Ids {
			long idLui;
			long idElle;
			long idMenage;
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);

				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				addForPrincipal(couple.getMenage(), dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Moudon);
				final Ids ids = new Ids();
				ids.idLui = lui.getNumero();
				ids.idElle = elle.getNumero();
				ids.idMenage = couple.getMenage().getNumero();
				return ids;
			}
		});

		// traitement de la séparation...
		// ce qu'on veut obtenir, c'est la fermeture du for du couple à la date de la séparation, l'ouverture d'un for sur chacun des
		// ancien conjoints (tous deux à Moudon, car elle a fait un départ vaudois sans arrivée... donc on reprends l'ancienne commune)
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				metierService.separe(mc, dateSeparation, null, null, null);
				return null;
			}
		});

		// vérification du résultat : for fermé sur le couple, et for ouvert sur les deux compères
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.idMenage);
				final ForFiscalPrincipal ffpMc = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffpMc);
				Assert.assertEquals(dateSeparation.getOneDayBefore(), ffpMc.getDateFin());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpMc.getMotifFermeture());

				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final ForFiscalPrincipal ffpLui = lui.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Monsieur ?", ffpLui);
				Assert.assertEquals(dateSeparation, ffpLui.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpLui.getMotifOuverture());
				Assert.assertNull(ffpLui.getDateFin());
				Assert.assertNull(ffpLui.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpLui.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Moudon.getNoOFS(), ffpLui.getNumeroOfsAutoriteFiscale());

				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
				final ForFiscalPrincipal ffpElle = elle.getDernierForFiscalPrincipal();
				Assert.assertNotNull("Pas de for créé sur Madame ?", ffpElle);
				Assert.assertEquals(dateSeparation, ffpElle.getDateDebut());
				Assert.assertEquals(MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, ffpElle.getMotifOuverture());
				Assert.assertNull(ffpElle.getDateFin());
				Assert.assertNull(ffpElle.getMotifFermeture());
				Assert.assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffpElle.getTypeAutoriteFiscale());
				Assert.assertEquals((Integer) MockCommune.Moudon.getNoOFS(), ffpElle.getNumeroOfsAutoriteFiscale());

				return null;
			}
		});
	}

	/**
	 * Montre que le for d'un couple qui se marie à l'étranger est repris du for de Monsieur pour les vrais non-habitants
	 */
	@Test
	public void testForCoupleApresMariageContribuablesSansAdresseDomicileEtForHorsSuisse() throws Exception {

		final RegDate dateMariage = date(2011, 11, 11);

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				// vide...
			}
		});

		// mise en place des individus HS
		class Ids {
			final long idLui;
			final long idElle;

			Ids(long idLui, long idElle) {
				this.idLui = idLui;
				this.idElle = idElle;
			}
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addNonHabitant("Luciano", "Pavarotti", null, Sexe.MASCULIN);
				final PersonnePhysique elle = addNonHabitant("Adriana", "Pavarotti", null, Sexe.FEMININ);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, MockPays.Allemagne);
				addForPrincipal(elle, date(2000, 1, 1), MotifFor.INDETERMINE, MockPays.Danemark);
				return new Ids(lui.getNumero(), elle.getNumero());
			}
		});

		// mariage
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
				metierService.marie(dateMariage, lui, elle, null, EtatCivil.MARIE, null);
				return null;
			}
		});

		// vérification du résultat (c'est le for du contribuable principal qui détermine celui du couple)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(lui, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals((Long) ids.idLui, couple.getPrincipal().getNumero());
				Assert.assertEquals((Long) ids.idElle, couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				return null;
			}
		});
	}

	/**
	 * Montre que le for du couple d'anciens habitants qui se marient à l'étranger vient du for de Monsieur même en présence d'anciennes adresses de domicile vaudoises avec GoesTo
	 */
	@Test
	public void testForCoupleApresMariageContribuablesAnciensHabitantsHorsSuisse() throws Exception {

		final RegDate dateDepart = date(2001, 5, 21);
		final RegDate dateMariage = date(2011, 11, 11);
		final long noIndividuLui = 12L;
		final long noIndividuElle = 6785L;

		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Pavarotti", "Luciano", Sexe.MASCULIN);
				final MockAdresse adrLui = addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, null, dateDepart);
				adrLui.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.EtatsUnis.getNoOFS(), null));

				final MockIndividu elle = addIndividu(noIndividuElle, null, "Tabadabada", "Tabata", Sexe.FEMININ);
				final MockAdresse adrElle = addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.Echallens.GrandRue, null, null, dateDepart);
				adrElle.setLocalisationSuivante(new Localisation(LocalisationType.HORS_SUISSE, MockPays.Colombie.getNoOFS(), null));
			}
		});

		// mise en place des individus HS
		class Ids {
			final long idLui;
			final long idElle;

			Ids(long idLui, long idElle) {
				this.idLui = idLui;
				this.idElle = idElle;
			}
		}
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = tiersService.createNonHabitantFromIndividu(noIndividuLui);
				final PersonnePhysique elle = tiersService.createNonHabitantFromIndividu(noIndividuElle);
				addForPrincipal(lui, date(2000, 1, 1), MotifFor.INDETERMINE, dateDepart, MotifFor.DEPART_HS, MockCommune.Cossonay);
				addForPrincipal(lui, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Allemagne);
				addForPrincipal(elle, date(2000, 1, 1), MotifFor.INDETERMINE, dateDepart, MotifFor.DEPART_HS, MockCommune.Echallens);
				addForPrincipal(elle, dateDepart.getOneDayAfter(), MotifFor.DEPART_HS, MockPays.Danemark);
				return new Ids(lui.getNumero(), elle.getNumero());
			}
		});

		// mariage
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.idElle);
				metierService.marie(dateMariage, lui, elle, null, EtatCivil.MARIE, null);
				return null;
			}
		});

		// vérification du résultat (c'est le for du contribuable principal qui détermine celui du couple)
		doInNewTransactionAndSession(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.idLui);
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(lui, dateMariage);
				Assert.assertNotNull(couple);
				Assert.assertNotNull(couple.getPrincipal());
				Assert.assertNotNull(couple.getConjoint());
				Assert.assertEquals((Long) ids.idLui, couple.getPrincipal().getNumero());
				Assert.assertEquals((Long) ids.idElle, couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				Assert.assertNotNull(mc);

				final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
				Assert.assertNotNull(ffp);
				Assert.assertEquals(dateMariage, ffp.getDateDebut());
				Assert.assertNull(ffp.getDateFin());
				Assert.assertEquals((Integer) MockPays.Allemagne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale());
				Assert.assertEquals(TypeAutoriteFiscale.PAYS_HS, ffp.getTypeAutoriteFiscale());
				return null;
			}
		});
	}

	/**
	 * [SIFISC-12290] NPE àa la sauvegarde dans le calcul de PIIS d'un des deux jeunes mariés (pour le calcul du flag de remboursement automatique) car le couple n'est pas encore en base
	 * (Cas de contribuables au rôle ordinaire)
	 */
	@Test
	public void testCreationNouveauCoupleOrdinaire() throws Exception {

		final long noIndividuLui = 26721454712L;
		final long noIndividuElle = 4378435L;
		final RegDate dateMariage = date(2014, 1, 5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuLui, null, "Katayama", "Igor", Sexe.MASCULIN);
				addIndividu(noIndividuElle, null, "Katayama", "Iko", Sexe.FEMININ);
			}
		});

		final class Ids {
			long lui;
			long elle;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);

				addForPrincipal(lui, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForPrincipal(elle, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);

				lui.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));
				elle.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));

				final Ids ids = new Ids();
				ids.lui = lui.getNumero();
				ids.elle = elle.getNumero();
				return ids;
			}
		});

		// mariage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);
				metierService.marie(dateMariage, lui, elle, null, EtatCivil.MARIE, null);
			}
		});

		// vérification du couple créé
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);

				assertTrue(lui.getBlocageRemboursementAutomatique());
				assertTrue(elle.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpLui = lui.getDernierForFiscalPrincipal();
				assertNotNull(ffpLui);
				assertEquals(dateMariage.getOneDayBefore(), ffpLui.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpLui.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffpLui.getModeImposition());

				final ForFiscalPrincipalPP ffpElle = elle.getDernierForFiscalPrincipal();
				assertNotNull(ffpElle);
				assertEquals(dateMariage.getOneDayBefore(), ffpElle.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpElle.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffpElle.getModeImposition());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(lui, null);
				assertNotNull(couple);
				assertEquals((Long) ids.lui, couple.getPrincipal().getNumero());
				assertEquals((Long) ids.elle, couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);
				assertFalse(mc.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateMariage, ffpMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpMc.getMotifOuverture());
				assertNull(ffpMc.getDateFin());
				assertNull(ffpMc.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffpMc.getModeImposition());
			}
		});
	}

	/**
	 * [SIFISC-12290] NPE àa la sauvegarde dans le calcul de PIIS d'un des deux jeunes mariés (pour le calcul du flag de remboursement automatique) car le couple n'est pas encore en base
	 * (Cas de contribuables sourciers purs)
	 */
	@Test
	public void testCreationNouveauCoupleSourciers() throws Exception {

		final long noIndividuLui = 26721454712L;
		final long noIndividuElle = 4378435L;
		final RegDate dateMariage = date(2014, 1, 5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuLui, null, "Katayama", "Igor", Sexe.MASCULIN);
				addIndividu(noIndividuElle, null, "Katayama", "Iko", Sexe.FEMININ);
			}
		});

		final class Ids {
			long lui;
			long elle;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);

				addForPrincipal(lui, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);
				addForPrincipal(elle, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);

				lui.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));
				elle.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));

				final Ids ids = new Ids();
				ids.lui = lui.getNumero();
				ids.elle = elle.getNumero();
				return ids;
			}
		});

		// mariage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);
				metierService.marie(dateMariage, lui, elle, null, EtatCivil.MARIE, null);
			}
		});

		// vérification du couple créé
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);

				// PIIS source pure -> pas bloqué
				assertFalse(lui.getBlocageRemboursementAutomatique());
				assertFalse(elle.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpLui = lui.getDernierForFiscalPrincipal();
				assertNotNull(ffpLui);
				assertEquals(dateMariage.getOneDayBefore(), ffpLui.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpLui.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffpLui.getModeImposition());

				final ForFiscalPrincipalPP ffpElle = elle.getDernierForFiscalPrincipal();
				assertNotNull(ffpElle);
				assertEquals(dateMariage.getOneDayBefore(), ffpElle.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpElle.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffpElle.getModeImposition());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(lui, null);
				assertNotNull(couple);
				assertEquals((Long) ids.lui, couple.getPrincipal().getNumero());
				assertEquals((Long) ids.elle, couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);
				assertFalse(mc.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateMariage, ffpMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpMc.getMotifOuverture());
				assertNull(ffpMc.getDateFin());
				assertNull(ffpMc.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffpMc.getModeImposition());
			}
		});
	}

	/**
	 * [SIFISC-12290] NPE àa la sauvegarde dans le calcul de PIIS d'un des deux jeunes mariés (pour le calcul du flag de remboursement automatique) car le couple n'est pas encore en base
	 * (Cas d'un contribuable sourcier pur alors que l'autre est au rôle ordinaire)
	 */
	@Test
	public void testCreationNouveauCoupleSourcierPlusOrdinaire() throws Exception {

		final long noIndividuLui = 26721454712L;
		final long noIndividuElle = 4378435L;
		final RegDate dateMariage = date(2014, 1, 5);

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				addIndividu(noIndividuLui, null, "Katayama", "Igor", Sexe.MASCULIN);
				addIndividu(noIndividuElle, null, "Katayama", "Iko", Sexe.FEMININ);
			}
		});

		final class Ids {
			long lui;
			long elle;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);

				addForPrincipal(lui, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle);
				addForPrincipal(elle, date(2000, 1, 1), MotifFor.ARRIVEE_HS, MockCommune.Aigle, ModeImposition.SOURCE);

				lui.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));
				elle.setCoordonneesFinancieres(new CoordonneesFinancieres("CH9308440717427290198", null));

				final Ids ids = new Ids();
				ids.lui = lui.getNumero();
				ids.elle = elle.getNumero();
				return ids;
			}
		});

		// mariage
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);
				metierService.marie(dateMariage, lui, elle, null, EtatCivil.MARIE, null);
			}
		});

		// vérification du couple créé
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.lui);
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.elle);

				// Lui est à l'ordinaire -> bloqué... Elle était à la source, mais ses PIIS ne sont plus "source pure" seulement -> bloqué
				assertTrue(lui.getBlocageRemboursementAutomatique());
				assertTrue(elle.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpLui = lui.getDernierForFiscalPrincipal();
				assertNotNull(ffpLui);
				assertEquals(dateMariage.getOneDayBefore(), ffpLui.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpLui.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffpLui.getModeImposition());

				final ForFiscalPrincipalPP ffpElle = elle.getDernierForFiscalPrincipal();
				assertNotNull(ffpElle);
				assertEquals(dateMariage.getOneDayBefore(), ffpElle.getDateFin());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpElle.getMotifFermeture());
				assertEquals(ModeImposition.SOURCE, ffpElle.getModeImposition());

				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(lui, null);
				assertNotNull(couple);
				assertEquals((Long) ids.lui, couple.getPrincipal().getNumero());
				assertEquals((Long) ids.elle, couple.getConjoint().getNumero());

				final MenageCommun mc = couple.getMenage();
				assertNotNull(mc);
				assertFalse(mc.getBlocageRemboursementAutomatique());

				final ForFiscalPrincipalPP ffpMc = mc.getDernierForFiscalPrincipal();
				assertNotNull(ffpMc);
				assertEquals(dateMariage, ffpMc.getDateDebut());
				assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffpMc.getMotifOuverture());
				assertNull(ffpMc.getDateFin());
				assertNull(ffpMc.getMotifFermeture());
				assertEquals(ModeImposition.ORDINAIRE, ffpMc.getModeImposition());
			}
		});
	}

	@Test
	public void testDecesEtEtiquettes() throws Exception {

		final RegDate dateDecesPrincipal = date(2000, 6, 14);
		final RegDate dateDecesConjoint = dateDecesPrincipal.addDays(54);

		// mise en place d'étiquettes qui réagissent au décès, et d'autres qui ne réagissent pas
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// on enlève d'abord toutes les étiquettes "automatiques" générées dans la construction par défaut des tests
				etiquetteDAO.getAll().forEach(hibernateTemplate::delete);

				// ajout des étiquettes qui nous intéressent
				{
					final Etiquette etiquette = addEtiquette("TOTO", "Décès PP 2Y fin mois", TypeTiersEtiquette.PP, null);
					etiquette.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.JOUR),
					                                                    new DecalageAvecCorrection(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_MOIS)));
				}
				{
					final Etiquette etiquette = addEtiquette("TITI", "Décès PM (aucun sens)", TypeTiersEtiquette.PM, null);
					etiquette.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.JOUR),
					                                                    new DecalageAvecCorrection(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_ANNEE)));
				}
				{
					final Etiquette etiquette = addEtiquette("TATA", "Décès PP sans date de fin", TypeTiersEtiquette.PP_MC, null);
					etiquette.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.SEMAINE), null));
				}
				{
					final Etiquette etiquette = addEtiquette("TÉTÉ", "inactive", TypeTiersEtiquette.PP_MC, null);
					etiquette.setActionSurDeces(new ActionAutoEtiquette(new Decalage(1, UniteDecalageDate.JOUR),
					                                                    new DecalageAvecCorrection(2, UniteDecalageDate.ANNEE, CorrectionSurDate.FIN_ANNEE)));
					etiquette.setActive(false);
				}
				{
					addEtiquette("TUTU", "Sans décès", TypeTiersEtiquette.PP_MC_PM, null);
				}
			}
		});

		final class Ids {
			long ppPrincipal;
			long ppConjoint;
			long mc;
		}

		// mise en place du ménage commun dont l'un des membre va mourir
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique prn = addNonHabitant("Albert", "Picolo", date(1942, 8, 13), Sexe.MASCULIN);
				final PersonnePhysique cjt = addNonHabitant("Germaine", "Picolo", date(1945, 12, 5), Sexe.FEMININ);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(prn, cjt, date(1967, 3, 1), null);
				final MenageCommun mc = couple.getMenage();

				final Ids ids = new Ids();
				ids.ppPrincipal = couple.getPrincipal().getNumero();
				ids.ppConjoint = couple.getConjoint().getNumero();
				ids.mc = mc.getNumero();
				return ids;
			}
		});

		// traitement du décès du principal
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique defunt = (PersonnePhysique) tiersService.getTiers(ids.ppPrincipal);
				Assert.assertNotNull(defunt);
				metierService.deces(defunt, dateDecesPrincipal, null, null);
			}
		});

		// vérification des étiquettes assignées aux différents tiers concernés
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// rien sur le ménage
				final MenageCommun menage = (MenageCommun) tiersService.getTiers(ids.mc);
				Assert.assertNotNull(menage);
				Assert.assertEquals(0, menage.getEtiquettes().size());

				// 2 étiquettes sur le principal (= défunt)
				final PersonnePhysique prn = (PersonnePhysique) tiersService.getTiers(ids.ppPrincipal);
				Assert.assertNotNull(prn);
				Assert.assertEquals(2, prn.getEtiquettes().size());
				final List<EtiquetteTiers> etiquettesPrincipal = prn.getEtiquettes().stream()
						.sorted(Comparator.comparing(e -> e.getEtiquette().getCode()))
						.collect(Collectors.toList());
				{
					final EtiquetteTiers etiquetteTiers = etiquettesPrincipal.get(0);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TATA", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(7), etiquetteTiers.getDateDebut());
					Assert.assertNull(etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
				{
					final EtiquetteTiers etiquetteTiers = etiquettesPrincipal.get(1);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TOTO", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(1), etiquetteTiers.getDateDebut());
					Assert.assertEquals(dateDecesPrincipal.addYears(2).getLastDayOfTheMonth(), etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}

				// 2 étiquettes (= les mêmes) sur le conjoint survivant
				final PersonnePhysique cjt = (PersonnePhysique) tiersService.getTiers(ids.ppConjoint);
				Assert.assertNotNull(cjt);
				Assert.assertEquals(2, cjt.getEtiquettes().size());
				final List<EtiquetteTiers> etiquettesConjoint = cjt.getEtiquettes().stream()
						.sorted(Comparator.comparing(e -> e.getEtiquette().getCode()))
						.collect(Collectors.toList());
				{
					final EtiquetteTiers etiquetteTiers = etiquettesConjoint.get(0);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TATA", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(7), etiquetteTiers.getDateDebut());
					Assert.assertNull(etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
				{
					final EtiquetteTiers etiquetteTiers = etiquettesConjoint.get(1);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TOTO", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(1), etiquetteTiers.getDateDebut());
					Assert.assertEquals(dateDecesPrincipal.addYears(2).getLastDayOfTheMonth(), etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
			}
		});

		// traitement du décès du conjoint quelques jours après
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus transactionStatus) throws Exception {
				final PersonnePhysique defunt = (PersonnePhysique) tiersService.getTiers(ids.ppConjoint);
				Assert.assertNotNull(defunt);
				metierService.deces(defunt, dateDecesConjoint, null, null);
			}
		});

		// vérification des étiquettes assignées aux différents tiers concernés
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				// toujours rien sur le ménage
				final MenageCommun menage = (MenageCommun) tiersService.getTiers(ids.mc);
				Assert.assertNotNull(menage);
				Assert.assertEquals(0, menage.getEtiquettes().size());

				// aucune différence sur le défunt précédent
				final PersonnePhysique prn = (PersonnePhysique) tiersService.getTiers(ids.ppPrincipal);
				Assert.assertNotNull(prn);
				Assert.assertEquals(2, prn.getEtiquettes().size());
				final List<EtiquetteTiers> etiquettesPrincipal = prn.getEtiquettes().stream()
						.sorted(Comparator.comparing(e -> e.getEtiquette().getCode()))
						.collect(Collectors.toList());
				{
					final EtiquetteTiers etiquetteTiers = etiquettesPrincipal.get(0);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TATA", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(7), etiquetteTiers.getDateDebut());
					Assert.assertNull(etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
				{
					final EtiquetteTiers etiquetteTiers = etiquettesPrincipal.get(1);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TOTO", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(1), etiquetteTiers.getDateDebut());
					Assert.assertEquals(dateDecesPrincipal.addYears(2).getLastDayOfTheMonth(), etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}

				// petit changement sur le nouveau défunt
				final PersonnePhysique cjt = (PersonnePhysique) tiersService.getTiers(ids.ppConjoint);
				Assert.assertNotNull(cjt);
				Assert.assertEquals(3, cjt.getEtiquettes().size());
				final Comparator<EtiquetteTiers> comparator = (et1, et2) -> {
					int comparison = et1.getEtiquette().getCode().compareTo(et2.getEtiquette().getCode());
					if (comparison == 0) {
						comparison = DateRangeComparator.compareRanges(et1, et2);
					}
					return comparison;
				};
				final List<EtiquetteTiers> etiquettesConjoint = cjt.getEtiquettes().stream()
						.sorted(comparator)
						.collect(Collectors.toList());
				{
					final EtiquetteTiers etiquetteTiers = etiquettesConjoint.get(0);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TATA", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(7), etiquetteTiers.getDateDebut());
					Assert.assertNull(etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
				{
					final EtiquetteTiers etiquetteTiers = etiquettesConjoint.get(1);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TOTO", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addDays(1), etiquetteTiers.getDateDebut());
					Assert.assertEquals(dateDecesPrincipal.addYears(2).getLastDayOfTheMonth(), etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
				{
					final EtiquetteTiers etiquetteTiers = etiquettesConjoint.get(2);
					final Etiquette etiquette = etiquetteTiers.getEtiquette();
					Assert.assertEquals("TOTO", etiquette.getCode());
					Assert.assertEquals(dateDecesPrincipal.addYears(2).getLastDayOfTheMonth().getOneDayAfter(), etiquetteTiers.getDateDebut());
					Assert.assertEquals(dateDecesConjoint.addYears(2).getLastDayOfTheMonth(), etiquetteTiers.getDateFin());
					Assert.assertFalse(etiquetteTiers.isAnnule());
					Assert.assertNull(etiquetteTiers.getCommentaire());
				}
			}
		});
	}
}
