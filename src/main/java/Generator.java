public interface Generator {
    void generateSystem();
    void computePossibilitiesAtI(int i);
    void collapseAt(int node);
}
