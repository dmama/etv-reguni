package ch.vd.unireg.jira;

import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.common.BusinessTest;
import ch.vd.unireg.metier.MetierService;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersDAO;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAutoriteFiscale;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class Jira318Test extends BusinessTest {

	private TiersDAO dao;
	private MetierService metierService;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		dao = getBean(TiersDAO.class, "tiersDAO");
		metierService = getBean(MetierService.class, "metierService");
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateCoupleFrom2NonHabitantKO() {

		RegDate dateOuvFor = RegDate.get(2008, 5, 1);
		RegDate dateMariageNOK = RegDate.get(2008, 4, 1);

		PersonnePhysique nh1;
		PersonnePhysique nh2;
		// Premier NonHabitant
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Principal");
			nh.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
			nh1 = (PersonnePhysique)dao.save(nh);
		}
		// Premier NonHabitant
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Conjoint");
			nh.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
			ForFiscalPrincipalPP ff = new ForFiscalPrincipalPP();
			ff.setDateDebut(dateOuvFor);
			ff.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			ff.setMotifRattachement(MotifRattachement.DOMICILE);
			ff.setModeImposition(ModeImposition.SOURCE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(5586);
			nh.addForFiscal(ff);
			nh2 = (PersonnePhysique)dao.save(nh);
		}

		// Mariage
		{
			try {
				metierService.marie(dateMariageNOK, nh1, nh2, null, null, null);
				fail();
			}
			catch (Exception e) {
				// OK, on a une exception parce que la date du FF est avant le mariage
			}
		}
	}

	@Test
	@Transactional(rollbackFor = Throwable.class)
	public void testCreateCoupleFrom2NonHabitantOK() throws MetierServiceException {

		RegDate dateOuvFor = RegDate.get(2008, 5, 1);
		RegDate dateMariageOK = RegDate.get(2008, 6, 1);

		PersonnePhysique nh1;
		PersonnePhysique nh2;
		// Premier NonHabitant
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Principal");
			nh.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
			nh1 = (PersonnePhysique)dao.save(nh);
		}
		// Premier NonHabitant
		{
			PersonnePhysique nh = new PersonnePhysique(false);
			nh.setNom("Conjoint");
			nh.setNumeroOfsNationalite(MockPays.Suisse.getNoOFS());
			ForFiscalPrincipalPP ff = new ForFiscalPrincipalPP();
			ff.setDateDebut(dateOuvFor);
			ff.setMotifOuverture(MotifFor.DEMENAGEMENT_VD);
			ff.setGenreImpot(GenreImpot.REVENU_FORTUNE);
			ff.setMotifRattachement(MotifRattachement.DOMICILE);
			ff.setModeImposition(ModeImposition.SOURCE);
			ff.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD);
			ff.setNumeroOfsAutoriteFiscale(5586);
			nh.addForFiscal(ff);
			nh2 = (PersonnePhysique)dao.save(nh);
		}

		// Mariage
		{
			MenageCommun mc = metierService.marie(dateMariageOK, nh1, nh2, null, null, null);
			assertNotNull(mc);
		}
	}
}
