package engine;

public interface OwnerAnalyzer<T> {
    T getOwner(String name, String namespace);
}