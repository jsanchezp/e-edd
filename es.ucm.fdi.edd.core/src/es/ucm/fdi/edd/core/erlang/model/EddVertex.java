package es.ucm.fdi.edd.core.erlang.model;

public class EddVertex {
	
	private Integer node;
	private String question;
	private EddInfo info;
	
	public EddVertex(Integer node, String question, EddInfo info) {
		this.node = node;
		this.question = question;
		this.info = info;
	}
	
	public EddVertex(Long node, String question, EddInfo info) {
		this.node = node.intValue();
		this.question = question;
		this.info = info;
	}

	public Integer getNode() {
		return node;
	}

	public void setNode(Integer node) {
		this.node = node;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}
	
	public EddInfo getInfo() {
		return info;
	}

	public void setInfo(EddInfo info) {
		this.info = info;
	}
}