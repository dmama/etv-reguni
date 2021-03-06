package ch.vd.unireg.norentes.civil.separation;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.mock.DefaultMockIndividuConnector;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.mock.MockCommune;
import ch.vd.unireg.norentes.annotation.Check;
import ch.vd.unireg.norentes.annotation.Etape;
import ch.vd.unireg.norentes.common.EvenementCivilScenario;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.ForsParType;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.tiers.Tache;
import ch.vd.unireg.tiers.TacheCriteria;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeEtatTache;
import ch.vd.unireg.type.TypeEvenementCivil;
import ch.vd.unireg.type.TypeTache;

public class Ec_6000_08_Separation_JIRA1057_Scenario extends EvenementCivilScenario {

	public static final String NAME = "6000_08_Separation";

	@Override
	public TypeEvenementCivil geTypeEvenementCivil() {
		return TypeEvenementCivil.SEPARATION;
	}

	@Override
	public String getDescription() {
		return "Evénement de séparation d'un couple avec des fors secondaire";
	}

	@Override
	public String getName() {
		return NAME;
	}

	private static final long noIndMomo = 54321;
	private static final long noIndBea = 23456;

	private final RegDate dateMariage = RegDate.get(1978, 1, 6);			// 06.01.1978
	private final RegDate dateSeparation = RegDate.get(2008, 1, 1);			// 01.01.2008
	
	private final Commune commune = MockCommune.Renens;
	private final Commune communeSecondaire = MockCommune.Aubonne;
	
	private long noHabMomo;
	private long noHabBea;
	private long noMenage;

	private final class LocalIndividuConnector extends DefaultMockIndividuConnector {
		
		void separeIndividus(RegDate dateSeparation) {
			separeIndividus(getIndividu(noIndMomo), getIndividu(noIndBea), dateSeparation);
		}
		
	}
	
	private LocalIndividuConnector serviceCivil;

	@Override
	protected void initServiceCivil() {
		serviceCivil = new LocalIndividuConnector();
		serviceCivilService.setUp(serviceCivil);
	}

	@Etape(id=1, descr="Chargement des habitants et du ménage commun")
	public void step1() {

		// Maurice
		final PersonnePhysique momo = addHabitant(noIndMomo);
		noHabMomo = momo.getNumero();

		// Hélène
		final PersonnePhysique bea = addHabitant(noIndBea);
		noHabBea = bea.getNumero();

		// ménage
		{
			final MenageCommun menage = (MenageCommun) tiersDAO.save(new MenageCommun());
			
			noMenage = menage.getNumero();
			
			tiersService.addTiersToCouple(menage, momo, dateMariage, null);
			tiersService.addTiersToCouple(menage, bea, dateMariage, null);
			
			addForFiscalPrincipal(menage, commune, dateMariage, null, MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION, null);
			addForFiscalSecondaire(menage, communeSecondaire.getNoOFS(), dateMariage.addMonths(3), null);
			addForFiscalSecondaire(menage, communeSecondaire.getNoOFS(), dateMariage.addMonths(8), null);
		}
		
	}
	
	@Check(id=1, descr="Vérifie que les habitants n'ont pas de For ouvert et le For du ménage existe")
	public void check1() {
		{
			final PersonnePhysique momo = (PersonnePhysique) tiersDAO.get(noHabMomo);
			final ForFiscalPrincipal ffp = momo.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + momo.getNumero() + " non null");
		}

		{
			final PersonnePhysique bea = (PersonnePhysique) tiersDAO.get(noHabBea);
			final ForFiscalPrincipal ffp = bea.getDernierForFiscalPrincipal();
			assertNull(ffp, "For principal de l'Habitant " + bea.getNumero() + " non null");
		}

		{
			final MenageCommun mc = (MenageCommun)tiersDAO.get(noMenage);
			ForsParType forsParType = mc.getForsParType(false);
			assertEquals(1, forsParType.principauxPP.size(), "Le ménage a plus d'un for principal");
			assertEquals(0, forsParType.principauxPM.size(), "Le ménage a des fors principaux PM ???");
			final ForFiscalPrincipal ffp = mc.getDernierForFiscalPrincipal();
			assertNotNull(ffp, "For principal du Ménage " + mc.getNumero() + " null");
			assertEquals(dateMariage, ffp.getDateDebut(), "Date de début du dernier for fausse");
			assertNull(ffp.getDateFin(), "Date de fin du dernier for fausse");
			assertEquals(commune.getNoOFS(), ffp.getNumeroOfsAutoriteFiscale(),
					"Le dernier for n'est pas sur " + commune.getNomOfficiel());
			
			assertEquals(2, forsParType.secondaires.size(), "Le ménage a plus de deux fors secondaires");
			{
				SituationFamille sf = mc.getSituationFamilleActive();
				assertNull(sf, "Aucune situation famille aurait dû être trouvée");
			}
		}
	}
	
	@Etape(id=2, descr="Envoi de l'événement de séparation")
	public void etape2() throws Exception {
		serviceCivil.separeIndividus(dateSeparation);
		
		long id = addEvenementCivil(TypeEvenementCivil.SEPARATION, noIndBea, dateSeparation, commune.getNoOFS());
		commitAndStartTransaction();
		traiteEvenements(id);
	}
	
	@Check(id=2, descr="Vérifie qu'une tâche de control dossier a été générée")
	public void check2() {

		final MenageCommun menage = (MenageCommun) tiersDAO.get(noMenage);
		
		checkTachesGenerees(menage, null, 6);
		checkTachesGenerees(menage, TypeTache.TacheControleDossier, 1);
		checkTachesGenerees(menage, TypeTache.TacheEnvoiDeclarationImpotPP, 5); // 2003 + 2004 + 2005 + 2006 + 2007
	}

	private void checkTachesGenerees(final Contribuable contribuable, TypeTache typeTache, int expected) {
		final TacheCriteria tacheCriteria = new TacheCriteria();
		tacheCriteria.setContribuable(contribuable);
		tacheCriteria.setEtatTache(TypeEtatTache.EN_INSTANCE);
		tacheCriteria.setTypeTache(typeTache);
		final List<Tache> taches = tacheDAO.find(tacheCriteria);
		assertNotNull(taches, "Liste des tâches en instance nulle");
		
		String msg = expected + " tâche(s)";
		if (typeTache != null) {
			msg += " de type " + typeTache.toString();
		}
		msg += " aurait(ent) dû être générée(s)";
		assertEquals(expected, taches.size(), msg);
	}
	
}
