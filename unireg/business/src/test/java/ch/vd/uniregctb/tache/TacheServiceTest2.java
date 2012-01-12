package ch.vd.uniregctb.tache;

import java.util.List;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.common.BusinessTestingConstants;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.ModeleDocument;
import ch.vd.uniregctb.declaration.PeriodeFiscale;
import ch.vd.uniregctb.interfaces.model.mock.MockCollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheAnnulationDeclarationImpot;
import ch.vd.uniregctb.tiers.TacheDAO;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
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
public class TacheServiceTest2 extends BusinessTest {

	private TacheDAO tacheDAO;
	private DeclarationImpotOrdinaireDAO diDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		tiersService = getBean(TiersService.class, "tiersService");
		tacheDAO = getBean(TacheDAO.class, "tacheDAO");
		diDAO = getBean(DeclarationImpotOrdinaireDAO.class, "diDAO");

		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				for (MockCollectiviteAdministrative ca : MockCollectiviteAdministrative.getAll()) {
					addCollAdm(ca);
				}
				return null;
			}
		});

		setWantSynchroTache(true);
	}

	@Override
	protected boolean useTiersServiceToCreateDeclarationImpot() {
		return false;
	}

	/**
	 * [SIFISC-3141] Cas du contribuable n°100.486.64 où deux déclarations existent pour une seule période d'imposition (données incohérentes)
	 */
	@Test
	public void testSynchronizeTachesSurContribuableAvecDeuxDeclarationsPourUnePeriodeImposition() throws Exception {

		final class Ids {
			Long ppId;
			Long diHSId;
			Long diHCId;
		}
		final Ids ids = new Ids();

		// Création d'un contribuable avec deux déclarations pour une période d'imposition
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final PersonnePhysique pp = addNonHabitant("Jean", "Rappel", date(1954, 12, 3), Sexe.MASCULIN);
				addForPrincipal(pp, date(2006, 1, 26), MotifFor.INDETERMINE, date(2009, 7, 11), MotifFor.ARRIVEE_HS, MockPays.Danemark);
				addForPrincipal(pp, date(2009, 7, 12), MotifFor.ARRIVEE_HS, date(2009, 12, 27), MotifFor.DEPART_HC, MockCommune.Bussigny);
				addForPrincipal(pp, date(2009, 12, 28), MotifFor.DEPART_HC, MockCommune.Neuchatel);
				addForSecondaire(pp, date(2006, 1, 26), MotifFor.ACHAT_IMMOBILIER, date(2010, 12, 31), MotifFor.VENTE_IMMOBILIER, MockCommune.Aigle.getNoOFS(), MotifRattachement.IMMEUBLE_PRIVE);

				final PeriodeFiscale periode2009 = addPeriodeFiscale(2009);
				final ModeleDocument modeleHS2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, periode2009);
				final ModeleDocument modeleHC2009 = addModeleDocument(TypeDocument.DECLARATION_IMPOT_HC_IMMEUBLE, periode2009);
				final DeclarationImpotOrdinaire diHS = addDeclarationImpot(pp, periode2009, date(2009, 1, 1), date(2009, 7, 11), TypeContribuable.HORS_SUISSE, modeleHS2009);
				final DeclarationImpotOrdinaire diHC = addDeclarationImpot(pp, periode2009, date(2009, 7, 12), date(2009, 12, 31), TypeContribuable.HORS_CANTON, modeleHC2009);

				ids.ppId = pp.getId();
				ids.diHSId = diHS.getId();
				ids.diHCId = diHC.getId();
				return null;
			}
		});

		// Vérification des tâches créées
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// on vérifie qu'il existe bien une tâche d'annulation de la deuxième déclaration (la hors-canton)
				final List<Tache> taches = tacheDAO.find(ids.ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				final TacheAnnulationDeclarationImpot tache0 = (TacheAnnulationDeclarationImpot) taches.get(0);
				assertTacheAnnulationDI(TypeEtatTache.EN_INSTANCE, ids.diHCId, false, tache0);

				// on vérifie que les déclarations pour 2009 restent inchangées (car la première déclaration n'est toujours pas annulée et
				// donc que la deuxième déclaration ne peut pas être modifiée sans provoquer une erreur de validation)
				final Contribuable ctb = (Contribuable) tiersDAO.get(ids.ppId);
				assertNotNull(ctb);

				final List<Declaration> declarations = ctb.getDeclarationsForPeriode(2009, false);
				assertNotNull(declarations);
				assertEquals(2, declarations.size());

				final Declaration decl0 = declarations.get(0);
				assertEquals(ids.diHSId, decl0.getId());
				assertEquals(date(2009, 1, 1), decl0.getDateDebut());
				assertEquals(date(2009, 7, 11), decl0.getDateFin());
				assertFalse(decl0.isAnnule());

				final Declaration decl1 = declarations.get(1);
				assertEquals(ids.diHCId, decl1.getId());
				assertEquals(date(2009, 7, 12), decl1.getDateDebut());
				assertEquals(date(2009, 12, 31), decl1.getDateFin());
				assertFalse(decl1.isAnnule());

				return null;
			}
		});

		// Annulation de la seconde déclaration
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// on traite la tâche d'annulation
				final List<Tache> taches = tacheDAO.find(ids.ppId);
				final TacheAnnulationDeclarationImpot tache0 = (TacheAnnulationDeclarationImpot) taches.get(0);
				tache0.setEtat(TypeEtatTache.TRAITE);

				final DeclarationImpotOrdinaire diHC = diDAO.get(ids.diHCId);
				diHC.setAnnule(true);

				return null;
			}
		});

		// Vérification des tâches et déclarations
		doInNewTransactionAndSession(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				// on vérifie qu'il existe bien la tâche d'annulation est traitée
				final List<Tache> taches = tacheDAO.find(ids.ppId);
				assertNotNull(taches);
				assertEquals(1, taches.size());

				// on traite la tâche d'annulation
				final TacheAnnulationDeclarationImpot tache0 = (TacheAnnulationDeclarationImpot) taches.get(0);
				assertTacheAnnulationDI(TypeEtatTache.TRAITE, ids.diHCId, false, tache0);

				// on vérifie que :
				//  - la déclaration hors-canton pour 2009 est bien annulée
				//  - la déclaration hors-Suisse pour 2009 a bien été transformée en hors-canton et étendue sur toute l'année
				final Contribuable ctb = (Contribuable) tiersDAO.get(ids.ppId);
				assertNotNull(ctb);

				final List<Declaration> declarations = ctb.getDeclarationsForPeriode(2009, true);
				assertNotNull(declarations);
				assertEquals(2, declarations.size());

				final Declaration decl0 = declarations.get(0);
				assertEquals(ids.diHSId, decl0.getId());
				assertEquals(date(2009, 1, 1), decl0.getDateDebut());
				assertEquals(date(2009, 12, 31), decl0.getDateFin());
				assertEquals(TypeDocument.DECLARATION_IMPOT_COMPLETE_BATCH, decl0.getModeleDocument().getTypeDocument());
				assertFalse(decl0.isAnnule());

				final Declaration decl1 = declarations.get(1);
				assertEquals(ids.diHCId, decl1.getId());
				assertEquals(date(2009, 7, 12), decl1.getDateDebut());
				assertEquals(date(2009, 12, 31), decl1.getDateFin());
				assertTrue(decl1.isAnnule());

				return null;
			}
		});
	}
}
