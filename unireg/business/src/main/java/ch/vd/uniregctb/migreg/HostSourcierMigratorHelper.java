package ch.vd.uniregctb.migreg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.support.rowset.SqlRowSet;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamilleMenageCommun;
import ch.vd.uniregctb.tiers.TiersHelper;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class HostSourcierMigratorHelper {

	private HostMigratorHelper helper;

	public List<ForFiscalPrincipal> adaptListFor(List<ForFiscalPrincipal> listFor) {
		List<ForFiscalPrincipal> resultat = new ArrayList<ForFiscalPrincipal>();
		List<ForFiscalPrincipal> listeATraiter = new ArrayList<ForFiscalPrincipal>();

		ForFiscalPrincipal forPrecedent = null;

		for (ForFiscalPrincipal forPrincipalATester : listFor) {

			// Si le for a comme motif d'ouverture un changement de mode d'imposition
			// et qui a un mode d'imposition identique au précedent, on l'ignore
			if (forPrecedent != null && MotifFor.CHGT_MODE_IMPOSITION.equals(forPrincipalATester.getMotifOuverture())
					&& forPrecedent.getModeImposition().equals(forPrincipalATester.getModeImposition())) {
				forPrecedent.setDateFin(forPrincipalATester.getDateFin());
				forPrecedent.setMotifFermeture(forPrincipalATester.getMotifFermeture());

			}

			else {
				listeATraiter.add(forPrincipalATester);
				forPrecedent = forPrincipalATester;
			}
		}

		for (int i = 0; i < listeATraiter.size(); i++) {

			ForFiscalPrincipal forCourant = listeATraiter.get(i);

			// Dans le cas ou il y a plusieurs forFiscaux qui se suivent
			// on ferme le courant avec la date de début -1 du suivant. le motif de fermeture est le
			// motif d'ouverture du suivant.

			if (i + 1 < listeATraiter.size()) {

				ForFiscalPrincipal forSuivant = listeATraiter.get(i + 1);
				if (forSuivant.getDateDebut().isAfter(forCourant.getDateDebut())) {
					forCourant.setDateFin(forSuivant.getDateDebut().getOneDayBefore());
					forCourant.setMotifFermeture(forSuivant.getMotifOuverture());
				}

			}

			resultat.add(forCourant);

		}

		return resultat;
	}

	public List<ForFiscalPrincipal> removeForSameDate(List<ForFiscalPrincipal> listForAdresses, List<ForFiscalPrincipal> listForEtatCivil,
			List<ForFiscalPrincipal> listForTypeImposition) {

		List<ForFiscalPrincipal> resultat = new ArrayList<ForFiscalPrincipal>();

		List<ForFiscalPrincipal> adressesMaj = new ArrayList<ForFiscalPrincipal>();
		List<ForFiscalPrincipal> typeImpositionMaj = new ArrayList<ForFiscalPrincipal>();

		for (ForFiscalPrincipal forFiscalAdresse : listForAdresses) {
			boolean conflit = false;
			for (ForFiscalPrincipal forFiscalTypeImpo : listForTypeImposition) {
				if (forFiscalAdresse.getDateDebut().equals(forFiscalTypeImpo.getDateDebut())) {
					conflit = true;
					break;
				}
			}
			if (!conflit) {
				for (ForFiscalPrincipal forFiscalEtat : listForEtatCivil) {

					if (forFiscalAdresse.getDateDebut().equals(forFiscalEtat.getDateDebut())) {
						conflit = true;
						break;
					}

				}
			}

			if (!conflit) {
				adressesMaj.add(forFiscalAdresse);
			}

		}

		for (ForFiscalPrincipal forFiscalTypeImpo : listForTypeImposition) {
			boolean conflit = false;
			for (ForFiscalPrincipal forFiscalEtat : listForEtatCivil) {

				if (forFiscalTypeImpo.getDateDebut().equals(forFiscalEtat.getDateDebut())) {
					conflit = true;
					break;
				}

			}

			if (!conflit) {
				typeImpositionMaj.add(forFiscalTypeImpo);
			}

		}

		resultat.addAll(adressesMaj);
		resultat.addAll(listForEtatCivil);
		resultat.addAll(typeImpositionMaj);
		Collections.sort(resultat, new DateRangeComparator<ForFiscal>());

		return resultat;
	}

	public void mergeAndSaveFor(PersonnePhysique sourcier, List<ForFiscalPrincipal> forCalcules, boolean isConjointPrincipal, List<DateRange> listePeriodeCouple) {
		for (ForFiscalPrincipal forFiscalPrincipal : forCalcules) {
			DateRange periodeMenage = findMenageCommunIntersectWithFor(forFiscalPrincipal, sourcier);
			if (periodeMenage != null) {
				if (isConjointPrincipal) {

					if (forFiscalPrincipal.getDateDebut().isBefore(periodeMenage.getDateDebut())) {
						// on adapte le for est on l'ajoute à la personne physique
						forFiscalPrincipal.setDateFin(periodeMenage.getDateDebut().getOneDayBefore());
						forFiscalPrincipal.setMotifFermeture(MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION);
						addForOnContribuable(forFiscalPrincipal, sourcier);
					}
					else {
						//Le sourcier doit être en charge du menage
						MenageCommun couple = helper.tiersService.findMenageCommun(sourcier, periodeMenage.getDateDebut());
						SituationFamille situation = couple.getSituationFamilleAt( forFiscalPrincipal.getDateDebut());
						if (situation instanceof SituationFamilleMenageCommun) {
							SituationFamilleMenageCommun situationSource = (SituationFamilleMenageCommun)situation;
							Contribuable contribuablePrincipal = situationSource.getContribuablePrincipal();
							if (contribuablePrincipal!=null   && contribuablePrincipal.getNumero().equals(sourcier.getNumero())) {
								addForOnContribuable(forFiscalPrincipal, couple);
							}
						}


					}
				}

			}
			else {
				//Verification suplementaire, on vérifie que le for a rajouter sur la personne physique
				//ne correspond pas à une période de couple
				if(!isForOnPeriodeCouple(forFiscalPrincipal, listePeriodeCouple))
				addForOnContribuable(forFiscalPrincipal, sourcier);
			}
		}

	}

	private boolean isForOnPeriodeCouple(ForFiscalPrincipal forFiscal,List<DateRange> listePeriodeCouple){
		for (DateRange r : listePeriodeCouple) {
			if (DateRangeHelper.intersect(forFiscal, r)) {
				return true;
			}
		}
		return false;
	}

	public void mergeAndSaveForOld(PersonnePhysique sourcier, List<ForFiscalPrincipal> forCalcules, boolean isConjointPrincipal) {
		for (ForFiscalPrincipal forFiscalPrincipal : forCalcules) {

			DateRange periodeMenage = findMenageCommunIntersectWithFor(forFiscalPrincipal, sourcier);
			if (periodeMenage == null) {
				addForOnContribuable(forFiscalPrincipal, sourcier);
			}
			else {
				if (isConjointPrincipal) {
					MenageCommun couple = helper.tiersService.findMenageCommun(sourcier, periodeMenage.getDateDebut());
					if (couple != null) {
						addForOnContribuable(forFiscalPrincipal, couple);
					}
				}

			}
		}

	}


	/**
	 * Permet de savoir si un for a creer sur une personne recoupe une periode d'appartenance à un couple dans ce cas le for doit être créé
	 * sur le couple
	 *
	 * @param forFiscal
	 * @param personne
	 * @return la periode d'appartenance a un couple qui recoupe le for, null sinon
	 */

	private DateRange findMenageCommunIntersectWithFor(ForFiscalPrincipal forFiscal, PersonnePhysique personne) {
		/*
		 * On n'autorise pas la présence de forssur la personne durant la ou les périodes d'appartenance à un couple
		 */
		// Détermine les périodes de validités ininterrompues du ménage commun
		List<RapportEntreTiers> rapportsMenages = new ArrayList<RapportEntreTiers>();
		Set<RapportEntreTiers> rapports = personne.getRapportsSujet();
		if (rapports != null) {
			for (RapportEntreTiers r : rapports) {
				if (!r.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(r.getType())) {
					rapportsMenages.add(r);
				}
			}
		}
		Collections.sort(rapportsMenages, new DateRangeComparator<RapportEntreTiers>());
		final List<DateRange> periodes = DateRangeHelper.collateRange(rapportsMenages);
		for (DateRange r : periodes) {
			if (DateRangeHelper.intersect(forFiscal, r)) {
				return r;
			}
		}

		return null;
	}

	private void addForOnContribuableOld(ForFiscalPrincipal forFiscal, Contribuable contribuable) {
		List<ForFiscal> forSorted = contribuable.getForsFiscauxSorted();
		// recherche d'un role ordinaire,
		// Si on le trouve, le dernier for Sourcier doit s'arreter un jour avant la date d'ouverture du for Ordinaire
		// la suite de la vie fiscale est gérer par unireg PP (role ordinaire)

		// en cas de role Ordinaire, on determine le dernier for sourcier a inserer
		RegDate dateDebutRoleOrdinaire = findRoleOrdinaire(contribuable);

		if (dateDebutRoleOrdinaire != null
				&& (forFiscal.getDateFin() == null || dateDebutRoleOrdinaire.isBeforeOrEqual(forFiscal.getDateFin()))) {
			forFiscal.setDateFin(dateDebutRoleOrdinaire.getOneDayBefore());
			forFiscal.setMotifFermeture(MotifFor.CHGT_MODE_IMPOSITION);

		}

		if (forSorted != null && !forSorted.isEmpty()) {
			// recuperation et vérification des fors fiscaux de type revenuFortune
			List<ForFiscalPrincipal> forPrincipauxSorted = new ArrayList<ForFiscalPrincipal>();
			for (ForFiscal forATester : forSorted) {

				if (forATester instanceof ForFiscalPrincipal) {

					ForFiscalPrincipal forPrincipalATester = (ForFiscalPrincipal) forATester;

					forPrincipauxSorted.add(forPrincipalATester);

				}
			}
			if (!forPrincipauxSorted.isEmpty()) {

				// On teste si le for à inserer est ouvert avant ou après la liste des fors
				ForFiscalPrincipal premierForFiscal = forPrincipauxSorted.get(0);
				ForFiscalPrincipal dernierForFiscal = forPrincipauxSorted.get(forSorted.size() - 1);
				// On teste si le for à inserer est ouvert avant la liste des fors
				if (forFiscal.getDateDebut().isBefore(premierForFiscal.getDateDebut())) {

					forFiscal.setDateFin(premierForFiscal.getDateDebut().getOneDayBefore());
					forFiscal.setMotifFermeture(premierForFiscal.getMotifOuverture());

				}
				// On teste si le for à inserer est ouvert après la liste des fors
				else if (forFiscal.getDateDebut().isAfter(dernierForFiscal.getDateDebut())) {

					dernierForFiscal.setDateFin(forFiscal.getDateDebut().getOneDayBefore());
					dernierForFiscal.setMotifFermeture(forFiscal.getMotifOuverture());

				}
				// le for à inserer est au milieu
				else {
					for (int i = 0; i < forPrincipauxSorted.size(); i++) {

						ForFiscalPrincipal forCourant = forPrincipauxSorted.get(i);
						ForFiscalPrincipal forSuivant = null;
						// on recupere le for suivant si il existe
						if (i + 1 < forPrincipauxSorted.size()) {
							forSuivant = forPrincipauxSorted.get(i + 1);
						}
						if (forSuivant != null && forFiscal.getDateDebut().isAfter(forCourant.getDateDebut())
								&& forFiscal.getDateDebut().isBefore(forSuivant.getDateDebut())) {
							// modification du for courant
							forCourant.setDateFin(forFiscal.getDateDebut().getOneDayBefore());
							forCourant.setMotifFermeture(forFiscal.getMotifOuverture());

							// modification du for à inserer
							forFiscal.setDateFin(forSuivant.getDateDebut().getOneDayBefore());
							forFiscal.setMotifFermeture(forSuivant.getMotifOuverture());

						}

					}
				}
			}

		}
		contribuable.addForFiscal(forFiscal);
	}

	private void addForOnContribuable(ForFiscalPrincipal forFiscal, Contribuable contribuable) {
		// recherche d'un role ordinaire,
		// Si on le trouve, le dernier for Sourcier doit s'arreter un jour avant la date d'ouverture du for Ordinaire
		// la suite de la vie fiscale est gérer par unireg PP (role ordinaire)

		// en cas de role Ordinaire, on determine le dernier for sourcier a inserer
		RegDate dateDebutRoleOrdinaire = findRoleOrdinaire(contribuable);

		if (dateDebutRoleOrdinaire != null) {
			if (forFiscal.getDateDebut().isBefore(dateDebutRoleOrdinaire)) {
				if (forFiscal.getDateFin() == null || forFiscal.getDateFin().isAfterOrEqual(dateDebutRoleOrdinaire)) {
					forFiscal.setDateFin(dateDebutRoleOrdinaire.getOneDayBefore());
					forFiscal.setMotifFermeture(MotifFor.CHGT_MODE_IMPOSITION);

				}
				else if(forFiscal.getDateFin() !=null && forFiscal.getDateFin().equals(dateDebutRoleOrdinaire.getOneDayBefore())){
					forFiscal.setMotifFermeture(MotifFor.CHGT_MODE_IMPOSITION);

				}

				addAnSaveForOnContribuable(forFiscal,contribuable);


			}
		}
		else {
			addAnSaveForOnContribuable(forFiscal,contribuable);

		}
	}

	public RegDate findRoleOrdinaire(Contribuable contribuable) {

		RegDate dateRoleOrdinaire = returnRoleOrdinaire(contribuable);

		if (dateRoleOrdinaire == null && contribuable instanceof PersonnePhysique) {
			//Recherche du role ordinaire sur un couple s'il existe
			PersonnePhysique sourcierCourant = (PersonnePhysique)contribuable;
			MenageCommun couple = findDernierMenageCommunActif(sourcierCourant);
			if (couple!=null) {
				dateRoleOrdinaire = returnRoleOrdinaire(couple);
			}

		}
		return dateRoleOrdinaire;
	}

	private RegDate returnRoleOrdinaire(Contribuable contribuable){

		RegDate dateRoleOrdinaire = null;
		List<ForFiscalPrincipal> forPrincipaux = contribuable.getForsFiscauxPrincipauxActifsSorted();
		if (forPrincipaux != null && !forPrincipaux.isEmpty()) {
			for (ForFiscalPrincipal forFiscalPrincipal : forPrincipaux) {
				if (ModeImposition.ORDINAIRE.equals(forFiscalPrincipal.getModeImposition())
						|| ModeImposition.MIXTE_137_2.equals(forFiscalPrincipal.getModeImposition())
						||ModeImposition.MIXTE_137_1.equals(forFiscalPrincipal.getModeImposition())) {
					dateRoleOrdinaire = forFiscalPrincipal.getDateDebut();
					break;
				}
			}
		}
		return dateRoleOrdinaire;
	}

	public void setHelper(HostMigratorHelper helper) {
		this.helper = helper;
	}



public MenageCommun findDernierMenageCommunActif(PersonnePhysique personne){
	MenageCommun menage = null;
	if (personne.getRapportsSujet()!=null) {
		menage =helper.tiersService.findMenageCommun(personne,RegDate.get());
	}
	return menage;

}

	public PersonnePhysique getConjointSourcier(PersonnePhysique sourcierPrincipal) {
		PersonnePhysique sourcierConjoint = null;
		MenageCommun menage = findDernierMenageCommunActif(sourcierPrincipal);
		if (menage != null) {
			Set<PersonnePhysique> personnes = TiersHelper.getComposantsMenage(menage, null);

			if (personnes != null && personnes.size() == 2) {

				final Iterator<PersonnePhysique> iter = personnes.iterator();
				final PersonnePhysique p1 = iter.next();
				final PersonnePhysique p2 = iter.next();
				if (p1.getNumero() != sourcierPrincipal.getNumero()) {
					sourcierConjoint = p1;
				}
				else if (p2.getNumero() != sourcierPrincipal.getNumero()) {
					sourcierConjoint = p2;
				}

			}

		}
		return sourcierConjoint;
	}

	public void addAnSaveForOnContribuable(ForFiscalPrincipal forFiscal, Contribuable contribuable){

		if (!forIsPresent(forFiscal, contribuable)) {

			forFiscal.setTiers(contribuable);
			contribuable.addForFiscal(forFiscal);
			helper.hibernateTemplate.save(forFiscal);
		}


	}


	public boolean isDoublon(Long numeroIndividu) {

		if (numeroIndividu !=null && numeroIndividu > 0) {

			String sql = "select NO_IND_REG_FISCAL, count(*) as nombre from " + helper.getTableIs("SOURCIER")
					+ " where NO_IND_REG_FISCAL =" + numeroIndividu + " group by NO_IND_REG_FISCAL having count (*)> 1";
			SqlRowSet rsDoublon = helper.isTemplate.queryForRowSet(sql);

			if (rsDoublon.first()) {
				return true;
			}
		}

		return false;
	}

	private boolean forIsPresent(ForFiscal forFiscalATester, Contribuable c){

		List<ForFiscal> listeFor = c.getForsFiscauxSorted();
		if (listeFor!=null) {
			for (ForFiscal forFiscal : listeFor) {
				if (forFiscal.getDateDebut()!=null && forFiscal.getDateDebut().equals(forFiscalATester.getDateDebut())) {
					if (forFiscal.getDateFin()!=null && forFiscal.getDateFin().equals(forFiscalATester.getDateFin())) {
						return true;
					}
					else if(forFiscal.getDateFin()==null && forFiscalATester.getDateFin()==null){
						return true;
					}

				}

			}
		}
		return false;

	}


	public boolean situtionIsPresent(SituationFamille situationATester, Contribuable c){

		Set<SituationFamille> setSituation = c.getSituationsFamille();

		if (setSituation!=null) {
			for (SituationFamille situationFamille : setSituation) {
				if (situationFamille.getDateDebut()!=null && situationFamille.getDateDebut().equals(situationATester.getDateDebut())) {
					if (situationFamille.getDateFin()!=null && situationFamille.getDateFin().equals(situationATester.getDateFin())) {
						return true;
					}
					else if(situationFamille.getDateFin()==null && situationATester.getDateFin()==null){
						return true;
					}

				}

			}
		}
		return false;

	}

}
