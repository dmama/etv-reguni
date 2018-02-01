package ch.vd.unireg.tiers.view;

import java.util.List;

/**
 * Structure model pour l'ecran d'edition des Tiers
 *
 * @author xcifde
 *
 */
public class TiersEditView extends TiersView {

	private IdentificationPersonneView identificationPersonne;

	private List<AdresseView> adressesActives;

	private List<AdresseView> adressesFiscalesModifiables;

	private String libelleOfsPaysOrigine ;

	private String sdateNaissance;

	private String sdateDeces;

	private String sdateDebutValiditeAutorisation;

	private boolean  separed;

	private Long numeroCtbAssocie;

	//true si une situation de famille peut être ajoutée
	private boolean situationFamilleActive;

	public void clear() {
		adressesActives = null;
		identificationPersonne = null;
		setAdressesEnErreur(null);
		setAdressesEnErreurMessage(null);
		libelleOfsPaysOrigine = null;
		sdateNaissance = null;
		sdateDeces = null;
		sdateDebutValiditeAutorisation = null;
		separed = false;
		numeroCtbAssocie = null;
		situationFamilleActive = false;
		setAllowed(false);
	}


	public boolean isSepared() {
		return separed;
	}

	public void setSepared(boolean separed) {
		this.separed = separed;
	}

	public IdentificationPersonneView getIdentificationPersonne() {
		return identificationPersonne;
	}

	public void setIdentificationPersonne(IdentificationPersonneView identificationPersonne) {
		this.identificationPersonne = identificationPersonne;
	}


	public List<AdresseView> getAdressesActives() {
		return adressesActives;
	}

	public void setAdressesActives(List<AdresseView> adressesActives) {
		this.adressesActives = adressesActives;
	}

	public String getSdateNaissance() {
		return sdateNaissance;
	}

	public void setSdateNaissance(String sdateNaissance) {
		this.sdateNaissance = sdateNaissance;
	}

	public String getSdateDeces() {
		return sdateDeces;
	}

	public void setSdateDeces(String sdateDeces) {
		this.sdateDeces = sdateDeces;
	}

	public String getSdateDebutValiditeAutorisation() {
		return sdateDebutValiditeAutorisation;
	}

	public void setSdateDebutValiditeAutorisation(String sdateDebutValiditeAutorisation) {
		this.sdateDebutValiditeAutorisation = sdateDebutValiditeAutorisation;
	}

	public String getLibelleOfsPaysOrigine() {
		return libelleOfsPaysOrigine;
	}

	public void setLibelleOfsPaysOrigine(String libelleOfsPaysOrigine) {
		this.libelleOfsPaysOrigine = libelleOfsPaysOrigine;
	}

	public Long getNumeroCtbAssocie() {
		return numeroCtbAssocie;
	}

	public void setNumeroCtbAssocie(Long numeroCtbAssocie) {
		this.numeroCtbAssocie = numeroCtbAssocie;
	}

	public boolean isSituationFamilleActive() {
		return situationFamilleActive;
	}

	public void setSituationFamilleActive(boolean situationFamilleActive) {
		this.situationFamilleActive = situationFamilleActive;
	}

	public List<AdresseView> getAdressesFiscalesModifiables() {
		return adressesFiscalesModifiables;
	}

	public void setAdressesFiscalesModifiables(List<AdresseView> adressesFiscalesModifiables) {
		this.adressesFiscalesModifiables = adressesFiscalesModifiables;
	}
}
