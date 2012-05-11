package ch.vd.uniregctb.tiers.view;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.infra.data.InstitutionFinanciere;
import ch.vd.uniregctb.iban.IbanValidator;
import ch.vd.uniregctb.interfaces.model.CompteBancaire;
import ch.vd.uniregctb.interfaces.model.Mandat;
import ch.vd.uniregctb.interfaces.model.PartPM;
import ch.vd.uniregctb.interfaces.model.PersonneMorale;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.interfaces.service.ServicePersonneMoraleService;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;

/**
 * Form backing-object de l'onglet "complément" associé à un tiers.
 */
public class ComplementView {

	private String personneContact;
	private String complementNom;
	private String numeroTelephonePrive;
	private String numeroTelephonePortable;
	private String numeroTelephoneProfessionnel;
	private String numeroTelecopie;
	private String adresseCourrierElectronique;

	private CompteBancaireView compteBancaire = new CompteBancaireView();
	private List<CompteBancaireView> autresComptesBancaires = new ArrayList<CompteBancaireView>();

	private Boolean blocageRemboursementAutomatique;

	private Long ancienNumeroSourcier;

	public ComplementView() {
	}

	public ComplementView(Tiers tiers, ServicePersonneMoraleService servicePM, ServiceInfrastructureService serviceInfra, IbanValidator ibanValidator) {

		if (tiers instanceof Entreprise) {

			final PersonneMorale pm = servicePM.getPersonneMorale(tiers.getNumero(), PartPM.MANDATS);
			if (pm != null) {
				// numéros de téléphone
				this.numeroTelephonePrive = pm.getTelephoneContact();
				this.numeroTelecopie = pm.getTelecopieContact();
				this.numeroTelephonePortable = null;
				this.numeroTelephoneProfessionnel = null;

				// autres comptes bancaires
				final List<CompteBancaire> comptes = pm.getComptesBancaires();
				if (comptes != null && !comptes.isEmpty()) {
					for (CompteBancaire compte : comptes) {
						if (compte.getFormat() == CompteBancaire.Format.IBAN) {
							final String ibanValidationMessage = verifierIban(compte.getNumero(), ibanValidator); // [UNIREG-2582]
							autresComptesBancaires
									.add(new CompteBancaireView(tiers.getNumero(), pm.getTitulaireCompte(), null, null, compte.getNomInstitution(), compte.getNumero(), ibanValidationMessage, null));
						}
						else {
							final String numero = compte.getNumero();
							final String numeroCCP;
							final String numeroCompteBancaire;
							if (numero.contains("-")) { // on essaie de départager entre les deux : best effort...
								numeroCCP = numero;
								numeroCompteBancaire = null;
							}
							else {
								numeroCCP = null;
								numeroCompteBancaire = numero;
							}
							autresComptesBancaires
									.add(new CompteBancaireView(tiers.getNumero(), pm.getTitulaireCompte(), numeroCCP, numeroCompteBancaire, compte.getNomInstitution(), null, null, null));
						}
					}
				}

				final List<Mandat> mandats = pm.getMandats();
				if (mandats != null) {
					for (Mandat mandat : mandats) {
						if (RegDateHelper.isBetween(null, mandat.getDateDebut(), mandat.getDateFin(), NullDateBehavior.LATEST)) {
							if (StringUtils.isNotBlank(mandat.getCompteBancaire()) || StringUtils.isNotBlank(mandat.getCCP()) || StringUtils.isNotBlank(mandat.getIBAN())) {
								final String nomInstitutionFinanciere = getNomInstitution(mandat.getNumeroInstitutionFinanciere(), serviceInfra);
								final String ibanValidationMessage = verifierIban(mandat.getIBAN(), ibanValidator); // [UNIREG-2582]
								autresComptesBancaires.add(new CompteBancaireView(mandat.getNumeroMandataire(), pm.getTitulaireCompte(), mandat.getCCP(), mandat.getCompteBancaire(),
										nomInstitutionFinanciere, mandat.getIBAN(), ibanValidationMessage, mandat.getBicSwift()));
							}
						}
					}
				}
			}
		}
		else {
			// nom
			this.personneContact = tiers.getPersonneContact();
			this.complementNom = tiers.getComplementNom();

			// téléphone
			this.numeroTelecopie = tiers.getNumeroTelecopie();
			this.numeroTelephonePortable = tiers.getNumeroTelephonePortable();
			this.numeroTelephonePrive = tiers.getNumeroTelephonePrive();
			this.numeroTelephoneProfessionnel = tiers.getNumeroTelephoneProfessionnel();
			this.adresseCourrierElectronique = tiers.getAdresseCourrierElectronique();

			// compte bancaire
			final String iban = tiers.getNumeroCompteBancaire();
			final String ibanValidationMessage = verifierIban(iban, ibanValidator); // [UNIREG-2582]
			compteBancaire = new CompteBancaireView(tiers.getNumero(), tiers.getTitulaireCompteBancaire(), null, null, null, iban, ibanValidationMessage, tiers.getAdresseBicSwift());

			if (tiers instanceof PersonnePhysique) {
				final PersonnePhysique pp = (PersonnePhysique) tiers;
				this.ancienNumeroSourcier = pp.getAncienNumeroSourcier();
			}
		}

		this.blocageRemboursementAutomatique = tiers.getBlocageRemboursementAutomatique();
	}

	private static String getNomInstitution(Long noInstit, ServiceInfrastructureService serviceInfra) {

		if (noInstit == null) {
			return null;
		}

		final InstitutionFinanciere instit;
		instit = serviceInfra.getInstitutionFinanciere(noInstit.intValue());

		return instit.getNomInstitutionFinanciere();
	}

	/**
	 * Permet renseigner la view sur le fait que l'iban du tiers associé est valide ou pas
	 *
	 * @param iban          l'iban à vérifier
	 * @param ibanValidator le validator d'iban
	 * @return <code>null</code> si l'IBAN est valide, explication textuelle de l'erreur sinon
	 */
	private static String verifierIban(String iban, IbanValidator ibanValidator) {
		if (StringUtils.isNotBlank(iban)) {
			return ibanValidator.getIbanValidationError(iban);
		}
		return null;
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

	/**
	 * @return le compte bancaire du tiers courant.
	 */
	public CompteBancaireView getCompteBancaire() {
		return compteBancaire;
	}

	public void setCompteBancaire(CompteBancaireView compteBancaire) {
		this.compteBancaire = compteBancaire;
	}

	/**
	 * @return les autres comptes bancaires associés (ex. mandats pm). En lecture seule.
	 */
	public List<CompteBancaireView> getAutresComptesBancaires() {
		return autresComptesBancaires;
	}

	public void setAutresComptesBancaires(List<CompteBancaireView> autresComptesBancaires) {
		this.autresComptesBancaires = autresComptesBancaires;
	}

	public Boolean getBlocageRemboursementAutomatique() {
		return blocageRemboursementAutomatique;
	}

	public void setBlocageRemboursementAutomatique(Boolean blocageRemboursementAutomatique) {
		this.blocageRemboursementAutomatique = blocageRemboursementAutomatique;
	}

	public Long getAncienNumeroSourcier() {
		return ancienNumeroSourcier;
	}

	public void setAncienNumeroSourcier(Long ancienNumeroSourcier) {
		this.ancienNumeroSourcier = ancienNumeroSourcier;
	}
}
