package ch.vd.unireg.declaration;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.annotations.Type;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.type.ModeCommunication;
import ch.vd.unireg.type.PeriodiciteDecompte;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

/**
 * @author jec
 */
@Entity
@DiscriminatorValue("LR")
public class DeclarationImpotSource extends Declaration {

	private PeriodiciteDecompte periodicite;
	private ModeCommunication modeCommunication;
	private Boolean sansRappel;

	/**
	 * @return the periodicite
	 */
	@Column(name = "PERIODICITE", length = LengthConstants.DPI_PERIODICITE)
	@Type(type = "ch.vd.unireg.hibernate.PeriodiciteDecompteUserType")
	public PeriodiciteDecompte getPeriodicite() {
		return periodicite;
	}

	/**
	 * @param thePeriodicite the periodicite to set
	 */
	public void setPeriodicite(PeriodiciteDecompte thePeriodicite) {
		periodicite = thePeriodicite;
	}

	/**
	 * @return the modeCommunication
	 */
	@Column(name = "MODE_COM", length = LengthConstants.DPI_MODECOM)
	@Type(type = "ch.vd.unireg.hibernate.ModeCommunicationUserType")
	public ModeCommunication getModeCommunication() {
		return modeCommunication;
	}

	/**
	 * @param theModeCommunication the modeCommunication to set
	 */
	public void setModeCommunication(ModeCommunication theModeCommunication) {
		modeCommunication = theModeCommunication;
	}

	/**
	 * @return the sansRappel
	 */
	@Column(name = "SANS_RAPPEL")
	public Boolean getSansRappel() {
		return sansRappel;
	}

	/**
	 * @param theSansRappel
	 */
	public void setSansRappel(Boolean sansRappel) {
		this.sansRappel = sansRappel;
	}

	@Transient
	public RegDate getDateSommation() {
		RegDate dateSommation = null;
		GregorianCalendar calSommation = new GregorianCalendar();
		Set<EtatDeclaration> etatsDocument = getEtatsDeclaration();
		if (etatsDocument.size() == 1) {
			Iterator<EtatDeclaration> itEtat = etatsDocument.iterator();
			EtatDeclaration etat = itEtat.next();
			if (etat.getEtat() == TypeEtatDocumentFiscal.EMIS) {
				RegDate dateObtention = etat.getDateObtention();
				calSommation.setTime(dateObtention.asJavaDate());
				calSommation.add(Calendar.MONTH, 1);
				calSommation.add(Calendar.DATE, 15);
				dateSommation = RegDateHelper.get(calSommation.getTime());
			}
		}
		return dateSommation;
	}

	@Transient
	@Override
	public boolean isSommable() {
		return true;
	}

	@Transient
	@Override
	public boolean isRappelable() {
		return false;
	}
}