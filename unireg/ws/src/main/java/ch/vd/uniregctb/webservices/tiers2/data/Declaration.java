package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

/**
 * <b>Dans la version 3 du web-service :</b> <i>taxDeclarationType</i> (xml) / <i>TaxDeclaration</i> (client java)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Declaration", propOrder = {
		"id", "dateDebut", "dateFin", "dateAnnulation", "periodesFiscale", "delais", "etats"
})
public class Declaration implements Range {

	/**
	 * L'id technique (= clé primaire)
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>id</i>
	 */
	@XmlElement(required = true)
	public Long id;

	/**
	 * La date de début de validité de la déclaration.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateFrom</i>
	 */
	@XmlElement(required = true)
	public Date dateDebut;

	/**
	 * La date de fin de validité de la déclaration. Si la déclaration est toujours ouverte, cette date n'est pas renseignée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>dateTo</i>
	 */
	@XmlElement(required = false)
	public Date dateFin;

	/**
	 * Date à laquelle la déclaration a été annulée, ou <b>null</b> si elle n'est pas annulée.
	 * <p/>
	 * <b>Dans la version 3 du web-service :</b> <i>cancellationDate</i>
	 */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>taxPeriod</i>
	 */
	@XmlElement(required = true)
	public PeriodeFiscale periodesFiscale;

	/**
	 * <b>Dans la version 3 du web-service :</b> supprimé.
	 */
	@XmlElement(required = true)
	public final List<DelaiDeclaration> delais = new ArrayList<DelaiDeclaration>();

	/**
	 * <b>Dans la version 3 du web-service :</b> <i>statuses</i>
	 */
	@XmlElement(required = true)
	public final List<EtatDeclaration> etats = new ArrayList<EtatDeclaration>();

	public Declaration() {
	}

	public Declaration(ch.vd.uniregctb.declaration.Declaration declaration) {
		this.id = declaration.getId();
		this.dateDebut = DataHelper.coreToWeb(declaration.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(declaration.getDateFin());
		this.dateAnnulation = DataHelper.coreToWeb(declaration.getAnnulationDate());
		this.periodesFiscale = new PeriodeFiscale(declaration.getPeriode());

		// msi (11.09.2009) à priori, aucune application tiers n'est intéressée par les délais.
		// On évite donc de les charger pour des raisons de performances.
		// for (ch.vd.uniregctb.declaration.DelaiDeclaration delai : declaration.getDelais()) {
		// this.delais.add(new DelaiDeclaration(delai));
		// }

		for (ch.vd.uniregctb.declaration.EtatDeclaration etat : declaration.getEtats()) {
			this.etats.add(new EtatDeclaration(etat));
		}
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
