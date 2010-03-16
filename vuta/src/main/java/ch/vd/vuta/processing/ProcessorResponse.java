package ch.vd.vuta.processing;

import ch.vd.vuta.model.SmsModel;

public class ProcessorResponse {

	public static final String ERROR_RESPONSE = "IFD: Le sms que nous avons reçu est malheureusement invalide. Merci de contrôler la procédure SMS et votre no de contribuable. Admin. cantonale des impôts";
	public static final String STATUS_XML_INVALIDE = "XML invalide";
	public static final String STATUS_NO_CTB_INVALIDE = "No de CTB invalide";
	public static final String STATUS_NO_CTB_INTROUVABLE = "No de CTB introuvable";
	public static final String STATUS_OK = "SMS OK";

	private String textForSender;
	private SmsModel sms;
	
	public ProcessorResponse(String response, SmsModel sms) {

		this.sms = sms;
		textForSender = response;
	}

	public String getTexteForSender() {
		return textForSender;
	}

	public SmsModel getSms() {
		return sms;
	}

}
