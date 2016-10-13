package ch.vd.unireg.interfaces.civil.data;

import java.io.Serializable;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class RelationVersIndividuImpl implements RelationVersIndividu, Serializable {

	private static final long serialVersionUID = -807338357035537506L;

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
	public boolean isCollatable(DateRange next) {
		return DateRangeHelper.isCollatable(this, next)
				&& numeroAutreIndividu == ((RelationVersIndividu) next).getNumeroAutreIndividu()
				&& type == ((RelationVersIndividu) next).getTypeRelation();
	}

	@Override
	public DateRange collate(DateRange next) {
		return new RelationVersIndividuImpl(numeroAutreIndividu, type, dateDebut, next.getDateFin());
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
