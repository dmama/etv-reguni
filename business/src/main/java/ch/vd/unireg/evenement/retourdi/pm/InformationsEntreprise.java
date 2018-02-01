package ch.vd.unireg.evenement.retourdi.pm;

import ch.vd.registre.base.date.RegDate;

/**
 * Informations sur l'entreprise telles qu'annoncées dans la déclaration
 */
public class InformationsEntreprise {

	private final RegDate dateFinExerciceCommercial;
	private final AdresseRaisonSociale adresseCourrier;
	private final Localisation siege;
	private final Localisation administrationEffective;
	private final String iban;
	private final String titulaireCompteBancaire;
	private final String noTelContact;

	public InformationsEntreprise(RegDate dateFinExerciceCommercial,
	                              AdresseRaisonSociale adresseCourrier,
	                              Localisation siege,
	                              Localisation administrationEffective,
	                              String iban,
	                              String titulaireCompteBancaire,
	                              String noTelContact) {
		this.dateFinExerciceCommercial = dateFinExerciceCommercial;
		this.adresseCourrier = adresseCourrier;
		this.siege = siege;
		this.administrationEffective = administrationEffective;
		this.iban = iban;
		this.titulaireCompteBancaire = titulaireCompteBancaire;
		this.noTelContact = noTelContact;
	}

	public RegDate getDateFinExerciceCommercial() {
		return dateFinExerciceCommercial;
	}

	public AdresseRaisonSociale getAdresseCourrier() {
		return adresseCourrier;
	}

	public Localisation getAdministrationEffective() {
		return administrationEffective;
	}

	public Localisation getSiege() {
		return siege;
	}

	public String getIban() {
		return iban;
	}

	public String getTitulaireCompteBancaire() {
		return titulaireCompteBancaire;
	}

	public String getNoTelContact() {
		return noTelContact;
	}
}
