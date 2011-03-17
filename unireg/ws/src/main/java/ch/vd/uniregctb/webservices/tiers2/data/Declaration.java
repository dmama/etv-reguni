package ch.vd.uniregctb.webservices.tiers2.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Declaration", propOrder = {
		"id", "dateDebut", "dateFin", "dateAnnulation", "periodesFiscale", "delais", "etats"
})
public class Declaration implements Range {

	/** L'id technique (= clé primaire) */
	@XmlElement(required = true)
	public Long id;

	/** La date de début de validité de la déclaration. */
	@XmlElement(required = true)
	public Date dateDebut;

	/** La date de fin de validité de la déclaration. Si la déclaration est toujours ouverte, cette date n'est pas renseignée. */
	@XmlElement(required = false)
	public Date dateFin;

	/** Date à laquelle la déclaration a été annulée, ou <b>null</b> si elle n'est pas annulée. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	@XmlElement(required = true)
	public PeriodeFiscale periodesFiscale;

	@XmlElement(required = true)
	public final List<DelaiDeclaration> delais = new ArrayList<DelaiDeclaration>();

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

	public Date getDateDebut() {
		return dateDebut;
	}

	public Date getDateFin() {
		return dateFin;
	}

	public void setDateDebut(Date v) {
		dateDebut = v;
	}

	public void setDateFin(Date v) {
		dateFin = v;
	}
}
