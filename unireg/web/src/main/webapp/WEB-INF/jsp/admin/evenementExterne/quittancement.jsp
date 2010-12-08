<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="contextPath" scope="request" value="${pageContext.request.contextPath}" />

<form method="post" name="quittancement" action="<c:out value='${contextPath}/admin/evenementExterne/quittancement.do' />">
	<table style="width: 100%; margin-top: 10px;" cellpadding="3" border="0">
		<tbody>
			<tr>
				<td nowrap="nowrap" width="20%">Numero Débiteur<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td width="20%">
					<spring:bind path="command.numeroCtb">
	                            <input type="text" id="numeroCtb" name="${status.expression}" value="${status.value}"/>
	                 </spring:bind>
				</td>
				<td width="60%">
					<span class="error" id="quittancement.null.numeroCtb"></span>
					<span class="error" id="quittancement.wrong.numeroCtb"></span>
					
				</td>
			</tr>
			<tr>
				<td nowrap="nowrap">Type Quittancement<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>
					<spring:bind path="command.typeQuittance">
	                  	<select id="typeQuittance" name="${status.expression}" onchange="return TypeQuittance_OnChange(this);">	                            
							<c:forEach items="${typeQuittances}" var="type">
								<option value="${type.value}">${type.name}</option>
							</c:forEach>
						</select>
					</spring:bind>
				</td>
				<td ><span class="error" id="quittancement.null.typeQuittance"></span></td>
			</tr>
			<tr>
				<td nowrap="nowrap">Date debut<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>
	                 <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="command.dateDebut" />
						<jsp:param name="id" value="dateDebut" />
					</jsp:include>
				</td>
				<td><span class="error" id="quittancement.null.dateDebut"></span></td>
			</tr>
			<tr>
				<td nowrap="nowrap">Date fin</td>
				<td>            
	                 <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="command.dateFin" />
						<jsp:param name="id" value="dateFin" />
					</jsp:include>
				</td>
				<td></td>
			</tr>
			<tr>
				<td nowrap="nowrap">Date quittancement<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>
	                 <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="command.dateQuittance" />
						<jsp:param name="id" value="dateQuittance" />
					</jsp:include>
				</td>
				<td><span class="error" id="quittancement.null.dateQuittance"></span></td>
			</tr>
			<tr>
				<td colspan="2"><input type="button" value="Envoyer"  onclick="XT.doAjaxSubmit('send', this,null, {'formName' : 'quittancement' });" />  <input type="button" value="Effacer"  onclick="return Quittancement_Reset();" /></td>
			</tr>
			<tr>
				<td colspan="2"><div class="error" id="error.global"></div></td>
			</tr>
		</tbody>
	</table>
</form>
<script type="text/javascript">
	function Quittancement_Reset() {
		Element.setText('quittancement.null.numeroCtb',"");
		Element.setText('quittancement.wrong.numeroCtb',"");
		Element.setText('quittancement.null.typeQuittance',"");
		Element.setText('quittancement.null.dateDebut',"");
		Element.setText('quittancement.null.dateQuittance',"");
		Element.setText('error.global', "");
		E$('numeroCtb').value = "";
		E$('dateDebut').value = "";
		E$('dateFin').value = "";
		E$('dateQuittance').value = "";
		var select = E$('typeQuittance');
		select.selectedIndex = 0;
		Element.fireObserver( select, "change");
		return true;
	}
	
	
	function TypeQuittance_OnChange( element) {
		var option = element.options[element.selectedIndex];
		if (option) {
			var annulation =( option.value ==='Annulation') ;
			E$('dateQuittance').readOnly  =  annulation ;
			E$('dateQuittance').className  = (annulation ? "readonly" :"");
			E$('dateQuittance_Anchor').style.visibility  = (annulation ? 'hidden' : 'visible');
			if ( annulation)
				E$('dateQuittance').value = "";
		}
		return true;
	}
	
</script>


