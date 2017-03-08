package ch.vd.uniregctb.declaration.view;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.declaration.Declaration;
import ch.vd.uniregctb.declaration.EtatDeclaration;
import ch.vd.uniregctb.declaration.EtatDeclarationAvecDocumentArchive;
import ch.vd.uniregctb.declaration.EtatDeclarationRappelee;
import ch.vd.uniregctb.declaration.EtatDeclarationRetournee;
import ch.vd.uniregctb.declaration.EtatDeclarationSommee;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.type.TypeEtatDeclaration;
import ch.vd.uniregctb.utils.WebContextUtils;

public class EtatDeclarationView implements Comparable<EtatDeclarationView>, Annulable {

	private Long id;
	private RegDate dateObtention;
	private Date logCreationDate;
	private boolean annule;
	private TypeEtatDeclaration etat;
	private String etatMessage;

	/**
	 * La source de quittancement dans le cas ou #etat == "RETOURNEE".
	 */
	private String source;
	private String sourceMessage;

	/**
	 * La date d'envoi de la sommation dans le cas ou #etat == 'SOMMEE' ou #etat == 'RAPPELEE'
	 */
	private RegDate dateEnvoiCourrier;
	private String dateEnvoiCourrierMessage;

	/**
	 * L'url de visualisation externe du document (s'il y en a un, bien-sûr)
	 */
	private String urlVisualisationExterneDocument;

	public EtatDeclarationView(EtatDeclaration etat, ServiceInfrastructureService infraService, MessageSource messageSource) {
		this.id = etat.getId();
		this.dateObtention = etat.getDateObtention();
		this.logCreationDate = etat.getLogCreationDate();
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
			final EtatDeclarationSommee sommation = (EtatDeclarationSommee) etat;
			this.dateEnvoiCourrier = sommation.getDateEnvoiCourrier();
			if (sommation.getEmolument() != null) {
				this.dateEnvoiCourrierMessage = messageSource.getMessage("label.date.envoi.courrier.avec.emolument",
				                                                         new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier), Integer.toString(sommation.getEmolument())},
				                                                         WebContextUtils.getDefaultLocale());
			}
			else {
				this.dateEnvoiCourrierMessage = messageSource.getMessage("label.date.envoi.courrier",
				                                                         new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier)},
				                                                         WebContextUtils.getDefaultLocale());
			}
		}
		if (etat instanceof EtatDeclarationRappelee) {
			this.dateEnvoiCourrier = ((EtatDeclarationRappelee) etat).getDateEnvoiCourrier();
			this.dateEnvoiCourrierMessage = messageSource.getMessage("label.date.envoi.courrier",
			                                                         new Object[]{RegDateHelper.dateToDisplayString(this.dateEnvoiCourrier)},
			                                                         WebContextUtils.getDefaultLocale());
		}
		if (etat instanceof EtatDeclarationAvecDocumentArchive) {
			final EtatDeclarationAvecDocumentArchive etatArchive = (EtatDeclarationAvecDocumentArchive) etat;
			final Declaration declaration = etat.getDeclaration();
			this.urlVisualisationExterneDocument = Optional.of(etatArchive)
					.map(EtatDeclarationAvecDocumentArchive::getCleDocument)
					.filter(StringUtils::isNotBlank)
					.map(cle -> infraService.getUrlVisualisationDocument(declaration.getTiers().getNumero(), declaration.getPeriode().getAnnee(), cle))
					.orElse(null);
		}
	}

	public Long getId() {
		return id;
	}

	public RegDate getDateObtention() {
		return dateObtention;
	}

	@Override
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

	public String getUrlVisualisationExterneDocument() {
		return urlVisualisationExterneDocument;
	}

	@Override
	public int compareTo(EtatDeclarationView o) {
		if (this.dateObtention == o.dateObtention) {
			return -1 * logCreationDate.compareTo(o.logCreationDate);
		}
		else {
			// du plus récent au plus ancient
			return -1 * dateObtention.compareTo(o.dateObtention);
		}
	}
}
