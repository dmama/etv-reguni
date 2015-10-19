package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatTache;

@Entity
public abstract class TacheEnvoiDeclarationImpot extends TacheEnvoiDocument implements DateRange {

	/**
	 * Date de début d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateDebut;

	/**
	 * Date de fin d'imposition (précalculé) pour la déclaration à envoyer.
	 */
	private RegDate dateFin;

	// Ce constructeur est requis par Hibernate
	protected TacheEnvoiDeclarationImpot() {
	}

	public TacheEnvoiDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeDocument typeDocument, CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable, collectivite, typeDocument);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	@Column(name = "DECL_DATE_DEBUT")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	@Override
	@Column(name = "DECL_DATE_FIN")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate theDateFin) {
		dateFin = theDateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
