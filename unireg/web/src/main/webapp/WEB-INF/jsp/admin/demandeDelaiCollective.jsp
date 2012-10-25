<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
		<fmt:message key="title.demande.delai.collective"/>  	
	</tiles:put>
  	<tiles:put name="head">
  	<script type="text/javascript">
		$(document).ready(function() {
			$("#traitementEnCours").hide()
			$("#submit").click( function () {
				$("#submit").hide()
				$("#traitementEnCours").show()
				return true
			})		
		})
  	</script>
	</tiles:put>
  	<tiles:put name="body">
  	 	<form:form method="post" enctype="multipart/form-data">
  	 	<fieldset>
  	 	<legend><fmt:message key="label.demande.delai.collective.fieldset.legend"/></legend>
  		<table>
  				<spring:bind path="file">
		 		<tr class="even">
		 			<td><fmt:message key="label.demande.delai.collective.file"/></td>
		 			<td>
		 				<input type="file" name="file" size="75"/>
						<c:if test="${status.error}">
		 					&nbsp;<span class="erreur">${status.errorMessage}</span>
		 				</c:if>
		 			</td>
		 		</tr>
		 		</spring:bind>
		 		<spring:bind path="delai">	
		 		<tr class="odd">
		 			<td><fmt:message key="label.demande.delai.collective.delai"/></td>
		 			<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="delai" />
						<jsp:param name="id" value="delai" />
					</jsp:include>
		 			</td>
		 		</tr>
		 		</spring:bind>
		 		<spring:bind path="tailleLot">	
		 		<tr class="even">
		 			<td><fmt:message key="label.demande.delai.collective.tailleLot"/></td>
		 			<td>
		 			<form:input path="tailleLot" maxlength="4"/>
					<c:if test="${status.error}">
	 					&nbsp;<span class="erreur">${status.errorMessage}</span>
	 				</c:if>
		 			</td>
		 		</tr>
		 		</spring:bind>	
 		</table>
 		</fieldset>
		<input id="submit" type="submit" value="<fmt:message key="label.bouton.envoyer"/>"/>
		<span id="traitementEnCours">Traitement en cours ...</span>
 		</form:form>
 	</tiles:put>
</tiles:insert>