package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Individu;

/**
 * Contient les caractéristiques d'un rapport de filiation entre une personne physique/individu et une autre personne physique/individu.
 */
public class RapportFiliation {

	public enum Type {
		ENFANT,
		PARENT
	}

	/**
	 * L'individu de référence
	 */
	private Individu individu;

	/**
	 * La personne physique de référence
	 */
	private PersonnePhysique personnePhysique;

	/**
	 * L'autre individu (enfant ou parent)
	 */
	private Individu autreIndividu;

	/**
	 * L'autre personne physique (enfant ou parent)
	 */
	private PersonnePhysique autrePersonnePhysique;

	private final Type type;
	private RegDate dateDebut;
	private RegDate dateFin;

	public RapportFiliation(Individu individu, PersonnePhysique personnePhysique, Individu autreIndividu, @Nullable PersonnePhysique autrePersonnePhysique, Type type) {
		this.individu = individu;
		this.personnePhysique = personnePhysique;
		this.autreIndividu = autreIndividu;
		this.autrePersonnePhysique = autrePersonnePhysique;
		this.type = type;

		// le rapport est terminé au décès de l'un des membres
		if (individu.getDateDeces() != null || autreIndividu.getDateDeces() != null) {
			this.dateFin = RegDateHelper.minimum(individu.getDateDeces(), autreIndividu.getDateDeces(), NullDateBehavior.LATEST);
		}

		// le rapport démarre à la naissance du dernier membre
		this.dateDebut = RegDateHelper.maximum(individu.getDateNaissance(), autreIndividu.getDateNaissance(), NullDateBehavior.EARLIEST);

	}

	public Individu getIndividu() {
		return individu;
	}

	public PersonnePhysique getPersonnePhysique() {
		return personnePhysique;
	}

	public Individu getAutreIndividu() {
		return autreIndividu;
	}

	@Nullable("Dans le cas où aucun contribuable n'existe par rapport au numéro d'individu")
	public PersonnePhysique getAutrePersonnePhysique() {
		return autrePersonnePhysique;
	}

	public Type getType() {
		return type;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}
}
