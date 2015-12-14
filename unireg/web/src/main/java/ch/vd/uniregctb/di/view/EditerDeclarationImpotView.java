package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePM;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EditerDeclarationImpotView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private String codeControle;
	private String sourceQuittancement;
	private TypeEtatDeclaration dernierEtat;
	private List<DelaiDeclarationView> delais;
	private List<EtatDeclarationView> etats;
	private boolean isSommable;
	/**
	 * VRAI si la DI a été sommée (même si elle est maintenant retournée ou échue)
	 */
	private boolean wasSommee;
	@Nullable
	private Long tacheId;

	// Données dépendant des droits de l'utilisateur
	private boolean isAllowedQuittancement;
	private boolean isAllowedDelai;
	private boolean isAllowedSommation;
	private boolean isAllowedDuplicata;

	private boolean isDiPP;
	private boolean isDiPM;

	public EditerDeclarationImpotView() {
	}

	public EditerDeclarationImpotView(DeclarationImpotOrdinaire di, @Nullable Long tacheId, MessageSource messageSource, boolean allowedQuittancement, boolean allowedDelai, boolean allowedSommation,
	                                  boolean allowedDuplicata) {
		initReadOnlyValues(di, messageSource, allowedQuittancement, allowedDelai, allowedSommation, allowedDuplicata);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
		this.tacheId = tacheId;
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di, MessageSource messageSource, boolean allowedQuittancement, boolean allowedDelai, boolean allowedSommation, boolean allowedDuplicata) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateDebut().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.codeControle = di.getCodeControle();
		this.sourceQuittancement = initSourceQuittancement(di);
		this.dernierEtat = getDernierEtat(di);
		this.delais = initDelais(di, messageSource);
		this.etats = initEtats(di.getEtats(), messageSource);
		this.isAllowedQuittancement = allowedQuittancement;
		this.isAllowedDelai = allowedDelai;
		this.isAllowedSommation = allowedSommation;
		this.isAllowedDuplicata = allowedDuplicata;
		this.isSommable = isSommable(di);
		this.wasSommee = initWasSommee(di);
		this.isDiPP = di instanceof DeclarationImpotOrdinairePP;
		this.isDiPM = di instanceof DeclarationImpotOrdinairePM;
	}

	private static boolean initWasSommee(DeclarationImpotOrdinaire di) {
		boolean wasSommee = false;
		for (EtatDeclaration etat : di.getEtats()) {
			if (!etat.isAnnule() && etat.getEtat() == TypeEtatDeclaration.SOMMEE) {
				wasSommee = true;
				break;
			}
		}
		return wasSommee;
	}

	public static boolean isSommable(DeclarationImpotOrdinaire di) {
		final TypeEtatDeclaration dernierEtat = getDernierEtat(di);
		boolean isSommable = false;
		if (dernierEtat == TypeEtatDeclaration.EMISE) {
			if (di.getDelaiAccordeAu() == null || RegDate.get().isAfter(di.getDelaiAccordeAu())) {
				isSommable = true;
			}
		}
		return isSommable;
	}

	public static TypeEtatDeclaration getDernierEtat(DeclarationImpotOrdinaire di) {
		final EtatDeclaration etatDI = di.getDernierEtat();
		return etatDI == null ? null : etatDI.getEtat();
	}

	private static String initSourceQuittancement(DeclarationImpotOrdinaire di) {
		final EtatDeclarationRetournee etatRourne = (EtatDeclarationRetournee) di.getDernierEtatOfType(TypeEtatDeclaration.RETOURNEE);
		return etatRourne == null ? null : etatRourne.getSource();
	}

	private static List<DelaiDeclarationView> initDelais(DeclarationImpotOrdinaire di, MessageSource messageSource) {
		final Set<DelaiDeclaration> delais = di.getDelais();
		if (delais == null || delais.isEmpty()) {
			return Collections.emptyList();
		}
		final RegDate first = di.getPremierDelai();
		final List<DelaiDeclarationView> list = new ArrayList<>(delais.size());
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView d = new DelaiDeclarationView(delai, messageSource);
			d.setFirst(d.getDelaiAccordeAu() == first);
			list.add(d);
		}

		// Trie par ordre décroissant des dates de traitement (pour tenir compte des délais non-accordés)
		Collections.sort(list, new Comparator<DelaiDeclarationView>() {
			@Override
			public int compare(DelaiDeclarationView o1, DelaiDeclarationView o2) {
				int comparison = o2.getDateTraitement().compareTo(o1.getDateTraitement());
				if (comparison == 0) {
					comparison = Long.compare(o2.getId(), o1.getId());
				}
				return comparison;
			}
		});

		return list;
	}

	private static List<EtatDeclarationView> initEtats(Set<EtatDeclaration> etats, MessageSource messageSource) {
		final List<EtatDeclarationView> list = new ArrayList<>();
		for (EtatDeclaration etat : etats) {
			list.add(new EtatDeclarationView(etat, messageSource));
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

	public TypeEtatDeclaration getDernierEtat() {
		return dernierEtat;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public List<EtatDeclarationView> getEtats() {
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
