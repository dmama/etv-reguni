<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
		<fmt:message key="title.ajout.principal.communaute"/>
  	</tiles:put>
  	<tiles:put name="head">
	    <style type="text/css">
		    .principal {
			    font-weight: bold;
		    }
	    </style>
  	</tiles:put>
  	<tiles:put name="body">

	    <%--@elvariable id="modele" type="ch.vd.unireg.registrefoncier.communaute.ModeleCommunauteView"--%>
	    <fieldset class="information">
		    <legend><span><fmt:message key="label.caracteristiques.modele.communaute" /></span></legend>
		    <table cellspacing="0" cellpadding="5" border="0" class="display_table">
			    <tr class="odd">
				    <td width="15%" nowrap="">N° du modèle de communauté&nbsp;:&nbsp;</td>
				    <td width="50%">${modele.id}</td>
				    <td width="35%">&nbsp;</td>
			    </tr>
		    </table>
	    </fieldset>

	    <fieldset>
		    <legend><span><fmt:message key="label.histo.principaux.communaute" /></span></legend>
		    <display:table name="modele.principaux" id="principal" pagesize="25" class="display" requestURI="/registrefoncier/communaute/addPrincipal.do" decorator="ch.vd.unireg.decorator.TableEntityDecorator">
			    <display:column titleKey="label.par.defaut">
				    <input type="checkbox" disabled="disabled" <c:if test="${principal.parDefaut}">checked</c:if>/>
			    </display:column>
			    <display:column titleKey="label.date.debut">
				    <unireg:regdate regdate="${principal.dateDebut}" />
			    </display:column>
			    <display:column titleKey="label.date.fin">
				    <unireg:regdate regdate="${principal.dateFin}" />
			    </display:column>
			    <display:column titleKey="label.numero.contribuable">
				    <c:set var="noctb">
					    <unireg:numCTB numero="${principal.principal.ctbId}"/>
				    </c:set>
				    <unireg:linkTo name="${noctb}" action="/registrefoncier/communaute/showTiers.do" params="{id:${principal.principal.ctbId}}"/>
			    </display:column>
			    <display:column titleKey="label.nom.raison">
				    <c:out value="${principal.principal.nom}"/>
			    </display:column>
			    <display:column titleKey="label.prenoms">
				    <c:out value="${principal.principal.prenom}"/>
			    </display:column>
		    </display:table>
	    </fieldset>

	    <%--@elvariable id="addPrincipalView" type="ch.vd.unireg.registrefoncier.communaute.AddPrincipalView"--%>
	    <form:form modelAttribute="addPrincipalView" method="post" action="addPrincipal.do" id="addPrincipalForm">

		    <form:hidden path="modeleId"/>
		    <form:hidden path="membreId"/>

		    <fieldset>
			    <legend><span><fmt:message key="title.ajout.principal.details"/></span></legend>
			    <fmt:setLocale value="fr_CH" scope="session"/>

			    <table border="0">
				    <tr class="odd">
					    <td style="width: 15%;"><fmt:message key="label.numero.contribuable"/>&nbsp;:</td>
					    <td style="width: 35%;"><unireg:numCTB numero="${membre.ctbId}"/></td>
				    </tr>
				    <tr class="even">
					    <td style="width: 15%;"><fmt:message key="label.nom.raison"/>&nbsp;:</td>
					    <td style="width: 35%;"><c:out value="${membre.nom}"/></td>
					    <td style="width: 15%;"><fmt:message key="label.date.naissance"/>&nbsp;:</td>
					    <td style="width: 35%;"><unireg:regdate regdate="${membre.dateNaissance}" /></td>
				    </tr>
				    <tr class="odd">
					    <td style="width: 15%;"><fmt:message key="label.prenoms"/>&nbsp;:</td>
					    <td style="width: 35%;"><c:out value="${membre.prenom}"/></td>
					    <td style="width: 15%;"><fmt:message key="label.date.deces"/>&nbsp;:</td>
					    <td style="width: 35%;"><unireg:regdate regdate="${membre.dateDeces}" /></td>
				    </tr>
				    <tr class="even">
					    <td style="width: 15%;"><fmt:message key="label.debut.periode"/>&nbsp;:</td>
					    <td style="width: 35%;"><form:input path="periodeDebut" maxlength="4" /><form:errors path="periodeDebut" cssClass="error" /></td>
				    </tr>
			    </table>
		    </fieldset>

		    <div style="padding: 0 25%;">
			    <div style="padding: 0 20%; display: inline">
				    <input type="button" value="<fmt:message key='label.bouton.ajouter'/>" onclick="Form.disableButtonAndSubmitForm(this, 'addPrincipalForm');"/>
			    </div>
			    <div style="padding: 0 20%; display: inline">
				    <unireg:buttonTo name="Annuler" action="/registrefoncier/communaute/showModele.do" params="{id:${modele.id}}" method="GET"/>
			    </div>
		    </div>

	    </form:form>



	</tiles:put>
</tiles:insert>