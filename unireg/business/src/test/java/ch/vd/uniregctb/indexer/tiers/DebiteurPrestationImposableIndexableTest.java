package ch.vd.uniregctb.indexer.tiers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.tiers.AutreCommunaute;
import ch.vd.uniregctb.tiers.ContactImpotSource;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.PeriodiciteDecompte;
import ch.vd.uniregctb.type.TypeAdresseTiers;

public class DebiteurPrestationImposableIndexableTest extends BusinessTest {

	private TiersDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	/**
	 * Vérifie qu'un débiteur est bien indexé en fonction du nom du contribuable auquel il est associé.
	 */
	@Test
	public void testIndexationDebiteurEtContribuable() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbNh;
			Long noCtbDpi;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi.setPeriodiciteDecompte(PeriodiciteDecompte.MENSUEL);
				dpi = (DebiteurPrestationImposable) dao.save(dpi);
				numeros.noCtbDpi = dpi.getNumero();

				PersonnePhysique nh = new PersonnePhysique(false);
				nh.setNom("Entrepreneur");
				nh = (PersonnePhysique) dao.save(nh);
				numeros.noCtbNh = nh.getNumero();

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, nh, dpi);
				dao.getHibernateTemplate().merge(contact);

				return null;
			}
		});

		/* On vérifie que les liens sont bien établis */
		{
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) dao.get(numeros.noCtbDpi);
			assertNotNull(dpi);
			assertEquals(PeriodiciteDecompte.MENSUEL, dpi.getPeriodiciteDecompte());
			assertNotNull(dpi.getContribuable());

			final PersonnePhysique nh = (PersonnePhysique) dao.get(numeros.noCtbNh);
			assertNotNull(nh);
			assertEquals("Entrepreneur", nh.getNom());
			assertEquals(1, nh.getDebiteursPrestationImposable().size());

			final DebiteurPrestationImposable debiteur = nh.getDebiteursPrestationImposable().iterator().next();
			assertSame(dpi, debiteur);
		}

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Entrepreneur");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());

			TiersIndexedData data0 = resultats.get(0);
			TiersIndexedData data1;
			if (data0.getNumero().equals(numeros.noCtbDpi)) {
				data0 = resultats.get(0);
				data1 = resultats.get(1);
			}
			else {
				data0 = resultats.get(1);
				data1 = resultats.get(0);
			}
			assertNotNull(data0);
			assertNotNull(data1);

			final TiersIndexedData dataNh = (data0.getNumero().equals(numeros.noCtbNh) ? data0 : data1);
			final TiersIndexedData dataDpi = (data0.getNumero().equals(numeros.noCtbNh) ? data1 : data0);
			assertEquals(numeros.noCtbNh, dataNh.getNumero());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
		}
	}

	@Test
	public void testIndexationDebiteurEtEntreprise() throws Exception {

		/* Création d'un contribuable et d'un débiteur lié au contribuable */
		final class Numeros {
			Long noCtbEnt;
			Long noCtbDpi;
		}
		final Numeros numeros = new Numeros();
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi.setPeriodiciteDecompte(PeriodiciteDecompte.MENSUEL);
				dpi = (DebiteurPrestationImposable) dao.save(dpi);
				numeros.noCtbDpi = dpi.getNumero();

				AutreCommunaute ent = new AutreCommunaute();
				ent.setNom("Nestle");
				ent.setComplementNom("Orbe");
				ent = (AutreCommunaute) dao.save(ent);
				numeros.noCtbEnt = ent.getNumero();

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ent, dpi);
				dao.getHibernateTemplate().merge(contact);

				return null;
			}
		});

		{
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) dao.get(numeros.noCtbDpi);
			assertNotNull(dpi);
			assertEquals(PeriodiciteDecompte.MENSUEL, dpi.getPeriodiciteDecompte());
			assertNotNull(dpi.getContribuable());

			final AutreCommunaute ac = (AutreCommunaute) dao.get(numeros.noCtbEnt);
			assertNotNull(ac);
			assertEquals("Nestle", ac.getNom());
			assertEquals("Orbe", ac.getComplementNom());
			assertEquals(1, ac.getDebiteursPrestationImposable().size());

			final DebiteurPrestationImposable debiteur = ac.getDebiteursPrestationImposable().iterator().next();
			assertSame(dpi, debiteur);
		}

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Nestle");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());

			TiersIndexedData data0 = resultats.get(0);
			TiersIndexedData data1;
			if (data0.getNumero().equals(numeros.noCtbDpi)) {
				data0 = resultats.get(0);
				data1 = resultats.get(1);
			}
			else {
				data0 = resultats.get(1);
				data1 = resultats.get(0);
			}
			assertNotNull(data0);
			assertNotNull(data1);

			final TiersIndexedData dataNh = (data0.getNumero().equals(numeros.noCtbEnt) ? data0 : data1);
			final TiersIndexedData dataDpi = (data0.getNumero().equals(numeros.noCtbEnt) ? data1 : data0);
			assertEquals(numeros.noCtbEnt, dataNh.getNumero());
			assertEquals(numeros.noCtbDpi, dataDpi.getNumero());
		}

		/* On vérifie que les liens sont bien établis */
		/*
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
		return null;
			}
		});
		*/

		/// On modifie l'AutreCommunaute et on cherche sur la modif => 2 hits
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				final AutreCommunaute ac = (AutreCommunaute) dao.get(numeros.noCtbEnt);
				assertNotNull(ac);
				ac.setComplementNom("Vevey");
				return null;
			}
		});

		/* On vérifie que le débiteur et le contribuables sont bien indexés */
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Vevey");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());
		}

	}

	/**
	 * [UNIREG-1376] Indexation d'un débiteur lié à une autre communauté.
	 */
	@Test
	public void testIndexationDebiteurEtAutreCommunaute() throws Exception {

		final long idDpi = 1500040L;
		final long idAC = 2001400L; // Bollet SA

		// Création d'un contribuable et d'un débiteur lié au contribuable
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {

				DebiteurPrestationImposable dpi = new DebiteurPrestationImposable();
				dpi.setNumero(idDpi);
				dpi.setPersonneContact("Jean-François Burnier");
				dpi.setPeriodiciteDecompte(PeriodiciteDecompte.MENSUEL);
				dpi = (DebiteurPrestationImposable) dao.save(dpi);

				addAdresseSuisse(dpi, TypeAdresseTiers.COURRIER, date(2009,1,1), null,  MockRue.Lausanne.AvenueDeBeaulieu);
				addForDebiteur(dpi, date(2009,1,1), null, MockCommune.Lausanne);

				AutreCommunaute ac = new AutreCommunaute();
				ac.setNumero(idAC);
				ac.setNom("Bollet SA");
				ac.setComplementNom("Vive les champignons !");
				ac = (AutreCommunaute) dao.save(ac);

				ContactImpotSource contact = new ContactImpotSource(RegDate.get(), null, ac, dpi);
				dao.getHibernateTemplate().merge(contact);

				return null;
			}
		});

		// On vérifie que le débiteur et l'autre communauté sont bien indexés
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNomRaison("Bollet");
			final List<TiersIndexedData> resultats = globalTiersSearcher.search(criteria);
			assertNotNull(resultats);
			assertEquals(2, resultats.size());

			final TiersIndexedData dataDpi = (resultats.get(0).getNumero().longValue() == idDpi ? resultats.get(0) : resultats.get(1));
			assertEquals(idDpi, dataDpi.getNumero().longValue());
			assertEquals("Bollet SA", dataDpi.getNom1());
			assertEquals("Vive les champignons !", dataDpi.getNom2());
			assertEquals("1000", dataDpi.getNpa());
			assertEquals("Lausanne", dataDpi.getLocaliteOuPays());
			assertEquals("Lausanne", dataDpi.getForPrincipal());
			assertSameDay(DateHelper.getDate(2009, 1, 1), dataDpi.getDateOuvertureFor());
			assertNull(dataDpi.getDateFermetureFor());
			assertEquals("", dataDpi.getDateNaissance());
			assertEquals("", dataDpi.getDateDeces());

			final TiersIndexedData dataAC = (resultats.get(0).getNumero().longValue() == idAC ? resultats.get(0) : resultats.get(1));
			assertEquals(idAC, dataAC.getNumero().longValue());
			assertEquals("Bollet SA", dataAC.getNom1());
			assertEquals("Vive les champignons !", dataAC.getNom2());
			assertEquals("", dataAC.getNpa());
			assertEquals("", dataAC.getLocaliteOuPays());
			assertEquals("", dataAC.getForPrincipal());
			assertNull(dataAC.getDateOuvertureFor());
			assertNull(dataAC.getDateFermetureFor());
			assertEquals("", dataAC.getDateNaissance());
			assertEquals("", dataAC.getDateDeces());
		}
	}
}
