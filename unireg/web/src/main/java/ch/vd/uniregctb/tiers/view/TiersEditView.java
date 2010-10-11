package ch.vd.uniregctb.tiers.view;

import java.util.List;

/**
 * Structure model pour l'ecran d'edition des Tiers
 *
 * @author xcifde
 *
 */
public class TiersEditView extends TiersView {

	public static final String FISCAL_FOR_PRINC = "FOR_PRINC";
	public static final String FISCAL_FOR_SEC = "FOR_SEC";
	public static final String FISCAL_FOR_AUTRE = "FOR_AUTRE";
	public static final String FISCAL_SIT_FAMILLLE = "SIT_FAM";
	public static final String ADR_D = "ADR_D";
	public static final String ADR_C = "ADR_C";
	public static final String ADR_B = "ADR_B";
	public static final String ADR_P = "ADR_P";
	public static final String COMPLEMENT_COMMUNICATION = "CPLT_COM";
	public static final String COMPLEMENT_COOR_FIN = "CPLT_COOR_FIN";
	public static final String DOSSIER_TRAVAIL = "DOS_TRA";
	public static final String DOSSIER_NO_TRAVAIL = "DOS_NO_TRA";

	private IdentificationPersonneView identificationPersonne;

	private List<AdresseView> adressesActives;

	private List<AdresseView> adressesFiscalesModifiables;

	private String libelleOfsPaysOrigine ;

	private String libelleOfsCommuneOrigine ;

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
		libelleOfsCommuneOrigine = null;
		sdateNaissance = null;
		sdateDeces = null;
		sdateDebutValiditeAutorisation = null;
		separed = false;
		numeroCtbAssocie = null;
		situationFamilleActive = false;
		setAllowedOnglet(null);
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

	public String getLibelleOfsCommuneOrigine() {
		return libelleOfsCommuneOrigine;
	}

	public void setLibelleOfsCommuneOrigine(String libelleOfsCommuneOrigine) {
		this.libelleOfsCommuneOrigine = libelleOfsCommuneOrigine;
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
