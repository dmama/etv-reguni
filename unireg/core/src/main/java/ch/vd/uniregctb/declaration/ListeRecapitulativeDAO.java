package ch.vd.uniregctb.declaration;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.dao.GenericDAO;
import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.common.ParamPagination;


/*
 * @hidden
 */
public interface ListeRecapitulativeDAO extends GenericDAO<DeclarationImpotSource, Long> {


	/**
	 * Recherche des listes recapitulatives selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	public List<DeclarationImpotSource> find(ListeRecapCriteria criterion, @Nullable ParamPagination paramPagination) ;

	/**
	 * Recherche toutes les LR en fonction du numero de debiteur
	 *
	 * @param numero
	 * @return
	 */
	public List<DeclarationImpotSource> findByNumero(Long numero);

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param lrId
	 * @return
	 */
	public EtatDeclaration findDerniereLrEnvoyee(Long numeroDpi) ;

	/**
	 * Retourne le nombre de LR associées aux critères donnés
	 * @param criterion
	 * @return
	 */
	public int count(ListeRecapCriteria criterion);

	/**
	 * Retourne une liste de date ranges représentant des LR qui intersectent
	 * avec la période donnée pour le débiteur donné
	 *
	 * @param numeroDpi
	 * @param range
	 * @return
	 */
	public List<DateRange> findIntersection(long numeroDpi, DateRange range);

}