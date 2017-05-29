package ch.vd.uniregctb.evenement.fiscal;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.tiers.Tiers;

@Entity
@DiscriminatorValue(value = "INFORMATION_COMPLEMENTAIRE")
public class EvenementFiscalInformationComplementaire extends EvenementFiscalTiers {

	public enum TypeInformationComplementaire {
		/**
		 * Type de l'événement fiscal émis lors du changement de forme juridique d'une entreprise conservant la même catégorie.
		 */
		CHANGEMENT_FORME_JURIDIQUE_MEME_CATEGORIE("Changement de forme juridique sans changer de catégorie"),

		/**
		 * Type de l'événement fiscal émis lors d'une modification des statuts d'une entreprise.
		 */
		MODIFICATION_STATUTS("Modification des statuts"),

		/**
		 * Type de l'événement fiscal émis lors d'une modification du but d'une entreprise.
		 */
		MODIFICATION_BUT("Modification des buts"),

		/**
		 * Type de l'événement fiscal émis lors d'une modification du capital d'une entreprise.
		 */
		MODIFICATION_CAPITAL("Modification du capital"),

		/**
		 * Type de l'événement fiscal émis lors d'une fusion d'entreprises.
		 */
		FUSION("Fusion"),

		/**
		 * Type de l'événement fiscal émis lors d'une scission d'entreprise.
		 */
		SCISSION("Scission"),

		/**
		 * Type de l'événement fiscal émis lors d'un transfert de patrimoine entre entreprises.
		 */
		TRANSFERT_PATRIMOINE("Transfert de patrimoine"),

		/**
		 * Type de l'événement fiscal émis lors de la liquidation d'une entreprise.
		 */
		LIQUIDATION("Liquidation"),

		/**
		 * Type de l'événement fiscal émis lors de l'avis préalable à l'ouverture d'une faillite.
		 */
		AVIS_PREALABLE_OUVERTURE_FAILLITE("Avis préalable d'ouverture de la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de l'état de collocation et inventaire dans la faillite.
		 */
		ETAT_COLLOCATION_INVENTAIRE_FAILLITE("Etat de collocation et inventaire dans la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de la vente aux enchères forcée d'immeubles dans la faillite.
		 */
		VENTE_ENCHERES_FORCEE_IMMEUBLES_FAILLITE("Vente aux enchères forcée d'immeubles dans la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de l'octroi d'un sursis concordataire provisoire.
		 */
		SURSIS_CONCORDATAIRE_PROVISOIRE("Sursis concordataire provisoire"),

		/**
		 * Type de l'événement fiscal émis lors de l'octroi d'un sursis concordataire.
		 */
		SURSIS_CONCORDATAIRE("Sursis concordataire"),

		/**
		 * Type de l'événement fiscal émis lors de l'appel aux créanciers dans un concordat.
		 */
		APPEL_CREANCIERS_CONCORDAT("Appel aux créanciers dans le concordat"),

		/**
		 * Type de l'événement fiscal émis lors de l'audience de liquidation par abandon d'actif.
		 */
		AUDIENCE_LIQUIDATION_ABANDON_ACTIF("Audience de liquidation par abandon d'actif"),

		/**
		 * Type de l'événement fiscal émis lors de la prolongation d'un sursis concordataire.
		 */
		PROLONGATION_SURSIS_CONCORDATAIRE("Prolongation du sursis concordataire"),

		/**
		 * Type de l'événement fiscal émis lors de l'annulation d'un sursis concordataire.
		 */
		ANNULATION_SURSIS_CONCORDATAIRE("Annulation du sursis concordataire"),

		/**
		 * Type de l'événement fiscal émis lors de l'homologation d'un concordat.
		 */
		HOMOLOGATION_CONCORDAT("Homologation du concordat"),

		/**
		 * Type de l'événement fiscal émis lors de l'état de collocation dans un concordat par abandon d'actif.
		 */
		ETAT_COLLOCATION_CONCORDAT_ABANDON_ACTIF("Etat de collocation dans le concordat par abandon d'actif"),

		/**
		 * Type de l'événement fiscal émis lors de l'établissement du tableau de distribution et du décompte final dans un concordat par abandon d'actif.
		 */
		TABLEAU_DISTRIBUTION_DECOMPTE_FINAL_CONCORDAT("Tableau de distribution et décompte final dans le concordat par abandon d'actif"),

		/**
		 * Type de l'événement fiscal émis lors de la constitution d'un concordat de banque et de caisse d'épargne.
		 */
		CONCORDAT_BANQUE_CAISSE_EPARGNE("Concordat de banque et de caisse d'épargne"),

		/**
		 * Type de l'événement fiscal émis lors de la vente aux enchères forcée d'immeubles dans la poursuite.
		 */
		VENTE_ENCHERES_FORCEE_IMMEUBLES_POURSUITE("Vente aux enchères forcée d'immeubles dans la poursuite"),

		/**
		 * Type de l'événement fiscal émis lors de la publication d'une faillite et de l'appel aux créanciers.
		 */
		PUBLICATION_FAILLITE_APPEL_CREANCIERS("Publication de la faillite et appel aux créanciers"),

		/**
		 * Type de l'événement fiscal émis lors de la suspension d'une faillite.
		 */
		SUSPENSION_FAILLITE("Suspension de la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de la clôture d'une faillite.
		 */
		CLOTURE_FAILLITE("Clôture de la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de la révocation d'une faillite.
		 */
		REVOCATION_FAILLITE("Révocation de la faillite"),

		/**
		 * Type de l'événement fiscal émis lors de l'appel aux créanciers suite à un transfert à l'étranger.
		 */
		APPEL_CREANCIERS_TRANSFERT_HS("Appel aux créanciers suite à transfert à l'étranger"),

		/**
		 * Type de l'événement fiscal émis lors de l'annulation d'une fusion d'entreprises (processus dit 'complexe')
		 */
		ANNULATION_FUSION("Annulation de fusion d'entreprises"),

		/**
		 * Type de l'événement fiscal émis lors de l'annulation d'une scission d'entreprise (processus dit 'complexe')
		 */
		ANNULATION_SCISSION("Annulation de scission d'entreprise"),

		/**
		 * Type de l'événement fiscal émis lors de l'annulation d'un transfert de patrimoine entre entreprises (processus dit 'complexe')
		 */
		ANNULATION_TRANFERT_PATRIMOINE("Annulation de transfert de patrimoine entre entreprises"),

		/**
		 * Type de l'événement fiscal émis lors de l'annulation d'une faillite d'entreprise (processus dit 'complexe')
		 */
		ANNULATION_FAILLITE("Annulation de faillite");

		private final String libelle;

		TypeInformationComplementaire(String libelle) {
			this.libelle = libelle;
		}

		@Override
		public String toString() {
			return libelle;
		}
	}

	private TypeInformationComplementaire type;

	public EvenementFiscalInformationComplementaire() {
	}

	public EvenementFiscalInformationComplementaire(Tiers tiers, RegDate dateValeur, TypeInformationComplementaire type) {
		super(tiers, dateValeur);
		this.type = type;
	}

	@Column(name = "TYPE_EVT_INFO_COMPL", length = LengthConstants.EVTFISCAL_TYPE_EVT_INFO_COMPLEMENTAIRE)
	@Enumerated(EnumType.STRING)
	public TypeInformationComplementaire getType() {
		return type;
	}

	public void setType(TypeInformationComplementaire type) {
		this.type = type;
	}
}
