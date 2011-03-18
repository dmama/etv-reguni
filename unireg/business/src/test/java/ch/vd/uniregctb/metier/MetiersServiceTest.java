package ch.vd.uniregctb.metier;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.interfaces.model.AttributeIndividu;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.mock.MockAdresse;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockNationalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.mock.DefaultMockServiceCivil;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParType;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypePermis;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

	//UNIREG-2771 La fusion doit être empéchée s'il existe au moins un for ou une Di non annule 

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
		doInTransaction(new TxCallback() {
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

					final ForFiscalPrincipal f = addForPrincipal(menage,dateMariageAlfredo,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
							date(2009,3,1),MotifFor.INDETERMINE, MockCommune.Lausanne, MotifRattachement.DOMICILE);
					f.setModeImposition(ModeImposition.ORDINAIRE);
					menage.setBlocageRemboursementAutomatique(false);
				}

				// Armando
				final PersonnePhysique armando =  addNonHabitant("Armando", "Dunant", date(1970, 1, 1), Sexe.MASCULIN);

				// ménage Armando
				{
					MenageCommun menage = new MenageCommun();
					menage = (MenageCommun) tiersDAO.save(menage);
					ids.noMenageArmando = menage.getNumero();
					tiersService.addTiersToCouple(menage, armando, dateMariageArmando, null);

					final ForFiscalPrincipal f = addForPrincipal(menage,dateMariageArmando,MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION,
							date(2009,3,1),MotifFor.INDETERMINE, MockCommune.Lausanne, MotifRattachement.DOMICILE);

					f.setModeImposition(ModeImposition.ORDINAIRE);
					menage.setBlocageRemboursementAutomatique(false);
				}
				return null;
			}
		});
try {
			metierService.fusionneMenages((MenageCommun) tiersDAO.get(ids.noMenageAlfredo), (MenageCommun) tiersDAO.get(ids.noMenageArmando), null, EtatCivil.LIE_PARTENARIAT_ENREGISTRE);
			ch.vd.registre.base.utils.Assert.fail();
		 }
		 catch (EvenementCivilHandlerException e){
		  ch.vd.registre.base.utils.Assert.hasText(e.getMessage());
		}
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

	@Test
	@NotTransactional
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
				addNationalite(m, MockPays.RoyaumeUni, dateNaissanceM, null, 1);
				addNationalite(mme, MockPays.RoyaumeUni, dateNaissanceMme, null, 1);
				addPermis(m, TypePermis.ETABLISSEMENT, dateMariage, null, 1, false);
				addPermis(mme, TypePermis.ETABLISSEMENT, dateMariage, null, 1, false);
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
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
					public void modifyIndividu(MockIndividu individu) {
						individu.setDateDeces(dateDeces);
					}
				});

				metierService.deces(m, dateDeces, "Pour le test", null);
				return null;
			}
		});

		// vérification des fors et des blocages de remboursement automatique après décès
		doInNewTransactionAndSession(new TransactionCallback() {
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// on ne connait que l'adresse courrier sur Genève (= prise par défaut pour l'adresse de domicile)
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateSeparation, null));
					}
				});

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
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
	 */
	@Test
	public void testSeparationAdresseDomicileOuCourrierHorsSuisseDepuisMemePf() throws Exception {

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
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final RegDate dateDepart = dateSeparation.addDays(-20);
						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateDepart);

						// on ne connait que l'adresse courrier à Paris (= prise par défaut pour l'adresse de domicile)
						individu.getAdresses()
								.add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, "5 Avenue des Champs-Elysées", null, "75017 Paris", MockPays.France, dateDepart.addDays(1), null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				try {
					metierService.separe(mc, dateSeparation, "test", null, true, null);
					fail("La séparation aurait dû partir en erreur puisque l'on passe pour Georgette d'un couple vaudois à un for hors-Suisse");
				}
				catch (EvenementCivilHandlerException e) {
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
					}
				});

				// changement d'adresse de Georgette, mais seulement sur l'adresse COURRIER (et ce depuis un an déjà au moment de la séparation -> période fiscale différente)
				doModificationIndividu(noIndGeorgette, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.addYears(-1).getOneDayBefore());

						// on ne connait que l'adresse courrier à Paris (= prise par défaut pour l'adresse de domicile)
						individu.getAdresses()
								.add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, "5 Avenue des Champs-Elysées", null, "75017 Paris", MockPays.France, dateSeparation.addYears(-1), null));
					}
				});

				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);

				metierService.separe(mc, dateSeparation, "test", null, true, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Fabrice à Bex, Georgette à Paris

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

		// For ouvert sur Georgette : à Paris, car l'adresse courrier est utilisée en lieu et place de l'adresse de domicile, inconnue
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

	/**
	 * Teste que la séparation d'un ménage pour lequel les adresses des membres du couple sont différentes crée bien les fors au bon endroit
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
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
					public void modifyIndividu(MockIndividu individu) {
						assertNotNull(individu);
						assertNotNull(individu.getAdresses());
						assertEquals(1, individu.getAdresses().size());

						final MockAdresse adresse = (MockAdresse) individu.getAdresses().iterator().next();
						assertNull(adresse.getDateFin());
						assertEquals(dateMariage, adresse.getDateDebut());
						adresse.setDateFinValidite(dateSeparation.getOneDayBefore());

						// domicile passe à Bex -> le for devra s'ouvrir là
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, dateSeparation, null));
						individu.getAdresses().add(MockServiceCivil.newAdresse(TypeAdresseCivil.COURRIER, MockRue.Bussigny.RueDeLIndustrie, null, dateSeparation, null));
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Grandvaux.RueSaintGeorges, null, dateMariage, dateSeparation.getOneDayBefore());
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Riex.RouteDeLaCorniche, null, dateSeparation, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Grandvaux.RueSaintGeorges, null, dateMariage, null);

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

				addForPrincipal(menage, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Grandvaux);
				return null;
			}
		});

		// Sépare les époux. Monsieur déménage alors à Riex et Madame garde le camion de Ken
		doInTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
				assertNotNull(mc);
				metierService.separe(mc, dateSeparation, "test", null, true, null);
				return null;
			}
		});

		// vérifie les fors principaux ouverts sur les séparés : Monsieur à Riex, Madame à Grandvaux

		// For fermé sur le couple
		{
			final MenageCommun mc = (MenageCommun) tiersDAO.get(ids.menage);
			assertNotNull(mc);

			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertForPrincipal(dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, dateSeparation.getOneDayBefore(), MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT,
					MockCommune.Grandvaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
		}

		// For ouvert sur Monsieur : à Riex, car sa nouvelle adresse de domicile est là-bas
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ids.fabrice);
			assertNotNull(fabrice);

			final ForFiscalPrincipal ffp = fabrice.getDernierForFiscalPrincipal();
			assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Riex, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
		}

		// For ouvert sur Georgette : à Grandvaux, car son domicile n'a pas changé
		{
			final PersonnePhysique georgette = (PersonnePhysique) tiersDAO.get(ids.georgette);
			assertNotNull(georgette);

			final ForFiscalPrincipal ffp = georgette.getDernierForFiscalPrincipal();
			assertForPrincipal(dateSeparation, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, MockCommune.Grandvaux, MotifRattachement.DOMICILE, ModeImposition.ORDINAIRE, ffp);
		}
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateMariage.addMonths(10));
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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
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
				final Nationalite nationaliteFabrice = new MockNationalite(naissanceFabrice, null, MockPays.Suisse, 1);
				fabrice.setNationalites(Arrays.asList(nationaliteFabrice));
				addAdresse(fabrice, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, null);

				final MockIndividu georgette = addIndividu(noIndGeorgette, naissanceGeorgette, "Dunant", "Georgette", false);
				final Nationalite nationaliteGeorgette = new MockNationalite(naissanceGeorgette, null, MockPays.Suisse, 1);
				georgette.setNationalites(Arrays.asList(nationaliteGeorgette));
				addAdresse(georgette, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.AvenueDeMarcelin, null, dateMariage, dateMariage.addMonths(10));
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
				doModificationIndividu(noIndFabrice, new IndividuModification() {
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
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
				addEtatCivil(fabrice, dateNaissance, TypeEtatCivil.CELIBATAIRE);
				addEtatCivil(fabrice, dateMariage, TypeEtatCivil.MARIE);
				addEtatCivil(fabrice, dateSeparation, TypeEtatCivil.SEPARE);
				addEtatCivil(fabrice, dateVeuvage, TypeEtatCivil.VEUF);
			}
		});

		// mise en place fiscale
		final long ppId = (Long) doInNewTransaction(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus transactionStatus) {
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
		{
			final PersonnePhysique fabrice = (PersonnePhysique) tiersDAO.get(ppId);
			final ValidationResults resultatValidation = metierService.validateVeuvage(fabrice, dateVeuvage);
			Assert.assertNotNull(resultatValidation);
			Assert.assertEquals(1, resultatValidation.errorsCount());

			final String erreur = resultatValidation.getErrors().get(0);
			Assert.assertEquals("L'individu veuf n'a ni couple connu ni for valide à la date de veuvage : problème d'assujettissement ?", erreur);
		}
	}

	@Test
	@NotTransactional
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
				addNationalite(mr, MockPays.Suisse, date(1948, 1, 26), null, 1);
				addNationalite(mme, MockPays.Suisse, date(1948, 9, 4), null, 1);
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
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique mr = (PersonnePhysique) tiersService.getTiers(ids.mrId);
				metierService.deces(mr, dateDeces, "Décès de Monsieur", 1L);
				return null;
			}
		});

		// vérification du résultat
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
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
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		// décès fiscal
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDecesFiscal, "Décédé", null);
				return null;
			}
		});

		// vérification de l'impact du décès fiscal (ne doit pas être passé non-habitant!)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());
				return null;
			}
		});
	}

	/**
	 * Cas UNIREG-2653 le décès fiscal d'un habitant également décédé au civil doit faire passer la personne physique en non-habitant
	 */
	@Test
	@NotTransactional
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
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noInd);
				return pp.getNumero();
			}
		});

		// décès fiscal
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", null);
				return null;
			}
		});

		// vérification de l'impact du décès fiscal (doit être passé non-habitant!)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				return null;
			}
		});
	}

	/**
	 * UNIREG-2653 une annulation de décès dans le civil doit repasser la personne physique en habitant
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
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndDecede);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", 1L);
				return null;
			}
		});

		// vérification de l'impact fiscal
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));
				return null;
			}
		});

		// annulation du décès dans le civil déjà
		doModificationIndividu(noIndDecede, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(null);
			}
		});

		// impact fiscal de l'annulation de décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.annuleDeces(pp, dateDeces);
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès
		doInNewTransactionAndSession(new TransactionCallback() {
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
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndDecede);
				tiersService.changeHabitantenNH(pp);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", 1L);
				return null;
			}
		});

		// vérification de l'impact fiscal
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));
				return null;
			}
		});

		// annulation du décès dans le civil déjà
		doModificationIndividu(noIndDecede, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(null);
			}
		});

		// impact fiscal de l'annulation de décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				metierService.annuleDeces(pp, dateDeces);
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès (la personne habite hors-canton, elle ne doit donc pas passer habitante!)
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertNull(tiersService.getDateDeces(pp));
				return null;
			}
		});
	}

	/**
	 * UNIREG-2653 une annulation de décès seulement fiscale ne doit pas repasser la personne physique en habitant
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
		final long ppId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = addHabitant(noIndDecede);
				return pp.getNumero();
			}
		});

		// décès au civil
		doModificationIndividu(noIndDecede, new IndividuModification() {
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateDeces(dateDeces);
			}
		});

		// impact fiscal du décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertTrue(pp.isHabitantVD());

				metierService.deces(pp, dateDeces, "Décédé", 1L);
				return null;
			}
		});

		// vérification de l'impact fiscal et annulation fiscale du décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertEquals(dateDeces, tiersService.getDateDeces(pp));

				metierService.annuleDeces(pp, dateDeces);
				return null;
			}
		});

		// vérification de l'impact fiscal de l'annulation du décès
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique pp = (PersonnePhysique) tiersDAO.get(ppId);
				Assert.assertFalse(pp.isHabitantVD());
				Assert.assertNull(tiersService.getDateDeces(pp));
				return null;
			}
		});
	}

	@Test
	@NotTransactional
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
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = addHabitant(noIndividuMonsieur);
				addForPrincipal(m, date(2000, 4, 1), MotifFor.ARRIVEE_HS, MockCommune.Lausanne);
				addForSecondaire(m, date(2004, 4, 28), MotifFor.ACHAT_IMMOBILIER, dateMariage.getOneDayBefore(), MotifFor.VENTE_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(),
						MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addHabitant(noIndividuMadame);
				addForPrincipal(mme, date(2002, 8, 12), MotifFor.ARRIVEE_HS, MockCommune.Renens);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, null, EtatCivil.MARIE, true, null);
				Assert.assertNotNull(mc);

				final Ids ids = new Ids();
				ids.idMonsieur = m.getNumero();
				ids.idMadame = mme.getNumero();
				ids.idMenage = mc.getNumero();
				return ids;
			}
		});

		// et on vérifie les fors après l'enregistrement du mariage
		doInNewTransactionAndSession(new TransactionCallback() {
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
	@NotTransactional
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

				addNationalite(m, MockPays.Suisse, date(1972, 11, 4), null, 1);
				addNationalite(mme, MockPays.France, date(1977, 8, 16), null, 1);
				addPermis(mme, TypePermis.ANNUEL, date(2002, 7, 18), null, 1, false);
				marieIndividus(m, mme, dateMariage);
			}
		});

		final class Ids {
			long idMonsieur;
			long idMadame;
			long idMenage;
		}

		// mise en place fiscale
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

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
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

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
	@NotTransactional
	public void testMariageAvecForsSecondairesIdentiques() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires identiques", EtatCivil.MARIE, false, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principaux);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principaux.size());
				Assert.assertEquals(1, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principaux.get(0);
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
				Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) ffs.getNumeroOfsAutoriteFiscale());
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
	@NotTransactional
	public void testMariageAvecForsSecondairesDifferentsParCommune() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Croy.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires différents sur la commune", EtatCivil.MARIE, false, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principaux);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principaux.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principaux.get(0);
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

					if (ffs.getNumeroOfsAutoriteFiscale() == MockCommune.Echallens.getNoOFSEtendu()) {
						Assert.assertFalse("Double for à Echallens trouvé", foundEchallens);
						foundEchallens = true;
					}
					else if (ffs.getNumeroOfsAutoriteFiscale() == MockCommune.Croy.getNoOFSEtendu()) {
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
	@NotTransactional
	public void testMariageAvecForsSecondairesDifferentsParMotifRattachement() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut, MotifFor.DEBUT_EXPLOITATION, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.ACTIVITE_INDEPENDANTE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires différents selon leur motif de rattachement", EtatCivil.MARIE, false, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principaux);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principaux.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principaux.get(0);
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
					Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) ffs.getNumeroOfsAutoriteFiscale());

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
	@NotTransactional
	public void testMariageAvecForsSecondairesDifferentsParDateOuverture() throws Exception {

		final class Ids {
			long m;
			long mme;
		}

		// mise en place
		final Ids ids = (Ids) doInNewTransactionAndSession(new TransactionCallback() {
			public Ids doInTransaction(TransactionStatus status) {

				final RegDate dateDebut = date(2000, 1, 1);

				final PersonnePhysique m = addNonHabitant("Vernon", "Dursley", date(1975, 8, 31), Sexe.MASCULIN);
				addForPrincipal(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(m, dateDebut, MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final PersonnePhysique mme = addNonHabitant("Petunia", "Dursley", date(1976, 10, 4), Sexe.FEMININ);
				addForPrincipal(mme, dateDebut.addMonths(1), MotifFor.ACHAT_IMMOBILIER, MockPays.RoyaumeUni);
				addForSecondaire(mme, dateDebut.addMonths(1), MotifFor.ACHAT_IMMOBILIER, MockCommune.Echallens.getNoOFSEtendu(), MotifRattachement.IMMEUBLE_PRIVE);

				final Ids ids = new Ids();
				ids.m = m.getNumero();
				ids.mme = mme.getNumero();
				return ids;
			}
		});

		final RegDate dateMariage = date(2008, 5, 1);

		// maintenant, on va marier les tourtereaux
		final long mcId = (Long) doInNewTransactionAndSession(new TransactionCallback() {
			public Long doInTransaction(TransactionStatus status) {

				final PersonnePhysique m = (PersonnePhysique) tiersService.getTiers(ids.m);
				Assert.assertNotNull(m);

				final PersonnePhysique mme = (PersonnePhysique) tiersService.getTiers(ids.mme);
				Assert.assertNotNull(mme);

				final MenageCommun mc = metierService.marie(dateMariage, m, mme, "Mariage avec fors secondaires identiques", EtatCivil.MARIE, false, null);
				return mc.getNumero();
			}
		});

		// et on vérifie les fors créés sur le couple
		doInNewTransactionAndSession(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {

				final MenageCommun mc = (MenageCommun) tiersService.getTiers(mcId);
				Assert.assertNotNull(mc);

				final ForsParType fors = mc.getForsParType(false);
				Assert.assertNotNull(fors);
				Assert.assertNotNull(fors.principaux);
				Assert.assertNotNull(fors.secondaires);
				Assert.assertEquals(1, fors.principaux.size());
				Assert.assertEquals(2, fors.secondaires.size());

				final ForFiscalPrincipal ffp = fors.principaux.get(0);
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
					Assert.assertEquals(MockCommune.Echallens.getNoOFSEtendu(), (int) ffs.getNumeroOfsAutoriteFiscale());
					Assert.assertEquals(dateMariage, ffs.getDateDebut());
					Assert.assertEquals(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, ffs.getMotifOuverture());
					Assert.assertNull(ffs.getDateFin());
					Assert.assertNull(ffs.getMotifFermeture());
				}
				return null;
			}
		});
	}
}
