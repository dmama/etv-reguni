package ch.vd.uniregctb.supergra;

import ch.vd.uniregctb.common.HibernateEntity;
import ch.vd.uniregctb.tiers.DonneeCivileEntreprise;

/**
 * Les types d'entité pouvant être édités en mode SuperGra.
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum EntityType {
	Tiers(ch.vd.uniregctb.tiers.Tiers.class, "tiers", "le tiers", "du tiers"),
	ForFiscal(ch.vd.uniregctb.tiers.ForFiscal.class, "for fiscal", "le for fiscal", "du for fiscal"),
	AdresseTiers(ch.vd.uniregctb.adresse.AdresseTiers.class, "adresse", "l'adresse", "de l'adresse"),
	Declaration(ch.vd.uniregctb.declaration.Declaration.class, "déclaration", "la déclaration", "de la déclaration"),
	EtatDeclaration(ch.vd.uniregctb.declaration.EtatDeclaration.class, "état de déclaration", "l'état de la déclaration", "de l'état de la déclaration"),
	DelaiDeclaration(ch.vd.uniregctb.declaration.DelaiDeclaration.class, "délai de déclaration", "le délai de la déclaration", "du délai de la déclaration"),
	RapportEntreTiers(ch.vd.uniregctb.tiers.RapportEntreTiers.class, "rapport entre tiers", "le rapport entre tiers", "du rapport en tiers"),
	IdentificationPersonne(ch.vd.uniregctb.tiers.IdentificationPersonne.class, "identification de personne", "l'identification de personne", "de l'identification de personne"),
	IdentificationEntreprise(ch.vd.uniregctb.tiers.IdentificationEntreprise.class, "identification d'entreprise", "l'identification d'entreprise", "de l'identification d'entreprise"),
	Periodicite(ch.vd.uniregctb.declaration.Periodicite.class, "périodicité", "la périodicité", "de la périodicité"),
	SituationFamille(ch.vd.uniregctb.tiers.SituationFamille.class, "situation de famille", "la situation de famille", "de la situation de famille"),
	ModeleDocument(ch.vd.uniregctb.declaration.ModeleDocument.class, "modèle de document", "le modèle de document", "du modèle de document"),
	PeriodeFiscale(ch.vd.uniregctb.declaration.PeriodeFiscale.class, "période fiscale", "la période fiscale", "de la période fiscale"),
	Immeuble(ch.vd.uniregctb.rf.Immeuble.class, "immeuble", "l'immeuble", "de l'immeuble"),
	EtiquetteTiers(ch.vd.uniregctb.etiquette.EtiquetteTiers.class, "étiquette", "l'étiquette", "de l'étiquette"),
	Etiquette(ch.vd.uniregctb.etiquette.Etiquette.class, "étiquette", "l'étiquette", "de l'étiquette"),
	Remarque(ch.vd.uniregctb.tiers.Remarque.class, "remarque", "la remarque", "de la remarque"),
	MouvementDossier(ch.vd.uniregctb.mouvement.MouvementDossier.class, "mouvement de dossier", "le mouvement de dossier", "du mouvement de dossier"),
	DecisionAci(ch.vd.uniregctb.tiers.DecisionAci.class,"décision ACI", "la décision ACI","de la décision ACI"),
	DomicileEtablissement(ch.vd.uniregctb.tiers.DomicileEtablissement.class, "domicile", "le domicile", "du domicile"),
	RegimeFiscal(ch.vd.uniregctb.tiers.RegimeFiscal.class, "régime fiscal", "le régime fiscal", "du régime fiscal"),
	DonneeCivileEntreprise(DonneeCivileEntreprise.class, "donnée civile d'entreprise", "la donnée civile d'entreprise", "de la donnée civile d'entreprise"),
	AllegementFiscal(ch.vd.uniregctb.tiers.AllegementFiscal.class, "allègement fiscal", "l'allègement fiscal", "de l'allègement fiscal"),
	Bouclement(ch.vd.uniregctb.tiers.Bouclement.class, "bouclement", "le bouclement", "du bouclement"),
	EtatEntreprise(ch.vd.uniregctb.tiers.EtatEntreprise.class, "état", "l'état", "de l'état"),
	FlagEntreprise(ch.vd.uniregctb.tiers.FlagEntreprise.class, "flag entreprise", "le flag entreprise", "du flag entreprise"),
	AdresseMandataire(ch.vd.uniregctb.adresse.AdresseMandataire.class, "adresse mandataire", "l'adresse mandataire", "de l'adresse mandataire"),
	AutreDocumentFiscal(ch.vd.uniregctb.documentfiscal.AutreDocumentFiscal.class, "document fiscal", "le document fiscal", "du document fiscal"),
	AyantDroitRF(ch.vd.uniregctb.registrefoncier.AyantDroitRF.class, "ayant-droit", "l'ayant-droit", "de l'ayant-droit"),
	DroitRF(ch.vd.uniregctb.registrefoncier.DroitRF.class, "droit", "le droit", "du droit"),
	ImmeubleRF(ch.vd.uniregctb.registrefoncier.ImmeubleRF.class, "immeuble", "l'immeuble", "de l'immeuble"),
	CommuneRF(ch.vd.uniregctb.registrefoncier.CommuneRF.class, "commune", "la commune", "de la commune"),
	SituationRF(ch.vd.uniregctb.registrefoncier.SituationRF.class, "situation", "la situation", "de la situation"),
	SurfaceTotaleRF(ch.vd.uniregctb.registrefoncier.SurfaceTotaleRF.class, "surface totale", "la surface totale", "de la surface totale"),
	EstimationRF(ch.vd.uniregctb.registrefoncier.EstimationRF.class, "estimation fiscale", "l'estimation fiscale", "de l'estimation fiscale"),
	SurfaceAuSolRF(ch.vd.uniregctb.registrefoncier.SurfaceAuSolRF.class, "surface au sol", "la surface au sol", "de la surface au sol"),
	BatimentRF(ch.vd.uniregctb.registrefoncier.BatimentRF.class, "bâtiment", "le bâtiment", "du bâtiment"),
	ImplantationRF(ch.vd.uniregctb.registrefoncier.ImplantationRF.class, "implantation", "l'implantation", "de l'implantation"),
	DescriptionBatimentRF(ch.vd.uniregctb.registrefoncier.DescriptionBatimentRF.class, "description du bâtiment", "la description du bâtiment", "de la description du bâtiment");

	private final Class<? extends HibernateEntity> hibernateClass;
	private final String displayName;   // "chat"
	private final String displayArticleName; // "le chat"
	private final String displayPrepositionName; // "du chat"

	EntityType(Class<? extends HibernateEntity> hibernateClass, String displayName, String displayArticleName, String displayPrepositionName) {
		this.hibernateClass = hibernateClass;
		this.displayName = displayName;
		this.displayArticleName = displayArticleName;
		this.displayPrepositionName = displayPrepositionName;
	}

	public Class<? extends HibernateEntity> getHibernateClass() {
		return hibernateClass;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getDisplayArticleName() {
		return displayArticleName;
	}

	public String getDisplayPrepositionName() {
		return displayPrepositionName;
	}

	public static EntityType fromHibernateClass(Class<?> clazz) {
		for (EntityType e : EntityType.values()) {
			if (e.getHibernateClass() == clazz || e.getHibernateClass().isAssignableFrom(clazz)) {
				return e;
			}
		}
		return null;
	}
}
