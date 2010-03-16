package ch.vd.uniregctb.interfaces.model.mock;

import java.util.ArrayList;
import java.util.Collection;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.pm.model.EnumFormeJuridique;
import ch.vd.registre.pm.model.EnumTypeAdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.AdresseEntreprise;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;

public class MockPersonneMorale implements PersonneMorale {

	private static final long serialVersionUID = -8639829263196146440L;

	private Collection<AdresseEntreprise> adresses = new ArrayList<AdresseEntreprise>();
	private RegDate debut;
	private RegDate fin;
	private EnumFormeJuridique formeJuridique;
	private String nomContact;
	private String numeroCompteBancaire;
	private long numeroEntreprise;
	private String raisonSociale;
	private String telecopieContact;
	private String telephoneContact;
	private String titulaireCompte;

	public static MockPersonneMorale NestleSuisse = new MockPersonneMorale(27769, "Nestlé Suisse S.A.", EnumFormeJuridique.SOCIETE_ANONYME, "Myriam Steiner", RegDate.get(1996, 12, 18), null);

	static {
		MockAdresseEntreprise siege = new MockAdresseEntreprise();
		siege.setComplement(null);
		siege.setLocalite("1800 Vevey");
		siege.setRue("Entre-Deux-Villes");
		siege.setNumeroMaison(null);
		siege.setType(EnumTypeAdresseEntreprise.SIEGE);
		siege.setDateDebutValidite(RegDate.get(1996, 12, 5));
		siege.setDateFinValidite(null);
		NestleSuisse.addAdresse(siege);

		MockAdresseEntreprise courrier = new MockAdresseEntreprise();
		courrier.setComplement("Finance et Audit");
		courrier.setLocalite("1800 Vevey");
		courrier.setRue("pa Myriam Steiner / CP 352");
		courrier.setNumeroMaison(null);
		courrier.setType(EnumTypeAdresseEntreprise.COURRIER);
		courrier.setDateDebutValidite(RegDate.get(2003, 6, 13));
		courrier.setDateFinValidite(null);
		NestleSuisse.addAdresse(courrier);

		MockAdresseEntreprise facturation = new MockAdresseEntreprise();
		facturation.setComplement("Finance et Audit");
		facturation.setLocalite("1800 Vevey");
		facturation.setRue("pa Myriam Steiner / CP 352");
		facturation.setNumeroMaison(null);
		facturation.setType(EnumTypeAdresseEntreprise.FACTURATION);
		facturation.setDateDebutValidite(RegDate.get(2003, 6, 13));
		facturation.setDateFinValidite(null);
		NestleSuisse.addAdresse(facturation);
	}

	public MockPersonneMorale() {
	}

	public MockPersonneMorale(long numeroEntreprise, String raisonSociale, EnumFormeJuridique forme, String nomContact, RegDate debut, RegDate fin) {
		this.numeroEntreprise = numeroEntreprise;
		this.raisonSociale = raisonSociale;
		this.formeJuridique = forme;
		this.nomContact = nomContact;
		this.debut = debut;
		this.fin = fin;
	}

	public void addAdresse(AdresseEntreprise adresse) {
		adresses.add(adresse);
	}

	@SuppressWarnings("unchecked")
	public Collection getAdresses() {
		return adresses;
	}

	public RegDate getDateConstitution() {
		return debut;
	}

	public RegDate getDateFinActivite() {
		return fin;
	}

	public EnumFormeJuridique getFormeJuridique() {
		return formeJuridique;
	}

	public String getNomContact() {
		return nomContact;
	}

	public String getNumeroCompteBancaire() {
		return numeroCompteBancaire;
	}

	public long getNumeroEntreprise() {
		return numeroEntreprise;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public String getTelecopieContact() {
		return telecopieContact;
	}

	public String getTelephoneContact() {
		return telephoneContact;
	}

	public String getTitulaireCompte() {
		return titulaireCompte;
	}

	public RegDate getDebut() {
		return debut;
	}

	public RegDate getFin() {
		return fin;
	}

	public EnumFormeJuridique getForme() {
		return formeJuridique;
	}

	public void setNumeroCompteBancaire(String numeroCompteBancaire) {
		this.numeroCompteBancaire = numeroCompteBancaire;
	}

	public void setTelecopieContact(String telecopieContact) {
		this.telecopieContact = telecopieContact;
	}

	public void setTelephoneContact(String telephoneContact) {
		this.telephoneContact = telephoneContact;
	}

	public void setTitulaireCompte(String titulaireCompte) {
		this.titulaireCompte = titulaireCompte;
	}

	public static void setNestleSuisse(MockPersonneMorale nestleSuisse) {
		NestleSuisse = nestleSuisse;
	}

	public void setDebut(RegDate debut) {
		this.debut = debut;
	}

	public void setFin(RegDate fin) {
		this.fin = fin;
	}

	public void setDateConstitution(RegDate debut) {
		this.debut = debut;
	}

	public void setDateFinActivite(RegDate fin) {
		this.fin = fin;
	}

	public void setFormeJurique(EnumFormeJuridique forme) {
		this.formeJuridique = forme;
	}

	public void setNomContact(String nomContact) {
		this.nomContact = nomContact;
	}

	public void setNumeroEntreprise(long numeroEntreprise) {
		this.numeroEntreprise = numeroEntreprise;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public void setAdresses(Collection<AdresseEntreprise> adresses) {
		this.adresses = adresses;
	}
}
