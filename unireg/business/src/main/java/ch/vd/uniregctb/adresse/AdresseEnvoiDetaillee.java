package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.CasePostale;
import ch.vd.uniregctb.common.NomPrenom;
import ch.vd.uniregctb.common.NpaEtLocalite;
import ch.vd.uniregctb.common.RueEtNumero;
import ch.vd.uniregctb.interfaces.model.TypeAffranchissement;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.type.FormulePolitesse;

/**
 * Adresse d'envoi du courrier avec détail des valeurs.
 */
public class AdresseEnvoiDetaillee extends AdresseEnvoi implements DateRange {

	private static final long serialVersionUID = 8557039282754715615L;

	private RegDate dateDebut;
	private RegDate dateFin;
	private final Tiers destinataire;
	private String salutations;
	private String formuleAppel;
	private final List<NomPrenom> nomsPrenoms = new ArrayList<NomPrenom>();
	private final List<String> raisonsSociales = new ArrayList<String>();
	private String complement;
	private String pourAdresse;
	private String numeroAppartement;
	private RueEtNumero rueEtNumero;
	private CasePostale casePostale;
	private NpaEtLocalite npaEtLocalite;
	private String pays;
	private TypeAffranchissement typeAffranchissement = TypeAffranchissement.SUISSE;
	private Integer numeroOrdrePostal;
	private Integer numeroTechniqueRue;
	private Integer noOfsPays;
	private final AdresseGenerique.SourceType source;

	public AdresseEnvoiDetaillee(Tiers destinataire, AdresseGenerique.SourceType source, RegDate dateDebut, RegDate dateFin) {
		this.source = source;
		this.destinataire = destinataire;
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public Tiers getDestinataire() {
		return destinataire;
	}

	public void addFormulePolitesse(FormulePolitesse formule) {
		this.salutations = formule.salutations();
		this.formuleAppel = formule.formuleAppel();
		if (this.salutations != null) {
			addLine(this.salutations);
		}
	}

	public void addFormulePolitesse(FormulePolitesse formule, int optionalite) {
		this.salutations = formule.salutations();
		this.formuleAppel = formule.formuleAppel();
		if (this.salutations != null) {
			addLine(this.salutations, optionalite);
		}
	}

	public void addNomPrenom(NomPrenom ligne) {
		this.nomsPrenoms.add(ligne);
		addLine(ligne.getNomPrenom());
	}

	public void addNomPrenom(NomPrenom ligne, int optionalite) {
		this.nomsPrenoms.add(ligne);
		addLine(ligne.getNomPrenom(), optionalite);
	}

	public void addRaisonSociale(String ligne) {
		this.raisonsSociales.add(ligne);
		addLine(ligne);
	}

	public void addRaisonSociale(String ligne, int optionalite) {
		this.raisonsSociales.add(ligne);
		addLine(ligne, optionalite);
	}

	public void addComplement(String ligne) {
		this.complement = ligne;
		addLine(ligne);
	}

	public void addComplement(String ligne, int optionalite) {
		this.complement = ligne;
		addLine(ligne, optionalite);
	}

	public void addPourAdresse(String ligne) {
		this.pourAdresse = ligne;
		addLine(ligne);
	}

	public void addPourAdresse(String ligne, int optionalite) {
		this.pourAdresse = ligne;
		addLine(ligne, optionalite);
	}

	public void addRueEtNumero(RueEtNumero ligne) {
		this.rueEtNumero = ligne;
		addLine(ligne.getRueEtNumero());
	}

	public void addRueEtNumero(RueEtNumero ligne, int optionalite) {
		this.rueEtNumero = ligne;
		addLine(ligne.getRueEtNumero(), optionalite);
	}

	public void addCasePostale(CasePostale ligne) {
		this.casePostale = ligne;
		addLine(ligne.toString());
	}

	public void addCasePostale(CasePostale ligne, int optionalite) {
		this.casePostale = ligne;
		addLine(ligne.toString(), optionalite);
	}

	public void addNpaEtLocalite(NpaEtLocalite ligne) {
		this.npaEtLocalite = ligne;
		addLine(ligne.toString());
	}

	public void addNpaEtLocalite(NpaEtLocalite ligne, int optionalite) {
		this.npaEtLocalite = ligne;
		addLine(ligne.toString(), optionalite);
	}

	public void addPays(String ligne, TypeAffranchissement typeAffranchissement) {
		this.pays = ligne;
		this.typeAffranchissement = typeAffranchissement;
		addLine(ligne);
	}

	public void addPays(String ligne, TypeAffranchissement typeAffranchissement, int optionalite) {
		this.pays = ligne;
		this.typeAffranchissement = typeAffranchissement;
		addLine(ligne, optionalite);
	}

	public void setNumeroAppartement(String numeroAppartement) {
		this.numeroAppartement = numeroAppartement;
	}

	public void setNumeroOrdrePostal(Integer numeroOrdrePostal) {
		this.numeroOrdrePostal = numeroOrdrePostal;
	}

	public void setNumeroTechniqueRue(Integer numeroTechniqueRue) {
		this.numeroTechniqueRue = numeroTechniqueRue;
	}

	public void setNoOfsPays(Integer noOfsPays) {
		this.noOfsPays = noOfsPays;
	}

	public AdresseGenerique.SourceType getSource() {
		return source;
	}

	/**
	 * @return les salutations selon les us et coutumes de l'ACI. Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Aux héritiers de</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String getSalutations() {
		return salutations;
	}

	/**
	 * [UNIREG-1398]
	 *
	 * @return la formule d'appel stricte. C'est-à-dire les salutations mais <b>sans formule spéciale</b> propre à l'ACI (pas de <i>Aux
	 *         héritiers de</i>). Exemples :
	 *         <ul>
	 *         <li>Monsieur</li>
	 *         <li>Madame</li>
	 *         <li>Madame, Monsieur</li>
	 *         <li>...</li>
	 *         </ul>
	 */
	public String getFormuleAppel() {
		return formuleAppel;
	}

	@NotNull
	public List<NomPrenom> getNomsPrenoms() {
		return Collections.unmodifiableList(nomsPrenoms);
	}

	@NotNull
	public List<String> getRaisonsSociales() {
		return Collections.unmodifiableList(raisonsSociales);
	}

	/**
	 * @return la concaténation des listes <i>nomsPrenoms</i> et <i>raisonsSociales</i> (en sachant que - logiquement - seule une des deux peut être remplie).
	 */
	@NotNull
	public List<String> getNomsPrenomsOuRaisonsSociales() {
		List<String> list = new ArrayList<String>();
		for (NomPrenom nomPrenom : nomsPrenoms) {
			list.add(nomPrenom.getNomPrenom());
		}
		for (String raison : raisonsSociales) {
			list.add(raison);
		}
		return list;
	}

	public String getComplement() {
		return complement;
	}

	public String getPourAdresse() {
		return pourAdresse;
	}

	public String getNumeroAppartement() {
		return numeroAppartement;
	}

	public RueEtNumero getRueEtNumero() {
		return rueEtNumero;
	}

	public CasePostale getCasePostale() {
		return casePostale;
	}

	public NpaEtLocalite getNpaEtLocalite() {
		return npaEtLocalite;
	}

	public String getPays() {
		return pays;
	}

	public TypeAffranchissement getTypeAffranchissement() {
		return typeAffranchissement;
	}

	/**
	 * @return <code>vrai</code> si l'adresse est en Suisse; <code>faux</code> autrement.
	 */
	public boolean isSuisse() {
		return typeAffranchissement == TypeAffranchissement.SUISSE;
	}

	public Integer getNumeroOrdrePostal() {
		return numeroOrdrePostal;
	}

	public Integer getNumeroTechniqueRue() {
		return numeroTechniqueRue;
	}

	public Integer getNoOfsPays() {
		return noOfsPays;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		final AdresseEnvoiDetaillee that = (AdresseEnvoiDetaillee) o;

		if (casePostale != null ? !casePostale.equals(that.casePostale) : that.casePostale != null) return false;
		if (complement != null ? !complement.equals(that.complement) : that.complement != null) return false;
		if (dateDebut != null ? !dateDebut.equals(that.dateDebut) : that.dateDebut != null) return false;
		if (dateFin != null ? !dateFin.equals(that.dateFin) : that.dateFin != null) return false;
		if (destinataire != null ? !destinataire.equals(that.destinataire) : that.destinataire != null) return false;
		if (formuleAppel != null ? !formuleAppel.equals(that.formuleAppel) : that.formuleAppel != null) return false;
		if (noOfsPays != null ? !noOfsPays.equals(that.noOfsPays) : that.noOfsPays != null) return false;
		if (nomsPrenoms != null ? !nomsPrenoms.equals(that.nomsPrenoms) : that.nomsPrenoms != null) return false;
		if (npaEtLocalite != null ? !npaEtLocalite.equals(that.npaEtLocalite) : that.npaEtLocalite != null) return false;
		if (numeroAppartement != null ? !numeroAppartement.equals(that.numeroAppartement) : that.numeroAppartement != null) return false;
		if (numeroOrdrePostal != null ? !numeroOrdrePostal.equals(that.numeroOrdrePostal) : that.numeroOrdrePostal != null) return false;
		if (numeroTechniqueRue != null ? !numeroTechniqueRue.equals(that.numeroTechniqueRue) : that.numeroTechniqueRue != null) return false;
		if (pays != null ? !pays.equals(that.pays) : that.pays != null) return false;
		if (pourAdresse != null ? !pourAdresse.equals(that.pourAdresse) : that.pourAdresse != null) return false;
		if (raisonsSociales != null ? !raisonsSociales.equals(that.raisonsSociales) : that.raisonsSociales != null) return false;
		if (rueEtNumero != null ? !rueEtNumero.equals(that.rueEtNumero) : that.rueEtNumero != null) return false;
		if (salutations != null ? !salutations.equals(that.salutations) : that.salutations != null) return false;
		if (source != that.source) return false;
		if (typeAffranchissement != that.typeAffranchissement) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (dateDebut != null ? dateDebut.hashCode() : 0);
		result = 31 * result + (dateFin != null ? dateFin.hashCode() : 0);
		result = 31 * result + (destinataire != null ? destinataire.hashCode() : 0);
		result = 31 * result + (salutations != null ? salutations.hashCode() : 0);
		result = 31 * result + (formuleAppel != null ? formuleAppel.hashCode() : 0);
		result = 31 * result + (nomsPrenoms != null ? nomsPrenoms.hashCode() : 0);
		result = 31 * result + (raisonsSociales != null ? raisonsSociales.hashCode() : 0);
		result = 31 * result + (complement != null ? complement.hashCode() : 0);
		result = 31 * result + (pourAdresse != null ? pourAdresse.hashCode() : 0);
		result = 31 * result + (numeroAppartement != null ? numeroAppartement.hashCode() : 0);
		result = 31 * result + (rueEtNumero != null ? rueEtNumero.hashCode() : 0);
		result = 31 * result + (casePostale != null ? casePostale.hashCode() : 0);
		result = 31 * result + (npaEtLocalite != null ? npaEtLocalite.hashCode() : 0);
		result = 31 * result + (pays != null ? pays.hashCode() : 0);
		result = 31 * result + (typeAffranchissement != null ? typeAffranchissement.hashCode() : 0);
		result = 31 * result + (numeroOrdrePostal != null ? numeroOrdrePostal.hashCode() : 0);
		result = 31 * result + (numeroTechniqueRue != null ? numeroTechniqueRue.hashCode() : 0);
		result = 31 * result + (noOfsPays != null ? noOfsPays.hashCode() : 0);
		result = 31 * result + (source != null ? source.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "AdresseEnvoiDetaillee{" +
				"dateDebut=" + dateDebut +
				", dateFin=" + dateFin +
				", destinataire=" + destinataire +
				", salutations='" + salutations + '\'' +
				", formuleAppel='" + formuleAppel + '\'' +
				", nomsPrenoms=" + nomsPrenoms +
				", raisonsSociales=" + raisonsSociales +
				", complement='" + complement + '\'' +
				", pourAdresse='" + pourAdresse + '\'' +
				", rueEtNumero=" + rueEtNumero +
				", casePostale=" + casePostale +
				", npaEtLocalite='" + npaEtLocalite + '\'' +
				", pays='" + pays + '\'' +
				", typeAffranchissement=" + typeAffranchissement +
				", source=" + source +
				"} " + super.toString();
	}
}
