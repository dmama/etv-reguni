<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<tiles:insert template="/WEB-INF/jsp/templates/templateDialog.jsp">
    <tiles:put name="body">
	    <fmt:message key="label.efacture.valider.action">
		    <fmt:param value="${libelleAction}"/>
		    <fmt:param>
			    <unireg:numCTB numero="${ctb}"/>
		    </fmt:param>
	    </fmt:message>
	    <br/><br/>
	    <c:set var="url">
		    <c:url value="${actionUrl}"/>
	    </c:set>
	    <form:form name="commentForm" id="commentForm" action="${url}" method="post">
		    <table cellpadding="0" cellspacing="0">
			    <tr>
				    <td>
					    <fmt:message key="label.efacture.commentaire.optionnel"/>
				    </td>
				    <td align="right" style="font-style: italic">
					    (<span id="remainingLen">${maxlen}</span> <fmt:message key="label.efacture.caracteres.restants"/>)
				    </td>
		        </tr>
		    </table>
		    <br/>
		    <form:textarea id="commentValue" path="comment" cols="50" rows="4" cssClass="add-comment"/>
		    <span style="color: red;">*</span>

		    <script type="text/javascript">
			    var CommentaireManuelEFacture = {
				    checkTextAreaLength : function() {
					    var textArea = $('#commentValue')[0];
					    var currentText = textArea.value;
					    if (currentText.length > ${maxlen}) {
						    textArea.value = currentText.substr(0, ${maxlen});
					    }

					    // gestion du décompte de caractères restants
					    var remainingLen = ${maxlen} - textArea.value.length;
					    $('#remainingLen').text(remainingLen);
				    }
			    };
			    $(document).everyTime('100ms', CommentaireManuelEFacture.checkTextAreaLength);
		    </script>
	    </form:form>
    </tiles:put>
</tiles:insert>
