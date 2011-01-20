package ch.vd.uniregctb.webservices.tiers;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import org.springframework.util.Assert;

import ch.vd.uniregctb.webservices.tiers.impl.DataHelper;

/**
 * Cette classe contient les détails de l'assujettissement <b>au rôle ordinaire</b> d'un contribuable.
 * <p>
 * <b>Note:</b> un assujettissement n'est pas limité par une année fiscale: il peut être plus court, égale ou plus long. Les dates de début
 * et de fin correspondent donc aux dates réelles de changement du status fiscal du contribuable.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Assujettissement", propOrder = {
		"dateDebut", "dateFin", "LIC", "LIFD"
})
public class Assujettissement {

	@XmlType(name = "TypeAssujettissement")
	@XmlEnum(String.class)
	public static enum TypeAssujettissement {
		NON_ASSUJETTI,
		/** Le contribuable est assujetti à raison de fors secondaires uniquement. */
		LIMITE,
		/** Le contribuable est assujetti à raison de un ou plusieurs fors principaux */
		ILLIMITE;

		public static TypeAssujettissement fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * La date de début effective de l'assujettissement.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin effective de l'assujettissement.
	 */
	@XmlElement(required = true)
	public Date dateFin;

	/** L'assujettissement LIC (Commune/canton) */
	@XmlElement(required = true)
	public TypeAssujettissement LIC;

	/** L'assujettissement LIFD (fédéral) */
	@XmlElement(required = true)
	public TypeAssujettissement LIFD;

	public Assujettissement() {
	}

	public Assujettissement(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement) {
		this.dateDebut = DataHelper.coreToWeb(assujettissement.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(assujettissement.getDateFin());

		if (assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			this.LIC = TypeAssujettissement.NON_ASSUJETTI;
			this.LIFD = TypeAssujettissement.ILLIMITE;
		}
		else if (assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			this.LIC = TypeAssujettissement.LIMITE;
			this.LIFD = TypeAssujettissement.NON_ASSUJETTI; // il sera assujetti de manière illimité dans son canton de résidence
		}
		else if (assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			this.LIC = TypeAssujettissement.LIMITE;
			this.LIFD = TypeAssujettissement.LIMITE;
		}
		else if (assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) assujettissement;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				this.LIC = TypeAssujettissement.ILLIMITE;
				this.LIFD = TypeAssujettissement.ILLIMITE;
			}
			else {
				this.LIC = TypeAssujettissement.LIMITE;
				this.LIFD = TypeAssujettissement.LIMITE;
			}
		}
		else if (assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			this.LIC = TypeAssujettissement.NON_ASSUJETTI;
			this.LIFD = TypeAssujettissement.NON_ASSUJETTI;
		}
		else {
			Assert.isTrue(assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire
					|| assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisDepense
					|| assujettissement instanceof ch.vd.uniregctb.metier.assujettissement.Indigent);
			this.LIC = TypeAssujettissement.ILLIMITE;
			this.LIFD = TypeAssujettissement.ILLIMITE;
		}
	}
}
