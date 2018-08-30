package ch.vd.unireg.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.declaration.DeclarationImpotOrdinaire;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePM;
import ch.vd.unireg.declaration.DeclarationImpotOrdinairePP;
import ch.vd.unireg.declaration.DelaiDeclaration;
import ch.vd.unireg.declaration.EtatDeclaration;
import ch.vd.unireg.declaration.EtatDeclarationRetournee;
import ch.vd.unireg.declaration.view.DelaiDocumentFiscalView;
import ch.vd.unireg.declaration.view.EtatDocumentFiscalView;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;
import ch.vd.unireg.message.MessageHelper;
import ch.vd.unireg.type.EtatDelaiDocumentFiscal;
import ch.vd.unireg.type.TypeDocument;
import ch.vd.unireg.type.TypeEtatDocumentFiscal;

public class EditerDeclarationImpotView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private RegDate dateDebutExerciceCommercial;
	private RegDate dateFinExerciceCommercial;
	private String codeControle;
	private String sourceQuittancement;
	private TypeEtatDocumentFiscal dernierEtat;
	private List<DelaiDocumentFiscalView> delais;
	private List<EtatDocumentFiscalView> etats;
	private boolean isSommable;
	/**
	 * VRAI si la DI a été sommée (même si elle est maintenant retournée ou échue)
	 */
	private boolean wasSommee;
	@Nullable
	private Long tacheId;

	// Données dépendant des droits de l'utilisateur (et de l'état de la DI, évidemment, aussi)
	private boolean isAllowedQuittancement;
	private boolean isAllowedDelai;
	private boolean isAllowedSommation;
	private boolean isAllowedDuplicata;
	private boolean isAllowedSuspension;
	private boolean isAllowedAnnulationSuspension;
	private boolean isAllowedLiberation;

	private boolean isDiPP;
	private boolean isDiPM;

	public EditerDeclarationImpotView() {
	}

	public EditerDeclarationImpotView(DeclarationImpotOrdinaire di, @Nullable Long tacheId, ServiceInfrastructureService infraService, MessageHelper messageHelper,
	                                  boolean allowedQuittancement, boolean allowedDelai, boolean allowedSommation,
	                                  boolean allowedDuplicata, boolean allowedSuspension, boolean allowedAnnulationSuspension, boolean allowedLiberation) {
		initReadOnlyValues(di, infraService, messageHelper, allowedQuittancement, allowedDelai, allowedSommation, allowedDuplicata, allowedSuspension, allowedAnnulationSuspension, allowedLiberation);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
		this.tacheId = tacheId;
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di, ServiceInfrastructureService infraService, MessageHelper messageHelper,
	                               boolean allowedQuittancement, boolean allowedDelai, boolean allowedSommation, boolean allowedDuplicata, boolean allowedSuspension, boolean allowedAnnulationSuspension, boolean allowedLiberation) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateFin().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.codeControle = di.getCodeControle();
		this.sourceQuittancement = initSourceQuittancement(di);
		this.dernierEtat = getDernierEtat(di);
		this.delais = initDelais(di, infraService, messageHelper);
		this.etats = initEtats(di.getEtatsDeclaration(), infraService, messageHelper);
		this.isAllowedQuittancement = allowedQuittancement;
		this.isAllowedDelai = allowedDelai;
		this.isAllowedSommation = allowedSommation;
		this.isAllowedDuplicata = allowedDuplicata;
		this.isAllowedSuspension = allowedSuspension;
		this.isAllowedAnnulationSuspension = allowedAnnulationSuspension;
		this.isAllowedLiberation = allowedLiberation;
		this.isSommable = isSommable(di);
		this.wasSommee = initWasSommee(di);
		this.isDiPP = di instanceof DeclarationImpotOrdinairePP;
		this.isDiPM = di instanceof DeclarationImpotOrdinairePM;

		if (di instanceof DeclarationImpotOrdinairePM) {
			this.dateDebutExerciceCommercial = ((DeclarationImpotOrdinairePM) di).getDateDebutExerciceCommercial();
			this.dateFinExerciceCommercial = ((DeclarationImpotOrdinairePM) di).getDateFinExerciceCommercial();
		}
	}

	private static boolean initWasSommee(DeclarationImpotOrdinaire di) {
		boolean wasSommee = false;
		for (EtatDeclaration etat : di.getEtatsDeclaration()) {
			if (!etat.isAnnule() && etat.getEtat() == TypeEtatDocumentFiscal.SOMME) {
				wasSommee = true;
				break;
			}
		}
		return wasSommee;
	}

	public static boolean isSommable(DeclarationImpotOrdinaire di) {
		final TypeEtatDocumentFiscal dernierEtat = getDernierEtat(di);
		boolean isSommable = false;
		if (dernierEtat == TypeEtatDocumentFiscal.EMIS) {
			if (di.getDelaiAccordeAu() == null || RegDate.get().isAfter(di.getDelaiAccordeAu())) {
				isSommable = true;
			}
		}
		return isSommable;
	}

	public static boolean isSuspendable(DeclarationImpotOrdinaire di) {
		// seules les DI PM ont cette capacité
		if (di instanceof DeclarationImpotOrdinairePM) {
			final TypeEtatDocumentFiscal dernierEtat = getDernierEtat(di);
			return dernierEtat != TypeEtatDocumentFiscal.RETOURNE && dernierEtat != TypeEtatDocumentFiscal.SUSPENDU;
		}

		return false;
	}

	public static TypeEtatDocumentFiscal getDernierEtat(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etatDI = di.getDernierEtatDeclaration();
		return etatDI == null ? null : etatDI.getEtat();
	}

	private static String initSourceQuittancement(DeclarationImpotOrdinaire di) {
		final EtatDeclarationRetournee etatRourne = (EtatDeclarationRetournee) di.getDernierEtatDeclarationOfType(TypeEtatDocumentFiscal.RETOURNE);
		return etatRourne == null ? null : etatRourne.getSource();
	}

	private static List<DelaiDocumentFiscalView> initDelais(DeclarationImpotOrdinaire di, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final Set<DelaiDeclaration> delais = di.getDelaisDeclaration();
		if (delais == null || delais.isEmpty()) {
			return Collections.emptyList();
		}
		final RegDate first = di.getPremierDelai();
		final List<DelaiDocumentFiscalView> list = new ArrayList<>(delais.size());
		for (DelaiDeclaration delai : delais) {
			final DelaiDocumentFiscalView d = new DelaiDocumentFiscalView(delai, infraService);
			d.setFirst(d.getDelaiAccordeAu() == first);
			list.add(d);
		}

		// Trie par ordre décroissant des dates de traitement (pour tenir compte des délais non-accordés)
		list.sort(Comparator.comparing(DelaiDocumentFiscalView::getDateTraitement)
				          .thenComparingLong(DelaiDocumentFiscalView::getId)
				          .reversed());

		// calcul du flag "lastOfState" pour le dernier délai à chaque état
		list.stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toMap(DelaiDocumentFiscalView::getEtat,
				                          Function.identity(),
				                          (dd1, dd2) -> dd1,            // selon le tri ci-dessus, le premier vu est le dernier valide...
				                          () -> new EnumMap<>(EtatDelaiDocumentFiscal.class)))
				.forEach((etat, dd) -> dd.setLastOfState(true));

		return list;
	}

	private static List<EtatDocumentFiscalView> initEtats(Set<EtatDeclaration> etats, ServiceInfrastructureService infraService, MessageHelper messageHelper) {
		final List<EtatDocumentFiscalView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDocumentFiscalView(etat, infraService, messageHelper));
		}
		Collections.sort(list);
		return list;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public Long getId() {
		return id;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebutPeriodeImposition() {
		return dateDebutPeriodeImposition;
	}

	public RegDate getDateFinPeriodeImposition() {
		return dateFinPeriodeImposition;
	}

	public RegDate getDateDebutExerciceCommercial() {
		return dateDebutExerciceCommercial;
	}

	public RegDate getDateFinExerciceCommercial() {
		return dateFinExerciceCommercial;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public String getSourceQuittancement() {
		return sourceQuittancement;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public TypeEtatDocumentFiscal getDernierEtat() {
		return dernierEtat;
	}

	public List<DelaiDocumentFiscalView> getDelais() {
		return delais;
	}

	public List<EtatDocumentFiscalView> getEtats() {
		return etats;
	}

	public boolean isDepuisTache() {
		return tacheId != null;
	}

	@Nullable
	public Long getTacheId() {
		return tacheId;
	}

	public boolean isAllowedQuittancement() {
		return isAllowedQuittancement;
	}

	public boolean isAllowedDelai() {
		return isAllowedDelai;
	}

	public boolean isAllowedSommation() {
		return isAllowedSommation;
	}

	public boolean isAllowedDuplicata() {
		return isAllowedDuplicata;
	}

	public boolean isAllowedSuspension() {
		return isAllowedSuspension;
	}

	public boolean isAllowedAnnulationSuspension() {
		return isAllowedAnnulationSuspension;
	}

	public boolean isAllowedLiberation() {
		return isAllowedLiberation;
	}

	public boolean isSommable() {
		return isSommable;
	}

	public boolean isWasSommee() {
		return wasSommee;
	}

	public boolean isDiPP() {
		return isDiPP;
	}

	public boolean isDiPM() {
		return isDiPM;
	}

	public boolean isAllowedDuplicataDirect() {
		return isDiPM && isAllowedDuplicata;
	}
}
