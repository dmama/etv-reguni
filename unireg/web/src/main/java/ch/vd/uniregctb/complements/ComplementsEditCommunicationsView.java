package ch.vd.uniregctb.complements;

import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;

@SuppressWarnings("UnusedDeclaration")
public class ComplementsEditCommunicationsView {

	private long id;

	// seulement pour les débiteurs sans contribuables associés
	private boolean debiteurWithoutCtb;
	private String nom1;
	private String nom2;

	// points de communications
	private String personneContact;
	private String complementNom;
	private String numeroTelephonePrive;
	private String numeroTelephonePortable;
	private String numeroTelephoneProfessionnel;
	private String numeroTelecopie;
	private String adresseCourrierElectronique;

	public ComplementsEditCommunicationsView() {
	}

	public ComplementsEditCommunicationsView(Tiers tiers) {
		initReadOnlyData(tiers);
		if (tiers instanceof DebiteurPrestationImposable) {
			final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
			this.nom1 = dpi.getNom1();
			this.nom2 = dpi.getNom2();
		}

		this.personneContact = tiers.getPersonneContact();
		this.complementNom = tiers.getComplementNom();
		this.numeroTelephonePrive = tiers.getNumeroTelephonePrive();
		this.numeroTelephonePortable = tiers.getNumeroTelephonePortable();
		this.numeroTelephoneProfessionnel = tiers.getNumeroTelephoneProfessionnel();
		this.numeroTelecopie = tiers.getNumeroTelecopie();
		this.adresseCourrierElectronique = tiers.getAdresseCourrierElectronique();
	}

	public void initReadOnlyData(Tiers tiers) {
		this.id = tiers.getId();
		this.debiteurWithoutCtb = tiers instanceof DebiteurPrestationImposable && ((DebiteurPrestationImposable)tiers).getContribuableId() == null;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isDebiteurWithoutCtb() {
		return debiteurWithoutCtb;
	}

	public String getNom1() {
		return nom1;
	}

	public void setNom1(String nom1) {
		this.nom1 = nom1;
	}

	public String getNom2() {
		return nom2;
	}

	public void setNom2(String nom2) {
		this.nom2 = nom2;
	}

	public String getPersonneContact() {
		return personneContact;
	}

	public void setPersonneContact(String personneContact) {
		this.personneContact = personneContact;
	}

	public String getComplementNom() {
		return complementNom;
	}

	public void setComplementNom(String complementNom) {
		this.complementNom = complementNom;
	}

	public String getNumeroTelephonePrive() {
		return numeroTelephonePrive;
	}

	public void setNumeroTelephonePrive(String numeroTelephonePrive) {
		this.numeroTelephonePrive = numeroTelephonePrive;
	}

	public String getNumeroTelephonePortable() {
		return numeroTelephonePortable;
	}

	public void setNumeroTelephonePortable(String numeroTelephonePortable) {
		this.numeroTelephonePortable = numeroTelephonePortable;
	}

	public String getNumeroTelephoneProfessionnel() {
		return numeroTelephoneProfessionnel;
	}

	public void setNumeroTelephoneProfessionnel(String numeroTelephoneProfessionnel) {
		this.numeroTelephoneProfessionnel = numeroTelephoneProfessionnel;
	}

	public String getNumeroTelecopie() {
		return numeroTelecopie;
	}

	public void setNumeroTelecopie(String numeroTelecopie) {
		this.numeroTelecopie = numeroTelecopie;
	}

	public String getAdresseCourrierElectronique() {
		return adresseCourrierElectronique;
	}

	public void setAdresseCourrierElectronique(String adresseCourrierElectronique) {
		this.adresseCourrierElectronique = adresseCourrierElectronique;
	}
}
