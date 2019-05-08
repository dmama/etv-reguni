package ch.vd.unireg.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.TypeContribuable;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatTache;

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

	/**
	 * Type de contribuable (précalculé) pour la déclaration à envoyer.
	 */
	private TypeContribuable typeContribuable;


	// Ce constructeur est requis par Hibernate
	protected TacheEnvoiDeclarationImpot() {
	}

	public TacheEnvoiDeclarationImpot(TypeEtatTache etat, RegDate dateEcheance, Contribuable contribuable, RegDate dateDebut, RegDate dateFin, TypeContribuable typeContribuable,
	                                  TypeDocument typeDocument, CollectiviteAdministrative collectivite) {
		super(etat, dateEcheance, contribuable, collectivite, typeDocument);
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.typeContribuable = typeContribuable;
	}

	@Override
	@Column(name = "DECL_DATE_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate theDateDebut) {
		dateDebut = theDateDebut;
	}

	@Override
	@Column(name = "DECL_DATE_FIN")
	@Type(type = "ch.vd.unireg.hibernate.RegDateUserType")
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate theDateFin) {
		dateFin = theDateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@Column(name = "DECL_TYPE_CTB", length = LengthConstants.DI_TYPE_CTB)
	@Type(type = "ch.vd.unireg.hibernate.TypeContribuableUserType")
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}
}
