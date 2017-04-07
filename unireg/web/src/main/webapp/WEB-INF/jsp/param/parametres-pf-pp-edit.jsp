<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="index" value="${param.index}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="head">
		<style>
			span.bouton {
				width: 50%;
				text-align: center;
			}
			div.checkbox, div.emolument {
				margin: 10px;
			}
			div.emolument {
				height: 2em;
			}
		</style>
	</tiles:put>

	<tiles:put name="title">
		<fmt:message key="title.edit.param.periode.fiscale.pp">
			<fmt:param>${command.anneePeriodeFiscale}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
	<form:form name="form" id="formTermes" commandName="command">
		<form:hidden path="idPeriodeFiscale"/>
		<form:hidden path="anneePeriodeFiscale"/>
		<fieldset>
			<legend><fmt:message key="label.param.parametres-pf-edit" /></legend>

			<div class="checkbox">
				<c:set var="labelCheckbox">
					<fmt:message key="label.param.code.controle.sur.sommation.pp"/>
				</c:set>
				<form:checkbox path="codeControleSurSommationDI" label=" ${labelCheckbox}"/>
			</div>
			<div class="emolument">
				<c:set var="labelEmolument">
					<fmt:message key="label.param.emolument.sommation"/>
				</c:set>
				<form:checkbox path="emolumentSommationDI" label=" ${labelEmolument}" id="emolumentCheckBox" onchange="ParamPeriodePPEdit.onChangeEmolumentFlag();"/>
				<span id="montantEmolumentSpan" style="display: none; margin-left: 50px;">
					<form:input path="montantEmolumentSommationDI" maxlength="8" cssStyle="width: 8ex;"/>
					&nbsp;<fmt:message key="label.chf"/>
					<span style="color: red;">*</span>
					<form:errors path="montantEmolumentSommationDI" cssClass="error" cssStyle="margin-left: 20px;"/>
				</span>
			</div>

			<table>
			<tr>
				<th></th>
				<th><fmt:message key="label.param.entete.VD"/></th>
				<th><fmt:message key="label.param.entete.HC"/></th>
				<th><fmt:message key="label.param.entete.HS"/></th>
				<th><fmt:message key="label.param.entete.dep"/></th>
				<th><fmt:message key="label.param.entete.DS"/></th>
			</tr>
			<tr>
				<th><fmt:message key="label.param.som.reg"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireVaud" />
						<jsp:param name="id" value="sommationReglementaireVaud" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireHorsCanton" />
						<jsp:param name="id" value="sommationReglementaireHorsCanton" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireHorsSuisse" />
						<jsp:param name="id" value="sommationReglementaireHorsSuisse" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireDepense" />
						<jsp:param name="id" value="sommationReglementaireDepense" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationReglementaireDiplomate" />
						<jsp:param name="id" value="sommationReglementaireDiplomate" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				
			</tr>
			<tr>
				<th><fmt:message key="label.param.som.eff"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveVaud" />
						<jsp:param name="id" value="sommationEffectiveVaud" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveHorsCanton" />
						<jsp:param name="id" value="sommationEffectiveHorsCanton" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveHorsSuisse" />
						<jsp:param name="id" value="sommationEffectiveHorsSuisse" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveDepense" />
						<jsp:param name="id" value="sommationEffectiveDepense" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="sommationEffectiveDiplomate" />
						<jsp:param name="id" value="sommationEffectiveDiplomate" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
			</tr>
			<tr>
				<th><fmt:message key="label.param.masse.di"/></th>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIVaud" />
						<jsp:param name="id" value="finEnvoiMasseDIVaud" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIHorsCanton" />
						<jsp:param name="id" value="finEnvoiMasseDIHorsCanton" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIHorsSuisse" />
						<jsp:param name="id" value="finEnvoiMasseDIHorsSuisse" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIDepense" />
						<jsp:param name="id" value="finEnvoiMasseDIDepense" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
				<td>
					<jsp:include page="/WEB-INF/jsp/include/inputCalendar.jsp">
						<jsp:param name="path" value="finEnvoiMasseDIDiplomate" />
						<jsp:param name="id" value="finEnvoiMasseDIDiplomate" />
					</jsp:include>
					<span style="color: red;">*</span>
				</td>
			</tr>
		</table>
		</fieldset>
		<div>
			<span class="bouton">
				<input type="submit" id="maj" value="<fmt:message key="label.bouton.mettre.a.jour" />">
			</span>
			<span class="bouton">
				<input type="button" id="annuler" value="<fmt:message key="label.bouton.annuler" />" onclick="document.location.href='list.do?pf=${command.idPeriodeFiscale}'">
			</span>
		</div>

		<script type="application/javascript">
			var ParamPeriodePPEdit = {
				onChangeEmolumentFlag: function() {
					const checked = $('#emolumentCheckBox').is(':checked');
					const emolumentSpan = $('#montantEmolumentSpan');
					if (checked) {
						emolumentSpan.find(':input').removeAttr('disabled');
						emolumentSpan.show();
					}
					else {
						emolumentSpan.hide();
						emolumentSpan.find(':input').attr('disabled', 'disabled');
					}
				}
			};

			$(function() {
				ParamPeriodePPEdit.onChangeEmolumentFlag();
			});
		</script>
	</form:form>	
	</tiles:put>
</tiles:insert>
