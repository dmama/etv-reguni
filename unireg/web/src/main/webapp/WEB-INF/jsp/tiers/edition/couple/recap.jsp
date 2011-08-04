<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
  	<tiles:put name="title">
  		<fmt:message key="${pageTitle}" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/creation-couple.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">

	  	<form:form method="post" id="formRecapCouple"  name="formRecapCouple">
			
			<c:set var="premierePPClassName" value="couplePremierePP" />
			<c:if test="${command.secondePersonne == null}">
				<c:set var="premierePPClassName" value="seulPP" />
			</c:if>
			
			<jsp:include page="../../../general/pp.jsp">
				<jsp:param name="page" value="couple" />
				<jsp:param name="path" value="premierePersonne" />
				<jsp:param name="className" value="${premierePPClassName}"/>
			</jsp:include>
			<c:if test="${command.secondePersonne != null}">
				<jsp:include page="../../../general/pp.jsp">
					<jsp:param name="page" value="couple" />
					<jsp:param name="path" value="secondePersonne" />
					<jsp:param name="className" value="coupleSecondePP"/>
				</jsp:include>
			</c:if>
			<c:choose>
				<c:when test="${command.typeUnion == 'COUPLE' or command.typeUnion == 'SEUL'}">
					<jsp:include page="rapport-non-hab.jsp" />
				</c:when>
				<c:otherwise>
					<jsp:include page="rapport.jsp" />
				</c:otherwise>
			</c:choose>
			<!-- Debut Boutons -->
			<div id="buttons">
				<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="javascript:Page_RetourRecapCouple(${command.premierePersonne.numero});" />
				<input type="submit" value="<fmt:message key="label.bouton.sauver"/>" onClick="javascript:return Page_sauverCouple(event || window.event);" />
			</div>
			<!-- Fin Boutons -->
		</form:form>
		<script type="text/javascript" language="Javascript">
			function Page_RetourRecapCouple(numeroPP1) {
				if(confirm('<fmt:message key="message.confirm.quit" />')) {
					document.location.href='list-pp.do?numeroPP1=' + numeroPP1 ;
				}
			}
			function Page_sauverCouple(event) {
				if(!confirm('<fmt:message key="${coupleConfirmationMsg}" /> ')) {
					return Event.stop(event);
			 	}
			 	return true ;
			}
		</script>
	</tiles:put>
</tiles:insert>