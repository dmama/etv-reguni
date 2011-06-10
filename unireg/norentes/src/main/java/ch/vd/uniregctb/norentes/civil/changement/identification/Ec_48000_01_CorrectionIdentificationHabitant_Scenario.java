package ch.vd.uniregctb.norentes.civil.changement.identification;

import java.util.List;

import annotation.Check;
import annotation.Etape;
import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
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
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public class Ec_48000_01_CorrectionIdentificationHabitant_Scenario extends EvenementCivilScenario {

	public static final String NAME = "48000_01_CorrectionIdentificationHabitant";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION;
	}

	@Override
	public String getDescription() {
		return "Correction des données d'identification d'une personne habitante (NAVS13)";
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
			protected void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, null, null, MockLocalite.Bex.getNPA(), MockLocalite.Bex, "4848", RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, null, null, MockLocalite.Renens.getNPA(), MockLocalite.Renens, "5252", RegDate.get(1980, 11, 2), null);
			}
		});
	}

	@Etape(id=1, descr="Chargement de l'habitant avec ses fors principaux")
	public void step1() {
		// momo
		PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		final ForFiscalPrincipal f = addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissance.addYears(18), null, MotifFor.MAJORITE, null);
		f.setModeImposition(ModeImposition.ORDINAIRE);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice a bien été indexé")
	public void check1() {

		globalIndexer.sync();
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

		doModificationIndividu(noIndMomo, new IndividuModification() {
			@Override
			public void modifyIndividu(MockIndividu individu) {
				individu.setNouveauNoAVS(avsNouveau);
			}
		});

		final long id = addEvenementCivil(TypeEvenementCivil.CHGT_CORREC_IDENTIFICATION, noIndMomo, RegDate.get(), commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérifie que le numéro AVS a été mise à jour dans l'indexeur")
	public void check2() throws Exception {

		globalIndexer.sync();
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
	}
}
