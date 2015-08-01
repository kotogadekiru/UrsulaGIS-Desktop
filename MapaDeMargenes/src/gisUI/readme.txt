harvestMapViewer0.1.jar is a demo aplication to display the information contained in a harvest map.
To use just open harvestMapViewer0.1.jar with a java 8 jre installed and select the shapeflile. 
La Union L3 C.shp is a sample file intended to be used in tests.
A harvest map to be compatible with harvestMapViewer0.1 must have the following properties in its shcema.
"Curso_deg_" : direction of movement in degrees from the north
"Anch__de_f" : with of the harvest head
"Distancia_" : distance advanced in this feature
"Prod__ha_h" : yield 

harvestMapViewer tries to correct inconsistencies of the harvest shapefile like gaps between consecutive features and overlapping.