<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

	<tiles:put name="head">
		<style type="text/css">
			.pageheader {
				margin-top: 0px;
			}
		</style>
	</tiles:put>

  	<tiles:put name="body">

		<c:if test="${not empty results.errors || not empty results.warnings}">
			<table class="validation_error" cellspacing="0" cellpadding="0" border="0">
			<tr>
				<td class="heading">
					<fmt:message key="label.validation.problemes.detectes"/>
					<span id="val_script">(<a href="#" onclick="javascript:showDetails()"><fmt:message key="label.validation.voir.details"/></a>)</span>
				</td>
			</tr>
			<tr id="val_errors">
				<td class="details">
					<ul>
						<c:forEach items="${results.errors}" var="error">
							<li class="err"><fmt:message key="label.validation.erreur"/>: <c:out value="${error}"/></li>
						</c:forEach>
						<c:forEach items="${results.warnings}" var="warning">
							<li class="warn"><fmt:message key="label.validation.warning"/>: <c:out value="${warning}"/></li>
						</c:forEach>
					</ul>
				</td>
			</tr>
			</table>

			<script type="text/javascript">
				// cache les erreurs par défaut
				 $('#val_errors').hide();

				 // affiche les erreurs
				 function showDetails() {
					 $('#val_errors').show();
					 $('#val_script').hide();
				 }
			</script>
		</c:if>

	</tiles:put>
	
</tiles:insert>
