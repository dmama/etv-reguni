<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}"/>
<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">

	<tiles:put name="body">
		<form:form name="formLiberation" id="formLiberation">
			<input type="hidden" name="idDocument" value="${command.idDocument}"/>

			<table cellpadding="3" cellspacing="1">
				<tr>
					<td>
						<fmt:message key="label.di.motif.liberation"/> :
					</td>
					<td align="right" style="font-style: italic">
						(<span id="remainingLen">${maxlen}</span> <fmt:message key="label.efacture.caracteres.restants"/>)
					</td>
				</tr>
				<tr>
					<td></td>
					<td>
						<form:textarea id="motifValue" path="motif" cols="50" rows="6" cssClass="add-motif"/>
						<script type="text/javascript">
							let motivation = {
								checkTextAreaLength: function () {
									let textArea = $('#motifValue')[0];

									if (typeof textArea.value === "undefined" || typeof textArea === "undefined") {
										return;
									}
									let currentText = textArea.value;
									if (currentText.length > ${maxlen}) {
										textArea.value = currentText.substr(0, ${maxlen});
									}

									// gestion du décompte de caractères restants
									let remainingLen = ${maxlen} -textArea.value.length;
									$('#remainingLen').text(remainingLen);
								}
							};
							$(document).everyTime('100ms', motivation.checkTextAreaLength);
						</script>
					</td>

				</tr>
			</table>
		</form:form>

	</tiles:put>
</tiles:insert>
