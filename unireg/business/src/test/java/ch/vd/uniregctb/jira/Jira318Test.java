package ch.vd.uniregctb.jira;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import org.junit.Test;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.BusinessTest;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

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
			ForFiscalPrincipal ff = new ForFiscalPrincipal();
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
				metierService.marie(dateMariageNOK, nh1, nh2, null, null, true, null);
				fail();
			}
			catch (Exception e) {
				// OK, on a une exception parce que la date du FF est avant le mariage
			}
		}
	}

	@Test
	public void testCreateCoupleFrom2NonHabitantOK() {

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
			ForFiscalPrincipal ff = new ForFiscalPrincipal();
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
			MenageCommun mc = metierService.marie(dateMariageOK, nh1, nh2, null, null, true, null);
			assertNotNull(mc);
		}
	}
}
