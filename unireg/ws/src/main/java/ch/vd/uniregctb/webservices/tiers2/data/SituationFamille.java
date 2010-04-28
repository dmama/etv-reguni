package ch.vd.uniregctb.webservices.tiers2.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import ch.vd.uniregctb.webservices.tiers2.impl.DataHelper;
import ch.vd.uniregctb.webservices.tiers2.impl.EnumHelper;

/**
 * Contient la situation familliale d'un contribuable <b>d'un point de vue fiscal</b>.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SituationFamille", propOrder = {
		"id", "dateDebut", "dateFin", "dateAnnulation", "etatCivil", "nombreEnfants", "tarifApplicable", "numeroContribuablePrincipal"
})
public class SituationFamille {

	/** L'id technique (= clé primaire) */
	@XmlElement(required = true)
	public Long id;

	@XmlElement(required = true)
	public Date dateDebut;

	@XmlElement(required = false)
	public Date dateFin;

	/** Date à laquelle la situation de famille a été annulée, ou <b>null</b> si elle n'est pas annulée. */
	@XmlElement(required = false)
	public Date dateAnnulation;

	/**
	 * Etat civil admis par l’ACI et imprimé sur la déclaration d’impôt, qui peut différer de celui connu par le contrôle des habitants.
	 */
	@XmlElement(required = true)
	public EtatCivil etatCivil;

	/** Le nombre d'enfants à charge du contribuable, ou <b>null</b> si cette information n'est pas connue ou inapplicable. */
	@XmlElement(required = false)
	public Integer nombreEnfants;

	/**
	 * Tarif impôt source applicable.
	 * <p>
	 * Ce champ est renseigné uniquement dans le cas où le contribuable associé est:
	 * <ul>
	 * <li>un ménage commun</li>
	 * <li>composé uniquement de sourciers</li>
	 * </ul>
	 * Il est null dans tous les autres cas.
	 */
	@XmlElement(required = false)
	public TarifImpotSource tarifApplicable;

	/**
	 * Numéro du contribuable principal d'un couple sourcier.
	 * <p>
	 * Ce champ est renseigné uniquement dans le cas où le contribuable associé est:
	 * <ul>
	 * <li>un ménage commun</li>
	 * <li>composé uniquement de sourciers</li>
	 * </ul>
	 * Il est null dans tous les autres cas.
	 */
	@XmlElement(required = false)
	public Long numeroContribuablePrincipal;

	public SituationFamille() {
	}

	public SituationFamille(ch.vd.uniregctb.situationfamille.VueSituationFamille situation) {
		this.id = situation.getId();
		this.dateDebut = DataHelper.coreToWeb(situation.getDateDebut());
		this.dateFin = DataHelper.coreToWeb(situation.getDateFin());
		this.dateAnnulation = DataHelper.coreToWeb(situation.getAnnulationDate());
		this.nombreEnfants = situation.getNombreEnfants();

		if (situation instanceof ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) {
			final ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun situtationMenage = (ch.vd.uniregctb.situationfamille.VueSituationFamilleMenageCommun) situation;

			this.tarifApplicable = EnumHelper.coreToWeb(situtationMenage.getTarifApplicable());
			this.numeroContribuablePrincipal = situtationMenage.getNumeroContribuablePrincipal();
		}
		else if (situation instanceof ch.vd.uniregctb.situationfamille.VueSituationFamillePersonnePhysique) {
			ch.vd.uniregctb.situationfamille.VueSituationFamillePersonnePhysique situationsPersonne = (ch.vd.uniregctb.situationfamille.VueSituationFamillePersonnePhysique) situation;

			this.etatCivil = EnumHelper.coreToWeb(situationsPersonne.getEtatCivil());
		}
	}
}
