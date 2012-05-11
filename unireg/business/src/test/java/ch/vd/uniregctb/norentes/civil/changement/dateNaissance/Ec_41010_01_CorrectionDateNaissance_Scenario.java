package ch.vd.uniregctb.norentes.civil.changement.dateNaissance;

import java.util.List;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.CasePostale;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.norentes.annotation.Check;
import ch.vd.uniregctb.norentes.annotation.Etape;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_41010_01_CorrectionDateNaissance_Scenario extends EvenementCivilScenario {

	public static final String NAME = "41010_01_CorrectionDateNaissance";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CORREC_DATE_NAISSANCE;
	}

	@Override
	public String getDescription() {
		return "Correction de la date de naissance d'une personne";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMomo = 54321;  // momo

	private long noHabMomo;

	private static final RegDate dateNaissanceOriginale = RegDate.get(1980, 3, 12);
	private static final RegDate dateNaissanceCorrigee = RegDate.get(1980, 3, 8);
	private static final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(54321, dateNaissanceOriginale, "Durant", "Maurice", true);
				addDefaultAdressesTo(momo);
				addOrigine(momo, MockPays.Suisse.getNomMinuscule());
				addNationalite(momo, MockPays.Suisse, dateNaissanceOriginale, null);
			}

			@SuppressWarnings("deprecation")
			protected void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, null, null, MockLocalite.Bex.getNPA(), MockLocalite.Bex, new CasePostale(TexteCasePostale.CASE_POSTALE, 4848), RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, null, null, MockLocalite.Renens.getNPA(), MockLocalite.Renens, new CasePostale(TexteCasePostale.CASE_POSTALE, 5252), RegDate.get(1980, 11, 2), null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant avec ses fors principaux")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		ForFiscalPrincipal f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissanceOriginale.addYears(18),
				dateNaissanceOriginale.addYears(22), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissanceOriginale.addYears(22).addDays(1), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice a bien été indexé")
	public void check1() {

		globalIndexer.sync();
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(noHabMomo);
			List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
			TiersIndexedData tiers = list.get(0);
			Assert.isTrue(tiers.getNumero().equals(noHabMomo), "Le numéro du tiers est incorrect");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de correction de date de naissance")
	public void step2() throws Exception {

		doModificationIndividu(noIndMomo, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setDateNaissance(dateNaissanceCorrigee);
			}
		});

		final long id = addEvenementCivil(TypeEvenementCivil.CORREC_DATE_NAISSANCE, noIndMomo, dateNaissanceCorrigee, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que la date de naissance a été mise à jour dans l'indexer")
	public void check2() throws Exception {

		globalIndexer.sync();
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(noHabMomo);
			List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 1, "L'indexation n'a pas fonctionné");
			TiersIndexedData tiers = list.get(0);
			Assert.isTrue(tiers.getNumero().equals(noHabMomo), "Le numéro du tiers est incorrect");

			String dateNaissance =
				String.format("%4d%02d%02d", dateNaissanceCorrigee.year(), dateNaissanceCorrigee.month(), dateNaissanceCorrigee.day());
			Assert.isTrue(tiers.getDateNaissance().equals(dateNaissance), "La nouvelle date de naissance n'a pas été indexé");
		}
		{
			PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			ForFiscalPrincipal ffp = null;
			for (ForFiscal forFiscal : momo.getForsFiscauxSorted()) {
				if (forFiscal.isPrincipal() && MotifFor.MAJORITE == ((ForFiscalPrincipal) forFiscal).getMotifOuverture()) {
					ffp = (ForFiscalPrincipal) forFiscal;
				}
			}
			assertNotNull(ffp, "Aucun for fiscal principal avec motif d'ouverture MAJORITE n'a été trouvé");
			assertEquals(dateNaissanceCorrigee.addYears(18), ffp.getDateDebut(), "La date d'ouverture du for MAJORITE n'a pas été modifiée");
		}
	}
}
