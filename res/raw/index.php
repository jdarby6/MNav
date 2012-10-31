
<!--<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"

   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">-->

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">



<html xmlns="http://www.w3.org/1999/xhtml">

	

	<head>

      

		<title>U of M Magic Bus - Map View</title>

		

		<link rel="stylesheet" type="text/css" href="css/style.css" />

		<link rel="stylesheet" type="text/css" href="css/arrivals.css" />

		<link rel="stylesheet" type="text/css" href="css/merged.css" />

        <link rel="stylesheet" type="text/css" href="css/psa.css" />

		<meta http-equiv = "Pragma" content="no-cache" />

		<meta http-equiv = "Expires" content="-1" />



		

		<meta http-equiv="content-type" content="text/html; charset=UTF-8"/>

<script type="text/javascript">

var infoforstop = "";

</script>    

    	<script src="http://maps.google.com/maps?file=api&amp;v=2.63&amp;key=0Swejj587aUMnEZ9QijQMCIiU9fyT_2PMLYZSSA" type="text/javascript"></script>

    	<script type="text/javascript" src="js/pdmarker.js"></script>

		<script type="text/javascript" src = "js/tracking.js"></script>

		<script type="text/javascript" src = "js/stops.js"></script>

		<script type="text/javascript" src = "js/icons.js"></script>

		<script type="text/javascript" src = "js/setMath.js"></script>

        <script type="text/javascript" src = "js/routes.js"></script>

		<script type="text/javascript" src="js/psa.js"></script>



		<!-- include the javascript file, but do it this way so it can see the PHP GET variable-->

		<script type="text/javascript">

<!--



// global vars

var isIE = false; // is this Internet Explorer



var xmlDoc2;	// xml response

var xmlDocStopCoords;

	

var req2;	// xml request (TOA)

var stopCoordsReq; // xml request (Stop Coordinates)



var map;	// map

var stopLat; // latitude of the requested stop

var stopLon; // longitude of the requested stop

var wantedStop = ""; // the requested stop's name, as given by the GET string 

function loadXMLDocTOA(url) 

{

	// branch for native XMLHttpRequest object

	if (window.XMLHttpRequest)

	{

		req2 = new XMLHttpRequest();

		req2.onreadystatechange = processReqChangeTOA;

		req2.open("GET", url, true);

		req2.send(null);

	// branch for IE/Windows ActiveX version

	}

	else if (window.ActiveXObject)

	{

		isIE = true;

		req2 = new ActiveXObject("Microsoft.XMLHTTP");

		if (req2)

		{

			req2.onreadystatechange = processReqChangeTOA;

			req2.open("GET", url, true);

			req2.setRequestHeader("If-Modified-Since","Sat, 1 Jan 2000");

			req2.send();

		}

	}

}





function loadXMLDocStopCoords(url) 

{

	// branch for native XMLHttpRequest object

	if (window.XMLHttpRequest)

	{

		stopCoordsReq = new XMLHttpRequest();

		stopCoordsReq.onreadystatechange = processReqChangeStopCoords;

		stopCoordsReq.open("GET", url, true);

		stopCoordsReq.send(null);

	}

	// branch for IE/Windows ActiveX version

	else if (window.ActiveXObject)

	{

		isIE = true;

		stopCoordsReq = new ActiveXObject("Microsoft.XMLHTTP");

		if (stopCoordsReq)

		{

			stopCoordsReq.onreadystatechange = processReqChangeStopCoords;

			stopCoordsReq.open("GET", url, true);

			stopCoordsReq.setRequestHeader("If-Modified-Since","Sat, 1 Jan 2000");

			stopCoordsReq.send();

		}

	}

}



document.removeChildNodes = function(node)

{

	while (node.childNodes.length > 0)

	{

		node.removeChild(node.childNodes[0]);

	}

}



function processReqChangeStopCoords()

{

	// only if requset is finished loading

	if (stopCoordsReq.readyState == 4 && stopCoordsReq.status == 200) 

	{

		xmlDocStopCoords = stopCoordsReq.responseXML;

		var stops = xmlDocStopCoords.documentElement.getElementsByTagName("stop");

		

		var s;

		var thePoint = new Array(stops.length);

		var wantedStops = new Array();

		var center = new GLatLng(42.27880, -83.72328);

		var zoom = 14;

		//var center = new GLatLng(42.274543, -83.741154);

		

		

		var x = 0;

		

		//alert("Number of stops:\n" + stops.length);

		

		for(s = 0; s < stops.length; s++)

		{

			if(stops[s].getElementsByTagName("name2")[0].childNodes[0].nodeValue == wantedStop)

			{

				

				wantedStops[x] = stops[s];

				x++;

				

				stopLat = parseFloat(stops[s].getElementsByTagName("lat")[0].childNodes[0].nodeValue);

				stopLon = parseFloat(stops[s].getElementsByTagName("lon")[0].childNodes[0].nodeValue);



				thePoint[s] = new GLatLng(stopLat, stopLon);

				center = thePoint[s];

				zoom = 16

				

			}

		}

		

		map.setCenter(center, zoom);

				



		if(wantedStops.length>0)

		{

			drawStops(wantedStops);

		}

		

	}

	else

	{

		//alert("There was a problem retrieving the stop XML data:\n" + stopCoordsReq.statusText);

		//getStopCoords();

	}

}



function processReqChangeTOA() 

{

	// only if requset is finished loading 

	if (req2.readyState == 4 && req2.status == 200) 

	{



		xmlDoc2 = req2.responseXML;

		markers = xmlDoc2.documentElement.getElementsByTagName("routecount");

		var routecount = parseInt(markers[0].childNodes[0].nodeValue);



		var routes = xmlDoc2.documentElement.getElementsByTagName("route");

		

		if (document.getElementById("tableContainer").hasChildNodes())

			document.removeChildNodes(document.getElementById("tableContainer"));

		

		var dummyContainer = document.createElement("div");



		for(i = 0; i < routecount; i++)

		{

			var routename = routes[i].getElementsByTagName("name")[0].childNodes[0].nodeValue;

			var busroutecolor = routes[i].getElementsByTagName("busroutecolor")[0].childNodes[0].nodeValue;

			var stopcount = parseInt(routes[i].getElementsByTagName("stopcount")[0].childNodes[0].nodeValue);

			var topofloop = parseInt(routes[i].getElementsByTagName("topofloop")[0].childNodes[0].nodeValue);

			

			var toaTable = document.createElement("table");

			toaTable.setAttribute("cellspacing", 0);

			toaTable.setAttribute("cellpadding", 0);

			toaTable.setAttribute("class", "timetable");

			toaTable.setAttribute("id", routename.replace(" ", "-"));

			

			var toaTbody = document.createElement("tbody");

			toaTable.appendChild(toaTbody);

			

			var labelRow = document.createElement("tr");

			var labelCell = document.createElement("td");

			labelCell.setAttribute("class", "routeName");

			

			if(topofloop == 0)

			{

				labelCell.setAttribute("colspan", 4);

			}

			else

			{

				labelCell.setAttribute("colspan", 5);

			}

			

			var labelH2 = document.createElement("h2");

			

			// IE doesn't set 'style' attribute requires workaround

			//labelH2.setAttribute("style","background-color: #"+busroutecolor+";");

			labelH2.removeAttribute("style");

			labelH2.setAttribute("style","background-color: #"+busroutecolor+";");

			labelH2.style.cssText = "background-color:"+busroutecolor+";";

			

			labelH2.appendChild(document.createTextNode(routename));

			labelCell.appendChild(labelH2);

			labelRow.appendChild(labelCell);

			toaTbody.appendChild(labelRow);

			

			

			var thRow = document.createElement("tr");

			var spacerTD = document.createElement("td");

			// IE doesn't set 'style' attribute need a workaround

			spacerTD.setAttribute("style","width: 7px;");

			thRow.appendChild(spacerTD);

			

			if(topofloop != 0)

			{

				var dirTH = document.createElement("th");

				dirTH.appendChild(document.createTextNode("Direction"));

				thRow.appendChild(dirTH);

			}

			

			var nextTH = document.createElement("th");

			nextTH.appendChild(document.createTextNode("Next Bus"));

			thRow.appendChild(nextTH);

			

			var secTH = document.createElement("th");

			secTH.appendChild(document.createTextNode("2nd Bus"));

			thRow.appendChild(secTH);

			

			var spacer2TD = document.createElement("td");

			// IE doesn't set 'style' attribute need a workaround

			spacer2TD.setAttribute("style", "width: 7px");

			thRow.appendChild(spacer2TD);



			toaTbody.appendChild(thRow);

			



			var stops = routes[i].getElementsByTagName("stop");



			for(j = 0; j < stopcount; j++)

			{

				var stopname = stops[j].getElementsByTagName("name2")[0].childNodes[0].nodeValue;

				

				// if this is not the stop we want, do nothing

				if(stopname != wantedStop)

				{

					continue;

				}

				

				var toacount = parseInt(stops[j].getElementsByTagName("toacount")[0].childNodes[0].nodeValue);

				

				var toatr = document.createElement("tr");

				var toatd = document.createElement("td");

				

				var arrowLTD = document.createElement("td");

				arrowLTD.setAttribute("class", "arrowCell");

				var arrowLA = document.createElement("a");

				var arrowLIMG = document.createElement("img");

				arrowLIMG.setAttribute("alt", "Previous Stop");

				if(j == 0)

				{

					if(topofloop == 0)

					{

						arrowLA.setAttribute("href", "#");

						arrowLIMG.setAttribute("src", "../images/arrowLgrey.gif");

					}

					else

					{

						arrowLA.setAttribute("href", "index.php?name=" + stops[stopcount-1].getElementsByTagName("name2")[0].childNodes[0].nodeValue);

						arrowLIMG.setAttribute("src", "../images/arrowL.gif");

					}

				}

				else

				{

					arrowLA.setAttribute("href", "index.php?name=" + stops[j-1].getElementsByTagName("name2")[0].childNodes[0].nodeValue);

					arrowLIMG.setAttribute("src", "../images/arrowL.gif");

				}

				arrowLA.appendChild(arrowLIMG);

				arrowLTD.appendChild(arrowLA);

				toatr.appendChild(arrowLTD);

				

				

				if(topofloop != 0)

				{

					toatd.setAttribute("class", "align_right");

					if(j < topofloop)

					{

						toatd.appendChild(document.createTextNode("Out:"));

					}

					else

					{

						toatd.appendChild(document.createTextNode("In:"));

					}

					toatr.appendChild(toatd);

				}						



				var mintoa = -1; 

				var mintoa2 = -1;



				for(k = 1; k <= toacount; k++)

				{

					var toa = parseFloat(stops[j].getElementsByTagName("toa" + k)[0].childNodes[0].nodeValue);



					if(k == 1)

						mintoa = toa;				

					

					if ((toa < mintoa2 || mintoa2 == -1) && toa > mintoa)

						mintoa2 = toa;

					else if(toa < mintoa)

					{

						mintoa2 = mintoa;

						mintoa = toa;

					}

				}

				

				mintoamin = Math.floor(parseFloat(mintoa)/60.);

				mintoamin2 = Math.floor(parseFloat(mintoa2)/60.);



				var toatd = document.createElement("td");

				if(mintoamin > 1)

				{

					toatd.appendChild(document.createTextNode(mintoamin+" min"));

				}

				else if(mintoa > 0)

				{

					toatd.appendChild(document.createTextNode("Arriving"));

					toatd.style.color = "red";

					toatd.style.textDecoration = "blink";

				}

				else

				{

					toatd.appendChild(document.createTextNode("--"));

				}

				toatr.appendChild(toatd);

				

				toatd = document.createElement("td");

				if(mintoamin2 > 1)

					toatd.appendChild(document.createTextNode(mintoamin2+" min"));

				else if(mintoa2 >0) // so... if it gets here, there's two buses within a minute of each other

					toatd.appendChild(document.createTextNode("Arriving"));

				else

					toatd.appendChild(document.createTextNode("--"));

				toatr.appendChild(toatd);

				

				var arrowRTD = document.createElement("td");

				var arrowRA = document.createElement("a");

				var arrowRIMG = document.createElement("img");

				arrowRTD.setAttribute("class", "arrowCell");

				arrowRIMG.setAttribute("alt", "Next Stop");

				if(j == stopcount - 1)

				{

					if(topofloop == 0)

					{

						arrowRA.setAttribute("href", "#");

						arrowRIMG.setAttribute("src", "../images/arrowRgrey.gif");

					}

					else

					{

						arrowRA.setAttribute("href", "index.php?name=" + stops[0].getElementsByTagName("name2")[0].childNodes[0].nodeValue);

						arrowRIMG.setAttribute("src", "../images/arrowR.gif");

					}

				}

				else

				{

					arrowRA.setAttribute("href", "index.php?name=" + stops[j+1].getElementsByTagName("name2")[0].childNodes[0].nodeValue);

					arrowRIMG.setAttribute("src", "../images/arrowR.gif");

				}

				arrowRA.appendChild(arrowRIMG);

				arrowRTD.appendChild(arrowRA);

				toatr.appendChild(arrowRTD);



				toaTbody.appendChild(toatr);

			



				dummyContainer.appendChild(toaTable);

		

			}

			

		

		}

		

		// For the cluster stops, let people know

		if(wantedStop == "Pierpont")

		{

			var notice = document.createTextNode("Note:");

			var noticeP = document.createElement("p");

			noticeP.appendChild(notice);

			

			var noteList = document.createElement("ul");

			noteList.setAttribute("class","note");

			var noteListItem = new Array();

			var itemText = new Array();

			itemText[1] = document.createTextNode("Commuter Northbound arrives at the Art & Architecture Building");

			itemText[2] = document.createTextNode("Commuter Southbound arrives at Pierpont on Bonisteel");

			itemText[3] = document.createTextNode("Northwood Outbound arrives at Pierpont on Murfin");

			itemText[4] = document.createTextNode("Northwood Inbound arrives at Pierpont  on Bonisteel");

			itemText[5] = document.createTextNode("Bursley-Baits Outbound arrives at Pierpont on Murfin");

			itemText[6] = document.createTextNode("Bursley-Baits Inbound arrives across from Pierpont on Murfin");

			

			for(var noteCount = 1; noteCount <= 6; noteCount++)

			{

				noteListItem[noteCount] = document.createElement("li");

				noteListItem[noteCount].appendChild(itemText[noteCount]);

				noteList.appendChild(noteListItem[noteCount]);

			}

			

			dummyContainer.appendChild(noticeP);

			dummyContainer.appendChild(noteList);

		}

		

		if(wantedStop == "Central Transit Center")

		{

			var notice = document.createTextNode("Note:");

			var noticeP = document.createElement("p");

			noticeP.appendChild(notice);

			

			var noteList = document.createElement("ul");

			noteList.setAttribute("class","note");

			var noteListItem = new Array();

			var itemText = new Array();

			itemText[1] = document.createTextNode("Northwood, Northwood Express, and Diag<->Diag arrive at the Ruthven Museum");

			

			for(var noteCount = 1; noteCount <= 1; noteCount++)

			{

				noteListItem[noteCount] = document.createElement("li");

				noteListItem[noteCount].appendChild(itemText[noteCount]);

				noteList.appendChild(noteListItem[noteCount]);

			}

			

			dummyContainer.appendChild(noticeP);

			dummyContainer.appendChild(noteList);

		}

		

		else

		{

			

		}

	

		if(!dummyContainer.hasChildNodes())

		{

			var noRoutesText = document.createTextNode("This stop is not being served at this time.");

			var noRoutes = document.createElement("p");

			noRoutes.appendChild(noRoutesText);

			dummyContainer.appendChild(noRoutes);

		}

		document.getElementById("tableContainer").innerHTML = dummyContainer.innerHTML;

	}

	else

	{

		

	}

}



function getTOA()

{	     

		if(wantedStop != "")

		{

			loadXMLDocTOA("../../shared/public_feed.xml");

		}

}



function getStopCoords()

{

	loadXMLDocStopCoords("../../shared/stops.xml");

}



-->

</script>

      

		

		<script type="text/javascript">

	

		function disableselect(e3){return false}

		function reEnable(){return true}

		document.onselectstart=new Function ("return false")

		if (window.sidebar){

		document.onmousedown=disableselect

		document.onclick=reEnable

		}

	

		function onLoad()

		{



			map = new GMap2(document.getElementById("mapdiv"));

			map.addControl(new GLargeMapControl());

			map.setCenter(new GLatLng(42.27880, -83.72328), 14);

			map.setMapType(G_NORMAL_MAP);

			//map.setMapType(G_HYBRID_MAP);			

			map.addControl(new GMapTypeControl());

			map.enableDoubleClickZoom();	

			map.enableContinuousZoom();

			//map.setCenter(new GLatLng(42.28500, -83.71828), 13);

	

			//getStops();

	

			retrieveStopList();

			getStopCoords();

			makeRouteList();		

			setTimeout("getBuses();",250);	

			setTimeout("getTOA();",250);		

			setInterval("getBuses();", 4000);

			setInterval("moveBuses();", 4000/splitMove);

			setInterval("getTOA();", 10000);

			//setInterval("cleanBuses()", 10000);

					}

		

		function ShowHide(elementId)

		{

			var element = document.getElementById(elementId);

			if(element.style.display != "block")

			{

				element.style.display = "block";

			}

			else

			{

				element.style.display = "none";

			}

		}

		function UpdateText(element)

		{

			if(element.innerHTML.indexOf("Select") != -1)

			{

				element.innerHTML = "Hide Route List";

			}

			else

			{

				element.innerHTML = "Select a Route:";

			}

		}	

		

		

		

		

		

		function ShowHide2(elementId)

		{

			var element = document.getElementById(elementId);

			if(element.style.display != "block")

			{

				element.style.display = "block";

			}

			else

			{

				element.style.display = "none";

			}

		}

		function UpdateText2(element)

		{

			if(element.innerHTML.indexOf("Select") != -1)

			{

				element.innerHTML = "Hide Stop List";

			}

			else

			{

				element.innerHTML = "Select a Stop:";

			}

		}			

		

		function browserSize(){

			if (screen.height>800||screen.width>800)

				document.getElementById("textonly").style.display = 'none';

		}		

	

		</script>



    

    	

	</head>



<body onLoad="onLoad(); onLoadPSA(); browserSize();" onUnload="GUnload()"  oncontextmenu="return false;" >

		<a id="textonly" title="click here for text only page" href = "mobile/index.php">(click here for the text only page)</a>

        <div id = "headContainer" style=" white-space:nowrap; vertical-align:middle; width:100%; min-width:1064px">

       		 <div id = "header" >

                     <p >&nbsp;&nbsp;&nbsp;

                     <a href = "http://pts.umich.edu"><img src = "images/PTSlogo.jpg" style="vertical-align:middle; padding-left:40px"  class = "iehack" alt = "PTS Logo" /></a>

                    <font size = "9" font color="#000099" style="vertical-align:middle; padding-left:50px"><B>

                    &nbsp;Magic Bus

                    </B></font>

                    

                    </p>

         		   



			  </div>



           

        </div>	

         <div id = "makeWhite">

				<noscript>

					<p style="color:#FF0000; text-align:left;">

						Your browser does not support JavaScript. Click <a title="click here for text only page" href = "mobile/index.php">here</a> for the text only page. Otherwise, please upgrade your browser.

					</p>

				</noscript>

                <div id = "psaContainer"> </div>

				<div id = "hardcodepsaContainer">

					<ul class="psaList">

					</ul>

				</div>         

                       

				

                <div id = "EmergencyContainer"> </div>

                <br>

         </div>

            

	<table cellpadding = "0" cellspacing = "0" border = "0" id = "bodyContainer">

		<tr>

			<td id = "panel">

                          

               	<img src = "images/panel_top.gif" alt = "panel top" onClick="ShowHide('panelContent2'); UpdateText(changer); " style="cursor:pointer;" />	

				<div id = "leftColumn2" style="cursor:pointer;" onClick="ShowHide('panelContent2'); UpdateText(changer); ">

                    <h1 id = "changer">Select a Route:</h1>

					<div id = "panelContent2" style="display:none " >

						<ul id = "routeList" >

					    </ul>

					</div>

				</div>

				<img src = "images/panel_bottom.gif" alt = "panel bottom" onClick="ShowHide('panelContent2'); UpdateText(changer); " style="cursor:pointer;" class = "panel_bottom_image" />

   

				<img src = "images/panel_top.gif" onClick="ShowHide2('panelContent7'); UpdateText2(changer2); " style="cursor:pointer;" alt = "panel top" />

				<div id = "leftColumn7" onClick="ShowHide2('panelContent7'); UpdateText2(changer2); " style="cursor:pointer;">

					<h1 id = "changer2">Select a Stop:</h1>

					<div id = "panelContent7" style="display:none ">

						<ul id = "stopList2">

						</ul>

					</div>

				</div>

				<img src = "images/panel_bottom.gif" onClick="ShowHide2('panelContent7'); UpdateText2(changer2); " style="cursor:pointer;" alt = "panel bottom" class = "panel_bottom_image" />   



   

   

           

				

					 <img src = "images/panel_top.gif" alt = "panel top" onClick="location.href='index.php';" style="cursor:pointer;" />   	  

					<div id = "leftColumn1" onClick="location.href='index.php';" style="cursor:pointer;">

						<h1>Reset Map</h1>

						<div id = "panelContent1">

							<div id = "tableContainer1"></div>

						</div>

					</div>

					<img src = "images/panel_bottom.gif" alt = "panel bottom"  style="cursor:pointer;" class = "panel_bottom_image" onClick="location.href='index.php';" />

				

               	<img src = "images/panel_top.gif" alt = "panel top" style="cursor:pointer;" onClick="location.href='lineview.php';" />	           

                <div id = "leftColumn3" onClick="location.href='lineview.php';" style="cursor:pointer;">

                    <h1>Line View</h1>

					<div id = "panelContent3">

						<div id = "tableContainer2">					    </div>

					</div>

				</div>

				<img src = "images/panel_bottom.gif" alt = "panel bottom" style="cursor:pointer;" class = "panel_bottom_image" onClick="location.href='lineview.php';" />	 

          

                

                <img src = "images/panel_top.gif" alt = "panel top" style="cursor:pointer;" onClick="location.href='resources.php';" />	           

                <div id = "leftColumn3" onClick="location.href='resources.php';" style="cursor:pointer;">

                    <h1>Resources</h1>

					<div id = "panelContent3">

						<div id = "tableContainer2">					    </div>

					</div>

				</div>

				<img src = "images/panel_bottom.gif" alt = "panel bottom" style="cursor:pointer;" class = "panel_bottom_image" onClick="location.href='resources.php';" />

                

                               	<img src = "images/panel_top.gif" alt = "panel top" style="cursor:pointer;" onClick="location.href='aboutus.php';" />	           

                <div id = "leftColumn3" onClick="location.href='aboutus.php';" style="cursor:pointer;">

                    <h1>About Us</h1>

					<div id = "panelContent3">

						<div id = "tableContainer2">					    </div>

					</div>

				</div>

				<img src = "images/panel_bottom.gif" alt = "panel bottom" style="cursor:pointer;" class = "panel_bottom_image" onClick="location.href='aboutus.php';" />

                      <img src = "images/blockMemboss.gif" alt = "logo" />

                </td>

			<td style=" vertical-align:top; width:100%; height:100%">

																	

				<table cellpadding = "0" cellspacing = "0" border = "0" id = "mapContainer" style="height:800px" >

					<tr class = "shadowTop">

						<td class = "upperLeft"></td>

						<td class = "upperMid"></td>

						<td class = "upperRight"></td>

					</tr>

					<tr>

						<td class = "midLeft" ></td>

						<td id = "mapCell" >

							<div id="mapdiv" style="width: 100%; height: 100%; "> </div>

                         </td>

						<td class = "midRight" ></td>

					</tr>

					<tr class = "shadowBottom">

						<td class = "bottomLeft"></td>

						<td class = "bottomMid"></td>

						<td class = "bottomRight"></td>

					</tr>

			    </table>

		</tr>

    </table>

                    <div id = "footer"><center>

                  &copy; 2007-2012 <a href = "http://www.regents.umich.edu" target = "_blank">The Regents of the University of Michigan</a>. <a href = "https://mbus.pts.umich.edu">Administrator login.</a>.<br/>The Magic Bus system and this website are still under development.  Bus riders are encouraged to use it.  However, reliable and accurate service is not guaranteed.



<a id="textonly" title="click here for text only page" href = "../../mobile/index.php">(click here for text only page)</a>                </center></div></td>

 <script type="text/javascript">

var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");

document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));

</script>

<script type="text/javascript">

try {

var pageTracker = _gat._getTracker("UA-10011318-1");

pageTracker._trackPageview();

} catch(err) {}</script>             

</body>

</html>

