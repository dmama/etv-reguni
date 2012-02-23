package ch.vd.uniregctb.evenement.regpp.view;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.individu.IndividuView;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 *
 * @author xcifde
 */
public class EvenementView {

	private EvenementCivilRegPP evenement;

	private IndividuView individuPrincipal;

	private IndividuView individuConjoint;

	private List<TiersAssocieView> tiersAssocies;

	private List<String> erreursTiersAssocies;

	private AdresseEnvoi adressePrincipal;

	private AdresseEnvoi adresseConjoint;

	public EvenementCivilRegPP getEvenement() {
		return evenement;
	}

	public void setEvenement(EvenementCivilRegPP evenement) {
		this.evenement = evenement;
	}

	public IndividuView getIndividuPrincipal() {
		return individuPrincipal;
	}

	public void setIndividuPrincipal(IndividuView individuPrincipal) {
		this.individuPrincipal = individuPrincipal;
	}

	public IndividuView getIndividuConjoint() {
		return individuConjoint;
	}

	public void setIndividuConjoint(IndividuView individuConjoint) {
		this.individuConjoint = individuConjoint;
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

	public AdresseEnvoi getAdressePrincipal() {
		return adressePrincipal;
	}

	public void setAdressePrincipal(AdresseEnvoi adressePrincipal) {
		this.adressePrincipal = adressePrincipal;
	}

	public AdresseEnvoi getAdresseConjoint() {
		return adresseConjoint;
	}

	public void setAdresseConjoint(AdresseEnvoi adresseConjoint) {
		this.adresseConjoint = adresseConjoint;
	}

}
