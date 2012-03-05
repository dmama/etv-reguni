package ch.vd.uniregctb.evenement.ech.view;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.common.view.TiersAssocieView;
import ch.vd.uniregctb.individu.IndividuView;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 *
 */
public class EvenementCivilEchDetailView {

	private EvenementCivilEch evenement;

	private IndividuView individu;

	private List<TiersAssocieView> tiersAssocies;

	private List<String> erreursTiersAssocies;

	private AdresseEnvoi adresse;

	public EvenementCivilEch getEvenement() {
		return evenement;
	}

	public void setEvenement(EvenementCivilEch evenement) {
		this.evenement = evenement;
	}

	public IndividuView getIndividu() {
		return individu;
	}

	public void setIndividu(IndividuView individu) {
		this.individu = individu;
	}

	public List<TiersAssocieView> getTiersAssocies() {
		return tiersAssocies;
	}

	public void setTiersAssocies(List<TiersAssocieView> tiersAssocies) {
		this.tiersAssocies = tiersAssocies;
	}

	public List<String> getErreursTiersAssocies() {
		return erreursTiersAssocies;
	}

	public void setErreursTiersAssocies(List<String> erreursTiersAssocies) {
		this.erreursTiersAssocies = erreursTiersAssocies;
	}

	public AdresseEnvoi getAdresse() {
		return adresse;
	}

	public void setAdresse(AdresseEnvoi adresse) {
		this.adresse = adresse;
	}


}
