<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="title.recapitulatif.separation" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onclick="ouvrirAide('<c:url value='/docs/creation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
	    <unireg:bandeauTiers numero="${idMenage}" showValidation="true" showEvenementsCivils="true" showLinks="false"/>
	  	<form:form method="post" id="formRecapSeparation"  name="formRecapSeparation" commandName="separationCommand" action="commit.do">
		    <form:hidden path="idMenage"/>
			<jsp:include page="rapport.jsp" />
			<!-- Debut Boutons -->
			<unireg:RetourButton link="list.do" message="Voulez-vous vraiment quitter cette page sans sauver?"/>
			<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onclick="return confirm('Voulez-vous vraiment séparer ces deux personnes ?');" />
			<!-- Fin Boutons -->
		</form:form>
	</tiles:put>
</tiles:insert>