package ch.vd.uniregctb.webservices.tiers;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.infrastructure.service.InfrastructureException;
import ch.vd.uniregctb.interfaces.model.InstitutionFinanciere;
import ch.vd.uniregctb.webservices.tiers.impl.Context;

/**
 * Contient les coordonnées financière d'un compte auprès d'un organisme financier (CCP ou compte bancaire, suisse ou étranger).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompteBancaire", propOrder = {
		"titulaire", "numero", "format", "clearing", "adresseBicSwift", "nomInstitution"
})
public class CompteBancaire {

	/**
	 * Formats permettant d'interpréter le numéro d'un compte.
	 */
	@XmlType(name = "FormatNumeroCompte")
	@XmlEnum(String.class)
	public static enum Format {
		/**
		 * Le format spécifique à l'organisme financier suisse (banque ou poste). L'organisme est spécifié par le numéro de clearing.
		 * Exemples :
		 * <ul>
		 * <li>BCV: 477.512.01 Z</li>
		 * <li>La Poste: 10-3848-4</li>
		 * <li>...</li>
		 * </ul>
		 */
		SPECIFIQUE_CH,
		/**
		 * L'International Bank Account Number. Exemples :
		 * <ul>
		 * <li>Greek IBAN: GR16 0110 1050 0000 1054 7023 795</li>
		 * <li>British IBAN: GB35 MIDL 4025 3432 1446 70</li>
		 * <li>Swiss IBAN: CH51 0868 6001 2565 1500 1</li>
		 * <li>...</li>
		 * </ul>
		 */
		IBAN;

		public static Format fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * Le titulaire du compte.
	 */
	@XmlElement(required = false)
	public String titulaire;

	/**
	 * Le numéro du compte
	 */
	@XmlElement(required = true)
	public String numero;

	/**
	 * Le format du numéro
	 */
	@XmlElement(required = true)
	public Format format;

	/**
	 * Le clearing bancaire (uniquement renseigné sur les comptes bancaires suisses)
	 */
	@XmlElement(required = false)
	public String clearing;

	/**
	 * Le numéro BIC SWIFT (uniquement renseigné sur les comptes bancaires étrangers).
	 */
	@XmlElement(required = false)
	public String adresseBicSwift;

	/**
	 * Le nom de l'institution financière (lorsque cette information est connnue).
	 */
	@XmlElement(required = false)
	public String nomInstitution;

	public CompteBancaire() {
	}

	public CompteBancaire(String titulaire, String numero, Format format, String clearing, String adresseBicSwift, String nomInstitution) {
		this.titulaire = titulaire;
		this.numero = numero;
		this.format = format;
		this.clearing = clearing;
		this.adresseBicSwift = adresseBicSwift;
		this.nomInstitution = nomInstitution;
	}

	/**
	 * Ce constructor permet de créer un compte à partir des informations stockées sur un tiers d'Unireg (qui ne peut donc possèder qu'un
	 * seul compte).
	 */
	public CompteBancaire(ch.vd.uniregctb.tiers.Tiers tiers, Context context) {
		this.titulaire = tiers.getTitulaireCompteBancaire();
		this.numero = tiers.getNumeroCompteBancaire();
		this.format = Format.IBAN; // par définition, on ne stocke que le format IBAN dans Unireg
		this.clearing = context.ibanValidator.getClearing(this.numero);
		this.adresseBicSwift = tiers.getAdresseBicSwift();

		try {
			final List<InstitutionFinanciere> list = context.infraService.getInstitutionsFinancieres(this.clearing);
			if (list != null && !list.isEmpty()) {
				// on peut trouver plusieurs institutions, mais laquelle choisir ?
				// la première ne semble pas un choix plus bête qu'un autre...
				final InstitutionFinanciere institution = list.get(0);
				this.nomInstitution = institution.getNomInstitutionFinanciere();
			}
		}
		catch (InfrastructureException ignored) {
			// que faire de cette exception ?
		}
	}
}
