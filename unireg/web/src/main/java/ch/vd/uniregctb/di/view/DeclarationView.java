package ch.vd.uniregctb.di.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.DeclarationImpotOrdinairePP;
import ch.vd.uniregctb.declaration.DelaiDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.type.TypeDocument;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

/**
 * Vue d'une déclaration d'impôt (ordinaire ou source).
 */
@SuppressWarnings("UnusedDeclaration")
public class DeclarationView implements Annulable {

	private long id;
	private Long tiersId;
	private String codeControle;
	private int periodeFiscale;
	private RegDate dateDebut;
	private RegDate dateFin;
	private RegDate delaiAccorde;
	private RegDate dateRetour;
	private TypeEtatDeclaration etat;
	private String sourceRetour;
	private boolean annule;
	private TypeDocument typeDocument;
	private String typeDocumentMessage;
	private List<DelaiDeclarationView> delais;
	private List<EtatDeclarationView> etats;

	public DeclarationView(Declaration decl, MessageSource messageSource) {
		this.id = decl.getId();
		this.tiersId = decl.getTiers().getId();
		this.periodeFiscale = decl.getPeriode().getAnnee();
		this.dateDebut = decl.getDateDebut();
		this.dateFin = decl.getDateFin();
		this.delaiAccorde = decl.getDelaiAccordeAu();
		this.dateRetour = decl.getDateRetour();

		final EtatDeclaration etat = decl.getDernierEtat();
		this.etat = (etat == null ? null : etat.getEtat());
		if (etat instanceof EtatDeclarationRetournee) {
			this.sourceRetour = ((EtatDeclarationRetournee) etat).getSource();
		}

		this.annule = decl.isAnnule();
		if (decl instanceof DeclarationImpotOrdinairePP) {
			final DeclarationImpotOrdinairePP di = (DeclarationImpotOrdinairePP) decl;
			this.codeControle = di.getCodeControle();
			this.typeDocument = di.getTypeDeclaration();
			if (this.typeDocument != null) {
				this.typeDocumentMessage = messageSource.getMessage("option.type.document." + this.typeDocument.name(), null, WebContextUtils.getDefaultLocale());
			}
		}

		this.delais = initDelais(decl.getDelais(), decl.getPremierDelai());
		this.etats = initEtats(decl.getEtats(), messageSource);
	}

	private static List<DelaiDeclarationView> initDelais(Set<DelaiDeclaration> delais, RegDate premierDelai) {
		final List<DelaiDeclarationView> list = new ArrayList<>();
		for (DelaiDeclaration delai : delais) {
			final DelaiDeclarationView delaiView = new DelaiDeclarationView(delai);
			delaiView.setFirst(premierDelai == delai.getDelaiAccordeAu());
			list.add(delaiView);
		}
		Collections.sort(list);
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

	public long getId() {
		return id;
	}

	public Long getTiersId() {
		return tiersId;
	}

	public String getCodeControle() {
		return codeControle;
	}

	public int getPeriodeFiscale() {
		return periodeFiscale;
	}

	public RegDate getDateDebut() {
		return dateDebut;
	}

	public RegDate getDateFin() {
		return dateFin;
	}

	public TypeDocument getTypeDocument() {
		return typeDocument;
	}

	public String getTypeDocumentMessage() {
		return typeDocumentMessage;
	}

	public List<DelaiDeclarationView> getDelais() {
		return delais;
	}

	public List<EtatDeclarationView> getEtats() {
		return etats;
	}

	public RegDate getDelaiAccorde() {
		return delaiAccorde;
	}

	public RegDate getDateRetour() {
		return dateRetour;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public String getSourceRetour() {
		return sourceRetour;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}
}
