package ch.vd.uniregctb.declaration;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.tiers.LinkedEntity;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

/**
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @author jec
 *
 * @uml.annotations
 *     derived_abstraction="platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_0d5HUOqeEdySTq6PFlf9jQ"
 */
@Entity
public abstract class EtatDeclaration extends EtatDocumentFiscal<TypeEtatDeclaration, EtatDeclaration> implements LinkedEntity {

	private TypeEtatDeclaration etat;

	public EtatDeclaration() {
		super();
	}

	public EtatDeclaration(RegDate dateObtention) {
		super(dateObtention);
	}

	@Override
	@Transient
	public abstract TypeEtatDeclaration getEtat();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param theEtat the etat to set
	 * @generated "sourceid:platform:/resource/UniregCTB/04Unireg%20-%20data%20model%20tiers.emx#_TNdzAOqfEdySTq6PFlf9jQ?SETTER"
	 */
	public void setEtat(TypeEtatDeclaration theEtat) {
		// begin-user-code
		etat = theEtat;
		// end-user-code
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
	@JoinColumn(name = "DOCUMENT_FISCAL_ID", insertable = false, updatable = false, nullable = false)
	@ForeignKey(name = "FK_ET_DOCFISC_DOCFISC_ID")
	@Index(name = "IDX_ET_DOCFISC_DOCFISC_ID", columnNames = "DOCUMENT_FISCAL_ID")
	public Declaration getDeclaration() {
		// begin-user-code
		return (Declaration) getDocumentFiscal();
		// end-user-code
	}

	public void setDeclaration(Declaration theDeclaration) {
		// begin-user-code
		setDocumentFiscal(theDeclaration);
		// end-user-code
	}

	@Override
	@Transient
	public java.util.Comparator<EtatDeclaration> getComparator() {
		return new Comparator();
	}

	/**
	 * Permet de trier les états d'une déclaration du plus ancien au plus récent. En cas de plusieurs états tombant le même jour, des règles
	 * de métier permettent de les départager.
	 *
	 * @author Manuel Siggen <manuel.siggen@vd.ch>
	 */
	public static class Comparator implements java.util.Comparator<EtatDeclaration> {

		@Override
		public int compare(EtatDeclaration o1, EtatDeclaration o2) {

			final RegDate dateObtention1 = o1.getDateObtention();
			final RegDate dateObtention2 = o2.getDateObtention();

			// [SIFISC-17758] dans l'écran SuperGRA, quand on ajoute à la main un état, la date d'obtention n'est pas encore assignée...
			if (dateObtention1 == null || dateObtention2 == null) {
				if (dateObtention1 != dateObtention2) {
					return dateObtention1 == null ? -1 : 1;
				}
			}

			if (dateObtention1 != dateObtention2) {
				// cas normal
				return dateObtention1.compareTo(dateObtention2);
			}

			// cas exceptionnel : deux états obtenu le même jour.
			final TypeEtatDeclaration etat1 = o1.getEtat();
			final TypeEtatDeclaration etat2 = o2.getEtat();

			// [SIFISC-17758] dans l'écran SuperGRA, les états ne sont pas toujours renseignés quand on lance la validation
			if (etat1 == null || etat2 == null) {
				if (etat1 != etat2) {
					return etat1 == null ? -1 : 1;
				}
			}

			// l'ordre est simplement l'ordre logique de l'enum
			if (etat1 != etat2) {
				return etat1.compareTo(etat2);
			}

			// s'il y a des états identiques annulés aux mêmes dates, on mets l'état annulé avant
			final boolean e1annule = o1.isAnnule();
			final boolean e2annule = o2.isAnnule();
			return e1annule == e2annule ? 0 : (e1annule ? -1 : 1);
		}
	}

}
