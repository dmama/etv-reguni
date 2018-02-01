package ch.vd.unireg.declaration;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.unireg.common.pagination.ParamPagination;


/*
 * @hidden
 */
public interface ListeRecapitulativeDAO extends DeclarationDAO<DeclarationImpotSource> {


	/**
	 * Recherche des listes recapitulatives selon des criteres
	 *
	 * @param criterion
	 * @return
	 */
	List<DeclarationImpotSource> find(ListeRecapitulativeCriteria criterion, @Nullable ParamPagination paramPagination) ;

	/**
	 * Recherche toutes les LR en fonction du numero de debiteur
	 *
	 * @param numero
	 * @return
	 */
	List<DeclarationImpotSource> findByNumero(Long numero);

	/**
	 * Retourne le dernier EtatPeriodeDeclaration retournee
	 *
	 * @param numeroDpi
	 * @return
	 */
	EtatDeclaration findDerniereLrEnvoyee(Long numeroDpi) ;

	/**
	 * Retourne le nombre de LR associées aux critères donnés
	 * @param criterion
	 * @return
	 */
	int count(ListeRecapitulativeCriteria criterion);

	/**
	 * Retourne une liste de date ranges représentant des LR qui intersectent
	 * avec la période donnée pour le débiteur donné
	 *
	 * @param numeroDpi
	 * @param range
	 * @return
	 */
	List<DateRange> findIntersection(long numeroDpi, DateRange range);

}