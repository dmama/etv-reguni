<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.identification.recherche.personne" /></tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formNonIdentifie" name="formNonIdentifie" action="edit.do">
	    	<input type="hidden"  name="__TARGET__" value="">
			<input type="hidden"  name="__EVENT_ARGUMENT__" value="">
	    	<jsp:include page="../demande-identification.jsp" >
		    	<jsp:param name="path" value="demandeIdentificationView" />
	    	</jsp:include>
		
		<!-- Debut Message retour -->
			<jsp:include page="raison-non-identification.jsp">
				<jsp:param name="path" value="demandeIdentificationView" />	
		    </jsp:include> 
		<!-- Fin message retour -->
	</form:form>
	
	</tiles:put>
</tiles:insert>