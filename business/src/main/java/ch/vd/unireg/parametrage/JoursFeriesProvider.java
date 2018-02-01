package ch.vd.unireg.parametrage;

import java.util.List;

import ch.vd.registre.base.date.RegDate;

public interface JoursFeriesProvider {
	List<RegDate> getDatesJoursFeries(int annee);
}
