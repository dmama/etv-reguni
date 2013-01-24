package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * Cette classe contient les détails de l'assujettissement <b>au rôle ordinaire</b> d'un contribuable.
 * <p/>
 * <b>Note:</b> un assujettissement n'est pas limité par une année fiscale: il peut être plus court, égale ou plus long. Les dates de début et de fin correspondent donc aux dates réelles de changement
 * du status fiscal du contribuable.
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>simplifiedTaxLiabilityType</i> (xml) / <i>SimplifiedTaxLiability</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Assujettissement", propOrder = {
		"dateDebut", "dateFin", "type"
})
public class Assujettissement implements Range {

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>simplifiedTaxLiabilityTypeType</i> (xml) / <i>SimplifiedTaxLiabilityType</i> (client java)
	 */
	@XmlType(name = "TypeAssujettissement")
	@XmlEnum(String.class)
	public static enum TypeAssujettissement {
		/**
		 * Le contribuable est assujetti à raison de fors secondaires uniquement.
		 */
		LIMITE,
		/**
		 * Le contribuable est assujetti à raison de un ou plusieurs fors principaux
		 */
		ILLIMITE;

		public static TypeAssujettissement fromValue(String v) {
			return valueOf(v);
		}
	}

	/**
	 * La date de début effective de l'assujettissement.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin effective de l'assujettissement.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>.
	 */
	@XmlElement(required = true)
	public Date dateFin;

	/**
	 * Le type d'assujettissement
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>type</i>.
	 */
	@XmlElement(required = true)
	public TypeAssujettissement type;

	public Assujettissement() {
	}

	public Assujettissement(Assujettissement right) {
		this.dateDebut = right.dateDebut;
		this.dateFin = right.dateFin;
		this.type = right.type;
	}

	public Assujettissement(ch.vd.uniregctb.metier.assujettissement.Assujettissement assujettissement, TypeAssujettissement type) {
		this.dateDebut = DataHelper.coreToWeb(assujettissement.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(assujettissement.getDateFin());
		this.type = type;
	}

	public Assujettissement(Date dateDebut, Date dateFin, TypeAssujettissement type) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
	}

	public static Assujettissement coreToLIC(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {

		final Assujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = null;
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = new Assujettissement(a, Assujettissement.TypeAssujettissement.ILLIMITE);
			}
			else {
				result = new Assujettissement(a, Assujettissement.TypeAssujettissement.LIMITE);
			}
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.Indigent);
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.ILLIMITE);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.dateFin != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = RegDateHelper.get(result.dateFin.asJavaDate());
			if (dateFin.isAfter(aujourdhui)) {
				result.dateFin = null;
			}
		}

		return result;
	}

	public static Assujettissement coreToLIFD(ch.vd.uniregctb.metier.assujettissement.Assujettissement a) {

		final Assujettissement result;

		if (a instanceof ch.vd.uniregctb.metier.assujettissement.DiplomateSuisse) {
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.ILLIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsCanton) {
			result = null; // il sera assujetti de manière illimité dans son canton de résidence
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.HorsSuisse) {
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.LIMITE);
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierMixte) {
			ch.vd.uniregctb.metier.assujettissement.SourcierMixte mixte = (ch.vd.uniregctb.metier.assujettissement.SourcierMixte) a;
			if (mixte.getTypeAutoriteFiscale() == ch.vd.uniregctb.type.TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
				result = new Assujettissement(a, Assujettissement.TypeAssujettissement.ILLIMITE);
			}
			else {
				result = new Assujettissement(a, Assujettissement.TypeAssujettissement.LIMITE);
			}
		}
		else if (a instanceof ch.vd.uniregctb.metier.assujettissement.SourcierPur) {
			// un sourcier pure n'est pas assujetti au rôle ordinaire.
			result = null;
		}
		else {
			Assert.isTrue(a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisOrdinaire
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.VaudoisDepense
					|| a instanceof ch.vd.uniregctb.metier.assujettissement.Indigent);
			result = new Assujettissement(a, Assujettissement.TypeAssujettissement.ILLIMITE);
		}

		// [UNIREG-1517] l'assujettissement courant est laissé ouvert
		if (result != null && result.dateFin != null) {
			final RegDate aujourdhui = RegDate.get();
			final RegDate dateFin = RegDateHelper.get(result.dateFin.asJavaDate());
			if (dateFin.isAfter(aujourdhui)) {
				result.dateFin = null;
			}
		}

		return result;
	}

	@Override
	public Date getDateDebut() {
		return dateDebut;
	}

	@Override
	public Date getDateFin() {
		return dateFin;
	}

	@Override
	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	@Override
	public void setDateFin(Date v) {
		dateFin = v;
	}
}
