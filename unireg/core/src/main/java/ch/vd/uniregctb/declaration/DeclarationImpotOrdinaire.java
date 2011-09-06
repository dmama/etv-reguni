package ch.vd.uniregctb.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Date;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.type.Qualification;
import ch.vd.uniregctb.type.TypeContribuable;
import ch.vd.uniregctb.type.TypeDocument;

@Entity
@DiscriminatorValue("DI")
public class DeclarationImpotOrdinaire extends Declaration {

	private static final long serialVersionUID = -4869699873165367700L;

	/**
	 * [SIFISC-1368] première année où le retour par courrier électronique des déclarations d'impôt est possible.
	 */
	public static final int PREMIERE_ANNEE_RETOUR_ELECTRONIQUE = 2011;

	/**
	 * Numéro de séquence de la déclaration pour une période fiscale. La première déclaration prends le numéro 1.
	 */
	private Integer numero;

	private Integer numeroOfsForGestion;

	private TypeContribuable typeContribuable;

	private Date dateImpressionChemiseTaxationOffice;

	private Qualification qualification;

	private RegDate delaiRetourImprime;

	private Long retourCollectiviteAdministrativeId;

	/**
	 * <code>true</code> si la DI a été créée comme une "di libre", c'est-à-dire une DI sur la période courante (au moment de sa création) sans fin d'assujettissement connue (comme un décès ou un départ
	 * HS)
	 */
	private boolean libre;

	/**
	 * [SIFISC-1368] Code pour le contrôle du retour électronique de la DI. Ce code est le même sur toutes les déclarations d'une période fiscale et d'un contribuable donné.
	 */
	private String codeControle;

	/**
	 * [SIFISC-2100] Code de segmentation, ou Code Segment, fourni par TAO et utilisé lors de l'émission de la DI suivante
	 */
	private Integer codeSegment;

	@Column(name = "RETOUR_COLL_ADMIN_ID")
	@ForeignKey(name = "FK_DECL_RET_COLL_ADMIN_ID")
	public Long getRetourCollectiviteAdministrativeId() {
		return retourCollectiviteAdministrativeId;
	}

	public void setRetourCollectiviteAdministrativeId(Long retourCollectiviteAdministrativeId) {
		this.retourCollectiviteAdministrativeId = retourCollectiviteAdministrativeId;
	}

	@Column(name = "NUMERO")
	public Integer getNumero() {
		return numero;
	}

	public void setNumero(Integer theNumero) {
		numero = theNumero;
	}

	@Column(name = "TYPE_CTB", length = LengthConstants.DI_TYPE)
	@Type(type = "ch.vd.uniregctb.hibernate.TypeContribuableUserType")
	public TypeContribuable getTypeContribuable() {
		return typeContribuable;
	}

	public void setTypeContribuable(TypeContribuable theTypeContribuable) {
		typeContribuable = theTypeContribuable;
	}

	@Column(name = "DATE_IMPR_CHEMISE_TO")
	public Date getDateImpressionChemiseTaxationOffice() {
		return dateImpressionChemiseTaxationOffice;
	}

	public void setDateImpressionChemiseTaxationOffice(Date theDateImpressionChemiseTaxationOffice) {
		dateImpressionChemiseTaxationOffice = theDateImpressionChemiseTaxationOffice;
	}

	@Column(name = "QUALIFICATION", length = LengthConstants.DI_QUALIF)
	@Type(type = "ch.vd.uniregctb.hibernate.QualificationUserType")
	public Qualification getQualification() {
		return qualification;
	}

	public void setQualification(Qualification qualification) {
		this.qualification = qualification;
	}

	@Column(name = "CODE_SEGMENT")
	public Integer getCodeSegment() {
		return codeSegment;
	}

	public void setCodeSegment(Integer codeSegment) {
		this.codeSegment = codeSegment;
	}

	/**
	 * [UNIREG-1740] Le délai de retour tel que devant être imprimé sur le déclaration papier. Ce délai peut être nul, auquel cas on utilisera le délai accordé comme valeur de remplacement.
	 *
	 * @return une date correspondant au délai de retour; ou <i>null</i> si l'information n'est pas disponible.
	 */
	@Column(name = "DELAI_RETOUR_IMPRIME")
	@Type(type = "ch.vd.uniregctb.hibernate.RegDateUserType")
	public RegDate getDelaiRetourImprime() {
		return delaiRetourImprime;
	}

	public void setDelaiRetourImprime(RegDate delaiRetourImprime) {
		this.delaiRetourImprime = delaiRetourImprime;
	}

	@Column(name = "NO_OFS_FOR_GESTION")
	public Integer getNumeroOfsForGestion() {
		return numeroOfsForGestion;
	}

	public void setNumeroOfsForGestion(Integer theNumeroOfsForGestion) {
		numeroOfsForGestion = theNumeroOfsForGestion;
	}

	@Column(name = "LIBRE")
	public boolean isLibre() {
		return libre;
	}

	public void setLibre(boolean libre) {
		this.libre = libre;
	}

	@Column(name = "CODE_CONTROLE", length = LengthConstants.DI_CODE_CONTROLE)
	public String getCodeControle() {
		return codeControle;
	}

	public void setCodeControle(String codeControle) {
		this.codeControle = codeControle;
	}

	@Transient
	public TypeDocument getTypeDeclaration() {
		final ModeleDocument modele = getModeleDocument();
		if (modele == null) {
			return null;
		}
		return modele.getTypeDocument();
	}

	// Toutes les lettres, sauf le 'O' qui peut être confondu avec le '0'.
	private static final char CODE_LETTERS[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	/**
	 * Génère un code de contrôle pour le retour des déclarations d'impôt sous forme électronique. Ce code de contrôle est une string de 6 caractères composée d'une lettre suivie de 5 chiffres pris au
	 * hazard (voir spécification dans SIFISC-1368).
	 * <p/>
	 * <b>Exemples</b>:
	 * <ul>
	 *     <li>B62116</li>
	 *     <li>U94624</li>
	 *     <li>H57736</li>
	 *     <li>E93590</li>
	 *     <li>V34032</li>
	 *     <li>N43118</li>
	 *     <li>B98052</li>
	 *     <li>S67086</li>
	 *     <li>...</li>
	 * </ul>
	 *
	 * @return un code de contrôle
	 */
	public static String generateCodeControle() {
		final int letter_index = (int) (CODE_LETTERS.length * Math.random());
		final int number = (int) (100000 * Math.random());
		return String.format("%s%05d", CODE_LETTERS[letter_index], number);
	}
}
