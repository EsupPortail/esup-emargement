//insert before
function insertBefore(el, referenceNode) {
	referenceNode.parentNode.insertBefore(el, referenceNode);
}
//Random id
var ID = function() {
	return '_' + Math.random().toString(36).substr(2, 9);
};
var allSelect = selectAll;
var myChartBar = null;
var suneditor0 = null;
var selectCmp = null;
var sortDate;
//==jQuery remove()
function remove(id) {
	var elem = document.getElementById(id);
	if (elem != null) {
		return elem.parentNode.removeChild(elem);
	}
}

function backToTop() {
	document.body.scrollTop = 0;
	document.documentElement.scrollTop = 0;
}

//Convertion rgb-->hex
function rgb2hex(rgb) {
	rgb = rgb.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);
	return (rgb && rgb.length === 4) ? "#" +
		("0" + parseInt(rgb[1], 10).toString(16)).slice(-2) +
		("0" + parseInt(rgb[2], 10).toString(16)).slice(-2) +
		("0" + parseInt(rgb[3], 10).toString(16)).slice(-2) : '';
}

//affinage repartition
function displayAffinage(classes, isTiers) {
	var total = 0;
	classes.forEach(function(item) {
		var number = parseInt(item.value.replace(/\s/g, ''));
		total = (total + number);
	});
	var affinageId = null;
	var tempCapacite = null;
	var currentCapacite = null;
	if (isTiers) {
		affinageId = $("#affinageTiersTemps");
		tempCapacite = $("#tempCapaciteTiers");
		currentCapacite = $("#currentCapaciteTiers");
	} else {
		affinageId = $("#affinageNotTiersTemps");
		tempCapacite = $("#tempCapaciteNotTiers");
		currentCapacite = $("#currentCapaciteNotTiers");
	}
	affinageId.removeClass("bg-light");
	affinageId.removeClass("bg-success");
	affinageId.removeClass("bg-danger");
	affinageId.removeClass("text-white");
	affinageId.removeClass("text-dark");
	var realTotal = total / 2;
	if (realTotal == currentCapacite[0].textContent) {
		affinageId.addClass("bg-success text-white");
		$("#affinerButton").removeAttr("disabled");
	} else {
		affinageId.addClass("bg-danger text-white");
		$("#affinerButton").prop("disabled", "disabled");
	}

	tempCapacite.text(realTotal);
}

function displayToast() {
	$(".userToast").on("click", function(event) {
		const toastLiveExample = document.getElementById('liveToast');
		if (toastLiveExample != null) {
			const toast = new bootstrap.Toast(toastLiveExample);
			var splitField = this.getAttribute("data-whatever").split("//");
			$("#photoPresent").prop("src", emargementContextUrl + "/supervisor/" + splitField[0] + "/photo");
			var nom = "";
			var prenom = "";
			if (splitField[1] != "null") {
				nom = splitField[1];
				prenom = splitField[2];
			}
			$("#photoPresent").prop("alt", "Photo " + prenom + " " + nom);
			$("#nomPresence").text(nom);
			$("#prenomPresence").text(prenom);
			if (splitField[3] != "null") {
				$("#numIdentifiantPresence").text(splitField[3]);
			}
			if (splitField[4] != "null") {
				$("#codeEtape").text(splitField[4]);
			}
			toast.show();
		}
	});
}

function getCalendar(calendarEl, urlEvents, editable) {
	var calendar = new FullCalendar.Calendar(calendarEl, {
		headerToolbar: {
			start: 'prev,next today',
			center: 'title',
			end: 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'
		},
		navLinks: true, // can click day/week names to navigate views
		editable: editable,
		locale: 'fr',
		events: urlEvents,
		contentHeight: 600,
		themeSystem: 'bootstrap5'
	});

	calendar.render();
}

function createDateFromString(dateString) {
	const [day, month, year] = dateString.split('/').map(Number);
	const adjustedYear = year < 70 ? 2000 + year : 1900 + year;

	return new Date(adjustedYear, month - 1, day); // month is 0-indexed in JavaScript Date objects
}

function searchUsersAutocomplete(id, url, paramurl, maxItems) {
	var searchBox = document.getElementById(id);
	if (searchBox != null) {
		var awesomplete = new Awesomplete(searchBox, {
			minChars: 3,
			maxItems: maxItems,
			autoFirst: false
		});
		searchBox.addEventListener("keyup", function() {
			if (this.value.length > 2) {
				var request = new XMLHttpRequest();
				request.open('GET', url + "?searchValue=" + this.value + paramurl, true);
				request.onload = function() {
					if (request.status >= 200 && request.status < 400) {
						var data = JSON.parse(this.response);
						var list = [];
						data.forEach(function(value, key) {
							var labelNumEtu = "";
							var valueNumEtu = "";
							if (id == "searchTagCheck" || id == "searchUserApp") {
								var labelValue = "<strong>Nom : </strong>" + value.nom + "<strong class='ms-2'>Prénom : </strong>" + value.prenom + "<strong class='ms-2'>Eppn : </strong>" + value.eppn + labelNumEtu;
								list.push({
									label: labelValue,
									value: value.eppn + "//" + value.nom + "//" + value.prenom + valueNumEtu
								});
							} else if (id == "searchIndividuTagCheck" || id == "searchIndividuTagChecker" || id == "searchIndividu") {
								var labelValue = "<strong>Nom : </strong>" + value.nom + "<strong class='ms-2'>Prénom : </strong>" + value.prenom + "<strong class='ms-2'>Identifiant : </strong>" + value.identifiant + labelNumEtu
									+ "<strong class='ms-2'>Type : </strong>" + value.typeObject;
								list.push({
									label: labelValue,
									value: value.identifiant + "//" + value.nom + "//" + value.prenom + valueNumEtu
								});
							}
							else if (id == "searchIndividuGroupe") {
								var labelValue = "<strong>Nom : </strong>" + value.groupe.nom + "<strong> Année : </strong>" + value.groupe.anneeUniv
								list.push({
									label: labelValue,
									value: value.groupe.id + "//" + value.groupe.nom
								});
							}
							else if (id == "searchLocation") {
								var labelValue = "<strong>Nom : </strong>" + value.nom + "<strong class='ms-2'>Site : </strong>" + value.campus.site + "<strong class='ms-2'>Adresse : </strong>" +
									value.adresse;
								list.push({
									label: labelValue,
									value: value.nom + "//" + value.campus.site + "//" + value.adresse
								});
							} else if (id == "searchSessionEpreuve") {
								var realDate = value.dateExamen.substring(0, 10);
								var splitDate = realDate.split("-");
								var frDate = splitDate[2] + "-" + splitDate[1] + "-" + splitDate[0];
								var labelValue = "<strong>Nom : </strong>" + value.nomSessionEpreuve + "<strong class='ms-2'>Site : </strong>" + value.campus.site + "<strong class='ms-2'>Date : </strong>" +
									frDate;
								list.push({
									label: labelValue,
									value: value.id
								});
							} else if (id == "searchGroup") {
								list.push(value);
							} else {
								if (value.numEtudiant != null) {
									labelNumEtu = "<strong class='ms-2'>N° Identifiant : </strong>" + value.numEtudiant;
									valueNumEtu = "//" + value.numEtudiant
								}
								if (value.name != null) {
									var labelValue = "<strong>Individu: </strong>" + value.name + " " + value.prenom + "<strong class='ms-2'>Eppn : </strong>" + value.eppn + labelNumEtu;
									list.push({
										label: labelValue,
										value: value.eppn + "//" + value.name + "//" + value.prenom + valueNumEtu
									});
								}
								if (value.numIdentifiant != null) {
									labelNumEtu = "<strong class='ms-2'>N° Identifiant : </strong>" + value.numIdentifiant;
									valueNumEtu = "//" + value.numEtudiant
								}
							}
						});
						awesomplete.list = list;
					} else {
						console.log("erreur du serveur!");
					}
				};
				request.send();
			}
		});
	}
}

function changeSelectSessionEpreuve2(id, id2, url, change) {
	var selectOrigin = document.getElementById(id);
	var selectToPopulate = document.getElementById(id2);
	var request = new XMLHttpRequest();
	if (selectOrigin.value != "") {
		request.open('GET', url + "?sessionEpreuve=" + selectOrigin.value, true);
		request.onload = function() {
			if (request.status >= 200 && request.status < 400) {
				selectToPopulate.innerHTML = "";
				var data = JSON.parse(this.response);
				if (data.length == 0) {
					$(".alertmsg").remove();
					$("#" + id2).closest(".col").append("<p class='text-danger font-weight-bold alertmsg'>Aucun lieu associé à cette session</p>");
					option = document.createElement('option');
					option.value = "";
					option.textContent = "";
					selectToPopulate.appendChild(option);
					$("#" + id2).prop("disabled", true);
					$("#submitForm").prop("disabled", true);
				} else {
					option = document.createElement('option');
					option.value = "";
					option.textContent = "";
					option.setAttribute("data-placeholder", "true");
					selectToPopulate.appendChild(option);
					data.forEach(function(value, key) {
						$(".alertmsg").remove();
						$("#" + id2).removeAttr("disabled");
						$("#submitForm").removeAttr("disabled");
						option = document.createElement('option');
						if (currentLocation !== undefined) {
							if (value.id == currentLocation) {
								option.selected = true;
							}
						}
						option.value = value.id;
						option.textContent = value.location.nom;
						selectToPopulate.appendChild(option);
					});
					if (change && data.length == 1) {
						var redirect = "?sessionEpreuve=" + selectOrigin.value + "&location=" + data[0].id;
						window.location.href = redirect;
					}
				}
			} else {
				console.log("erreur du serveur!");
			}
		};
		request.send();
	}
}
//Configs
function displayFormconfig(val, valeur) {
	var checkTrue = "";
	var checkFalse = "";
	if (valeur == "true") {
		checkTrue = "checked='checked'";
	} else if (valeur == "false") {
		checkFalse = "checked='checked'";
	}
	if (checkTrue == "" && checkFalse == "" && val == "BOOLEAN") {
		checkTrue = "checked='checked'";
	}
	var bool = '<div class="col-lg-10"><div id="boolGroup"><label class="radio-inline"><input type="radio" name="value" id="boolTrue" value="true"' + checkTrue + '/> True' +
		'</label><label class="radio-inline ml-2"><input type="radio" name="value" id="boolFalse" value="false" ' + checkFalse + ' /> False</label></div></div>';

	switch (val) {
		case 'HTML':
			remove("boolGroup");
			remove("valeur");
			remove("suneditor_valeur");
			$("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'></textarea></div>");
			suneditor0 = createSunEditor('valeur');
			suneditor0.setContents(valeur);
			break;
		case 'TEXT':
			if (suneditor0 != null) {
				suneditor0 = suneditor0.hide();
			}
			remove("suneditor_valeur");
			remove("boolGroup");
			remove("valeur");
			$("#suneditor").closest("div").after("<div class='col-lg-10'><textarea class='form-control' id='valeur' name='value'>" + valeur + "</textarea></div>");
			break;
		case 'BOOLEAN':
			if (suneditor0 != null) {
				suneditor0 = suneditor0.hide();
			}
			remove("boolGroup");
			remove("suneditor_valeur");
			remove("valeur");
			$("#suneditor").closest("div").after(bool);
			break;
	}
}

function deleteParam(urlLocation, name) {
	const [head, tail] = urlLocation.split('?');
	var url = head + '?' + tail.replace(new RegExp(`&${name}=[^&]*|${name}=[^&]*&`), '');
	return url;
}

function updatePresence(url, numEtu, currentLocation) {
	var request = new XMLHttpRequest();
	var location = (currentLocation != null) ? "&currentLocation=" + currentLocation : "";
	request.open('GET', url + "?presence=" + numEtu + location, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			if (document.getElementById("newbie") != null) {
				const data = JSON.parse(request.responseText);
				if (data != null) {
					var tc = data[0];
					var tagDate = moment(tc.tagDate).format('DD-MM-YYYY HH:mm:ss');
					var info = moment(tc.sessionEpreuve.dateExamen).format('DD-MM-YYYY') + " // " +
						tc.sessionEpreuve.nomSessionEpreuve + " // " + tc.sessionLocationBadged.location.nom;
					$("#norole").addClass("d-none");
					$("#newbie").removeClass("d-none");
					$("#tagDate").text(tagDate);
					$("#sessionNewbie").text(info);
					$("#accordionNewbie").hide();
				}
			}
			deleteParam(window.location.href, "tc");
			setTimeout(function() {
				$("#displayedIdentity").addClass("d-none");
			}, 2000);
		} else {
			console.log("erreur du serveur!");
		}
	};
	request.send();
}

//Revup param GET
function $_GET(param) {
	var vars = {};
	window.location.href.replace(location.hash, '').replace(
		/[?&]+([^=&]+)=?([^&]*)?/gi, // regexp
		function(m, key, value) { // callback
			vars[key] = value !== undefined ? value : '';
		}
	);

	if (param) {
		return vars[param] ? vars[param] : null;
	}
	return vars;
}

//Configs
function createSunEditor(id) {
	const editor = SUNEDITOR.create(id, {

		// height/width of the editor
		width: '100%',
		height: '150px',

		// show/hide toolbar icons
		buttonList: [
			['font', 'fontSize', 'formatBlock'],
			['bold', 'underline', 'italic'],
			['fontColor', 'hiliteColor'],
			['align', 'horizontalRule', 'list', 'link', 'image'],
			['codeView']
		]
	});
	return editor;
}

//Ajax count nb Etu
function countItem(apogeeBean, countUrl, type) {
	$.ajax({
		url: countUrl,
		contentType: "application/json",
		data: apogeeBean,
		success: function(data) {
			if (type == "matiere") {
				$("#nbInscritsMatiere").text("[" + data + "]");
			}
			if (type == "groupe") {
				$("#nbInscritsGroupe").text("[" + data + "]");
			}
			if (type == "composante") {
				$("#nbInscritsComposante").text("[" + data + "]");
			}
			if (type == "diplome") {
				$("#nbInscritsEtp").text("[" + data + "]");
			}
		}
	});
}

//Recherches...
function submitSearchForm(id, url, endUrl) {
	var search = document.getElementById(id);
	if (search != null) {
		search.onfocus = function() {
			this.value = "";
		};
		searchUsersAutocomplete(id, url, endUrl, 100);
		var formSearch = document.getElementById("formSearch");
		search.addEventListener("awesomplete-selectcomplete", function(event) {
			var splitResult = this.value.split("//");
			search.value = splitResult[0].toString().trim();
			formSearch.submit();
		});
	}
}

//Couleurs pour graphiques stats
var seed = 11;

function random() {
	var x = Math.sin(seed++) * 1000;
	return x - Math.floor(x);
}
var generateColors = [];
var generateBorderColors = [];
var generateStackColors = [];
for (var k = 0; k < 100; k++) {
	var color = Math.floor(random() * 256) + ',' + Math.floor(random() * 256) + ',' + Math.floor(random() * 256);
	generateColors.push('rgba(' + color + ', 0.6)');
	generateStackColors.push('rgba(' + color + ', 0.6)');
	generateBorderColors.push('rgba(' + color + ', 1)');
}
var monthsArray = ["Sept", "Oct", "Nov", "Déc", "Jan", "Fev", "Mar", "Avr", "Mai", "Juin", "Juil", "Août"];
const moisArray = [[9, 'Sept'], [10, 'Oct'], [11, 'Nov'], [12, 'Déc'], [1, 'Jan'], [2, 'Fev'],
[3, 'Mar'], [4, 'Avr'], [5, 'Mai'], [6, 'Juin'], [7, 'Juil'], [8, 'Août']
];
let moisMap = new Map(moisArray);

function chartNoData(ctx, chart) {
	ctx.save();
	ctx.textAlign = 'center';
	ctx.textBaseline = 'middle';
	ctx.font = "22px Arial";
	ctx.fillStyle = "gray";
	ctx.fillText('Aucune donnée disponible', chart.width / 2, chart.height / 2);
	ctx.restore();
}

//affiche stats
function getStats(year, param, url, id, chartType, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, datalabels) {
	Chart.register({
		id: id,
		afterDraw: function(chart) {
			if (chart.data.datasets.length === 0 || (chart.data.datasets.length != 0 && chart.data.datasets[0].data.length === 0)) {
				var ctx = chart.ctx;
				chartNoData(ctx, chart);
			}
		}
	});
	var request = new XMLHttpRequest();
	var paramUrl = (param != null) ? '&' + param : '';
	request.open('GET', emargementContextUrl + "/" + url + "/stats/json?&anneeUniv=" + year + "&type=" + id + paramUrl, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			if (this.response != "") {
				var data = JSON.parse(this.response);
				if ((Object.keys(data[id]).length) > 0) {
					if (chartType == "multiBar") {
						multiChartStackBar(data[id], id, 3, transTooltip, formatDate, 'linear');
					} else if (chartType == "chartBar") {
						chartBar(data[id], label1, id, transTooltip, formatDate, data[data2], label2);
					} else if (chartType == "pie") {
						chartPieorDoughnut(data[id], id, chartType, option, datalabels);
					} else if (chartType == "doughnut") {
						chartPieorDoughnut(data[id], id, chartType, option, datalabels);
					} else if (chartType == "lineChart") {
						lineChart(data[id], id, fill, arrayDates, formatDate);
					}
				}
			}
		}
	}
	request.send();
}

function lineChart(data, id, fill, arrayDates, formatDate) {
	if (document.getElementById(id) != null) {
		let inlineValeurs = [];
		var inlineDatasets = [];
		var a = 8;
		const valeursArray = [["9", 0], ["10", 0], ["11", 0], ["12", 0], ["1", 0],
		["2", 0], ["3", 0], ["4", 0], ["5", 0], ["6", 0], ["7", 0], ["8", 0]
		];
		const mapArray = new Map(valeursArray);
		//dates
		var dates = arrayDates;
		for (i = 0; i < data[0].length; i++) {
			mapArray.set(data[0][i], data[1][i]);
			inlineValeurs = Array.from(mapArray.values());
		}
		inlineDatasets.push({
			label: '',
			backgroundColor: generateColors[a],
			borderColor: generateColors[a],
			pointBorderColor: "#fff",
			pointHoverBorderColor: "#fff",
			pointBackgroundColor: generateColors[a],
			data: inlineValeurs,
			spanGaps: true,
			fill: fill,
		});
		var dateLabels = dates;
		if (formatDate) {
			dateLabels = [];
			for (var ind in dates) {
				dateLabels.push(formatDateString(dates[ind]));
			};
		}
		var dataMois = {
			labels: dateLabels,
			datasets: inlineDatasets
		};
		var ctx3 = document.getElementById(id).getContext("2d");
		new Chart(ctx3, {
			type: 'line',
			data: dataMois,
		});
	}
}

function chartPieorDoughnut(data, id, type, option, datalabels) {
	if (document.getElementById(id) != null) {
		if (typeof datalabels == "undefined") {
			datalabels = false;
		}
		var legend = true;
		if (option == "legend") {
			legend = false;
		}
		var dataSets = [];
		var doughnutDataArray = [];
		dataSets.push({
			data: data[1],
			backgroundColor: generateStackColors,
			hoverBackgroundColor: generateColors
		});
		var doughnutDataArray = {
			labels: data[0],
			datasets: dataSets
		};
		var ctx3 = document.getElementById(id).getContext("2d");
		new Chart(ctx3, {
			type: type,
			data: doughnutDataArray,
			options: {
				responsive: true, animateRotate: false,
				legend: {
					display: legend
				},
				plugins: {
					datalabels: {
						backgroundColor: function(context) {
							return context.dataset.backgroundColor;
						},
						borderColor: 'white',
						borderRadius: 25,
						borderWidth: 2,
						color: 'white',
						display: function(context) {
							var dataset = context.dataset;
							var count = dataset.data.length;
							var value = dataset.data[context.dataIndex];
							return value > count * 1.5;
						},
						font: {
							weight: 'bold'
						},
						formatter: Math.round
					},
					tooltip: {
						enabled: true,
						callbacks: {
							label: function(context) {
								let total1 = context.dataset.data.reduce((x, y) => x + y)
								var currentValue = context.parsed;
								var percentage = ((currentValue / total1) * 100).toFixed(1);
								var label = ' ' + context.label || '';
								if (label) {
									label += ' : ';
								}
								label += currentValue + " (" + percentage + " %)";

								return label;
							}
						}
					}
				}
			}
		});
	}
}

function multiChartStackBar(allData, id, start, transTooltip, formatDate, scaleType) {
	if (document.getElementById(id) != null) {
		const footer = (tooltipItems) => {
			let sum = 0;

			tooltipItems.forEach(function(tooltipItem) {
				sum += tooltipItem.parsed.y;
			});
			return 'Total: ' + sum;
		};
		var dataSets = [];
		var k = 0;
		for (key in allData[1]) {
			dataSets.push({
				label: key,
				data: allData[1][key],
				backgroundColor: generateStackColors[k]
			});
			k++;
		}
		var barChartData = {
			labels: allData[0],
			datasets: dataSets
		}
		var ctx = document.getElementById(id).getContext("2d");
		new Chart(ctx, {
			type: 'bar',
			data: barChartData,
			options: {
				responsive: true,
				interaction: {
					mode: 'index'
				},
				legend: {
					display: true
				},
				tooltip: {
					callbacks: {
						footer: footer,
					}
				},
				scales: {
					x: {
						stacked: true,
					},
					y: {
						stacked: true
					}
				},
				plugins: {
					tooltip: {
						callbacks: {
							footer: footer,
						}
					}
				}
			}
		});
	}
}
function chartBar(data1, label1, id, transTooltip, formatDate, data2, label2) {
	if (document.getElementById(id) != null) {
		var listLabels = [];
		var listValeurs = [];
		var listTooltipLabels = [];
		var datasets = [{
			label: label1,
			backgroundColor: generateColors[3],
			borderColor: generateBorderColors[3],
			borderWidth: 1,
			data: data1[1],
			datalabels: {
				display: true
			}
		}];
		if (data2 != null) {
			var listValeurs2 = [];
			for (var idx2 in data2) {
				listValeurs2.push(data2[idx2]);
			};
			datasets.push({
				label: label2,
				backgroundColor: generateColors[4],
				borderColor: generateBorderColors[4],
				borderWidth: 1,
				data: listValeurs2,
				datalabels: {
					display: true
				}
			})
		}
		var barChartData = {
			labels: data1[0],
			datasets: datasets
		}
		var ctx = document.getElementById(id).getContext("2d");
		new Chart(ctx, {
			type: 'bar',
			data: barChartData,
			options: {
				responsive: true,
				plugins: {
					legend: {
						display: false
					}
				},
				scales: {
					y: {
						ticks: {
							beginAtZero: true
						}
					}
				}
			}
		});
	}
}
//Select event
function setSelectEvent(id, idEvent) {
	var url = emargementContextUrl + "/manager/event/selectEvent/";
	var request = new XMLHttpRequest();
	request.open('GET', url + id, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			$(idEvent).html();
			$(idEvent).html(this.response);
			new SlimSelect({
				select: '#icsEvents'
			})
		} else {
			console.log("erreur du serveur!");
		}
	};
	request.send();
}
//Choix lieu import
function getLocations(type) {
	$('#formImportCascading' + type).cascadingDropdown({
		selectBoxes: [{
			selector: '.step1Import'
		}, {
			selector: '.step2Import',
			requires: ['.step1Import'],
			source: function(request, response) {
				$.getJSON(emargementContextUrl + "/manager/extraction/searchLocations", request, function(data) {
					response($.map(data, function(item, index) {
						return {
							label: item.location.nom + " (" + item.capacite + ")",
							value: item.id,
						};
					}));
				});
			},
		}],
		onChange: function(event, value, requiredValues, requirementsMet) {
			var sessionEpreuveCsv = $('#sessionEpreuve' + type).val();
		},
		onReady: function(event, value, requiredValues, requirementsMet) {
			$('#sessionLocation' + type).prop("disabled", false)
		}
	});
}
//Tri date ade Campus
function datesSorter(a, b) {
	var splitDate1 = a.split("-");
	var splitDate2 = b.split("-");
	var date1 = new Date(splitDate1[2], splitDate1[1], splitDate1[0]);
	var date2 = new Date(splitDate2[2], splitDate2[1], splitDate2[0]);
	if (date1 > date2) return 1;
	if (date1 < date2) return -1;
	return 0;
}

function getDateFromStringDateTime(str) {
	var splitTime = str.split(" ");
	var splitDate = splitTime[0].split("-");
	temp = splitDate[2] + "-" + splitDate[1] + "-" + splitDate[0];
	return new Date(temp + "T" + splitTime[1]);
}

function datesSorter2(a, b) {
	var noDate = '1970-01-01T00:00';
	var date1 = (a == "") ? new Date(noDate) : getDateFromStringDateTime(a);
	var date2 = (b == "") ? new Date(noDate) : getDateFromStringDateTime(b);
	if (date1 > date2) return 1;
	if (date1 < date2) return -1;

	return 0;
}

//jstree
function openAndCheckNodes(nodeIds) {
	$.each(nodeIds, function(index, nodeId) {
		var node = $('#jstree').jstree(true).get_node(nodeId);
		if (node) {
			$('#frmt').jstree(true).check_node(node);
			$('#frmt').jstree(true).open_node(node);
		}
	});
}

function updateJsTree(selectedData, category) {
	var selectedUrl = emargementContextUrl + "/manager/adeCampus/json?category=" + category + "&fatherId=" + selectedData;
	var request = new XMLHttpRequest();
	request.open('GET', selectedUrl, true);
	request.onload = function() {
		if (request.status >= 200 && request.status < 400) {
			const data = JSON.parse(request.responseText);
			$('#frmt').jstree(true).settings.core.data = data;
			$('#frmt').jstree(true).refresh();
			$('#frmt').removeClass("d-none");
			$("#spinnerComps").addClass("d-none");
		}
	}
	request.send();
}

function getCapacite(location) {
	$.ajax({
		type: 'GET',
		url: emargementContextUrl + "/manager/sessionLocation/searchCapacite?id=" + addSessionLocation.value,
		success: function(response) {
			$("#capacite").val(response);
		},
		error: function(error) {
			console.log("Error: " + error);
		}
	});
}

function getQrCodeSession(url, idImg) {
	$.ajax({
		url: url,
		method: 'GET',
		success: function(response) {
			$("#" + idImg).attr("src", "data:image/png;base64, " + response);
		},
		error: function(xhr, status, error) {
			console.error('Error:', error);
		}
	});
}

function initTablePresence(sortDate){
	new DataTable("#tablePresence", {
		responsive: true,
		ordering: true,
		paging: false,
		searching: true,
		info: false,
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}, order: [
			sortDate // Adjust the column index if needed
		],
		columnDefs: [
			{
				targets: 4,
				type: 'datetime-moment', // Use the datetime-moment plugin
				render: function(data, type, row) {
					if (!data) { // Check if data is empty
						return ''; // Return empty string if data is empty
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YY hh:mm:ss').unix();
					}
					return moment(data, 'DD/MM/YY hh:mm:ss').format('DD/MM/YY hh:mm:ss');
				}
			}
		]
	});
}

//==jQuery document.ready
document.addEventListener('DOMContentLoaded', function() {
	//Autocomplete
	var userAppEppn = document.getElementById("eppn");
	var numIdentifiant = document.getElementById("numIdentifiant");
	var superAdmin = document.getElementById("searchSuperAdmin");
	var searchSuEppn = document.getElementById("searchSuEppn");

	if (userAppEppn != null) {
		if (superAdmin != null) {
			searchUsersAutocomplete("eppn", emargementContextUrl + "/superadmin/admins/searchUsersLdap", "", 100);
		} else {
			searchUsersAutocomplete("eppn", emargementContextUrl + "/admin/userApp/searchUsersLdap", "", 100);
		}
		userAppEppn.addEventListener("awesomplete-selectcomplete", function(e) {
			var splitEppn = this.value.split("//");
			userAppEppn.value = splitEppn[0].toString().trim();
		});
	}
	//input num Etu
	if (numIdentifiant != null) {
		searchUsersAutocomplete("eppn", emargementContextUrl + "/manager/tagCheck/searchUsersLdap", "", 100);
		if (userAppEppn != null) {
			userAppEppn.addEventListener("awesomplete-selectcomplete", function(e) {
				var splitEppn = this.value.split("//");
				userAppEppn.value = splitEppn[0].toString().trim();
				numIdentifiant.value = (typeof splitEppn[3] == "undefined") ? "" : splitEppn[3].toString().trim();
			});
		}
	}

	//select presence
	var presenceForm = document.getElementById("presenceForm");
	if (presenceForm != null) {
		var selectSessionEpreuve = document.getElementById("sessionEpreuvePresence");
		changeSelectSessionEpreuve2("sessionEpreuvePresence", "location", emargementContextUrl + "/supervisor/sessionLocation/searchSessionLocations", false);
		selectSessionEpreuve.addEventListener("change", function() {
			changeSelectSessionEpreuve2("sessionEpreuvePresence", "location", emargementContextUrl + "/supervisor/sessionLocation/searchSessionLocations", true);
		});
	}

	//Messages modal bilan CSv
	var dialogMsg = document.querySelector('#modalBilanCsvBody');
	if (dialogMsg != null) {
		$('#modalBilanCsv').modal('show');
	}
	var modalMsg = document.querySelector('#modalMsg');
	if (modalMsg != null) {
		$('#modalMsg').modal('show');
	}
	var idArray = [];
	var checkboxes = document.querySelectorAll('.case');
	var selectAll = document.getElementById("selectall");
	var listeIds = document.getElementById("listeIds");
	if (selectAll != null) {
		selectAll.addEventListener("click", function() {
			idArray = [];
			for (var i = 0, n = checkboxes.length; i < n; i++) {
				if (!checkboxes[i].disabled) {
					checkboxes[i].checked = this.checked;
				}
			}
			idArray = Array.from(document.querySelectorAll("input[name='case']")).filter(function(checkbox) {
				return checkbox.checked;
			}).map(function(checkbox) {
				return checkbox.value;
			});
			if (listeIds != null) {
				listeIds.value = idArray;
			}
		});

		Array.from(checkboxes).forEach(function(link) {
			return link.addEventListener('click', function(event) {
				if (idArray.indexOf(this.value) < 0) {
					idArray.push(this.value);
				} else {
					idArray.splice(idArray.indexOf(this.value), 1);
				}
				if (!this.checked) {
					selectAll.checked = false;
				}
				if (listeIds != null) {
					listeIds.value = idArray;
				}
			});
		});
	}

	var suneditor = document.getElementById("pdfArea");
	var editor1 = null;
	if (suneditor != null) {
		editor1 = createSunEditor('pdfArea');
		editor2 = createSunEditor('emailArea');
	}

	//Click events
	document.addEventListener('click', function(event) {
		if (event.target.matches('.presenceCheck')) {
			var isPresent = event.target.checked;
			var isPresentValue = isPresent + ',' + event.target.value;
			updatePresence(emargementContextUrl + "/updatePresents", isPresentValue, null);
		}
		if (event.target.matches('#pdfPreview')) {
			var htmltemplate = document.getElementById("htmltemplate");
			htmltemplate.value = editor1.getContents();
			var formPdfConvoc = document.getElementById("formPdfConvoc");
			formPdfConvoc.submit();
		}
		if (event.target.matches('#submitEmailConvocation')) {
			event.preventDefault();
			var bodyMsg = document.getElementById("bodyMsg");
			bodyMsg.value = editor2.getContents();
			var htmltemplatePdf = document.getElementById("htmltemplatePdf");
			htmltemplatePdf.value = editor1.getContents();
			var sendConvocation = document.getElementById("sendConvocation");
			var subject = document.getElementById("subject");
			var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
			if (subject.value.length == 0) {
				alert("Vous devez rentrer un sujet");
			} else if (!isPreviewPdfOk.checked) {
				alert("Vous devez valider le PDF de convocation");
			} else {
				sendConvocation.submit();
			}
		}
		if (event.target.matches('#submitEmailConvsignes')) {
			event.preventDefault();
			var bodyMsg = document.getElementById("bodyMsg");
			bodyMsg.value = editor2.getContents();
			var htmltemplatePdf = document.getElementById("htmltemplatePdf");
			htmltemplatePdf.value = editor1.getContents();
			var sendConvocation = document.getElementById("sendConsignes");
			var subject = document.getElementById("subject");
			var isPreviewPdfOk = document.getElementById("isPreviewPdfOk");
			if (subject.value.length == 0) {
				alert("Vous devez rentrer un sujet");
			} else if (!isPreviewPdfOk.checked) {
				alert("Vous devez valider le PDF de consignes");
			} else {
				sendConvocation.submit();
			}
		}

	}, false);

	//Configs
	var boolGroup = document.getElementById("boolGroup");
	if (boolGroup != null) {
		boolGroup.style.display = "none";
	}
	var valeur = document.getElementById("hiddenValeur");
	if (valeur != null) {
		var typeConfig = document.querySelector("input[type=radio][name=type]:checked");
		if (typeConfig != null) {
			displayFormconfig(typeConfig.value, valeur.value);
		}

		var radioType = document.querySelectorAll('input[type=radio][name=type]');
		Array.from(radioType).forEach(function(link) {
			return link.addEventListener('click', function(event) {
				displayFormconfig(this.value, valeur.value);
			});
		});
	}

	//configs
	var appliConfig = document.getElementById('configForm');
	if (appliConfig != null) {
		appliConfig.addEventListener('submit', function(e) {
			e.preventDefault();
			var radioType = document.querySelectorAll('input[type=radio][name=type]:checked')[0].value;
			if (radioType == "HTML") {
				document.getElementById("valeur").value = suneditor0.getContents();
			}
			appliConfig.submit();
		});
	}
	//submit export inscrits
	var submitExport = document.getElementById('submitExport');
	if (submitExport != null) {
		var importBtn = document.getElementById('import');
		$(".statusExport").hide();
		importBtn.addEventListener('click', function(e) {
			var sessionEpreuve = document.getElementById('sessionEpreuve');
			if (sessionEpreuve.value == "") {
				e.preventDefault();
				alert("Vous devez choisir une Session!");
			} else {
				$(".statusExport").show();
				submitExport.submit();
			}
		});

		$('#formApogee').cascadingDropdown({
			selectBoxes: [{
				selector: '.step1'
			}, {
				selector: '.step2'
			}, {
				selector: '.step3',
			}, {
				selector: '.step4',
				requires: ['.step1', '.step2', '.step3'],
				source: function(request, response) {
					$.getJSON(emargementContextUrl + "/manager/extraction/search/diplome", request, function(data) {
						$("#nbCodEtp").text("[" + data.length + "]");
						response($.map(data, function(item, index) {
							return {
								label: item.libEtp,
								value: item.codEtp,
								selected: index == 0 // Select first available option

							};
						}));
					});
				},
			}, {
				selector: '.step5',
				requires: ['.step1', '.step2', '.step3', '.step4'],
				source: function(request, response) {
					$.getJSON(emargementContextUrl + "/manager/extraction/search/matiere", request, function(data) {
						$("#nbCodElp").text("[" + data.length + "]");
						response($.map(data, function(item, index) {

							return {
								label: item.libElp + " (" + item.codElp + ")",
								value: item.codElp,
								selected: index == 0 // Select first available option

							};
						}));
					});
				},
			}, {
				selector: '.step6',
				requires: ['.step1', '.step2', '.step3', '.step4', '.step5'],
				source: function(request, response) {
					$.getJSON(emargementContextUrl + "/manager/extraction/search/groupe", request, function(data) {
						$("#nbCodExtGpe").text("[" + data.length + "]");

						response($.map(data, function(item, index) {
							return {
								label: item.libGpe + " (" + item.codExtGpe + ")",
								value: item.codExtGpe,
								selected: index == 0 // Select first available option

							};
						}));
					});
				},
			}],
			onChange: function(event, value, requiredValues, requirementsMet) {
				var apogeeBean = {
					"codCmp": codCmp.value,
					"codAnu": codAnu.value,
					"codEtp": codEtp.value,
					"codElp": codElp.value,
					"codSes": codSes.value,
					"codExtGpe": codExtGpe.value
				};
				var countUrlGroupe = emargementContextUrl + "/manager/extraction/countAutorises/groupe";
				var countUrlDiplome = emargementContextUrl + "/manager/extraction/countAutorises/diplome";
				var countUrlComposante = emargementContextUrl + "/manager/extraction/countAutorises/composante";
				var countUrl = emargementContextUrl + "/manager/extraction/countAutorises/matiere";

				countItem(apogeeBean, countUrlDiplome, "diplome");
				countItem(apogeeBean, countUrl, "matiere");
				countItem(apogeeBean, countUrlGroupe, "groupe");
				countItem(apogeeBean, countUrlComposante, "composante");
			}
		});
	}

	//Choix lieu import
	getLocations("Ldap");
	getLocations("Csv");
	getLocations("Groupe");
	getLocations("");

	var importUsersLdapform = document.getElementById('importUsersLdapform');
	if (importUsersLdapform != null) {
		var importBtn = document.getElementById('importLdap');
		var nbSelected = $("#usersGroupLdap option:selected").length;
		$(".statusExportLdap").hide();
		importBtn.addEventListener('click', function(e) {
			var sessionEpreuveLdap = document.getElementById('sessionEpreuveLdap');
			if (sessionEpreuveLdap.value == "") {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				if (nbSelected > 0) {
					$(".statusExportLdap").show();
					importUsersLdapform.submit();
				}
			}
		});
	}
	var importGroupe = document.getElementById('importGroupe');
	$(".statusExportGroupe").hide();
	if (importGroupe != null) {
		importGroupe.addEventListener('click', function(e) {
			var sessionEpreuve = document.getElementById('sessionEpreuveGroupe');
			if (sessionEpreuve.value == "") {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				$(".statusExportGroupe").show();
				submitExport.submit();
			}
		});
	}
	var importCsv = document.getElementById('importCsv');
	$(".statusExportCsv").hide();
	if (importCsv != null) {
		importCsv.addEventListener('click', function(e) {
			var sessionEpreuve = document.getElementById('sessionEpreuveCsv');
			if (sessionEpreuve.value == "") {
				e.preventDefault();
				alert("Vous devez choisr une Session!");
			} else {
				$(".statusExportCsv").show();
				submitExport.submit();
			}
		});
	}
	$('#selectAll').on("click", function() {
		var selectedItems = 0;
		var nbSelected = $("#usersGroupLdap option:selected").length;
		$('#usersGroupLdap option').each(function() {
			if (!this.disabled && this.value != '') {
				$(this).prop('selected', (nbSelected == 0) ? true : false);
				selectedItems = $(this).prop('selected') ? $("#ldapMembersSize").text() : 0;
			}
		});
		$("#selectedUsers").text(selectedItems);
	});

	$('#usersGroupLdap option').on("click", function() {
		nbSelected = $("#usersGroupLdap option:selected").length;
		$("#selectedUsers").text(nbSelected);
	});

	// tableau inscrits
	var tempsAmenage = document.getElementById('tempsAmenage');
	var repartition = document.getElementById('repartition');
	if (tempsAmenage != null) {
		tempsAmenage.addEventListener('change', function(e) {
			document.getElementById('formSearch').submit();
		});
	}
	if (repartition != null) {
		repartition.addEventListener('change', function(e) {
			document.getElementById('formSearch').submit();
		});
	}

	//Recherches
	if (document.getElementById("searchTagCheck") != null) {
		//     var sessionId = document.getElementById("sessionId");
		//   submitSearchForm("searchTagCheck", emargementContextUrl + "/manager/tagCheck/searchTagCheck", "&sessionId=" + sessionId.value);
	} else if (document.getElementById("searchUserApp") != null) {
		submitSearchForm("searchUserApp", emargementContextUrl + "/admin/userApp/search", "");
	} else if (document.getElementById("searchLocation") != null) {
		submitSearchForm("searchLocation", emargementContextUrl + "/admin/location/search", "");
	} else if (document.getElementById("searchSessionEpreuve") != null) {
		submitSearchForm("searchSessionEpreuve", emargementContextUrl + "/manager/sessionEpreuve/search", "");
	} else if (document.getElementById("searchIndividuTagCheck") != null && document.getElementById("searchIndividuTagChecker") != null
		&& document.getElementById("searchIndividuGroupe") != null) {
		submitSearchForm("searchIndividuTagCheck", emargementContextUrl + "/manager/individu/search", "&type=tagCheck");
		submitSearchForm("searchIndividuTagChecker", emargementContextUrl + "/manager/individu/search", "&type=tagChecker");
		submitSearchForm("searchIndividuGroupe", emargementContextUrl + "/manager/individu/search", "&type=groupe");
	} else if (document.getElementById("searchIndividu") != null) {
		searchUsersAutocomplete("searchIndividu", emargementContextUrl + "/manager/individu/search", "&type=tagCheck", 100);
		searchIndividu.addEventListener("awesomplete-selectcomplete", function(event) {
			var splitResult = this.value.split("//");
			searchIndividu.value = splitResult[0].toString().trim();
			$("#tcIdentity").html(splitResult[2].toString().trim() + ' ' + splitResult[1].toString().trim());
		});
	}
	else if (document.getElementById("searchSuEppn") != null) {
		submitSearchForm("searchSuEppn", emargementContextUrl + "/superadmin/su/searchUsersLdap", "");
	}
	if (document.getElementById("searchLdap") != null) {
		submitSearchForm("searchLdap", emargementContextUrl + "/supervisor/searchUsersLdap", "");
	}

	var helpForm = document.getElementById('helpForm');
	if (helpForm != null) {
		var areaEditor = document.getElementById("areaEditor");
		var suneditor = createSunEditor('value');
		helpForm.addEventListener('submit', function(e) {
			e.preventDefault();
			document.getElementById("value").value = suneditor.getContents();
			helpForm.submit();
		});
	}

	//stats
	var year = $("#anneeUnivSelect option:selected").val();
	if (document.getElementById('statsCharts') != null) {
		var url = "manager";
		$("body").css("background-color", "#f2f2f2");
		//getStats(year, param, null,id, chartType,  option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
		getStats(year, null, url, "sessionEpreuvesByCampus", "pie");
		getStats(year, null, url, "sessionLocationByLocation", "doughnut");
		getStats(year, null, url, "tagCheckersByContext", "pie", "legend");
		getStats(year, null, url, "presenceByContext", "pie");
		getStats(year, null, url, "sessionEpreuveByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray);
		getStats(year, null, url, "countTagCheckByYearMonth", "lineChart", null, null, false, null, null, null, true, monthsArray);
		getStats(year, null, url, "countTagChecksByTypeBadgeage", "doughnut");
		getStats(year, null, url, "countTagCheckBySessionLocationBadgedAndPerson", "doughnut");
		getStats(year, null, url, "countSessionEpreuveByType", "pie");
	}
	//stats superadmin
	if (document.getElementById('statsSuperAdminCharts') != null) {
		var url = "superadmin";
		$("body").css("background-color", "#f2f2f2");
		//getStats(param, null,id, chartType,  spinner, option, transTooltip, formatDate, label1, data2, label2, fill, arrayDates, byMonth)
		getStats(year, null, url, "sessionEpreuvesByContext", "multiBar");
		getStats(year, null, url, "countTagChecksByContext", "multiBar");
		getStats(year, null, url, "countLocationsByContext", "chartBar");
		getStats(year, null, url, "countUserAppsByContext", "multiBar");
		getStats(year, null, url, "countCampusesByContext", "chartBar");
		getStats(year, null, url, "countSessionEpreuveByTypeByContext", "multiBar");
	}

	//stats session epreuve
	$('[id^=modalChartSeBtn]').on('click', function(e) {
		var url = "manager";
		var splitId = this.id.split("-");
		var canvas = document.getElementById("countTagChecksByTimeBadgeage");
		canvas.remove();
		$('#chartListSession').append("<canvas id='countTagChecksByTimeBadgeage' style='height: 394px; width: 789px;' height='394' width='789'></canvas>");
		getStats(null, "&param=" + splitId[1], url, "countTagChecksByTimeBadgeage", "chartBar");
	})
	$('#modal-chart').on('shown.bs.modal', function(event) {
		var button = $(event.relatedTarget) // Button that triggered the modal
		var title = button.data('whatever') // Extract info from data-* attributes
		var modal = $(this)
		modal.find('.modal-title').text(title)
	})
	var calendarEl = document.getElementById('calendar');

	if (calendarEl != null) {
		getCalendar(calendarEl, emargementContextUrl + "/manager/calendar/events", true);
	}

	var calendarElAll = document.getElementById('calendarAll');
	if (calendarElAll != null) {
		getCalendar(calendarElAll, emargementContextUrl + "/superadmin/calendar/events", false);
	}

	//Pagination --->rajout tous
	if (document.getElementById('pagination') != null) {
		if (allSelect != null && allSelect > 10) {
			var location = window.location;
			var dataUrl = location.pathname;
			var allSelectUrl = "";
			if (location.search == "") {
				allSelectUrl = dataUrl + "?size=" + allSelect;
			} else {
				var url = new URL(location);
				var query_string = url.search;
				var search_params = new URLSearchParams(query_string);
				search_params.set('size', allSelect);
				url.search = search_params;
				allSelectUrl = url.pathname + url.search;
				search_params.set('size', "10");
				url.search = search_params;
				selectUrl10 = url.pathname + url.search;
			}
			$("#pagination select").prepend("<option value='" + allSelect + "' data-page-size-url='" + allSelectUrl + "'>Tous</option>");
		}
		$("#pagination option:selected").removeProp("selected");
		var sizeSelected = ($_GET("size") != null) ? $_GET("size") : allSelect;
		$("#pagination option[value='" + sizeSelected + "']").attr('selected', 'selected');
	}

	//Extraction
	if (document.getElementById('extractionPage') != null) {
		var slimArray = ['codCmp', 'sessionEpreuve', 'sessionEpreuveLdap', 'sessionEpreuveGroupe', 'sessionEpreuveCsv', 'groupe', 'sessionLocationCsv', 'sessionLocationLdap',
			'sessionLocationGroupe', 'sessionLocation'];

		slimArray.forEach(function(item, index, array) {
			var select = '#' + item;
			if (document.getElementById(item) != null) {
				new SlimSelect({
					select: select
				});
			}
		});

		var slimArraySearchFalse = ['codSes', 'codAnu'];
		slimArraySearchFalse.forEach(function(item, index, array) {
			var select = '#' + item;
			if (document.getElementById(item) != null) {
				new SlimSelect({
					select: select,
					showSearch: false
				});
			}
		});

		var slimArrayEnabled = ['codEtp', 'codElp', 'codExtGpe'];
		slimArrayEnabled.forEach(function(item, index, array) {
			var select = '#' + item;
			if (document.getElementById(item) != null) {
				var slim = new SlimSelect({
					select: select,
					allowDeselect: true
				});
				slim.enable();
			}
		});
	}
	if (document.getElementById('icsSelect') != null) {
		new SlimSelect({
			select: '#icsSelect',
			showSearch: false
		});
	}
	if (document.getElementById('suList') != null) {
		new SlimSelect({
			select: '#suList',
			placeholder: 'Rechercher Utilisateur'
		});
	}
	if (document.getElementById('gpId') != null) {
		new SlimSelect({
			select: '#gpId',
			placeholder: 'Rechercher groupe'
		});
	}

	if (document.getElementById('sessionLocationExpected') != null) {
		new SlimSelect({
			select: '#sessionLocationExpected',
			placeholder: 'Rechercher Lieu'
		});
	}
	if (document.getElementById('sessionLocationExpected2') != null) {
		new SlimSelect({
			select: '#sessionLocationExpected2',
			placeholder: 'Rechercher Lieu'
		});
	}

	if (document.getElementById('blackListGroupe') != null) {
		new SlimSelect({
			select: '#blackListGroupe',
			placeholder: 'Rechercher Groupe'
		});
	}

	//Groupes
	if (document.getElementById('addMembersGroupe') != null) {
		var slimArray2 = ['sessionEpreuveGroupes', 'groupes', 'groupes2'];

		slimArray2.forEach(function(item, index, array) {
			var select2 = '#' + item;
			if (document.getElementById(item) != null) {
				new SlimSelect({
					select: select2,
					placeholder: '--Choisir--'
				});
			}
		});
	}

	//Presence
	if (document.getElementById('presencePage') != null || document.getElementById('unknownPage') != null) {
		const uuid = ID();
		const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
		eventSource.addEventListener('tc', response => {
			var tagCheck = JSON.parse(response.data);
			if (!tagCheck.isBlacklisted) {
				var person = tagCheck.person;;
				var guest = tagCheck.guest;
				var sessionId = tagCheck.sessionEpreuve.id;
				var sessionLocationExpected = (tagCheck.sessionLocationExpected != null) ? tagCheck.sessionLocationExpected.id : null;
				var identifiant = "";
				var varUrl = "";
				if (person != null) {
					identifiant = person.eppn;
					varUrl = person.eppn;
				} else if (guest != null) {
					identifiant = guest.email;
					varUrl = "inconnu";
				}
				var url = emargementContextUrl + "/supervisor/" + varUrl + "/photo";
				var nom = "";
				var prenom = "";
				if (person != null && person.nom != null) {
					nom = person.nom.toUpperCase();
					prenom = person.prenom;
				} else if (guest != null && guest.nom != null) {
					nom = guest.nom.toUpperCase();
					prenom = guest.prenom;
				}
				if (sessionLocationExpected != null) {
					var displayedIdentity = sessionLocationExpected + "_displayedIdentity2";
					var displayedIdentity2 = $("#" + displayedIdentity);
					displayedIdentity2.removeClass("d-none");
					displayedIdentity2.find("img").prop("src", url);
					displayedIdentity2.find("img").prop("alt", identifiant);
					displayedIdentity2.find('#prenomPresence3').text(prenom);
					displayedIdentity2.find('#nomPresence3').text(nom);
					if (person != null && person.numIdentifiant != null) {
						displayedIdentity2.find('#numIdentifiantPresence2').text('N° ' + person.numIdentifiant);
					}
					const toastLiveExample = document.getElementById(displayedIdentity);
					const toast = new bootstrap.Toast(toastLiveExample, { 'delay': 2000 })
					toast.show();
					$("body").scrollTop();
				}
				var urlLocation = (sessionLocationExpected != null) ? sessionLocationExpected : $_GET("location");
				var url = emargementContextUrl + "/supervisor/presence?sessionEpreuve=" + sessionId +
					"&location=" + urlLocation + "&update=" + tagCheck.id;
				$("#resultsBlock").load(url, function(responseText, textStatus, XMLHttpRequest) {
					backToTop();
					displayToast();
					sortDate = (triBadgeage == 'true')? [0, 'asc'] : [4, 'desc'];
					initTablePresence(sortDate);
				});
			}
		}, false);

		eventSource.addEventListener('sl', response => {
			var sl = JSON.parse(response.data);
			$("#sl" + sl.id).html(sl.nbPresentsSessionLocation);
		}, false);

		eventSource.addEventListener('message', this.onmessage = function(e) {
			var splitData = e.data.split("@@");
			var percent = parseFloat(splitData[1]).toFixed(2);
			$("#progressBar" + splitData[0]).css("width", percent + '%');
			$("#progressBar" + splitData[0]).text(percent + '%');
		});
		eventSource.addEventListener('total', response => {
			var total = response.data;
			var splitData1 = total.split("@@");
			$("#totalPresent" + splitData1[0]).text(splitData1[1]);
		}, false);

		eventSource.addEventListener('customMsg', response => {
			var customMsg = response.data;
			if (customMsg != "") {
				$("#customMsg").removeClass("d-none").addClass("show");
				setTimeout(function() {
					$("#customMsg").addClass("d-none");
				}, 2000);
			}
		}, false);

		if (document.getElementById('searchTagCheck') != null) {
			var selectPresence = new SlimSelect({
				select: '#searchTagCheck',
				allowDeselect: true
			});
		}

		if (document.getElementById('sessionEpreuvePresence') != null) {
			new SlimSelect({
				select: '#sessionEpreuvePresence',
				placeholder: 'Choisir une session'
			});
		}
		if (document.getElementById('location') != null) {
			new SlimSelect({
				select: '#location',
				placeholder: 'Choisir un lieu'
			});
		}
		$("#searchTagCheck").change(function() {
			$("#collapseTable").removeClass("d-none");
			$("#collapseTable table tbody").empty();
			$("#" + this.value).clone().appendTo("#collapseTable table tbody");
		});
		$(document).on("click", "#toto", function(event) {
			if (window.confirm('Confirmez-vous la suppression cet individu?')) {
				var trParent = $(this)[0].parentElement.parentElement;
				var id = trParent.id;
				var request = new XMLHttpRequest();
				request.open('POST', emargementContextUrl + "/supervisor/tagCheck/" + id, true);
				request.onload = function() {
					if (request.status >= 200 && request.status < 400) {
						$("#" + id).hide();
					}
				}
				request.send();
			}
		});
	}

	//Extraction ldap
	var searchGroup = document.getElementById('searchLdapGroupForm');
	var searchLdapGroupForm = document.getElementById('searchLdapGroupForm');
	if (searchGroup != null) {
		searchUsersAutocomplete("searchGroup", emargementContextUrl + "/manager/extraction/ldap/searchGroup", "", 100);

		searchGroup.addEventListener("awesomplete-selectcomplete", function(event) {
			searchLdapGroupForm.submit();
		});
	}

	//Remplissage form session à partir d'ics
	var icsEvents = document.getElementById('icsEvents');
	if (icsEvents != null) {
		new SlimSelect({
			select: '#icsEvents',
			placeholder: 'Recherche épreuve'
		});
		var icsSelect = document.getElementById('icsSelect');
		icsSelect.addEventListener("change", function() {
			setSelectEvent(this.value, "#divEvents");
		})

		$(document).on("change", "#icsEvents", function() {
			var url = emargementContextUrl + "/manager/event/searchEvent";
			var request = new XMLHttpRequest();
			request.open('GET', url + "?uid=" + this.value, true);
			request.onload = function() {
				if (request.status >= 200 && request.status < 400) {
					var data = JSON.parse(this.response);
					$("#nomSessionEpreuve").val(data.summary);
					$("#dateSessionEpreuve").val(moment(data.startDate).format('YYYY-MM-DD'));
					$("#heureConvocation").val(moment(data.startDate).subtract(30, 'minutes').format('HH:mm'));
					$("#heureEpreuve").val(moment(data.startDate).format('HH:mm'));
					$("#finEpreuve").val(moment(data.endDate).format('HH:mm'));
				} else {
					console.log("erreur du serveur!");
				}
			};
			request.send();
		});
	}
	//modal events
	var modalEvent = document.getElementById('modal-event');
	if (modalEvent != null) {
		$('[id^=event]').on('click', function(e) {
			var splitId = this.id.split("-");
			setSelectEvent(splitId[1], "#modal-body-event");
		})
		$('#modal-event').on('shown.bs.modal', function(event) {
			var button = $(event.relatedTarget) // Button that triggered the modal
			var title = button.data('whatever') // Extract info from data-* attributes
			var modal = $(this)
			modal.find('.modal-title').text(title)
		})
	}

	//Locations
	var icsEventLocations = document.getElementById('icsEventLocations');
	if (icsEventLocations != null) {
		new SlimSelect({
			select: '#icsEventLocations',
			placeholder: 'Recherche Lieu'
		})
		icsEventLocations.addEventListener("change", function() {
			$("#nom").val(this.value);
		});
	}

	//select context
	var selectContext = document.getElementById('selectContext');
	if (selectContext != null) {
		selectContext.addEventListener("change", function() {
			var text = $(this).find("option:selected").text();
			if (text == "Ordre") {
				window.location.href = this.value;
			} else {
				this.form.submit();
			}
		})
	}

	//Changement année univ
	$("#anneeUnivSelect").on("change", function(e) {
		var annee = this.value;
		window.location.href = window.location.pathname + "?anneeUniv=" + annee;
	});

	//procuration
	$(document).on('click', '.proxyPersonCheck', function(e) {
		e.preventDefault();
		var splitProxy = null;

		if (typeof $(this).attr("data-whatever") == "undefined") {
			splitProxy = this.value.split(",");
			$("#substituteId option[value='']").prop("selected", "selected");
		} else {
			splitProxy = $(this).attr("data-whatever").split(",");
			let el = document.querySelector('#substituteId');
			$("#substituteId option[value='" + splitProxy[2] + "']").prop("selected", "selected");
		}

		$("#procurationPerson").text(splitProxy[1]);
		$("#tcId").val(splitProxy[0]);

		const offcanvasElementList = document.getElementById('offcanvasExample')
		const offcanvasList = new bootstrap.Offcanvas(offcanvasElementList);
		offcanvasList.show();
	});

	if (document.getElementById('substituteId') != null) {
		var select = new SlimSelect({
			select: '#substituteId'
		})
	}

	var fileInput = document.querySelector('#files');

	if (fileInput != null) {
		if (seId == "") {
			$("#files").fileinput({
				theme: "fas",
				language: "fr",
				maxFileSize: 1000,
				maxFileCount: 10,
				mainClass: "input-group-lg",
				dropZoneEnabled: false
			});
		} else {
			const config = {
				type: "",
				filetype: "",
				caption: "",
				downloadUrl: false,
				size: "",
				width: "120px",
				key: 0
			};
			var urls = [];
			var configsArray = [];
			var rootUrlSe = emargementContextUrl + "/manager/sessionEpreuve/";
			var deleteUrl = emargementContextUrl + "/manager/sessionEpreuve/storedFiles/delete";
			var request = new XMLHttpRequest();
			request.open('GET', emargementContextUrl + "/manager/sessionEpreuve/storedFiles/" + seId, true);
			request.onload = function() {
				if (request.status >= 200 && request.status < 400) {
					var data = JSON.parse(this.response);
					var seId = "";
					data.forEach(function(value, key) {
						urls.push(rootUrlSe + value.id + "/photo");
						const me = Object.create(config);
						if (value.contentType.indexOf("pdf") !== -1) {
							me.type = "pdf";
						} else {
							me.type = "image";
						}
						me.filetype = value.contentType;
						me.caption = value.filename,
							me.downloadUrl = rootUrlSe + value.id + "/photo",
							me.size = value.fileSize,
							// me.width= "120px",
							me.key = value.id
						configsArray.push(me);
						seId = value.sessionEpreuve.id;
					});
					$("#files").fileinput({
						theme: "fas",
						initialPreview: urls,
						initialPreviewAsData: true,
						initialPreviewConfig: configsArray,
						deleteUrl: deleteUrl,
						overwriteInitial: false,
						language: "fr",
						maxFileSize: 1000,
						maxFileCount: 10,
						mainClass: "input-group-lg",
						allowedPreviewTypes: ['image', 'html', 'text', 'video', 'audio', 'flash', 'object', 'pdf']
					});
				}
			}
			request.send();
		}
	}
	$("#files").on("filepredelete", function(jqXHR) {
		var abort = true;
		if (confirm("Are you sure you want to delete this image?")) {
			abort = false;
		}
	});

	//Select all !!
	$("#selectAll").click(function() {
		$('.notUsed').find("input[type=checkbox]").prop('checked', $(this).prop('checked'));
	});

	$('.notUsed').find("input[type=checkbox]").click(function() {
		if (!$(this).prop("checked")) {
			$("#selectAll").prop("checked", false);
		}
	});

	//Préférence : voir sessions antérieure
	$(document).on('change', '#oldSessionsCheck', function() {
		var value = (this.checked) ? "true" : "false";
		var redirect = window.location.origin + window.location.pathname;
		var request = new XMLHttpRequest();
		request.open('GET', emargementContextUrl + "/supervisor/prefs/updatePrefs?pref=seeOldSessions&value=" + value, true);
		request.onload = function() {
			if (request.status >= 200 && request.status < 400) {
				window.location.href = redirect;
			}
		}
		request.send();
	});
	//Foemulaire présence
	$("#presencePage #location").on("change", function() {
		$("#presenceForm").submit();
	});

	var dialogMsg = document.querySelector('#modalUser');
	if (dialogMsg != null) {
		$('#modalUser').modal('show');
	}

	//Activation webcam
	$(document).on('change', '#webCamCheck', function() {
		var value = (this.checked) ? "true" : "false";
		var redirect = window.location.origin + window.location.pathname;
		var request = new XMLHttpRequest();
		request.open('GET', emargementContextUrl + "/supervisor/prefs/updatePrefs?pref=enableWebcam&value=" + value, true);
		request.onload = function() {
			if (request.status >= 200 && request.status < 400) {
				window.location.href = redirect;
			}
		}
		request.send();
	});

	var qrCodeCam = document.getElementById("qrCodeCam");
	if (qrCodeCam != null) {
		var video = document.createElement("video");
		var canvasElement = document.getElementById("canvas");
		var canvas = canvasElement.getContext("2d");
		var loadingMessage = document.getElementById("loadingMessage");

		function drawLine(begin, end, color) {
			canvas.beginPath();
			canvas.moveTo(begin.x, begin.y);
			canvas.lineTo(end.x, end.y);
			canvas.lineWidth = 4;
			canvas.strokeStyle = color;
			canvas.stroke();
		}

		// Use facingMode: environment to attemt to get the front camera on phones
		navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } }).then(function(stream) {
			video.srcObject = stream;
			video.setAttribute("playsinline", true); // required to tell iOS safari we don't want fullscreen
			video.play();
			requestAnimationFrame(tick);
		});
		let isCodeScanned = false;
		function tick() {
			loadingMessage.innerText = "⌛ Loading video..."
			if (video.readyState === video.HAVE_ENOUGH_DATA) {
				loadingMessage.hidden = true;
				canvasElement.hidden = false;
				canvasElement.height = video.videoHeight;
				canvasElement.width = video.videoWidth;
				canvas.drawImage(video, 0, 0, canvasElement.width, canvasElement.height);
				var imageData = canvas.getImageData(0, 0, canvasElement.width, canvasElement.height);
				var code = jsQR(imageData.data, imageData.width, imageData.height, {
					inversionAttempts: "dontInvert",
				});
				if (code) {
					drawLine(code.location.topLeftCorner, code.location.topRightCorner, "#FF3B58");
					drawLine(code.location.topRightCorner, code.location.bottomRightCorner, "#FF3B58");
					drawLine(code.location.bottomRightCorner, code.location.bottomLeftCorner, "#FF3B58");
					drawLine(code.location.bottomLeftCorner, code.location.topLeftCorner, "#FF3B58");
				}
				if (!isCodeScanned) {
					if (code) {
						var value = code.data;
						var sessionData = qrCodeCam.getAttribute("data-qrcode");
						if (sessionData != null) {
							var splitCode = value.split("value=");
							value = splitCode[1] + "@@@" + sessionData;
						}
						var prefix = "";
						if (qrCodeCam.hasAttribute('data-role')) {
							prefix = emargementContextUrl + "/";
						}
						var location = document.getElementById("location");
						var valLocation = (location != null) ? location.value : null;
						updatePresence(prefix + "updatePresents", value, valLocation);
						isCodeScanned = true;
						console.log("QR Code scanned once.");
						setTimeout(() => {
							isCodeScanned = false;
							console.log("Simulated scan. Code scanned only once.");
						}, 2000); //
					} else {
						isCodeScanned = false; // Reset the flag if no QR code is found
					}
				}
			}
			requestAnimationFrame(tick);
		}
		tick();
	}

	//Affinage reparttion
	$(".affinage").inputSpinner();

	$(".affinage").on("input", function(event) {
		var max = parseInt($(this).prop("max"));
		var current = parseInt($(this).val());
		var newPercent = $(this)[0].closest("span").nextElementSibling;
		var percentVal = (current * 100) / max;
		newPercent.textContent = Math.round(percentVal) + ' %';
		$(this).removeClass('is-valid');
		$(this).removeClass('is-invalid');
		if (current <= max) {
			$(this).addClass('is-valid');
		} else {
			$(this).addClass('is-invalid');
		}

		//Not tiers
		var notTiers = document.querySelectorAll('[id^="spinner_false"]');
		if (notTiers.length > 0) {
			displayAffinage(notTiers, false);
		}

		//Tiers
		var isTiers = document.querySelectorAll('[id^="spinner_true"]');
		if (isTiers.length > 0) {
			displayAffinage(isTiers, true);
		}

		var submitAffinage = document.getElementById("affinerButton");
		if (submitAffinage != null) {
			submitAffinage.addEventListener('click', function(e) {
				var formAffinage = document.getElementById("formAffinage");
				formAffinage.submit();
			});
		}
	})

	$('input[type=radio][name=tagCheckOrder]').on("change", function(event) {
		var tagCheckOrder = $(this).val();
		$("#alphaOrderAffinage").val(tagCheckOrder);
		$("#alphaOrderRepartition").val(tagCheckOrder);
	});

	$(".contextualHelp").on("mouseover", function(event) {
		$(this).tooltip("show");
		$(this).prop("title", "");
	});

	if (document.getElementById("sessionSearch") != null) {
		$("#formSearch .form-select").on("change", function(event) {
			$("#searchSessionEpreuve").val(null);
			document.getElementById("formSearch").submit();
		});
		$("#formSearch #resetSearch").on("click", function(event) {
			$("#searchSessionEpreuve").val(null);
			$("#statut").val("");
			$("#typeSession").val("");
			document.getElementById("formSearch").submit();
		});
		$("#sessionSearch .btn-check").on("change", function(event) {
			document.getElementById("formSearch").submit();
		});
	}

	//Import event ADE
	$("#codeSalleSelect").on("change", function(event) {
		document.getElementById("displaySalles").submit();
	});

	$("#displayEvents").on("submit", function(event) {
		$("#spinnerLoad").removeClass("d-none");
	});

	//Import event ADE
	$("#strDateMin").on("change", function() {
		var strDateMin = $("#strDateMin").val();
		$("#strDateMinImport").val(strDateMin);
		if ($("#strDateMax").val() == "") {
			$("#strDateMax").val(strDateMin);
			$("#strDateMaxImport").val(strDateMin);
		}
	});
	$("#strDateMax").on("change", function() {
		$("#strDateMaxImport").val($("#strDateMax").val());
	});
	const uuid = ID();
	const eventSource = new EventSource(emargementContextUrl + `/supervisor/register/${uuid}`);
	eventSource.addEventListener('nbImportSession', response => {
		var total = response.data;
		$("#nbImportSession").text("Imports ADE : " + total);
	}, false);

	//affiche photo
	displayToast();

	//jstree adecampus
	$('#frmt').on('changed.jstree', function(e, data) {
		var checkedNodes = data.selected;  // Get checked nodes
		// Remove parent nodes if their children are checked
		var nodesToRemove = [];
		checkedNodes.forEach(function(nodeId) {
			var node = data.instance.get_node(nodeId);
			if (node.children.length > 0) {
				node.children.forEach(function(childId) {
					if (checkedNodes.includes(childId)) {
						nodesToRemove.push(nodeId);
					}
				});
			}
		});

		// Remove the parent nodes
		nodesToRemove.forEach(function(nodeId) {
			var index = checkedNodes.indexOf(nodeId);
			if (index > -1) {
				checkedNodes.splice(index, 1);
			}
		});
		var r = [];
		checkedNodes.forEach(function(nodeId) {
			var node = data.instance.get_node(nodeId);
			r.push(node.id);
		});
		$("#idList").val(r.join(', '));
		$("#idListImport").val(r.join(', '));
	}).jstree({
		"checkbox": {
			"keep_selected_style": false,
			"three_state": false,
			"cascade": "up"
		},
		"plugins": ["checkbox", "sort", "state"],
		'core': {
			'data': []
		}
	});

	var table = $('.tableFoo').DataTable({
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		}
	});

	$("#codeComposante1").on("change", function(event) {
		const selectedData = $(this).val();
		$("#codeFormation").val($(this).val());
		$("input[id^=hiddenCodeComposante]").val(selectedData);
		if (selectedData == "myEvents") {
			$('#frmt').addClass("d-none");
		} else {
			$("#spinnerComps").removeClass("d-none");
			updateJsTree(selectedData, "trainee");
		}
		table.clear().draw();
	});
	$("#codeFormation").on("change", function(event) {
		const selectedData = $(this).val();
		$("#codeComposante1").val("");
		$("input[id^=hiddenCodeComposante]").val(selectedData);
		if (selectedData == "myEvents") {
			$('#frmt').addClass("d-none");
		} else {
			$("#spinnerComps").removeClass("d-none");
			updateJsTree(selectedData, category6);
		}
		table.clear().draw();
	});

	$('#checkAll').on('click', function() {
		var checked = this.checked;
		$('.data-checkbox').prop('checked', checked);
	});
	$('.data-checkbox').on('click', function() {
		var allChecked = $('.data-checkbox:checked').length === $('.data-checkbox').length;
		$('#checkAll').prop('checked', allChecked);
	});

	$('#displayEvents').submit(function(e) {
		e.preventDefault();
		var formData = $(this).serialize();
		$.ajax({
			type: 'GET',
			url: emargementContextUrl + "/manager/adeCampus/Events",
			data: formData,
			success: function(response) {
				table.destroy();
				$("#tableEvents").html(response);
				$("#spinnerLoad").addClass("d-none");
				table = $('.tableFoo').DataTable({
					columnDefs: [
						{
							targets: 0, // Column index for the checkbox column
							orderable: false, // Disable sorting on this column
							className: 'select-checkbox', // Add a class for the checkbox column
							render: function(data, type, row, meta) {
								return '<input type="checkbox"  class="data-checkbox" name="btSelectItem" value="' + row[1] + '">';
							}
						},
						{ type: 'date-eu', targets: 6 },
						{ type: 'date-eu', targets: 2 }
					],
					select: {
						style: 'multi', // Set the selection style (you can use 'single' or 'os' as well)
					},
					order: [[6, 'desc']], // Set the default sorting column and order
					responsive: true,
					ordering: true,
					paging: true,
					searching: true,
					info: false,
					language: {
						url: "/webjars/datatables-plugins/i18n/fr-FR.json"
					}
				});
			},
			error: function(error) {
				console.log('Error: ' + error);
			}
		});
	});

	$("#displayEventsImport").submit(function(event) {
		event.preventDefault();
		$("#spinnerLoad").removeClass("d-none");
		$.ajax({
			url: emargementContextUrl + "/manager/adeCampus/importEvents",
			type: "POST",
			data: $("#displayEventsImport").serialize(), // Serialize form data
			success: function() {
				$('#displayEvents').submit();
			},
			error: function(error) {
				console.log("Error: " + error);
			}
		});
	});

	//Create sessionLocation
	var addSessionLocation = document.getElementById("addSessionLocation");
	if (addSessionLocation != null) {
		getCapacite($("#location").val());
		$("#addSessionLocation").on("change", function(event) {
			getCapacite($(this).val())
		});
	}
	var qrCodeDisplay = document.getElementById("qrCodeDisplay");
	if (qrCodeDisplay != null) {
		var url = emargementContextUrl + "/supervisor/qrCodeSession/" + currentLocation;
		getQrCodeSession(url, "imgQrCode");
		setInterval(function() { getQrCodeSession(url, "imgQrCode") }, qrcodeChange);
	}

	$('#userPage .modal').on('show.bs.modal', function(event) {
		var id = this.id.replace("qrCodeModal", "");
		var dataEppn = this.getAttribute("data-eppn");
		var dataSession = this.getAttribute("data-session");
		var url = emargementContextUrl + "/user/qrCode/" + dataEppn + "/" + dataSession;
		var imgQrCodeUser = "imgQrCodeUser" + id;
		getQrCodeSession(url, imgQrCodeUser);
		var interval = setInterval(function() {
			getQrCodeSession(url, imgQrCodeUser);
		}, qrcodeChange);
		$('#userPage .modal').on('hidden.bs.modal', function() {
			clearInterval(interval);
		});
	});
	
	//Recherche assiduité
	let minDate, maxDate;
	DataTable.ext.search.push(function(settings, data, dataIndex) {
		let min = minDate.val();
		let max = maxDate.val();
		let date = new Date(newDate = createDateFromString(data[4]));
		if (
			(min === null && max === null) ||
			(min === null && date <= max) ||
			(min <= date && max === null) ||
			(min <= date && date <= max)
		) {
			return true;
		}
		return false;
	});
	// Create date inputs
	minDate = new DateTime('#min', {
		format: 'DD/MM/YY'
	});
	maxDate = new DateTime('#max', {
		format: 'DD/MM/YY'
	});
	var title = '';
	var dataTableOptions = {
		responsive: true,
		ordering: true,
		paging: true,
		searching: true,
		info: false,
		language: {
			url: "/webjars/datatables-plugins/i18n/fr-FR.json"
		},
		columnDefs: [
			{
				targets: 'dateItem',
				type: 'datetime-moment', // Use the datetime-moment plugin
				render: function(data, type, row) {
					if (!data) { // Check if data is empty
						return ''; // Return empty string if data is empty
					}
					if (type === 'sort' || type === 'type') {
						return moment(data, 'DD/MM/YY').unix();
					}
					return moment(data, 'DD/MM/YY').format('DD/MM/YY');
				}
			}
		],
		pageLength: -1,
		lengthMenu: [
	        [10, 25, 50, -1],
	        [10, 25, 50, 'All']
	    ],
        dom: 'Bfrtilp',
        buttons: [
            {extend: 'copy', exportOptions: {orthogonal: 'filter', columns: ':not(.exclude)'}, title:function () {return 'assiduite_' + title;}},
            {extend: 'csv', exportOptions: {orthogonal: 'filter', columns: ':not(.exclude)'}, title:function () {return 'assiduite_' + title;}},
            {extend: 'pdf', orientation: 'landscape', pageSize: 'LEGAL', exportOptions: {orthogonal: 'filter', columns: ':not(.exclude)'}, title:function () {return 'assiduite_' + title;}},
            {extend: 'print', exportOptions: {orthogonal: 'filter', columns: ':not(.exclude)'}, title:function () {return 'assiduite_' + title;}}
        ]
	 }
	
	$('table.assiduite').DataTable(dataTableOptions).on('buttons-processing', function(e, buttonApi, dataTable, node, config) {
		title = $(dataTable.table().node()).attr('data-export-title');
	});

	let table3 = $('table.assiduite2').DataTable(dataTableOptions).on('buttons-processing', function(e, buttonApi, dataTable, node, config) {
		title = $(dataTable.table().node()).attr('data-export-title');
	});
	 
	// Refilter the table
	document.querySelectorAll('#min, #max').forEach((el) => {
		el.addEventListener('change', () => table3.draw());
	});
	
	sortDate = (triBadgeage == 'true')? [0, 'asc'] : [4, 'desc'];
	initTablePresence(sortDate);
});