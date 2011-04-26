package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Cette classe de caractériser un rapport (par exemple: tutelle, appartenance ménage, ...) entre deux tiers.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RapportEntreTiers", propOrder = {
		"type", "dateDebut", "dateFin", "dateAnnulation", "autreTiersNumero", "typeActivite", "tauxActivite", "finDernierElementImposable", "extensionExecutionForcee"
})
public class RapportEntreTiers {

	@XmlType(name = "TypeRapportEntreTiers")
	@XmlEnum(String.class)
	public static enum Type {

		TUTELLE,
		CURATELLE,
		CONSEIL_LEGAL,
		PRESTATION_IMPOSABLE,
		APPARTENANCE_MENAGE,
		REPRESENTATION,
		/**
		 * Rapport qui existe entre un débiteur et son contribuable lié.
		 */
		CONTACT_IMPOT_SOURCE,
		/**
		 * Rapport qui existe lorsqu'un tiers à été annulé et remplacé par un autre tiers (correction de doublons)
		 */
		ANNULE_ET_REMPLACE;

		public static Type fromValue(String v) {
			return valueOf(v);
		}
	}

	/** Le type de rapport entre tiers */
	@XmlElement(required = true)
	public Type type;

	/** La date de début de validité du rapport. */
	@XmlElement(required = true)
	public Date dateDebut;

	/** La date de fin de validité du rapport. Si le rapport est toujours actif, cette date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Date à laquelle le rapport a été annulé, ou <b>null</b> s'il n'est pas annulé. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/**
	 * Numéro de tiers pointé par cette relation.
	 * <p>
	 * En fonction du type de rapport-entre-tiers, les tiers participants sont de types suivants:
	 * <ul>
	 * <li>Tutelle : Personne physique <=> Personne physique</li>
	 * <li>Curatelle : Personne physique <=> Personne physique</li>
	 * <li>Conseil légal : Personne physique <=> Personne physique</li>
	 * <li>Prestations imposables : Personne physique <=> Débiteur prestation imposable</li>
	 * <li>Appartenance ménage : Personne physique <=> Ménage</li>
	 * <li>Représentation : Personne physique <=> Personne physique</li>
	 * <li>Contact impôt source : Débiteur prestation imposable <=> Contribuable</li>
	 * <li>Annule et remplace : Tiers <=> Tiers</li>
	 * </ul>
	 */
	@XmlElement(required = true)
	public Long autreTiersNumero;

	/** Type d'activité d'un tiers par rapport au débiteur imposable associé. Seulement valable pour le type = PRESTATION_IMPOSABLE. */
	@XmlElement(required = false)
	public TypeActivite typeActivite;

	/** Taux d'activité d'un tiers par rapport au débiteur imposable associé. Seulement valable pour le type = PRESTATION_IMPOSABLE. */
	@XmlElement(required = false)
	public Integer tauxActivite;

	/** Seulement valable pour le type = PRESTATION_IMPOSABLE. */
	@XmlElement(required = false)
	public Date finDernierElementImposable;

	/** <b>vrai</b> si la représentation conventionnelle s'étend à l'exécution forcée; <b>faux</b> autrement. Seulement renseigné pour le type = REPRESENTATION. */
	@XmlElement(required = false)
	public Boolean extensionExecutionForcee;

	public RapportEntreTiers() {
	}

	public RapportEntreTiers(ch.vd.uniregctb.tiers.RapportEntreTiers rapport, Long autreTiersNumero) {
		this.type = EnumHelper.coreToWeb(rapport.getType());
		this.dateDebut = DataHelper.coreToWeb(rapport.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(rapport.getDateFin());
		this.dateAnnulation = DataHelper.coreToWeb(rapport.getAnnulationDate());
		this.autreTiersNumero = autreTiersNumero;

		if (rapport instanceof ch.vd.uniregctb.tiers.RapportPrestationImposable) {
			final ch.vd.uniregctb.tiers.RapportPrestationImposable rpi = (ch.vd.uniregctb.tiers.RapportPrestationImposable) rapport;

			this.typeActivite = EnumHelper.coreToWeb(rpi.getTypeActivite());
			this.tauxActivite = rpi.getTauxActivite();
			this.finDernierElementImposable = DataHelper.coreToWeb(rpi.getFinDernierElementImposable());
		}
		else {
			this.typeActivite = null;
			this.tauxActivite = 0;
			this.finDernierElementImposable = null;
		}

		// [UNIREG-2662] ajout de l'attribut extensionExecutionForcee 
		if (rapport instanceof ch.vd.uniregctb.tiers.RepresentationConventionnelle) {
			final ch.vd.uniregctb.tiers.RepresentationConventionnelle repres = (ch.vd.uniregctb.tiers.RepresentationConventionnelle) rapport;
			this.extensionExecutionForcee = repres.getExtensionExecutionForcee();
		}
		else {
			this.extensionExecutionForcee = null;
		}
	}
}
