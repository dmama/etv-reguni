package ch.vd.unireg.tiers;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.CoreDAOTest;
import ch.vd.unireg.declaration.Periodicite;
import ch.vd.unireg.declaration.PeriodiciteDAO;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DebiteurPrestationImposableTest extends CoreDAOTest {

	private TiersDAO dao;
	private PeriodiciteDAO periodiciteDAO;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAddContribuableExistingEntreprise() throws Exception {

		doInNewTransaction(status -> {
			// Créée une Entreprise
			{
				Entreprise ent = new Entreprise();
				ent.setNumero(1234L);
				dao.save(ent);
			}
			return null;
		});

		doInNewTransaction(status -> {
			// Ajoute un DPI
			{
				Entreprise ent = (Entreprise) dao.getAll().get(0);

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi = (DebiteurPrestationImposable) dao.save(dpi);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ent, dpi);
				dao.save(contact);
			}
			return null;
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
	@Transactional(rollbackFor = Throwable.class)
	public void testAddContribuableNewEntreprise() throws Exception {

		doInNewTransaction(status -> {
			{
				Entreprise ent = new Entreprise();
				ent.setNumero(1235L);
				ent = (Entreprise) dao.save(ent);

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi = (DebiteurPrestationImposable) dao.save(dpi);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ent, dpi);
				dao.save(contact);
			}
			return null;
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
	@Transactional(rollbackFor = Throwable.class)
	public void testAddContribuableNewAutreCommunaute() throws Exception {

		doInNewTransaction(status -> {
			{
				AutreCommunaute ac = new AutreCommunaute();
				ac.setNom("Bla bli");
				ac = (AutreCommunaute) dao.save(ac);

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi = (DebiteurPrestationImposable) dao.save(dpi);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
				dao.save(contact);
			}
			return null;
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

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testAjoutPeriodicite() throws Exception {
		doInNewTransaction(status -> {
			DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
			Periodicite periodicite = new Periodicite(PeriodiciteDecompte.TRIMESTRIEL);
			periodicite.setDateDebut(date(2010, 1, 1));
			dpi.addPeriodicite(periodicite);
			dao.save(dpi);
			return null;
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
	@Transactional(rollbackFor = Throwable.class)
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

		// for fermé -> toujours pas désactivé
		final RegDate dateFin1 = date(2002, 8, 15);
		f1.setDateFin(dateFin1);
		assertNull(dpi.getDateDesactivation());

		// nouvelle ouverture de for -> pas de changement
		final ForDebiteurPrestationImposable f2 = new ForDebiteurPrestationImposable();
		f2.setDateDebut(date(2003, 4, 12));
		f2.setNumeroOfsAutoriteFiscale(1234);
		f2.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
		dpi.addForFiscal(f2);
		assertNull(dpi.getDateDesactivation());

		// for fermé -> toujours pas désactivé
		final RegDate dateFin2 = date(2004, 12, 1);
		f2.setDateFin(dateFin2);
		assertNull(dpi.getDateDesactivation());

		// annulation du dernier for -> rien ne change
		f2.setAnnule(true);
		assertNull(dpi.getDateDesactivation());
	}
}
