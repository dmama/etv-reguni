package ch.vd.uniregctb.hibernate.interceptor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.springframework.transaction.TransactionStatus;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

public class ModificationLogInterceptorTest extends CoreDAOTest {

	// private static final Logger LOGGER = Logger.getLogger(ModificationLogInterceptorTest.class);

	private TiersDAO dao;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(TiersDAO.class, "tiersDAO");
	}

	@Test
	public void testCreationInfos() throws Exception {

		String activeUser = "BlaBla";
		AuthenticationHelper.setPrincipal(activeUser);

		long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = new PersonnePhysique(false);
				nhab.setNom("Broulis");
				nhab.setPrenom("Broulis");

				nhab = (PersonnePhysique) dao.save(nhab);
				return nhab.getNumero();
			}
		});

		PersonnePhysique nhab = (PersonnePhysique) dao.get(id);
		Date creationDate = nhab.getLogCreationDate();
		// String dateStr = DateHelper.dateTimeToDisplayString(creationDate);
		String user = nhab.getLogCreationUser();

		assertEquals(activeUser, user);
		assertNotNull(creationDate);
	}

	@Test
	public void testModificationInfos() throws Exception {

		final String activeUser = "BlaBla";
		AuthenticationHelper.setPrincipal(activeUser);

		final Date modifInitalDate = DateHelper.getDate(2002, 3, 21);
		final String oldUser = "BliBli";

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = new PersonnePhysique(false);
				nhab.setNom("Broulis");
				nhab.setPrenom("Broulis");
				nhab.setLogCreationUser(oldUser);
				nhab.setLogCreationDate(modifInitalDate);
				nhab.setLogModifUser(oldUser);
				nhab.setLogModifMillis(modifInitalDate.getTime());

				nhab = (PersonnePhysique) dao.save(nhab);
				return nhab.getNumero();
			}
		});

		Date beforeTx = DateHelper.getCurrentDate();
		Thread.sleep(100);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique) dao.get(id);
				nhab.setNom("Pauli");
				return null;
			}
		});

		Thread.sleep(100);
		Date afterTx = DateHelper.getCurrentDate();

		{
			PersonnePhysique nhab = (PersonnePhysique) dao.get(id);
			Date modifAfterDate = nhab.getLogModifDate();
			String user = nhab.getLogCreationUser();

			String modifAfterDateStr = modifAfterDate.toString();
			// String modifInitalDateStr = modifInitalDate.toString();

			assertEquals(activeUser, user);
			assertNotNull(modifAfterDateStr);
			assertTrue(modifInitalDate.before(modifAfterDate));
			assertTrue(beforeTx.getTime() < modifAfterDate.getTime());
			assertTrue(afterTx.getTime() > modifAfterDate.getTime());
		}
	}

	/**
	 * Test qu'une modification du numéro Ofs du for principal est bien détectée
	 */
	@Test
	public void testModificationForFiscaux() throws Exception {

		final String activeUser = "BlaBla";
		AuthenticationHelper.setPrincipal(activeUser);

		final Date modifInitalDate = DateHelper.getDate(2002, 3, 21);
		final String oldUser = "BliBli";

		final long id = doInNewTransaction(new TxCallback<Long>() {
			@Override
			public Long execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = new PersonnePhysique(false);
				nhab.setNom("Broulis");
				nhab.setPrenom("Broulis");
				nhab.setLogCreationUser(oldUser);
				nhab.setLogCreationDate(modifInitalDate);
				nhab.setLogModifUser(oldUser);
				nhab.setLogModifMillis(modifInitalDate.getTime());

				ForFiscalPrincipal f = new ForFiscalPrincipal();
				f.setDateDebut(RegDate.get(1990, 1, 1));
				f.setDateFin(null);
				f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				f.setNumeroOfsAutoriteFiscale(5586);
				f.setMotifRattachement(MotifRattachement.DOMICILE);
				f.setMotifOuverture(MotifFor.MAJORITE);
				f.setModeImposition(ModeImposition.ORDINAIRE);
				nhab.addForFiscal(f);

				nhab = (PersonnePhysique) dao.save(nhab);
				return nhab.getNumero();
			}
		});

		Date beforeTx = DateHelper.getCurrentDate();
		Thread.sleep(100);

		doInNewTransaction(new TxCallback<Object>() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				PersonnePhysique nhab = (PersonnePhysique) dao.get(id);
				ForFiscalPrincipal f = nhab.getDernierForFiscalPrincipal();
				f.setDateFin(RegDate.get(2008, 10, 10));
				f.setMotifFermeture(MotifFor.DEMENAGEMENT_VD);

				f = new ForFiscalPrincipal();
				f.setDateDebut(RegDate.get(2008, 10, 11));
				f.setDateFin(null);
				f.setGenreImpot(GenreImpot.REVENU_FORTUNE);
				f.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
				f.setNumeroOfsAutoriteFiscale(4321);
				f.setMotifRattachement(MotifRattachement.DOMICILE);
				f.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
				f.setModeImposition(ModeImposition.ORDINAIRE);
				nhab.addForFiscal(f);

				return null;
			}
		});

		Thread.sleep(100);
		Date afterTx = DateHelper.getCurrentDate();

		{
			PersonnePhysique nhab = (PersonnePhysique) dao.get(id);
			Date modifAfterDate = nhab.getLogModifDate();
			String user = nhab.getLogCreationUser();

			String modifAfterDateStr = modifAfterDate.toString();
			// String modifInitalDateStr = modifInitalDate.toString();

			assertEquals(activeUser, user);
			assertNotNull(modifAfterDateStr);
			assertTrue(modifInitalDate.before(modifAfterDate));
			assertTrue(beforeTx.getTime() < modifAfterDate.getTime());
			assertTrue(afterTx.getTime() > modifAfterDate.getTime());
		}
	}
}
