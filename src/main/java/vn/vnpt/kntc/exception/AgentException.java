package vn.vnpt.kntc.exception;

public class AgentException extends RuntimeException {

    private final String agentId;

    public AgentException(String agentId, String message) {
        super("[" + agentId + "] " + message);
        this.agentId = agentId;
    }

    public AgentException(String agentId, String message, Throwable cause) {
        super("[" + agentId + "] " + message, cause);
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}
