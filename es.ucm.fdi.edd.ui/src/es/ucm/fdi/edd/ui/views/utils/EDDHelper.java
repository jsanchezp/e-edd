package es.ucm.fdi.edd.ui.views.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import es.ucm.fdi.edd.core.erlang.jinterface.EDDJInterface;
import es.ucm.fdi.edd.core.graphviz.GraphViz;
import es.ucm.fdi.edd.core.json.model.Edges;
import es.ucm.fdi.edd.core.json.model.JsonDocument;
import es.ucm.fdi.edd.core.json.model.Vertices;
import es.ucm.fdi.edd.core.json.utils.JsonHelper;

/**
 * 
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
	public void startEDDebugger() throws Exception {
		String[] args = new String[] {"ackermann:main([3,4])", "../examples/ackermann/"};
		EDDJInterface.main(args);
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
	 * 
	 */
	public void buildDOT() {
		GraphViz gv = new GraphViz();
		gv.addln(gv.start_graph());
		gv.addln("\tgraph [dpi = 400]; ");
		gv.addln("\tratio=\"fill\"; ");
		gv.addln("\tsize=\"8.3,11.7!\"; ");
		gv.addln("\tmargin = 0; ");
//		gv.addln("\tnode [shape=circle]; ");
		gv.addln("\tnode [style=filled]; ");
		gv.addln("\tnode [fillcolor=\"#EEEEEE\"]; ");
		gv.addln("\tnode [color=\"#EEEEEE\"]; ");
		gv.addln("\tedge [color=\"#31CEF0\"]; ");
		
		LinkedList<Vertices> vertices = document.getVertices();
		for (int i=0; i<vertices.size(); i++) {
			Vertices vertice = vertices.get(i);
			String node = vertice.getNode();
			gv.addln(node + " [label=\"" + node + ". " + vertice.getQuestion() + "\"];");
		}
		
		LinkedList<Edges> edges = document.getEdges();
		for (Edges edge : edges) {
			gv.addln(edge.getFrom() + " -> "+ edge.getTo() + ";");
//			int from = edge.getFrom();
//			gv.addln(from + " -> "+ edge.getTo() + " [label=\"" + vertices.get(from).getQuestion() + "\"];");
		}
		
//		gv.addln("rankdir=LR;"); // Para cambiar el sentido 
		gv.addln(gv.end_graph());
		
		String dotSource = gv.getDotSource();
		System.out.println(dotSource);
		
//		String type = "gif";
//		File out = new File("files/output/images/sample1." + type);
//		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), type), out);
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