package ch.vd.uniregctb.tache;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.util.ResourceUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockOfficeImpot;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.evenement.cedi.DossierElectroniqueHandler;
import ch.vd.uniregctb.evenement.cedi.EvenementCediEsbMessageHandler;
import ch.vd.uniregctb.evenement.cedi.EvenementCediService;
import ch.vd.uniregctb.evenement.cedi.V1Handler;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.parametrage.ParametreAppService;
import ch.vd.uniregctb.tache.sync.AddDIPP;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.CollectiviteAdministrative;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpotPM;
import ch.vd.uniregctb.type.CategorieEntreprise;
import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class TacheSynchronizerInterceptorTest extends BusinessTest {

	private TacheService tacheService;
	private TacheDAO tacheDAO;

	private ParametreAppService parametreAppService;
	private Integer oldPremierePeriodeFiscaleDeclarationsPersonnesMorales;

	private EvenementCediEsbMessageHandler cediListener;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		tacheService = getBean(TacheService.class, "tacheService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		parametreAppService = getBean(ParametreAppService.class, "parametreAppService");

		final EvenementCediService cediHandler = getBean(EvenementCediService.class, "evenementCediService");
		final V1Handler v1Handler = new V1Handler();
		v1Handler.setEvenementCediService(cediHandler);

		cediListener = new EvenementCediEsbMessageHandler();
		cediListener.setHandlers(Arrays.<DossierElectroniqueHandler<?>>asList(v1Handler));
		cediListener.setHibernateTemplate(hibernateTemplate);
		cediListener.afterPropertiesSet();

		// pour permettre la gestion de tâches avant 2016 tant qu'on n'est pas encore en 2016
		oldPremierePeriodeFiscaleDeclarationsPersonnesMorales = parametreAppService.getPremierePeriodeFiscaleDeclarationsPersonnesMorales();
		parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(2013);
	}

	@Override
	public void onTearDown() throws Exception {
		// restauration de la valeur en base une fois les tests terminés
		if (oldPremierePeriodeFiscaleDeclarationsPersonnesMorales != null) {
			parametreAppService.setPremierePeriodeFiscaleDeclarationsPersonnesMorales(oldPremierePeriodeFiscaleDeclarationsPersonnesMorales);
		}
		super.onTearDown();
	}

	/**
	 * [UNIREG-2894] Teste que la synchronisation des tâches après l'arrivée d'un message Cedi se déroule correctement et notamment qu'il n'y a pas de  problème d'authentification nulle.
	 */
	@Test
	public void testSynchronizeTachesOnEsbMessage() throws Exception {

		final long CTB_ID = 12500001L;

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/tache/event_retour_di.xml");
		final String texte = FileUtils.readFileToString(file);

		// crée un contribuable assujetti entre 2008 et 2009, avec une seule déclaration 2009 (celle de 2008 manque) et qui ne possède pas de tâche d'envoi de DI (= création automatique à la prochaine modification)
		setWantSynchroTache(false); // on désactive l'intercepteur pour éviter de créer les tâches d'envoi automatiquement
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {

				// les périodes fiscales qui vont bien
				for (int annee = 2003; annee < 2009; annee++) {
					final PeriodeFiscale periode = addPeriodeFiscale(annee);
					addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode);
					addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode);
				}
				final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
				addModeleDocument(TypeDocument.DECLARATION_IMPOT_VAUDTAX, periode2009);
				final ModeleDocument modele2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);

				// et enfin le contribuable
				final PersonnePhysique ralf = addNonHabitant(CTB_ID, "Ralf", "Leboet", date(1960, 1, 1), Sexe.MASCULIN);
				addForPrincipal(ralf, date(2008, 1, 1), MotifFor.ARRIVEE_HC, date(2010, 6, 30), MotifFor.DEPART_HC, MockCommune.Aubonne);
				addDeclarationImpot(ralf, periode2009, date(2009, 1, 1), date(2009, 12, 31), TypeContribuable.VAUDOIS_ORDINAIRE, modele2009);
				
				return null;
			}
		});

		// on s'assure qu'il manque bien une tâche d'envoi de DI pour 2008 sur le contribuable
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final PersonnePhysique ralf = hibernateTemplate.get(PersonnePhysique.class, CTB_ID);
				assertNotNull(ralf);
				try {
					final List<SynchronizeAction> list = tacheService.determineSynchronizeActionsForDIs(ralf);
					assertNotNull(list);
					assertEquals(1, list.size());
					final AddDIPP action = (AddDIPP) list.get(0);
					assertEquals(date(2008, 1, 1), action.periodeImposition.getDateDebut());
					assertEquals(date(2008, 12, 31), action.periodeImposition.getDateFin());
				}
				catch (AssujettissementException e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});

		setWantSynchroTache(true); // on l'intercepteur pour maintenant crééer les tâches d'envoi automatiquement

		assertTrue(AuthenticationHelper.hasCurrentPrincipal());
		final String principal = AuthenticationHelper.getCurrentPrincipal();
		AuthenticationHelper.resetAuthentication();
		try {
			// on s'assure qu'on est dans le même mode que dans l'environnement web : dans une transaction et sans authentification
			assertFalse(AuthenticationHelper.hasCurrentPrincipal());

			// simule l'envoi d'un événement de retour de DI sur le contribuable n°12500001
			// (cet événement de retour de DI est normalement englobé dans une transaction JTA gérée par Géronimo)
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					try {
						final EsbMessage message = EsbMessageFactory.createMessage();
						message.setBody(texte);
						cediListener.onEsbMessage(message);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					return null;
				}
			});
		}
		finally {
			AuthenticationHelper.pushPrincipal(principal);
		}

		// on s'assure que la tâche a bien été créée
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				
				final List<Tache> list = tacheDAO.find(CTB_ID);
				assertNotNull(list);
				assertEquals(1, list.size());
				
				final TacheEnvoiDeclarationImpot tache = (TacheEnvoiDeclarationImpot) list.get(0);
				assertNotNull(tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertEquals(date(2008, 1, 1), tache.getDateDebut());
				assertEquals(date(2008, 12, 31), tache.getDateFin());
				assertEquals("AutoSynchro", tache.getLogCreationUser());
				return null;
			}
		});
	}

	/**
	 * Vérification que la catégorie d'entreprise est bien mise à jour sur une tâche d'envoi de DI PM
	 */
	@Test
	public void testModificationTacheEnvoiDeclarationPM() throws Exception {

		final RegDate dateDebut = date(2014, 1, 1);
		final RegDate dateFin = date(2014, 12, 31);

		// création d'une entreprise avec une tâche d'envoi de DI dans laquelle la catégorie d'entreprise n'est pas la bonne
		final long idpm = doInNewTransactionAndSessionUnderSwitch(tacheSynchronizer, true, new TransactionCallback<Long>() {
			@Override
			public Long doInTransaction(TransactionStatus status) {
				final CollectiviteAdministrative oipm = tiersService.getCollectiviteAdministrative(MockOfficeImpot.OID_PM.getNoColAdm());
				final Entreprise entreprise = addEntrepriseInconnueAuCivil();
				addRaisonSociale(entreprise, dateDebut, null, "Truc machin SA");
				addFormeJuridique(entreprise, dateDebut, null, FormeJuridiqueEntreprise.SA);
				addBouclement(entreprise, dateDebut, DayMonth.get(12, 31), 12);
				addForPrincipal(entreprise, dateDebut, MotifFor.DEBUT_EXPLOITATION, dateFin, MotifFor.FIN_EXPLOITATION, MockCommune.Aubonne);

				// une SA est de catégorie PM, pas SP...
				addTacheEnvoiDIPM(TypeEtatTache.EN_INSTANCE, null, dateDebut, dateFin, dateDebut, dateFin, TypeContribuable.VAUDOIS_ORDINAIRE, TypeDocument.DECLARATION_IMPOT_PM, entreprise, CategorieEntreprise.SP, oipm);
				return entreprise.getNumero();
			}
		});

		// normalement, la synchronisation a été lancée et a corrigé la catégorie d'entreprise dans la tâche
		doInNewTransactionAndSession(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				final List<Tache> taches = tacheDAO.find(idpm);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final Tache tache = taches.get(0);
				assertNotNull(tache);
				assertInstanceOf(TacheEnvoiDeclarationImpotPM.class, tache);
				assertEquals(TypeEtatTache.EN_INSTANCE, tache.getEtat());
				assertFalse(tache.isAnnule());

				final TacheEnvoiDeclarationImpotPM tacheEnvoi = (TacheEnvoiDeclarationImpotPM) tache;
				assertEquals(dateDebut, tacheEnvoi.getDateDebut());
				assertEquals(dateFin, tacheEnvoi.getDateFin());
				assertEquals(dateDebut, tacheEnvoi.getDateDebutExercice());
				assertEquals(dateFin, tacheEnvoi.getDateFinExercice());
				assertEquals(TypeDocument.DECLARATION_IMPOT_PM, tacheEnvoi.getTypeDocument());
				assertEquals(TypeContribuable.VAUDOIS_ORDINAIRE, tacheEnvoi.getTypeContribuable());
				assertEquals(CategorieEntreprise.PM, tacheEnvoi.getCategorieEntreprise());          // valeur corrigée !
			}
		});
	}
}
