package ch.vd.uniregctb.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class DebiteurPrestationImposableTest extends CoreDAOTest {

	private TiersDAO dao;

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
			assertEquals(ent, dpi.getContribuable());
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
			assertEquals(ent, dpi.getContribuable());
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
			assertEquals(ent, dpi.getContribuable());
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
}
