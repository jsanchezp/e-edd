package es.ucm.fdi.edd.ui.views.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.emf.common.util.EList;

import es.ucm.fdi.edd.core.graphviz.GraphViz;
import es.ucm.fdi.edd.core.json.model.Edges;
import es.ucm.fdi.edd.core.json.model.JsonDocument;
import es.ucm.fdi.edd.core.json.model.Vertices;
import es.ucm.fdi.edd.core.json.utils.JsonHelper;
import es.ucm.fdi.edd.core.util.FileManager;
import es.ucm.fdi.emf.model.ed2.ED2;
import es.ucm.fdi.emf.model.ed2.Ed2Factory;
import es.ucm.fdi.emf.model.ed2.Node;
import es.ucm.fdi.emf.model.ed2.TreeElement;

/**
 * EDD Helper 
 */
public class EDDHelper {
	
	private File file;
	private JsonDocument document;

	/**
	 * @param file
	 */
	public EDDHelper(File file) {
		this.file = file;
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	public void startEDDebugger(String buggyCall, String location) throws Exception {
//		String[] args = new String[] {"ackermann:main([3,4])", "../examples/ackermann/"};
		String[] args = new String[] {buggyCall, location};
//		EDDJInterface.main(args);
	}
	
	/**
	 * 
	 */
	public void readJson() {
		this.document = readJsonFromFile();
	}
	
	/**
	 * @return
	 */
	private JsonDocument readJsonFromFile() {
		JsonDocument document = null;
		try {
			document = (JsonDocument) JsonHelper.readJson(file.getAbsolutePath(), JsonDocument.class);
			return document;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return document;
	}
	
	/**
	 * @throws IOException 
	 */
	public void buildDOT(File file, int highlightNode, boolean cw) throws IOException {
		GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		gv.addln("\tgraph [dpi = 600]; ");
//		gv.addln("\tratio=\"fill\"; ");
		gv.addln("\tsize=\"8.3,11.7!\"; ");
		gv.addln("\tmargin = 0; ");
//		gv.addln("\tnode [shape=circle]; ");
		gv.addln("\tnode [style=filled]; ");
		gv.addln("\tnode [fillcolor=\"#EEEEEE\"]; "); // Relleno gris
//		gv.addln("\tnode [color=\"#EEEEEE\"]; "); // Borde gris
		gv.addln("\tedge [color=\"#7092BE\"]; ");
		
		LinkedList<Vertices> vertices = document.getVertices();
		for (int i=0; i<vertices.size(); i++) {
			Vertices vertice = vertices.get(i);
			String node = vertice.getNode();
			if (Integer.parseInt(node) == highlightNode) {
				gv.addln(node + " [label=\"" + node + ". " + vertice.getQuestion() + "\", style=filled, fillcolor=\"#ED1C3A\"];");
			}
			else {
				gv.addln(node + " [label=\"" + node + ". " + vertice.getQuestion() + "\"];");
			}
		}
		
		LinkedList<Edges> edges = document.getEdges();
		for (Edges edge : edges) {
			int from = edge.getFrom();
			int to = edge.getTo();
			if (from == highlightNode) {
				gv.addln(from + " -> "+ to + " [style=filled, color=\"#ED1C3A\", fillcolor=\"#ED1C3A\"];");
			}
			else {
				gv.addln(from + " -> "+ to + ";");
			}
		}
		
		// Para cambiar el sentido
		if (cw) {
			gv.addln("rankdir=LR;"); 
		}
		gv.addln(gv.end_graph());
		
		String dotSource = gv.getDotSource();
		FileManager.writeToFile(file.getAbsolutePath(), dotSource);
		
//		String type = "gif";
//		File out = new File("files/output/images/sample1." + type);
//		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
	}
	
	public ED2 buildEMF(String name) {
		ED2 root = Ed2Factory.eINSTANCE.createED2();
		root.setName(name);
		
		Map<Integer, Node> nodesMap = new HashMap<Integer, Node>();
		LinkedList<Vertices> vertices = document.getVertices();
		for (Vertices vertice : vertices) {
			Node node = Ed2Factory.eINSTANCE.createNode();
			int index = Integer.parseInt(vertice.getNode());
			node.setIndex(index);
			node.setName(index + ": " +vertice.getQuestion());
			
			nodesMap.put(index, node);
		}
		
		LinkedList<Edges> edges = document.getEdges();
		for (Edges edge : edges) {
			int from = edge.getFrom();
			int to = edge.getTo();
			
			Node source = nodesMap.get(from);
			Node target = nodesMap.get(to);
			source.getNodes().add(target);
		}
		
		EList<TreeElement> elements = root.getTreeElements();
		elements.add(nodesMap.get(90)); // Raíz
		
		return root;
	}
	
	/**
	 * @return
	 */
	public Integer getQuestionsSize() {
		return document.getVertices().size();
	}

	/**
	 * @param i
	 * @return
	 */
	public String getQuestion(int index) {
		LinkedList<Vertices> vertices = document.getVertices();
		for (Vertices vertice : vertices) {
			int node = Integer.parseInt(vertice.getNode());
			if (node == index) {
				return vertice.getQuestion();
			}
		}
		
		return null;
	}
}