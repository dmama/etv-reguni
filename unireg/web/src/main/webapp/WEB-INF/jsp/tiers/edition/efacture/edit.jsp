<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<jsp:useBean id="histo" scope="request" type="ch.vd.uniregctb.efacture.DestinataireAvecHistoView"/>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="title">
        <fmt:message key="title.edition.efacture" />
    </tiles:put>
    <tiles:put name="body">

        <unireg:nextRowClass reset="1"/>
        <unireg:bandeauTiers numero="${histo.ctbId}" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="true"/>

        <%-- destinataire --%>
        <fieldset>
            <legend><span><fmt:message key="label.efacture.historique.destinataire" /></span></legend>
            <display:table id="destinataire" name="histo.etats" class="display">
                <display:column titleKey="label.efacture.date.obtention">
                    <unireg:sdate sdate="${destinataire.dateObtention}"/>
                </display:column>
                <display:column titleKey="label.efacture.etat">
                    <c:out value="${destinataire.descriptionEtat}"/>
                </display:column>
                <display:column titleKey="label.efacture.motifTransition">
                    <c:out value="${destinataire.motifObtention}"/>
                </display:column>
            </display:table>

            <table border="0" style="display: ${histo.suspendable || histo.activable ? '' : 'none'};">
                <tr>
                    <td align="center">
                        <c:if test="${histo.suspendable}">
                            <input id="suspend" type="button" value="<fmt:message key='label.efacture.bouton.suspendre'/>" onclick="EditEFacture.addComment(${histo.ctbId}, 'SUSPEND');"/>
                            <c:if test="${histo.activable}">
                                &nbsp;
                            </c:if>
                        </c:if>
                        <c:if test="${histo.activable}">
                            <input id="activate" type="button" value="<fmt:message key='label.efacture.bouton.activer'/>" onclick="EditEFacture.addComment(${histo.ctbId}, 'ACTIVATE');"/>
                        </c:if>
                    </td>
                </tr>
            </table>
        </fieldset>

        <%-- demande d'inscription en cours --%>
        <fieldset>
            <legend><span><fmt:message key="label.efacture.historique.demande.en.cours" /></span></legend>
            <c:set var="demandeEnCours" value="${histo.demandeEnCours}"/>
            <c:if test="${demandeEnCours != null}">
                <display:table id="etatDemande" name="histo.demandeEnCours.etats" class="display">
                    <display:column titleKey="label.efacture.date.obtention">
                        <unireg:sdate sdate="${etatDemande.dateObtention}"/>
                    </display:column>
                    <display:column titleKey="label.efacture.etat">
                        <c:out value="${etatDemande.descriptionEtat}"/>
                    </display:column>
                    <display:column titleKey="label.efacture.motifTransition">
                        <c:out value="${etatDemande.motifObtention}"/>
                    </display:column>
                </display:table>

                <c:set var="etatDemandeEnCours" value="${demandeEnCours.etatCourant}"/>
                <table border="0" style="display: ${etatDemandeEnCours.validable || etatDemandeEnCours.refusable || etatDemandeEnCours.mettableEnAttenteSignature || etatDemandeEnCours.mettableEnAttenteContact ? '' : 'none'};">
                    <tr>
                        <td align="center">
                            <c:if test="${etatDemandeEnCours.validable}">
                                &nbsp;
                                <input id="suspend" type="button" value="<fmt:message key='label.efacture.bouton.valider'/>" onclick="EditEFacture.validate(${histo.ctbId}, ${demandeEnCours.idDemande});"/>
                                &nbsp;
                            </c:if>
                            <c:if test="${etatDemandeEnCours.refusable}">
                                &nbsp;
                                <input id="activate" type="button" value="<fmt:message key='label.efacture.bouton.refuser'/>" onclick="EditEFacture.refuse(${histo.ctbId}, ${demandeEnCours.idDemande});"/>
                                &nbsp;
                            </c:if>
                            <c:if test="${etatDemandeEnCours.mettableEnAttenteSignature}">
                                &nbsp;
                                <input id="wait-signature" type="button" value="<fmt:message key='label.efacture.bouton.attente.signature'/>" onclick="EditEFacture.waitForSignature();"/>
	                            <div style="display:none;">
		                            <form:form id="signature-form" commandName="dataDemande" action="wait-signature.do" method="post">
			                            <form:hidden path="noAdherent"/>
			                            <form:hidden path="ctbId"/>
			                            <form:hidden path="idDemande"/>
			                            <form:hidden path="dateDemande"/>
		                            </form:form>
			                    </div>
                                &nbsp;
                            </c:if>
                            <c:if test="${etatDemandeEnCours.mettableEnAttenteContact}">
                                &nbsp;
                                <input id="wait-contact" type="button" value="<fmt:message key='label.efacture.bouton.attente.contact'/>" onclick="EditEFacture.waitForContact();"/>
	                            <div style="display:none;">
		                            <form:form id="contact-form" commandName="dataDemande" action="wait-contact.do" method="post">
			                            <form:hidden path="noAdherent"/>
			                            <form:hidden path="ctbId"/>
			                            <form:hidden path="idDemande"/>
			                            <form:hidden path="dateDemande"/>
		                            </form:form>
	                            </div>
                                &nbsp;
                            </c:if>
                        </td>
                    </tr>
                </table>
            </c:if>
            <c:if test="${demandeEnCours == null}">
                <span style="font-style: italic;"><c:out value="Aucune demande d'insctription en cours"/></span>
            </c:if>
        </fieldset>

        <input id="boutonRetour" type="button" value="<fmt:message key='label.efacture.bouton.retour'/>" onclick="document.location.href='../tiers/visu.do?id=${histo.ctbId}'"/>

        <script type="text/javascript">

            var EditEFacture = {

	            addComment: function(ctbid, action) {
		            var dialog = Dialog.create_dialog_div('add-comment-dialog');

		            // charge le contenu de la boîte de dialogue
		            dialog.load(App.curl('/efacture/add-comment.do') + '?ctb=' + ctbid + '&action=' + action + '&' + new Date().getTime());

		            dialog.dialog({
			                          title: "Activation / suspension d'un contribuable e-Facture",
			                          height: 220,
			                          width: 400,
			                          modal: true,
			                          buttons: {
				                          "Valider": function() {
					                          if (confirm("Êtes-vous sûr ?")) {
						                          // les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal
						                          var buttons = $('.ui-button');
						                          buttons.each(function() {
							                          if ($(this).text() == 'Valider') {
								                          $(this).addClass('ui-state-disabled');
								                          $(this).attr('disabled', true);
							                          }
						                          });
						                          var form = dialog.find('#commentForm');
						                          form.submit();
				                              }
				                          },
				                          "Annuler": function() {
					                          dialog.dialog("close");
				                          }
			                          }
		                          });
	            },

                validate: function(ctbId, idDemande) {
                    if (confirm('Êtes-vous sûr de vouloir valider la demande d\'inscription de ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/validate.do?ctb=" + ctbId + '&idDemande=' + idDemande) + '"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                },

                refuse: function(ctbId, idDemande) {
                    if (confirm('Êtes-vous sûr de vouloir refuser la demande d\'inscription de ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/refuse.do?ctb=" + ctbId + '&idDemande=' + idDemande) + '"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                },

                waitForSignature: function() {
                    if (confirm('Êtes-vous sûr de vouloir envoyer le formulaire d\'acceptation à ce contribuable ?')) {
	                    $("#signature-form").submit();
                    }
                },

                waitForContact: function() {
                    if (confirm('Êtes-vous sûr de vouloir envoyer le courrier de demande de contact à ce contribuable ?')) {
	                    $("#contact-form").submit();
                    }
                }
            };

        </script>

    </tiles:put>
</tiles:insert>
