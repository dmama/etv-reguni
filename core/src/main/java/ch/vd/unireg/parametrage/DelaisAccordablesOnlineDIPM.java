package ch.vd.unireg.parametrage;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.type.delai.Delai;

/**
 * Délais accordables pour les demandes de délais online (e-Délai) sur des DIs PM et valables à partir d'un certain temps après la date de bouclement (e.g. à partir de la date bouclement, les délais accordables sont DB + 6 mois et DB + 9 mois).
 */
@Entity
@DiscriminatorValue("DI_PM")
public class DelaisAccordablesOnlineDIPM extends DelaisAccordablesOnline {

	private int index;
	private Delai delaiDebut;
	private List<Delai> delaisDemandeUnitaire;
	private List<Delai> delaisDemandeGroupee;

	// nécessaire pour Hibernate
	public DelaisAccordablesOnlineDIPM() {
	}

	public DelaisAccordablesOnlineDIPM(int index, Delai delaiDebut) {
		this.index = index;
		this.delaiDebut = delaiDebut;
		this.delaisDemandeUnitaire = Collections.emptyList();
		this.delaisDemandeGroupee = Collections.emptyList();
	}

	public DelaisAccordablesOnlineDIPM(int index, @NotNull Delai delaiDebut, @Nullable Delai delaiDemandeUnitaire, @Nullable Delai delaiDemandeGroupee) {
		this.index = index;
		this.delaiDebut = delaiDebut;
		this.delaisDemandeUnitaire = (delaiDemandeUnitaire == null ? Collections.emptyList() : Collections.singletonList(delaiDemandeUnitaire));
		this.delaisDemandeGroupee = (delaiDemandeGroupee == null ? Collections.emptyList() : Collections.singletonList(delaiDemandeGroupee));
	}

	public DelaisAccordablesOnlineDIPM(int index, @NotNull Delai delaiDebut, @NotNull List<Delai> delaisDemandeUnitaire, @NotNull List<Delai> delaisDemandeGroupee) {
		this.index = index;
		this.delaiDebut = delaiDebut;
		this.delaisDemandeUnitaire = delaisDemandeUnitaire;
		this.delaisDemandeGroupee = delaisDemandeGroupee;
	}

	/**
	 * <b>Note:</b> cet index est obligatoire car il n'est pas possible d'ordonner les périodes en utilisant le <i>delaiDebut</i> parce que les délais exprimés en mois ont des durées relatives en fonction du jour de départ.
	 * <p>
	 * Exemple : délais de 1 mois vs 30 jours
	 * <table border="1">
	 * <tr><th>Date de référence / Délai</th><th>+ 1 mois</th><th>+ 30 jours</th><th>Résultat de la comparaison</th></th>
	 * <tr><th>01.01.2003</th><td>01.02.2003</td><td>31.01.2003</td><td>1 mois > 30 jours</td></tr>
	 * <tr><th>01.02.2003</th><td>01.03.2003</td><td>02.03.2003</td><td>30 jours > 1 mois</td></tr>
	 * </table>
	 *
	 * @return l'index qui permet d'ordonner les périodes de validité des délais PM
	 */
	@Column(name = "INDEX_PERIODE")
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * @return le délai compté depuis de la date de bouclement (ou d'émission, selon la paramètrisation) à partir duquel les délais sont valables.
	 */
	@Column(name = "DELAI_DEBUT")
	@Type(type = "ch.vd.unireg.hibernate.DelaiUserType")
	public Delai getDelaiDebut() {
		return delaiDebut;
	}

	public void setDelaiDebut(Delai delaiDebut) {
		this.delaiDebut = delaiDebut;
	}

	@Column(name = "UNITAIRE_PM")
	@Type(type = "ch.vd.unireg.hibernate.DelaiListUserType")
	public List<Delai> getDelaisDemandeUnitaire() {
		return delaisDemandeUnitaire;
	}

	public void setDelaisDemandeUnitaire(List<Delai> delaisDemandeUnitaire) {
		this.delaisDemandeUnitaire = delaisDemandeUnitaire;
	}

	@Column(name = "GROUPEE_PM")
	@Type(type = "ch.vd.unireg.hibernate.DelaiListUserType")
	public List<Delai> getDelaisDemandeGroupee() {
		return delaisDemandeGroupee;
	}

	public void setDelaisDemandeGroupee(List<Delai> delaisDemandeGroupee) {
		this.delaisDemandeGroupee = delaisDemandeGroupee;
	}
}
