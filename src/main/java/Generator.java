public interface Generator {
    void generateSystem();
    void computePossibilities();
    void collapseAt(int node);
}
