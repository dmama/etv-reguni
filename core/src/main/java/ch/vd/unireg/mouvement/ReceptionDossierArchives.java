package ch.vd.uniregctb.mouvement;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import ch.vd.uniregctb.type.Localisation;

@Entity
@DiscriminatorValue(value = "ReceptionArchives")
public class ReceptionDossierArchives extends ReceptionDossier implements ElementDeBordereau {

	private BordereauMouvementDossier bordereau;

	@Transient
	@Override
	public Localisation getLocalisation() {
		return Localisation.ARCHIVES;
	}

	@Override
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
	@JoinColumn(name = "BORDEREAU_ID", insertable = false, updatable = false, nullable = true)
	@Index(name = "IDX_MVT_DOSSIER_BORD_ID", columnNames = "BORDEREAU_ID")
	public BordereauMouvementDossier getBordereau() {
		return bordereau;
	}

	@Override
	public void setBordereau(BordereauMouvementDossier bordereau) {
		this.bordereau = bordereau;
	}
}
