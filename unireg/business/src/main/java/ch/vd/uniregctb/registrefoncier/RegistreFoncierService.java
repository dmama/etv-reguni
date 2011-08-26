package ch.vd.uniregctb.registrefoncier;

import java.util.List;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.StatusManager;

public interface RegistreFoncierService {

	/**
	 * Fait le rapprochement entre les contribuables et les données transmises par le registre foncier
	 *
	 * @param listeProprietaireFoncier
	 * 					La liste des propriétaires fonciers à rapprocher
	 * @param dateTraitement TODO
	 * @param nbThreads
	 * @return
	 */
	RapprocherCtbResults rapprocherCtbRegistreFoncier(List<ProprietaireFoncier> listeProprietaireFoncier, StatusManager s, RegDate dateTraitement, int nbThreads);
}
