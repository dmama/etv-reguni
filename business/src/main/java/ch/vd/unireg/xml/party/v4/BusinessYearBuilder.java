package ch.vd.unireg.xml.party.v4;

import ch.vd.unireg.xml.party.corporation.v4.BusinessYear;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;
import ch.vd.unireg.xml.DataHelper;

public class BusinessYearBuilder {

	public static BusinessYear newBusinessYear(ExerciceCommercial exerciceCommercial) {
		final BusinessYear by = new BusinessYear();
		by.setDateFrom(DataHelper.coreToXMLv2(exerciceCommercial.getDateDebut()));
		by.setDateTo(DataHelper.coreToXMLv2(exerciceCommercial.getDateFin()));
		return by;
	}
}
