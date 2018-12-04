<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>
<%--@elvariable id="delaisPM" type="ch.vd.unireg.param.online.DelaisOnlinePMView"--%>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">
	<tiles:put name="title">
		<fmt:message key="title.edit.param.delais.online.pm">
			<fmt:param>${delaisPM.annee}</fmt:param>
		</fmt:message>
	</tiles:put>

	<tiles:put name="body">
		<form:form action="edit.do" method="post" modelAttribute="delaisPM">
			<form:hidden path="periodeFiscaleId"/>
			<form:hidden path="annee"/>
			<fieldset class="information">
				<legend>
					<fmt:message key="label.param.params.delais.online.pm"/>
				</legend>
				<div style="margin: 4px"><a href="#" class="add" onclick="addLine()">Ajouter une ligne</a></div>

				<table id="delaisTable">
					<tr>
						<th rowspan="2"><fmt:message key="label.param.periode.demande.delai"/></th>
						<th colspan="2"><fmt:message key="label.param.demande.unitaire"/></th>
						<th colspan="2"><fmt:message key="label.param.demande.groupee"/></th>
						<th rowspan="2"><fmt:message key="label.actions"/></th>
					</tr>
					<tr>
						<th><fmt:message key="label.param.delai.1"/></th>
						<th><fmt:message key="label.param.delai.2"/></th>
						<th><fmt:message key="label.param.delai.1"/></th>
						<th><fmt:message key="label.param.delai.2"/></th>
						<th></th>
					</tr>
					<unireg:nextRowClass reset="0"/>
					<c:forEach var="periode" varStatus="ps" items="${delaisPM.periodes}">
					<tr id="lineIndex_${ps.index}" class="<unireg:nextRowClass/> delais">
						<td>
							<form:hidden id="id_${ps.index}" path="periodes[${ps.index}].id"/>
							<form:hidden id="index_${ps.index}" cssClass="index" path="periodes[${ps.index}].index"/>
							<form:input id="delaiDebut_${ps.index}" path="periodes[${ps.index}].delaiDebut"/><span class="mandatory" type="hidden" style="padding-left: 5px">*</span>
							<form:errors path="periodes[${ps.index}].delaiDebut" cssClass="error" delimiter=". "/>
						</td>
						<td>
							<form:input id="delai1DemandeUnitaire_${ps.index}" path="periodes[${ps.index}].delai1DemandeUnitaire"/>
							<form:errors path="periodes[${ps.index}].delai1DemandeUnitaire" cssClass="error" delimiter=". "/>
						</td>
						<td>
							<form:input id="delai2DemandeUnitaire_${ps.index}" path="periodes[${ps.index}].delai2DemandeUnitaire"/>
							<form:errors path="periodes[${ps.index}].delai2DemandeUnitaire" cssClass="error" delimiter=". "/>
						</td>
						<td>
							<form:input id="delai1DemandeGroupee_${ps.index}" path="periodes[${ps.index}].delai1DemandeGroupee"/>
							<form:errors path="periodes[${ps.index}].delai1DemandeGroupee" cssClass="error" delimiter=". "/>
						</td>
						<td>
							<form:input id="delai2DemandeGroupee_${ps.index}" path="periodes[${ps.index}].delai2DemandeGroupee"/>
							<form:errors path="periodes[${ps.index}].delai2DemandeGroupee" cssClass="error" delimiter=". "/>
						</td>
						<td>
							<a href="#" class="moveUp" onclick="moveUpLine(${ps.index})"></a>
							<a href="#" class="moveDown" onclick="moveDownLine(${ps.index})"></a>

							<a href="#" class="delete" style="margin-left: 30px" onclick="deleteLine(${ps.index})"></a>
						</td>
					</tr>
					</c:forEach>
				</table>

				<div style="margin: 10px"><b>Note:</b> Tous les délais sont à considérer depuis la date de bouclement de la PM ou depuis la date d'émission de la DI, en fonction du paramètrage général défini pour les DIs PM.</div>
			</fieldset>

			<input type="submit" value="<fmt:message key="label.bouton.mettre.a.jour"/>" />
			<input type="button" value="<fmt:message key="label.bouton.annuler"/>" onclick="Navigation.backTo(['/param/periode/list.do'], '/param/periode/list.do', 'id=${delaisPM.periodeFiscaleId}')" />

		</form:form>


		<div id="syntaxe">
			<table >
				<tr><th>Type</th><th>Syntaxe</th><th>Exemple</th></tr>
				<tr><td>Délai en jour</td><td>&lt;nombre&gt; jours</td><td>75 jours</td></tr>
				<tr><td>Délai en jour avec report de fin de mois</td><td>&lt;nombre&gt; jours~</td><td>75 jours~</td></tr>
				<tr><td>Délai en mois</td><td>&lt;nombre&gt; mois</td><td>6 mois</td></tr>
				<tr><td>Délai en mois avec report de fin de mois</td><td>&lt;nombre&gt; mois~</td><td>6 mois~</td></tr>
				<tr><td>Délai composé</td><td>&lt;délai 1&gt; + &lt;délai 2&gt;</td><td>6 mois + 75 jours</td></tr>
				<tr><td>Délai composé avec report de fin de mois</td><td>&lt;délai 1&gt; + &lt;délai 2&gt;</td><td>6 mois~ + 75 jours~</td></tr>
			</table>
		</div>

		<script>

			$(function() {
				$("#syntaxe").dialog({
					                    title: 'Syntaxe des délais',
					                    position: ['right', 'bottom'],
					                    width: 600
				                    });
			});

			const lineTemplate = '<tr id="lineIndex_@index" class="@oddeven delais">' +
				'<td><input id="id_@index" name="periodes[@index].id" type="hidden" value="">' +
				'<input id="index_@index" class="index" name="periodes[@index].index" type="hidden" value="@index">' +
				'<input id="delaiDebut_@index" name="periodes[@index].delaiDebut" type="text" value="">' +
				'<span class="mandatory" type="hidden" style="padding-left: 5px">*</span></td>\n' +
				'<td><input id="delai1DemandeUnitaire_@index" name="periodes[@index].delai1DemandeUnitaire" type="text" value=""></td>\n' +
				'<td><input id="delai2DemandeUnitaire_@index" name="periodes[@index].delai2DemandeUnitaire" type="text" value=""></td>\n' +
				'<td><input id="delai1DemandeGroupee_@index" name="periodes[@index].delai1DemandeGroupee" type="text" value=""></td>\n' +
				'<td><input id="delai2DemandeGroupee_@index" name="periodes[@index].delai2DemandeGroupee" type="text" value=""></td>\n' +
				'<td><a href="#" class="moveUp" onclick="moveUpLine(@index)"></a><a href="#" class="moveDown" onclick="moveDownLine(@index)"></a><a href="#" class="delete" style="margin-left: 30px" onclick="deleteLine(@index)"></a></td></tr>';

			function addLine() {
				// on ajoute la nouvelle ligne
				let body = $('#delaisTable').find('tbody');
				let count = body.find('tr.delais').length;
				let html = lineTemplate.replace(/@index/gi, count).replace(/@oddeven/, count % 2 === 0 ? 'even' : 'odd');
				body.append(html);
			}

			function deleteLine(index) {
				$('#lineIndex_' + index).remove();
				renumberLines();
			}

			function moveUpLine(index) {
				if (index === 0) {
					return;
				}

				$('#lineIndex_' + index).detach().insertBefore('#lineIndex_' + (index - 1));
				renumberLines();
			}

			function moveDownLine(index) {
				let body = $('#delaisTable').find('tbody');
				let count = body.find('tr.delais').length;

				if (index === count - 1) {
					return;
				}

				$('#lineIndex_' + index).detach().insertAfter('#lineIndex_' + (index + 1));
				renumberLines();
			}

			function renumberLines() {
				$('#delaisTable').find('tr.delais').each(function(index) {

					// on traite la ligne
					$(this).attr('id', $(this).attr('id').replace(/_\d+/gi, '_' + index));
					$(this).removeClass('odd even');
					$(this).addClass(index % 2 === 0 ? 'even' : 'odd');

					let columns = $(this).find('td');
					// on traite les inputs
					columns.find('input').each(function() {
						$(this).attr('id', $(this).attr('id').replace(/_\d+/gi, '_' + index));
						$(this).attr('name', $(this).attr('name').replace(/\[\d+]/gi, '[' + index + ']'));
					});
					// on traite les inputs d'index (cas spécial où la valeur doit aussi être modifiée)
					columns.find('input.index').each(function() {
						$(this).val($(this).val().replace(/\d+/gi, index));
					});
					// on traite les scripts
					columns.find('a').each(function() {
						$(this).attr('onclick', $(this).attr('onclick').replace(/\d+/gi, index));
					});
				});
			}
		</script>

	</tiles:put>
</tiles:insert>
