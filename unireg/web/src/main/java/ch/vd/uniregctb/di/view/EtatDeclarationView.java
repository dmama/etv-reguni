package ch.vd.uniregctb.di.view;

import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

public class EtatDeclarationView implements Comparable<EtatDeclarationView> {

	private Long id;
	private RegDate dateObtention;
	private boolean annule;
	private TypeEtatDeclaration etat;
	private String etatMessage;

	/**
	 * La source de quittancement dans le cas ou #etat == "RETOURNEE".
	 */
	private String source;
	private String sourceMessage;

	/**
	 * La date d'envoi de la sommation dans le cas ou #etat == 'SOMMEE';
	 */
	private RegDate dateEnvoiCourrier;
	private String dateEnvoiCourrierMessage;

	public EtatDeclarationView(EtatDeclaration etat, MessageSource messageSource) {
		this.id = etat.getId();
		this.dateObtention = etat.getDateObtention();
		this.annule = etat.isAnnule();
		this.etat = etat.getEtat();
		this.etatMessage = messageSource.getMessage("option.etat.avancement." + this.etat.name(), null, WebContextUtils.getDefaultLocale());

		if (etat instanceof EtatDeclarationRetournee) {
			this.source = ((EtatDeclarationRetournee) etat).getSource();
			if (this.source == null) {
				this.sourceMessage = messageSource.getMessage("option.source.quittancement.UNKNOWN", null, WebContextUtils.getDefaultLocale());
			}
			else {
				this.sourceMessage = messageSource.getMessage("option.source.quittancement." + this.source, null, WebContextUtils.getDefaultLocale());
			}
		}
		if (etat instanceof EtatDeclarationSommee) {
			this.dateEnvoiCourrier = ((EtatDeclarationSommee) etat).getDateEnvoiCourrier();
			this.dateEnvoiCourrierMessage =
					messageSource.getMessage("label.date.envoi.courrier", new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier)}, WebContextUtils.getDefaultLocale());
		}
	}

	public Long getId() {
		return id;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	public boolean isAnnule() {
		return annule;
	}

	public TypeEtatDeclaration getEtat() {
		return etat;
	}

	public String getEtatMessage() {
		return etatMessage;
	}

	public String getSource() {
		return source;
	}

	public String getSourceMessage() {
		return sourceMessage;
	}

	public RegDate getDateEnvoiCourrier() {
		return dateEnvoiCourrier;
	}

	public String getDateEnvoiCourrierMessage() {
		return dateEnvoiCourrierMessage;
	}

	@Override
	public int compareTo(EtatDeclarationView o) {
		// du plus récent au plus ancient
		return -1 * dateObtention.compareTo(o.dateObtention);
	}
}
