public interface Generator {
    void generateReceive(String source, String variable);
    void generateBranching(String source, String label);
    void generateBranch(String label);

    void generateSend(String destination, String variable);
    void generateSelect(String destination, String label);

    void generateIf(String cdt);
    void generateElse();

    void generateCall(String variableName);
    void generateDef(String variableName);

    void generateEnd();

    void generateSystem(int nodes);
}
