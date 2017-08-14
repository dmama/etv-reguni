package ch.vd.uniregctb.fourreNeutre;

import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.Tiers;

public class FourreNeutreHelperImpl implements FourreNeutreHelper {

	private  boolean fourreNeutreAutoriseePourPP;
	private  boolean fourreNeutreAutoriseePourPM;
	private  boolean fourreNeutreAutoriseePourIS;
	private Integer premierePeriodeIS;
	private Integer premierePeriodePM;
	private Integer premierePeriodePP;


	public void setFourreNeutreAutoriseePourPP(boolean fourreNeutreAutoriseePourPP) {
		this.fourreNeutreAutoriseePourPP = fourreNeutreAutoriseePourPP;
	}

	public void setFourreNeutreAutoriseePourPM(boolean fourreNeutreAutoriseePourPM) {
		this.fourreNeutreAutoriseePourPM = fourreNeutreAutoriseePourPM;
	}

	public void setFourreNeutreAutoriseePourIS(boolean fourreNeutreAutoriseePourIS) {
		this.fourreNeutreAutoriseePourIS = fourreNeutreAutoriseePourIS;
	}


	public void setPremierePeriodeIS(Integer premierePeriodeIS) {
		this.premierePeriodeIS = premierePeriodeIS;
	}

	public void setPremierePeriodePM(Integer premierePeriodePM) {
		this.premierePeriodePM = premierePeriodePM;
	}

	public void setPremierePeriodePP(Integer premierePeriodePP) {
		this.premierePeriodePP = premierePeriodePP;
	}

	@Override
	public Integer getPremierePeriodePP() {
		return premierePeriodePP;
	}

	@Override
	public Integer getPremierePeriodePM() {
		return premierePeriodePM;
	}

	@Override
	public Integer getPremierePeriodeIS() {
		return premierePeriodeIS;
	}

	@Override
	public boolean isTiersAutorisePourFourreNeutre(Tiers tiers) {
		final boolean okPourPP = tiers instanceof ContribuableImpositionPersonnesPhysiques && fourreNeutreAutoriseePourPP;
		final boolean okPourPM = tiers instanceof Entreprise && fourreNeutreAutoriseePourPM;
		final boolean okPourIS = tiers instanceof DebiteurPrestationImposable && fourreNeutreAutoriseePourIS;
		return okPourPP || okPourPM || okPourIS;
	}

}
