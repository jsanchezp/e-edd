package es.ucm.fdi.edd.core.erlang.model;

public class EddVertex {
	
	private Integer node;
	private String question;
	private EddInfo info;
	private MFA mfa;
	
	/**
	 * Normal debugging
	 * 
	 * @param node
	 * @param question
	 * @param info
	 */
	public EddVertex(Integer node, String question, EddInfo info) {
		this.node = node;
		this.question = question;
		this.info = info;
	}
	
	/**
	 * Normal debugging
	 * 
	 * @param node
	 * @param question
	 * @param info
	 */
	public EddVertex(Long node, String question, EddInfo info, MFA mfa) {
		this.node = node.intValue();
		this.question = question;
		this.info = info;
		this.mfa = mfa;
	}
	
	/**
	 * Zoom debugging
	 * 
	 * @param node
	 * @param question
	 */
	public EddVertex(Long node, String question) {
		this.node = node.intValue();
		this.question = question;
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

	public MFA getMfa() {
		return mfa;
	}

	public void setMfa(MFA mfa) {
		this.mfa = mfa;
	}
}