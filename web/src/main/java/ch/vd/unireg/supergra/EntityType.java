package ch.vd.unireg.supergra;

import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.documentfiscal.DelaiDocumentFiscal;
import ch.vd.unireg.documentfiscal.DocumentFiscal;
import ch.vd.unireg.documentfiscal.EtatDocumentFiscal;
import ch.vd.unireg.documentfiscal.LiberationDocumentFiscal;
import ch.vd.unireg.tiers.CoordonneesFinancieres;
import ch.vd.unireg.tiers.DonneeCivileEntreprise;

/**
 * Les types d'entité pouvant être édités en mode SuperGra.
 */
@SuppressWarnings({"UnusedDeclaration"})
public enum EntityType {
	Tiers(ch.vd.unireg.tiers.Tiers.class, "tiers", "le tiers", "du tiers"),
	ForFiscal(ch.vd.unireg.tiers.ForFiscal.class, "for fiscal", "le for fiscal", "du for fiscal"),
	AdresseTiers(ch.vd.unireg.adresse.AdresseTiers.class, "adresse", "l'adresse", "de l'adresse"),
	CoordonneesFinancieres(CoordonneesFinancieres.class, "coordonnées financières", "les coordonnées financières", "des coordonnées financières"),
	DocumentFiscal(DocumentFiscal.class, "document fiscal", "le document fiscal", "du document fiscal"),
	EtatDocument(EtatDocumentFiscal.class, "état du document", "l'état du document", "de l'état du document"),
	DelaiDocument(DelaiDocumentFiscal.class, "délai du document", "le délai du document", "du délai du document"),
	LiberationDocument(LiberationDocumentFiscal.class, "libération du document", "libération du document", "de la libération du document"),
	RapportEntreTiers(ch.vd.unireg.tiers.RapportEntreTiers.class, "rapport entre tiers", "le rapport entre tiers", "du rapport en tiers"),
	IdentificationPersonne(ch.vd.unireg.tiers.IdentificationPersonne.class, "identification de personne", "l'identification de personne", "de l'identification de personne"),
	IdentificationEntreprise(ch.vd.unireg.tiers.IdentificationEntreprise.class, "identification d'entreprise", "l'identification d'entreprise", "de l'identification d'entreprise"),
	Periodicite(ch.vd.unireg.declaration.Periodicite.class, "périodicité", "la périodicité", "de la périodicité"),
	SituationFamille(ch.vd.unireg.tiers.SituationFamille.class, "situation de famille", "la situation de famille", "de la situation de famille"),
	ModeleDocument(ch.vd.unireg.declaration.ModeleDocument.class, "modèle de document", "le modèle de document", "du modèle de document"),
	PeriodeFiscale(ch.vd.unireg.declaration.PeriodeFiscale.class, "période fiscale", "la période fiscale", "de la période fiscale"),
	EtiquetteTiers(ch.vd.unireg.etiquette.EtiquetteTiers.class, "étiquette", "l'étiquette", "de l'étiquette"),
	Etiquette(ch.vd.unireg.etiquette.Etiquette.class, "étiquette", "l'étiquette", "de l'étiquette"),
	Remarque(ch.vd.unireg.tiers.Remarque.class, "remarque", "la remarque", "de la remarque"),
	MouvementDossier(ch.vd.unireg.mouvement.MouvementDossier.class, "mouvement de dossier", "le mouvement de dossier", "du mouvement de dossier"),
	DecisionAci(ch.vd.unireg.tiers.DecisionAci.class,"décision ACI", "la décision ACI","de la décision ACI"),
	DomicileEtablissement(ch.vd.unireg.tiers.DomicileEtablissement.class, "domicile", "le domicile", "du domicile"),
	RegimeFiscal(ch.vd.unireg.tiers.RegimeFiscal.class, "régime fiscal", "le régime fiscal", "du régime fiscal"),
	DonneeCivileEntreprise(DonneeCivileEntreprise.class, "donnée civile d'entreprise", "la donnée civile d'entreprise", "de la donnée civile d'entreprise"),
	AllegementFiscal(ch.vd.unireg.tiers.AllegementFiscal.class, "allègement fiscal", "l'allègement fiscal", "de l'allègement fiscal"),
	Bouclement(ch.vd.unireg.tiers.Bouclement.class, "bouclement", "le bouclement", "du bouclement"),
	EtatEntreprise(ch.vd.unireg.tiers.EtatEntreprise.class, "état", "l'état", "de l'état"),
	FlagEntreprise(ch.vd.unireg.tiers.FlagEntreprise.class, "flag entreprise", "le flag entreprise", "du flag entreprise"),
	AdresseMandataire(ch.vd.unireg.adresse.AdresseMandataire.class, "adresse mandataire", "l'adresse mandataire", "de l'adresse mandataire"),
	AyantDroitRF(ch.vd.unireg.registrefoncier.AyantDroitRF.class, "ayant-droit", "l'ayant-droit", "de l'ayant-droit"),
	DroitRF(ch.vd.unireg.registrefoncier.DroitRF.class, "droit", "le droit", "du droit"),
	ImmeubleRF(ch.vd.unireg.registrefoncier.ImmeubleRF.class, "immeuble", "l'immeuble", "de l'immeuble"),
	RaisonAcquisitionRF(ch.vd.unireg.registrefoncier.RaisonAcquisitionRF.class, "raison d'acquisition", "la raison d'acquisition", "de la raison d'acquisition"),
	QuotePartRF(ch.vd.unireg.registrefoncier.QuotePartRF.class, "quote-part", "la quote-part", "de la quote-part"),
	CommuneRF(ch.vd.unireg.registrefoncier.CommuneRF.class, "commune", "la commune", "de la commune"),
	SituationRF(ch.vd.unireg.registrefoncier.SituationRF.class, "situation", "la situation", "de la situation"),
	SurfaceTotaleRF(ch.vd.unireg.registrefoncier.SurfaceTotaleRF.class, "surface totale", "la surface totale", "de la surface totale"),
	EstimationRF(ch.vd.unireg.registrefoncier.EstimationRF.class, "estimation fiscale", "l'estimation fiscale", "de l'estimation fiscale"),
	SurfaceAuSolRF(ch.vd.unireg.registrefoncier.SurfaceAuSolRF.class, "surface au sol", "la surface au sol", "de la surface au sol"),
	BatimentRF(ch.vd.unireg.registrefoncier.BatimentRF.class, "bâtiment", "le bâtiment", "du bâtiment"),
	ImplantationRF(ch.vd.unireg.registrefoncier.ImplantationRF.class, "implantation", "l'implantation", "de l'implantation"),
	DescriptionBatimentRF(ch.vd.unireg.registrefoncier.DescriptionBatimentRF.class, "description du bâtiment", "la description du bâtiment", "de la description du bâtiment"),
	RapprochementRF(ch.vd.unireg.registrefoncier.RapprochementRF.class, "rapprochement", "le rapprochement", "du rapprochement"),
	RegroupementRF(ch.vd.unireg.registrefoncier.RegroupementCommunauteRF.class, "regroupement", "le regroupement", "du regroupement"),
	ModeleCommunauteRF(ch.vd.unireg.registrefoncier.ModeleCommunauteRF.class, "modèle de communauté", "le modèle de communauté", "du modèle de communauté"),
	PrincipalCommunauteRF(ch.vd.unireg.registrefoncier.PrincipalCommunauteRF.class, "principal de communauté", "le principal de communauté", "du principal de communauté"),
	AllegementFoncier(ch.vd.unireg.foncier.AllegementFoncier.class, "allègement foncier", "l'allègement foncier", "de l'allègement foncier"),
	BeneficeServitude(ch.vd.unireg.registrefoncier.BeneficeServitudeRF.class, "bénéfice de servitude", "le bénéfice de servitude", "du bénéfice de servitude"),
	ChargeServitude(ch.vd.unireg.registrefoncier.ChargeServitudeRF.class, "charge de servitude", "la charge de servitude", "de la charge de servitude");

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
