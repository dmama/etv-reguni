<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="messageData" type="ch.vd.unireg.identification.contribuable.view.DemandeIdentificationView"--%>
<%--@elvariable id="source" type="ch.vd.unireg.identification.contribuable.IdentificationController.Source"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.identification.recherche.personne" /></tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formNonIdentifie" name="formNonIdentifie" action="non-identifie.do?source=${source}&id=${messageData.id}" commandName="nonIdentification">
	    	<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
	    	<jsp:include page="../demande-identification.jsp" />

		    <!-- Debut Caracteristiques identification -->
		    <fieldset class="information">
			    <legend><span><fmt:message key="caracteristiques.message.Retour" /></span></legend>
			    <table>
				    <tr class="<unireg:nextRowClass/>" >
					    <td>
						    <form:select path="erreurMessage">
							    <form:options items="${erreursMessage}" />
						    </form:select>
						    <span class="mandatory">*</span>
						    <form:errors cssClass="error" path="erreurMessage"/>
					    </td>
				    </tr>
				    <tr class="<unireg:nextRowClass/>" >
					    <td width="50%" colspan="2">&nbsp;</td>
				    </tr>
			    </table>
		    </fieldset>

		    <unireg:buttonTo name="label.bouton.retour" action="/identification/gestion-messages/edit.do" confirm="Voulez-vous vraiment quitter cette page sans sauver ?" method="get" params="{id:${messageData.id},source:'${source}'}"/>
		    &nbsp;
		    <input type="button" name="nonIdentifier" value="<fmt:message key="label.bouton.identification.valider" />" onClick="IdentificationCtb.confirmerImpossibleAIdentifier();" />

		    <!-- Fin Boutons -->

		    <script type="text/javascript" language="javascript" src="<c:url value="/js/identification.js"/>"></script>

		    <!-- Fin Caracteristiques identification -->

		</form:form>
	
	</tiles:put>
</tiles:insert>