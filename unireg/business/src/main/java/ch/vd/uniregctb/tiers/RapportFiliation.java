package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Individu;

/**
 * Contient les caractéristiques d'un rapport de filiation entre une personne physique/individu et une autre personne physique/individu.
 */
public class RapportFiliation implements DateRange {

	public static enum Type {
		ENFANT,
		PARENT
	}

	/**
	 * L'individu de référence
	 */
	private final Individu individu;

	/**
	 * La personne physique de référence
	 */
	private final PersonnePhysique personnePhysique;

	/**
	 * L'autre individu (enfant ou parent)
	 */
	private final Individu autreIndividu;

	/**
	 * L'autre personne physique (enfant ou parent)
	 */
	private final PersonnePhysique autrePersonnePhysique;

	private final Type type;
	private final RegDate dateDebut;
	private final RegDate dateFin;

	public RapportFiliation(Individu individu, PersonnePhysique personnePhysique, Individu autreIndividu, @Nullable PersonnePhysique autrePersonnePhysique,
	                        RegDate dateDebut, RegDate dateFin, Type type) {
		this.individu = individu;
		this.personnePhysique = personnePhysique;
		this.autreIndividu = autreIndividu;
		this.autrePersonnePhysique = autrePersonnePhysique;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
		this.type = type;
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

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}
}
