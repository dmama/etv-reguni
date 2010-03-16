package ch.vd.uniregctb.evenement;

import java.util.Collection;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.TypeEvenementFiscal;

/**
 * DAO des événements fiscaux.
 *
 * @author xcicfh (last modified by $Author: $ @ $Date: $)
 * @version $Revision: $
 */
public interface EvenementFiscalDAO extends GenericDAO<EvenementFiscal, Long> {


	/**
	 * Créer une nouvelle instance d'un événement situation de famille transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la situation de famille
	 * @return Retourne une nouvelle instance d'un événement situation de famille transient.
	 */
	public EvenementFiscalSituationFamille creerEvenementSituationFamille(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, Long id) ;

	/**
	 * Créer une nouvelle instance d'un événement for transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param motifFor
	 * @param id du for
	 * @return Retourne une nouvelle instance d'un événement for transient.
	 */
	public EvenementFiscalFor creerEvenementFor(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, MotifFor motifFor, ModeImposition modeImposition, Long id) ;


	/**
	 * Créer une nouvelle instance d'un événement LR transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la LR
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @return Retourne une nouvelle instance d'un événement LR transient.
	 */
	public EvenementFiscalLR creerEvenementLR(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, Long id,
			RegDate dateDebutPeriode, RegDate dateFinPeriode) ;

	/**
	 * Créer une nouvelle instance d'un événement DI transient.
	 * @param tiers
	 * @param typeEvenement
	 * @param dateEvenement
	 * @param id de la DI
	 * @param dateDebutPeriode
	 * @param dateFinPeriode
	 * @return Retourne une nouvelle instance d'un événement DI transient.
	 */
	public EvenementFiscalDI creerEvenementDI(Tiers tiers, TypeEvenementFiscal typeEvenement,
			RegDate dateEvenement, Long id,
			RegDate dateDebutPeriode, RegDate dateFinPeriode) ;


	/**
	 * Retourne la liste des événements fiscaux pour un tiers.
	 * @param tiers Tiers.
	 * @return  Retourne la liste des événements fiscaux pour un tiers.
	 */
	Collection<EvenementFiscal> getEvenementFiscals( Tiers tiers) ;
}

