package ch.vd.uniregctb.tiers;

import java.util.List;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.declaration.Periodicite;
import ch.vd.uniregctb.declaration.PeriodiciteDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

public class DebiteurPrestationImposableTest extends CoreDAOTest {

	private TiersDAO dao;
	private PeriodiciteDAO periodiciteDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testAddContribuableExistingEntreprise() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Créée une Entreprise
				{
					Entreprise ent = new Entreprise();
					ent.setNumero(1234L);
					ent.setAdresseBicSwift("Bla");
					dao.save(ent);
				}
				return null;
			}
		});

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				// Ajoute un DPI
				{
					Entreprise ent = (Entreprise) dao.getAll().get(0);

					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					dpi = (DebiteurPrestationImposable)dao.save(dpi);

					ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ent, dpi);
					dao.getHibernateTemplate().merge(contact);
				}
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(2, l.size());

			Entreprise ent = null;
			DebiteurPrestationImposable dpi = null;
			for (Tiers t : l) {
				if (t instanceof Entreprise) {
					ent = (Entreprise) t;
					assertEquals(new Long(1234L), ent.getNumero());
				}
				else {
					dpi = (DebiteurPrestationImposable) t;
				}
			}
			assertNotNull(ent);
			assertNotNull(dpi);
			assertEquals(ent.getId(), dpi.getContribuableId());
		}
	}

	@Test
	public void testAddContribuableNewEntreprise() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				{
					Entreprise ent = new Entreprise();
					ent.setNumero(1235L);
					ent.setAdresseBicSwift("Bla");
					ent = (Entreprise) dao.save(ent);

					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					dpi = (DebiteurPrestationImposable) dao.save(dpi);

					ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ent, dpi);
					dao.getHibernateTemplate().merge(contact);
				}
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(2, l.size());

			Entreprise ent = null;
			DebiteurPrestationImposable dpi = null;
			for (Tiers t : l) {
				if (t instanceof Entreprise) {
					ent = (Entreprise) t;
					assertEquals(new Long(1235L), ent.getNumero());
				}
				else {
					dpi = (DebiteurPrestationImposable) t;
				}
			}
			assertNotNull(ent);
			assertNotNull(dpi);
			assertEquals(ent.getId(), dpi.getContribuableId());
		}

	}

	@Test
	public void testAddContribuableNewAutreCommunaute() throws Exception {

		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				{
					AutreCommunaute ac = new AutreCommunaute();
					ac.setNom("Bla bli");
					ac.setAdresseBicSwift("Bla");
					ac = (AutreCommunaute) dao.save(ac);

					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					dpi = (DebiteurPrestationImposable) dao.save(dpi);

					ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
					dao.getHibernateTemplate().merge(contact);
				}
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(2, l.size());

			AutreCommunaute ent = null;
			DebiteurPrestationImposable dpi = null;
			for (Tiers t : l) {
				if (t instanceof AutreCommunaute) {
					ent = (AutreCommunaute) t;
				}
				else {
					dpi = (DebiteurPrestationImposable) t;
				}
			}
			assertNotNull(ent);
			assertNotNull(dpi);
			assertEquals(ent.getId(), dpi.getContribuableId());
		}

	}

	/**
	 * Cas où un for intermédiaire est ouvert (date de fin = null).
	 */
	@Test
	public void testDetectionChevauchementForsDebiteurForIntermediateOuvert() {

		DebiteurPrestationImposable debiteur = new DebiteurPrestationImposable();
		{
			ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2003, 12, 1));
			forFiscal.setDateFin(date(2004, 8, 11));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5601); // Chexbres
			debiteur.addForFiscal(forFiscal);
		}
		{
			ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2004, 8, 12));
			forFiscal.setDateFin(date(2006, 10, 1));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5890); // Vevey
			debiteur.addForFiscal(forFiscal);
		}
		{ // ce for intermédiaire est ouvert => il doit entrer en conflit avec le for suivant
			ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 2));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5889); // La Tour-de-Peilz
			debiteur.addForFiscal(forFiscal);
		}
		{
			ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2006, 10, 3));
			forFiscal.setDateFin(date(2007, 3, 30));
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5889); // La Tour-de-Peilz
			debiteur.addForFiscal(forFiscal);
		}
		{
			ForDebiteurPrestationImposable forFiscal = new ForDebiteurPrestationImposable();
			forFiscal.setDateDebut(date(2007, 3, 31));
			forFiscal.setDateFin(null);
			forFiscal.setGenreImpot(GenreImpot.DEBITEUR_PRESTATION_IMPOSABLE);
			forFiscal.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			forFiscal.setNumeroOfsAutoriteFiscale(5886); // Montreux
			debiteur.addForFiscal(forFiscal);
		}

		assertEquals(1, debiteur.validate().errorsCount());
	}

	@Test
	public void testAjoutPeriodicite() throws Exception {
	doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				{

					DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
					Periodicite periodicite = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL);
					periodicite.setDateDebut(date(2010,1,1));
					dpi.addPeriodicite(periodicite);
					dpi = (DebiteurPrestationImposable) dao.save(dpi);


					
				}
				return null;
			}
		});

		{
			List<Tiers> l = dao.getAll();
			assertEquals(1, l.size());

			AutreCommunaute ent = null;
			DebiteurPrestationImposable dpi = (DebiteurPrestationImposable)l.get(0);
			assertNotNull(dpi.getPeriodiciteAt(null));
			
		}
	}

	@Test
	public void testDateDesactivation() throws Exception {

		// pas de for -> pas désactivé
		final DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
		assertNull(dpi.getDateDesactivation());

		// un for créé et ouvert -> toujours pas désactivé
		final ForDebiteurPrestationImposable f1 = new ForDebiteurPrestationImposable();
		f1.setDateDebut(date(2000, 4, 12));
		f1.setNumeroOfsAutoriteFiscale(1234);
		f1.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi.addForFiscal(f1);
		assertNull(dpi.getDateDesactivation());

		// for fermé -> désactivé
		final RegDate dateFin1 = date(2002, 8, 15);
		f1.setDateFin(dateFin1);
		assertEquals(dateFin1, dpi.getDateDesactivation());

		// nouvelle ouverture de for -> ré-activé
		final ForDebiteurPrestationImposable f2 = new ForDebiteurPrestationImposable();
		f2.setDateDebut(date(2003, 4, 12));
		f2.setNumeroOfsAutoriteFiscale(1234);
		f2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi.addForFiscal(f2);
		assertNull(dpi.getDateDesactivation());

		// for fermé -> désactivé
		final RegDate dateFin2 = date(2004, 12, 1);
		f2.setDateFin(dateFin2);
		assertEquals(dateFin2, dpi.getDateDesactivation());

		// annulation du dernier for -> désactivation re-devient sur la fin du premier
		f2.setAnnule(true);
		assertEquals(dateFin1, dpi.getDateDesactivation());
	}

	@Test
	public void testValidateTiersAnnule() {

		final DebiteurPrestationImposable tiers = new DebiteurPrestationImposable();

		// Tiers invalide (périodicité avec date de début nulle) mais annulé => pas d'erreur
		{
			tiers.addPeriodicite(new Periodicite());
			tiers.setAnnule(true);
			Assert.assertFalse(tiers.validate().hasErrors());
		}

		// Tiers valide et annulée => pas d'erreur
		{
			tiers.getPeriodicites().clear();
			tiers.setAnnule(true);
			Assert.assertFalse(tiers.validate().hasErrors());
		}
	}
}
