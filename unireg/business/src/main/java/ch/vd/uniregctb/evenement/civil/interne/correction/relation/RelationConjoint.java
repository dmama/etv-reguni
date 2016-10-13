package ch.vd.uniregctb.evenement.civil.interne.correction.relation;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.RelationVersIndividu;
import ch.vd.unireg.interfaces.civil.data.TypeRelationVersIndividu;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Structure commune entre les relations "conjoint" qui viennent du civil et celles qui viennent du fiscal
 * (pour faciliter la comparaison)
 * <p/>
 * <b>Important :</b> Deux instances distinctes sont forcément différentes (au sens de {@link #equals(Object)}) dès que
 * le numéro d'individu du conjoint est inconnu dans au moins l'une des deux
 */
public final class RelationConjoint implements DateRange, Comparable<RelationConjoint> {

	public final RegDate dateDebut;
	public final RegDate dateFin;
	public final Long noIndividuConjoint;
	public final boolean conjointFiscalConnu;

	/**
	 * Constructeur interne utilisé en interne, donc, et par les tests (le code de production doit plutôt utiliser les méthodes statiques <em>from</em>)
	 * @param dateDebut date de début de la relation
	 * @param dateFin date de fin de la relation
	 * @param noIndividuConjoint numéro d'individu civil du conjoint
	 * @param conjointFiscalConnu <code>true</code> si un conjoint fiscal existe, <code>false</code> si marié seul fiscal
	 * @see #from(ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate, ch.vd.uniregctb.tiers.PersonnePhysique)
	 * @see #from(ch.vd.registre.base.date.RegDate, ch.vd.registre.base.date.RegDate, ch.vd.unireg.interfaces.civil.data.RelationVersIndividu)
	 */
	RelationConjoint(RegDate dateDebut, RegDate dateFin, Long noIndividuConjoint, boolean conjointFiscalConnu) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.noIndividuConjoint = noIndividuConjoint;
		this.conjointFiscalConnu = conjointFiscalConnu;
	}

	public static RelationConjoint from(RegDate dateDebut, RegDate dateFin, RelationVersIndividu src) {
		if (src.getTypeRelation() == TypeRelationVersIndividu.CONJOINT) {
			return new RelationConjoint(dateDebut, dateFin, src.getNumeroAutreIndividu(), false);
		}
		else {
			return null;
		}
	}

	public static RelationConjoint from(RegDate dateDebut, RegDate dateFin, PersonnePhysique conjoint) {
		return new RelationConjoint(dateDebut, dateFin, conjoint.getNumeroIndividu(), true);
	}

	/**
	 * Pour gérer les mariés seuls -> relation vers un individu inconnu
	 */
	public static RelationConjoint seul(RegDate dateDebut, RegDate dateFin) {
		return new RelationConjoint(dateDebut, dateFin, null, false);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RelationConjoint that = (RelationConjoint) o;

		if (dateDebut != null ? !dateDebut.equals(that.dateDebut) : that.dateDebut != null) return false;
		if (dateFin != null ? !dateFin.equals(that.dateFin) : that.dateFin != null) return false;

		// si les deux relations sont "marié-seul", ok ; sinon, il faut que les deux numéros d'individu soient les mêmes
		return (isMarieSeul() && that.isMarieSeul()) || (noIndividuConjoint != null && that.noIndividuConjoint != null && noIndividuConjoint.equals(that.noIndividuConjoint));
	}

	@Override
	public int hashCode() {
		int result = dateDebut != null ? dateDebut.hashCode() : 0;
		result = 31 * result + (dateFin != null ? dateFin.hashCode() : 0);
		result = 31 * result + (noIndividuConjoint != null ? noIndividuConjoint.hashCode() : 0);
		return result;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public int compareTo(@NotNull RelationConjoint o) {
		int comparison = DateRangeComparator.compareRanges(this, o);
		if (comparison == 0) {
			if (o.noIndividuConjoint != null || noIndividuConjoint != null) {
				if (noIndividuConjoint == null) {
					comparison = -1;
				}
				else if (o.noIndividuConjoint == null) {
					comparison = 1;
				}
				else {
					comparison = Long.compare(noIndividuConjoint, o.noIndividuConjoint);
				}
			}
		}
		return comparison;
	}

	public boolean isMarieSeul() {
		return !conjointFiscalConnu && noIndividuConjoint == null;
	}

	/**
	 * Externalisé dans une méthode à part pour être testé unitairement
	 * @param fiscales liste triée des relations vers les conjoints fiscaux
	 * @param civiles liste triée des relations vers les conjoints civils
	 * @return <code>true</code> si au moins une différence a été constatée, <code>false</code> si les listes sont identiques
	 */
	public static boolean hasDifference(List<RelationConjoint> fiscales, List<RelationConjoint> civiles) {
		boolean same = fiscales.size() == civiles.size();
		for (int i = 0 ; same && i < fiscales.size() ; ++ i) {
			final RelationConjoint rcFiscale = fiscales.get(i);
			final RelationConjoint rcCivile = civiles.get(i);
			same = rcFiscale.equals(rcCivile);
		}
		return !same;
	}
}
