package ch.vd.unireg.norentes.civil.changement.dateNaissance;

import java.util.List;

import org.junit.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.common.CasePostale;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockLocalite;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersCriteria;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TexteCasePostale;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

import static java.lang.String.format;

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
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {
				MockIndividu momo = addIndividu(54321, dateNaissanceOriginale, "Durant", "Maurice", true);
				addDefaultAdressesTo(momo);
				addOrigine(momo, MockCommune.Geneve);
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
		addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissanceOriginale.addYears(18), dateNaissanceOriginale.addYears(22), MotifFor.MAJORITE, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
		addForFiscalPrincipal(momo, MockCommune.VillarsSousYens, dateNaissanceOriginale.addYears(22).addDays(1), null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant Maurice a bien été indexé")
	public void check1() {

		globalIndexer.sync();
		{
			TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(noHabMomo);
			List<TiersIndexedData> list = globalSearcher.search(criteria);
			Assert.assertEquals("Le tiers n'a pas été indexé", 1, list.size());
			TiersIndexedData tiers = list.get(0);
			Assert.assertEquals("Le numéro du tiers est incorrect", (long) tiers.getNumero(), noHabMomo);
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
			Assert.assertEquals("L'indexation n'a pas fonctionné", 1, list.size());
			TiersIndexedData tiers = list.get(0);
			Assert.assertEquals("Le numéro du tiers est incorrect", (long) tiers.getNumero(), noHabMomo);

			String dateNaissance =
					format("%4d%02d%02d", dateNaissanceCorrigee.year(), dateNaissanceCorrigee.month(), dateNaissanceCorrigee.day());
			Assert.assertEquals("La nouvelle date de naissance n'a pas été indexé", tiers.getDateNaissanceInscriptionRC(), dateNaissance);
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
