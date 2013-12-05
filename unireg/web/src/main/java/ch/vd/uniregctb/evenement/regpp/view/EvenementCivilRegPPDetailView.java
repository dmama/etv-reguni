package ch.vd.uniregctb.evenement.regpp.view;

import java.io.Serializable;

import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.evenement.common.view.EvenementCivilDetailView;
import ch.vd.uniregctb.individu.IndividuView;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Structure permettant l'affichage de la page de detail de l'evenement
 *
 */
public class EvenementCivilRegPPDetailView extends EvenementCivilDetailView implements Serializable {

	private static final long serialVersionUID = 2886171493023303888L;
	
	private TypeEvenementCivil evtType;
	private Integer evtNumeroOfsCommuneAnnonce;
	private IndividuView individuConjoint;
	private AdresseEnvoi adresseConjoint;


	@SuppressWarnings("unused")
	public TypeEvenementCivil getEvtType() {
		return evtType;
	}

	public void setEvtType(TypeEvenementCivil evtType) {
		this.evtType = evtType;
	}

	@SuppressWarnings("unused")
	public Integer getEvtNumeroOfsCommuneAnnonce() {
		return evtNumeroOfsCommuneAnnonce;
	}

	public void setEvtNumeroOfsCommuneAnnonce(Integer evtNumeroOfsCommuneAnnonce) {
		this.evtNumeroOfsCommuneAnnonce = evtNumeroOfsCommuneAnnonce;
	}

	@SuppressWarnings("unused")
	public IndividuView getIndividuPrincipal() {
		return getIndividu();
	}

	public void setIndividuPrincipal(IndividuView individuPrincipal) {
		setIndividu(individuPrincipal);
	}

	@SuppressWarnings("unused")
	public IndividuView getIndividuConjoint() {
		return individuConjoint;
	}

	public void setIndividuConjoint(IndividuView individuConjoint) {
		this.individuConjoint = individuConjoint;
	}

	@SuppressWarnings("unused")
	public AdresseEnvoi getAdressePrincipal() {
		return getAdresse();
	}

	public void setAdressePrincipal(AdresseEnvoi adressePrincipal) {
		setAdresse(adressePrincipal);
	}

	@SuppressWarnings("unused")
	public AdresseEnvoi getAdresseConjoint() {
		return adresseConjoint;
	}

	public void setAdresseConjoint(AdresseEnvoi adresseConjoint) {
		this.adresseConjoint = adresseConjoint;
	}

}
