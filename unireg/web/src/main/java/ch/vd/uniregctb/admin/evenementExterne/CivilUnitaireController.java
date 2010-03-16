package ch.vd.uniregctb.admin.evenementExterne;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.MessageCreator;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springmodules.xt.ajax.AjaxResponse;
import org.springmodules.xt.ajax.AjaxSubmitEvent;
import org.springmodules.xt.ajax.action.ReplaceContentAction;
import org.springmodules.xt.ajax.component.TaggedText;
import org.springmodules.xt.ajax.web.servlet.AjaxModelAndView;

import ch.vd.infrastructure.model.impl.DateUtils;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument;
import ch.vd.schema.registreCivil.x20070914.evtRegCivil.EvtRegCivilDocument.EvtRegCivil;
import ch.vd.uniregctb.common.SelectContainerValue;
import ch.vd.uniregctb.type.TypeEvenementCivil;
import ch.vd.uniregctb.web.xt.AbstractEnhancedSimpleFormController;

public class CivilUnitaireController extends AbstractEnhancedSimpleFormController {

	private static Logger logger = Logger.getLogger(CivilUnitaireController.class);

	private JmsOperations jmsTemplateOutput;


	@Override
	protected Object formBackingObject(HttpServletRequest request) throws Exception {
		return new CivilUnitaireView();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>(3);
		ArrayList<SelectContainerValue> typeEvenementCivils = new ArrayList<SelectContainerValue>();
		for (TypeEvenementCivil typeEvenementCivil : TypeEvenementCivil.values()) {
			typeEvenementCivils.add(new SelectContainerValue(typeEvenementCivil.getFullDescription(), typeEvenementCivil.getId()));
		}
		map.put("typeEvenementCivils", typeEvenementCivils);
		return map;
	}



	@Override
	protected void onBindAndValidate(HttpServletRequest request, Object command, BindException errors) throws Exception {
		super.onBindAndValidate(request, command, errors);
		CivilUnitaireView view = (CivilUnitaireView) command;
		if (view.getNoIndividu() == null || "".equals(view.getNoIndividu())) {
			errors.rejectValue("noIndividu", "quittancement.null.noIndividu", "Numéro d'individu est obligatoire");
		} else {
			try {
				Long.parseLong(view.getNoIndividu());
			} catch(NumberFormatException ex) {
				errors.rejectValue("noIndividu", "quittancement.wrong.noIndividu", "Numéro d'individu est mal formaté");
			}
		}
		if (view.getNoTechnique() == null || "".equals(view.getNoTechnique())) {
			errors.rejectValue("noTechnique", "quittancement.null.noTechnique", "Numéro technique est obligatoire");
		} else {
			try {
				Long.parseLong(view.getNoTechnique());
			} catch(NumberFormatException ex) {
				errors.rejectValue("noTechnique", "quittancement.wrong.noTechnique", "Numéro technique est mal formaté");
			}
		}
		if (view.getDateEvenement() == null) {
			errors.rejectValue("dateEvenement", "quittancement.null.dateEvenement", "La date de l'événement est obligatoire");
		}
		if (view.getDateTraitement() == null) {
			errors.rejectValue("dateTraitement", "quittancement.null.dateTraitement", "La date de traitement est obligatoire");
		}
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors)
			throws Exception {
		return new AjaxModelAndView(this.getSuccessView(), errors);
	}

	public AjaxResponse send(AjaxSubmitEvent event, AjaxResponse response) throws Exception {
		if ( event.getValidationErrors().hasErrors()) {
			return response;
		}

		CivilUnitaireView view = (CivilUnitaireView) event.getCommandObject();
		try {
			EvtRegCivil evt= createEvenement(view);

			sendEvent(evt);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText("événement envoyé", TaggedText.Tag.DIV)));
		}
		catch (Exception ex) {
			logger.warn("Erreur lors de l'envoie d'une événement externe: ", ex);
			response.addAction( new ReplaceContentAction("error.global", new TaggedText(ex.getMessage(), TaggedText.Tag.DIV)));
		}
		return response;
	}


	public EvtRegCivil createEvenement(CivilUnitaireView view) {
		EvtRegCivil evenement = EvtRegCivilDocument.Factory.newInstance().addNewEvtRegCivil();
		evenement.setNoTechnique(Integer.parseInt(view.getNoTechnique()));
		evenement.setNumeroOFS(Integer.parseInt(view.getNumeroOFS()));
		evenement.setNoIndividu(Integer.parseInt(view.getNoIndividu()));
		evenement.setDateEvenement(DateUtils.calendar(view.getDateEvenement()));
		evenement.setDateTraitement(DateUtils.calendar(view.getDateTraitement()));
		evenement.setCode(Integer.parseInt(view.getTypeEvenementCivil()));
		return evenement;
	}

	public void sendEvent(EvtRegCivil evtRegCivil) throws Exception {
		if (evtRegCivil == null) {
			throw new IllegalArgumentException("Argument evtRegCivil ne peut être null.");
		}
		final ByteArrayOutputStream writer = new ByteArrayOutputStream();
		try {
			writeXml(writer, evtRegCivil);
		}
		catch (Exception e) {
			String message = "Exception lors de la sérialisation xml";
			logger.fatal(message, e);
			throw new Exception(message);
		}
		try {
			jmsTemplateOutput.send(new MessageCreator() {

				public Message createMessage(Session session) throws JMSException {
					TextMessage message = session.createTextMessage();
					message.setText(writer.toString());
					return message;
				}
			});
		}
		catch (Exception e) {
			String message = "Exception lors du processus d'envoi d'un message JMS";
			logger.fatal(message, e);

			throw new Exception(message);
		}
	}


	protected void writeXml(OutputStream writer, XmlObject object) throws Exception {
		XmlOptions validateOptions = new XmlOptions();
		ArrayList<XmlError> errorList = new ArrayList<XmlError>();
		validateOptions.setErrorListener(errorList);
		if (!object.validate(validateOptions)) {
			StringBuilder builder = new StringBuilder();
			for (XmlError error : errorList) {
				builder.append("\n");
				builder.append("Message: " + error.getErrorCode() + " " + error.getMessage() + "\n");
				builder.append("Location of invalid XML: " + error.getCursorLocation().xmlText() + "\n");
				throw new Exception(builder.toString());
			}
		}
		object.save(writer, new XmlOptions().setSaveOuter());
	}

	/**
	 * @param jmsTemplateOutput the jmsTemplateOutput to set
	 */
	public void setJmsTemplateOutput(JmsOperations jmsTemplateOutput) {
		this.jmsTemplateOutput = jmsTemplateOutput;
	}


}
