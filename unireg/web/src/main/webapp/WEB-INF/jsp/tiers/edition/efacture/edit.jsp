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
                            <input id="suspend" type="button" value="<fmt:message key='label.efacture.bouton.suspendre'/>" onclick="EditEFacture.suspend(${histo.ctbId});"/>
                            <c:if test="${histo.activable}">
                                &nbsp;
                            </c:if>
                        </c:if>
                        <c:if test="${histo.activable}">
                            <input id="activate" type="button" value="<fmt:message key='label.efacture.bouton.activer'/>" onclick="EditEFacture.activate(${histo.ctbId});"/>
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
                                <input id="activate" type="button" value="<fmt:message key='label.efacture.bouton.attente.signature'/>" onclick="EditEFacture.waitForSignature(${histo.ctbId}, ${demandeEnCours.idDemande}, '<unireg:regdate regdate="${demandeEnCours.dateDemande}" />');"/>
                                &nbsp;
                            </c:if>
                            <c:if test="${etatDemandeEnCours.mettableEnAttenteContact}">
                                &nbsp;
                                <input id="activate" type="button" value="<fmt:message key='label.efacture.bouton.attente.contact'/>" onclick="EditEFacture.waitForContact(${histo.ctbId}, ${demandeEnCours.idDemande},  '<unireg:regdate regdate="${demandeEnCours.dateDemande}" />');"/>
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

                suspend: function(ctbId) {
                    if (confirm('Êtes-vous sûr de vouloir suspendre l\'utilisation des e-Factures pour ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/suspend.do?ctb=" + ctbId) + '"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                },

                activate: function(ctbId) {
                    if (confirm('Êtes-vous sûr de vouloir activer l\'utilisation des e-Factures pour ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/activate.do?ctb=" + ctbId) + '"/>');
                        form.appendTo('body');
                        form.submit();
                    }
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

                waitForSignature: function(ctbId, idDemande, dateDemande) {
                    if (confirm('Êtes-vous sûr de vouloir envoyer le formulaire d\'acceptation à ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/wait-signature.do?ctb=" + ctbId + '&idDemande=' + idDemande + '&dateDemande=' + dateDemande) +'"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                },

                waitForContact: function(ctbId, idDemande, dateDemande) {
                    if (confirm('Êtes-vous sûr de vouloir envoyer le courrier de demande de contact à ce contribuable ?')) {
                        var form = $('<form method="POST" action="' + App.curl("/efacture/wait-contact.do?ctb=" + ctbId + '&idDemande=' + idDemande + '&dateDemande=' + dateDemande) +'"/>');
                        form.appendTo('body');
                        form.submit();
                    }
                }
            };

        </script>

    </tiles:put>
</tiles:insert>
