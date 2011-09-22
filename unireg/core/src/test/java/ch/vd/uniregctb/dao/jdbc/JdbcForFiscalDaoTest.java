package ch.vd.uniregctb.dao.jdbc;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.common.CoreDAOTest;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class JdbcForFiscalDaoTest extends CoreDAOTest {

	private JdbcForFiscalDao dao = new JdbcForFiscalDaoImpl();

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGet() throws Exception {

		class Ids {
			long raoul;
			long ff;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique raoul = addNonHabitant("Raoul", "Laplanche", date(1967, 3, 4), Sexe.MASCULIN);
				ids.raoul = raoul.getNumero();

				ForFiscalPrincipal ff = addForPrincipal(raoul, date(2002, 3, 23), MotifFor.ARRIVEE_HS, null, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				ids.ff = ff.getId();
				return null;
			}
		});

		JdbcTemplate template = new JdbcTemplate(dataSource);
		final ForFiscalPrincipal ffp = (ForFiscalPrincipal) dao.get(ids.ff, template);
		assertNotNull(ffp);
		assertEquals(ids.ff, ffp.getId().longValue());
		assertEquals(date(2002, 3, 23), ffp.getDateDebut());
		assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
		assertNull(ffp.getDateFin());
		assertNull(ffp.getMotifFermeture());
		assertEquals(1234L, ffp.getNumeroOfsAutoriteFiscale().longValue());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForTiers() throws Exception {

		class Ids {
			long raoul;
			long ffp;
			long ffs;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique raoul = addNonHabitant("Raoul", "Laplanche", date(1967, 3, 4), Sexe.MASCULIN);
				ids.raoul = raoul.getNumero();

				ForFiscalPrincipal ffp = addForPrincipal(raoul, date(2002, 3, 23), MotifFor.ARRIVEE_HS, null, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				ids.ffp = ffp.getId();

				ForFiscalSecondaire ffs = addForSecondaire(raoul, date(2004, 1, 1), MotifFor.ACHAT_IMMOBILIER, date(2005, 4, 3), MotifFor.VENTE_IMMOBILIER, 4321, MotifRattachement.IMMEUBLE_PRIVE);
				ids.ffs = ffs.getId();
				return null;
			}
		});

		JdbcTemplate template = new JdbcTemplate(dataSource);
		final Set<ForFiscal> fors = dao.getForTiers(ids.raoul, template);
		assertNotNull(fors);
		assertEquals(2, fors.size());

		ForFiscalPrincipal ffp = null;
		ForFiscalSecondaire ffs = null;
		for (ForFiscal ff : fors) {
			if (ff instanceof ForFiscalPrincipal) {
				assertNull(ffp);
				ffp = (ForFiscalPrincipal) ff;
			}
			else if (ff instanceof ForFiscalSecondaire) {
				assertNull(ffs);
				ffs = (ForFiscalSecondaire) ff;
			}
			else {
				fail();
			}
		}

		assertNotNull(ffp);
		assertEquals(ids.ffp, ffp.getId().longValue());
		assertEquals(date(2002, 3, 23), ffp.getDateDebut());
		assertEquals(MotifFor.ARRIVEE_HS, ffp.getMotifOuverture());
		assertNull(ffp.getDateFin());
		assertNull(ffp.getMotifFermeture());
		assertEquals(1234L, ffp.getNumeroOfsAutoriteFiscale().longValue());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffp.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement());

		assertNotNull(ffs);
		assertEquals(ids.ffs, ffs.getId().longValue());
		assertEquals(date(2004, 1, 1), ffs.getDateDebut());
		assertEquals(MotifFor.ACHAT_IMMOBILIER, ffs.getMotifOuverture());
		assertEquals(date(2005, 4, 3), ffs.getDateFin());
		assertEquals(MotifFor.VENTE_IMMOBILIER, ffs.getMotifFermeture());
		assertEquals(4321L, ffs.getNumeroOfsAutoriteFiscale().longValue());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, ffs.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.IMMEUBLE_PRIVE, ffs.getMotifRattachement());
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testGetForMultipleTiers() throws Exception {

		class Ids {
			long raoul;
			long rffp;
			long sophie;
			long sffp;
		}
		final Ids ids = new Ids();

		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				PersonnePhysique raoul = addNonHabitant("Raoul", "Laplanche", date(1967, 3, 4), Sexe.MASCULIN);
				ids.raoul = raoul.getNumero();

				ForFiscalPrincipal ffp = addForPrincipal(raoul, date(2002, 3, 23), MotifFor.ARRIVEE_HS, null, null, 1234, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				ids.rffp = ffp.getId();

				PersonnePhysique sophie = addNonHabitant("Sophie", "Ramone", date(1965, 12, 24), Sexe.FEMININ);
				ids.sophie = sophie.getNumero();

				ffp = addForPrincipal(sophie, date(2001, 1, 31), MotifFor.ARRIVEE_HC, null, null, 333, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifRattachement.DOMICILE);
				ids.sffp = ffp.getId();

				return null;
			}
		});

		JdbcTemplate template = new JdbcTemplate(dataSource);
		final Map<Long, Set<ForFiscal>> map = dao.getForTiers(Arrays.asList(ids.raoul, ids.sophie), template);
		assertNotNull(map);
		assertEquals(2, map.size());

		final Set<ForFiscal> forsRaoul = map.get(ids.raoul);
		assertNotNull(forsRaoul);
		assertEquals(1, forsRaoul.size());

		final ForFiscalPrincipal rffp = (ForFiscalPrincipal) forsRaoul.iterator().next();
		assertNotNull(rffp);
		assertEquals(ids.rffp, rffp.getId().longValue());
		assertEquals(date(2002, 3, 23), rffp.getDateDebut());
		assertEquals(MotifFor.ARRIVEE_HS, rffp.getMotifOuverture());
		assertNull(rffp.getDateFin());
		assertNull(rffp.getMotifFermeture());
		assertEquals(1234L, rffp.getNumeroOfsAutoriteFiscale().longValue());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, rffp.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, rffp.getMotifRattachement());

		final Set<ForFiscal> forsSophie = map.get(ids.sophie);
		assertNotNull(forsSophie);
		assertEquals(1, forsSophie.size());

		final ForFiscalPrincipal sffp = (ForFiscalPrincipal) forsSophie.iterator().next();
		assertNotNull(sffp);
		assertEquals(ids.sffp, sffp.getId().longValue());
		assertEquals(date(2001, 1, 31), sffp.getDateDebut());
		assertEquals(MotifFor.ARRIVEE_HC, sffp.getMotifOuverture());
		assertNull(sffp.getDateFin());
		assertNull(sffp.getMotifFermeture());
		assertEquals(333L, sffp.getNumeroOfsAutoriteFiscale().longValue());
		assertEquals(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, sffp.getTypeAutoriteFiscale());
		assertEquals(MotifRattachement.DOMICILE, sffp.getMotifRattachement());
	}
}