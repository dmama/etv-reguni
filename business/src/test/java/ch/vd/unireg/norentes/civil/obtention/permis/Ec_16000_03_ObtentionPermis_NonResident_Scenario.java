package ch.vd.unireg.norentes.civil.obtention.permis;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockIndividuConnector;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockPays;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.Sexe;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeAutoriteFiscale;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypePermis;

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

	private static final long noIndMomo = 54321; // momo
	private MockIndividu indMomo;

	private long noCtbMomo;

	private final RegDate dateNaissanceMomo = RegDate.get(1961, 3, 12);
	private final RegDate dateObtentionPermisB = RegDate.get(2006, 4, 27);
	private final RegDate dateObtentionPermisC =  RegDate.get(2008, 7, 15);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockIndividuConnector() {
			@Override
			protected void init() {

				indMomo = addIndividu(noIndMomo, dateNaissanceMomo, "Durant", "Maurice", true);

				addPermis(indMomo, TypePermis.SEJOUR, dateObtentionPermisB, dateObtentionPermisC.getOneDayBefore(), false);
				addPermis(indMomo, TypePermis.ETABLISSEMENT, dateObtentionPermisC, null, false);
				addNationalite(indMomo, MockPays.France, dateNaissanceMomo, null);
				addAdresse(indMomo, TypeAdresseCivil.PRINCIPALE, MockRue.Neuchatel.RueDesBeauxArts, null, dateObtentionPermisB, null);
			}
		});
	}

	@Etape(id=1, descr="Création du contribuable sourcier non-résident")
	public void etape1() throws Exception {

		// il a été habitant un jour mais ne l'est plus...
		final PersonnePhysique momo = addNonHabitant("Durant", "Maurice", dateNaissanceMomo, Sexe.MASCULIN);
		momo.setNumeroIndividu(noIndMomo);
		noCtbMomo = momo.getNumero();

		addForFiscalPrincipal(momo, MockCommune.Neuchatel, dateObtentionPermisB, null, MotifFor.ARRIVEE_HS, null, ModeImposition.SOURCE);
	}

	@Check(id=1, descr="Vérification de l'état du for")
	public void check1() throws Exception {

		final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noCtbMomo);
		final ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Pas de for fiscal principal");
		assertEquals(ModeImposition.SOURCE, ffp.getModeImposition(), "Mauvais mode d'imposition");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Neuchatel.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise autorité fiscale");
	}

	@Etape(id=2, descr="Réception de l'événement d'obtention de permis C")
	public void etape2() throws Exception {
		final long id = addEvenementCivil(TypeEvenementCivil.CHGT_CATEGORIE_ETRANGER, noIndMomo, dateObtentionPermisC, MockCommune.Aubonne.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=2, descr="Vérification de l'état du for courant")
	public void check2() throws Exception {

		final EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noCtbMomo);
		assertNotNull(evt, "L'événement est introuvable.");
		assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "L'événement aurait dû être traité");

		final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noCtbMomo);
		final ForFiscalPrincipalPP ffp = momo.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "Pas de for fiscal principal");
		assertEquals(ModeImposition.ORDINAIRE, ffp.getModeImposition(), "Mauvais mode d'imposition");
		assertEquals(TypeAutoriteFiscale.COMMUNE_HC, ffp.getTypeAutoriteFiscale(), "Mauvais type d'autorité fiscale");
		assertEquals(MockCommune.Neuchatel.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "Mauvaise autorité fiscale");
		assertEquals(MotifRattachement.DOMICILE, ffp.getMotifRattachement(), "Mauvais motif de rattachement");
		assertEquals(MotifFor.PERMIS_C_SUISSE, ffp.getMotifOuverture(), "Mauvais motif d'ouverture");
		assertEquals(dateObtentionPermisC.getOneDayAfter(), ffp.getDateDebut(), "Mauvaise date d'ouverture du for actif");
		assertNull(ffp.getDateFin(), "For actif fermé");
	}
}
