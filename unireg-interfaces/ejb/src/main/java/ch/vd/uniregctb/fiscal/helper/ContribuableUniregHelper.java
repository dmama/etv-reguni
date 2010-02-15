/**
 *
 */
package ch.vd.uniregctb.fiscal.helper;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import ch.vd.infrastructure.model.Commune;
import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.infrastructure.service.ServiceInfrastructure;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.date.DateRangeHelper.Range;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.registre.civil.model.impl.EtatCivilImpl;
import ch.vd.registre.common.service.RegistreException;
import ch.vd.registre.fiscal.model.EnumEtatDeclarationImpot;
import ch.vd.registre.fiscal.model.EnumTypeContribuable;
import ch.vd.registre.fiscal.model.EnumTypeFor;
import ch.vd.registre.fiscal.model.impl.ForImpl;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.HorsCanton;
import ch.vd.uniregctb.metier.assujettissement.HorsSuisse;
import ch.vd.uniregctb.metier.assujettissement.SourcierMixte;
import ch.vd.uniregctb.metier.assujettissement.VaudoisDepense;
import ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire;
import ch.vd.uniregctb.situationfamille.VueSituationFamille;
import ch.vd.uniregctb.situationfamille.VueSituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.ForFiscalAutreImpot;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalRevenuFortune;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForGestion;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * @author xsibnm
 *
 */
public class ContribuableUniregHelper {

	/**
	 * Permet de determiner le type de contribuable à partir de son for s'il en a
	 *
	 * @param contribuableUnireg
	 * @param date
	 * @return un type de contribuable
	 */
	public static EnumTypeContribuable getTypeContribuable(final Contribuable contribuableUnireg, RegDate date) {
		// On recupere le for principal valide à la date N
		ForFiscalPrincipal forPrincipal = contribuableUnireg.getForFiscalPrincipalAt(date);
		List<Assujettissement> listAssujetissement = null;
		try {
			listAssujetissement = Assujettissement.determine(contribuableUnireg, date.year());
		}
		catch (AssujettissementException e) {
			return null;
		}

		if (listAssujetissement != null && listAssujetissement.isEmpty() == false) {
			Assujettissement assujettissement = listAssujetissement.get(listAssujetissement.size() - 1);

			if (assujettissement instanceof VaudoisOrdinaire) {
				return EnumTypeContribuable.ORDINAIRE;
			}
			else if (assujettissement instanceof VaudoisDepense) {
				return EnumTypeContribuable.DEPENSE;
			}

			else if (assujettissement instanceof HorsCanton) {
				return EnumTypeContribuable.HORS_CANTON;
			}
			else if (assujettissement instanceof HorsSuisse) {
				return EnumTypeContribuable.HORS_SUISSE;
			}
			else if (assujettissement instanceof SourcierMixte) {
				return EnumTypeContribuable.SOURCIER_MIXTE;
			}
		}
		return null;
	}

	/**
	 * Permet de recuperer un etat civil type unireg et de le traduire en etat civile sous le modèle host-interface
	 *
	 * @param situationFamille
	 * @return Etat civil au format host-interfaces
	 */
	public static EtatCivilImpl getEtatCivil(final VueSituationFamille situationFamille) {
		EtatCivilImpl etatCivil = new EtatCivilImpl();

		if (situationFamille != null) {
			if (situationFamille.getDateDebut() != null) {
				etatCivil.setDateDebutValidite(situationFamille.getDateDebut().asJavaDate());
			}
			else {
				etatCivil.setDateDebutValidite(null);
			}
			etatCivil.setTypeEtatCivil(convertEtatCivil(situationFamille.getEtatCivil()));
		}
		etatCivil.setNoSequence(0);
		return etatCivil;
	}

	/**
	 * Convertit la valeur null en chaine vide poour une string passée en paramètre
	 *
	 * @param data
	 * @return chaine vide
	 */
	public static String convertNullToEmpty(final String data) {
		String maChaine;
		maChaine = (data == null) ? "" : data;
		return maChaine;

	}

	/**
	 * permet de ocnvertir un etat civil unireg en etat civil Host
	 *
	 * @param etat
	 * @return etat civil host
	 */
	public static EnumTypeEtatCivil convertEtatCivil(final EtatCivil etat) {
		if (etat == null) {
			return null;
		}
		else if (EtatCivil.CELIBATAIRE.equals(etat)) {
			return EnumTypeEtatCivil.CELIBATAIRE;
		}
		else if (EtatCivil.DIVORCE.equals(etat)) {
			return EnumTypeEtatCivil.DIVORCE;
		}
		else if (EtatCivil.MARIE.equals(etat)) {
			return EnumTypeEtatCivil.MARIE;
		}
		else if (EtatCivil.LIE_PARTENARIAT_ENREGISTRE.equals(etat)) {
			return EnumTypeEtatCivil.PACS;
		}
		else if (EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT.equals(etat)) {
			return EnumTypeEtatCivil.PACS_ANNULE;
		}
		else if (EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT.equals(etat)) {
			return EnumTypeEtatCivil.PACS_INTERROMPU;
		}
		else if (EtatCivil.SEPARE.equals(etat)) {
			return EnumTypeEtatCivil.SEPARE;
		}
		else if (EtatCivil.VEUF.equals(etat)) {
			return EnumTypeEtatCivil.VEUF;
		}
		else {
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + etat.name() + "]");
		}
	}

	/**
	 * Conversion du motif d'ouverture pour les fors
	 *
	 * @param for
	 * @return lr motif douverture Host
	 * @throws RegistreException
	 */
	public static String convertMotifOuverture(final ForFiscalRevenuFortune forUnireg, Contribuable contribuableUnireg,
			ServiceInfrastructure serviceInfrastructure) throws RegistreException {

		MotifFor motif = forUnireg.getMotifOuverture();

		if (MotifFor.ARRIVEE_HS.equals(motif)) {

			return new String("995");
		}
		else if (MotifFor.ARRIVEE_HC.equals(motif)) {

			return new String("992");
		}
		else if (MotifFor.DEMENAGEMENT_VD.equals(motif)) {
			if (forUnireg.getDateDebut() != null) {
				String numeroACICommuneDepart = null;
				ForFiscalPrincipal forFiscal = contribuableUnireg.getForFiscalPrincipalAt(forUnireg.getDateDebut().getOneDayBefore());
				if (forFiscal != null) {
					int numeroCommuneDepart = forFiscal.getNumeroOfsAutoriteFiscale();
					try {
						Commune commune = serviceInfrastructure.getCommune(numeroCommuneDepart);
						if (commune != null) {
							numeroACICommuneDepart = serviceInfrastructure.getCommune(numeroCommuneDepart).getNoACI();
							return numeroACICommuneDepart;
						}
						else {
							return new String("NA");
						}

					}

					catch (InfrastructureException e) {
						throw new RegistreException(
								"Problème durant la recherche du numero ACI  ",
								e);
					} catch (RemoteException e) {

						throw new RegistreException(
								"Problème d'accés distant au service infrastructure distant  ",
								e);
					}

				} 
				else {
					return new String("NA");
				}
			}
			else {
				return new String("NA");
			}
		}

		else if (MotifFor.MAJORITE.equals(motif) || MotifFor.PERMIS_C_SUISSE.equals(motif) || MotifFor.CHGT_MODE_IMPOSITION.equals(motif)
				|| MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(motif) || MotifFor.INDETERMINE.equals(motif)) {
			return new String("7");
		}

		else if (MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT.equals(motif)) {
			return new String("10");
		}

		else if (MotifFor.VEUVAGE_DECES.equals(motif)) {
			return new String("12");
		}

		else if (MotifFor.ACHAT_IMMOBILIER.equals(motif)) {
			return new String("4");
		}
		else if (MotifFor.DEBUT_EXPLOITATION.equals(motif)) {
			return new String("6");
		}
		else {
			return null;
		}

	}

	/**
	 * Conversion du motif d'ouverture pour les fors
	 *
	 * @param motif
	 * @return le motif de fermeture Host
	 * @throws RegistreException
	 */
	public static String convertMotifFermeture(final ForFiscalRevenuFortune forUnireg, Contribuable contribuableUnireg,
			ServiceInfrastructure serviceInfrastructure) throws RegistreException {

		MotifFor motif = forUnireg.getMotifFermeture();

		if (MotifFor.DEPART_HC.equals(motif)) {
			return new String("991");
		}
		else if (MotifFor.DEPART_HS.equals(motif)) {
			return new String("996");
		}
		else if (MotifFor.DEMENAGEMENT_VD.equals(motif)) {
			String numeroACICommuneArrivee = null;
			if (forUnireg.getDateFin() != null) {
				ForFiscalPrincipal forFiscal = contribuableUnireg.getForFiscalPrincipalAt(forUnireg.getDateFin().getOneDayAfter());
				if (forFiscal!=null) {
					int numeroCommuneArrivee = forFiscal.getNumeroOfsAutoriteFiscale();
					try {
						Commune commune = serviceInfrastructure.getCommune(numeroCommuneArrivee);
						if (commune!=null) {
							numeroACICommuneArrivee = commune.getNoACI();
							return numeroACICommuneArrivee;
						}
						else {
							return new String("NA");
						}
						
		
					}
		
					catch (InfrastructureException e) {
						throw new RegistreException("Problème durant la recherche du numero ACI ", e);
					}
					catch (RemoteException e) {
		
						throw new RegistreException("Problème d'accés distant au service infrastructure distant  ", e);
					}
		
					
				}
				else {
					return new String("NA");
				}
						
			}
			else {
				return new String("NA");
			}
		}

		else if (MotifFor.SEPARATION_DIVORCE_DISSOLUTION_PARTENARIAT.equals(motif)) {
			return new String("14");
		}
		else if (MotifFor.MARIAGE_ENREGISTREMENT_PARTENARIAT_RECONCILIATION.equals(motif)) {
			return new String("8");
		}
		else if (MotifFor.VEUVAGE_DECES.equals(motif)) {
			return new String("9");
		}
		else if (MotifFor.VENTE_IMMOBILIER.equals(motif)) {
			return new String("3");
		}
		else if (MotifFor.FIN_EXPLOITATION.equals(motif)) {
			return new String("5");
		}

		else {
			return null;
		}

	}

	/**
	 * convertie l'etat de declaration Unireg en etat de declaration Host
	 *
	 * @param type
	 * @return l'etat de la déclaration
	 */

	public static EnumEtatDeclarationImpot convertEtatDeclaration(final TypeEtatDeclaration type) {

		if (TypeEtatDeclaration.EMISE.equals(type)) {
			return EnumEtatDeclarationImpot.ENVOYE;
		}
		else if (TypeEtatDeclaration.RETOURNEE.equals(type)) {
			return EnumEtatDeclarationImpot.QUITTANCE;
		}
		else if (TypeEtatDeclaration.SOMMEE.equals(type)) {
			return EnumEtatDeclarationImpot.SOMME;
		}
		else if (TypeEtatDeclaration.ECHUE.equals(type)) {
			return EnumEtatDeclarationImpot.ECHUE;
		}
		else {
			return null;
		}

	}

	/**
	 * Recherche le menage commun actif auquel est rattaché une personne
	 *
	 * @param personne
	 *            la personne potentiellement rattachée à un ménage commun
	 * @param periode
	 * @return le ménage commun trouvé, ou null si cette personne n'est pas rattaché au ménage.
	 * @throws RegistreException
	 *             si plus d'un ménage commun est trouvé.
	 */
	public static MenageCommun getMenageCommunActifAt(final PersonnePhysique personne, final Range periode) throws RegistreException {

		if (personne == null) {
			return null;
		}

		MenageCommun menageCommun = null;

		final Set<RapportEntreTiers> rapportsSujet = personne.getRapportsSujet();
		if (rapportsSujet != null) {
			for (RapportEntreTiers rapportSujet : rapportsSujet) {
				if (!rapportSujet.isAnnule() && TypeRapportEntreTiers.APPARTENANCE_MENAGE.equals(rapportSujet.getType())
						&& RegDateHelper.isBeforeOrEqual(periode.getDateDebut(), rapportSujet.getDateFin(), NullDateBehavior.LATEST)) {
					/*
					 * le rapport de l'apartenance a été trouvé, on en déduit donc le tiers ménage
					 */
					if (menageCommun != null) {
						throw new RegistreException("Plus d'un ménage commun trouvé pour la personne = [" + personne.toString() + "]");
					}

					menageCommun = (MenageCommun) rapportSujet.getObjet();
					// on verifie la presence d'un for principal ou secondaire sur la période

					if (!isForActifSurPeriode(menageCommun, periode)) {
						menageCommun = null;
					}
				}
			}
		}

		return menageCommun;
	}

	/**
	 * Recherche la presence d'un for actif sur une période
	 *
	 * @param contribuableUnireg
	 * @param periode
	 * @return booleen
	 */
	public static boolean isForActifSurPeriode(final ch.vd.uniregctb.tiers.Contribuable contribuableUnireg, final Range periode) {

		for (ForFiscal f : contribuableUnireg.getForsFiscaux()) {
			if (DateRangeHelper.intersect(f, periode) == true && !f.isAnnule()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * calcul la date de debut de la période d'assujettisement
	 *
	 * @param contribuableUnireg
	 * @param periodeRecherche
	 * @return la date de debut
	 * @throws AssujettissementException
	 */
	public static Range getDateAssujetissement(final ch.vd.uniregctb.tiers.Contribuable contribuableUnireg, final Range periodeRecherche)
			throws AssujettissementException {
		int annee = periodeRecherche.getDateFin().year();
		RegDate dateDebutAssujetti = null;
		RegDate dateFinAssujetti = null;
		boolean assujettiDebut = true;
		boolean assujettiFin = true;
		List<ch.vd.uniregctb.metier.assujettissement.Assujettissement> assujettissementUnireg = null;

		// Recherche de la date de début d'assujettissement
		while (assujettiDebut) {
			assujettissementUnireg = Assujettissement.determine(contribuableUnireg, annee);
			if (assujettissementUnireg != null && !assujettissementUnireg.isEmpty()) {
				if (assujettissementUnireg.size() == 1) {
					dateDebutAssujetti = assujettissementUnireg.get(0).getDateDebut();
					annee--;
				}
				else {
					// on prend le dernier assujetissement dans l'année (cas exotiques)
					dateDebutAssujetti = assujettissementUnireg.get(assujettissementUnireg.size() - 1).getDateDebut();
					// on arrete la recherche
					assujettiDebut = false;
				}

			}
			else {
				assujettiDebut = false;
			}

		}

		annee = periodeRecherche.getDateFin().year();
		// Recherche de la date de fin d'assujettissement ne doit pas depasser l'année en cours
		while (assujettiFin && annee < RegDate.get().year()) {

			assujettissementUnireg = Assujettissement.determine(contribuableUnireg, annee);
			if (assujettissementUnireg != null && !assujettissementUnireg.isEmpty()) {
				if (assujettissementUnireg.size() == 1) {
					dateFinAssujetti = assujettissementUnireg.get(0).getDateFin();
					annee++;
				}
				else {
					// on prend le premier assujetissement dans l'année (cas exotiques)
					dateFinAssujetti = assujettissementUnireg.get(0).getDateFin();
					// on arrete la recherche
					assujettiFin = false;
				}

			}
			else {
				assujettiFin = false;
			}

		}

		 return new Range(dateDebutAssujetti,dateFinAssujetti);
	}

	/**
	 * Permet de convertir un for revenu fortune unireg en for host interfaces
	 *
	 * @param forUnireg
	 * @param idForGestion
	 * @param serviceInfrastructure
	 * @return le for Host
	 * @throws RegistreException
	 */
	public static ForImpl convertFor(final ForFiscalRevenuFortune forUnireg, Contribuable contribuableUnireg,
			ServiceInfrastructure serviceInfrastructure,TiersService tiersService) throws RegistreException {
		
		ForImpl unFor = new ForImpl();		
		if (forUnireg.getDateDebut() == null) {
			unFor.setDateDebutValidite(null);
			
		}
		else{
			unFor.setDateDebutValidite(forUnireg.getDateDebut().asJavaDate());
		}
		

		if (forUnireg.getDateFin() == null) {
			unFor.setDateFinValidite(null);
		}
		else {
			unFor.setDateFinValidite(forUnireg.getDateFin().asJavaDate());
		}
		
		ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuableUnireg, forUnireg.getDateDebut());
		
		if (forGestion!=null && forUnireg.getId() == forGestion.getSousjacent().getId()) {
			unFor.setForGestion(true);
		}
		else {
			unFor.setForGestion(false);
		}

		unFor.setMotifDebut(ContribuableUniregHelper.convertMotifOuverture(forUnireg, contribuableUnireg, serviceInfrastructure));
		unFor.setMotifFin(ContribuableUniregHelper.convertMotifFermeture(forUnireg, contribuableUnireg, serviceInfrastructure));
		unFor.setNoSequence(0);
		unFor.setSejourPourActivite(false);

		if (forUnireg instanceof ForFiscalPrincipal) {
			unFor.setTypeFor(EnumTypeFor.PRINCIPAL);
		}
		else if (forUnireg instanceof ForFiscalSecondaire) {
			unFor.setTypeFor(EnumTypeFor.SECONDAIRE);
		}

		// Initialisation de la commune et du pays en fonction du type d'autorité fiscale
		try {
			if (forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					|| forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {

				unFor.setCommune(serviceInfrastructure.getCommune(forUnireg.getNumeroOfsAutoriteFiscale()));
				unFor.setPays(null);

			}
			else if (forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {

				unFor.setCommune(null);
				unFor.setPays(serviceInfrastructure.getPays(forUnireg.getNumeroOfsAutoriteFiscale()));
			}

		}
		catch (InfrastructureException e) {
			throw new RegistreException("Problème durant la recherche de l'autorité fiscale ", e);
		}
		catch (RemoteException e) {

			throw new RegistreException("Problème d'accés distant au service infrastructure distant  ", e);
		}
		return unFor;
	}

	
	/**
	 * Permet de convertir un for revenu fortune unireg en for host interfaces
	 *
	 * @param forUnireg
	 * @param idForGestion
	 * @param serviceInfrastructure
	 * @return le for Host
	 * @throws RegistreException
	 */
	public static ForImpl convertForAutreImpot(final ForFiscalAutreImpot forUnireg, Contribuable contribuableUnireg,
			ServiceInfrastructure serviceInfrastructure,TiersService tiersService) throws RegistreException {
		
		ForImpl unFor = new ForImpl();		
		if (forUnireg.getDateDebut() == null) {
			unFor.setDateDebutValidite(null);
			
		}
		else{
			unFor.setDateDebutValidite(forUnireg.getDateDebut().asJavaDate());
		}
		

		if (forUnireg.getDateFin() == null) {
			unFor.setDateFinValidite(null);
		}
		else {
			unFor.setDateFinValidite(forUnireg.getDateFin().asJavaDate());
		}
		
		ForGestion forGestion = tiersService.getDernierForGestionConnu(contribuableUnireg, forUnireg.getDateDebut());
		
		if (forGestion!=null && forUnireg.getId() == forGestion.getSousjacent().getId()) {
			unFor.setForGestion(true);
		}
		else {
			unFor.setForGestion(false);
		}

		unFor.setMotifDebut(null);
		unFor.setMotifFin(null);
		unFor.setNoSequence(0);
		unFor.setSejourPourActivite(false);

		
		unFor.setTypeFor(EnumTypeFor.SPECIAL);
		

		// Initialisation de la commune et du pays en fonction du type d'autorité fiscale
		try {
			if (forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD
					|| forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_HC) {

				unFor.setCommune(serviceInfrastructure.getCommune(forUnireg.getNumeroOfsAutoriteFiscale()));
				unFor.setPays(null);

			}
			else if (forUnireg.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS) {

				unFor.setCommune(null);
				unFor.setPays(serviceInfrastructure.getPays(forUnireg.getNumeroOfsAutoriteFiscale()));
			}

		}
		catch (InfrastructureException e) {
			throw new RegistreException("Problème durant la recherche de l'autorité fiscale ", e);
		}
		catch (RemoteException e) {

			throw new RegistreException("Problème d'accés distant au service infrastructure distant  ", e);
		}
		return unFor;
	}
	
	/**
	 * Retourne le contribuable menage commun si celui ci existe
	 *
	 * @param contribuable
	 * @param periodeRecherche
	 * @return
	 * @throws RegistreException
	 */

	public static Contribuable resolveContribuable(Contribuable contribuable, Range periodeRecherche,TiersService tiersService) throws RegistreException {
		// recherche de la situation de famille du contribuable individu
		if (contribuable instanceof PersonnePhysique) {
			//MenageCommun menageCommun = getMenageCommunActifAt((PersonnePhysique) contribuable, periodeRecherche);
			MenageCommun menageCommun = tiersService.findMenageCommun((PersonnePhysique) contribuable, periodeRecherche.getDateDebut());
			// Si un menage a été trouvé, il est substitué au contribuable seul
			if (menageCommun != null) {
				contribuable = menageCommun;
			}
		}
		return contribuable;
	}

	/**
	 * renvoie true si le contribuable est valide sur la periode
	 *
	 * @param contribuable
	 * @param periodeRecherche
	 * @return
	 */

	public static boolean isContribuableValideForPeriode(final Contribuable contribuable, final Range periodeRecherche) {
		RegDate debutActivite = contribuable.getDateFinActivite();

		// Si le contribuable est en dehors de la plage de recherhce on renvoie une exception
		if (periodeRecherche.getDateDebut().isAfter((debutActivite == null) ? RegDate.getLateDate() : debutActivite)
				|| periodeRecherche.getDateFin().isBefore(contribuable.getDateDebutActivite())) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Recherche la declaration active sur la periode entree ou la derniere declaration connue pour un contribuable
	 *
	 * @param contribuable
	 * @param periodeRecherche
	 * @return la declaration active sur la periode entree ou la derniere declaration connue
	 */
	public static Declaration getDerniereDeclaration(final Contribuable contribuable, final Range periodeRecherche) {

		Declaration derniereDeclarationUnireg = null;

		int anneeDebut = contribuable.getDateDebutActivite().year();
		int anneeCourante = periodeRecherche.getDateFin().year();

		while (anneeCourante >= anneeDebut) {
			List<Declaration> declarations = contribuable.getDeclarationForPeriode(anneeCourante);
			if (declarations != null && !declarations.isEmpty()) {
				derniereDeclarationUnireg = declarations.get(declarations.size() - 1);
				if (derniereDeclarationUnireg != null) {
					break;
				}
			}
			anneeCourante--;
		}

		return derniereDeclarationUnireg;
	}

	/**
	 * Recherche la declaration pour une année et un numéro de déclaration dans l'année
	 *
	 * @param contribuable
	 * @param annee
	 * @param numeroAnnee
	 * @return la declaration
	 */
	public static DeclarationImpotOrdinaire findDeclaration(final Contribuable contribuable, final int annee, int numeroAnnee) {

		DeclarationImpotOrdinaire declarationUnireg = null;

		List<Declaration> declarations = contribuable.getDeclarationForPeriode(annee);
		if (declarations != null && !declarations.isEmpty()) {
			for (Declaration d : declarations) {
				DeclarationImpotOrdinaire declarationImpot = (DeclarationImpotOrdinaire) d;
				if (numeroAnnee != 0) {
					if (declarationImpot.getNumero() == numeroAnnee) {
						declarationUnireg = declarationImpot;
						break;
					}
				}
				// Dans le cas ou le numero dans l'année n'est pas spécifié
				// on prend la premieère DI trouvée sur la période
				else {
					declarationUnireg = declarationImpot;
				}
			}
		}

		return declarationUnireg;
	}
}
