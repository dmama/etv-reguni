package ch.vd.uniregctb.norentes.civil.changement.identification;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockLocalite;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import org.springframework.util.Assert;

import java.util.List;

public class Ec_48000_02_CorrectionIdentificationNonHabitant_Scenario extends EvenementCivilScenario {

	public static final String NAME = "48000_02_CorrectionIdentificationNonHabitant";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION;
	}

	@Override
	public String getDescription() {
		return "Correction des données d'identification d'une personne non-habitante (NAVS13)";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndMomo = 54321;  // momo

	private long noHabMomo;

	private static final RegDate dateNaissance = RegDate.get(1960, 1, 1);
	private static final String avsOriginal = "7566387268929";
	private static final String avsNouveau = "7563821202792";
	private static final MockCommune commune = MockCommune.Lausanne;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(noIndMomo, dateNaissance, "Durant", "Maurice", true);
				addDefaultAdressesTo(momo);
				addOrigine(momo, MockPays.Suisse, null, dateNaissance);
				addNationalite(momo, MockPays.Suisse, dateNaissance, null, 0);
				momo.setNouveauNoAVS(avsOriginal);
			}

			@SuppressWarnings("deprecation")
			protected void addDefaultAdressesTo(Individu individu) {
				addAdresse(individu, EnumTypeAdresse.PRINCIPALE, null, null, MockLocalite.Bex.getNPA(), MockLocalite.Bex, "4848", RegDate.get(1980, 11, 2), null);
				addAdresse(individu, EnumTypeAdresse.COURRIER, null, null, MockLocalite.Renens.getNPA(), MockLocalite.Renens, "5252", RegDate.get(1980, 11, 2), null);
			}
		});
	}

	@Etape(id=1, descr="Chargement du non-habitant avec ses fors principaux")
	public void step1() {
		// momo
		final PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		final ForFiscalPrincipal f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissance.addYears(18), dateNaissance.addYears(22), MotifFor.MAJORITE, MotifFor.DEPART_HC);
		f.setModeImposition(ModeImposition.ORDINAIRE);

		final ForFiscalPrincipal fhc = addForFiscalPrincipal(momo, MockCommune.Neuchatel, dateNaissance.addYears(22).getOneDayAfter(), null, MotifFor.DEPART_HC, null);
		fhc.setModeImposition(ModeImposition.ORDINAIRE);

		tiersService.changeHabitantenNH(momo);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice a bien été indexé")
	public void check1() {
		{
			final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(noIndMomo);
			Assert.isTrue(!momo.isHabitant(), "Maurice devrait être non-habitant");
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(noHabMomo);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé");
			final TiersIndexedData tiers = list.get(0);
			Assert.isTrue(tiers.getNumero().equals(noHabMomo), "Le numéro du tiers est incorrect");
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsOriginal);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé par numéro AVS");
			final TiersIndexedData tiers = list.get(0);
			Assert.isTrue(tiers.getNumero().equals(noHabMomo), "Ce n'est pas le bon tiers qui est retrouvé par numéro AVS");
		}
		{
			// vérification que la recherche par la nouvelle valeur du numéro AVS ne donne rien
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsNouveau);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 0, "Un tiers est déjà trouvé par la nouvelle valeur du numéro AVS");
		}
	}

	@Etape(id=2, descr="Envoi de l'événement de correction d'identification")
	public void step2() throws Exception {
		final MockIndividu individu = (MockIndividu) serviceCivilService.getIndividu(noIndMomo, 2008);
		individu.setNouveauNoAVS(avsNouveau);

		long id = addEvenementCivil(TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION, noIndMomo, RegDate.get(), commune.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le numéro AVS a été mise à jour dans l'indexeur et dans le tiers")
	public void check2() throws Exception {
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsNouveau);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 1, "Le tiers n'a pas été indexé par numéro AVS");
			final TiersIndexedData tiers = list.get(0);
			Assert.isTrue(tiers.getNumero().equals(noHabMomo), "Ce n'est pas le bon tiers qui est retrouvé par numéro AVS");
		}
		{
			// vérification que la recherche par l'ancienne valeur du numéro AVS ne donne rien
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsOriginal);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.isTrue(list.size() == 0, "Un tiers est encore trouvé par l'ancienne valeur du numéro AVS");
		}
		{
			final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(noIndMomo);
			final String avs = momo.getNumeroAssureSocial();
			Assert.isTrue(avsNouveau.equals(avs), "numéro avs erroné dans la personne physique");
		}
	}
}