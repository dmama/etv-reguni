package ch.vd.uniregctb.adresse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.vd.uniregctb.type.FormulePolitesse;

/**
 * Adresse d'envoi du courrier avec détail des valeurs.
 */
public class AdresseEnvoiDetaillee extends AdresseEnvoi {

	private static final long serialVersionUID = 8557039282754715615L;
	private String salutations;
	private String formuleAppel;
	private final List<String> nomPrenom = new ArrayList<String>();
	private String complement;
	private String pourAdresse;
	private String rueEtNumero;
	private String casePostale;
	private String npaEtLocalite;
	private String pays;

	public void addFormulePolitesse(FormulePolitesse formule) {
		this.salutations = formule.salutations();
		this.formuleAppel = formule.formuleAppel();
		addLine(this.salutations);
	}

	public void addFormulePolitesse(FormulePolitesse formule, int optionalite) {
		this.salutations = formule.salutations();
		this.formuleAppel = formule.formuleAppel();
		addLine(this.salutations, optionalite);
	}

	public void addNomPrenom(String ligne) {
		this.nomPrenom.add(ligne);
		addLine(ligne);
	}

	public void addNomPrenom(String ligne, int optionalite) {
		this.nomPrenom.add(ligne);
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

	public void addRueEtNumero(String ligne) {
		this.rueEtNumero = ligne;
		addLine(ligne);
	}

	public void addRueEtNumero(String ligne, int optionalite) {
		this.rueEtNumero = ligne;
		addLine(ligne, optionalite);
	}

	public void addCasePostale(String ligne) {
		this.casePostale = ligne;
		addLine(ligne);
	}

	public void addCasePostale(String ligne, int optionalite) {
		this.casePostale = ligne;
		addLine(ligne, optionalite);
	}

	public void addNpaEtLocalite(String ligne) {
		this.npaEtLocalite = ligne;
		addLine(ligne);
	}

	public void addNpaEtLocalite(String ligne, int optionalite) {
		this.npaEtLocalite = ligne;
		addLine(ligne, optionalite);
	}

	public void addPays(String ligne) {
		this.pays = ligne;
		addLine(ligne);
	}

	public void addPays(String ligne, int optionalite) {
		this.pays = ligne;
		addLine(ligne, optionalite);
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

	public List<String> getNomPrenom() {
		return Collections.unmodifiableList(nomPrenom);
	}

	public String getComplement() {
		return complement;
	}

	public String getPourAdresse() {
		return pourAdresse;
	}

	public String getRueEtNumero() {
		return rueEtNumero;
	}

	public String getCasePostale() {
		return casePostale;
	}

	public String getNpaEtLocalite() {
		return npaEtLocalite;
	}

	public String getPays() {
		return pays;
	}

	/**
	 * @return <code>vrai</code> si l'adresse est en Suisse; <code>faux</code> autrement.
	 */
	public boolean isSuisse() {
		return pays == null;
	}
}
