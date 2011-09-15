package ch.vd.uniregctb.norentes.civil.depart;

import java.util.List;
import java.util.Set;

import annotation.Check;
import annotation.Etape;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.adresse.AdressesCiviles;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotCriteria;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaireDAO;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationHelper;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.interfaces.model.mock.MockCommune;
import ch.vd.uniregctb.interfaces.model.mock.MockIndividu;
import ch.vd.uniregctb.interfaces.model.mock.MockOfficeImpot;
import ch.vd.uniregctb.interfaces.model.mock.MockPays;
import ch.vd.uniregctb.interfaces.model.mock.MockRue;
import ch.vd.uniregctb.interfaces.service.mock.MockServiceCivil;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tache;
import ch.vd.uniregctb.tiers.TacheCriteria;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeAdresseCivil;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.type.TypeTache;

public class Ec_19000_04_Depart_JIRA1262_Scenario extends DepartScenario {

	private DeclarationImpotOrdinaireDAO diDAO;

	public void setDiDAO(DeclarationImpotOrdinaireDAO diDAO) {
		this.diDAO = diDAO;
	}

	public static final String NAME = "19000_04_Depart";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {

		return TypeEvenementCivil.DEPART_COMMUNE;
	}

	@Override
	public String getDescription() {

		return "Départ hors canton d'un habitant";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private final long noIndSebastien = 844770;

	private MockIndividu indSebastien;

	private long noHabSebastien;

	private final RegDate dateSeparation = date(2008, 6, 29);
	final RegDate dateDepart = date(2008, 6, 30);
	final RegDate dateArrivee = dateDepart.getOneDayAfter();
	private final MockCommune communeDepart = MockCommune.Bex;
	private final MockCommune communeArrivee = MockCommune.Zurich;

	@Override
	protected void initServiceCivil() {
		serviceCivilService.setUp(new MockServiceCivil() {

			@Override
			protected void init() {

				final RegDate dateNaissanceSebastien = date(1971, 6, 27);
				indSebastien = addIndividu(noIndSebastien, dateNaissanceSebastien, "Fournier", "Sebastien", true);
				addOrigine(indSebastien, MockCommune.Neuchatel);
				addNationalite(indSebastien, MockPays.Suisse, dateNaissanceSebastien, null);
				addAdresse(indSebastien, TypeAdresseCivil.PRINCIPALE, MockRue.Bex.RouteDuBoet, null, null, null);

				separeIndividu(indSebastien, null, dateSeparation);
			}

		});
	}

	@Etape(id=1, descr="Chargement de l'habitant")
	public void etape1() throws Exception {

		addColAdm(MockOfficeImpot.OID_AIGLE);
		addColAdm(MockOfficeImpot.ACISUCCESSIONS);

		final PersonnePhysique sebastien = addHabitant(noIndSebastien);
		noHabSebastien = sebastien.getNumero();

		addForFiscalPrincipal(sebastien, communeDepart, dateSeparation, null, MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT, null);
		sebastien.setBlocageRemboursementAutomatique(false);

		final DeclarationImpotOrdinaire di2007 = addDeclarationImpot(sebastien, date(2007, 1, 1), date(2007, 12, 31), date(2008, 1, 13), 90);
		{
			addEtat(di2007, di2007.getDernierEtat().getDateObtention().addMonths(2), TypeEtatDeclaration.RETOURNEE);
		}

		addDeclarationImpot(sebastien, date(2008, 1, 1), date(2008, 12, 31), date(2009, 1, 13), 90);
	}

	private void addEtat(DeclarationImpotOrdinaire di, RegDate dateObtention, TypeEtatDeclaration typeEtat) {
		final EtatDeclaration etat = EtatDeclarationHelper.getInstanceOfEtatDeclaration(typeEtat);
		etat.setDateObtention(dateObtention);
		etat.setDeclaration(di);
		final Set<EtatDeclaration> etats = di.getEtats();
		etats.add(etat);
	}

	@Check(id=1, descr="Vérifie que l'habitant a son adresse et son For à Bex")
	public void check1() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");

			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Sebastien fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			final List<Declaration> declarations = sebastien.getDeclarationsSorted();
			assertNotNull(declarations, "Liste des DI nulle");
			assertTrue(declarations.size() > 0, "Mauvais nombre de déclarations");

			// vérification que les adresses civiles sont à Bex
			final AdressesCiviles adresses = new AdressesCiviles(serviceCivilService.getAdresses(noIndSebastien, RegDate.get(), false));
			assertEquals(communeDepart.getNomMinuscule(), adresses.principale.getLocalite(), "L'adresse principale n'est pas à " + communeDepart.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(false);
	}

	@Etape(id=2, descr="Départ de l'individu hors canton")
	public void etape2() throws Exception {
		fermerAdresse(indSebastien, dateDepart);
		ouvrirAdresseZurich(indSebastien, dateArrivee);
	}

	@Check(id=2, descr="Vérifie que l'habitant a toujours son For à Bex mais l'adresse hors canton")
	public void check2() throws Exception {

		{
			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);
			assertNotNull(sebastien, "Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " non existant");

			final ForFiscalPrincipal ffp = sebastien.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal de l'Habitant " + FormatNumeroHelper.numeroCTBToDisplay(noHabSebastien) + " null");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for de Sebastien fausse");
			assertEquals(communeDepart.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(), "For pas attaché à la bonne commune");

			// vérification que les adresses civiles sont à Zurich
			final AdressesCiviles adresses = new AdressesCiviles(serviceCivilService.getAdresses(noIndSebastien, dateDepart.addDays(1), false));
			assertEquals(communeArrivee.getNomMinuscule(), adresses.principale.getLocalite(), "L'adresse principale n'est pas à " + communeArrivee.getNomMinuscule());
		}

		assertBlocageRemboursementAutomatique(false);
	}


	@Etape(id=3, descr="Envoi de l'événement de départ")
	public void etape3() throws Exception {
		long id = addEvenementCivil(TypeEvenementCivil.DEPART_COMMUNE, noIndSebastien, dateDepart, communeDepart.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}

	@Check(id=3, descr="Vérifie que l'habitant n'a plus son for sur Bex mais sur Zurich")
	public void check3() throws Exception {

		// On check que le contribuable est parti
		{
			final EvenementCivilExterne evt = getEvenementCivilRegoupeForHabitant(noHabSebastien);
			assertEquals(EtatEvenementCivil.TRAITE, evt.getEtat(), "Etat invalide");

			final PersonnePhysique sebastien = (PersonnePhysique) tiersDAO.get(noHabSebastien);

			final List<ForFiscal> list = sebastien.getForsFiscauxSorted();

			// For fermé sur Bex
			final ForFiscalPrincipal ffpFerme = (ForFiscalPrincipal) list.get(list.size()-2);
			assertEquals(dateDepart, ffpFerme.getDateFin(), "Le for sur " + communeDepart.getNomMinuscule() + " n'est pas fermé à la bonne date");
			assertEquals(MotifFor.DEPART_HC, ffpFerme.getMotifFermeture(), "Le for sur " + communeDepart.getNomMinuscule() + " n'est pas fermé à la bonne date");

			// For ouvert sur Zurich
			final ForFiscalPrincipal ffpOuvert = (ForFiscalPrincipal) list.get(list.size()-1);
			assertEquals(dateDepart.addDays(1), ffpOuvert.getDateDebut(), "Le for sur " + communeArrivee.getNomMinuscule() + " n'est pas ouvert à la bonne date");
			assertEquals(communeArrivee.getNoOFS(), ffpOuvert.getNumeroOfsAutoriteFiscale(), "Le for ouvert n'est pas sur " + communeArrivee.getNomMinuscule());
			assertEquals(MotifRattachement.DOMICILE, ffpOuvert.getMotifRattachement(), "Le motif de rattachement du for est faux");
			assertEquals(GenreImpot.REVENU_FORTUNE, ffpOuvert.getGenreImpot(), "Le genre d'impot du for est faux");
			assertEquals(ModeImposition.ORDINAIRE, ffpOuvert.getModeImposition(), "Le mode d'imposition du for est faux");

			final DeclarationImpotCriteria diCriteria = new DeclarationImpotCriteria();
			diCriteria.setContribuable(sebastien.getNumero());
			final List<DeclarationImpotOrdinaire> dis = diDAO.find(diCriteria);
			assertNotNull(dis, "Liste des DI nulle");
			assertTrue(dis.size() > 0, "Mauvais nombre de déclarations");

			int nombreTachesDevantEtreGenerees = 0;
			for (DeclarationImpotOrdinaire di : dis) {
				EtatDeclaration etat = di.getDernierEtat();
				if (TypeEtatDeclaration.EMISE == etat.getEtat()) {
					assertTrue(di.isAnnule(), "La DI est pas annulée");
				}
				else {
					nombreTachesDevantEtreGenerees++;
				}
			}

			final TacheCriteria tacheCriteria = new TacheCriteria();
			tacheCriteria.setContribuable(sebastien);
			tacheCriteria.setTypeTache(TypeTache.TacheAnnulationDeclarationImpot);
			final List<Tache> tachesGenerees = tacheDAO.find(tacheCriteria);
			assertEquals(nombreTachesDevantEtreGenerees, tachesGenerees.size(), "Mauvais nombre de tâches d'annulation de DI");
		}

		assertBlocageRemboursementAutomatique(true);
	}

	private void assertBlocageRemboursementAutomatique(boolean blocageAttendu) {
		assertBlocageRemboursementAutomatique(blocageAttendu, tiersDAO.get(noHabSebastien));
	}
}
