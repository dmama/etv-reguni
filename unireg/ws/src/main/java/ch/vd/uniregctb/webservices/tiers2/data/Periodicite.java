package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * La Periodicite permet de determiner à quel frequence les listes récapitulatives sont envoyées au débiteurs. Il n'y a qu'une périodicité par période fiscale
 * <p/>
 * <b>Dans la version 3 du web-service :</b> <i>debtorPeriodicityType</i> (xml) / <i>DebtorPeriodicity</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Periodicite", propOrder = {
		 "dateDebut", "dateFin", "dateAnnulation", "periodiciteDecompte", "periodeDecompte"})
public class Periodicite {

	/**
	 * La date de début de validité de la periodicité.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>.
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin de validité de la periodicité. peut être renseigner en cas de nouvelle périodicité pour la période fiscale suivante
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>.
	 */
	@XmlElement(required = false)
	public Date dateFin;

	/**
	 * Date à laquelle la périodicité a été annulée, ou <b>null</b> si elle n'est pas annulée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>cancellationDate</i>.
	 */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/**
	 * Type de periodicite
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>periodicity</i>.
	 */
	@XmlElement(required = true)
	public PeriodiciteDecompte periodiciteDecompte;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>specificPeriod</i>.
	 */
	@XmlElement(required = false)
	public PeriodeDecompte periodeDecompte;


	public Periodicite() {

	}

	public Periodicite(ch.vd.uniregctb.declaration.Periodicite periodicite) {
	
		dateDebut = DataHelper.coreToWeb(periodicite.getDateDebut());
		dateFin = DataHelper.coreToWeb(periodicite.getDateFin());
		dateAnnulation = DataHelper.coreToWeb(periodicite.getAnnulationDate());
		periodiciteDecompte = EnumHelper.coreToWeb(periodicite.getPeriodiciteDecompte());
		periodeDecompte = EnumHelper.coreToWeb(periodicite.getPeriodeDecompte());

	}

}
