<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%@ page import="ch.vd.unireg.common.LengthConstants" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
    <tiles:put name="title">
        <fmt:message key="title.edition.informations.entreprise" />
    </tiles:put>
    <tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value="/docs/maj-civil-complement.pdf"/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
    </tiles:put>
    <tiles:put name="body">

        <unireg:setAuth var="autorisations" tiersId="${tiersId}"/>
        <c:if test="${autorisations.identificationEntreprise}">

            <unireg:bandeauTiers numero="${tiersId}" showLinks="false" showComplements="false" showEvenementsCivils="false" showValidation="false"/>

            <c:set var="length_ide_min" value="<%=LengthConstants.IDENT_ENTREPRISE_IDE%>" scope="request" />

            <form:form method="post" action="edit.do?id=${tiersId}" name="editidecontribuable" commandName="data" id="editForm">
                <span><%-- span vide pour que IE8 calcul correctement la hauteur du fieldset (voir fieldsets-workaround.jsp) --%></span>
                <fieldset class="information">
                    <legend><span><fmt:message key="label.tiers.information.entreprise" /></span></legend>
                    <unireg:nextRowClass reset="1"/>
                    <table>
                        <c:set var="length_ide_max" value="${length_ide_min + 3}" scope="request" />
                        <tr class="<unireg:nextRowClass/>" >
                            <td width="20%"><fmt:message key="label.numero.ide"/>&nbsp;:</td>
                            <td>
                                <form:input path="ide" id="ac.ide" cssErrorClass="input-with-errors" size="20" maxlength="${length_ide_max}" tabindex="1"
                                            onchange="NumeroIDE.checkValue(this.value, '${length_ide_min}', '${tiersId}', 'ide_utilise_warning');"
                                            onkeyup="NumeroIDE.checkValue(this.value, '${length_ide_min}', '${tiersId}', 'ide_utilise_warning');"/>
                                <form:errors path="ide" cssClass="error"/>
                                <span id="ide_utilise_warning" style="display:none; margin-left: 1em;" class="warn warning_icon"></span>
                            </td>
                        </tr>
                    </table>
                </fieldset>

                <c:set var="libelleBoutonRetour">
                    <fmt:message key="label.bouton.retour"/>
                </c:set>
                <c:set var="confirmationMessageRetour">
                    <fmt:message key="message.confirm.quit"/>
                </c:set>
                <unireg:buttonTo method="get" action="/tiers/visu.do" params="{id:${tiersId}}" name="${libelleBoutonRetour}" confirm="${confirmationMessageRetour}"/>
                <input type="button" name="save" value="<fmt:message key="label.bouton.sauver"/>" onclick="editIdeContribuable.onSave($('#editForm'))"/>

                <c:set var="confirmationMessageSauvegarde">
                    <fmt:message key="label.demande.confirmation.sauvegarde"/>
                </c:set>
                <script type="text/javascript">
                    var editIdeContribuable = {
                        onSave : function(myform) {
                            if (confirm('${confirmationMessageSauvegarde}')) {
                                myform.submit();
                            }
                        }
                    };

                    $(function() {
	                    NumeroIDE.checkValue('${data.ide}', '${length_ide_min}', '${tiersId}', 'ide_utilise_warning');
                    });
                </script>

            </form:form>

        </c:if>
        <c:if test="${!autorisations.identificationEntreprise}">
            <span class="error"><fmt:message key="error.tiers.interdit" /></span>
        </c:if>

    </tiles:put>
</tiles:insert>