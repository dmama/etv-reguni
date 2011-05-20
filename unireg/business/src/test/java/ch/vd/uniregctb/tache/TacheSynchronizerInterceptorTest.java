package ch.vd.uniregctb.tache;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.test.annotation.NotTransactional;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.util.ResourceUtils;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.EsbMessageFactory;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.evenement.cedi.EvenementCediHandler;
import ch.vd.uniregctb.evenement.cedi.EvenementCediListenerImpl;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.tache.sync.AddDI;
import ch.vd.uniregctb.tache.sync.SynchronizeAction;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TacheEnvoiDeclarationImpot;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings({"JavaDoc"})
@ContextConfiguration(locations = {
		BusinessTestingConstants.UNIREG_BUSINESS_UT_TACHES
})
public class TacheSynchronizerInterceptorTest extends BusinessTest {

	private EsbMessageFactory esbMessageFactory;
	private TacheService tacheService;
	private TacheDAO tacheDAO;

	private EvenementCediListenerImpl cediListener;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		final EvenementCediHandler cediHandler = getBean(EvenementCediHandler.class, "evenementCediService");
		esbMessageFactory = new EsbMessageFactory();
		tacheService = getBean(TacheService.class, "tacheService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");

		cediListener = new EvenementCediListenerImpl();
		cediListener.setHandler(cediHandler);
		cediListener.setHibernateTemplate(hibernateTemplate);
	}

	/**
	 * [UNIREG-2894] Teste que la synchronisation des tâches après l'arrivée d'un message Cedi se déroule correctement et notamment qu'il n'y a pas de  problème d'authentification nulle.
	 */
	@NotTransactional
	@Test
	public void testSynchronizeTachesOnEsbMessage() throws Exception {

		final long CTB_ID = 12500001L;

		final File file = ResourceUtils.getFile("classpath:ch/vd/uniregctb/tache/event_retour_di.xml");
		final String texte = FileUtils.readFileToString(file);

		// crée un contribuable assujetti entre 2008 et 2009, avec une seule déclaration 2009 (celle de 2008 manque) et qui ne possède pas de tâche d'envoi de DI (= création automatique à la prochaine modification)
		setWantSynchroTache(false); // on désactive l'intercepteur pour éviter de créer les tâches d'envoi automatiquement
		doInNewTransaction(new TransactionCallback<Object>() {
			public Object doInTransaction(TransactionStatus status) {

				// des trucs d'infrastructure
				addCollAdm(ServiceInfrastructureService.noCEDI);
				addCollAdm(MockOfficeImpot.OID_ROLLE_AUBONNE);
				addCollAdm(MockOfficeImpot.ACISUCCESSIONS);

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
			public Object doInTransaction(TransactionStatus status) {
				final Contribuable ralf = (Contribuable) hibernateTemplate.get(Contribuable.class, CTB_ID);
				assertNotNull(ralf);
				try {
					final List<SynchronizeAction> list = tacheService.determineSynchronizeActionsForDIs(ralf);
					assertNotNull(list);
					assertEquals(1, list.size());
					final AddDI action = (AddDI) list.get(0);
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

		final String principal = AuthenticationHelper.getCurrentPrincipal();
		AuthenticationHelper.resetAuthentication();
		try {
			// on s'assure qu'on est dans le même mode que dans l'environnement web : dans une transaction et sans authentification
			assertFalse(AuthenticationHelper.isAuthenticated());

			// simule l'envoi d'un événement de retour de DI sur le contribuable n°12500001
			// (cet événement de retour de DI est normalement englobé dans une transaction JTA gérée par Géronimo)
			doInNewTransaction(new TransactionCallback<Object>() {
				public Object doInTransaction(TransactionStatus status) {
					try {
						final EsbMessage message = esbMessageFactory.createMessage();
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
			AuthenticationHelper.setPrincipal(principal);
		}

		// on s'assure que la tâche a bien été créée
		doInNewTransaction(new TransactionCallback<Object>() {
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
}
