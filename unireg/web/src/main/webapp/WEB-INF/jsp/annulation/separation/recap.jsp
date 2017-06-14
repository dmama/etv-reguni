<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<%--@elvariable id="idMenage" type="java.lang.Long"--%>
<%--@elvariable id="idContribuablePrincipal" type="java.lang.Long"--%>
<%--@elvariable id="idContribuableConjoint" type="java.lang.Long"--%>
<%--@elvariable id="dateSeparation" type="ch.vd.registre.base.date.RegDate"--%>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.annulation.separation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/annulation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">

	    <div style="width: 48%; float: left;">
		    <c:set var="titreCaracteristiques"><fmt:message key="label.caracteristiques.premierePersonne"/></c:set>
		    <unireg:bandeauTiers numero="${idContribuablePrincipal}" showComplements="true" showValidation="true" showLinks="false" showAvatar="false" titre="${titreCaracteristiques}"/>
	    </div>
	    <c:if test="${idContribuableConjoint != null}">
		    <div style="width: 50%; padding-left: 1ex; float: right;">
			    <c:set var="titreCaracteristiques"><fmt:message key="label.caracteristiques.secondePersonne"/></c:set>
			    <unireg:bandeauTiers numero="${idContribuableConjoint}" showComplements="true" showValidation="true" showLinks="false" showAvatar="false" titre="${titreCaracteristiques}"/>
		    </div>
	    </c:if>

	    <fieldset class="information">
		    <legend><span><fmt:message key="title.caracteristiques.annulation.separation" /></span></legend>
		    <table>
			    <tr class="<unireg:nextRowClass/>" >
				    <td width="25%"><fmt:message key="label.date.separation" />&nbsp;:</td>
				    <td width="75%">
					    <c:choose>
						    <c:when test="${dateSeparation != null}">
							    <unireg:regdate regdate="${dateSeparation}" />
						    </c:when>
						    <c:otherwise>
							    <span class="error"><fmt:message key="error.date.separation.introuvable"/></span>
						    </c:otherwise>
					    </c:choose>

				    </td>
			    </tr>
		    </table>
	    </fieldset>

	    <!-- Debut Boutons -->
	    <unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver ?"/>
	    <c:if test="${dateSeparation != null}">
		    <c:set var="NomBoutonSauver"><fmt:message key="label.bouton.sauver"/></c:set>
		    <c:set var="dateSeparationString"><unireg:regdate regdate="${dateSeparation}"/></c:set>
		    <unireg:buttonTo name="${NomBoutonSauver}" action="/annulation/separation/commit.do" params="{numeroCple:${idMenage},date:'${dateSeparationString}'}" method="post" confirm="Voulez-vous vraiment annuler la séparation de ces deux personnes ?"/>
	    </c:if>
		<!-- Fin Boutons -->

	</tiles:put>
</tiles:insert>