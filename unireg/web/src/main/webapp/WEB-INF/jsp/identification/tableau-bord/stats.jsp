<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.tableaubord.messages" /></tiles:put>
  	<tiles:put name="fichierAide">
	</tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    <form:form method="post" id="formRechercheMessage" name="theForm" action="stats.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche.messages"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp" />
			</fieldset>
			<fieldset class="info">
			<legend><span><fmt:message key="label.info.resultat" /></span></legend>
			<display:table name="statistiques" id="resultat" pagesize="25"  requestURI="/identification/tableau-bord/stats.do" class="display_table" sort="external">
				<display:column sortable ="false" titleKey="label.identification.etat.message">
					${resultat.etat}
				</display:column>
				<display:column sortable ="false" titleKey="label.identification.resultat">
					${resultat.resultatIdentification}
				</display:column>
				<display:column sortable ="false" titleKey="label.identification.nombre">				
				<c:choose>
					<c:when test="${(resultat.etatTechnique == 'A_TRAITER_MANUELLEMENT') || 
					 (resultat.etatTechnique == 'A_EXPERTISER') ||
					  (resultat.etatTechnique == 'A_EXPERTISER_SUSPENDU') ||
					   (resultat.etatTechnique == 'A_TRAITER_MAN_SUSPENDU')}">
						 <a href="../gestion-messages/listEnCours.do?etat=${resultat.etatTechnique}&typeMessage=${resultat.typeMessage}&periode=${resultat.periode}">
						${resultat.nombre}</a>
					</c:when>	
					
					<c:when test="${(resultat.etatTechnique == 'TRAITE_AUTOMATIQUEMENT') || 
					 (resultat.etatTechnique == 'TRAITE_MANUELLEMENT') ||
					  (resultat.etatTechnique == 'TRAITE_MAN_EXPERT') ||
					   (resultat.etatTechnique == 'NON_IDENTIFIE')}">
						<a href="../gestion-messages/listTraite.do?etat=${resultat.etatTechnique}&typeMessage=${resultat.typeMessage}&periode=${resultat.periode}">
						${resultat.nombre}</a>
					</c:when>
					<c:when test="${(resultat.etatTechnique == 'EXCEPTION')}">
						<a href="../gestion-messages/listEnCours.do?etat=${resultat.etatTechnique}&typeMessage=${resultat.typeMessage}&periode=${resultat.periode}">
						${resultat.nombre}</a>
					</c:when>
				</c:choose>
				</display:column>
			</display:table>
		</fieldset>
		</form:form>
		
		<script type="text/javascript">
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}
		</script>
	</tiles:put>
</tiles:insert>
