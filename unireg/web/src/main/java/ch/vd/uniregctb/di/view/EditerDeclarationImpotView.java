package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinaire;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;

public class EditerDeclarationImpotView {

	// Données en lecture-seule du formulaire
	private Long tiersId;
	private int periodeFiscale;
	private RegDate dateDebutPeriodeImposition;
	private RegDate dateFinPeriodeImposition;
	private String codeControle;
	private String sourceQuittancement;
	private TypeEtatDeclaration dernierEtat;
	private List<DelaiDeclarationView> delais;
	private boolean isSommable;
	/**
	 * VRAI si la DI a été sommée (même si elle est maintenant retournée ou échue)
	 */
	private boolean wasSommee;

	// Données modifiables du formulaire
	private Long id;
	private TypeDocument typeDocument;
	private RegDate dateRetour;
	@Nullable
	private Long tacheId;

	// Données dépendant des droits de l'utilisateur
	private final boolean isAllowedQuittancement = SecurityProvider.isGranted(Role.DI_QUIT_PP);
	private boolean isAllowedDelai;
	private final boolean isAllowedSommation = SecurityProvider.isGranted(Role.DI_SOM_PP);
	private final boolean isAllowedDuplicata = SecurityProvider.isGranted(Role.DI_DUPLIC_PP);

	public EditerDeclarationImpotView() {
	}

	public EditerDeclarationImpotView(DeclarationImpotOrdinaire di, @Nullable Long tacheId) {
		initReadOnlyValues(di);
		this.typeDocument = di.getTypeDeclaration();
		this.dateRetour = di.getDateRetour();
		this.tacheId = tacheId;
	}

	public void initReadOnlyValues(DeclarationImpotOrdinaire di) {
		this.tiersId = di.getTiers().getId();
		this.id = di.getId();
		this.periodeFiscale = di.getDateDebut().year();
		this.dateDebutPeriodeImposition = di.getDateDebut();
		this.dateFinPeriodeImposition = di.getDateFin();
		this.codeControle = di.getCodeControle();
		this.sourceQuittancement = initSourceQuittancement(di);
		this.dernierEtat = getDernierEtat(di);
		this.delais = initDelais(di);
		this.isAllowedDelai = initIsAllowedDelai(this.dernierEtat);
		this.isSommable = isSommable(di);
		this.wasSommee = initWasSommee(di);
	}

	private static boolean initIsAllowedDelai(TypeEtatDeclaration dernierEtat) {
		boolean d = SecurityProvider.isGranted(Role.DI_DELAI_PP);
		if (dernierEtat != TypeEtatDeclaration.EMISE) {
			d = false;
		}
		return d;
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
		final EtatDeclarationRetournee etatRourne = (EtatDeclarationRetournee) di.getEtatDeclarationActif(TypeEtatDeclaration.RETOURNEE);
		return etatRourne == null ? null : etatRourne.getSource();
	}

	private static List<DelaiDeclarationView> initDelais(DeclarationImpotOrdinaire di) {
		final Set<DelaiDeclaration> delais = di.getDelais();
		if (delais == null || delais.isEmpty()) {
			return Collections.emptyList();
		}
		final RegDate first = di.getPremierDelai();
		final List<DelaiDeclarationView> list = new ArrayList<DelaiDeclarationView>(delais.size());
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView d = new DelaiDeclarationView(delai);
			d.setFirst(d.getDelaiAccordeAu() == first);
			list.add(d);
		}

		// Trie par ordre décroissant des dates de délai
		Collections.sort(list, new Comparator<DelaiDeclarationView>() {
			@Override
			public int compare(DelaiDeclarationView o1, DelaiDeclarationView o2) {
				return o2.getDelaiAccordeAu().compareTo(o1.getDelaiAccordeAu());
			}
		});

		return list;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public void setId(Long id) {
		this.id = id;
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

	public void setTypeDocument(TypeDocument typeDocument) {
		this.typeDocument = typeDocument;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public void setDateRetour(RegDate dateRetour) {
		this.dateRetour = dateRetour;
	}

	public TypeEtatDeclaration getDernierEtat() {
		return dernierEtat;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public boolean isDepuisTache() {
		return tacheId != null;
	}

	@Nullable
	public Long getTacheId() {
		return tacheId;
	}

	public void setTacheId(@Nullable Long tacheId) {
		this.tacheId = tacheId;
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
}
