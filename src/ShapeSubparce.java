import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import com.linuxense.javadbf.DBFReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class ShapeSubparce extends Shape {

	private List<LineString> poligons; //[0] Outer, [1..N] inner
	private List<Long> nodes;
	private List<Long> ways;
	private Long relation; // Relacion de sus ways
	private String refCatastral; // Referencia catastral
	private String subparce; // Clave de Subparcela
	private String cultivo; // Tipo de cultivo de la subparcela
	private List<ShapeAttribute> atributos;


	/** Constructor
	 * @param f Linea del archivo shp
	 * @throws IOException 
	 */
	public ShapeSubparce(SimpleFeature f) throws IOException {
		
		super(f);
		
		this.poligons = new ArrayList<LineString>();

		// Parcela.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			Geometry geom = (Geometry) f.getDefaultGeometry();

			// Cogemos cada poligono del shapefile (por lo general sera uno solo
			// que puede tener algun subpoligono)
			for (int x = 0; x < geom.getNumGeometries(); x++) { 
				Polygon p = (Polygon) geom.getGeometryN(x); 

				// Obtener el outer
				LineString outer = p.getExteriorRing();
				poligons.add(outer);

				// Comprobar si tiene subpoligonos
				for (int y = 0; y < p.getNumInteriorRing(); y++)
					poligons.add(p.getInteriorRingN(y));
			}
		}
		else {
			System.out.println("Formato geométrico "+ f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile");
		}

		this.nodes = new ArrayList<Long>();
		this.ways = new ArrayList<Long>();

		// Los demas atributos son metadatos y de ellos sacamos 
		refCatastral = (String) f.getAttribute("REFCAT");
		subparce = (String) f.getAttribute("SUBPARCE");
		cultivo = getTipoCultivo(subparce);

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/
	}


	public void addNode(long nodeId){
		if (!nodes.contains(nodeId))
			nodes.add(nodeId);
	}

	public void addWay(long wayId){
		if (!ways.contains(wayId))
			ways.add(wayId);
	}

	public void removeWay(long wayId){
		ways.remove(wayId);
	}

	public void setRelation(long relationId){
		relation = relationId;
	}

	public List<LineString> getPoligons(){
		return poligons;
	}

	public List<Long> getNodes(){
		return nodes;
	}

	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @param utils clase utils que tiene metodos
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public List<Long> getNodesPoligonN(int pos, Cat2OsmUtils utils){

		if (getPoligons().size()>pos){
			List<Long> l = new ArrayList<Long>();
			for (int x = 0; x < poligons.get(pos).getNumPoints(); x++)
				l.add(utils.getNodeId(getPoligons().get(pos).getCoordinateN(x), null));
			return l;
		}
		return null;
	}

	/** Devuelve la lista de ids de ways (todos como ways de 2 nodos)
	 * del poligono en posicion pos
	 * No vale para despues de la simplificacion de ways
	 * @param pos posicion que ocupa el poligono en la lista
	 * @param utils clase utils que tiene metodos
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public List<Long> getWaysPoligonN(int pos, Cat2OsmUtils utils){

		if (getPoligons().size()>pos){
			List<Long> wayList = new ArrayList<Long>();
			List<Long> nodeList = getNodesPoligonN(pos, utils);
			for (int x = 0; x < nodeList.size()-1; x++){
				List<Long> way = new ArrayList<Long>();
				way.add(nodeList.get(x));
				way.add(nodeList.get(x+1));
				wayList.add(utils.getWayId(way, null));
			}

			return wayList;
		}
		return null;
	}

	/** Comprueba la fechaAlta y fechaBaja del shape para ver si se ha creado entre AnyoDesde y AnyoHasta
	 * @param shp Shapefile a comprobar
	 * @return boolean Devuelve si se ha creado entre fechaAlta y fechaBaja o no
	 */
	public boolean checkShapeDate(long fechaDesde, long fechaHasta){
		return (fechaAlta >= fechaDesde && fechaAlta < fechaHasta && fechaBaja >= fechaHasta);
	}

	public List<Long> getWays() {
		return ways;
	}

	public Long getRelation(){
		return relation;
	}

	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	/** Devuelve los atributos del shape
	 * @return Lista de atributos
	 */
	public List<String[]> getAttributes(){
		List <String[]> l = new ArrayList<String[]>();

		if (refCatastral != null){
			String[] s = new String[2];
			s[0] = "catastro:ref"; s[1] = refCatastral;
			l.add(s);
		}
		
			if (subparce != null){
				String[] s = new String[2];
				s[0] = "SUBPARCE"; s[1] = subparce;
				l.add(s);
		}

		//s = new String[2];
		//s[0] = "FECHAALTA"; s[1] = String.valueOf(fechaAlta);
		//l.add(s);

		//s = new String[2];
		//s[0] = "FECHABAJA"; s[1] = String.valueOf(fechaBaja);
		//l.add(s);

		return l;
	}

	public String getRefCat(){
		return refCatastral;
	}
	
	public String getSubparce(){
		return subparce;
	}

	public Coordinate[] getCoordenadas(int i){
		return poligons.get(i).getCoordinates();
	}

	public Coordinate getCoor(){
		return null;
	}
	
	
	/** Relaciona el numero de subparcela que trae el Subparce.shp con
	 * el codigo de cultivo que trae Rusubparcela.dbf y este a su vez con el 
	 * Rucultivo.dbf. Solo es para subparcelas rurales
	 * @param v Numero de subparcela a buscar
	 * @return String tipo de cultivo
	 * @throws IOException
	 */
	public String getTipoCultivo(String s) throws IOException{
		InputStream inputStream  = new FileInputStream(Config.get("RuralSHPDir") + "\\RUSUBPARCELA\\RUSUBPARCELA.DBF");
		DBFReader reader = new DBFReader(inputStream); 

		
		Object[] rowObjects;
		
		while((rowObjects = reader.nextRecord()) != null) {
			
			if ( rowObjects[2].equals(s)){
				inputStream.close();
				return ((String) rowObjects[3]);
			}
		}
		return null;
	}  
	
	
}
