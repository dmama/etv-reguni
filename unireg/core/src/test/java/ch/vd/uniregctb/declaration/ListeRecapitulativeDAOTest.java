package ch.vd.uniregctb.declaration;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.type.CategorieImpotSource;
import ch.vd.uniregctb.type.ModeCommunication;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@SuppressWarnings({"JavaDoc"})
public class ListeRecapitulativeDAOTest extends CoreDAOTest {

	protected static final Logger LOGGER = Logger.getLogger(ListeRecapitulativeDAOTest.class);

	private static final String DAO_NAME = "lrDAO";

	private static final String TOUS = "TOUS";

	private static final String DB_UNIT_DATA_FILE = "ListeRecapitulativeDAOTest.xml";

	/**
	 * Le DAO.
	 */
	private ListeRecapitulativeDAO lrDao;

	/**
	 * @throws Exception
	 *
	 */
	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		lrDao = getBean(ListeRecapitulativeDAO.class, DAO_NAME);
	}

	/**
	 * Teste la methode qui recherche les LRs suivant certains criteres
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFind() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		ListeRecapCriteria criterion = new ListeRecapCriteria();
		criterion.setPeriodicite(PeriodiciteDecompte.MENSUEL.toString());
        RegDate dateDebutPeriode = RegDate.get(2008, 1, 1);
		criterion.setPeriode(dateDebutPeriode);
		criterion.setModeCommunication(ModeCommunication.PAPIER.toString());
		criterion.setEtat(TypeEtatDeclaration.EMISE.toString());
		criterion.setCategorie(CategorieImpotSource.ADMINISTRATEURS.toString());
		List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
		assertNotNull(lrs);
		assertEquals(1, lrs.size());
	}

	/**
	 * Teste la methode qui renvoi les LRs d'un contribuable
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindByNumero() throws Exception {
		loadDatabase(DB_UNIT_DATA_FILE);
		List<DeclarationImpotSource> lrs = lrDao.findByNumero(12500001L);
		assertNotNull(lrs);
		assertEquals(2, lrs.size());
	}

	/**
	 * Teste que la methode qui renvoi les informations sur l'etat
	 * de la derniere LR envoyee pour un contribuable donne
	 */
	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testFindDerniereLrEnvoyee() throws Exception {

		loadDatabase(DB_UNIT_DATA_FILE);
		EtatDeclaration etat = lrDao.findDerniereLrEnvoyee(12500001L);
		assertNotNull(etat);
		assertEquals(TypeEtatDeclaration.EMISE, etat.getEtat());
		assertEquals(new Long(12500001), etat.getDeclaration().getTiers().getNumero());
	}

	public ListeRecapitulativeDAO getLrDao() {
		return lrDao;
	}

	public void setLrDao(ListeRecapitulativeDAO lrDao) {
		this.lrDao = lrDao;
	}

	@Test
	public void testFindSurEtatAvancementRetourne() throws Exception {

		final int annee = 2010;

		// mise en place
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setNom1("Débiteur de test");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

				// LR émise pour janvier
				{
					final RegDate debut = date(annee, 1, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
				}

				// LR émise et retournée pour février
				{
					final RegDate debut = date(annee, 2, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationRetournee(lr, fin);
				}

				// LR émise puis sommée pour mars
				{
					final RegDate debut = date(annee, 3, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
				}

				// LR émise, sommée et finalement retournée pour avril
				{
					final RegDate debut = date(annee, 4, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate retour = sommation.addDays(15);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationRetournee(lr, retour);
				}

				// LR émise, sommée et échue pour mai
				{
					final RegDate debut = date(annee, 5, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
				}

				// LR émise, sommée, échue et finalement retournée pour juin
				{
					final RegDate debut = date(annee, 6, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);
					final RegDate retour = echeance.addDays(5);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
					addEtatDeclarationRetournee(lr, retour);
				}

				return null;
			}
		});

		// et maintenant, la recherche
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ListeRecapCriteria criterion = new ListeRecapCriteria();
				criterion.setEtat(TypeEtatDeclaration.RETOURNEE.name());
				criterion.setCategorie(TOUS);
				criterion.setModeCommunication(TOUS);
				criterion.setPeriodicite(TOUS);

				final List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
				assertNotNull(lrs);
				assertEquals(3, lrs.size());

				final DeclarationImpotSource[] array = new DeclarationImpotSource[6];
				for (DeclarationImpotSource lr : lrs) {
					assertNotNull(lr);
					final int index = lr.getDateDebut().month();
					assertNull("Deux LR pour le même mois... : " + index, array[index - 1]);
					array[index - 1] = lr;
				}

				// les LR retournées sont celles de février, avril et juin
				assertNull("Janvier n'est pas retournée", array[0]);
				assertNotNull("Février est retournée", array[1]);
				assertNull("Mars n'est pas retournée", array[2]);
				assertNotNull("Avril est retournée", array[3]);
				assertNull("Mai n'est pas retournée", array[4]);
				assertNotNull("Juin est retournée", array[5]);
				return null;
			}
		});
	}

	@Test
	public void testFindSurEtatAvancementEmis() throws Exception {

		final int annee = 2010;

		// mise en place
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setNom1("Débiteur de test");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

				// LR émise pour janvier
				{
					final RegDate debut = date(annee, 1, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
				}

				// LR émise et retournée pour février
				{
					final RegDate debut = date(annee, 2, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationRetournee(lr, fin);
				}

				// LR émise puis sommée pour mars
				{
					final RegDate debut = date(annee, 3, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
				}

				// LR émise, sommée et finalement retournée pour avril
				{
					final RegDate debut = date(annee, 4, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate retour = sommation.addDays(15);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationRetournee(lr, retour);
				}

				// LR émise, sommée et échue pour mai
				{
					final RegDate debut = date(annee, 5, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
				}

				// LR émise, sommée, échue et finalement retournée pour juin
				{
					final RegDate debut = date(annee, 6, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);
					final RegDate retour = echeance.addDays(5);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
					addEtatDeclarationRetournee(lr, retour);
				}

				return null;
			}
		});

		// et maintenant, la recherche
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ListeRecapCriteria criterion = new ListeRecapCriteria();
				criterion.setEtat(TypeEtatDeclaration.EMISE.name());
				criterion.setCategorie(TOUS);
				criterion.setModeCommunication(TOUS);
				criterion.setPeriodicite(TOUS);

				final List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = lrs.get(0);
				assertNotNull(lr);
				assertEquals(date(annee, 1, 1), lr.getDateDebut());     // celle de janvier seulement
				return null;
			}
		});
	}

	@Test
	public void testFindSurEtatAvancementSomme() throws Exception {

		final int annee = 2010;

		// mise en place
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setNom1("Débiteur de test");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

				// LR émise pour janvier
				{
					final RegDate debut = date(annee, 1, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
				}

				// LR émise et retournée pour février
				{
					final RegDate debut = date(annee, 2, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationRetournee(lr, fin);
				}

				// LR émise puis sommée pour mars
				{
					final RegDate debut = date(annee, 3, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
				}

				// LR émise, sommée et finalement retournée pour avril
				{
					final RegDate debut = date(annee, 4, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate retour = sommation.addDays(15);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationRetournee(lr, retour);
				}

				// LR émise, sommée et échue pour mai
				{
					final RegDate debut = date(annee, 5, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
				}

				// LR émise, sommée, échue et finalement retournée pour juin
				{
					final RegDate debut = date(annee, 6, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);
					final RegDate retour = echeance.addDays(5);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
					addEtatDeclarationRetournee(lr, retour);
				}

				return null;
			}
		});

		// et maintenant, la recherche
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ListeRecapCriteria criterion = new ListeRecapCriteria();
				criterion.setEtat(TypeEtatDeclaration.SOMMEE.name());
				criterion.setCategorie(TOUS);
				criterion.setModeCommunication(TOUS);
				criterion.setPeriodicite(TOUS);

				final List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = lrs.get(0);
				assertNotNull(lr);
				assertEquals(date(annee, 3, 1), lr.getDateDebut());     // celle de mars seulement
				return null;
			}
		});
	}

	@Test
	public void testFindSurEtatAvancementEchu() throws Exception {

		final int annee = 2010;

		// mise en place
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final DebiteurPrestationImposable dpi = addDebiteur();
				dpi.setNom1("Débiteur de test");

				final PeriodeFiscale pf = addPeriodeFiscale(annee);
				final ModeleDocument modeleLr = addModeleDocument(TypeDocument.LISTE_RECAPITULATIVE, pf);

				// LR émise pour janvier
				{
					final RegDate debut = date(annee, 1, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
				}

				// LR émise et retournée pour février
				{
					final RegDate debut = date(annee, 2, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationRetournee(lr, fin);
				}

				// LR émise puis sommée pour mars
				{
					final RegDate debut = date(annee, 3, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
				}

				// LR émise, sommée et finalement retournée pour avril
				{
					final RegDate debut = date(annee, 4, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate retour = sommation.addDays(15);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationRetournee(lr, retour);
				}

				// LR émise, sommée et échue pour mai
				{
					final RegDate debut = date(annee, 5, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
				}

				// LR émise, sommée, échue et finalement retournée pour juin
				{
					final RegDate debut = date(annee, 6, 1);
					final RegDate fin = debut.addMonths(1).getOneDayBefore();
					final RegDate emission = debut.addDays(20);
					final RegDate sommation = emission.addDays(50);
					final RegDate echeance = sommation.addDays(45);
					final RegDate retour = echeance.addDays(5);

					final DeclarationImpotSource lr = addListeRecapitulative(dpi, pf, debut, fin, modeleLr);
					addEtatDeclarationEmise(lr, emission);
					addEtatDeclarationSommee(lr, sommation, sommation);
					addEtatDeclarationEchue(lr, echeance);
					addEtatDeclarationRetournee(lr, retour);
				}

				return null;
			}
		});

		// et maintenant, la recherche
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final ListeRecapCriteria criterion = new ListeRecapCriteria();
				criterion.setEtat(TypeEtatDeclaration.ECHUE.name());
				criterion.setCategorie(TOUS);
				criterion.setModeCommunication(TOUS);
				criterion.setPeriodicite(TOUS);

				final List<DeclarationImpotSource> lrs = lrDao.find(criterion, null);
				assertNotNull(lrs);
				assertEquals(1, lrs.size());

				final DeclarationImpotSource lr = lrs.get(0);
				assertNotNull(lr);
				assertEquals(date(annee, 5, 1), lr.getDateDebut());     // celle de mai seulement
				return null;
			}
		});
	}
}
