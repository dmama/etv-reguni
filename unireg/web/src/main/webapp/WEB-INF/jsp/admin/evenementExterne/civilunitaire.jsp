<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<c:set var="contextPath" scope="request" value="${pageContext.request.contextPath}" />

<form method="post" name="civilunitaire" action="<c:out value='${contextPath}/admin/evenementExterne/civilunitaire.do' />">
	<table style="width: 100%; margin-top: 10px;" cellpadding="3" border="0">
		<tbody>
			<tr>
				<td nowrap="nowrap" width="20%">Numero Technique<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td width="20%">
					<spring:bind path="command.noTechnique">
	                            <input type="text" id="noTechnique" name="${status.expression}" value="${status.value}"/>
	                 </spring:bind>
				</td>
				<td width="60%">
					<span class="error" id="quittancement.null.noTechnique"></span>
					<span class="error" id="quittancement.wrong.noTechnique"></span>
					
				</td>
			</tr>
			<tr>
				<td nowrap="nowrap">Type<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>
					<spring:bind path="command.typeEvenementCivil">
	                  	<select id="typeEvenementCivil" name="${status.expression}">	                            
							<c:forEach items="${typeEvenementCivils}" var="type">
								<option value="${type.value}">${type.name}</option>
							</c:forEach>
						</select>
					</spring:bind>
				</td>
				<td ><span class="error" id="quittancement.null.typeEvenementCivil"></span></td>
			</tr>
			<tr>
				<td nowrap="nowrap" width="20%">Numero Individu<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td width="20%">
					<spring:bind path="command.noIndividu">
	                            <input type="text" id="noIndividu" name="${status.expression}" value="${status.value}"/>
	                 </spring:bind>
				</td>
				<td width="60%">
					<span class="error" id="quittancement.null.noIndividu"></span>
					<span class="error" id="quittancement.wrong.noIndividu"></span>
					
				</td>
			</tr>
			<tr>
				<td nowrap="nowrap" width="20%">Numero OFS<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td width="20%">
					<spring:bind path="command.numeroOFS">
	                            <input type="text" id="numeroOFS" name="${status.expression}" value="${status.value}"/>
	                 </spring:bind>
				</td>
				<td width="60%">
					<span class="error" id="quittancement.null.numeroOFS"></span>
					<span class="error" id="quittancement.wrong.numeroOFS"></span>
					
				</td>
			</tr>
			<tr>
				<td nowrap="nowrap">Date Evenement<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>
	                 <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="command.dateEvenement" />
						<jsp:param name="id" value="dateEvenement" />
					</jsp:include>
				</td>
				<td><span class="error" id="quittancement.null.dateEvenement"></span></td>
			</tr>
			<tr>
				<td nowrap="nowrap">Date Traitement<span class="mandatory" title="Champ obligatoire">*</span></td>
				<td>            
	                 <jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="command.dateTraitement" />
						<jsp:param name="id" value="dateTraitement" />
					</jsp:include>
				</td>
				<td><span class="error" id="quittancement.null.dateTraitement"></span></td>
			</tr>
			<tr>
				<td colspan="2"><input type="button" value="Envoyer"  onclick="XT.doAjaxSubmit('send', this,null, {'formName' : 'civilunitaire' });" />  <input type="button" value="Effacer"  onclick="return CiviUnitaire_Reset();" /></td>
			</tr>
			<tr>
				<td colspan="2"><div class="error" id="error.global"></div></td>
			</tr>
		</tbody>
	</table>
</form>
<script type="text/javascript">
	function CiviUnitaire_Reset() {
		$('#quittancement.null.noTechnique').val("");
		$('#quittancement.wrong.noTechnique').val("");
		$('#quittancement.null.numeroOFS').val("");
		$('#quittancement.wrong.numeroOFS').val("");
		$('#quittancement.null.noIndividu').val("");
		$('#quittancement.wrong.noIndividu').val("");		
		$('#quittancement.null.typeEvenementCivil').val("");
		$('#quittancement.null.dateEvenement').val("");
		$('#quittancement.null.dateTraitement').val("");
		$('#error.global').val("");
		$('#noTechnique').val("");
		$('#numeroOFS').val("");
		$('#noIndividu').val("");
		$('#dateEvenement').val("");
		$('#typeEvenementCivil').get(0).selectIndex = 0;
		$('#typeEvenementCivil').change();
		return true;
	}
	
	
</script>


