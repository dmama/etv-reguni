package ch.vd.uniregctb.norentes.civil.obtention.permis;

import annotation.Check;
import annotation.Etape;
import ch.vd.common.model.EnumTypeAdresse;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.evenement.EvenementCivilRegroupe;
import ch.vd.uniregctb.interfaces.model.mock.*;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.norentes.common.EvenementCivilScenario;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.*;

public class Ec_16000_03_ObtentionPermis_NonResident_Scenario extends EvenementCivilScenario {

	public static final String NAME = "Ec_16000_03_ObtentionPermis";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return "Obtention d'un permis C sur un sourcier dont le for de domicile n'est pas vaudois";
	}

	private final long noIndMomo = 54321; // momo
	private MockIndividu indMomo;

	private long noCtbMomo;

	private final RegDate dateNaissanceMomo = RegDate.get(1961, 3, 12);
	private final RegDate dateObtentionPermisB = RegDate.get(2006, 4, 27);
	private final RegDate dateObtentionPermisC =  RegDate.get(2008, 7, 15);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, dateNaissanceMomo, "Durant", "Maurice", true);

				addOrigine(indMomo, MockPays.France, null, dateNaissanceMomo);
				addPermis(indMomo, EnumTypePermis.ANNUEL, dateObtentionPermisB, dateObtentionPermisC.getOneDayBefore(), 0, false);
				addPermis(indMomo, EnumTypePermis.ETABLLISSEMENT, dateObtentionPermisC, null, 1, false);
				addNationalite(indMomo, MockPays.France, dateNaissanceMomo, null, 0);
				addAdresse(indMomo, EnumTypeAdresse.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, MockLocalite.Neuchatel, dateObtentionPermisB, null);
			}
		});
	}

	@Etape(id=1, descr="Création du contribuable sourcier non-résident")
	public void etape1() throws Exception {

		// il a été habitant un jour mais ne l'est plus...
		final PersonnePhysique momo = addNonHabitant("Durant", "Maurice", dateNaissanceMomo);
		momo.setNumeroIndividu(noIndMomo);
		noCtbMomo = momo.getNumero();

		final ForFiscalPrincipal ffp = addForFiscalPrincipal(momo, MockCommune.Neuchatel.getNoOFS(), dateObtentionPermisB, null, MotifFor.ARRIVEE_HS, null);
		ffp.setModeImposition(ModeImposition.SOURCE);
		ffp.setTypeAutoriteFiscale(TypeAutoriteFiscale.COMMUNE_HC);
	}

	@Check(id=1, descr="Vérification de l'état du for")
	public void check1() throws Exception {

		final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noCtbMomo);
		final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Pas de for fiscal principal");
		assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Mauvais mode d'imposition");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Neuchatel.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise autorité fiscale");
	}

	@Etape(id=2, descr="Réception de l'événement d'obtention de permis C")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, noIndMomo, dateObtentionPermisC, MockCommune.Aubonne.getNoOFS());
		commitAndStartTransaction();
		regroupeEtTraiteEvenements(id);
	}

	@Check(id=2, descr="Vérification de l'état du for courant")
	public void check2() throws Exception {

		final EvenementCivilRegroupe evt = getEvenementCivilRegoupeForHabitant(noCtbMomo);
		assertNotNull(evt, "Pas d'événement regroupé");
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement aurait dû être traité");

		final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noCtbMomo);
		final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Pas de for fiscal principal");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Mauvais mode d'imposition");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Neuchatel.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise autorité fiscale");
		assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement(), "Mauvais motif de rattachement");
		assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture(), "Mauvais motif d'ouverture");
		assertEquals(dateObtentionPermisC, ffp.getDateDebut(), "Mauvaise date d'ouverture du for actif");
		assertNull(ffp.getDateFin(), "For actif fermé");
	}
}
