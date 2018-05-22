package ch.vd.unireg.norentes.civil.changement.identification;

import java.util.List;

import org.junit.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;


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

	private static final long noIndMomo = 54321;  // momo

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
				addOrigine(momo, MockCommune.Lausanne);
				addNationalite(momo, MockPays.Suisse, dateNaissance, null);
				momo.setNouveauNoAVS(avsOriginal);
			}

			@SuppressWarnings("deprecation")
			protected void addDefaultAdressesTo(MockIndividu individu) {
				addAdresse(individu, TypeAdresseCivil.PRINCIPALE, null, null, MockLocalite.Bex.getNPA(), MockLocalite.Bex, new CasePostale(TexteCasePostale.CASE_POSTALE, 4848), RegDate.get(1980, 11, 2), null);
				addAdresse(individu, TypeAdresseCivil.COURRIER, null, null, MockLocalite.Renens.getNPA(), MockLocalite.Renens, new CasePostale(TexteCasePostale.CASE_POSTALE, 5252), RegDate.get(1980, 11, 2), null);
			}
		});
	}

	@Etape(id=1, descr="Chargement du non-habitant avec ses fors principaux")
	public void step1() {
		// momo
		final PersonnePhysique momo = tiersService.createNonHabitantFromIndividu(noIndMomo);
		noHabMomo = momo.getNumero();

		addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissance.addYears(18), dateNaissance.addYears(22), MotifFor.MAJORITE, MotifFor.DEPART_HC);
		addForFiscalPrincipal(momo, MockCommune.Neuchatel, dateNaissance.addYears(22).getOneDayAfter(), null, MotifFor.DEPART_HC, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice a bien été indexé")
	public void check1() {

		globalIndexer.sync();
		{
			final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(noIndMomo);
			Assert.assertTrue("Maurice devrait être non-habitant", !momo.isHabitantVD());
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(noHabMomo);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertEquals("Le tiers n'a pas été indexé", 1, list.size());
			final TiersIndexedData tiers = list.get(0);
			Assert.assertEquals("Le numéro du tiers est incorrect", (long) tiers.getNumero(), noHabMomo);
		}
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsOriginal);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertEquals("Le tiers n'a pas été indexé par numéro AVS", 1, list.size());
			final TiersIndexedData tiers = list.get(0);
			Assert.assertEquals("Ce n'est pas le bon tiers qui est retrouvé par numéro AVS", (long) tiers.getNumero(), noHabMomo);
		}
		{
			// vérification que la recherche par la nouvelle valeur du numéro AVS ne donne rien
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsNouveau);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertTrue("Un tiers est déjà trouvé par la nouvelle valeur du numéro AVS", list.isEmpty());
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

	@Check(id=2, descr="Vérifie que le numéro AVS a été mise à jour dans l'indexeur et dans le tiers")
	public void check2() throws Exception {

		globalIndexer.sync();
		{
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsNouveau);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertEquals("Le tiers n'a pas été indexé par numéro AVS", 1, list.size());
			final TiersIndexedData tiers = list.get(0);
			Assert.assertEquals("Ce n'est pas le bon tiers qui est retrouvé par numéro AVS", (long) tiers.getNumero(), noHabMomo);
		}
		{
			// vérification que la recherche par l'ancienne valeur du numéro AVS ne donne rien
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumeroAVS(avsOriginal);
			final List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertTrue("Un tiers est encore trouvé par l'ancienne valeur du numéro AVS", list.isEmpty());
		}
		{
			final PersonnePhysique momo = tiersDAO.getPPByNumeroIndividu(noIndMomo);
			final String avs = momo.getNumeroAssureSocial();
			Assert.assertEquals("numéro avs erroné dans la personne physique", avsNouveau, avs);
		}
	}
}
