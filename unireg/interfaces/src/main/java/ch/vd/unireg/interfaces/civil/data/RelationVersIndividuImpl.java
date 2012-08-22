package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RelationVersIndividuImpl implements RelationVersIndividu, Serializable {

	private static final long serialVersionUID = -6624424767436254182L;

	private final long numeroAutreIndividu;
	private final TypeRelationVersIndividu type;
	private final RegDate dateDebut;
	private RegDate dateFin;

	public RelationVersIndividuImpl(long numeroAutreIndividu, TypeRelationVersIndividu type, RegDate dateDebut, RegDate dateFin) {
		this.numeroAutreIndividu = numeroAutreIndividu;
		this.type = type;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public long getNumeroAutreIndividu() {
		return numeroAutreIndividu;
	}

	@Override
	public TypeRelationVersIndividu getTypeRelation() {
		return type;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(@Nullable RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RelationVersIndividuImpl that = (RelationVersIndividuImpl) o;

		if (numeroAutreIndividu != that.numeroAutreIndividu) return false;
		if (type != that.type) return false;
		if (!dateDebut.equals(that.dateDebut)) return false;
		if (dateFin != null ? !dateFin.equals(that.dateFin) : that.dateFin != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (numeroAutreIndividu ^ (numeroAutreIndividu >>> 32));
		result = 31 * result + type.hashCode();
		result = 31 * result + dateDebut.hashCode();
		result = 31 * result + (dateFin != null ? dateFin.hashCode() : 0);
		return result;
	}
}
