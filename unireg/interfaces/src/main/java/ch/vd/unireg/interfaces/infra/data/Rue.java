package ch.vd.unireg.interfaces.infra.data;

import ch.vd.registre.base.date.DateRange;

public interface Rue extends DateRange {

	String getDesignationCourrier();

	Integer getNoRue();

	Integer getNoLocalite();
}
