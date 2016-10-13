package ch.vd.uniregctb.tiers;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.TypeContribuable;
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
		return !isAnnule() && DateRange.super.isValidAt(date);
	}

	@Column(name = "DECL_TYPE_CTB", length = LengthConstants.DI_TYPE_CTB)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	@Index(name = "IDX_TACHE_TYPE_CTB")
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}
}
