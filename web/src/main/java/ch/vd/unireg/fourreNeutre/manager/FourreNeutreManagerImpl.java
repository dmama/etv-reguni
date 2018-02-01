package ch.vd.unireg.fourreNeutre.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.editique.EditiqueResultat;
import ch.vd.unireg.fourreNeutre.FourreNeutreException;
import ch.vd.unireg.fourreNeutre.FourreNeutreService;

public class FourreNeutreManagerImpl implements FourreNeutreManager {

	private FourreNeutreService fourreNeutreService;


	public void setFourreNeutreService(FourreNeutreService fourreNeutreService) {
		this.fourreNeutreService = fourreNeutreService;
	}


	@Override
	public List<Integer> getPeriodesAutoriseesPourImpression(long tiersId) {


		final Integer premierePeriode = getPremierePeriode(tiersId);
		final List<Integer> periodes = new ArrayList<>();
		final Integer periodeCourante = RegDate.get().year();
		for (int i = premierePeriode; i <=periodeCourante ; i++) {
			periodes.add(i);
		}
		Collections.reverse(periodes);
		return periodes;
	}

	private Integer getPremierePeriode(long tiersId) {
		return fourreNeutreService.getPremierePeriodeSelonType(tiersId);
	}

	@Override
	@Transactional(rollbackFor = Throwable.class)
	public EditiqueResultat envoieImpressionLocaleFourreNeutre(Long ctbId, int pf) throws FourreNeutreException {
			return fourreNeutreService.imprimerFourreNeutre(ctbId,pf);
	}

	@Override
	public boolean isAutorisePourFourreNeutre(long tiersId) {
		return fourreNeutreService.isAutorisePourFourreNeutre(tiersId);
	}

}
