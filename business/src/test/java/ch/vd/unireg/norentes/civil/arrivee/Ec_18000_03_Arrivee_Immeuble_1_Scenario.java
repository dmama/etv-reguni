package ch.vd.unireg.norentes.civil.arrivee;

import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.MockIndividu;
import ch.vd.unireg.interfaces.civil.mock.MockServiceCivil;
import ch.vd.unireg.interfaces.common.Adresse;
import ch.vd.unireg.interfaces.infra.mock.MockAdresse;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.interfaces.infra.mock.MockRue;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForFiscalPrincipalPP;
import ch.vd.unireg.tiers.ForFiscalSecondaire;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.EtatEvenementCivil;
import ch.vd.unireg.type.GenreImpot;
import ch.vd.unireg.type.ModeImposition;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.MotifRattachement;
import ch.vd.unireg.type.TypeAdresseCivil;
import ch.vd.unireg.type.TypeEvenementCivil;

public class Ec_18000_03_Arrivee_Immeuble_1_Scenario extends EvenementCivilScenario {

	public static final String NAME = "18000_03_Arrivee_Immeuble";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE;
	}

	@Override
	public String getDescription() {
		return "déménagement dans le canton d'un vaudois propriétaire d'un immeuble";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndAlain = 122456L;

	private MockIndividu indAlain;

	private long noHabAlain;

	private final RegDate dateArrivee = RegDate.get(1982, 7, 5);
	private final RegDate dateDemenagement = RegDate.get(2006, 4, 11);
	private final RegDate dateAchatImmeuble = RegDate.get(1992, 4, 23);

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {
			@Override
			protected void init() {

				indAlain = addIndividu(noIndAlain, RegDate.get(1952, 2, 21), "Gregoire", "Alain", true);

				addAdresse(indAlain, TypeAdresseCivil.PRINCIPALE, MockRue.Lausanne.RouteGrangeNeuve, null, dateArrivee, null);

			}
		});
	}


	@Etape(id=1, descr="Chargement d'un habitant à Lausanne")
	public void etape1() throws Exception {

		PersonnePhysique alain = addHabitant(noIndAlain);
		noHabAlain = alain.getNumero();
		addForFiscalPrincipal(alain, MockCommune.Lausanne, dateArrivee, null, MotifFor.ARRIVEE_HC, null);

		addForFiscalSecondaire(alain, MockCommune.Renens.getNoOFS(), dateAchatImmeuble, null);
	}

	@Check(id=1, descr="Vérifie que l'habitant Alain a son adresse à Lausanne et son For à Lausanne")
	public void check1() throws Exception {
		PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
		ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
		assertNull( ffp.getDateFin(), "Date de fin du dernier for d'Alain fausse");
		assertEquals(MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "le for principal n'est pas sur Lausanne");
	}

	@Etape(id=2, descr="Déménagement de Alain")
	public void etape2() throws Exception {
		addNouvelleAdresse(indAlain);
	}

	@Check(id=2, descr="Vérifie que l'habitant Alain a toujours son For à Lausanne mais son adresse a Bex")
	public void check2() throws Exception {
		PersonnePhysique alain = (PersonnePhysique)tiersDAO.get(noHabAlain);
		ForFiscalPrincipal ffp = alain.getDernierForFiscalPrincipal();
		assertNotNull(ffp, "For principal de l'Habitant "+alain.getNumero()+" null");
		assertNull( ffp.getDateFin(), "Date de fin du dernier for fausse");
		assertEquals(MockCommune.Lausanne.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "le for principal n'est pas sur Lausanne");
	}

	@Etape(id=3, descr="Envoi de l'événement de déménagement de l'individu Alain")
	public void etape3() throws Exception {

		long id = addEvenementCivil(TypeEvenementCivil.ARRIVEE_PRINCIPALE_VAUDOISE, noIndAlain, dateDemenagement.addDays(1), MockCommune.Bex.getNoOFS());

		commitAndStartTransaction();

		// On traite les evenements
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'habitant Alain n'a plus son for sur Lausanne mais sur Bex")
	public void check3() throws Exception {
		// On check que Alain  est parti
		{
			EvenementCivilRegPP evt = getEvenementCivilRegoupeForHabitant(noHabAlain);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "");

			PersonnePhysique hab = (PersonnePhysique)tiersDAO.get(noHabAlain);
			List<ForFiscalPrincipalPP> list = hab.getForsFiscauxPrincipauxActifsSorted();

			// For fermé sur Lausanne
			ForFiscalPrincipalPP ffpFerme = list.get(list.size()-2);
			assertEquals(dateDemenagement, ffpFerme.getDateFin(), "Le for sur Lausanne n'est pas fermé à la bonne date");
			assertEquals(MockCommune.Lausanne.getNoOFS(), ffpFerme.getNumeroOfsAutoriteFiscale(), "le for précédent n'est pas sur Lausanne");

			// For ouvert sur Bex
			ForFiscalPrincipalPP ffpOuvert = list.get(list.size()-1);
			assertEquals(dateDemenagement.addDays(1) , ffpOuvert.getDateDebut(), "Le for sur Bex n'est pas ouvert à la bonne date");
			assertEquals(MockCommune.Bex.getNoOFS(), ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur Bex");
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le MotifRattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le GenreImpot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le ModeImposition du for est faux");

			// For secondaire inchangé
			List<ForFiscal> listFor = hab.getForsFiscauxSorted();
			if (listFor != null) {
				for (ForFiscal forFiscal : listFor) {
					if (!forFiscal.isAnnule() && forFiscal instanceof ForFiscalSecondaire) {
						assertEquals(dateAchatImmeuble, forFiscal.getDateDebut(), "Le for secondaire a changé");
					}
				}
			}
		}
	}
	private void addNouvelleAdresse(MockIndividu ind) {
		Collection<Adresse> adrs = ind.getAdresses();
		MockAdresse last = null;
		for (Adresse a : adrs) {
			last = (MockAdresse)a;
		}
		last.setDateFinValidite(dateDemenagement);
		ind.addAdresse(new MockAdresse(TypeAdresseCivil.PRINCIPALE, MockRue.Bex.CheminDeLaForet, null, dateDemenagement.addDays(1), null));
	}
}
