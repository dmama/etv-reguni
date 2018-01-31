package ch.vd.uniregctb.xml.party.v5;

import ch.vd.unireg.xml.party.corporation.v5.BusinessYear;
import ch.vd.uniregctb.metier.bouclement.ExerciceCommercial;
import ch.vd.uniregctb.xml.DataHelper;

public class BusinessYearBuilder {

	public static BusinessYear newBusinessYear(ExerciceCommercial exerciceCommercial) {
		final BusinessYear by = new BusinessYear();
		by.setDateFrom(DataHelper.coreToXMLv2(exerciceCommercial.getDateDebut()));
		by.setDateTo(DataHelper.coreToXMLv2(exerciceCommercial.getDateFin()));
		return by;
	}
}
