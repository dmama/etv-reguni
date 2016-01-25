package ch.vd.uniregctb.migration.pm.regpm;

import ch.vd.registre.base.date.RegDate;

public interface AdresseAvecRue {

	RegDate getDateDebut();
	RegDate getDateFin();
	String getNomRue();
	String getNoPolice();
	String getLieu();
	RegpmLocalitePostale getLocalitePostale();
	RegpmRue getRue();
	Integer getOfsPays();
}
