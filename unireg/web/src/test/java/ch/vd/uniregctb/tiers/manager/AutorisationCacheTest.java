package ch.vd.uniregctb.tiers.manager;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.tx.TxCallbackWithoutResult;
import ch.vd.unireg.interfaces.civil.data.Localisation;
import ch.vd.unireg.interfaces.civil.data.LocalisationType;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.uniregctb.common.WebTest;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceSecuriteService;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.tiers.DecisionAci;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertTrue;

public class AutorisationCacheTest extends WebTest {

	private AutorisationCacheImpl cache;

	@Override
	protected void runOnSetUp() throws Exception {
		super.runOnSetUp();

		cache = getBean(AutorisationCacheImpl.class, "autorisationCache");
		cache.reset();
	}

	/**
	 * [SIFISC-12035] La situation est la suivante :
	 * <ol>
	 *     <li>Monsieur et Madame sont résidents</li>
	 *     <li>Monsieur part hors-Canton --> devient non-résident, mais pour autant, comme le couple reste vaudois (Madame est toujours là), les adresses de Monsieur ne sont pas éditables par le profile OID</li>
	 *     <li>Madame part hors-Canton --> devient non-résidente, et le couple voit son for modifié ; le couple n'est alors plus vaudois et les adresses des trois éléments doivent maintenant être
	 *     éditables par un profile OID</li>
	 * </ol>
	 * <p/>
	 * Dans la situation du cas jira, les adresses de Monsieur restaient non-éditables par le profile OID le temps de l'expiration du cache (celles de Madame et du couple étaient éditables immédiatement)
	 */
	@Test
	public void testEvictionPersonnePhysiqueDepuisMenage() throws Exception {

		final String visaOperateur = "XXXXX";
		final long noIndividuLui = 452120L;
		final long noIndividuElle = 4564121L;
		final RegDate dateMariage = date(2001, 5, 1);
		final RegDate dateDepart = date(2013, 8, 31);

		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateur, 42L, Role.VISU_ALL, Role.MODIF_HC_HS, Role.ADR_PP_D, Role.ADR_PP_C, Role.ADR_PP_B);
			}
		});

		// mise en place civile
		serviceCivil.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				final MockIndividu lui = addIndividu(noIndividuLui, null, "Duchnok", "Federico", Sexe.MASCULIN);
				final MockIndividu elle = addIndividu(noIndividuElle, null, "Duchnok", "Albertina", Sexe.FEMININ);
				marieIndividus(lui, elle, dateMariage);
				addAdresse(lui, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addAdresse(elle, TypeAdresseCivil.PRINCIPALE, MockRue.CossonayVille.AvenueDuFuniculaire, null, dateMariage, null);
				addNationalite(lui, MockPays.Suisse, dateMariage, null);
				addNationalite(elle, MockPays.Suisse, dateMariage, null);
			}
		});

		final class Ids {
			long ppLui;
			long ppElle;
			long ppMenageCommun;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique lui = addHabitant(noIndividuLui);
				final PersonnePhysique elle = addHabitant(noIndividuElle);
				final EnsembleTiersCouple couple = addEnsembleTiersCouple(lui, elle, dateMariage, null);
				final MenageCommun mc = couple.getMenage();
				addForPrincipal(mc, dateMariage, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, MockCommune.Cossonay);

				final Ids ids = new Ids();
				ids.ppLui = lui.getNumero();
				ids.ppElle = elle.getNumero();
				ids.ppMenageCommun = mc.getNumero();
				return ids;
			}
		});

		// contenu du cache -> vide
		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));

		// pas de droit de modifier les adresses des résidents, c'est normal
		Assert.assertFalse(cache.getAutorisations(ids.ppLui, visaOperateur, 5).isAdresses());
		Assert.assertFalse(cache.getAutorisations(ids.ppElle, visaOperateur, 5).isAdresses());
		Assert.assertFalse(cache.getAutorisations(ids.ppMenageCommun, visaOperateur, 5).isAdresses());

		// contenu du cache -> plein
		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));

		// départ de Monsieur, partie civile
		doModificationIndividu(noIndividuLui, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				final MockAdresse oldAddress = (MockAdresse) individu.getAdresses().iterator().next();
				oldAddress.setDateFinValidite(dateDepart);
				oldAddress.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));

				final MockAdresse newAddress = new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
				newAddress.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Cossonay.getNoOFS(), null));
				individu.addAdresse(newAddress);
			}
		});

		// départ de Monsieur, partie fiscale
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				final PersonnePhysique lui = (PersonnePhysique) tiersDAO.get(ids.ppLui);
				final TiersService.UpdateHabitantFlagResultat res = tiersService.updateHabitantFlag(lui, noIndividuLui, null);
				Assert.assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, res);
				Assert.assertFalse(lui.isHabitantVD());
			}
		});

		// contenu du cache -> le cache de monsieur a dû être effacé (mais pas les autres)
		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));

		// pas de droit de modifier les adresses des résidents, c'est normal (même si Monsieur est maintenant non-résident, son couple l'est toujours, donc toujours pas de droits)
		Assert.assertFalse(cache.getAutorisations(ids.ppLui, visaOperateur, 5).isAdresses());
		Assert.assertFalse(cache.getAutorisations(ids.ppElle, visaOperateur, 5).isAdresses());
		Assert.assertFalse(cache.getAutorisations(ids.ppMenageCommun, visaOperateur, 5).isAdresses());

		// contenu du cache -> plein (vient d'être re-rempli pour Monsieur)
		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));

		// départ de Madame, partie civile
		doModificationIndividu(noIndividuElle, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				// recalcul du flag "habitant" sur Madame
				final MockAdresse oldAddress = (MockAdresse) individu.getAdresses().iterator().next();
				oldAddress.setDateFinValidite(dateDepart);
				oldAddress.setLocalisationSuivante(new Localisation(LocalisationType.HORS_CANTON, MockCommune.Geneve.getNoOFS(), null));

				final MockAdresse newAddress = new MockAdresse(TypeAdresseCivil.COURRIER, MockRue.Geneve.AvenueGuiseppeMotta, null, dateDepart.getOneDayAfter(), null);
				newAddress.setLocalisationPrecedente(new Localisation(LocalisationType.CANTON_VD, MockCommune.Cossonay.getNoOFS(), null));
				individu.addAdresse(newAddress);
			}
		});

		// départ de Madame, partie fiscale
		doInNewTransactionAndSession(new TxCallbackWithoutResult() {
			@Override
			public void execute(TransactionStatus status) throws Exception {
				// recalcul du flag "habitant" sur Madame
				final PersonnePhysique elle = (PersonnePhysique) tiersDAO.get(ids.ppElle);
				final TiersService.UpdateHabitantFlagResultat res = tiersService.updateHabitantFlag(elle, noIndividuElle, null);
				Assert.assertEquals(TiersService.UpdateHabitantFlagResultat.CHANGE_EN_NONHABITANT, res);
				Assert.assertFalse(elle.isHabitantVD());

				// tout le monde est parti, il faut donc maintenant déplacer le for principal du couple aussi
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(elle, dateMariage);
				final MenageCommun mc = couple.getMenage();
				tiersService.closeForFiscalPrincipal(mc, dateDepart, MotifFor.DEPART_HC);
				tiersService.openForFiscalPrincipal(mc, dateDepart.getOneDayAfter(), MotifRattachement.DOMICILE, MockCommune.Geneve.getNoOFS(), TypeAutoriteFiscale.COMMUNE_HC, ModeImposition.ORDINAIRE, MotifFor.DEPART_HC);
			}
		});

		// contenu du cache -> le cache de tout le monde a dû être effacé
		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));

		// maintenant on a le droit de modifier les adresses (tout le monde est non-résident)
		Assert.assertTrue(cache.getAutorisations(ids.ppLui, visaOperateur, 5).isAdresses());
		Assert.assertTrue(cache.getAutorisations(ids.ppElle, visaOperateur, 5).isAdresses());
		Assert.assertTrue(cache.getAutorisations(ids.ppMenageCommun, visaOperateur, 5).isAdresses());

		// contenu du cache -> plein (vient d'être re-rempli pour Monsieur)
		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateur, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppMenageCommun, visaOperateur, 5));
	}

	@Test
	public void testEvictionContribuableDepuisCtbAvecDecisionAci() throws Exception {

		final String visaOperateurAvecDroitDecision = "XXXXX";
		final String visaOperateurSansDroitDecision = "YYYYY";


		// extrait du profile OID
		serviceSecurite.setUp(new MockServiceSecuriteService() {
			@Override
			protected void init() {
				addOperateur(visaOperateurAvecDroitDecision, 42L, Role.VISU_ALL, Role.GEST_DECISION_ACI, Role.MODIF_VD_ORD, Role.MODIF_HAB_DEBPUR, Role.FOR_AUTRE);
				addOperateur(visaOperateurSansDroitDecision, 42L, Role.VISU_ALL, Role.MODIF_VD_ORD, Role.MODIF_HAB_DEBPUR, Role.FOR_AUTRE);
			}
		});


		final long noIndividuLui = 236723537L;
		final long noIndividuElle = 231923587L;
		final long noIndividuElleEx = 236736537L;
		final class IdsDecision {
			public Long idOriginal;
			Long idNouvel;
		}
		final class Ids {
			long ppLui;
			long ppElle;
			long ppElleEx;
			long menageCommun;
			long menageCommunEx;
		}

		// mise en place fiscale
		final Ids ids = doInNewTransactionAndSession(new TransactionCallback<Ids>() {
			@Override
			public Ids doInTransaction(TransactionStatus status) {
				final PersonnePhysique m = addNonHabitant("Albert", "Dubourg", null, Sexe.MASCULIN);
				final PersonnePhysique mm = addNonHabitant("Juliane", "Dubourg", null, Sexe.FEMININ);
				final PersonnePhysique mmEx = addNonHabitant("Valentine", "Dubourg", null, Sexe.FEMININ);
				final int anneeFinAncienCouple = RegDate.get().year() - 4;
				final int anneeDebutNouveauCouple = RegDate.get().year() - 3;
				EnsembleTiersCouple etcEx = addEnsembleTiersCouple(m, mmEx, date(2001, 3, 7), date(anneeFinAncienCouple, 5, 1));
				EnsembleTiersCouple etc = addEnsembleTiersCouple(m, mm, date(anneeDebutNouveauCouple, 5, 7), null);
				final MenageCommun mc = etc.getMenage();
				final MenageCommun mcEx = etcEx.getMenage();
				final Ids ids = new Ids();
				ids.ppLui = m.getNumero();
				ids.ppElle = mm.getNumero();
				ids.ppElleEx = mmEx.getNumero();
				ids.menageCommun = mc.getNumero();
				ids.menageCommunEx = mcEx.getNumero();

				return ids;
			}
		});

		// mise en place decision
		final IdsDecision idsDecision = doInNewTransactionAndSession(new TransactionCallback<IdsDecision>() {
			@Override
			public IdsDecision doInTransaction(TransactionStatus status) {
				final PersonnePhysique personneEx = (PersonnePhysique) tiersDAO.get(ids.ppElleEx);
				DecisionAci d = addDecisionAci(personneEx, date(2013, 11, 1), null, MockCommune.Lausanne.getNoOFS(), TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, null);
				final IdsDecision idDec = new IdsDecision();
				idDec.idOriginal = d.getId();
				return idDec;
			}
		});


		// vérification de la situation sur le nouveau couple,
		//tout le monde sous influence de la décision aci posé sur l'ex, c'est la nouvelle femme qui va être contente ....
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique ppLui = (PersonnePhysique) tiersDAO.get(ids.ppLui);
				Assert.assertNotNull(ppLui);
				assertTrue(tiersService.isSousInfluenceDecisions(ppLui));

				final MenageCommun nouveauMc = (MenageCommun) tiersDAO.get(ids.menageCommun);
				Assert.assertNotNull(nouveauMc);
				assertTrue(tiersService.isSousInfluenceDecisions(nouveauMc));

				final PersonnePhysique ppElle = (PersonnePhysique) tiersDAO.get(ids.ppElle);
				Assert.assertNotNull(ppElle);
				assertTrue(tiersService.isSousInfluenceDecisions(ppElle));
			}
		});

		// contenu du cache -> vide
		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElleEx, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommun, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5));

		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElleEx, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommun, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommunEx, visaOperateurSansDroitDecision, 5));

		// droit de modification du fiscal, c'est normal
		Assert.assertTrue(cache.getAutorisations(ids.ppLui, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElle, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElleEx, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommun, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());

		//Pas le droit de modif sur le fiscal à cause présence de la decision
		Assert.assertFalse(cache.getAutorisations(ids.ppLui, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertFalse(cache.getAutorisations(ids.ppElle, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertFalse(cache.getAutorisations(ids.ppElleEx, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertFalse(cache.getAutorisations(ids.menageCommun, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertFalse(cache.getAutorisations(ids.menageCommunEx, visaOperateurSansDroitDecision, 5).isDonneesFiscales());

		// contenu du cache -> plein
		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElleEx, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommun, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5));

		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElleEx, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommun, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommunEx, visaOperateurSansDroitDecision, 5));

		//fermeture Decision
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				final PersonnePhysique personneEx = (PersonnePhysique) tiersDAO.get(ids.ppElleEx);
				DecisionAci d = personneEx.getDecisionsSorted().get(0);
				final RegDate dateFin = date(RegDate.get().year() - 2, 12, 31);
				d.setDateFin(dateFin);

			}
		});

		//Cache vide pour tout le monde
		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElleEx, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommun, visaOperateurAvecDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5));

		Assert.assertFalse(cache.hasCachedData(ids.ppLui, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElle, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.ppElleEx, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommun, visaOperateurSansDroitDecision, 5));
		Assert.assertFalse(cache.hasCachedData(ids.menageCommunEx, visaOperateurSansDroitDecision, 5));
		// droit de modification du fiscal, c'est normal
		Assert.assertTrue(cache.getAutorisations(ids.ppLui, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElle, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElleEx, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommun, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5).isDonneesFiscales());

		//droit de modif sur le fiscal car decision aci fermée
		Assert.assertTrue(cache.getAutorisations(ids.ppLui, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElle, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.ppElleEx, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommun, visaOperateurSansDroitDecision, 5).isDonneesFiscales());
		Assert.assertTrue(cache.getAutorisations(ids.menageCommunEx, visaOperateurSansDroitDecision, 5).isDonneesFiscales());

		// contenu du cache -> plein
		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElleEx, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommun, visaOperateurAvecDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommunEx, visaOperateurAvecDroitDecision, 5));

		Assert.assertTrue(cache.hasCachedData(ids.ppLui, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElle, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.ppElleEx, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommun, visaOperateurSansDroitDecision, 5));
		Assert.assertTrue(cache.hasCachedData(ids.menageCommunEx, visaOperateurSansDroitDecision, 5));


	}

}
